package edu.sdsc.nbcr.opal.state;

import org.hibernate.Session;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.HibernateException;

import org.hibernate.criterion.Restrictions;

import org.apache.log4j.Logger;

import java.util.Date;
import java.util.Calendar;
import java.util.List;
import java.util.Iterator;
import java.util.TimeZone;

import org.globus.gram.GramJob;

import edu.sdsc.nbcr.opal.StatusOutputType;
import edu.sdsc.nbcr.opal.JobStatisticsType;
import edu.sdsc.nbcr.opal.JobOutputType;
import edu.sdsc.nbcr.opal.OutputFileType;

import org.apache.axis.types.URI;
import org.apache.axis.types.URI.MalformedURIException;

import edu.sdsc.nbcr.opal.dashboard.util.DateHelper;

/**
 *
 * Utility class for hibernate functions
 *
 * @author Sriram Krishnan
 */

public class HibernateUtil {

    // get an instance of the log4j Logger
    private static Logger logger = 
        Logger.getLogger(HibernateUtil.class.getName());

    private static SessionFactory sessionFactory = null;
    //default for the hiberante configuration file 
    private static String confFile = "hibernate-opal.cfg.xml";

    private static SessionFactory buildSessionFactory()  {
        try {
            // Create the SessionFactory from hibernate.cfg.xml
            Configuration conf = new Configuration();
            conf = conf.configure(confFile);
            SessionFactory sessionFactoryTemp = conf.buildSessionFactory();
            return sessionFactoryTemp;
        } catch (HibernateException ex) {
            // Make sure you log the exception, as it might be swallowed
            logger.error("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Set the configuration file path for hibernate
     */
    public static void setConfFile(String conf){
        confFile = conf;
    }

    /**
     * Return a hibernate session factory loaded from the configuration
     */
    public static SessionFactory getSessionFactory() {
        if ( sessionFactory == null ) {
            //let's initialize the sessionFactory
            sessionFactory = buildSessionFactory();
        }
        return sessionFactory;
    }

    /**
     * Saves the job information into the hibernate database
     *
     * @param info the job information object to be saved
     * @return true if job is saved successfully
     * @throws StateManagerException if there is an error during the database commit
     */
    public static boolean saveJobInfoInDatabase(JobInfo info)
        throws StateManagerException {
	logger.debug("called");

	try {
	    Session session = getSessionFactory().openSession();
	    session.beginTransaction();
	    session.save(info);
	    session.getTransaction().commit();
	    session.close();
	} catch (HibernateException ex) {
	    String msg = "Error during database update: " + ex.getMessage();
	    logger.error(msg);
	    throw new StateManagerException(msg);
	}

	return true;
    }

    /**
     * Marks all jobs currently active as zombies - useful during startup
     */
    public static int markZombieJobs() 
        throws StateManagerException {
	logger.debug("called");

	try {
	    Session session = getSessionFactory().openSession();
	    session.beginTransaction();
	    Date lastUpdate = new Date();
	    int numUpdates = 
		session.createQuery("update JobInfo info " +
				    "set info.lastUpdateDate = :lastUpdateDate, " +
				    "info.lastUpdateTime = :lastUpdateTime, " + 
				    "info.code = :code, " +
				    "info.message = :message " +
				    "where info.code != " + GramJob.STATUS_DONE + 
				    "and info.code != " + GramJob.STATUS_FAILED)
		.setDate("lastUpdateDate", lastUpdate)
		.setTime("lastUpdateTime", lastUpdate)
		.setInteger("code", GramJob.STATUS_FAILED)
		.setString("message", 
			   "Job failed - server was restarted during job execution")
		.executeUpdate();
	    session.getTransaction().commit();
	    session.close();

	    return numUpdates;
	} catch (HibernateException ex) {
	    String msg = "Error during database update: " + ex.getMessage();
	    logger.error(msg);
	    throw new StateManagerException(msg);
	}
    }

    /**
     * Update the job info for a job already in the database
     * 
     * @param jobID the job id for this job
     * @param code the status code for this job
     * @param message the status message for this job
     * @param baseURL the base URL for this job
     * @param handle the manager specific handle to communicate with the job
     * @return number of rows updated
     * @throws StateManagerException if there is an error during the database commit
     */
    public static int updateJobInfoInDatabase(String jobID, int code, String message,
					      String baseURL, String handle) throws StateManagerException {
	logger.debug("called");
	return updateJobInfoInDatabase(jobID, code, message, baseURL, null, null, handle);
    }

    /**
     * Update the job info for a job already in the database
     * 
     * @param jobID the job id for this job
     * @param code the status code for this job
     * @param message the status message for this job
     * @param baseURL the base URL for this job
     * @param activationTime the time the job was activated, if not null
     * @param completionTime the time the job was completed, if not null
     * @param handle the manager specific handle to communicate with the job
     * @return number of rows updated
     * @throws StateManagerException if there is an error during the database commit
     */
    public static int updateJobInfoInDatabase(String jobID,
					      int code,
					      String message,
					      String baseURL,
					      Date activationTime,
					      Date completionTime,
					      String handle)
        throws StateManagerException {
	logger.debug("called");
	logger.debug("Updating status to: " + message);

	int numRows = 1;
	try {
	    Session session = getSessionFactory().openSession();
	    session.beginTransaction();
	    Date lastUpdate = new Date();
	    String queryString = "update JobInfo info " +
		"set info.lastUpdateDate = :lastUpdateDate, " +
		"info.lastUpdateTime = :lastUpdateTime, " + 
		"info.code = :code, " +
		"info.message = :message, " +
		"info.baseURL = :baseURL, ";
	    if (activationTime != null) {
		queryString += "info.activationTimeTime = :activationTimeTime, ";
		queryString += "info.activationTimeDate = :activationTimeDate, ";
	    }
	    if (completionTime != null) {
		queryString += "info.completionTimeTime = :completionTimeTime, ";
		queryString += "info.completionTimeDate = :completionTimeDate, ";
	    }
	    queryString +=
		"info.handle = :handle " +
		"where info.jobID = '" +
		jobID + "'";

	    Query query = session.createQuery(queryString);
	    query.setDate("lastUpdateDate", lastUpdate)
		.setTime("lastUpdateTime", lastUpdate)
		.setInteger("code", code)
		.setString("message", message)
		.setString("baseURL", baseURL)
		.setString("handle", handle);
	    if (activationTime != null) {
		query.setDate("activationTimeDate", activationTime);
		query.setTime("activationTimeTime", activationTime);
	    }
	    if (completionTime != null) {
		query.setDate("completionTimeDate", completionTime);
		query.setTime("completionTimeTime", completionTime);
	    }
	    numRows = query.executeUpdate();
	    session.getTransaction().commit();
	    session.close();
	} catch (HibernateException ex) {
	    String msg = "Error during database update: " + ex.getMessage();
	    logger.error(msg);
	    throw new StateManagerException(msg);
	}

	if (numRows == 1) {
	    logger.debug("Updated status for job: " + jobID);
	} else {
	    String msg = "Unable to update status for job: " + jobID;
	    logger.error(msg);
	    throw new StateManagerException(msg);
	}

	return numRows;
    }

    /**
     * Saves the job information into the hibernate database
     *
     * @param jobID the job id for this job
     * @param outputs job outputs for this job
     * @return true if output is saved successfully
     * @throws StateManagerException if there is an error during the database commit
     */
    public static boolean saveOutputsInDatabase(String jobID,
						JobOutputType outputs)
        throws StateManagerException {
	logger.debug("called");

	try {
	    Session session = getSessionFactory().openSession();
	    session.beginTransaction();

	    // retrieve the job info object
	    List results = session.createCriteria(JobInfo.class)
		.add(Restrictions.eq("jobID", jobID))
		.list();
	    if (results.size() != 1) {
		session.close();
		throw new StateManagerException("Can't find job info for job: " + jobID);
	    }
	    JobInfo info = (JobInfo) results.get(0);

	    // initialize job outputs
	    JobOutput out = new JobOutput();
	    out.setJob(info);
	    out.setStdOut(outputs.getStdOut().toString());
	    out.setStdErr(outputs.getStdErr().toString());

	    // initialize the output files
	    OutputFile files[] = new OutputFile[outputs.getOutputFile().length];
	    for (int i = 0; i < outputs.getOutputFile().length; i++) {
		// initialize output files
		files[i] = new OutputFile();
		files[i].setJob(info);
		files[i].setName(outputs.getOutputFile()[i].getName());
		files[i].setUrl(outputs.getOutputFile()[i].getUrl().toString());
	    }

	    // save the outputs
	    session.save(out);
	    for (int i = 0; i < files.length; i++) {
		session.save(files[i]);
	    }
	    session.getTransaction().commit();
	    session.close();
	} catch (HibernateException ex) {
	    String msg = "Error during database update: " + ex.getMessage();
	    logger.error(msg);
	    throw new StateManagerException(msg);
	}

	return true;
    }

    /**
     * Retrieves job status from the database using the jobID
     *
     * @param jobID the job id for this job
     * @return the status for this job
     * @throws StateManagerException if there is an error during status retrieval
     */
    public static StatusOutputType getStatus(String jobID) 
        throws StateManagerException {
	logger.debug("called");

	StatusOutputType status = null;

	try {
	    // retrieve job status from hibernate
	    Session session = getSessionFactory().openSession();
	    List results = session.createCriteria(JobInfo.class)
		.add(Restrictions.eq("jobID", jobID))
		.list();
	    if (results.size() == 1) {
		JobInfo info = (JobInfo) results.get(0);
		status = new StatusOutputType();
		status.setCode(info.getCode());
		status.setMessage(info.getMessage());
		try {
		    status.setBaseURL(new URI(info.getBaseURL()));
		} catch (MalformedURIException e) {
		    // log and contiue
		    logger.error(e.getMessage());
		}
	    }
	    session.close();
	} catch (HibernateException ex) {
	    String msg = "Error while getting status from database: " + ex.getMessage();
	    logger.error(msg);
	    throw new StateManagerException(msg);
	}

	if (status == null) {
	    String msg = "Can't retrieve status for job: " + jobID;
	    logger.error(msg);
	    throw new StateManagerException(msg);
	}

	return status;
    }

    /**
     * Retrieves job statistics from the database using the jobID
     *
     * @param jobID the job id for this job
     * @return the statistics for this job
     * @throws StateManagerException if there is an error during status retrieval
     */
    public static JobStatisticsType getStatistics(String jobID) 
        throws StateManagerException {
	logger.debug("called");

	JobStatisticsType stats = null;

	try {
	    // retrieve job status from hibernate
	    Session session = getSessionFactory().openSession();
	    List results = session.createCriteria(JobInfo.class)
		.add(Restrictions.eq("jobID", jobID))
		.list();
	    if (results.size() == 1) {
		JobInfo info = (JobInfo) results.get(0);
		stats = new JobStatisticsType();
		Calendar startTime = Calendar.getInstance();
		//we need to reset the TimeZone lost in the database
		TimeZone tz = TimeZone.getDefault();
		//long dateOffset = tz.getOffset( (new Date()).getTime() ); this doesn't work if there is daylightsaving
		long dateOffsetRaw = tz.getRawOffset();
		startTime.setTimeInMillis( info.getStartTimeTime().getTime() +  
					   info.getStartTimeDate().getTime() + 
					   dateOffsetRaw);
		stats.setStartTime(startTime);
		if ( (info.getActivationTimeTime() != null) && 
		     (info.getActivationTimeDate() != null) ) {
		    Calendar activationTime = Calendar.getInstance();
		    activationTime.setTimeInMillis(info.getActivationTimeTime().getTime() +  
						   info.getActivationTimeDate().getTime() + 
						   dateOffsetRaw);
		    stats.setActivationTime(activationTime);
		}
		if ((info.getCompletionTimeTime() != null) && 
		    (info.getCompletionTimeDate() != null)) {
		    Calendar completionTime = Calendar.getInstance();
		    completionTime.setTimeInMillis(info.getCompletionTimeTime().getTime() +  
						   info.getCompletionTimeDate().getTime() + 
						   dateOffsetRaw);
		    stats.setCompletionTime(completionTime);
		}
	    }
	    session.close();
	} catch (HibernateException ex) {
	    String msg = "Error while getting statistics from database: " + ex.getMessage();
	    logger.error(msg);
	    throw new StateManagerException(msg);
	}

	if (stats == null) {
	    String msg = "Can't retrieve statistics for job: " + jobID;
	    logger.error(msg);
	    throw new StateManagerException(msg);
	}

	return stats;
    }

    /**
     * Retrieves job status from the database using the jobID
     *
     * @param jobID the job id for this job
     * @return the outputs for this job
     * @throws StateManagerException if there is an error during status retrieval
     */
    public static JobOutputType getOutputs(String jobID) 
        throws StateManagerException {
	logger.debug("called");

	JobOutputType outputs = new JobOutputType();

	try {
	    Session session = getSessionFactory().openSession();
	    List results = session.createQuery("from JobOutput output " +
					       "left join fetch output.job " +
					       "where output.job.jobID = '" +
					       jobID + "'")
		.list();
	    JobOutput output = null;
	    if (results.size() == 1) {
		output = (JobOutput) results.get(0);
	    } else {
		String msg = "Can't get job outputs for job: " + jobID;
		logger.error(msg);
		throw new StateManagerException(msg);
	    }
	    try {
		// set the stdout and stderr from the DB
		outputs.setStdOut(new URI(output.getStdOut()));
		outputs.setStdErr(new URI(output.getStdErr()));
	    } catch (MalformedURIException e) {
		String msg = "Can't set URI for stdout/stderr for job: " + e.getMessage();
		logger.error(msg);
		throw new StateManagerException(msg);
	    }

	    results = session.createQuery("from OutputFile file " +
					  "left join fetch file.job " +
					  "where file.job.jobID = '" +
					  jobID + "'")
		.list();
	    if (results != null) {
		OutputFileType[] outputFileObj = new OutputFileType[results.size()];
		// set the output file objects from the database
		for (int i = 0; i < results.size(); i++) {
		    OutputFile file = (OutputFile) results.get(i);
		    outputFileObj[i] = new OutputFileType();
		    outputFileObj[i].setName(file.getName());
		    try {
			outputFileObj[i].setUrl(new URI(file.getUrl()));
		    } catch (MalformedURIException e) {
			String msg = "Can't set URI for output file: " + e.getMessage();
			logger.error(msg);
			throw new StateManagerException(msg);
		    }
		}

		outputs.setOutputFile(outputFileObj);
	    }
	    session.close();
	} catch (HibernateException ex) {
	    String msg = "Error while getting outputs from database: " + ex.getMessage();
	    logger.error(msg);
	    throw new StateManagerException(msg);
	}

	return outputs;
    }

    /**
     * Retrieves number of jobs this hour by IP
     *
     * @param IP for remote client
     * @return number of jobs this hour per IP
     * @throws StateManagerException if there is an error during retrieval
     */
    public static long getNumJobsThisHour(String remoteIP) 
	throws StateManagerException {
	logger.debug("called");

	Long numJobs = new Long(0);

	try {
	    // open a session
	    Session session = getSessionFactory().openSession();

	    // the date now
	    long timeNow = System.currentTimeMillis();
	    
	    // the date one hour back
	    long timePast = timeNow - 3600000;

	    // formulate the query
	    java.sql.Time endTimeSQL = new java.sql.Time(timeNow);
	    java.sql.Date endDateSQL = new java.sql.Date(timeNow);
	    java.sql.Time startTimeSQL = new java.sql.Time(timePast);
	    java.sql.Date startDateSQL = new java.sql.Date(timePast);

	    String query;
	    if (endDateSQL.toString().equals(startDateSQL.toString())) {
		// both start and end are the same day
		query = "select count(*)  " +            
		    " from JobInfo jobInfo where " +
		    " jobInfo.clientIP = :remoteIP " +
		    " and jobInfo.startTimeTime >= :startTime " +
		    " and jobInfo.startTimeTime <= :endTime " +
		    " and jobInfo.startTimeDate = :startDate" +
		    " and jobInfo.startTimeDate = :endDate ";
	    } else {
		// start and end are different days
		query = "select count(*)  " +            
		    " from JobInfo jobInfo where " +
		    " jobInfo.clientIP = :remoteIP " +
		    " and (jobInfo.startTimeTime >= :startTime " +
		    " and jobInfo.startTimeDate = :startDate)" +
		    " or (jobInfo.startTimeTime <= :endTime " +
		    " and jobInfo.startTimeDate = :endDate) ";
	    }
	    
	    // execute query
            Query queryStat = session.createQuery(query);
            queryStat.setString("remoteIP", remoteIP)
		.setTime("startTime", startTimeSQL)
                .setTime("endTime", endTimeSQL)
                .setDate("startDate", startDateSQL)
                .setDate("endDate", endDateSQL);
            List results = queryStat.list();
	    if (results.size() != 1) {
		session.close();
		String msg = 
		    "Error while trying to retrive number of jobs from database";
		throw new StateManagerException(msg);
	    } 
	    numJobs = (Long) results.get(0);

	    session.close();
	} catch (HibernateException he) {
	    String msg = "Error while trying to retrieve hits from database: " +
		he.getMessage();
	    logger.error(msg);
	    throw new StateManagerException(msg);
	}

	return numJobs.longValue();
    }

    /**
     * Get the number of jobs that are currently in execution
     */
    public static long getNumExecutingJobs() 
	throws StateManagerException {

	logger.debug("called");

	Long numJobs = new Long(0);

	try {
	    // open a session
	    Session session = getSessionFactory().openSession();

	    String query = 
		"select count(*)  " +            
		" from JobInfo jobInfo where " +
		" jobInfo.code = :code";
	    
	    // execute query
            Query queryStat = session.createQuery(query);
            queryStat.setInteger("code", GramJob.STATUS_ACTIVE);

            List results = queryStat.list();
	    if (results.size() != 1) {
		session.close();
		String msg = 
		    "Error while trying to retrive number of jobs from database";
		throw new StateManagerException(msg);
	    } 
	    numJobs = (Long) results.get(0);

	    session.close();
	} catch (HibernateException he) {
	    String msg = "Error while trying to retrieve hits from database: " +
		he.getMessage();
	    logger.error(msg);
	    throw new StateManagerException(msg);
	}

	return numJobs.longValue();
    }

    /**
     * Get the number of pending jobs 
     */
    public static long getNumPendingJobs() 
	throws StateManagerException {

	logger.debug("called");

	Long numJobs = new Long(0);

	try {
	    // open a session
	    Session session = getSessionFactory().openSession();

	    String query = 
		"select count(*)  " +            
		" from JobInfo jobInfo where " +
		" jobInfo.code = :code";
	    
	    // execute query
            Query queryStat = session.createQuery(query);
            queryStat.setInteger("code", GramJob.STATUS_PENDING);

            List results = queryStat.list();
	    if (results.size() != 1) {
		session.close();
		String msg = 
		    "Error while trying to retrive number of pending jobs from database";
		throw new StateManagerException(msg);
	    } 
	    numJobs = (Long) results.get(0);

	    session.close();
	} catch (HibernateException he) {
	    String msg = "Error while trying to retrieve hits for pending jobs from database: " +
		he.getMessage();
	    logger.error(msg);
	    throw new StateManagerException(msg);
	}

	return numJobs.longValue();
    }


    /**
     * Saves the service status in DB, not supported for HSQL DB
     *
     * @param status The service status to be saved
     * @return true if status is saved successfully
     * @throws StateManagerException if there is an error during the database commit
     */
    public static boolean saveServiceStatus(ServiceStatus status)
        throws StateManagerException {
	logger.debug("called");

	// return false if HSQL DB is being used
	Configuration conf = new Configuration();
	conf = conf.configure(confFile);
	String dialect = conf.getProperty("dialect");
	//if (dialect == null) {
	//    logger.error("Can't figure out the type of database being used");
	//    return false;
	//} else if (dialect.equals("org.hibernate.dialect.HSQLDialect")){
	//    logger.error("Update of service status not supported for HSQL DB");
	//    return false;
	//}

	try {
	    Session session = getSessionFactory().openSession();
	    session.beginTransaction();
	    session.saveOrUpdate(status);
	    session.getTransaction().commit();
	    session.close();
	} catch (HibernateException ex) {
	    String msg = "Error during database update: " + ex.getMessage();
	    logger.error(msg);
	    throw new StateManagerException(msg);
	}

	return true;
    }

    /**
     * Retrieves service status from the database using the serviceName
     *
     * @param serviceName The name of the service
     * @return the status for this service - ACTIVE, INACTIVE
     * @throws StateManagerException if there is an error during status retrieval
     */
    public static ServiceStatus getServiceStatus(String serviceName) 
        throws StateManagerException {
	logger.debug("called");

	ServiceStatus status = null;

	try {
	    // retrieve service status from hibernate
	    Session session = getSessionFactory().openSession();
	    List results = session.createCriteria(ServiceStatus.class)
		.add(Restrictions.eq("serviceName", serviceName))
		.list();
	    if (results.size() == 1) {
		status = (ServiceStatus) results.get(0);
	    }
	    session.close();
	} catch (HibernateException ex) {
	    String msg = "Error while getting status from database: " + ex.getMessage();
	    logger.error(msg);
	    throw new StateManagerException(msg);
	}

	if (status == null) {
	    String msg = "Can't retrieve status for service: " + serviceName;
	    logger.error(msg);
	    throw new StateManagerException(msg);
	}

	return status;
    }

    // A simple main method to test functionality
    public static void main(String[] args) 
        throws Exception {

	// initialize hibernate
	System.out.println("Initializing hibernate");
	Session session = getSessionFactory().openSession();

	// initialize info
	Date nowDate = new Date();
	JobInfo info = new JobInfo();
	String jobID = "app" + System.currentTimeMillis();
	info.setJobID(jobID);
	info.setCode(0);
	info.setMessage("This is a test");
	info.setBaseURL("http://localhost/test");
	info.setStartTimeTime(new java.sql.Time(nowDate.getTime()));
	info.setStartTimeDate(new java.sql.Date(nowDate.getTime()));
	info.setActivationTimeTime(new java.sql.Time(nowDate.getTime()));
	info.setActivationTimeDate(new java.sql.Date(nowDate.getTime()));
	info.setCompletionTimeTime(new java.sql.Time(nowDate.getTime()));
	info.setCompletionTimeDate(new java.sql.Date(nowDate.getTime()));
	info.setLastUpdateTime(new java.sql.Time(nowDate.getTime()));
	info.setLastUpdateDate(new java.sql.Date(nowDate.getTime()));
	info.setClientDN("CN=Test");
	info.setClientIP("127.0.0.1");
	info.setServiceName("Command-line");

	// save job info
	System.out.println("Trying to save JobInfo into database");
	saveJobInfoInDatabase(info);
	System.out.println("Saved JobInfo into database successfully");

	// save output files
	System.out.println("Trying to save job outputs into database");
	JobOutputType outputs = new JobOutputType();
	outputs.setStdOut(new URI("http://localhost/test/stdout.txt"));
	outputs.setStdErr(new URI("http://localhost/test/stderr.txt"));
	OutputFileType[] files = new OutputFileType[1];
	files[0] = new OutputFileType();
	files[0].setName("foo.txt");
	files[0].setUrl(new URI("http://localhost/test/foo.txt"));
	outputs.setOutputFile(files);
	saveOutputsInDatabase(jobID, outputs);
	System.out.println("Saved OutputFile into database successfully");

	System.out.println("Update job info for job: " + jobID);
	updateJobInfoInDatabase(jobID,
				1,
				"This is a test update",
				info.getBaseURL(),
				"testHandle");

	// do some searches
	System.out.println("Searching for status for job: " + jobID);
	StatusOutputType status = getStatus(jobID);
	System.out.println("Job Status: " + jobID +
			   " - {" + status.getCode() +
			   ", " + status.getMessage() +
			   ", " + status.getBaseURL() + "}");

	System.out.println("Searching for statistics for job: " + jobID);
	JobStatisticsType stats = getStatistics(jobID);
	System.out.println("Job Statistics: " + jobID +
			   " - {" + stats.getStartTime().getTime() +
			   ", " + stats.getActivationTime().getTime() +
			   ", " + stats.getCompletionTime().getTime() + "}");

	System.out.println("Searching for job outputs for job: " + jobID);
	outputs = getOutputs(jobID);
	System.out.println("Standard output: " + outputs.getStdOut());
	System.out.println("Standard error: " + outputs.getStdErr());
	files = outputs.getOutputFile();
	for (int i = 0; i < files.length; i++) {
	    System.out.println(files[i].getName() + ": " + files[i].getUrl());
	}

	// test service status updates
	ServiceStatus serviceStatus = new ServiceStatus();
	String serviceName = "FooService";
	serviceStatus.setServiceName(serviceName);
	serviceStatus.setStatus(ServiceStatus.STATUS_ACTIVE);
	System.out.println("Saving service status for service: " + 
			   serviceName);
	saveServiceStatus(serviceStatus);

	System.out.println("Retrieving status for service: " + 
			   serviceName);
	serviceStatus = getServiceStatus(serviceName);
	System.out.println("Service status is: " + 
			   serviceStatus.getStatus());
    }
}
