#!/usr/bin/python2
#
# Luca Clementi and Jane Rane
#
#  This script can be used to wrap the autodock command 
#  after that autodock sintax is
#  autodock <commands> URL
#  where URL is a URL pointing at a opal output directory of a 
#  autogrid run.
#  This script take the URL away from the command line 
#  download all the file from there and then invoke 
#  autodock <commands>
#


import sys, commands, os

#insert here the location of you autodock executable 
AUTODOCK = '/opt/bio/autodocksuite/bin/autodock4'

#insert here the location of this script
CURRENTBIN = '/share/apps/bin/run_autodock.py';

numdirs = -2

autogrid_url = sys.argv[len(sys.argv)-1]
autogrid_url_split = autogrid_url.split('/')

for autogrid_url_substr in autogrid_url_split:
	if len(autogrid_url_substr) > 0:
	         numdirs = numdirs + 1
				
CURL = '''/usr/bin/wget -r -nH --cut-dirs=''' + repr(numdirs) + ''' '''

cmd = '''ulimit -s unlimited ; ''' + AUTODOCK + ''' '''
cmds = []
links = []
garbage = []

for thing in sys.argv:
        if thing.startswith("http"):
                links.append(thing)
        elif thing.startswith(CURRENTBIN):
                garbage.append(thing)
        else:
                cmds.append(thing)

for url in links:
        wCmd = CURL + url
        #print 'getting link ' + wCmd
        stat,out = commands.getstatusoutput(wCmd)

if ( os.path.exists("index.html") ):
    os.remove("index.html")

for arg in cmds:
        cmd+=arg + ''' '''

#print 'running cmd ' + cmd

status,output = commands.getstatusoutput(cmd)

print output
