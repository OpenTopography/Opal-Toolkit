package edu.sdsc.nbcr.opal.manager;

import java.util.Properties;

import java.io.*;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Properties;
import java.net.InetAddress;
import org.globus.gram.GramJob;
import java.lang.System;
import org.apache.log4j.Logger;

import edu.sdsc.nbcr.opal.AppConfigType;
import edu.sdsc.nbcr.opal.StatusOutputType;

/**
 *
 * Implementation of an Opal Job Manager using CSF4
 */
public class CSFJobManager implements OpalJobManager {

    // get an instance of a log4j logger
    private static Logger logger = Logger.getLogger(CSFJobManager.class.getName());

    private Properties props; // the container properties being passed
    private AppConfigType config; // the application configuration
    private Process proc_submit_job; // submit job
    private Process proc_check_status; // check job status
    private Process proc; 			// application process
    private StatusOutputType status; // current status
    private String handle; // the OS specific process id for this job
    private Thread stdoutThread; // the thread that writes stdout
    private Thread stderrThread; // the thread that writes stderr
    private boolean started = false; // whether the execution has started
    private volatile boolean done = false; // whether the execution is complete
    private String[] envp=new String[8];//ENV for csf run
    private String csf_job_id = null; // job id from CSF4
    
    private String wd; // working directory
    /**
     * Initialize the Job Manager for a particular job
     *
     * @param props the properties file containing the value to configure this plugin
     * @param config the opal configuration for this application
     * @param handle manager specific handle to bind to, if this is a resumption. 
     * NULL,if this manager is being initialized for the first time.
     * 
     * @throws JobManagerException if there is an error during initialization
     */
    public void initialize(Properties props,
                           AppConfigType config,
                           String handle)
        throws JobManagerException {
        logger.info("called");

        this.props = props;
        this.config = config;
        this.handle = handle;

        

        envp[0]=new String("CLASSPATH="+System.getenv("CSF_CLASSPATH"));
        envp[1]=new String("SHLIB_PATH="+System.getenv("SHLIB_PATH"));
        envp[2]=new String("GLOBUS_PATH="+System.getenv("GLOBUS_PATH"));
        envp[3]=new String("PATH="+System.getenv("PATH"));
        envp[4]=new String("MANPATH="+System.getenv("MANPATH"));
        envp[5]=new String("LD_LIBRARY_PATH="+System.getenv("LD_LIBRARY_PATH"));
        envp[6]=new String("DYLD_LIBRARY_PATH="+System.getenv("DYLD_LIBRARY_PATH"));
        envp[7]=new String("LIBPATH="+System.getenv("LIBPATH"));
	logger.debug("GT/CSF4 Environment Variables:");
        logger.debug(envp[0]);
        logger.debug(envp[1]);
        logger.debug(envp[2]);
        logger.debug(envp[3]);
        logger.debug(envp[4]);
        logger.debug(envp[5]);
        logger.debug(envp[6]);
        logger.debug(envp[7]);
        
        // initialize status
        status = new StatusOutputType();
    }
    
    /**
     * General clean up, if need be 
     *
     * @throws JobManagerException if there is an error during destruction
     */
    public void destroyJobManager()
        throws JobManagerException {
        logger.info("called");

        // TODO: not sure what needs to be done here
        throw new JobManagerException("destroyJobManager() method not implemented");
    }
    
