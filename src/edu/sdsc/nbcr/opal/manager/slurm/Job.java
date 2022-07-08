/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.sdsc.nbcr.opal.manager.slurm;

import edu.sdsc.nbcr.opal.util.SshUtils;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @author Choonhan Youn
 * @version 1.0
 * <p>
 * This class is a representation of a Job.
 * @since Feb. 27, 2018
 */
public class Job {

    private static Logger logger = Logger.getLogger(Job.class.getName());
    private String Name = "N/A";
    private String executableFile = "N/A";
    private SshUtils ssh_connect = null;

    /**
     * @param ssh_connect
     * @param JobName
     * @param ShellFile Creates a new Job.
     */
    public Job(SshUtils ssh_connect, String JobName, String ShellFile) {
        this.ssh_connect = ssh_connect;
        this.Name = JobName;
        this.executableFile = ShellFile;
    }

    public Job() {
    }

    /*
     Add this Job to the Scheduler Queue.
     */
    public String queue() throws Exception {

        String st = "sbatch" + " " + executableFile;
        logger.info("Command: " + st);
        String result = ssh_connect.sendCommand(st);
        //Submitted batch job 14657844
        logger.info("Result: "+result);

        return result.substring(result.lastIndexOf(' ') + 1).replace("\n", "").replace("\r", "");

    }

    //gateway_submit_attributes -gateway_user opentopo@opentopo.org -jobid $SLURM_JOB_ID -submit_time "$DATE"
    public void run_gateway_attribute_submission(String jobID) throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String sdate = dateFormat.format(date);

        String st = "gateway_submit_attributes -gateway_user opentopo@opentopo.org -jobid " + jobID + " -submit_time " +"\""+sdate+"\"";
        logger.info("Command: " + st);
        String result = ssh_connect.sendCommand(st);
        //Submitted batch job 14657844
        logger.info("Result: "+result);
    }

    public void run_gateway_attribute_submission_local(String jobID,
						       String gateway_api_key_file,
						       String gateway_url,
						       String resource_name,
						       String gateway_user,
						       String targeted_sw) throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String sdate = dateFormat.format(date);

        //String cmd = "curl -XPOST --data @" + gateway_api_key_file +
	//    " --data-urlencode \"gatewayuser=" + gateway_user + "\"" +
	//    " --data-urlencode \"xsederesourcename=" + resource_name + "\"" +
	//    " --data-urlencode \"jobid=" + jobID + "\"" +
	//    " --data-urlencode \"submittime=" + sdate + "\" " + gateway_url;
	if (targeted_sw == null) {
	    targeted_sw = "Unknown:Unknown v0.0";
	}
	
	String[] cmd = {"curl", "-X", "POST", "--data", "@"+gateway_api_key_file,
			"--data-urlencode", "gatewayuser="+gateway_user,
			"--data-urlencode", "xsederesourcename="+resource_name,
			"--data-urlencode", "jobid="+jobID,
			"--data-urlencode", "software="+targeted_sw,
			"--data-urlencode", "submittime="+sdate,
			gateway_url};

        logger.info("Command: " + cmd);
        //System.out.println("Command: " + cmd);
	ProcessBuilder process = new ProcessBuilder(cmd);
	Process p;
	try {
	    p = process.start();
	    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    StringBuilder builder = new StringBuilder();
	    String line = null;
	    while ((line = reader.readLine()) != null) {
		builder.append(line);
		builder.append(System.getProperty("line.separator"));
	    }
	    String result = builder.toString();
	    logger.info(result);

	    BufferedReader reader_error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
	    StringBuilder builder_error = new StringBuilder();
	    line = null;
	    while ((line = reader_error.readLine()) != null) {
		builder_error.append(line);
		builder_error.append(System.getProperty("line.separator"));
	    }
	    result = builder_error.toString();
	    logger.info(result);

	} catch (IOException e) {
	    logger.info("error: "+e.getMessage());
	    //e.printStackTrace();
	}

    }

    /*
    %t    Job  state,  compact  form:  PD (pending), R (running), CA
                    (cancelled), CF(configuring), CG  (completing),  CD  (com-
                    pleted),  F  (failed), TO (timeout), NF (node failure) and
                    SE (special exit state). )
     */
    public String getJobStatus(String JobID) throws Exception {

        String st = "squeue -h -o %t -j" + " " + JobID;
        //logger.info("Command: " + st);
        String result = ssh_connect.sendCommand(st);
        //logger.info("Result: "+result);
        return result;
    }

    public String destroy(String JobID) throws Exception {

        String st = "scancel" + " " + JobID;
        logger.info("Command: " + st);
        String result = ssh_connect.sendCommand(st);
        logger.info("Result: "+result);
        return result;
    }

}
