package edu.sdsc.nbcr.opal.gui.common;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.rpc.ServiceException;

import org.apache.axis.message.SOAPBodyElement;
import org.apache.axis.client.AdminClient;
import org.apache.axis.client.Call;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.deployment.wsdd.WSDDConstants;
import org.apache.axis.deployment.wsdd.WSDDDeployment;
import org.apache.axis.deployment.wsdd.WSDDService;
import org.apache.commons.logging.Log;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import edu.sdsc.nbcr.opal.AppMetadataInputType;
import edu.sdsc.nbcr.opal.AppMetadataType;
import edu.sdsc.nbcr.opal.AppServiceLocator;
import edu.sdsc.nbcr.opal.AppServicePortType;


/**
 * this class is in charge of getting the list of service from an opal service
 * Just use this code:
 * 
 * GetServiceListHelper helper = new GetServiceListHelper();
 * helper.setBaseURL(url);
 * SOAPBodyElement list = helper.getServiceList();
 * OPALService [] servicesList = helper.parseServiceList(list.toString());
 * helper.setGetSerivceName(serviceList);
 * 
 * @author clem
 *
 */
public class GetServiceListHelper {

    protected static Log log = LogFactory.getLog(GetServiceListHelper.class.getName());
    protected Call call;
    private String baseURL;
    private String basePrivateURL;
    private String basePublicURL;
	
    /**
     * default constructor
     */
    public GetServiceListHelper(){
    	org.apache.axis.client.Service service = new org.apache.axis.client.Service();
    	try { call = (Call) service.createCall(); }
    	catch (ServiceException e) {
    		log.error("Unable to instantiate the Call", e);
    		call = null;
    	}
    }
	
    /**
     * process the options then invoke the service
     */
    public SOAPBodyElement getServiceList()  {
    	if (call == null ){
    		//something went wrong
    		return null;
    	}
        call.setTargetEndpointAddress( basePrivateURL + "/AdminService" );
        //call.setUsername( "");
        //call.setPassword( password );
        //if(transportName != null && !transportName.equals("")) {
        //call.setProperty( Call.TRANSPORT_NAME, transportName );
        //}
        log.debug( "Getting the list of services." );
        call.setUseSOAPAction( true);
        call.setSOAPActionURI( "AdminService");
        
        
        String str = "<m:list xmlns:m=\"" + WSDDConstants.URI_WSDD + "\"/>";
        ByteArrayInputStream input = new ByteArrayInputStream(str.getBytes());
        

        Vector result = null ;
        Object[]  params = new Object[] { new SOAPBodyElement(input) };
        try { result = (Vector) call.invoke( params ); }
        catch (Exception e) {
        	log.error("Unable to invoke the service, maybe the target server is down", e);
        	return null;
        }
        if (result == null || result.isEmpty()) {
            log.error("The server returned an empty message (maybe you're not ivoking the right axis server???)!!");
            return null;
        }
        SOAPBodyElement body = (SOAPBodyElement) result.elementAt(0);
        
        return body;

    }

