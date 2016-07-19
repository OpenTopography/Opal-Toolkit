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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

/**
 * Implements ServletInputStream to convert possibly typed SOAP request to standard Opal SOAP request
 */
public class SoapInputStream extends ServletInputStream {

    /** The input data. */
    private InputStream rawRequest;
	
    /** Is this soap request a multipart one? */
    private boolean isMultipartRequest;
	
    /** The boundary in case of multipart request */
    private String multipartBoundary = "";
	
    /** A cached line of data */
    private String cachedLine = "";
	
    /** A cached line of XML data */
    private String cachedXmlLine = "";
	
    /** A reader for the cached XML line */
    private StringReader cachedXmlLineReader;
	
    /** A reader containing some XML transformed by XSLT file */
    private BufferedReader transformedXML = null;
	
    /** True when current cursor is in the header of a MIME section */
    private boolean isInMultipartSectionHeader = false;
	
    /** True when current cursor is in the header of a MIME section containing XML */
    private boolean isInXmlContentHeader = false;
	
    /** True when current cursor is in the content of a MIME section containing XML */
    private boolean isInXmlContent = false;

    /** Contains a copy of the last char read from raw request */
    private int previousDataChunk;
	
    /** Holds the last end of line char(s) we found while parsing xml data. */
    private String cachedXmlEOL = "";
	
    /** Set to true when you don't want to continue parsing of the request (used for performance reason when xslt is applied successfully) */
    private boolean stopParsing = false;
	
    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(SoapInputStream.class);

    /** The name of the service invoked. */
    private String serviceName;
	
    /** The context. */
    private ServletContext context;
	
    /** The wsdd name space. */
    static String wsddNameSpace = "http://xml.apache.org/axis/wsdd/";
	
