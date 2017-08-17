package edu.sdsc.nbcr.opal.manager;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;

import java.lang.Math;
import java.lang.String;

import java.net.MalformedURLException;

import java.rmi.RemoteException;

import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import javax.xml.rpc.ServiceException;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import edu.sdsc.nbcr.opal.AppConfigType;
import edu.sdsc.nbcr.opal.StatusOutputType;

import org.apache.log4j.Logger;

import edu.sdsc.nbcr.opal.*;

import org.apache.axis.types.URI;
import org.apache.axis.types.URI.MalformedURIException;
import org.apache.axis.MessageContext;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.description.ServiceDesc;

import org.globus.gram.GramJob;


/**
 * Implementation of the Opal MetaService
 */
public class MetaServiceJobManager implements OpalJobManager {

    private static Logger logger = Logger.getLogger(DRMAAJobManager.class.getName());    

    private Properties props; // the container properties being passed
    private Properties props_meta;
    private AppConfigType config; // the application configuration    
    private StatusOutputType status; // current status 
    private String handle; 
    private String remoteJobID;
    private String remote_url = null;
    private String remoteBaseURL = null;
    private String workdir = null;
    private AppServicePortType appServicePort;
    private boolean started = false; // whether the execution has started 
    private volatile boolean done = false; // whether the execution is complete

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
	// TODO: read the metaservice configuration as well

        logger.info("called");

        this.props = props;
        this.config = config;
        this.handle = handle;

