package edu.sdsc.nbcr.opal.dashboard.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

import edu.sdsc.nbcr.opal.gui.common.GetServiceListHelper;
import edu.sdsc.nbcr.opal.gui.common.Constants;
import edu.sdsc.nbcr.opal.gui.common.OPALService;
import edu.sdsc.nbcr.opal.AppServiceSkeleton;

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
        }

    }

    private SyndFeed getFeed(OPALService [] servicesList){

        Date lastUpdate = new Date();
        SyndFeed feed = new SyndFeedImpl();
        feed.setTitle("Opal Service List");
        feed.setLink(AppServiceSkeleton.getOpalBaseURL());
        feed.setDescription("This feed lists the available services on the Opal server " + AppServiceSkeleton.getOpalBaseURL());
        feed.setPublishedDate(lastUpdate);

        List<SyndEntry> entries = new ArrayList<>();
        SyndEntry entry;
        SyndContent description;

        for (OPALService aServicesList : servicesList) {
            //populate the entry
            entry = new SyndEntryImpl();
            entry.setTitle(aServicesList.getServiceName());
            entry.setLink(aServicesList.getURL());
            //entry.setPublishedDate(lastUpdate);
            description = new SyndContentImpl();
            description.setType("text/plain");
            description.setValue(aServicesList.getDescription());
            entry.setDescription(description);
            entries.add(entry);
        }

        feed.setEntries(entries);
        return feed;
    }


    /**
     * this function return a list of the Opal services currently deployed...
     */
    private OPALService [] getServiceList(HttpServletResponse res){
        String url = getServletContext().getInitParameter("OPAL_URL");
        if ( url == null ) {
            log.warn("the OPAL_URL was not found in the WEB-INF/web.xml file.\nUsing the default...");
            url = Constants.OPALDEFAULT_URL;
        }
        GetServiceListHelper helper = new GetServiceListHelper();
        helper.setServiceUrl(url);

        OPALService[] servicesList = helper.getServiceList();
        if ( servicesList == null ) {
            returnServiceError(res, "Unable to get the service list from the server");
            return null;
        }
        if ( ! helper.setServiceName(servicesList) ) {
            returnServiceError(res, "An error occurred when trying to the services names");
            return null;
        }
        return servicesList;

    }

    static private void returnServiceError(HttpServletResponse res, String error){
        //TODO implement some error handling
        log.error("Unable to create the service list: " + error);
        try { res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR ); }
        catch (Exception e) {
            log.error("Unable to return the error page.", e);
        }
    }

}