    /**
     * Instantiates a new custom soap input stream.
     * 
     * @param baos the output stream
     */
    public SoapInputStream(ServletInputStream requestStream, boolean isMultipart, String serviceName, ServletContext context) {
	super();
	rawRequest = requestStream;
	isMultipartRequest = isMultipart;
	this.context = context;
	this.serviceName = serviceName;
		
	if (!isMultipartRequest) {
	    // This is not a multipart request: launch xml transformation immediately
	    XmlPartInputStream xmlStream = new XmlPartInputStream(this);
	    try {
		transformedXML = applyXSLTTransformation(xmlStream);
	    } catch (Exception e) {
		logger.warn("Could not apply XSLT stylesheet to incoming xml.");
		transformedXML = null;
	    }
	    try {
		xmlStream.close();
	    } catch (IOException e) {
		logger.warn("Problem closing incoming xml stream.");
		logger.error(e);
	    }
	}
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#read()
     */
    @Override
	public int read() throws IOException {
	int dataChunk = 0;
	if (isMultipartRequest) {
	    if (transformedXML != null) {
		dataChunk = transformedXML.read();
	    }
			
	    if (((transformedXML == null) ^ (dataChunk < 0)) && (cachedXmlLineReader != null)) { // ^ is for exclusive OR (XOR)
		dataChunk = cachedXmlLineReader.read();
		transformedXML = null;
	    }
			
	    if (((transformedXML == null) && (cachedXmlLineReader == null)) ^ (dataChunk < 0)) { // ^ is for exclusive OR (XOR)
		dataChunk = rawRequest.read();
		transformedXML = null;
		cachedXmlLineReader = null;
	    }
			
	    if (!stopParsing) { // No need to continue parsing if soap request has been encountered and transformed
		if ((dataChunk == '\n') && (previousDataChunk == '\r')) { // LF from CRLF, just pass it as is...
		    cachedLine = "";
		    if (isInXmlContent) { // ...unless this is the last char before XML content. (MIME uses CRLF for this)
			logger.debug("Launching SOAP XSLT transformation");
			XmlPartInputStream xmlStream = new XmlPartInputStream(this);
			try {
			    transformedXML = applyXSLTTransformation(xmlStream);
			} catch (Exception e) {
			    logger.warn("Could not apply XSLT stylesheet to incoming SOAP request.");
			    transformedXML = null;
			    stopParsing = true; // And don't retry!
			}
			xmlStream.close();
		    }
		}
		else if ((dataChunk >= 0) && (dataChunk != '\r') && (dataChunk != '\n')){
		    // We're on a line (possibly text). Try to construct a String (may be garbage if binary)
		    cachedLine = cachedLine+(char)dataChunk;
		}
		else if (dataChunk < 0) {
		    // There's no more data. Clean some variables before returning -1.
		    cachedLine = ""; // This data is no more necessary
		}
		else { // This is \r or \n, see what we can do with this line
		    logger.trace("SOAP request line finished with content: "+cachedLine+(char)dataChunk);
					
		    if (multipartBoundary.length() == 0) {
			// Multipart but no boundary yet, find it!
			if (cachedLine.length() > 0) { // sometimes there's an empty line at the beginning. boundary is first line with content
			    multipartBoundary = cachedLine;
			    logger.debug("Found boundary for multipart request: "+multipartBoundary);
			    isInMultipartSectionHeader = true;
			}
		    }
		    else if (!isInMultipartSectionHeader && multipartBoundary.equals(cachedLine)) {
			// Multipart and boundary known + we are getting into a new part of the request.
			isInMultipartSectionHeader = true;
			isInXmlContentHeader = false;
			logger.debug("Getting into a part header of a multipart request");
		    }
		    else if (isInMultipartSectionHeader && (cachedLine.length() == 0)) {
			// We're in the header of a MIME part, but we found an empty line. This means it's the end of the header.
			isInMultipartSectionHeader = false;
			if (isInXmlContentHeader) {
			    logger.debug("Leaving an XML part header of a multipart request");
			    isInXmlContentHeader = false;
			    isInXmlContent = true;
			    // We launch XML conversion only after next read() call because when we get there, we have a \r but not yet the \n (which should not be send to transformer or it will fail)
			}
			else
			    logger.debug("Leaving a part header of a multipart request");
		    }
		    else if (isInMultipartSectionHeader && ((cachedLine.indexOf("Content-Type: text/xml") >= 0) || (cachedLine.indexOf("application/xop+xml") >= 0))) {
			// We're in the header of a MIME part, and it seems this part is xml. This is time to launch xslt transformation!
			logger.debug("Found an XML part to try to transform in incoming multipart request!");
			isInXmlContentHeader = true;
		    }
					
		    cachedLine = "";
		}
				
		previousDataChunk = dataChunk;
	    }
	}
	else { // Not multipart
	    if (transformedXML != null) {
		dataChunk = transformedXML.read();
	    }
	    else { // This happens when transformation fails
		dataChunk = rawRequest.read();
	    }
	}
		
	return dataChunk;
    }
	
    /**
     * Read xml data from this stream (stops when xml finished).
     * 
     * @return the data
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public int readXml() throws IOException {
	int dataChunk;
		
	if (isMultipartRequest) {
	    if (multipartBoundary.length() == 0) {
		logger.warn("Asking Xml content from multipart request without knowing boundary.");
		return -1;
	    }
			
	    if (!isInXmlContent) {
		return -1;
	    }
			
	    try {
		fillXmlCache();
	    } catch (Exception e) {
		return -1; // Problem filling cache, this probably means the request is completed
	    }
	
	    dataChunk = cachedXmlLineReader.read();
			
	    if (dataChunk < 0) {
		cachedXmlLine = ""; // Line finished, empty the line cache...
		cachedXmlLineReader.close();
		// ...and try to look at the following line
		try {
		    fillXmlCache();
		} catch (Exception e) {
		    return -1; // Problem filling cache, this probably means the request is completed
		}
				
		// We're here, so cache must have been filled. Try rereading
		dataChunk = cachedXmlLineReader.read();
	    }
	}
	else { // This is not multipart, simply use the whole request content
	    dataChunk = rawRequest.read();
	}
		
	return dataChunk;
    }
	
    /**
     * Fill Xml cache.
     * 
     * @throws Exception the exception thrown when there's no more data to eat
     */
    private void fillXmlCache() throws Exception {
	if (cachedXmlLine.length() == 0) {
	    int dataChunk = rawRequest.read();
			
	    if (dataChunk < 0) {
		// No more data. Return some EOL char(s) that may still be in cache
		cachedXmlLine = cachedXmlEOL;
		cachedXmlEOL = "";
	    }
			
	    while (dataChunk >= 0) {

		if ((dataChunk != '\r') && (dataChunk != '\n')) { // Normal text content
		    // We're on a line (possibly text). Try to construct a String
		    cachedXmlLine = cachedXmlLine+(char)dataChunk;
		}
		else { // Found an end of line (CR or LF).
		    if ((cachedXmlEOL.length() >= 2) // Another \r or \n after \r\n = empty line
			|| (cachedXmlEOL.equals(Character.toString((char)dataChunk)))// another \r after \r or another \n after \n = empty line
			|| (!cachedXmlEOL.endsWith("\n") && !cachedXmlEOL.endsWith("\r"))) { // Arriving at the end of line containing normal chars 
			cachedXmlLine = cachedXmlEOL+cachedXmlLine; // Prepend the previous end of line chars to this line
			cachedXmlEOL = ""; // And delete cache of previous end of line chars
		    }

		    if (isMultipartRequest && (cachedXmlLine.indexOf(multipartBoundary) >= 0) && !isInMultipartSectionHeader) {
			// Multipart and boundary known + we are getting into a new part of the request.
			isInMultipartSectionHeader = true;
			isInXmlContentHeader = false;
			logger.debug("Getting into a new part header: Xml part is finished!");
			cachedXmlLine = cachedXmlLine+(char)dataChunk; // We won't get back here so don't use cachedXmlEOL
			cachedXmlEOL = "";
			cachedXmlLineReader = new StringReader(cachedXmlLine);
			isInXmlContent = false;
			throw new Exception("No more Xml data in xml part of http request.");
		    }
		    else {// End of line in the middle of xml content, consider the cache is full.
			cachedXmlEOL = cachedXmlEOL+(char)dataChunk; // Begin to repopulate the cache of previous end of line chars
			break;
		    }
		}
		dataChunk = rawRequest.read();
	    }
			
			if (dataChunk < 0) {
				// This happens when reaching the end of request
				// If there's no EOL char after last boundary, we have to detect boundary here!
				if (isMultipartRequest && (cachedXmlLine.indexOf(multipartBoundary) >= 0) && !isInMultipartSectionHeader) {
					// Multipart and boundary known + we are getting into a new part of the request.
					isInMultipartSectionHeader = false;
					isInXmlContentHeader = false;
					logger.debug("End of the request: Xml part is finished!");
					cachedXmlLine = cachedXmlEOL+cachedXmlLine; // Prepend the previous end of line chars to this line
					cachedXmlEOL = "";
					cachedXmlLineReader = new StringReader(cachedXmlLine);
					isInXmlContent = false;
					throw new Exception("No more Xml data in xml part of http request.");
				}
			}

	    logger.trace("New Xml content in cache containing: "+cachedXmlLine);
	    cachedXmlLineReader = new StringReader(cachedXmlLine);
	}
    }

    /**
     * Apply xslt transformation.
     * 
     * @param inputXmlStream the input xml stream
     * 
     * @return a Reader containing transformed XML
     * 
     * @throws TransformerFactoryConfigurationError the transformer factory configuration error
     * @throws Exception the exception
     */
    private BufferedReader applyXSLTTransformation(InputStream inputXmlStream) throws TransformerFactoryConfigurationError, Exception {
	// Do the XML stuff
	InputStream xsltStream = getClass().getResourceAsStream("/xslt/typedSOAP2OpalSOAP.xsl");
	if (xsltStream == null) {
	    logger.error("The XSLT file could not be found.");
	    throw new Exception("The XSLT file could not be found.");
	}

	TransformerFactory transfoFact = TransformerFactory.newInstance();
	StreamSource stylesource = new StreamSource(xsltStream);
	Transformer transformer = transfoFact.newTransformer(stylesource);

	transformer.setOutputProperty(OutputKeys.INDENT, "no");
	transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		
	Source source;
	source = new StreamSource(inputXmlStream);

	StringWriter writer = new StringWriter();
	StreamResult result = new StreamResult(writer);
		
	if (("").equals(serviceName)) {
	    logger.error("Unknown service name.");
	    throw new Exception("Unknown service name.");
	}
		
	String serviceConfigFilePath = getServiceConfigFilePath(serviceName);
	if (serviceConfigFilePath != null) {
	    transformer.setParameter("configPath", serviceConfigFilePath);
	    transformer.transform(source, result);
	}
	else {
	    logger.warn("No config file found for asked service. Aborting XSLT transformation.");
	    throw new Exception("No config file found for asked service. Aborting XSLT transformation.");
	}
		
	writer.flush();
	writer.close();
	xsltStream.close();

	logger.debug("Transformation of incoming SOAP request finished with result in string: "+writer.toString());
		
	String resultStr = writer.toString();
		
	if (resultStr.trim().endsWith("Envelope>")) {
	    logger.debug("SOAP request successfully transformed. Stopping parsing of whole request.");
	    stopParsing = true;
	}
		
	BufferedReader transformedResult = new BufferedReader(new StringReader(resultStr));

	return transformedResult;
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#close()
     */
    @Override
	public void close() throws IOException {
	rawRequest.close();
	if (transformedXML != null)
	    transformedXML.close();
	if (cachedXmlLineReader != null)
	    cachedXmlLineReader.close();
    }

    @Override
	public long skip(long n) throws IOException {
	throw new IOException("Skip is not supported by SoapInputStream.");
    }

    /**
     * Gets the service config file path.
     * 
     * @param serviceName the service name
     * 
     * @return the service config file path
     */
    private String getServiceConfigFilePath(String serviceName) {
        String configFileName = null;
        try {
	    String path = context.getRealPath("/")+"WEB-INF"+File.separator+"server-config.wsdd";

	    // Creation de la source DOM
	    DocumentBuilderFactory docBFact = DocumentBuilderFactory.newInstance();
	    docBFact.setNamespaceAware(true);
	    DocumentBuilder docBuilder = docBFact.newDocumentBuilder();

	    Reader reader = new FileReader(path);
	    Document document = docBuilder.parse(new org.xml.sax.InputSource(reader));
	        
	    // XPath to find service config file path
	    XPathFactory xpathFact = XPathFactory.newInstance();
	    XPath xpath = xpathFact.newXPath();
			
	    // Namespace context for xpath
	    NamespaceContext namespace = new NamespaceContext(){
		    public String getNamespaceURI(String prefix){
			if(prefix.equals("dd")){
			    return wsddNameSpace;
			}else{
			    return "";
			}
		    }
		    public String getPrefix(String namespaceURI){
			if(namespaceURI.equals(wsddNameSpace)){
			    return "dd";
			}else{
			    return "";
			}
		    }
		    public Iterator<String> getPrefixes(String namespaceURI){
			List<String> list = new ArrayList<String>();

			if (namespaceURI.equals(wsddNameSpace)) {
			    list.add("dd");
			}
					
			return list.iterator();
		    } 
		};
	    xpath.setNamespaceContext(namespace);
			
	    // evaluate xpath
	    XPathExpression exp = xpath.compile("/dd:deployment/dd:service[@name='"+serviceName+"']/dd:parameter[@name='appConfig']/@value");
	    configFileName = (String) exp.evaluate(document, XPathConstants.STRING);
			
	    if (configFileName == null) {
		logger.error("Required parameter appConfig not found in WSDD");
		return null;
	    }
	        
	}
        catch(XPathExpressionException xpee){
	    logger.error(xpee);
	} catch (Exception e) {
	    logger.warn("We could not get the service list from the Axis Engine...");
	    logger.warn(e);
        }
        return configFileName;
    }

}
