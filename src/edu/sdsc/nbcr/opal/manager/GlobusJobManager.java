package edu.sdsc.nbcr.opal.manager;

import java.util.Properties;

import org.globus.axis.gsi.GSIConstants;
import org.globus.gram.Gram;
import org.globus.gram.GramJob;
import org.globus.gram.GramJobListener;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;

import org.apache.log4j.Logger;

import edu.sdsc.nbcr.opal.AppConfigType;
import edu.sdsc.nbcr.opal.StatusOutputType;

/**
 *
 * Implementation of an Opal Job Manager using Globus
 */
public class GlobusJobManager implements OpalJobManager, GramJobListener {

    // get an instance of a log4j logger
    private static Logger logger = Logger.getLogger(GlobusJobManager.class.getName());

    protected Properties props; // the container properties being passed
    protected AppConfigType config; // the application configuration
    protected StatusOutputType status; // current status
    protected String handle; // the OS specific process id for this job
    protected boolean started = false; // whether the execution has started
    protected boolean active = false; // whether the job is active already
    protected boolean destroyed = false; // whether the process was destroyed

    protected GramJob job; // the GramJob object for this run

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


	// launch executable using Globus
	String rsl = null;
	
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

	    // create RSL for parallel run
	    rsl =
		"&(directory=" + workingDir + ")" + 
		"(executable=" + config.getBinaryLocation() + ")" + 
		"(count=" + numProcs + ")" +
		"(jobtype=mpi)" +
		"(stdout=stdout.txt)" + 
		"(stderr=stderr.txt)";
	} else {
	    // create RSL for serial run
	    rsl = 
		"&(directory=" + workingDir + ")" + 
		"(executable=" + config.getBinaryLocation() + ")" + 
		"(stdout=stdout.txt)" + 
		"(stderr=stderr.txt)";
	}

	// set the hard run limit, if it exists
	long hardLimit = 0;
	if ((props.getProperty("opal.hard_limit") != null)) {
	    hardLimit = Long.parseLong(props.getProperty("opal.hard_limit")) / 60;
	    logger.info("All jobs have a hard limit of "  + hardLimit + " minutes");
            //what should we use here? maxWallTime maxCpuTime or maxTime?
            //for the moment we go for maxWallTime on most TG sites work
	    rsl += "(maxWallTime=" + hardLimit + ")";
	}
	
	// add arguments to the RSL
	if ((args != null) && (!(args.equals("")))) {
	    // put every argument within quotes - needed by Globus if some of 
	    // the arguments are of the form name=value
	    args = "\"" + args + "\"";
	    args = args.replaceAll("[\\s]+", "\" \"");
	    rsl += "(arguments=" + args + ")";
	}

	logger.debug("RSL: " + rsl);

	// get the service cert and key
	String serviceCertPath = props.getProperty("globus.service_cert");
	if (serviceCertPath == null) {
	    String msg = "Can't find property: globus.service_cert";
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}
	String serviceKeyPath = props.getProperty("globus.service_privkey");
	if (serviceKeyPath == null) {
	    String msg = "Can't find property: globus.service_privkey";
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}

	// get the location of the Globus gatekeeper
	String gatekeeperContact = props.getProperty("globus.gatekeeper");
	if (config.getGlobusGatekeeper() != null) {
	    gatekeeperContact = config.getGlobusGatekeeper().toString();
	}
	if (gatekeeperContact == null) {
	    String msg = "Can't find property: globus.gatekeeper";
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}

	// execute the job using GRAM
	try {
	    job = new GramJob(rsl);	    

	    // create credentials from service certificate/key
	    GlobusCredential globusCred = 
		new GlobusCredential(serviceCertPath, 
				     serviceKeyPath);
	    GSSCredential gssCred = 
		new GlobusGSSCredentialImpl(globusCred,
					    GSSCredential.INITIATE_AND_ACCEPT);
	    
	    // set the credentials for the job
	    job.setCredentials(gssCred);
	    
	    // execute the globus job
	    job.request(gatekeeperContact);

	    // add a listener
	    job.addListener(this);

	    // set the handle 
	    handle = job.getIDAsString();
	} catch (Exception e) {
	    String msg = "Error while running executable via Globus - " +
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
	logger.info("called");

	// wait till the job is active
	while (!active) {
	    try {
		synchronized(job) {
		    // will be notified once the job is active
		    job.wait();
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
	if (!started) {
	    String msg = "Can't wait for a process that hasn't be started";
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}

	// wait till the process finishes, and get final status
	try {
	    while (!((status.getCode() == GramJob.STATUS_FAILED) || 
		     (status.getCode() == GramJob.STATUS_DONE))) {
		synchronized(job) {
		    job.wait();
		}
	    }
	} catch (Exception e) {
	    String msg = "Exception while waiting for Globus process to finish";
	    logger.error(msg, e);
	    throw new JobManagerException(msg + " - " + e.getMessage());
	}

	// return status
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
	if (!started) {
	    String msg = "Can't destroy a process that hasn't be started";
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}

	// destroy process
	try {
	    job.cancel();
	    destroyed = true;
	} catch (Exception e) {
	    String msg = "Exception while trying to destroy Globus process";
	    logger.error(msg, e);
	    throw new JobManagerException(msg + " - " + e.getMessage());
	}

	// update status
	status.setCode(GramJob.STATUS_FAILED);
	status.setMessage("Process destroyed on user request");

	return status;
    }

    /**
     * Implementation of the method defined by the GramJobListener interface, which is
     * invoked by the Globus JobManager if the job status is updated
     * 
     * @param job reference to the Globus GRAM job representing this job
     */
    public void statusChanged(GramJob job) {
	logger.info("called for job: " + handle);
	
	// get the job status code and message
	int code = job.getStatus();
	String message;
	if (code == GramJob.STATUS_DONE) {
	    message = GramJob.getStatusAsString(code) +
		" - check outputs to verify successful execution";
	} else if (code == GramJob.STATUS_FAILED) {
	    message = GramJob.getStatusAsString(code) + 
		", Error code - " + job.getError();
	} else {
	    message = GramJob.getStatusAsString(code);
	}
	logger.info("Job status: " + message);

	// update job status
	status.setCode(code);
	if (!destroyed) {
	    // don't update message if this has been explicitly destroyed
	    status.setMessage(message);
	}
	
	// if the status just changed to active or failed, notify sleepers
	if (!active) {
	    if ((code == GramJob.STATUS_ACTIVE) ||
		(code == GramJob.STATUS_DONE) ||
		(code == GramJob.STATUS_FAILED)) {
		active = true;
		synchronized(job) {
		    job.notifyAll();
		}
	    }
	}

	// if done, deactivate listener, which gets rid of a GRAM thread
	if ((code == GramJob.STATUS_DONE) ||
	    (code == GramJob.STATUS_FAILED)) {
	    job.removeListener(this);
	    Gram.deactivateCallbackHandler(job.getCredentials());
	}

	// notify sleepers that the job is finished
	if ((code == GramJob.STATUS_FAILED) || (code == GramJob.STATUS_DONE)) {
	    logger.info("Job " + handle + " finished with status - " + message);
	    synchronized(job) {
		job.notifyAll();
	    }
	}
    }
}