    /**
     * Launch a job with the given arguments. The input files are already staged in by
     * the service implementation, and the plug in can assume that they are already
     * there
     *
     * @param argList a string containing the command line used to launch the application
     * @param numProcs the number of processors requested. Null, if it is a serial job
     * @param workingDir String representing the working directory of this job on the local system
     * 
     * @return a plugin specific job handle to be persisted by the service implementation
     * @throws JobManagerException if there is an error during job launch
     */
    public String launchJob(String argList, 
                            Integer numProcs, 
                            String workingDir)
        throws JobManagerException {
        logger.info("called");
        
        wd = workingDir;
        
        // make sure we have all parameters we need
        if (config == null) {
            String msg = "Can't find application configuration - "
                + "Plugin not initialized correctly";
            logger.error(msg);
            throw new JobManagerException(msg);
        }

        // create list of arguments
        String args = config.getDefaultArgs();
        if (args == null) {
            args = argList;
        } else {
            String userArgs = argList;
            if (userArgs != null)
                args += " " + userArgs;
        }
        if (args != null) {
            args = args.trim();
        }
        logger.debug("Argument list: " + args);

        // get the number of processors available
        String systemProcsString = props.getProperty("num.procs");
        int systemProcs = 0;
        if (systemProcsString != null) {
            systemProcs = Integer.parseInt(systemProcsString);
        }

        // launch executable using process exec
        String cmd = null;
        String rsl = new String();

        // append arguments
        if ((args != null) && (!(args.equals("")))) {
            logger.debug("Appending arguments: " + args);
            //cmd += " " + args;
            //rsl + = " "+ args;
        }
        logger.debug("CMD: " + cmd);
        
        
     // launch executable using Globus
        String localIP = null;


        // get jobID
        String[] wd_tokens = workingDir.split("/");
        String jobID = wd_tokens[wd_tokens.length-1];

        String csf4WD = props.getProperty("csf4.workingDir");
        if (csf4WD == null) {
        logger.fatal("Can't find property: csf4.workingDir");
        }

        // create RSL for serial run
        String remoteDir = csf4WD + "/" + jobID;

        try {
            localIP = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            logger.error("Can't figure out IP address for localhost: " + e.getMessage());
            logger.error("Can't figure out IP address for localhost: " + e.getMessage());
            // throw new FaultType("Can't figure out IP address for localhost: " + e.getMessage());
        }
        
     // check if the binary location is of form APPNAME:BIN                                              
        String fullBinLocation = config.getBinaryLocation();
        int index = fullBinLocation.indexOf(":");
        String appName = null;
        String binLocation = null;

        if (index > 0) {
            appName = fullBinLocation.substring(0, index);
            binLocation = fullBinLocation.substring(index+1, fullBinLocation.length());
        } else {
            binLocation = fullBinLocation;
        }

        rsl = "&";

        if (appName != null) {
            rsl += "(application=" + appName + ")";
        }

        rsl +=
            "(executable=" + binLocation + ")" +
            "(directory=opal_runs/" + jobID + ")" +
            "(stdout=csf_stdout.txt)" +
            "(stderr=csf_stderr.txt)" +
            "(stagein=\"" + workingDir + "->opal_runs/" + jobID + "/\")" +
            "(stageout=\"" + remoteDir + "/->" + workingDir + "\")" ;
            //"(localgridftp=no)";        

        if (config.isParallel()) {
            // make sure enough processors are present for the job
            if (numProcs == null) {
                String msg = "Number of processes unspecified for parallel job";
                logger.error(msg);
                throw new JobManagerException(msg);
            } else if (numProcs.intValue() > systemProcs) {
                String msg = "Processors required - " + numProcs +
                    ", available - " + systemProcs;
                logger.error(msg);
                throw new JobManagerException(msg);
            }

            rsl += "(jobtype=\"mpi\")" + "(count=" + numProcs + ")";
        }

        // add arguments to the RSL
        if (args != null) {
            // put every argument within quotes - needed by Globus if some of 
            // the arguments are of the form name=value
            args = "\"" + args + "\"";
            args = args.replaceAll("[\\s]+", "\" \"");
            rsl += "(arguments=" + args + ")";
        }
        logger.debug("RSL: " + rsl);
        
     
        
    BufferedWriter out;
    String read;
    String rslfile=new String(workingDir+"/job.rsl");
    //rsl="&(executable=/bin/hostname)(stdout=vs.output)(stderr=vs.error)";
        try {
            out = new BufferedWriter(new FileWriter(rslfile));
            out.write(rsl);//Write out the specfied string to the file
            
            //out = new BufferedWriter(new FileWriter("test.txt"));
            //out.write("testing");
            out.close();//flushes and closes the stream
    }catch(IOException e){
            System.out.println("There was a problem:" + e);
    }
    
      cmd="csf-job-create"+" "+"-r"+" "+rslfile+" "+"-sub";
        // run executable in the working directory
        try {
            logger.debug("Working directory: " + workingDir);
            //proc = Runtime.getRuntime().exec(cmd, null, new File(workingDir));
            proc_submit_job=Runtime.getRuntime().exec(cmd, envp, new File(workingDir));
            // spawn new threads to write out stdout, and stderr
            stdoutThread = writeStdOut(proc_submit_job, workingDir, "stdout.txt");
            stderrThread = writeStdErr(proc_submit_job, workingDir, "stderr.txt");
        } catch (IOException ioe) {
            String msg = "Error while running executable via fork - " +
                ioe.getMessage();
            logger.error(msg);
            throw new JobManagerException(msg);
        }

        // update status to active
        status.setCode(GramJob.STATUS_PENDING);
        status.setMessage("Execution in progress");
        System.out.println("CSF Job Submitting.");
        
        // notify listeners that process is activated
        started = true;
        synchronized(this) {
            this.notifyAll();
        }

        // return an identifier for this process

	return proc_submit_job.toString();  
    }

