package edu.sdsc.nbcr.opal.state;

import java.util.Date;

import org.hibernate.Session;

import java.util.List;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * The holder class for the output files, used by Hibernate
 * 
 * @author Sriram Krishnan
 */

public class OutputFile {

    // get an instance of the log4j Logger
    private static Logger logger =
        Logger.getLogger(OutputFile.class.getName());

    // primary key
    private long id;

    // reference to the job
    private JobInfo job;

    // file name and url
    private String name;
    private String url;

    public OutputFile() {}

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
