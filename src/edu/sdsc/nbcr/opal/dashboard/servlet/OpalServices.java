// GamaServices.java
//
// Servlet that publishes an Atom feed with the available Opal services
//
// 11/28/07   - created, Luca Clementi
//


package edu.sdsc.nbcr.opal.dashboard.servlet;

import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;

import org.apache.axis.message.SOAPBodyElement;
import org.apache.axis.transport.http.AxisServletBase;
import org.apache.axis.AxisEngine;
import org.apache.axis.AxisProperties;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.description.ServiceDesc;
import org.apache.axis.ConfigurationException;
import org.apache.axis.description.OperationDesc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

import edu.sdsc.nbcr.opal.gui.common.GetServiceListHelper;
import edu.sdsc.nbcr.opal.gui.common.Constants;
import edu.sdsc.nbcr.opal.gui.common.OPALService;
import edu.sdsc.nbcr.opal.AppServiceImpl;
import edu.sdsc.nbcr.opal.AppConfigType;
import edu.sdsc.nbcr.common.TypeDeserializer;
import edu.sdsc.nbcr.opal.AppMetadataType;

/** 
 *
 * This class implements the creation of the Atom Feed
 * 
 * @author clem
 *
 */
public class OpalServices extends HttpServlet {

    protected static Log log = LogFactory.getLog(OpalServices.class.getName());
    private static final String FEED_TYPE = "atom_1.0";
    private static final String MIME_TYPE = "application/xml; charset=UTF-8";
    //this are for Axis internal props
    public static final String OPTION_SERVER_CONFIG_FILE = "axis.ServerConfigFile";
    protected static final String SERVER_CONFIG_FILE = "server-config.wsdd";