        status = new StatusOutputType();
    }
    
    /**
     * General clean up, if need be 
     *
     * @throws JobManagerException if there is an error during destruction
     */
    public void destroyJobManager()
	throws JobManagerException {
    }
    
    /**
     * Launch a job with the given arguments. The input files are already staged in by
     * the service implementation, and the plug in can assume that they are already
     * there
     *
     * @param argList a string containing the command line used to launch the application
     * @param numproc the number of processors requested. Null, if it is a serial job
     * @param workingDir String representing the working directory of this job on the local system
     * 
     * @return a plugin specific job handle to be persisted by the service implementation
     * @throws JobManagerException if there is an error during job launch
     */
    public String launchJob(String argList, 
			    Integer numproc, 
			    String workingDir)
	throws JobManagerException {
	logger.info ("called");

	//	String remoteJobID = null;
	//	String remoteBaseURL = null;
	workdir = workingDir;

        // make sure we have all parameters we need
        if (config == null) {
            String msg = "Can't find application configuration - "
                + "Plugin not initialized correctly";
            logger.error(msg);
            throw new JobManagerException(msg);
        }

	String mscFilePath = config.getMetaServiceConfig();


	if (mscFilePath == null) {
            String msg = "metaServiceConfig tag not defined in app config";
            logger.error(msg);
            throw new JobManagerException(msg);
	}

        File file = new File(mscFilePath);
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        DataInputStream dis = null;
	String line;
	String url;
	Integer procs;
	String [] remote_urls;
	Integer [] remote_procs;
	Map <String, Integer> url_proc_map = new HashMap<String, Integer>();

        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            dis = new DataInputStream(bis);

            while (dis.available() != 0) {
                line = dis.readLine();
		int pos = line.indexOf(" " );

		if (pos > 0) {
		    url = line.substring(0, pos);
		    procs = new Integer(line.substring(pos+1));
		    
		    AppServiceLocator asl = new AppServiceLocator();
		    AppMetadataType amt;
		
		    try {
			appServicePort = asl.getAppServicePort(new java.net.URL(url));
			amt = appServicePort.getAppMetadata(new AppMetadataInputType());

			if (config.isParallel()) {
			    if (procs >= numproc.intValue()) 
				url_proc_map.put(url, procs);
			}
			else
			    url_proc_map.put(url, procs);
		    } catch (Exception e) {
			logger.error(e.getMessage());
		    } 
		}
            }

            fis.close();
            bis.close();
            dis.close();
        } catch (FileNotFoundException e) {
	    logger.error(e.getMessage());
        } catch (IOException e) {
	    logger.error(e.getMessage());
        } catch (Exception e) {
	    logger.error(e.getMessage());
        }

	if (url_proc_map.size() == 0) {
            String msg = "No suitable remote hosts found for the application: ";
	    msg += "num procs requested larger than available OR ";
	    msg += "num procs unspecified for host in meta service config OR ";
	    msg += "no remote hosts defined in meta service config";
            logger.error(msg);
            throw new JobManagerException(msg);
	}

	Set urlset = url_proc_map.entrySet();
	Iterator it = urlset.iterator();
	Random r = new Random();
	Integer rint = Math.abs(r.nextInt() % url_proc_map.size());
	Integer counter = 0;

	while (it.hasNext()) {
	    Map.Entry kv = (Map.Entry)it.next();
	    
	    if (counter == rint) {
		remote_url = kv.getKey().toString();
		break;
	    }

	    counter++;
	}

	logger.info("Using remote URL: " + remote_url);

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

	AppServiceLocator asl = new AppServiceLocator();
	AppMetadataType amt;
	JobInputType in = new JobInputType();

	if (args != null)
	    in.setArgList(args);

	if (numproc != null)
	    in.setNumProcs(numproc);

	File dir = new File(workingDir);
	String [] infiles = dir.list();
	Vector inputFileVector = new Vector();

	if (dir != null) {
	    for (int j = 0; j < infiles.length; j++) {
		DataHandler dh = new DataHandler(new FileDataSource(workingDir + File.separator + infiles[j]));
		InputFileType infile = new InputFileType();
		infile.setName(infiles[j]);
		infile.setAttachment(dh);
		inputFileVector.add(infile);
	    }
	}

	int arraySize = inputFileVector.size();

	if (arraySize > 0) {
	    InputFileType [] infileArray = new InputFileType[arraySize];

	    for (int k = 0; k < arraySize; k++) 
		infileArray[k] = (InputFileType) inputFileVector.get(k);

	    in.setInputFile(infileArray);
	}

	asl = new AppServiceLocator();

	try {
	    appServicePort = asl.getAppServicePort(new java.net.URL(remote_url));
	    JobSubOutputType subOut = appServicePort.launchJob(in);
	    remoteJobID = subOut.getJobID();
	    StatusOutputType remoteStatus = appServicePort.queryStatus(remoteJobID);
	    remoteBaseURL = remoteStatus.getBaseURL().toString();
	    
	    logger.info("LaunchJob Remote Job URL: " + remoteBaseURL);
	} catch (MalformedURLException e) {
	    logger.error(e.getMessage());
	} catch (ServiceException e) {
	    logger.error(e.getMessage());
	} catch (FaultType e) {
	    logger.error(e.getMessage());
	} catch (RemoteException e) {
	    logger.error(e.getMessage());
	}

	started = true;

	handle = remoteBaseURL;
	handle = remoteJobID;

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

        // check if this process has been started already                                                                                                       
        if (!started) {
            String msg = "Can't wait for a remote job that hasn't be started";
            logger.error(msg);
            throw new JobManagerException(msg);
	}

	try {
	    StatusOutputType status = appServicePort.queryStatus(remoteJobID);
	    int code = appServicePort.queryStatus(remoteJobID).getCode();
	
	    while (code == GramJob.STATUS_PENDING) {
		status = appServicePort.queryStatus(remoteJobID);
		code = status.getCode();
		Thread.sleep(3000);
	    }
	} catch (RemoteException e) {
	    logger.error("RemoteException in WaitForActivation - " + e.getMessage());
	} catch (Exception e) {
	    logger.error("Exception in WaitForActivation - " + e.getMessage());
	}

	status.setCode(GramJob.STATUS_ACTIVE);
	status.setMessage("Job active on remote: <A HREF=" + remoteBaseURL + ">" + remoteBaseURL + "</A>");

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
	// TODO: poll for completion and download results

        // check if this process has been started already                                                                                                        
        if (!started) {
            String msg = "Can't wait for a remote job that hasn't be started";
            logger.error(msg);
            throw new JobManagerException(msg);
        }

	int code = -1;

	try {
	    StatusOutputType status = appServicePort.queryStatus(remoteJobID);
	    code = appServicePort.queryStatus(remoteJobID).getCode();
	
	    while (code != GramJob.STATUS_DONE && code != GramJob.STATUS_FAILED) {
		status = appServicePort.queryStatus(remoteJobID);
		code = status.getCode();
		status.setCode(code);
		Thread.sleep(3000);
	    }
	} catch (RemoteException e) {
	    logger.error("RemoteException in WaitForActivation - " + e.getMessage());
	} catch (Exception e) {
	    logger.error("Exception in WaitForActivation - " + e.getMessage());
	}

	try {
	    JobOutputType out = appServicePort.getOutputs(remoteJobID);
	    //appServicePort.getAllOutputs("doh");
	    OutputFileType [] outfile = out.getOutputFile();
	    //URL filename = null;
	    //String fileurl;

	    String [] relPath;
	    String outPath;

	    if (outfile != null) {
		relPath = new String[outfile.length];
		
		for (int j = 0;  j < outfile.length; j++) {
		    String u = outfile[j].getUrl().toString();
		    int index = u.indexOf('/' + remoteJobID + '/');
		    relPath[j] = u.substring(index + remoteJobID.length() + 2);
		    
		    BufferedInputStream in = new BufferedInputStream(new java.net.URL(outfile[j].getUrl().toString()).openStream());

		    if (relPath[j].indexOf("/") != -1) {
			index = relPath[j].lastIndexOf("/");
			String d = workdir + File.separator + relPath[j].substring(0, index);
			boolean b = new File(d).mkdirs();
			outPath = workdir + File.separator + relPath[j];
		    }
		    else  
			outPath = workdir + File.separator + outfile[j].getName();

		    FileOutputStream fos = new FileOutputStream(outPath);
		    java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
		    byte [] data = new byte[1024];
		    int x = 0;
		    
		    while ((x = in.read(data, 0, 1024)) >= 0) 
			bout.write(data, 0, x);

		    bout.close();
		    in.close();
		}
	    }

	    String [] stdfiles = {"stdout.txt", "stderr.txt"};

	    try {
		for (int i = 0; i < stdfiles.length; i++) {
		    BufferedInputStream in = new BufferedInputStream(new java.net.URL(remoteBaseURL + "/" + stdfiles[i]).openStream());
		    FileOutputStream fos = new FileOutputStream(workdir + File.separator + stdfiles[i]);
		    java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
		    byte [] data = new byte[1024];
		    int x = 0;
		    
		    while ((x = in.read(data, 0, 1024)) >= 0) 
			bout.write(data, 0, x);
		    
		    bout.close();
		    in.close();
		}
	    } catch (Exception e) {
		logger.error(e.getMessage());
	    }


	} catch (RemoteException e) {
	    logger.error("RemoteException in WaitForCompletion - " + e.getMessage());
	} catch (Exception e) {
	    logger.error("Exception in WaitForCompletion - " + e.getMessage());
	}


	String smsg;

	if (code == GramJob.STATUS_DONE){
	    smsg = "Successfully completed on remote: <A HREF=" + remoteBaseURL + ">" + remoteBaseURL + "</A>";
	    logger.info(smsg);
	    status.setCode(GramJob.STATUS_DONE);
	    status.setMessage(smsg);
	    /*	    try {
	       Thread.sleep(300000);
	       } catch (Exception e) {}; */
	} 
	else {
	    smsg = "Failed on remote: <A HREF=" + remoteBaseURL + ">" + remoteBaseURL + "</A>";
	    logger.info(smsg);
	    status.setCode(GramJob.STATUS_FAILED);
	    status.setMessage(smsg);
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

	try {
	    appServicePort.destroy(remoteJobID);	    
	    logger.info("Remote job killed on user request: " + remoteBaseURL);
	    status.setCode(GramJob.STATUS_FAILED);
	    status.setMessage("Remote destroyed on user request");
	} catch (Exception e) {
	    logger.error(e.getMessage());
	} 

        return status;
    }
}
