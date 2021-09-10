package edu.sdsc.nbcr.opal.manager;

import java.util.Properties;
import java.util.Arrays;
import java.util.List;

import edu.sdsc.nbcr.opal.util.SshUtils;
import org.globus.gram.GramJob;
import org.apache.log4j.Logger;

import edu.sdsc.nbcr.opal.manager.slurm.Job;
import edu.sdsc.nbcr.opal.AppConfigType;
import edu.sdsc.nbcr.opal.StatusOutputType;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * Implementation of an Opal Job Manager using DRMAA
 */
public class SlurmJobManager implements OpalJobManager {

    // get an instance of a log4j logger
    private static Logger logger = Logger.getLogger(SlurmJobManager.class.getName());

    private Properties props; // the container properties being passed
    private AppConfigType config; // the application configuration
    private StatusOutputType status; // current status
    private String handle; // the DRMAA job id for this submission
    private boolean started = false; // whether the execution has started
    private volatile boolean done = false; // whether the execution is complete

    private Job job; // job information after Slurm submit
    //getting ssh connect
    private SshUtils ssh_connect;
    private String local_workingDir;
    private String remote_workingDir;


    /**
     * Initialize the Job Manager for a particular job
     *
     * @param props  the properties file containing the value to configure this plugin
     * @param config the opal configuration for this application
     * @param handle manager specific handle to bind to, if this is a resumption.
     *               NULL,if this manager is being initialized for the first time.
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
        //getting ssh connect
        ssh_connect = ssh_connect();
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
     * @param argList    a string containing the command line used to launch the application
     * @param numProcs   the number of processors requested. Null, if it is a serial job
     * @param workingDir String representing the working directory of this job on the local system
     * @return a plugin specific job handle to be persisted by the service implementation
     * @throws JobManagerException if there is an error during job launch
     */
    public String launchJob(String argList,
                            Integer numProcs,
                            String workingDir)
            throws JobManagerException {
        logger.info("called");

        //getting job id for application from workingDir
        String lastChar = workingDir.substring(workingDir.length() - 1);
        if (lastChar.equals("/")) {
            workingDir = workingDir.replaceFirst(".$", "");
        }
        local_workingDir = workingDir;

        String app_jobid = workingDir.substring(workingDir.lastIndexOf("/") + 1);
        logger.info("Jod ID for App: " + app_jobid);

        remote_workingDir = props.getProperty("slurm.job.wdir");
        if (remote_workingDir == null) {
            String msg = "Remote working directory unspecified for the job";
            logger.error(msg);
            throw new JobManagerException(msg);
        }
        remote_workingDir += File.separator + app_jobid;

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
            if (argList != null)
                args += " " + argList;
        }
        if (args != null) {
            args = args.trim();
        }
        logger.debug("Argument list: " + args);

        // get the number of processors available
        String systemProcsString = props.getProperty("slurm.num.procs");
        int systemProcs = 0;
        if (systemProcsString != null) {
            systemProcs = Integer.parseInt(systemProcsString);
        }

        // launch executable using PBS
        String cmd = null;

        if (config.isParallel()) {
            // make sure enough processors are present for the job
            if (numProcs == null) {
                String msg = "Number of processes unspecified for parallel job";
                logger.error(msg);
                throw new JobManagerException(msg);
            } else if (numProcs > systemProcs) {
                String msg = "Processors required - " + numProcs +
                        ", available - " + systemProcs;
                logger.error(msg);
                throw new JobManagerException(msg);
            }

            // append arguments
            cmd = config.getBinaryLocation();
            if ((args != null) && (!(args.equals("")))) {
                cmd += " " + args;
            }
            cmd += " -n " + numProcs;
            //cmd += " -z " + workingDir;
            logger.debug("CMD: " + args);
        } else {
            // create command string and arguments for serial run
            cmd = config.getBinaryLocation();
            if ((args != null) && (!(args.equals("")))) {
                cmd += " " + args;
            }
            logger.debug("CMD: " + cmd);
        }

        // get the hard run limit
        long hardLimit = 0;
        if ((props.getProperty("opal.hard_limit") != null)) {
            hardLimit = Long.parseLong(props.getProperty("opal.hard_limit"));
            logger.warn("Property hard_limit is not supported by this job manager");
        }

