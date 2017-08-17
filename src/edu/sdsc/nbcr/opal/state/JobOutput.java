package edu.sdsc.nbcr.opal.state;

import java.util.Date;

import org.hibernate.Session;

import java.util.List;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * The holder class for the job outputs, used by Hibernate
 * 
 * @author Sriram Krishnan
 */

public class JobOutput {

    // get an instance of the log4j Logger
    private static Logger logger =
        Logger.getLogger(JobOutput.class.getName());

    // primary key
    private long id;

    // reference to the job
    private JobInfo job;

    // standard output and error
    private String stdOut;
    private String stdErr;

    public JobOutput() {}

    // getter and setter methods
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public JobInfo getJob() {
        return job;
    }

    public void setJob(JobInfo job) {
        this.job = job;
    }

    public String getStdOut() {
        return stdOut;
    }

    public void setStdOut(String stdOut) {
        this.stdOut = stdOut;
    }

    public String getStdErr() {
        return stdErr;
    }

    public void setStdErr(String stdErr) {
        this.stdErr = stdErr;
    }
}
