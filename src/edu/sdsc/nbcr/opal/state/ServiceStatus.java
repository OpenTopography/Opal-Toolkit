package edu.sdsc.nbcr.opal.state;

import java.util.Date;

import org.apache.log4j.Logger;

/**
 * The holder class for the service status
 * 
 * @author Sriram Krishnan
 */

public class ServiceStatus {

    // get an instance of the log4j Logger
    private static Logger logger =
        Logger.getLogger(ServiceStatus.class.getName());

    // the name of this service
    private String serviceName;

    // the service status - ACTIVE and INACTIVE
    private String status;

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
}