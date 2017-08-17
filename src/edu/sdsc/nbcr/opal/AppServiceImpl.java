package edu.sdsc.nbcr.opal;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;

import javax.activation.DataHandler;

import java.net.URL;
import java.net.URLConnection;

import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Random;

import javax.activation.FileDataSource;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;

import org.apache.axis.types.URI;
import org.apache.axis.types.URI.MalformedURIException;
import org.apache.axis.MessageContext;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.description.ServiceDesc;

import java.sql.PreparedStatement;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import org.globus.gram.GramJob;

import edu.sdsc.nbcr.common.TypeDeserializer;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import edu.sdsc.nbcr.opal.manager.OpalJobManager;
import edu.sdsc.nbcr.opal.manager.OpalJobManagerFactory;
import edu.sdsc.nbcr.opal.manager.JobManagerException;

import edu.sdsc.nbcr.opal.state.JobInfo;
import edu.sdsc.nbcr.opal.state.JobOutput;
import edu.sdsc.nbcr.opal.state.OutputFile;
import edu.sdsc.nbcr.opal.state.HibernateUtil;
import edu.sdsc.nbcr.opal.state.StateManagerException;

import edu.sdsc.nbcr.opal.util.Util;
import edu.sdsc.nbcr.opal.util.ArgValidator;	
import edu.sdsc.nbcr.opal.util.Extract;

import org.apache.commons.io.FileUtils;

/**
 *
 * Implementation of the AppServicePortType, which represents every
 * Opal service
 *
 * @author Sriram Krishnan
 */

