package edu.sdsc.nbcr.opal.manager;

import org.apache.log4j.Logger;

/**
 * Factory class for creating instances of the Opal Job Manager
 */
public class OpalJobManagerFactory {

    // get an instance of the log4j Logger
    private static Logger logger = 
	Logger.getLogger(OpalJobManagerFactory.class.getName());

    public static OpalJobManager getOpalJobManager(String jobManagerFQCN) 
	throws JobManagerException {

	// check to see if the jobManagerFQCN is null
	if (jobManagerFQCN == null) {
	    throw new JobManagerException("Classname for job manager is null");
	}

	// instantiate a class object for this classname
	Class managerClass = null;
	try {
	    managerClass = Class.forName(jobManagerFQCN);
	} catch (ClassNotFoundException cnfe) {
	    String msg = "Can't instantiate Opal Job Manager - " + 
		"Job manager class " + jobManagerFQCN + " not found";
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}

	// make sure that this class implements the correct interface
	if (!OpalJobManager.class.isAssignableFrom(managerClass)) {
	    String msg = "Can't instantiate Opal Job Manager - " + 
		jobManagerFQCN + " does not implement the OpalJobManager interface";
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}

	// instantiate the object
	Object managerObject = null;
	try {
	    managerObject = managerClass.newInstance();
	} catch (Exception e) {
	    String msg = "Opal Job manager object can't be instantiated from class: " 
		+ jobManagerFQCN;
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}

	// return the manager class, typecast correctly
	return (OpalJobManager) managerObject;
    }
}
