package edu.sdsc.nbcr.opal.util;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import edu.sdsc.nbcr.opal.AppConfigType;
import edu.sdsc.nbcr.common.TypeDeserializer;

import edu.sdsc.nbcr.opal.state.HibernateUtil;
import edu.sdsc.nbcr.opal.state.ServiceStatus;

/**
 *
 * Utility class for deployment of Opal services
 *
 * @author Sriram Krishnan
 */
public class Deploy {
    private static Logger logger = Logger.getLogger(Deploy.class.getName());

    /**
     * Default constructor
     */
    public Deploy() {
    }

    /**
     * The main method which uses a WSDD template for Opal services, and replaces
     * the dummy variables with application specific parameters, which is 
     * then used by the Ant target to deploy a new Opal service.
     *
     * <p>The following system properties are required -
     * <br><i>appConfig</i>: the location of the application configuration file
     * <br><i>serviceName</i>: the name of the target service to deploy
     * <br><i>wsddTemplate</i>: the location of the WSDD template 
     * <br><i>wsddFinal</i>: the target location for the generated WSDD
     */
    public static void main(String[] args) throws Exception {

	// get the location of the application configuration
	String appConfig = System.getProperty("appConfig");
	if (appConfig == null) {
	    logger.info("System property appConfig not set!");
	    System.exit(1);
	} else {
	    logger.info("Property appConfig set to: " + appConfig);
	}

	// get the service name
	String serviceName = System.getProperty("serviceName");
	if (serviceName == null) {
	    logger.error("System property serviceName not set!");
	    System.exit(1);
	} else {
	    logger.info("Property serviceName set to: " + serviceName);
	}

	// get the version number - optional
	String userVersion = System.getProperty("appVersion");
	if (userVersion.equals("")) {
	    userVersion = null;
	}

	if (userVersion == null) {
	    logger.info("Version number not supplied by user");
	} else {
	    logger.info("Property appVersion set to: " + userVersion);
	}

	// location of the WSDD template - supplied by build.xml
	String wsddTemplate = System.getProperty("wsddTemplate");
	if (wsddTemplate == null) {
	    logger.error("System property wsddTemplate not set!");
	    System.exit(1);
	} else {
	    logger.info("Property wsddTemplate set to: " + wsddTemplate);
	}

	// check to make sure that the WSDD template exists
	File f = new File(wsddTemplate);
	if (!f.exists()) {
	    logger.error("WSDD template file " + wsddTemplate + " does not exist");
	    System.exit(1);
	}

	// location of final WSDD - also supplied by build.xml
	String wsddFinal = System.getProperty("wsddFinal");
	if (wsddFinal == null) {
	    logger.error("System property wsddFinal not set!");
	    System.exit(1);
	} else {
	    logger.info("Property wsddFinal set to: " + wsddFinal);
	}

	// check to make sure that the application configuration exists
	File configFile = new File(appConfig);
	if (!configFile.exists()) {
	    logger.error("Application configuration file " + 
			 appConfig + " does not exist");
	}

	// check to make sure that the file points to valid application
	// configuration
	AppConfigType config = null;
	try {
	    config = 
		(AppConfigType) TypeDeserializer.getValue(appConfig,
							  new AppConfigType());;

	} catch (Exception e) {
	    logger.error(e);
	    String msg = "Can't read application configuration from: " +
		appConfig;
	    logger.error(msg);
	    System.exit(1);
	}

	// get the version number from the application configuration
	String configVersion = null;
	if (config.getMetadata() != null) {
	    configVersion = config.getMetadata().getVersion();
	}

	if ((configVersion != null) && (userVersion != null)) {
	    if (!configVersion.equals(userVersion)) {
		logger.error("Version number supplied by user doesn't match " +
			     "with version number in appConfig file");
		System.exit(1);
	    }
	}

	// set the final version number
	String finalVersion = userVersion;
	if (finalVersion == null) {
	    finalVersion = configVersion;
	}

	// set the final service name
	if (finalVersion != null) {
	    serviceName += "_" + finalVersion;
	}
	logger.info("Service name used for deployment: " + serviceName);

	// replace SERVICE_NAME with actual service name
	byte[] data = new byte[(int) f.length()];
	FileInputStream fIn = new FileInputStream(f);
	fIn.read(data);
	fIn.close();
	String templateData = new String(data);
	String finalData = templateData.replaceAll("@SERVICE_NAME@", 
						   serviceName);

	// set the location of the config file
	String configLoc = configFile.getAbsolutePath().replace('\\', '/');
	finalData = finalData.replaceAll("@CONFIG_LOCATION@",
					 configLoc);
	logger.info("Using location of config file: " + configLoc);

	FileOutputStream fOut = new FileOutputStream(wsddFinal);
	fOut.write(finalData.getBytes());
	fOut.close();

	// updating service status in database
	logger.info("Updating service status in database to ACTIVE");
	ServiceStatus serviceStatus = new ServiceStatus();
	serviceStatus.setServiceName(serviceName);
	serviceStatus.setStatus(ServiceStatus.STATUS_ACTIVE);
	HibernateUtil.saveServiceStatus(serviceStatus);
    }
}
