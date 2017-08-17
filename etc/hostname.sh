#!/bin/bash
#
# Luca Clementi
#
# This script can be used to change the host name 
# of a opal instance running inside Amazon at boot 
# time. This script should be called by the tomcat 
# startup scripti.
#

#Insert here the opal.properties file locations
Scripts="/opt/tomcat-5.0.30/webapps/opal2/WEB-INF/classes/opal.properties /opt/opal/etc/opal.properties"

EC2METADATA=/opt/ec2/bin/ec2-metadata

HOSTNAME=""
$EC2METADATA -p

if [ $? == 1 ] ;
then 
    #local
    HOSTNAME=`hostname`
else
    hostnametemp=`$EC2METADATA -p`
    HOSTNAME=${hostnametemp##"public-hostname: "}
fi

if [ -z "$HOSTNAME" ] ;
then 
    echo "Could not determine the host name exiting"
    exit
fi


for i in $Scripts;
do 
    echo modifing $i
    sed -i "s%^tomcat.url=http://.*:8080%tomcat.url=http://$HOSTNAME:8080%" $i
    #debug
    #cat $i
done 

