package edu.sdsc.nbcr.opal.manager;

import java.util.Properties;

import org.globus.gsi.GlobusCredential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.globus.ftp.GridFTPClient;
import org.globus.util.GlobusURL;
import org.globus.io.urlcopy.UrlCopy;
import org.globus.ftp.FileInfo;

import org.apache.log4j.Logger;

import java.io.File;

import edu.sdsc.nbcr.opal.AppConfigType;
import edu.sdsc.nbcr.opal.StatusOutputType;

/**
 *
 * Implementation of an Opal Job Manager using Globus
 */
public class RemoteGlobusJobManager extends GlobusJobManager {

    // get an instance of a log4j logger
    private static Logger logger = Logger.getLogger(RemoteGlobusJobManager.class.getName());

    // local working directory
    private String workingDir;

    // base url for gridftp uploads
    private String gridFTPBase;

    // globus credential
    private GSSCredential gssCred;

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
	this.workingDir = workingDir;

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

	// create credentials from service certificate/key
	gridFTPBase = props.getProperty("globus.gridftp_base");
	if (config.getGridftpBase() != null) {
	    gridFTPBase = config.getGridftpBase().toString();
	}

	String remoteDir = null;
	File wd = new File(workingDir);
	try {
	    GlobusCredential globusCred = 
		new GlobusCredential(serviceCertPath, 
				     serviceKeyPath);
	    gssCred = 
		new GlobusGSSCredentialImpl(globusCred,
					    GSSCredential.INITIATE_AND_ACCEPT);

	    GlobusURL baseURL = new GlobusURL(gridFTPBase);	    
	    GridFTPClient client = new GridFTPClient(baseURL.getHost(),
						     baseURL.getPort());
	    
	    client.authenticate(gssCred);

	    // create working directory
	    remoteDir = baseURL.getPath() +
		File.separator +
		wd.getName();
	    client.makeDir(remoteDir);

	    String[] inputFiles = wd.list();
	    for (int i = 0; i < inputFiles.length; i++) {
		logger.info("Staging input file: " + inputFiles[i]);
		UrlCopy uc = new UrlCopy();
		uc.setDestinationUrl(new GlobusURL(gridFTPBase + "/" + 
						   wd.getName() + "/" +
						   inputFiles[i]));
		uc.setSourceUrl(new GlobusURL("file:///" + workingDir + "/" +
					      inputFiles[i]));
		uc.setCredentials(gssCred);
		
		uc.copy();
	    }
	} catch (Exception e) {
	    String msg = "Exception while preparing and staging input files";
	    logger.error(msg, e);
	    throw new JobManagerException(msg + " - " + e.getMessage());
	}

	// launch the job using the superclass
	return super.launchJob(argList, numProcs, remoteDir);
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

	// wait for completion using the superclass
	super.waitForCompletion();

	// stage output files back
	try {
	    GlobusURL baseURL = new GlobusURL(gridFTPBase);
	    
	    GridFTPClient client = new GridFTPClient(baseURL.getHost(),
						     baseURL.getPort());
	    
	    client.authenticate(gssCred);
	    client.setPassive();
	    client.setLocalActive();

	    File wd = new File(workingDir);
	    String remoteDir = baseURL.getPath() +
		File.separator +
		wd.getName();
	    client.changeDir(remoteDir);
	    Object[] remoteFiles = client.list().toArray();
	    for (int i = 0; i < remoteFiles.length; i++) {
		FileInfo fileInfo = (FileInfo) remoteFiles[i];
		if (fileInfo.isFile()) {
		    String fileName = fileInfo.getName();
		    logger.info("Staging output file: " + fileName);
		    UrlCopy uc = new UrlCopy();
		    uc.setSourceUrl(new GlobusURL(gridFTPBase + "/" +
						  wd.getName() + "/" +
						  fileName));
		    uc.setDestinationUrl(new GlobusURL("file:///" + workingDir + "/" +
						       fileName));
		    uc.setCredentials(gssCred);
		    uc.copy();
		}
	    }
	} catch (Exception e) {
	    String msg = "Exception while staging output files";
	    logger.error(msg, e);
	    throw new JobManagerException(msg + " - " + e.getMessage());
	}

	// return status
	return status;
    }
}
