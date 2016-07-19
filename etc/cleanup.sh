#!/bin/bash

# Removes opal job directories older then default days

CATALINA_HOME=/opt/tomcat
# default number of days to keep job direcotry 
n=4

cd $CATALINA_HOME/webapps/ROOT
dirs=`find . -type d -name 'app*' -mtime +$n -print`

for i in $dirs
do
    rm -rf $i
done