public class AppServiceImpl 
    implements AppServicePortType {

    // get an instance of the log4j Logger
    private static Logger logger = 
	Logger.getLogger(AppServiceImpl.class.getName());

    /** Location of tomcat installation */
    private static String catalinaHome; 
    private static String outputPrefix; // the location of the webapps/ROOT

    /** URL for tomcat installation */
    private static String tomcatURL;

    /** Whether to zip up data after completion */
    private static boolean archiveData;

    /**
     * The hash table that stores the references to the job managers
     */
    private static Hashtable jobTable = new Hashtable();

    /** The fully qualified class name of the job manager */
    private static String jobManagerFQCN;

    /** 
     * The properties for processing of per IP job limits
     */
    private static boolean ipProcessing;
    private static long ipLimit;
    private static String[] blackListIP;
    private static String[] whiteListIP;

    /**
     * The properties for allowing certain prefixes in command-line
     */
    private static String allowedDirsString;
    private static String[] allowedDirs;

    // the configuration information for the application
    private String serviceName;
    private AppConfigType config;
    private File configFile;
    private long lastModified;

    // random number generator
    Random rand = new Random();

    // containier properties - initialize only once
    private static Properties props;
    static {
	props = new Properties();
	String propsFileName = "opal.properties";
	try {
	    props.load(AppServiceImpl.class.getClassLoader().
		       getResourceAsStream(propsFileName));
	} catch (IOException ioe) {
	    logger.fatal("Failed to load opal.properties");
	}

	tomcatURL = props.getProperty("tomcat.url") + "/";
	catalinaHome = System.getProperty("catalina.home");
	outputPrefix = 	    
	    catalinaHome + File.separator + "webapps" + File.separator;
	String workingDir = props.getProperty("working.dir");
	if (workingDir != null) {
	    outputPrefix += workingDir;
	    if (tomcatURL != null) {
		tomcatURL += workingDir + "/";
	    }
	} else {
	    outputPrefix += "ROOT";
	}

	// traverse symbolic links, if need be
	try {
	    File prefixDir = new File(outputPrefix);
	    outputPrefix = prefixDir.getCanonicalPath();
	} catch (IOException e) {
	    logger.fatal(e);
	}

	// whether to archive data
	archiveData =
	    Boolean.valueOf(props.getProperty("data.archive")).booleanValue();
	if (archiveData) {
	    logger.info("Data will be available as archive after job completion");
	}

	// clean up zombie jobs
	try {
	    logger.debug("Checking if there are any zombie jobs");

	    int numUpdates = HibernateUtil.markZombieJobs();

	    logger.debug("Number of DB entries for zombie jobs cleaned up: " + numUpdates);
	} catch (StateManagerException se) {
	    logger.fatal("Caught exception while trying to clean database", se);
	}

	// get the FQCN for the job manager
	jobManagerFQCN = props.getProperty("opal.jobmanager");
	if (jobManagerFQCN == null) {
	    logger.fatal("Required property not set - opal.jobmanager");
	}

	// retrieve all the relevant properties for IP processing
	ipProcessing = 
	    Boolean.valueOf(props.getProperty("opal.ip.processing")).booleanValue();

	// return true if processing is not set up
	if (!ipProcessing) {
	    logger.debug("Upper limits per IP not turned on");
	} else {
	    // get the limit per hour
	    String ipLimitString = props.getProperty("opal.ip.limit");
	    if (ipLimitString == null) {
		logger.fatal("Unable to find a limit for number of jobs per IP");
		ipLimit = 0;
	    } else {
		ipLimit = Long.parseLong(ipLimitString);
		logger.debug("Number of jobs per IP per hour: " + ipLimit);
	    }

	    // get the black list of IP addresses
	    String blackListString = props.getProperty("opal.ip.blacklist");
	    if (blackListString != null) {
		blackListIP = blackListString.split("\\s*,\\s*");
	    } else {
		blackListIP = new String[0];
	    }
	    
	    // get the white list of IP addresses
	    String whiteListString = props.getProperty("opal.ip.whitelist");
	    if (whiteListString != null) {
		whiteListIP = whiteListString.split("\\s*,\\s*");
	    } else {
		whiteListIP = new String[0];
	    }
	}

	// process command-line path prefixes
	allowedDirsString = props.getProperty("allowed.path.prefixes");
	if (allowedDirsString != null) {
	    allowedDirs = allowedDirsString.split("\\s*,\\s*");
	} else {
	    allowedDirs = new String[0];
	}
    }

    
    /**
     * Method to access the tomcat base URL from other classes...
     */
    static public String getOpalBaseURL(){
        try {
            //I need to remove the possible output direcotry 
            java.net.URI uri = new java.net.URI(tomcatURL);
            //there's a better way to do this but for now it works
            return uri.resolve("/").toString() + "opal2";
        }catch (Exception e ){
            //this should never happen
            return null;
        }
    }

    /**
     * Default constructor
     *
     * @throws FaultType if there is an error during initialization
     */
    public AppServiceImpl() 
	throws FaultType {
	logger.debug("called");

	if (tomcatURL == null) {
	    logger.fatal("Can't find property: tomcatURL");
	    throw new FaultType("Can't find property: tomcatURL");
	}
	if (catalinaHome == null) {
	    logger.fatal("Can't find property: catalina.home");
	    throw new FaultType("Can't find property: catalina.home");
	}
    }

    //-------------------------------------------------------------//
    // Implementation of the methods inside the AppServicePortType //
    //-------------------------------------------------------------//

    /**
     * Get the metadata for this service
     * 
     * @param in dummy object representing doc-literal input parameter
     * @return application metadata, as specified by the WSDL
     * @throws FaultType if there is an error during retrieval of application metadata
     */    
    public AppMetadataType getAppMetadata(AppMetadataInputType in) 
	throws FaultType {
	logger.debug("called");

	// make sure that the config has been retrieved
	retrieveAppConfig();

	// return the metadata
	return config.getMetadata();
    }

    /**
     * Get the application configuration for this service
     * 
     * @param in dummy object representing doc-literal input parameter
     * @return application configuration, as specified by the WSDL
     * @throws FaultType if there is an error during retrieval of application configuration
     */
    public AppConfigType getAppConfig(AppConfigInputType in)
	throws FaultType {
	logger.debug("called");

	// make sure that the config has been retrieved
	retrieveAppConfig();

	// return the config
	return config;
    }

    /**
     * Get the system information for this service
     * 
     * @param in dummy object representing doc-literal input parameter
     * @return system information for this service
     * @throws FaultType if there is an error during retrieval of system information
     */
    public SystemInfoType getSystemInfo(SystemInfoInputType in) 
	throws FaultType {
	logger.info("called");

	// fill the business logic for this call
	SystemInfoType result = new SystemInfoType();

	// set job manager name
	String className = "edu.sdsc.nbcr.opal.manager.";
	int start = className.length();
	int end = jobManagerFQCN.indexOf("JobManager");
	String jobManagerType = jobManagerFQCN.substring(start, end);
	result.setJobManagerType(jobManagerType);

	// set user data lifetime
	String dataLifeTime = props.getProperty("opal.datalifetime");
	if (dataLifeTime == null) {
	    dataLifeTime = "4 days";
	    logger.info("SystemInfoType: Using opal.datalifetime=4 days");
	}
	result.setDataLifetime(dataLifeTime);

	// set hard limit
	int hardLimit = 0;
	if ((props.getProperty("opal.hard_limit") != null)) {
	     hardLimit = Integer.parseInt(props.getProperty("opal.hard_limit")) ;
	     logger.info("SystemInfoType: Using hard limit "  + hardLimit + " seconds");
	}
	result.setHardLimit(hardLimit);

	// set total CPU number 
	int numCpuTotal = 1;
        if ((props.getProperty("num.procs") != null)) {
	    numCpuTotal = Integer.parseInt(props.getProperty("num.procs")) ;
	    logger.info("SystemInfoType: Using num.procs = " + numCpuTotal );
	}
	result.setNumCpuTotal(numCpuTotal);

	// get number of currently excuting jobs 
	long numJobsExec = 0; 
	try {
	   numJobsExec =  HibernateUtil.getNumExecutingJobs();
	} catch (StateManagerException sme) {
	    String msg = sme.getMessage();
	    logger.error(msg);
	    throw new FaultType(msg);
	}
	result.setNumJobsRunning((int)numJobsExec);

	// get number of pending jobs 
	long numJobsPending = 0; 
	try {
	   numJobsExec =  HibernateUtil.getNumPendingJobs();
	} catch (StateManagerException sme) {
	    String msg = sme.getMessage();
	    logger.error(msg);
	    throw new FaultType(msg);
	}
	result.setNumJobsQueued((int)numJobsPending);

	// set number of available CPUs
	int numCpuFree = numCpuTotal - (int)numJobsExec - (int)numJobsPending;
	if (numCpuFree < 0) 
		numCpuFree = 0;
	result.setNumCpuFree(numCpuFree);

	return result;
    }


    /**
     * Launch a job on behalf of the user, using the given arguments
     *
     * @param in the input object, as defined by the WSDL, which contains the command-line
     * arguments, list of input files in Base64 encoded form, and a the number of processes
     * for parallel jobs
     * @return job submission output, as defined by the WSDL, which contains the <i>jobID</i>, 
     * and the initial job status
     * @throws FaultType if there is an error during job submission
     */
    public JobSubOutputType launchJob(JobInputType in)
	throws FaultType {
	long t0 = System.currentTimeMillis();
	logger.debug("called");

	// check to see if IP is within limits
	isWithinIPLimits();

	// make sure that the config has been retrieved
	retrieveAppConfig();

	// write the input files, and launch the job in a non-blocking fashion
	String jobID = launchApp(in, false);

	// create output object
	JobSubOutputType output = new JobSubOutputType();
	output.setJobID(jobID);
	logger.info("Launching job: " + jobID);
	StatusOutputType status = queryStatus(jobID);
	output.setStatus(status);

	long t1 = System.currentTimeMillis();
	logger.debug("Server execution time: " + (t1-t0) + " ms");
	return output;
    }

    /**
     * Launch a job on behalf of the user, using the given arguments, and block till it
     * finishes
     *
     * @param in the input object, as defined by the WSDL, which contains the command-line
     * arguments, list of input files in Base64 encoded form, and a the number of processes
     * for parallel jobs
     * @return job output, as defined by the WSDL, which contains the final job status
     * and output metadata
     * @throws FaultType if there is an error during job submission
     */
    public BlockingOutputType launchJobBlocking(JobInputType in)
	throws FaultType {
	long t0 = System.currentTimeMillis();
	logger.info("called");

	// check to see if IP is within limits
	isWithinIPLimits();

	// make sure that the config has been retrieved
	retrieveAppConfig();

	// write the input files, and launch the job in a blocking fashion
	String jobID = launchApp(in, true);

	// create output object
	BlockingOutputType output = new BlockingOutputType();
	StatusOutputType status = queryStatus(jobID);
	output.setStatus(status);
	JobOutputType jobOut = getOutputs(jobID);
	output.setJobOut(jobOut);

	long t1 = System.currentTimeMillis();
	logger.debug("Server execution time: " + (t1-t0) + " ms");
	return output;
    }

    /** 
     * Query job status for the given jobID
     * 
     * @param in the <i>jobID</i> for which to query status
     * @return the status, as described by the WSDL
     * @throws FaultType if there is an error during the status query
     */
    public StatusOutputType queryStatus(String in) 
	throws FaultType {
	long t0 = System.currentTimeMillis();
	logger.debug("Query status for job: " + in);
	
	// make sure that the config has been retrieved
	// retrieveAppConfig();

	// retrieve the status
	StatusOutputType status = null;

	try {
	    status = HibernateUtil.getStatus(in);
	} catch (StateManagerException se) {
	    logger.error(se.getMessage());
	    throw new FaultType(se.getMessage());
	}

	long t1 = System.currentTimeMillis();
	logger.debug("Query execution time: " + (t1-t0) + " ms");
	return status;
    }

    /** 
     * Query job statistics for the given jobID
     * 
     * @param in the <i>jobID</i> for which to query status
     * @return the statistics, as described by the WSDL
     * @throws FaultType if there is an error during the status query
     */
    public JobStatisticsType getJobStatistics(String in) 
	throws FaultType {
	long t0 = System.currentTimeMillis();
	logger.info("called for job: " + in);
	
	// make sure that the config has been retrieved
	// retrieveAppConfig();

	// retrieve the statistics
	JobStatisticsType stats = null;

	try {
	    stats = HibernateUtil.getStatistics(in);
	} catch (StateManagerException se) {
	    logger.error(se.getMessage());
	    throw new FaultType(se.getMessage());
	}

	long t1 = System.currentTimeMillis();
	logger.debug("Query execution time: " + (t1-t0) + " ms");
	return stats;
    }

    /**
     * Return output metadata for a particular job run
     * 
     * @param in <i>jobID</i> for a particular run
     * @return an object, as defined by the WSDL, which contains links to the 
     * standard output and error, and a list of output files
     * @throws FaultType if there is an error in output retrieval
     */
    public JobOutputType getOutputs(String in) 
	throws FaultType {
	long t0 = System.currentTimeMillis();
	logger.info("called for job: " + in);
	
	// make sure that the config has been retrieved
	// retrieveAppConfig();

	// retrieve the outputs
	JobOutputType outputs = null;
	
	try {
	    outputs = HibernateUtil.getOutputs(in);
	} catch (StateManagerException se) {
	    logger.error(se.getMessage());
	    throw new FaultType(se.getMessage());
	}

	// make sure the outputs still exist
	File test = new File(outputPrefix + File.separator + in);
	if (!test.exists()) {
	    logger.error("Job outputs have been cleaned up");
	    throw new FaultType("Job outputs have been cleaned up");
	}

	long t1 = System.currentTimeMillis();
	logger.debug("Output retrieval time: " + (t1-t0) + " ms");
	return outputs;
    }

    /**
     * Return a Base64 encoded output file for a particular run
     * 
     * @param in input object, as defined by the WSDL, which contains a 
     * <i>jobID</i> and a <i>fileName</i>
     * @return output file in Base64 encoded binary form
     * @throws FaultType if there is an error in output generation
     */
    public byte[] getOutputAsBase64ByName(OutputsByNameInputType in) 
	throws FaultType {
	long t0 = System.currentTimeMillis();
	logger.info("called for job: " + in.getJobID() + 
		    " with file name: " + in.getFileName());

	byte[] data = null;
	String outputDirName = 
	    outputPrefix + File.separator + in.getJobID() + File.separator;
	String outputURL = outputDirName + in.getFileName();
	File f = new File(outputURL);
	if (f.exists()) {
	    try {
		data = new byte[(int) f.length()];
		FileInputStream fIn = new FileInputStream(f);
		fIn.read(data);
		fIn.close();
	    } catch (Exception e) {
		logger.error("Error while trying to read output: " + e.getMessage());
		throw new FaultType("Error while trying to read output: " + e.getMessage());
	    }

	} else {
	    logger.error("File " + in.getFileName() + " doesn't exist on server");
	    throw new FaultType("File " + in.getFileName() + " doesn't exist on server");
	}
	long t1 = System.currentTimeMillis();
	logger.debug("Output retrieval time: " + (t1-t0) + " ms");
	return data;
    }

    /**
     * Destroy job representing this jobID
     *
     * @param in the jobID for this job
     * @return the final status for this job
     * @throws FaultType if there is an error during job destruction
     */
    public StatusOutputType destroy(String in) 
	throws FaultType {
	long t0 = System.currentTimeMillis();
	logger.info("called for job: " + in);

	// initialize output
	StatusOutputType status = null;

	// check to see if it is still running
	if (jobTable.containsKey(in)) {
	    // retrieve the job manager from the jobTable
	    OpalJobManager jobManager =
		(OpalJobManager) jobTable.get(in);

	    // destroy the job, and wait until it is done
	    try {
		status = jobManager.destroyJob();
	    } catch (JobManagerException jme) {
		String msg = jme.getMessage();
		logger.error(msg);
		throw new FaultType(msg);
	    }
	} else {
	    // retrieve status for a possible finished job
	    status = queryStatus(in);
	}

	// make sure baseURL is not empty
	if (status.getBaseURL() == null) {
	    try {
		URI baseURL = new URI(tomcatURL + in);
		status.setBaseURL(baseURL);
	    } catch (Exception e) {
		String message = "Exception while trying to construct base URL";
		logger.error(message);
		throw new FaultType(message);
	    }
	}

	long t1 = System.currentTimeMillis();
	logger.debug("Destruction time: " + (t1 - t0) + " ms");
	return status;
    }

    //--------------------------------------------------------------------//
    //    Private helper methods used by the above public impl methods    //
    //--------------------------------------------------------------------//

    private String launchApp(JobInputType in,
			     boolean blocking)
	throws FaultType {

	// create a working directory where it can be accessible
	final String jobID = 
	    "app" + serviceName + System.currentTimeMillis() + rand.nextInt();
	final String outputDirName = 
	    outputPrefix + File.separator + jobID + File.separator;
	final File outputDir = new File(outputDirName);
	if (!outputDir.mkdir()) {
	    logger.error("Can't create new directory to run application in" + outputDir);
	    throw new FaultType("Can't create new directory to run application in" + outputDir);
	}

	// create the application input files there 
	writeAppInput(in, outputDirName);

	// make sure that the arguments don't refer to any parent directories
	String args = in.getArgList();
	if (args != null) {
	    StringTokenizer argTokens = new StringTokenizer(args);
	    while (argTokens.hasMoreTokens()) {
		String next = argTokens.nextToken();

		// must not begin with "/" or "~", unless allowed
		if (next.startsWith(File.separator) ||
		    (next.startsWith("~"))) {
		    
		    // some dirs are allowed
		    boolean allowed = false;
		    for (int i = 0; i < allowedDirs.length; i++) {
			// logger.debug("Comparing command-line arg with: " +
			//	     allowedDirs[i]);
			if (next.startsWith(allowedDirs[i])) {
			    allowed = true;
			    break;
			}
		    }
		    
		    if (!allowed) {
			String msg = "Arguments are not allowed to begin with: " +
			    File.separator + " or " + "~";
			logger.error(msg);
			throw new FaultType(msg);
		    }
		}

		// must not include ".."
		if (next.indexOf("..") != -1) {
		    String msg = "Arguments are not allowed to include: ..";
		    logger.error(msg);
		    throw new FaultType(msg);
		}
	    }
	}

	// validate command-line arguments, if need be
	if (config.getValidateArgs() != null) {
	    if (config.getValidateArgs().booleanValue()) {
		logger.info("Validating command-line arguments");
		
		ArgumentsType argsDesc = config.getMetadata().getTypes();
		if (argsDesc == null) {
		    String msg = "Validation of arguments requested - " +
			"but argument schema not provided within app config";
		    logger.error(msg);
		    throw new FaultType(msg);
		}
		
		ArgValidator av = new ArgValidator(argsDesc);
		
		boolean success = av.validateArgList(outputDirName, 
						     in.getArgList());
		if (success) {
		    logger.info("Argument validation successful");
		} else {
		    String msg = "Argument validation unsuccessful";
		    logger.error(msg);
		    throw new FaultType(msg);
		}
	    }
	}

	// create a new status object and save it
	StatusOutputType status = new StatusOutputType();
	status.setCode(GramJob.STATUS_PENDING);
	status.setMessage("Launching executable");
	final URI baseURL;
	try {
	    baseURL = new URI(tomcatURL + jobID);
	    status.setBaseURL(baseURL);
	} catch (Exception e) {
	    String message = "Exception while trying to construct base URL";
	    logger.error(message);
	    throw new FaultType(message);
	}

	// initialize the job information in database
	final JobInfo info = new JobInfo();
	info.setJobID(jobID);
	info.setCode(status.getCode());
	info.setMessage(status.getMessage());
	info.setBaseURL(status.getBaseURL().toString());
	Date currentDate = new Date();
	info.setStartTimeDate(new java.sql.Date(currentDate.getTime()));
	info.setStartTimeTime(new java.sql.Time(currentDate.getTime()));
	info.setLastUpdateDate(new java.sql.Date(currentDate.getTime()));
	info.setLastUpdateTime(new java.sql.Time(currentDate.getTime()));
	info.setClientDN(Util.getRemoteDN());
	info.setClientIP(Util.getRemoteIP());
	info.setServiceName(serviceName);
	final String userEmail = in.getUserEmail();
	if (userEmail != null) {
	    logger.debug("User email for notification: " + userEmail);
	    info.setUserEmail(userEmail);
	} else {
	    info.setUserEmail("Unknown");
	}
	try {
	    HibernateUtil.saveJobInfoInDatabase(info);
	} catch (StateManagerException se) {
	    logger.error(se.getMessage());
	    throw new FaultType(se.getMessage());
	}

	// instantiate & initialize the job manager
	String jobManagerFQCNLocal = jobManagerFQCN;
	if (config.getJobManagerFQCN() != null) {
	    // if the app config has a job manager FQCN, use that
	    jobManagerFQCNLocal = config.getJobManagerFQCN();
	} 
	logger.debug("Using job manager class: " + jobManagerFQCNLocal);

	final OpalJobManager jobManager;
	try {
	    jobManager = OpalJobManagerFactory.getOpalJobManager(jobManagerFQCNLocal);
	} catch (JobManagerException jme) {
	    logger.error(jme.getMessage());

	    // save status in database
	    try {
		HibernateUtil.updateJobInfoInDatabase(jobID,
						      GramJob.STATUS_FAILED,
						      jme.getMessage(),
						      info.getBaseURL(),
						      null);
	    } catch (StateManagerException se) {
		logger.error(se.getMessage());
		throw new FaultType(se.getMessage());
	    }

	    throw new FaultType(jme.getMessage());
	}

	try {
	    jobManager.initialize(props, config, null);
	} catch (JobManagerException jme) {
	    logger.error(jme.getMessage());

	    // save status in database
	    try {
		HibernateUtil.updateJobInfoInDatabase(jobID,
						      GramJob.STATUS_FAILED,
						      jme.getMessage(),
						      info.getBaseURL(),
						      null);
	    } catch (StateManagerException se) {
		logger.error(se.getMessage());
		throw new FaultType(se.getMessage());
	    }

	    throw new FaultType(jme.getMessage());
	}

	// launch job with the given arguments
	final String handle;
	try {
	    handle = jobManager.launchJob(in.getArgList(), 
					  in.getNumProcs(),
					  outputDirName);
	} catch (JobManagerException jme) {
	    logger.error(jme.getMessage());

	    // save status in database
	    try {
		HibernateUtil.updateJobInfoInDatabase(jobID,
						      GramJob.STATUS_FAILED,
						      jme.getMessage(),
						      info.getBaseURL(),
						      null);
	    } catch (StateManagerException se) {
		logger.error(se.getMessage());
		throw new FaultType(se.getMessage());
	    }

	    throw new FaultType(jme.getMessage());
	}

	// add this jobLaunchUtil into the jobTable
	jobTable.put(jobID, jobManager);

	final Boolean sendNotification = in.getSendNotification();
	if (!blocking) {
	    // launch thread to monitor status
	    new Thread() {
		public void run() {
		    try {
			manageJob(jobManager, 
				  jobID, 
				  outputDirName, 
				  baseURL, 
				  handle);
		    } catch (FaultType f) {
			// status is logged, not much else to do here
			logger.error(f);
		    }

		    // after finishing, notify user if need be
		    if (sendNotification != null) {
			if (sendNotification) {
			    if (userEmail != null) {
				emailStatus(userEmail, jobID);
			    } else {
				logger.error("Can't send email to user as email is NULL");
			    }
			}
		    }
		}
	    }.start();
	} else {
	    try {
		// monitor status in the same thread
		manageJob(jobManager, jobID, outputDirName, baseURL, handle);
	    } catch (FaultType f) {
		// after finishing, notify user if need be
		if (sendNotification != null) {
		    if (sendNotification) {
			if (userEmail != null) {
			    emailStatus(userEmail, jobID);
			} else {
			    logger.error("Can't send email to user as email is NULL");
			}
		    }
		}
		// rethrow the exception
		throw f;
	    }

	    // no exceptions - notify user if need be
	    if (sendNotification != null) {
		if (sendNotification) {
		    if (userEmail != null) {
			emailStatus(userEmail, jobID);
		    } else {
			logger.error("Can't send email to user as email is NULL");
		    }		
		}    
	    }
	}

	// return the jobID
	return jobID;
    }

    private void manageJob(OpalJobManager jobManager,
			   String jobID,
			   String workingDir,
			   URI baseURL,
			   String handle)
	throws FaultType {

	// wait for job activation
	StatusOutputType status = null;
	try {
	    status = jobManager.waitForActivation();
	} catch (JobManagerException jme) {
	    logger.error(jme.getMessage());

	    // save status in database
	    try {
		HibernateUtil.updateJobInfoInDatabase(jobID,
						      GramJob.STATUS_FAILED,
						      jme.getMessage(),
						      baseURL.toString(),
						      handle);
	    } catch (StateManagerException se) {
		logger.error(se.getMessage());
		throw new FaultType(se.getMessage());
	    }

	    throw new FaultType(jme.getMessage());
	}

	if (status.getBaseURL() == null)
	    status.setBaseURL(baseURL);

	// update status in database
	try {
	    HibernateUtil.updateJobInfoInDatabase(jobID, 
						  status.getCode(),
						  status.getMessage(),
						  status.getBaseURL().toString(),
						  new Date(), // activation time
						  null,
						  handle);
	} catch (StateManagerException se) {
	    logger.error(se.getMessage());
	    throw new FaultType(se.getMessage());
	}

	// if the job is still running, wait for it to finish
	try {
	    status = jobManager.waitForCompletion();
	} catch (JobManagerException jme) {
	    logger.error(jme.getMessage());
	    
	    // save status in database
	    try {
		HibernateUtil.updateJobInfoInDatabase(jobID,
						      GramJob.STATUS_FAILED,
						      jme.getMessage(),
						      status.getBaseURL().toString(),
						      handle);
	    } catch (StateManagerException se) {
		logger.error(se.getMessage());
		throw new FaultType(se.getMessage());
	    }
	    
	    throw new FaultType(jme.getMessage());
	}

	if (status.getBaseURL() == null)
	    status.setBaseURL(baseURL);
    
	// bit of a hack because execution completion does not
	// equal completion of Web service call
	int jobCode = status.getCode();
	String jobMessage = status.getMessage();
	status.setCode(GramJob.STATUS_STAGE_OUT);
	status.setMessage("Writing output metadata");

	// update status in database
	try {
	    HibernateUtil.updateJobInfoInDatabase(jobID, 
						  status.getCode(),
						  status.getMessage(),
						  status.getBaseURL().toString(),
						  handle);
	} catch (StateManagerException se) {
	    logger.error(se.getMessage());
	    throw new FaultType(se.getMessage());
	}

	// retrieve job outputs
	// make sure the stdout and stderr exist
	File stdOutFile = new File(workingDir + File.separator + "stdout.txt");
	if (!stdOutFile.exists()) {
	    String msg = "Standard output file " + stdOutFile + " is missing";
	    logger.error(msg);
	    status.setCode(GramJob.STATUS_FAILED);
	    status.setMessage(msg);

	    // update status in database
	    try {
		HibernateUtil.updateJobInfoInDatabase(jobID, 
						      status.getCode(),
						      status.getMessage(),
						      status.getBaseURL().toString(),
						      handle);
	    } catch (StateManagerException se) {
		logger.error(se.getMessage());
		throw new FaultType(se.getMessage());
	    }

	    jobTable.remove(jobID);
	    return;
	}
	File stdErrFile = new File(workingDir + File.separator + "stderr.txt");
	if (!stdErrFile.exists()) {
	    String msg = "Standard error file " + stdErrFile + " is missing";
	    logger.error(msg);
	    status.setCode(GramJob.STATUS_FAILED);
	    status.setMessage(msg);

	    // update status in database
	    try {
		HibernateUtil.updateJobInfoInDatabase(jobID, 
						      status.getCode(),
						      status.getMessage(),
						      status.getBaseURL().toString(),
						      handle);
	    } catch (StateManagerException se) {
		logger.error(se.getMessage());
		throw new FaultType(se.getMessage());
	    }

	    jobTable.remove(jobID);
	    return;
	}
	
	// archive the outputs, if need be
	if (archiveData) {
	    logger.debug("Archiving output files");

	    // get a list of files
	    File f = new File(workingDir);
	    File [] outputFiles = getAllOutputs(workingDir);
	    
	    // Create a buffer for reading the files
	    byte[] buf = new byte[1024];
	    
	    try {

		ZipOutputStream out = 
		    new ZipOutputStream(new FileOutputStream(workingDir + 
							     File.separator + 
							     jobID + 
							     ".zip"));
		
		// Compress the files
		for (int i = 0; i < outputFiles.length; i++) {
		    FileInputStream in = new FileInputStream(outputFiles[i]);
		    
		    // Add ZIP entry to output stream.
		    String absolutePath = outputFiles[i].getPath();
		    int start = absolutePath.indexOf(jobID);
		    String relativePath = 
			absolutePath.substring(start + jobID.length() + 1);
		    out.putNextEntry(new ZipEntry(relativePath));
		    
		    // Transfer bytes from the file to the ZIP file
		    int len;
		    while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		    }
		    
		    // Complete the entry
		    out.closeEntry();
		    in.close();
		}
		
		// Complete the ZIP file
		out.close();
	    } catch (IOException e) {
		logger.error(e);
		logger.error("Error not fatal - moving on");
	    }
	}
	
	// at least 2 files exist now - stdout and stderr
	JobOutputType outputs = new JobOutputType();

	try {
	    File f = new File(workingDir);
	    File[] outputFiles = getAllOutputs(workingDir);
	    int count = 0;
	    OutputFileType [] outputFileObj = null;

	    for (int i = 0; i < outputFiles.length; i++) 
		if (outputFiles[i].getAbsolutePath().equals(stdOutFile.getAbsolutePath()) || 
		    outputFiles[i].getAbsolutePath().equals(stdErrFile.getAbsolutePath()))
		    count++;
	   
	    if (count == 2) 
		outputFileObj = new OutputFileType[outputFiles.length-2];
	    else if (count == 1) 
		outputFileObj = new OutputFileType[outputFiles.length-1];
	    else if (count == 0)
		outputFileObj = new OutputFileType[outputFiles.length];

	    int j = 0;
	    for (int i = 0; i < outputFiles.length; i++) {
		if (outputFiles[i].getAbsolutePath().equals(stdOutFile.getAbsolutePath())) {
		    outputs.setStdOut(new URI(tomcatURL +
					      jobID + 
					      "/stdout.txt"));
		}
		else if (outputFiles[i].getAbsolutePath().equals(stdErrFile.getAbsolutePath())) {
		    outputs.setStdErr(new URI(tomcatURL +
					      jobID + 
					      "/stderr.txt"));
		}
		else {
		    // NOTE: all input files will also be duplicated here
		    OutputFileType next = new OutputFileType();
		    //next.setName(outputFiles[i].getName());
		    String absolutePath = outputFiles[i].getPath();
		    int start = absolutePath.indexOf(jobID);
		    String relativePath = 
			absolutePath.substring(start + jobID.length() + 1);

		    next.setName(relativePath);

		    next.setUrl(new URI(tomcatURL +
					jobID +
					"/" +
					relativePath));

		    outputFileObj[j++] = next;
		}
	    }

	    outputs.setOutputFile(outputFileObj);

	    // update the outputs table
	    try {
		HibernateUtil.saveOutputsInDatabase(jobID, outputs);
	    } catch (StateManagerException se) {
		logger.error(se.getMessage());
		throw new FaultType(se.getMessage());
            }
	} catch (IOException e) {
	    // log exception
	    logger.error(e);

	    // set status to FAILED
	    status.setCode(GramJob.STATUS_FAILED);
	    status.setMessage("Cannot retrieve outputs after finish - " +
			      e.getMessage());

	    // finish up

	    // update status in database
	    try {
		HibernateUtil.updateJobInfoInDatabase(jobID, 
						      status.getCode(),
						      status.getMessage(),
						      status.getBaseURL().toString(),
						      handle);
	    } catch (StateManagerException se) {
		logger.error(se.getMessage());
		throw new FaultType(se.getMessage());
	    }

	    jobTable.remove(jobID);

	    return;
	}

	// update final status
	status.setCode(jobCode);
	status.setMessage(jobMessage);
	
	// update status in database
	try {
	    HibernateUtil.updateJobInfoInDatabase(jobID, 
						  status.getCode(),
						  status.getMessage(),
						  status.getBaseURL().toString(),
						  null,
						  new Date(), // completion time
						  handle);
	} catch (StateManagerException se) {
	    logger.error(se.getMessage());
	    throw new FaultType(se.getMessage());
	}

	// get rid of the jobManager from the jobTable
	jobTable.remove(jobID);

	logger.info("Execution complete for job: " + jobID);
    }

    private void writeAppInput(JobInputType in,
			       String outputDirName) 
	throws FaultType {
	logger.debug("called");

	// retrieve the list of files
	InputFileType[] inputFiles = in.getInputFile();
	if (inputFiles == null) {
	    // no files to be written
	    logger.debug("No input files to be written out");
	    return;
	}

	// write the input files into the working directory
	logger.debug("Number of input files to be written out: " + inputFiles.length);

	for (int i = 0; i < inputFiles.length; i++) {
	    // make sure the contents are supplied
	    if ((inputFiles[i].getContents() == null) &&
		(inputFiles[i].getLocation() == null) &&
		(inputFiles[i].getAttachment() == null)) {
		String msg = "File contents, URL and attachment are missing - " +
		    "one of them should be provided";
		logger.error(msg);
		throw new FaultType(msg);
	    }

	    // call utility method to write input files
	    writeInputFile(outputDirName, inputFiles[i], in.getExtractInputs());
	}
    }

    private void writeInputFile(String outputDirName, 
				InputFileType inputFile,
				Boolean extractInputs) 
	throws FaultType {
	logger.debug("called for file: " + inputFile.getName());

	try {
	    File f = new File(outputDirName, inputFile.getName());
	    BufferedOutputStream out = null;
	    if (inputFile.getContents() != null) {
		//it is a 'normal' file
		out = new BufferedOutputStream(new FileOutputStream(f));
		out.write(inputFile.getContents());
		out.close();
	    } else if (inputFile.getLocation() != null) {
		//it is a URL
		int index = inputFile.getLocation().toString().indexOf(":");
		if (index == -1) {
		    String msg = "Can't find protocol for URL: " + 
			inputFile.getLocation();
		    logger.error(msg);
		    throw new FaultType(msg);
		}
                    
		out = new BufferedOutputStream(new FileOutputStream(f));
		String protocol = inputFile.getLocation().toString().substring(0, index);
		logger.info("Using protocol: " + protocol);

		if (protocol.equals("http") ||
		    protocol.equals("https")) {
		    // fetch the file from the URL
		    URL url = new URL(inputFile.getLocation().toString());
		    URLConnection conn = url.openConnection();
		    InputStream input = conn.getInputStream();
		    byte[] buffer = new byte[1024];
		    int numRead;
		    long numWritten = 0;
		    while ((numRead = input.read(buffer)) != -1) {
			out.write(buffer, 0, numRead);
			numWritten += numRead;
		    }
		    logger.debug(numWritten + " bytes written from url: " +
				 inputFile.getLocation());
		    input.close();
		    out.close();
		} else {
		    String msg = "Unsupported protocol: " + protocol;
		    logger.error(msg);
		    throw new FaultType(msg);
		}
	    } else { 
		// it is an attachment
		DataHandler dh = inputFile.getAttachment();
                logger.debug("Received attachment: " + dh.getName());
                // first check to see if the DataHandler wraps a FileDataSource
                // as we can use OS operations to move the file
                if (dh.getDataSource() instanceof FileDataSource) {
                    File attachFile = ((FileDataSource)dh.getDataSource()).getFile();
                    logger.debug("Source is " + attachFile.toString() +
                            " and dest is " + f.toString());
                    try {
                        // Note that you can not use a try/catch for renaming problems
                        // as exceptions are only thrown when the SecurityManager
                        // denies access or when the destination file is null (which is isn't).
                        if (!attachFile.renameTo(f)) {
 		            String msg = "Unable to copy attachment correctly: " +
 			            dh.getName();
 		            logger.error(msg);
 		            throw new FaultType(msg);
                        }
                    } catch (SecurityException se) {
                        String msg = "SecurityException while trying to rename input file: " +
                                se.getMessage();
                        logger.error(msg);
                        throw new FaultType(msg);
                    }
                } else {
                    // treat as an unknown DataSource
                    logger.debug("Source is " + dh.getName() +
                            " and dest is " + f.toString() + " (unknow data source)");
                    try {
                        out = new BufferedOutputStream(new FileOutputStream(f));
                        dh.writeTo(out);
                        out.close(); out = null;
                    } finally {
                        if (out != null) {
                            try { out.close(); }
                            catch (IOException e) { /* ignore */ }
                        }
                    }
                }
	    }

	    // extract files if need be
	    if (extractInputs != null) {
		if (extractInputs) {
		    logger.debug("Trying to extract file: " + f.getName());
		    Extract ex = new Extract();
		    ex.extract(outputDirName,
			       f.getAbsolutePath());

		    // delete file after unzipping
		    f.delete();
		}
	    }
	} catch (IOException ioe) {
	    logger.error("IOException while trying to write input file: " + 
			 ioe.getMessage());
	    throw new FaultType("IOException while trying to write input file: " + 
				ioe.getMessage());
	}
    }

    private void retrieveAppConfig()
	throws FaultType {
	logger.debug("called");

	// read location of config file
	MessageContext mc = MessageContext.getCurrentContext();
	SOAPService service = mc.getService();
	serviceName = service.getName();
	if (serviceName == null) {
	    serviceName = "Unknown service";
	}

	String configFileName = (String) service.getOption("appConfig");
	if (configFileName == null) {
	    logger.error("Required parameter appConfig not found in WSDD");
	    throw new FaultType("Required parameter appConfig not found in WSDD");
	}
	    
	// read the config file if it is not set, or if has been modified
	boolean reconfigure = false;
	if (configFile == null) {
	    configFile = new File(configFileName);
	    lastModified = configFile.lastModified();
	}
	long newLastModified = configFile.lastModified();
	if (newLastModified > lastModified) {
	    reconfigure = true;
	    lastModified = newLastModified;
	    logger.info("Application config modified recently -- reconfiguring");
	}
	if (config == null) {
	    reconfigure = true;
	    logger.debug("Configuring service for the first time");
	}

	if (reconfigure) {
	    logger.info("Reading application config: " + configFileName);

	    try {
		config = 
		    (AppConfigType) TypeDeserializer.getValue(configFileName,
							      new AppConfigType());;
	    } catch (Exception e) {
		logger.error(e);
		String msg = "Can't read application configuration from XML for service: " +
		    serviceName;
		logger.error(msg);
		throw new FaultType(msg);	    
	    }
	}
    }

    private File [] getAllOutputs(String workingDir) 
	throws FaultType {

	FileFilter fileFilter = new FileFilter() {
		public boolean accept(File file) {
		    boolean a = true;

		    try {
			a = file.canRead() && file.getAbsolutePath().equals(file.getCanonicalPath());
		    } catch (IOException e) {
			logger.error(e);
		    }
		    
		    return a;
		}
	    };

	File wd = new File(workingDir);
	File [] top_files = wd.listFiles(fileFilter);
	Stack checks = new Stack();
	Stack ofs = new Stack();
	
	for (int i = 0; i < top_files.length; i++) 
	    checks.push(top_files[i]);

	while (!checks.empty()) {
	    File temp_file = (File)checks.pop();

	    if (temp_file.isDirectory()) {
		File [] dir_files = temp_file.listFiles(fileFilter);
		
		for (int i = 0; i < dir_files.length; i++) 
		    checks.push(dir_files[i]);
	    }
	    else
		ofs.push(temp_file);	
	}

	int numFiles = ofs.size();
	File [] outputFiles = new File[numFiles];
	int c = 0;
	
	while (!ofs.empty()) {
	    File of  = (File)ofs.pop();
	    outputFiles[c] = of;
	    c++;
	}

	return outputFiles;
    }


    /* 
     * returns "true" if IP processing is off
     * returns "true" if processing is on and {IP is in whitelist OR number of
       jobs during that hour from that IP is less than limit}
     * throws exception if IP is in blacklist OR number of jobs during that
       hour from that IP is greater than limit (with appropriate message)
    */
    private boolean isWithinIPLimits() 
	throws FaultType {

	logger.debug("called");

	// return true if processing is not set up
	if (!ipProcessing) {
	    return true;
	}

	// get the remote IP to start processing
	String remoteIP = Util.getRemoteIP();
	logger.debug("Request received from IP: " + remoteIP);

	// throw exception if IP is in blacklist
	for (int i = 0; i < blackListIP.length; i++) {
	    if (remoteIP.equals(blackListIP[i])) {
		String msg = "Remote IP " + remoteIP + " found in blacklist";
		logger.error(msg);
		throw new FaultType(msg);
	    }
	}

	// return true if IP is in whitelist 
	for (int i = 0; i < whiteListIP.length; i++) {
	    if (remoteIP.equals(whiteListIP[i])) {
		logger.debug("Remote IP " + remoteIP + " found in whitelist");
		return true;
	    }
	}

	// calculate number of jobs per hour from this IP
	long numJobsIP = 0; 
	try {
	   numJobsIP =  HibernateUtil.getNumJobsThisHour(remoteIP);
	} catch (StateManagerException sme) {
	    String msg = sme.getMessage();
	    logger.error(msg);
	    throw new FaultType(msg);
	}

	// return true if numJobs per hour from IP is less than limit
	if (numJobsIP < ipLimit) {
	    logger.debug("Number of jobs (" + numJobsIP + ") for client (" + 
			 remoteIP + ") is within limit");
	    return true;
	} else {
	    // TODO: should log IP in a database to monitor abuse
	    String msg = "Number of jobs (" + numJobsIP + ") for client (" + 
		remoteIP + ") at maximum limit (" + ipLimit + " per hour)";
	    logger.error(msg);
	    throw new FaultType(msg);
	}
    }

    /**
     * Emails the user with the status of job
     * Doesn't throw any exception, only logs it
     */
    private void emailStatus(String userEmail, String jobID) {
	try {
	    logger.info("called");

	    boolean emailEnabled = 
		Boolean.valueOf(props.getProperty("mail.enable")).booleanValue();
	    if (!emailEnabled) {
		logger.warn("Opal server is not enabled to send email to user");
		return;
	    }

	    StatusOutputType status = queryStatus(jobID);

	    // set the connection properties
	    Properties mailProps = new Properties();
	    
	    // get the mail server
	    String mailServer = props.getProperty("mail.smtp.host");
	    if (mailServer == null) {
		logger.error("No mail server specified in opal.properties");
		return;
	    }
	    mailProps.put("mail.smtp.host", mailServer);

	    // check whether authentication should be turned on
	    Authenticator auth = null;
	    String enableAuth = props.getProperty("mail.smtp.auth");
            //TODO fix this code
            //if ((enableAuth != null) && (enableAuth.equals("true"))) {
	    if ((enableAuth != null) && (enableAuth.equals("true"))) {
		mailProps.put("mail.smtp.starttls.enable",
			      enableAuth);
		mailProps.put("mail.smtp.auth", 
			      enableAuth);


		String userName = props.getProperty("mail.smtp.user");
		String password = props.getProperty("mail.smtp.password");

		if ((userName == null) || (password == null)) {
		    logger.error("Username/password for SMTP server is null");
		    return;
		}
		auth = new SMTPAuthenticator(userName,
					     password);
	    }

	    // get the default Session using above properties
	    Session session = Session.getDefaultInstance(mailProps, 
							 auth);
	    Boolean debug = Boolean.parseBoolean(props.getProperty("mail.smtp.debug"));
	    session.setDebug(debug);

	    // create a message
	    Message msg = new MimeMessage(session);
	    
	    // set the from and to address
	    String userFrom = props.getProperty("mail.smtp.from");
	    if (userFrom == null) {
		logger.error("Can't find a FROM email address in the opal.properties");
		return;
	    }

	    InternetAddress addressFrom = new InternetAddress(userFrom);
	    msg.setFrom(addressFrom);
	    
	    InternetAddress[] addressTo = new InternetAddress[1]; 
	    addressTo[0] = new InternetAddress(userEmail);

	    msg.setRecipients(Message.RecipientType.TO, addressTo);

	    // Optional : You can also set your custom headers in the Email if you Want
	    // msg.addHeader("MyHeaderName", "myHeaderValue");
	    
	    // Setting the Subject and Content Type
	    msg.setSubject("Results for Opal job: " + jobID);
	    String body = "Your Opal job is complete\n\n" +
		"Job ID: " + jobID + "\n" +
		"Status code: " + status.getCode() + "\n" +
		"Message: " + status.getMessage() + "\n" +
		"Output Base URL: " + status.getBaseURL() + "\n";
	    msg.setContent(body, "text/plain");
	    
	    Transport.send(msg);
	} catch (Exception e) {
	    // logger.error(e);
	    String msg = "Exception caught while trying to email user: " +
		e.getMessage();
	    logger.error(msg);
	}
    }

    /**
     * SimpleAuthenticator is used to do simple authentication
     * when the SMTP server requires it.
     */
    private class SMTPAuthenticator extends Authenticator {
	private String username;
	private String password;

	/**
	 * Default constructor
	 */
	SMTPAuthenticator(String username, String password) {
	    this.username = username;
	    this.password = password;
	}

	/**
	 * Return a PasswordAuthentication object based on username/password
	 */
	public PasswordAuthentication getPasswordAuthentication() {
	    return new PasswordAuthentication(username, password);
	}
    }
}
