Opal Toolkit
----------------
- Opal is a toolkit for wrapping scientific applications rapidly as Web services on cluster, grid or cloud resources and exposing them to various clients. End-users access these applications as a service using simple Web service APIs from their custom application and workflow environment.

- Opal provides several features such as job scheduling (using Condor and SGE via Globus or DRMAA), job data and state management as well as security (using GSI-based certificates). The application developer specifies a configuration for a scientific application and deploys the application as a service following a small sequence of steps. 

- In this directory you will find the core Opal package implemented in Java, which includes both client and server sides. Documentation is in the "docs" directory. Tested with Apache Tomcat 7.x asnd Tomcat 8.X.

- This is the OpenTopography (http://www.opentopography.org) fork of the Opal toolkit originally developed by Sriram Krishnan at the San Diego Supercomputer Center at UC San Diego and later updated by Luca Clementi. 

- This project was originally supported by grants from the National Center for Research Resources (5P41RR008605-19) and the National Institute of General Medical Sciences (8 P41 GM103426-19) from the National Institutes of Health.

#### XSEDE Comet

Edit $OPAL_HOME/etc/opal.properties to configure the static container properties correctly. The template
file looks similar to the following:

```$xslt
...
...

# Slurm scheduler for submitting the job
slurm.num.procs=<number of cpu, for exmple, 24 >
slurm.run=<MPI launcher command, for example, ibrun >
slurm.job.scriptdir=<the script directory for your scripts to be run in a cluster, for example, /home/opentopo/ot-taudem/scripts >
slurm.job.account=<the account for XSEDE allocation, for example, sds000> 
slurm.job.wdir=<working directory in a cluster, for example, /oasis/scratch/comet/opentopo/temp_project >
slurm.job.queue=<the queue type in a cluster, for example, shared >
slurm.job.ppn=<the number of cores per node, for example, 24 >
slurm.job.walltime=<the execution time, for example, 00:30:00 >

#ssh connection to XSEDE Comet
slurm.host=<the front end in a cluster, for example, comet.sdsc.edu >
slurm.port=<ssh port number, for example, 22 >
slurm.user=<community account or user account>
slurm.password=<password>
slurm.key.file=<ssh credentials for a user, for example, /Users/cyoun/.ssh/id_rsa >

# gateway attribute submission
gateway.attribute=true

...
...

```