package edu.sdsc.nbcr.opal.util;

import org.apache.axis.MessageContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.axis.transport.http.HTTPConstants;
import java.io.FileInputStream;

import org.globus.axis.gsi.GSIConstants;

import org.apache.log4j.Logger;

/**
 *
 * Utility class used by various other classes
 *
 * @author Sriram Krishnan
 */

public class Util {

    // get an instance of the log4j Logger
    private static Logger logger = 
	Logger.getLogger(Util.class.getName());

    /**
     * Get IP address of remote Web service client
     */
    public static String getRemoteIP() {
	// get the current MessageContext and the HttpServletRequest objects
	String clientIP;

	MessageContext msgContext = MessageContext.getCurrentContext();
	HttpServletRequest request =
		(HttpServletRequest) msgContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);

	// get the client IP
	if((request == null) || !(request instanceof HttpServletRequest)) {
		clientIP = "Unknown";
	} else {
		clientIP = request.getRemoteAddr();
	}

	logger.info("Client's IP: " + clientIP);
	return clientIP;
    }

    /**
     * Get the DN for the remote Web service client
     */
    public static String getRemoteDN() {
	// get the current MessageContext and the HttpServletRequest objects
        MessageContext mc = MessageContext.getCurrentContext();
        HttpServletRequest req = 
            (HttpServletRequest) mc.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);

	// get the client DN
        String clientDN = (String) req.getAttribute(GSIConstants.GSI_USER_DN);
        if(clientDN == null) {
	    clientDN = "Unknown client";
	}

	logger.debug("Client's DN: " + clientDN);
	return clientDN;
    }
}
