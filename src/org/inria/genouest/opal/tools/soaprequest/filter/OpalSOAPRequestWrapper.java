/**
 * OpalSOAPRequestFilter package
 * 
 * 
 * Licence: BSD
 * 
 * Genouest Platform (http://www.genouest.org)
 * Author: Anthony Bretaudeau <anthony.bretaudeau@irisa.fr>
 * Modified by: Sriram Krishnan <sriram@sdsc.edu>
 * Creation: May 26th, 2009
 */

package org.inria.genouest.opal.tools.soaprequest.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.apache.log4j.Logger;

/**
 * Implements HttpServletRequestWrapper to actually do the conversion.
 */
public class OpalSOAPRequestWrapper extends HttpServletRequestWrapper {
	
	/** The modified stream. */
	private SoapInputStream modifiedStream;
	
	/** The Constant logger. */
	private static final Logger logger = Logger.getLogger(OpalSOAPRequestWrapper.class);
	
	/** The context. */
	private ServletContext context;
	
	/** The wsdd name space. */
	static String wsddNameSpace = "http://xml.apache.org/axis/wsdd/";

	/**
	 * Instantiates a new opal soap request wrapper.
	 * 
	 * @param request the request
	 * @param servletContext the servlet context
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public OpalSOAPRequestWrapper(HttpServletRequest request, ServletContext servletContext, String serviceName) throws IOException {
		super(request);
		context = servletContext;

		logger.debug("Entering OpalSOAPRequestFilter");
		// make sure that this is indeed an Opal call by checking request params
		if (getContentType() != null) {
		    modifiedStream = new SoapInputStream(request.getInputStream(), getContentType().startsWith("multipart/"), serviceName, context);
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequestWrapper#getContentLength()
	 */
	@Override
	public int getContentLength() {
		return -1;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequestWrapper#getInputStream()
	 */
	@Override
	public ServletInputStream getInputStream() throws IOException {
	    if (modifiedStream != null) {
		return modifiedStream;
	    } else {
		return super.getInputStream();
	    }
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequestWrapper#getReader()
	 */
	@Override
	public BufferedReader getReader() throws IOException {
		final String enc = getCharacterEncoding();
		final InputStream istream = getInputStream();
		final Reader r = new InputStreamReader(istream, enc);

		return new BufferedReader(r);
	}
}
