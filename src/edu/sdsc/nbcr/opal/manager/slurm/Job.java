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
