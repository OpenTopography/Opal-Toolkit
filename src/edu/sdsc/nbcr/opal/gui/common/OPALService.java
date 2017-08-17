package edu.sdsc.nbcr.opal.gui.common;

/**
 * This class is used to hold the list of services that are available on the server.
 * 
 * This class has the following fields:
 * <ul>
 * <li>String serviceName - the name of this service
 * <li>String serviceID - the name of this service as in the URL of the deployed service
 * <li>String URL - the URL to reach this services
 * <li>String description - a textual description of this service
 * <li>Boolean complexForm - true if this service supports the advanced submission form
 * </ul>
 * 
 * @author clem
 *
 */
public class OPALService {
    
    private String serviceName;
    private String serviceID;
    private String URL;
    //textual description of the service 
    private String description;
    //true if this serivce support the complex submission form
    private Boolean complexForm;

    /**
     * default constructor
     */
    public OPALService(){
    	URL = null;
    	serviceName = null;
    	serviceID = null;
    }
    
    /**
     * see at the top of the page for the input parameters
     */
    public OPALService(String serviceName, String url, String serviceID) {
        this.serviceName = serviceName;
        URL = url;
        this.serviceID = serviceID;
    }
    
    /**
     * textual representation of this object
     */
    public String toString(){
        return "Service name: " + serviceName + " URL: " + URL + " ID: " + serviceID;
    }
    
    
    
    /** 
     * Return a string containing a debugging of the OPALSerivce
     * array.
     * 
     */
    public static String arrayToString(OPALService [] service) {
        String ret = new String();
        for (int i = 0; i < service.length; i++ ){
            ret += service[i] + "\n";
        }
        return ret;
    }//arrayToString

    /**
     * @return the serviceName
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * @param serviceName the serviceName to set
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * @return the serviceID
     */
    public String getServiceID() {
        return serviceID;
    }

    /**
     * @param serviceID the serviceID to set
     */
    public void setServiceID(String serviceID) {
        this.serviceID = serviceID;
    }

    /**
     * @return the uRL
     */
    public String getURL() {
        return URL;
    }

    /**
     * @param url the uRL to set
     */
    public void setURL(String url) {
        URL = url;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the complexForm
     */
    public Boolean getComplexForm() {
        return complexForm;
    }

    /**
     * @param complexForm the complexForm to set
     */
    public void setComplexForm(Boolean complexForm) {
        this.complexForm = complexForm;
    }

}