        // launch the job using the above information
        try {
            logger.debug("Working directory: " + workingDir);

            // create a submission script from the params
            File script = createSubmissionScript(cmd, numProcs, remote_workingDir);
            transferFileToRemote(script.getAbsolutePath(), remote_workingDir);
            String remote_script = remote_workingDir + File.separator + script.getName();
            logger.info("Remore job script: " + remote_script);
            job = new Job(ssh_connect, config.getBinaryLocation(), remote_script);

            //submit the job
            handle = job.queue();
            logger.info("Slurm job has been submitted with id " + handle);

            boolean gatewayAttr = Boolean.valueOf(props.getProperty("gateway.attribute"));
            if (gatewayAttr) {
                logger.info("gateway attribute submission is enabled.");
		String gateway_api_key_file = props.getProperty("gateway.attribute.api.key.file");
		if (gateway_api_key_file != null && gateway_api_key_file.trim().length() > 0) {
		    logger.info("the api key file for gateway attribute submission: "+gateway_api_key_file);

		    String api_url = props.getProperty("gateway.attribute.url");
                    if (api_url == null || api_url.trim().length() == 0)
                        api_url = "https://xsede-xdcdb-api.xsede.org/gateway/v2/job_attributes"; // as the default
		    
		    String resource_name = props.getProperty("gateway.attribute.xsede.resource.name");
		    if (resource_name == null || resource_name.trim().length() == 0)
			resource_name = "comet.sdsc.xsede"; // as the default
		    String gateway_user = props.getProperty("gateway.attribute.user");
		    if (gateway_user == null || gateway_user.trim().length() == 0)
			gateway_user = "OpenTopography User"; //as the default

		    String servicename = app_jobid.split("Service")[0] + "Service";
		    logger.info("Service name: "+servicename);
		    String targeted_sw = props.getProperty("gateway.attribute.sw."+servicename);
		    logger.info("Software name: "+targeted_sw);
		    
		    //job.run_gateway_attribute_submission(handle);
		    job.run_gateway_attribute_submission_local(handle,
							       gateway_api_key_file,
							       api_url,
							       resource_name,
							       gateway_user,
							       targeted_sw);
		}
            }

        } catch (Exception e) {
            String msg = "Error while running executable via SSH - " +
                    e.getMessage();
            logger.error(msg);
            throw new JobManagerException(msg);
        }

        // notify listeners that process is activated
        started = true;

