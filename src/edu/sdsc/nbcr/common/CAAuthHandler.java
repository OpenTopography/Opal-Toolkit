package edu.sdsc.nbcr.common;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.http.HTTPConstants;

import org.globus.axis.gsi.GSIConstants;
import org.globus.security.gridmap.GridMap;
import org.globus.gsi.gssapi.GlobusGSSContextImpl;
import org.globus.gsi.gssapi.GSSConstants;

import org.ietf.jgss.Oid;

import java.io.IOException;

import java.util.Enumeration;

/**
 * Axis Handler for NBCR services that authorizes clients on the basis of 
 * a list of acceptable CAs
 *
 * <p>To configure a service with GSI security, and enable authorization based
 * on acceptable CAs, please consult the 
 * <a href="http://nbcr.net/software/opal/docs/security.html">security docs</a>
 *
 * @author Sriram Krishnan
 */

public class CAAuthHandler extends AbstractHandler {

	// location of ca-map file
	private String caMapLoc = null;

	// get an instance of the log4j logger
	private static Logger logger =
			Logger.getLogger(CAAuthHandler.class.getName());

	/**
	 * Sole constructor
	 */
	public CAAuthHandler() {
		super();
		caMapLoc = (String) getParameter("ca-map").getValue();
		if (caMapLoc == null)
			logger.error("Property ca-map not set");
		else
			logger.info("Location of ca-map: " + caMapLoc);
	}

	/**
	 * Initialization method that is automatically invoked on startup

	 public void init() {
	 super.init();
	 caMapLoc = (String)getOption("ca-map");
	 if (caMapLoc == null)
	 logger.error("Property ca-map not set");
	 else
	 logger.info("Location of ca-map: " + caMapLoc);
	 }
	 */

	/**
	 * The main method which is invoked on every Web services call. For a GSI
	 * https connection, it checks whether the client's CA is on a list of
	 * acceptable CAs
	 *
	 * @param msgContext the Axis message context from which the method retrieves
	 * the client's DN
	 * @throws AxisFault if the client's CA is not on the list of acceptable CAs, or
	 * if there is any error
	 */
	@Override
	public InvocationResponse invoke(MessageContext msgContext)
			throws AxisFault {
		logger.info("entering");

		// get the HttpServletRequest object
		Object tmp =
				msgContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);

		if((tmp == null) || !(tmp instanceof HttpServletRequest)) {
			logger.info("exiting");
			return InvocationResponse.CONTINUE;
		}

		HttpServletRequest req = (HttpServletRequest) tmp;

		// get the user's DN
		Object userDN = req.getAttribute(GSIConstants.GSI_USER_DN);

		if(userDN == null) {
			// if this is not set, gsi https is not being used
			logger.info("exiting");
			return InvocationResponse.CONTINUE;
		}
		logger.info("Client's DN: " + userDN);

		// get the GSSContext
		Object gssContext = req.getAttribute("org.globus.gsi.context");
		String issuerDN = null;
		try {
			if (gssContext != null) {
				Object certs = ((GlobusGSSContextImpl)gssContext).
						inquireByOid(GSSConstants.X509_CERT_CHAIN);
				if (certs != null) {
					X509Certificate[] chain = (X509Certificate[]) certs;
					logger.debug("Certificate chain - ");
					for (int i = 0; i < chain.length; i++) {
						logger.debug(chain[i].getSubjectDN());
					}
					issuerDN = chain[chain.length-1].getSubjectDN().toString();
					logger.info("Client's CA DN: " + issuerDN);
				}
			} else {
				// if this is not set, gsi https is not being used
				logger.info("exiting");
				return InvocationResponse.CONTINUE;
			}
		} catch (Exception e) {
			// log, and return
			logger.error(e);
			throw new AxisFault("Error while reading certificate chain: " +
					e.getMessage());
		}

		GridMap caMap = new GridMap();
		try {
			caMap.load(caMapLoc);
		} catch (IOException ioe) {
			logger.fatal("Can't load ca-map", ioe);
			throw new AxisFault("Can't load ca-map", ioe);
		}

		if (issuerDN == null) {
			logger.error("Can't find DN for the client's CA");
			throw new AxisFault("Can't find DN for the client's CA");
		}

		if (caMap.getUserID(issuerDN) == null) {
			logger.info("DN for the client's CA not on the ca-map");
			throw new AxisFault("CA: " + issuerDN +
					" does not have an entry on the ca-map");
		}
		logger.info("exiting");
		return InvocationResponse.CONTINUE;
	}
}