    public final void init(ServletConfig config) throws ServletException {
        super.init(config);
        log.info("Loading OpalServices (init method).");
    }



    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        processRequest(req, res);
    }

    /**
     * @see #doGet
     */
    public final void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        doGet(req, res);
    }

    /**
     * Both doGet and goPort call this function that actually does the processing
     * 
     * @throws IOException
     * @throws ServletException
     */
    public void processRequest(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        //String command = req.getParameter("command");
        //get the services list
        OPALService [] servicesList = getServiceList(res);
        if (servicesList == null){
            String msg = "Unable to get the service list from the Axis...";
            log.error(msg);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            return;
        }

        //let's create the feed and send it back
        try {
            SyndFeed feed = getFeed(servicesList);
            feed.setFeedType(FEED_TYPE);
            res.setContentType(MIME_TYPE);
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed,res.getWriter());
        }
        catch (FeedException ex) {
            String msg = "Unable to generate the feed...";
            log.error(msg,ex);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,msg);
            return;
        }

    }

    private SyndFeed getFeed(OPALService [] servicesList){

        //get the last update of the server-config.xml
        String serverConfigFile = AxisProperties.getProperty(OPTION_SERVER_CONFIG_FILE, SERVER_CONFIG_FILE);
        Date lastUpdate = new Date((new File(serverConfigFile)).lastModified());

        SyndFeed feed = new SyndFeedImpl();
        feed.setTitle("Opal Service List");
        feed.setLink(AppServiceImpl.getOpalBaseURL());
        feed.setDescription("This feed lists the available services on the Opal server " + AppServiceImpl.getOpalBaseURL());
        feed.setPublishedDate(lastUpdate);

        List entries = new ArrayList();
        SyndEntry entry;
        SyndContent description;

        for (int i = 0; i < servicesList.length; i++) {
            //populate the entry
            entry = new SyndEntryImpl();
            entry.setTitle(servicesList[i].getServiceName());
            entry.setLink(servicesList[i].getURL());
            //entry.setPublishedDate(lastUpdate);
            description = new SyndContentImpl();
            description.setType("text/plain");
            description.setValue(servicesList[i].getDescription());
            entry.setDescription(description);
            entries.add(entry);
        }//for
        
        feed.setEntries(entries);
        return feed; 
    }//getFeed


    /**
     * this function return a list of the Opal services currently deployed...
     */
    private OPALService [] getServiceList(HttpServletResponse res){
        String url = getServletContext().getInitParameter("OPAL_URL");
        if ( url == null ) {
            log.warn("OPAL_URL not found in web.xml. Using default.");
            url = Constants.OPALDEFAULT_URL;
        }
        GetServiceListHelper helper = new GetServiceListHelper();
        helper.setBasePrivateURL(url);

        String publicUrl = null;
        String tomcatUrl = null;
        tomcatUrl = OpalInfoServlet.getTomcatUrl();
        URL tempURL = null;
        try {tempURL = new URL(url);}
        catch (Exception e){ 
            
        }

        if ( (tempURL != null) && (tomcatUrl != null)){
            helper.setBasePublicURL(tomcatUrl + tempURL.getFile());
        }else{
            helper.setBasePublicURL(url);
        }

        SOAPBodyElement list = helper.getServiceList();
        if ( list == null ) {
            returnServiceError(res, "Unable to get the service list from the server");
            return null;
        }
        OPALService [] servicesList = helper.parseServiceList(list.toString());
        if ( servicesList == null ) {
            returnServiceError(res, "Unable to parse the service list from the server");
            return null;
        }
        if ( ! helper.setServiceName(servicesList) ) {
            returnServiceError(res, "An error occurred when trying to the services names");
            return null;
        }
        return servicesList;
    /*
        This code doesn't work. For some weird reason Axis cache the list 
        of services and doesn't update then dinamically when new services 
        are deployed or undeployed.... :-(

        So Im forced to rollback to the AdminService for getting the list of services

        //the base URL of the axis services 
        //TODO get this value from the axis engine...getOpalBaseURL
        String baseEndpointURL = AppServiceImpl.getOpalBaseURL() + "/services/";
        AxisEngine engine = null;
        Iterator i;
        try { 
            engine = AxisServletBase.getEngine(this); 
            i = engine.getConfig().getDeployedServices();
        }
        catch (Exception e) {log.error("We could not get the service list from the Axis Engine...", e); return null;}
        List services = new ArrayList();
        while (i.hasNext()) {
            ServiceDesc sd = (ServiceDesc) i.next();
            String name = sd.getName();
            String endpointURL = baseEndpointURL + name;
            //verify if it is an opal service
            boolean isOpalService =false;
            ArrayList operations = sd.getOperations();
            if (!operations.isEmpty()) {
                for (Iterator it = operations.iterator(); it.hasNext(); ) {
                    OperationDesc desc = (OperationDesc) it.next();
                    if ( desc.getName().equals("getAppMetadata") ) isOpalService = true;
                }
            }
            if ( isOpalService == false ) continue; //this is not a Opal service go to the next service
            //ok we have a Opal service let's get the AppMetadata
            SOAPService serv;
            try { serv = engine.getService(name); }
            catch (Exception e) { 
                log.error("Unable to get service description.", e);
                return null;
            }
            serv.setServiceDescription(sd);
            name = serv.getName();
            String configFileName = (String) serv.getOption("appConfig");
            if (configFileName == null) {
                log.error("Required parameter appConfig not found in WSDD");
                return null;
            }
            AppConfigType config = null;
            try {
                config = (AppConfigType) TypeDeserializer.getValue(configFileName, new AppConfigType());;
            } catch (Exception e) {
                String msg = "Can't read application configuration from XML for service: " + name;
                log.error(msg, e);
                return null;
            }
            AppMetadataType metadata = config.getMetadata();
            String serviceName = metadata.getAppName();
            String description = metadata.getUsage();
            //ok now we have everything let's populate the opalService
            OPALService opalService = new OPALService();
            opalService.setServiceID(name);
            opalService.setURL(endpointURL);
            opalService.setDescription(description);
            if ( serviceName != null ) {
                opalService.setServiceName(serviceName);
            }else {
                // if the service name is not specified let's use the service ID
                opalService.setServiceName(name);
            }
            //is is a complex or simple submission form
            if ( (metadata.getTypes() == null) || ((metadata.getTypes().getTaggedParams() == null) && (metadata.getTypes().getUntaggedParams() == null)) )
                opalService.setComplexForm(Boolean.FALSE);
            else
                opalService.setComplexForm(Boolean.TRUE);
            //we are done! let's log and add the service to the return results
            services.add(opalService);
            log.info("the opalService is: " + opalService);
        }

        OPALService [] servicesList =(OPALService []) services.toArray(new OPALService [services.size()]);

        return servicesList;
        */
    }//getServiceList

    static private void returnServiceError(HttpServletResponse res, String error){
        //TODO implement some error handling
        log.error("Unable to create the service list: " + error);
        try { res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR ); }
        catch (Exception e) {
            log.error("Unable to return the error page.", e);
        }
        return;
    }

}