        // return an identifier for this process
        return handle;
    }

    private SshUtils ssh_connect() throws JobManagerException {
        String user = props.getProperty("slurm.user");
        if (user == null) {
            String msg = "SSH user unspecified for the file transfer";
            logger.error(msg);
            throw new JobManagerException(msg);
        }

        String host = props.getProperty("slurm.host");
        if (host == null) {
            String msg = "SSH host unspecified for the file transfer";
            logger.error(msg);
            throw new JobManagerException(msg);
        }

        int port = 22;
        String str_port = props.getProperty("slurm.port");
        if (str_port != null) {
            port = Integer.valueOf(str_port);
        }

        String keyFilePath = props.getProperty("slurm.key.file");
        if (keyFilePath == null) {
            String msg = "SSH key file unspecified for the file transfer";
            logger.error(msg);
            throw new JobManagerException(msg);
        }

        String password = props.getProperty("slurm.password");
        if (password == null) {
            String msg = "SSH password unspecified for the file transfer";
            logger.error(msg);
            throw new JobManagerException(msg);
        }

        return new SshUtils(user, host, port, keyFilePath, password);
    }

    private void transferFileToRemote(String jobscript_file,
                                      String job_wdir)
            throws JobManagerException {
        logger.info("Remote app job directory: " + job_wdir);
        String script_dir = props.getProperty("slurm.job.scriptdir");
        if (script_dir == null) {
            String msg = "Remote script directory unspecified for the job script";
            logger.error(msg);
            throw new JobManagerException(msg);
        }

        ssh_connect.transferFileToRemote(jobscript_file, job_wdir);
        String cmd = "cp -r " + script_dir + " " + job_wdir;
        logger.info("CMD: " + cmd);
        logger.info("Result: " + ssh_connect.sendCommand(cmd));
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
            String msg = "Can't wait for a process that hasn't be started";
            logger.error(msg);
            throw new JobManagerException(msg);
        }

        // poll till status is RUNNING
        while (true) {
            try {
                // if job status is not queued, terminate loop
                String jobState = job.getJobStatus(handle);
                logger.debug("Received job status: " + jobState);
                if (!jobState.equals("PD")) {
                    // not queued AKA active
                    break;
                } else {
                    // still queued - sleep for 3 seconds
                    Thread.sleep(3000);
                }
            } catch (Exception e) {
                // this is probably because keep_completed is not set
                // so PBS forgets about completed jobs
                String msg = "Can't wait for job to activate - " +
                        e.getMessage();
                logger.warn(msg);
                break;
            }
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
        logger.info("called");

        // check if this process has been started already
        if (!started) {
            String msg = "Can't wait for a process that hasn't be started";
            logger.error(msg);
            throw new JobManagerException(msg);
        }


        // poll till status is COMPLETE
        String jobState = "";
        boolean gotStatus = true;
        int numoftrials = 3;

        while (true) {
            try {
                // sleep for 3 seconds
                Thread.sleep(3000);

                // print job status
                jobState = job.getJobStatus(handle);
                logger.debug("Received job status: " + jobState);
                if (jobState.equals("CD") || jobState.equals("CG") ||
                        jobState.equals("DONE")) {
                    // execution complete
                    Thread.sleep(3000);
                    break;
                }
            } catch (Exception e) {
                // this is probably because keep_completed is not set
                // so PBS forgets about completed jobs
                String msg = "Can't wait for job to complete - " +
                        e.getMessage();
                logger.warn(msg);
                if (e.toString().contains("Auth fail") && numoftrials > 0) {
                    numoftrials--;
                    logger.warn("Retry to authenticate..."+numoftrials);
                    try {
                        Thread.sleep(5000);
                    } catch (Exception ex) {
                        logger.warn(ex.getMessage());
                    }
                } else {
                    e.printStackTrace();
                    logger.warn("Could not connect to server.");
                    gotStatus = false;
                    break;
                }
            }
        }

        // update status
        if (!gotStatus) {
            status.setCode(GramJob.STATUS_DONE);
            status.setMessage("Execution complete - " +
                    "check outputs to verify successful execution");
        } else {
            if (jobState.equals("CD") || jobState.equals("CG") ||
                    jobState.equals("DONE")) {
                status.setCode(GramJob.STATUS_DONE);
                status.setMessage("Execution complete - " +
                        "check outputs to verify successful execution - final job status is " + jobState);
            } else {
                status.setCode(GramJob.STATUS_FAILED);
                status.setMessage("Execution failed - " +
                        "final job status is " + jobState);
            }
        }

        //transfer files from remote to local
        String[] filters = new String[2];
        filters[0] = ".txt";
        filters[1] = ".tar.gz";
        ssh_connect.transferFilesToLocal(remote_workingDir, filters, local_workingDir);

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

        try {
            job.destroy(handle);
        } catch (Exception e) {
            String msg = "Error while destroying job: " +
                    e.getMessage();
            logger.error(e);
            throw new JobManagerException(msg);
        }

        // update status
        status.setCode(GramJob.STATUS_FAILED);
        status.setMessage("Process destroyed on user request");

        return status;
    }

    /**
     * Create a job submission script from input arguments
     */
    private File createSubmissionScript(String cmd,
                                        Integer numProcs,
                                        String workingDir)
            throws IOException, JobManagerException {

        // check if the pbs.script.repo property is set
        //String scriptRepo = props.getProperty("slurm.job.repo");
        //File tmpFile = null;

        /*
        if (scriptRepo == null) {
            tmpFile = File.createTempFile("/tmp", ".submit");
        } else {
            File scriptRepoDir = new File(scriptRepo);
            if (!scriptRepoDir.exists()) {
                if (!scriptRepoDir.mkdir()) {
                    String msg = "Can't create new directory to save the slurm script file";
                    logger.error(msg);
                    throw new JobManagerException(msg);
                }
            }
            tmpFile = File.createTempFile("/tmp", ".submit", scriptRepoDir);
        }
        */

        File tmpFile = File.createTempFile("slurm-", ".submit");
        tmpFile.deleteOnExit();

        PrintWriter pw = new PrintWriter(new FileWriter(tmpFile));

        Integer ppn = new Integer(props.getProperty("slurm.job.ppn"));
        int numprocs = ppn;
        int nodes = (int) Math.ceil(numProcs.doubleValue() / ppn.doubleValue());
        if (nodes == 1) numprocs = numProcs;

        // create the submission script
        String queue_type = props.getProperty("slurm.job.queue");
        String walltime = props.getProperty("slurm.job.walltime");

        pw.println("#!/bin/bash");
        pw.println("#SBATCH -A " + props.getProperty("slurm.job.account"));
        pw.println("#SBATCH -J " + config.getBinaryLocation());
        if (queue_type != null) pw.println("#SBATCH -p " + queue_type);
	pw.println("#SBATCH --mem=" + props.getProperty("slurm.job.memory_size"));
        if (walltime != null) pw.println("#SBATCH -t " + walltime);
        pw.println("#SBATCH --nodes=" + nodes);
        pw.println("#SBATCH --ntasks-per-node=" + numprocs);
        pw.println("#SBATCH -e " + workingDir + "/stderr.txt");
        pw.println("#SBATCH -o " + workingDir + "/stdout.txt");
        pw.println("#SBATCH --export=ALL");
        pw.println("");
        //pw.println("mkdir " + scheduler_wdir);
        pw.println("cd " + workingDir);

        pw.println("");
        pw.println(cmd);
        pw.close();

        // return file to submission script
        return tmpFile;
    }
}