    /**
     * Block until the job state is GramJob.STATUS_ACTIVE
     *
     * @return status for this job after blocking
     * @throws JobManagerException if there is an error while waiting for the job to be ACTIVE
     */
    public StatusOutputType waitForActivation() 
        throws JobManagerException {
        logger.info("called");

        // check if this process has been started already
        if (proc_submit_job == null) {
            String msg = "Can't wait for a process that hasn't be started";
            logger.error(msg);
            throw new JobManagerException(msg);
        }

        // poll till status is ACTIVE or ERROR
        while (!started) {
            try {
                synchronized(this) {
                    this.wait();
                }
            } catch (InterruptedException ie) {
                // minor exception - log exception and continue
                logger.error(ie.getMessage());
                continue;
            }
        }

        return status;
    }

    /**
     * Block until the job finishes executing
     *
     * @return final job status
     * @throws JobManagerException if there is an error while waiting for the job to finish
     */
    public StatusOutputType waitForCompletion() 
        throws JobManagerException {
        logger.info("called");

        // check if this process has been started already
        if (proc_submit_job == null) {
            String msg = "Can't wait for a process that hasn't be started";
            logger.error(msg);
            throw new JobManagerException(msg);
        }

        // wait till the process finishes
        int exitValue = 0;
        try {
            exitValue = proc_submit_job.waitFor();
        } catch (InterruptedException ie) {
            String msg = "Exception while waiting for process to finish";
            logger.error(msg, ie);
            throw new JobManagerException(msg + " - " + ie.getMessage());
        }

        // update status
        if (exitValue == 0) {
        	logger.debug("CSF Job Submitted.");
            status.setCode(GramJob.STATUS_ACTIVE);
            int pdone_exit=0;
            
            //check job status
            // run executable in the working directory
            try {
                // command line parameter
                FileInputStream fstream = new FileInputStream(wd+"/"+"stdout.txt");
                // Get the object of DataInputStream
                DataInputStream in = new DataInputStream(fstream);
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String strLine;
                //Read File Line By Line
                while ((strLine = br.readLine()) != null)   {
                  // Print the content on the console
                  if(strLine.startsWith("Query Job Status By csf-job-status")){
                            
                            String[] str_tokens = strLine.split(" ");
                            csf_job_id = str_tokens[str_tokens.length-1];
                            logger.debug ("csf_job_id:"+csf_job_id);
                    }
                }
                //Close the input stream
                in.close();
                
                String cmd=new String("csf-job-status"+" "+csf_job_id);
                
                logger.debug("Working directory: " + wd);
                
                while(pdone_exit==0){
                	
                	
                try{
                	Thread.sleep(10000);
                }catch (InterruptedException ie) {
                    // minor exception - log exception and continue                                                    
                    logger.error(ie.getMessage());
                    continue;
                }
                logger.debug("Check CSF Job Status.");
                
                proc_check_status=Runtime.getRuntime().exec(cmd, envp, new File(wd));
                //stdoutThread = writeStdOut(proc_check_status, wd, "csf_status_out.txt");
                //stderrThread = writeStdErr(proc_check_status, wd, "csf_status_err.txt");
                DataInputStream status_in = new DataInputStream(proc_check_status.getInputStream());
                BufferedReader status_br = new BufferedReader(new InputStreamReader(status_in));
                //Read File Line By Line
                BufferedWriter out=null;
                try {
                    out = new BufferedWriter(new FileWriter(wd+"csf_status_out.txt"));
                }catch(IOException e){
                    System.out.println("There was a problem:" + e);
                }
                while ((strLine = status_br.readLine()) != null)   {
                // Print the content on the console
                	logger.debug(strLine);
                	
                    try {
                            //out = new BufferedWriter(new FileWriter(wd+"csf_status_out.txt"));
                            out.write(strLine+"\n");//Write out the specfied string to the file
                    }catch(IOException e){
                            System.out.println("There was a problem:" + e);
                    }

                	if(strLine.indexOf("PDone")>=0 || strLine.indexOf("PExit")>=0|| strLine.indexOf("Run")>=0|| strLine.indexOf("Forwarded")>=0|| strLine.indexOf("Created")>=0|| strLine.indexOf("Pending")>=0|| strLine.indexOf("Unknown")>=0){
                		//System.out.println (strLine);           
                		String[] str_tokens = strLine.split(" ");
                		for(int i=0;i<str_tokens.length-1;i++){
                			if(str_tokens[i].equals("PDone")){
                				logger.debug(str_tokens[i]);
                				status.setCode(GramJob.STATUS_DONE);
                				logger.debug("CSF Job Status:PDone");
                                pdone_exit=1;
                				break;
                			}else if(str_tokens[i].equals("PExit")){
                				logger.debug(str_tokens[i]);
                				status.setCode(GramJob.STATUS_FAILED);
                				logger.debug("CSF Job Status:PExit");
                                pdone_exit=2;
                				break;
                			}else if(str_tokens[i].equals("Run")){
                				logger.debug(str_tokens[i]);
                				status.setCode(GramJob.STATUS_ACTIVE);
                				logger.debug("CSF Job Status:Run");
                                break;
                			}else if(str_tokens[i].equals("Forwarded")){
                				logger.debug(str_tokens[i]);
                				status.setCode(GramJob.STATUS_ACTIVE);
                				logger.debug("CSF Job Status:Forwarded");
                                break;
                			}else if(str_tokens[i].equals("Created")){
                				logger.debug(str_tokens[i]);
                				status.setCode(GramJob.STATUS_ACTIVE);
                				logger.debug("CSF Job Status:Created");
                                break;
                			}else if(str_tokens[i].equals("Unknown")){
                				logger.debug(str_tokens[i]);
                				status.setCode(GramJob.STATUS_PENDING);
                				logger.debug("CSF Job Status:Unknown");
                                break;
                			}
                		}
                    }
                }
                //Close the input stream
                in.close();
                out.close();//flushes and closes the stream

		proc_check_status.destroy();
                }
                
                
            } catch (IOException ioe) {
                String msg = "Error while running executable CSF - " +
                    ioe.getMessage();
                logger.error(msg);
                throw new JobManagerException(msg);
            }
            
            if(pdone_exit==1){
            	//Job complete
		proc_submit_job.destroy();
            	logger.debug("CSF Job Status:PDone, Job Completed");
                
            	status.setMessage("Execution complete - " + 
                              "check outputs to verify successful execution");
            }else {
		proc_submit_job.destroy();
            	logger.debug("CSF Job Status:PExit, Job failed");
                status.setMessage("Execution failed - process exited with value " +
                        "-1");
            }
        } else {
            status.setCode(GramJob.STATUS_FAILED);
            status.setMessage("Execution failed - process exited with value " +
                              exitValue);
        }

        // make sure all the threads are done
        done = true;
        try {
            logger.debug("Waiting for all outputs to be written out");
            stdoutThread.join();
            stderrThread.join();
            logger.debug("All outputs successfully written out");
        } catch (InterruptedException ignore) {}

        return status;
    }

