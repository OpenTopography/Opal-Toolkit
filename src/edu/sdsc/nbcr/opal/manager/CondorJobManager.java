package edu.sdsc.nbcr.opal.manager;

import java.util.Properties;
import java.util.Enumeration;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.globus.gram.GramJob;

import org.apache.log4j.Logger;

import edu.sdsc.nbcr.opal.manager.condorAPI.Condor;
import edu.sdsc.nbcr.opal.manager.condorAPI.Job;
import edu.sdsc.nbcr.opal.manager.condorAPI.Cluster;
import edu.sdsc.nbcr.opal.manager.condorAPI.JobDescription;
import edu.sdsc.nbcr.opal.manager.condorAPI.CondorException;

import edu.sdsc.nbcr.opal.AppConfigType;
import edu.sdsc.nbcr.opal.StatusOutputType;

/**
 *
 * Implementation of an Opal Job Manager using Condor
 */
public class CondorJobManager implements OpalJobManager {

    // get an instance of a log4j logger
    private static Logger logger = Logger.getLogger(CondorJobManager.class.getName());

    private Properties props; // the container properties being passed
    private AppConfigType config; // the application configuration
    private StatusOutputType status; // current status
    private String handle; // the Condor job id for this submission
    private boolean started = false; // whether the execution has started
    private volatile boolean done = false; // whether the execution is complete

    private Job job; // reference to the condor job
    private Condor condor; // reference to a condor instance

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
	logger.debug("called");

	this.props = props;
	this.config = config;
	this.handle = handle;

	// initialize status
	status = new StatusOutputType();

