package edu.sdsc.nbcr.opal.manager;

import java.util.Properties;

import java.io.File;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import org.globus.gram.GramJob;

import org.apache.log4j.Logger;

import edu.sdsc.nbcr.opal.AppConfigType;
import edu.sdsc.nbcr.opal.StatusOutputType;

import edu.sdsc.nbcr.opal.state.HibernateUtil;

/**
 *
 * Implementation of an Opal Job Manager using Process fork
 */
public class ForkJobManager implements OpalJobManager {

    // get an instance of a log4j logger
    private static Logger logger = Logger.getLogger(ForkJobManager.class.getName());

    private Properties props; // the container properties being passed
    private AppConfigType config; // the application configuration
    private Process proc; // the application process
    private StatusOutputType status; // current status
    private String handle; // the OS specific process id for this job
    private Thread stdoutThread; // the thread that writes stdout
    private Thread stderrThread; // the thread that writes stderr
    private boolean started = false; // whether the execution has started
    private volatile boolean done = false; // whether the execution is complete
    private final Object lock = new Object(); // an object for wait and notify

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
			    final String workingDir)
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

	// launch executable using process exec
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

	    // create command string for parallel run
	    String mpiRun = props.getProperty("mpi.run");
	    if (mpiRun == null) {
		String msg = "Can't find property mpi.run for running parallel job";
		logger.error(msg);
		throw new JobManagerException(msg);
	    }
	    cmd = new String(mpiRun + " " + "-np " + numProcs + " " +
			     config.getBinaryLocation());
	} else {
	    // create command string for serial run
	    cmd = new String(config.getBinaryLocation());
	}

	// append arguments
	if ((args != null) && (!(args.equals("")))) {
	    logger.debug("Appending arguments: " + args);
	    cmd += " " + args;
	}
	logger.debug("CMD: " + cmd);
	
	// run executable in the working directory, in a separate thread
	final String finalCmd = cmd;
	new Thread() {
	    public void run() {
		try {
		    executeJob(workingDir, finalCmd);
		} catch (JobManagerException jme) {
		    String msg =
			"Error while executing job: " + jme.getMessage();
		    logger.error(jme);

		    // update status to active
		    status.setCode(GramJob.STATUS_FAILED);
		    status.setMessage(msg);

		    // notify listeners that process is activated and failed
		    started = true;
		    synchronized(lock) {
			lock.notifyAll();
		    }
		}
	    }
	}.start();

	// return an identifier for this process
	if (proc != null) {
	    return proc.toString();
	} else {
	    return new String("Unavailable");
	}
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

	// poll till status is ACTIVE or ERROR
	while (!started) {
	    try {
		synchronized(lock) {
		    lock.wait();
		}
	    } catch (InterruptedException ie) {
		// minor exception - log exception and continue
		logger.error(ie.getMessage());
		continue;
	    }
	}

	// throw Exception if job failed during activation
	if (status.getCode() == GramJob.STATUS_FAILED) {
	    throw new JobManagerException(status.getMessage());
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
	logger.debug("called");

	// check if this process has been started already
	if (proc == null) {
	    String msg = "Can't wait for a process that hasn't be started";
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}

	// wait till the process finishes
	int exitValue = 0;
	try {
	    exitValue = proc.waitFor();
	} catch (InterruptedException ie) {
	    String msg = "Exception while waiting for process to finish";
	    logger.error(msg, ie);
	    throw new JobManagerException(msg + " - " + ie.getMessage());
	}

	// update status
	if (exitValue == 0) {
	    status.setCode(GramJob.STATUS_DONE);
	    status.setMessage("Execution complete - " + 
			      "check outputs to verify successful execution");
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
	logger.debug("called");

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

    /**
     * Method to wait for this jobs turn, and execute if appropriate
     */
    private void executeJob(String workingDir,
			    String cmd) 
	throws JobManagerException {
	try {
	    // check if job limit has been reached
	    if (props.getProperty("fork.jobs.limit") != null) {
		long jobLimit = Integer.parseInt(props.getProperty("fork.jobs.limit"));

		// wait, if need be
		while (true) {
		    // get number of jobs running from database
		    long runningJobs;
		    try {
			runningJobs = HibernateUtil.getNumExecutingJobs();
			logger.debug("Number of running jobs: " + runningJobs);
		    } catch (Exception e) {
			String msg = 
			    "Exception while retrieving number of jobs from database: " +
			    e.getMessage();
			logger.error(msg);
			throw new JobManagerException(msg);
		    }

		    // sleep 10 seconds, if limit has been reached
		    if (runningJobs >= jobLimit) {
			try {
			    logger.debug("Waiting for number of Fork jobs to fall below limit");
			    Thread.sleep(10000);
			} catch (Exception e) {
			    // log exception, and continue
			    logger.warn(e);
			}
		    } else {
			break;
		    }
		}
	    }

	    logger.debug("Working directory: " + workingDir);
	    proc = Runtime.getRuntime().exec(cmd, null, new File(workingDir));
	    
	    // spawn new threads to write out stdout, and stderr
	    stdoutThread = writeStdOut(proc, workingDir);
	    stderrThread = writeStdErr(proc, workingDir);
	} catch (IOException ioe) {
	    String msg = "Error while running executable via fork - " +
		ioe.getMessage();
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}
	
	// update status to active
	status.setCode(GramJob.STATUS_ACTIVE);
	status.setMessage("Execution in progress");

	// notify listeners that process is activated
	started = true;
	synchronized(lock) {
	    lock.notifyAll();
	}
    }

    /*
     * utility method to create a new thread to write stdout of a process
     */
    private Thread writeStdOut(Process p, String outputDirName) {
	final File outputDir = new File(outputDirName);
	final InputStreamReader isr 
	    = new InputStreamReader(p.getInputStream());
	final String outfileName =
	    outputDir.getAbsolutePath() + File.separator + "stdout.txt";
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
    private Thread writeStdErr(Process p, String outputDirName) {
	final File outputDir = new File(outputDirName);
	final InputStreamReader isr 
	    = new InputStreamReader(p.getErrorStream());
	final String errfileName =
	    outputDir.getAbsolutePath() + File.separator + "stderr.txt";
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
