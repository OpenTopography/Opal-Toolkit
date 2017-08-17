package edu.sdsc.nbcr.opal.state;

import java.util.Date;

import org.apache.log4j.Logger;

/**
 * The holder class for the job state, used by Hibernate
 * 
 * @author Sriram Krishnan
 */

public class JobInfo {

    // get an instance of the log4j Logger
    private static Logger logger =
        Logger.getLogger(JobInfo.class.getName());

    // the jobID for this job
    private String jobID;

    // the other fields - self explanatory
    private int code;
    private String message;
    private String baseURL;
    private String handle;
    private java.sql.Date startTimeDate;
    private java.sql.Time startTimeTime;
    private java.sql.Date activationTimeDate;
    private java.sql.Time activationTimeTime;
    private java.sql.Date completionTimeDate;
    private java.sql.Time completionTimeTime;
    private java.sql.Date lastUpdateDate;
    private java.sql.Time lastUpdateTime;
    private String clientDN;
    private String clientIP;
    private String serviceName;
    private String userEmail;

    // default constructor
    public JobInfo() {}

    // getter and setter methods
    public String getJobID() {
        return jobID;
    }

    public void setJobID(String jobID) {
        this.jobID = jobID;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public java.sql.Date getStartTimeDate() {
        return startTimeDate;
    }

    public void setStartTimeDate(java.sql.Date startTime) {
        this.startTimeDate = startTime;
    }

    public java.sql.Time getStartTimeTime() {
        return startTimeTime;
    }

    public void setStartTimeTime(java.sql.Time startTime) {
        this.startTimeTime = startTime;
    }

    public java.sql.Date getActivationTimeDate() {
        return activationTimeDate;
    }

    public void setActivationTimeDate(java.sql.Date activationTime) {
        this.activationTimeDate = activationTime;
    }

    public java.sql.Time getActivationTimeTime() {
        return activationTimeTime;
    }

    public void setActivationTimeTime(java.sql.Time activationTime) {
        this.activationTimeTime = activationTime;
    }

    public java.sql.Date getCompletionTimeDate() {
        return completionTimeDate;
    }

    public void setCompletionTimeDate(java.sql.Date completionTime) {
        this.completionTimeDate = completionTime;
    }

    public java.sql.Time getCompletionTimeTime() {
        return completionTimeTime;
    }

    public void setCompletionTimeTime(java.sql.Time completionTime) {
        this.completionTimeTime = completionTime;
    }

    public java.sql.Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(java.sql.Date lastUpdate) {
        this.lastUpdateDate = lastUpdate;
    }

    public java.sql.Time getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(java.sql.Time lastUpdate) {
        this.lastUpdateTime = lastUpdate;
    }

    public String getClientDN() {
        return clientDN;
    }

    public void setClientDN(String clientDN) {
        this.clientDN = clientDN;
    }

    public String getClientIP() {
        return clientIP;
    }

    public void setClientIP(String clientIP) {
        this.clientIP = clientIP;
    }


    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}