	// create a new Condor object
	condor = new Condor();
    }
    
    /**
     * General clean up, if need be 
     *
     * @throws JobManagerException if there is an error during destruction
     */
    public void destroyJobManager()
	throws JobManagerException {
	logger.debug("called");

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
	logger.debug("called");

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


	// launch executable using Condor
	String cmd = null;

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

	    // check if the mpi.script property is set
	    String mpiScript = props.getProperty("mpi.script");
	    if (mpiScript == null) {
		String msg = "Can't find property mpi.script for running parallel job";
		logger.error(msg);
		throw new JobManagerException(msg);
	    }

	    // create command string and arguments for parallel run
	    cmd = mpiScript;
	    
	    // append arguments - needs to be this way to locate machinefile
	    String newArgs = config.getBinaryLocation();
	    if ((args != null) && (!(args.equals("")))) {
		args = newArgs + " " + args;
	    } else {
		args = newArgs;
	    }
	    logger.debug("CMD: " + cmd + " " + args);
	} else {
	    // create command string and arguments for serial run
	    cmd = config.getBinaryLocation();
	    if (args == null)
		args = "";
	    logger.debug("CMD: " + cmd + " " + args);
	}

	// get the hard run limit
	long hardLimit = 0;
	if ((props.getProperty("opal.hard_limit") != null)) {
	    hardLimit = Long.parseLong(props.getProperty("opal.hard_limit"));
	    logger.debug("All jobs have a hard limit of "  + hardLimit + " seconds");
	}

	// launch the job using the above information
	try {
	    logger.debug("Working directory: " + workingDir);

	    // create a JobDescription object in the code
	    JobDescription jd = new JobDescription();
	    if (config.isParallel()) {
			jd.addAttribute("universe", "parallel");
			jd.addAttribute("machine_count", Integer.toString(numProcs));
	    } else {
			jd.addAttribute("universe", "vanilla");
	    }
	    jd.addAttribute("getenv", "true");
	    jd.addAttribute("executable", cmd);
	    jd.addAttribute("arguments", args);
	    jd.addAttribute("initialdir", workingDir);
	    jd.addAttribute("output", workingDir + "stdout.txt");
	    jd.addAttribute("error", workingDir + "stderr.txt");
	    jd.addAttribute("log", workingDir + "condor.log");
	    jd.addAttribute("log_xml", "true");
	    jd.addAttribute("should_transfer_files", "yes");
	    jd.addAttribute("when_to_transfer_output", "on_exit");
	    jd.addAttribute("notification", "never");

	    // transfer all input files - this way the compute nodes don't have to be on NFS
	    jd.addAttribute("transfer_input_files", config.getBinaryLocation());
	    File wd = new File(workingDir);
	    String[] wdFiles = wd.list();
	    if (wdFiles != null) {
			for (int i = 0; i < wdFiles.length; i++) {
		    	jd.addAttribute("transfer_input_files", 
				    workingDir + 
				    File.separator + 
				    wdFiles[i]);
			}
	    }

		// add server-specific condor expressions if exist
		String condorExprFileName = props.getProperty("condor.expr.file");
		if (condorExprFileName != null) {
			try {
				Properties condorProps = new Properties(); 
				FileInputStream in = new FileInputStream(condorExprFileName);
				condorProps.load(in);
				in.close();

				Enumeration keys;
				keys = condorProps.keys();
				while (keys.hasMoreElements()) {
					String condorKey = keys.nextElement().toString();
					String condorVal = condorProps.getProperty(condorKey);
					if (condorVal != null) {
						jd.addAttribute(condorKey,condorVal);
					}
				}
			} catch  (IOException e) {
				logger.warn("Failed to load " + condorExprFileName + "identified by condor.expr.file: " + e.getMessage());
			}
		}
	    jd.addQueue();

		condor.setLogFile(workingDir + "condor.log", 5);

	    Cluster cluster = condor.submit(jd);

	    job = cluster.getJob(0);
	    handle = job.getJobId().toString();
	    logger.info("Condor job id=" + handle + " has been submitted");
	} catch (Exception e) {
	    String msg = "Error while running executable via Condor - " +
		e.getMessage();
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}

	// job has been started
	started = true;

	// return an identifier for this process
	return handle;
    }

    /**
     * Block until the job state is GramJob.STATUS_ACTIVE
     *
     * @return status for this job after blocking
     * @throws JobManagerException if there is an error while waiting for the job to be ACTIVE
     */
    public StatusOutputType waitForActivation() 
	throws JobManagerException {
	logger.debug("called");

	// check if this process has been started already
	if (!started) {
	    String msg = "Can't wait for a process that hasn't be started";
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}

	// poll till status is RUNNING
	try {
	    job.waitForRun();
	} catch (CondorException e) {
	    String msg = "Can't wait for activation of Condor job: " + handle;
	    logger.error(msg, e);
	    throw new JobManagerException(msg + " - " + e.getMessage());
	}

	// update status to active
	status.setCode(GramJob.STATUS_ACTIVE);
	status.setMessage("Execution in progress");

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
	logger.debug("called");

	// check if this process has been started already
	if (!started) {
	    String msg = "Can't wait for a process that hasn't be started";
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}

	// wait till the process finishes, and get final status
	try {
	    job.waitFor();
	} catch (CondorException e) {
	    String msg = "Exception while waiting for process to finish";
	    logger.error(msg, e);
	    throw new JobManagerException(msg + " - " + e.getMessage());
	}

	// update status
	int exitValue = job.getStatus();

	if (exitValue == Job.COMPLETED) {
	    status.setCode(GramJob.STATUS_DONE);
	    status.setMessage("Execution complete - " + 
			      "check outputs to verify successful execution");
	} else {
	    status.setCode(GramJob.STATUS_FAILED);
	    status.setMessage("Execution failed - process exited with value " +
			      exitValue);
	}

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
	logger.debug("called");

	// check if this process has been started already
	if (!started) {
	    String msg = "Can't destroy a process that hasn't be started";
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}

	// destroy process
	try {
	    condor.rm(job);
	} catch (CondorException e) {
	    String msg = "Exception while trying to destroy condor job";
	    logger.error(msg, e);
	    throw new JobManagerException(msg + " - " + e.getMessage());
	}

	// update status
	status.setCode(GramJob.STATUS_FAILED);
	status.setMessage("Process destroyed on user request");

	return status;
    }
}