    /**
     * given the return value of the axis admin service it parses it and 
     * returns an array of OPALSerivce 
     * 
     */
    public OPALService[] parseServiceList(String body) {
        try {
            ArrayList list = new ArrayList();
            /* this does not work with the wsdd returned by axis, it seems it is incorrect... :-o 
        	WSDDDeployment deployment = new WSDDDeployment(element);
        	WSDDService [] services = deployment.getServices();
        	for (int i = 0; i < services.length; i++ ){
        		System.out.println("the service :" + services[i].getQName() + " e poi a: " + services[i].getServiceDesc().getName() );
        	*/
            
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            // let's add the header...
            body = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" + body;
            Document response = parser.parse(new InputSource(new StringReader(
                    body)));
            NodeList nl = response.getElementsByTagName("service");
            if ((nl == null) || (nl.getLength() < 1)) {
                // something went wrong
                log.error("the deployment descriptor does not have any elements!! Check the server.");
                return null;
            }
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if (isValidService(node)) {
                    NamedNodeMap attributes = node.getAttributes();
                    String serviceName = attributes.getNamedItem("name").getNodeValue();
                    OPALService service = new OPALService();
                    service.setURL(basePublicURL + "/" + serviceName);
                    service.setServiceID(serviceName);
                    service.setServiceName(serviceName);
                    list.add(service);
                    log.debug("added -> " + service);
                }
            }// for
            return (OPALService[]) list.toArray(new OPALService[list.size()]);
        } catch (Exception e) {
            //log.error("Something happen when parsing the list of services: " + e.getMessage(), e);
            e.printStackTrace();
            return null;
        }
    }
   

    public OPALService [] getOpalServiceList() {
        SOAPBodyElement list = this.getServiceList();
        if ( list == null ) {
            log.error("Unable to get the list of service.");
            return null;
        }
        return this.parseServiceList(list.toString());
    }
    
    /** 
     * I  check the the service node is a valid Opal service, to do this I 
     * just look that between the operations there is a getAppConfing operation
     * 
     * @param service
     * @return true if it is a valid Opal Serivce node
     */
    private static boolean isValidService(Node service){
    	NodeList ns = service.getChildNodes();
    	boolean isValid = false;
    	Node node = null;
    	if ( ns == null ) {
    		return false;
    	}
    	for ( int i = 0 ; i < ns.getLength(); i++ ){
    		node = ns.item(i);
    		if ( node.getNodeName().equals("operation") ) {
    			//I just check that among the service operations there is one called getAppConfig
        		if ( node.getAttributes().getNamedItem("name").getNodeValue().equals("getAppConfig") )
        			return true;
            }//if
    	}//for
    	return false;
    }

    /**
     * given the service list it check the metadata and if present it set change the service name 
     * with value contained appName attribute of the metadata tag
     * 
     * @param serviceList
     */
    public boolean setServiceName(OPALService [] serviceList){
        AppServiceLocator asl = new AppServiceLocator();
        AppServicePortType appServicePort;
        AppMetadataType amt;
        
        for (int i = 0; i < serviceList.length; i++ ) {
            String url = serviceList[i].getURL();
            
            try {
                appServicePort = asl.getAppServicePort(new URL(url));
                amt = appServicePort.getAppMetadata(new AppMetadataInputType());
            }catch (Exception e){
                log.error("Error retrieving the Service name for URL: " +  url, e);
                return false;
            }
            //setting general info
            String serviceName = amt.getAppName();
            String description = amt.getUsage();
            serviceList[i].setDescription(description);
            if ( serviceName != null ) {
                serviceList[i].setServiceName(serviceName);
                
            }else {
                // if the service name is not specified let's use the service ID
                serviceList[i].setServiceName(serviceList[i].getServiceID());
            }
            
            if ( (amt.getTypes() == null) || ((amt.getTypes().getTaggedParams() == null) && (amt.getTypes().getUntaggedParams() == null)) ) 
                serviceList[i].setComplexForm(Boolean.FALSE);
            else 
                serviceList[i].setComplexForm(Boolean.TRUE);
            
            
        }//for
        return true;
    }
    
    /**
     * Just for testing purpouse. not used
     * @param argv
     */
    public static void main(String [] argv){
    	GetServiceListHelper servicelist = new GetServiceListHelper();
    	servicelist.setBasePrivateURL("http://localhost:8080/axis/services");
        servicelist.setBasePublicURL("http://localhost:8080/axis/services");
    	SOAPBodyElement list = servicelist.getServiceList();

    	System.out.println("the result was: " + list.toString());
    	OPALService [] serviceList = servicelist.parseServiceList(list.toString());
    	
    	for (int i = 0; i < serviceList.length; i++){
    		System.out.println("the service " + i + " is: " + serviceList[i]);
    	}
    	
    }
    
    /**
     * This function set the private URL which is the URL used 
     * to query axis. This has to be localhost or Axis won't 
     * allow the connection.
     */
    public void setBasePrivateURL(String url){
    	basePrivateURL = url;
    }

    /**
     * This function set the Pubblic URL used to create the service 
     * URL when reporting the list of services.
     * Here there should be the pubblic IP address
     */
    public void setBasePublicURL(String url){
        basePublicURL = url;
    }
 
}
