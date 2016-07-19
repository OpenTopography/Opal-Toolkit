#!env python
#
# Luca Clementi 
#
#This script can be used to migrate data from 
#opal 1.9.X to opal 2.X postgres database format
#Change the first line of this script to adapt 
#it to you database configuration

import pg
import sys

#changeME
db_new = pg.connect (dbname="opal2_db", user="opal_user", passwd="opal_passwd", host='localhost', port=5433)
db_old = pg.connect (dbname="opal_db", user="opal_user", passwd="opal_passwd", host='localhost', port=5433)
startoutputID=60
startoutfilesID=60
#end changeme

old_job_status = db_old.query("select * from job_status")
counter=0
for entry in old_job_status.dictresult():
    #let get all the data we need 
    job_id = entry["job_id"]
    #job_outputs = db_old.query("select * from job_output where job_id = \'%s\'" % job_id)
    #output_files = db_old.query("select * from output_file where job_id = \'%s\'" % job_id)

    #we have to parse starttime and lastupdate
    import time, datetime
    start_time = datetime.datetime(*time.strptime(entry["start_time"], "%b %d, %Y %I:%M:%S %p")[0:6])
    last_update = datetime.datetime(*time.strptime(entry["last_update"], "%b %d, %Y %I:%M:%S %p")[0:6])

    #we have everything we need let's start to load the entry in the new DB
    #first with the job_info table
    #job_id code message base_url handle 
    result = db_new.query("insert into job_info values (\'"+job_id+"\', "+str(entry["code"])+", \'"+pg.escape_string(entry["message"])+"\', \'"+pg.escape_string(entry['base_url'])+"\', \'null\'," + \
        #start_time_date start_time_time activation_time_date activation_time_time completion_time_date completion_time_time 
        "\'"+str(start_time.date())+"\', \'"+str(start_time.time())+"\', NULL, NULL, NULL, NULL, " + \
        #last_update_date last_update_time client_dn client_ip service_name 
        "\'"+str(last_update.date())+"\', \'"+str(last_update.time())+"\', \'"+pg.escape_string(entry["client_dn"])+"\', \'"+entry["client_ip"]+"\', \'"+entry["service_name"]+"\') ")
    #if result != 1:
        #print "insert failed! Aborting"
        #sys.exit(-1)
    #second we load the job_outputs table 
    #if you are migrating you probably don't need to bring this in the new DB
    #for output in job_outputs.dictresult():
        #db_new.query("insert into job_output (id, job_id, std_out, std_err) values ("+str(startoutputID)+", \'"+job_id+"\', \'"+output['std_out']+\
        #    "\', \'"+output['std_err']+"\')")
        #startoutputID=startoutputID+1
    #for file in output_files.dictresult():
        #db_new.query("insert into output_file (id, url, name, job_id) values ("+str(startoutfilesID)+", \'"+file['url']+"\', \'"+file['name']+"\', \'"+job_id+"\')")
        #startoutfilesID=startoutfilesID+1
    #let's get some progress output
    counter=counter+1
    if counter%5 == 0:
        print "" + str(counter) + " inserted"