    /**
     * Destroy this job
     * 
     * @return final job status
     * @throws JobManagerException if there is an error during job destruction
     */
    public StatusOutputType destroyJob()
        throws JobManagerException {
        logger.info("called");

        // check if this process has been started already
        if (proc == null) {
            String msg = "Can't destroy a process that hasn't be started";
            logger.error(msg);
            throw new JobManagerException(msg);
        }

        // destroy process
        proc.destroy();

        // update status
        status.setCode(GramJob.STATUS_FAILED);
        status.setMessage("Process destroyed on user request");

        return status;
    }

    /*
     * utility method to create a new thread to write stdout of a process
     */
    private Thread writeStdOut(Process p, String outputDirName, String outputFileName) {
        final File outputDir = new File(outputDirName);
        final InputStreamReader isr 
            = new InputStreamReader(p.getInputStream());
        final String outfileName =
            outputDir.getAbsolutePath() + File.separator + outputFileName;
        Thread t_input = new Thread() {
                public void run() {
                    FileWriter fw;
                    try {
                        fw = new FileWriter(outfileName);
                    } catch (IOException ioe) {
                        logger.error(ioe);
                        return; // can't do much if the file can't be opened
                    }
                    int bytes = 0;
                    char [] buf = new char[256];
                    while (!(done && (bytes < 0))) {
                        try {
                            bytes = isr.read(buf);
                            if (bytes > 0) {
                                fw.write(buf, 0, bytes);
                                fw.flush();
                            }
                        } catch (IOException ignore) {
                            break; // time to quit
                        }
                    }

                    try {
                        fw.close();
                    } catch (IOException ioe) {
                        logger.error(ioe);
                        return; // tough to send the exception back from another thread
                    }

                    logger.debug("Done writing standard output");
                }
            };
        t_input.start();
        return t_input;
    }

    /*
     * utility method to create a new thread to write stderr of a process
     */
    private Thread writeStdErr(Process p, String outputDirName, String outputFileName) {
        final File outputDir = new File(outputDirName);
        final InputStreamReader isr 
            = new InputStreamReader(p.getErrorStream());
        final String errfileName =
            outputDir.getAbsolutePath() + File.separator + outputFileName;
        Thread t_error = new Thread() {
                public void run() {
                    FileWriter fw;
                    try {
                        fw = new FileWriter(errfileName);
                    } catch (IOException ioe) {
                        logger.error(ioe);
                        return; // can't do much if the file can't be opened
                    }
                    int bytes = 0;
                    char [] buf = new char[256];
                    while (!(done && (bytes < 0))) {
                        try {
                            bytes = isr.read(buf);
                            if (bytes > 0) {
                                fw.write(buf, 0, bytes);
                                fw.flush();
                            }
                        } catch (IOException ignore) {
                            break; // time to quit
                        }
                    }

                    try {
                        fw.close();
                    } catch (IOException ioe) {
                        logger.error(ioe);
                        return; // tough to send the exception back from another thread
                    }

                    logger.debug("Done writing standard error");
                }
            };
        t_error.start();
        return t_error;
    }
}

