#!/bin/bash

# USER-DEFINED PARAMETERS
## You should modify these parameters

# script directory path on local machine
shome="/home/opaluser/opal/OpalCheck_namd_mirume"

# notify
email="your_name@somedomain.com"
# email even if opal is running fine
email_if_good="yes"
log="$shome/check_opal.log"

# local opal installation
opal_home="/home/opaluser/opal/opal-ws-2.0beta"

# remote opal service info
opal_host="mirume.nbcr.net"
opal_version="2"                      
opal_service="namd2"

# opal job
args="free_eq2.namd"
# num of CPUS, use empty string if serial job
ncpu="2"
# input files with absolute paths, separated with commas with no space 
input_files="$shome/A.inpcrd,$shome/command,$shome/free_eq2.namd.sge,$shome/free_eq.restart.vel,$shome/rsl.namd,$shome/A.prmtop,$shome/free_eq2.namd,$shome/free_eq.restart.coor,$shome/free_eq.restart.xsc"

# expect output files, separated with commas
output_files="stdout.txt"
# report time-out error if job is not completed in the time frame
# in seconds
timeout=36000

#---------------------------------------------------------------------------

tmp=`echo $opal_host | grep "http://"`

if test -z $tmp; then
  full_opal_host="http://"$opal_host
else
  full_opal_host=$opal_host
fi

if test $opal_version = 2 || test $opal_version = "2"; then
  link_middle="/opal2/services/"
fi

service_link="$full_opal_host$link_middle$opal_service"

if test -z $ncpu; then
  parallel=""
else
  parallel="-n $ncpu"
fi

launch_cmd="java edu.sdsc.nbcr.opal.GenericServiceClient \
-l $service_link \
-r launchJob \
-a $args \
$parallel \
-f \"$input_files\""

cd $opal_home
source etc/classpath.sh > /dev/null
cd - > /dev/null

start_time=`date +%s`
date=`date`

echo "$date --" > $shome/email.txt
echo "Job launched: $launch_cmd" >> $shome/email.txt

#$launch_cmd >& $shome/tmp 
java edu.sdsc.nbcr.opal.GenericServiceClient \
-l $service_link \
-r launchJob \
-a "$args" \
$parallel \
-f "$input_files" >& $shome/tmp

ls $shome/tmp > /dev/null
output_url=`grep "Output Base URL" $shome/tmp | awk -F" " '{print $4}'`

if test -z $output_url; then
  echo "Opal job submission has failed." >> $shome/email.txt
  echo >> $shome/email.txt
  echo "Here is the output of " >> $shome/email.txt
  echo "$launch_cmd" >> $shome/email.txt    
  echo "--------------------------------------------------------------" >> $shome/email.txt
  cat $shome/tmp >> $shome/email.txt

  mailx -s "Opal $opal_service on $opal_host Failed" $email < $shome/email.txt
  cat $shome/email.txt >> $log
  rm -f $shome/tmp
  exit 0
fi

job_id=`grep "Received jobID" $shome/tmp | awk -F" " '{print $3}'`
query_cmd="java edu.sdsc.nbcr.opal.GenericServiceClient \
-l $service_link \
-r queryStatus \
-j $job_id"

job_done=-1

rm -f $shome/tmp

while test $job_done = -1; do
  cur_time=`date +%s`

  if test `expr $cur_time - $start_time` -gt $timeout; then
    job_done=-2
  fi

  output=`$query_cmd`

  check4=`echo $output | grep "Code: 4" | awk -F" " '{print $1}'`
  check8=`echo $output | grep "Code: 8" | awk -F" " '{print $1}'`

  if test ! -z $check4; then      #error
    job_done=4
  elif test ! -z $check8; then    #success
    job_done=8
  else
    sleep 3
  fi
done

expect_found="true"

date=`date`

echo "$date --" >> $shome/email.txt

if test $job_done = 4; then
  echo "Job on remote server $opal_host failed." >> $shome/email.txt
elif test $job_done = 8; then
  echo $output_files > $shome/tmp_outfiles
  sed -i 's/,/ /g' $shome/tmp_outfiles
  outfiles=`cat $shome/tmp_outfiles`
  rm -f $shome/tmp_outfiles

  for i in $outfiles; do
    cd $shome
    wget -q $output_url/$i
    ls $output_url/$i >& /dev/null
    cd - > /dev/null

    if test -e $shome/$i; then
      rm -f $shome/$i
    else
      echo "Expected output file $i is missing." >> $shome/email.txt
      expect_found="false"
    fi
  done
   
  echo "Job on remote server $opal_host completed sucessfully according to status code." >> $shome/email.txt
elif test $job_done = -2; then
  echo "Job on remote server $opal_host not completed after $timeout seconds.  This job is currently still running." >> $shome/email.txt
fi

echo "Visit $output_url for the output of" >> $shome/email.txt
echo "$launch_cmd" >> $shome/email.txt

if test $job_done = 4; then
  mailx -s "Opal $opal_service on $opal_host Failed" $email < $shome/email.txt
  cat $shome/email.txt >> $log
elif test $job_done = 8; then
  if test $expect_found = "false"; then
    mailx -s "Opal $opal_service Job on $opal_host Completed with Missing Outputs" $email < $shome/email.txt
    cat $shome/email.txt >> $log
    exit
  fi

  if test $email_if_good = "yes"; then
    mailx -s "Opal $opal_service on $opal_host Running Successfully" $email < $shome/email.txt
    cat $shome/email.txt >> $log
  fi
elif test $job_done = -2; then
  mailx -s "Opal $opal_service on $opal_host Timed Out" $email < $shome/email.txt
  cat $shome/email.txt >> $log
fi

rm -f $shome/tmp
