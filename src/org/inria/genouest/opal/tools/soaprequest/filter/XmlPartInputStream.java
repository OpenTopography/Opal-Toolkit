/**
 * OpalSOAPRequestFilter package
 * 
 * 
 * Licence: BSD
 * 
 * Genouest Platform (http://www.genouest.org)
 * Author: Anthony Bretaudeau <anthony.bretaudeau@irisa.fr>
 * Creation: April 15th, 2010
 */

package org.inria.genouest.opal.tools.soaprequest.filter;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implements InputStream to read only xml part of the input SOAP request.
 */
public class XmlPartInputStream extends InputStream {

	/** The soap whole request containing xml and possibly attachments. */
	private SoapInputStream wholeRequest;

	/**
	 * Instantiates a new custom soap input stream.
	 * 
	 * @param baos the output stream
	 */
	public XmlPartInputStream(SoapInputStream wholeRequest) {
		super();
		this.wholeRequest = wholeRequest;
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
		return wholeRequest.readXml();
	}


	/* (non-Javadoc)
	 * @see java.io.InputStream#close()
	 */
	@Override
	public void close() throws IOException {
		super.close();
	}

}
