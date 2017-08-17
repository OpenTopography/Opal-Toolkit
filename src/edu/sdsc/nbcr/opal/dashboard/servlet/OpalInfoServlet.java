// OpalInfoServlet.java
//
// Servlet that implements the opal dashboard
//
// 11/28/07   - created, Luca Clementi
//


package edu.sdsc.nbcr.opal.dashboard.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.RequestDispatcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.sdsc.nbcr.opal.dashboard.persistence.DBManager;
import edu.sdsc.nbcr.opal.dashboard.util.DateHelper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.Character;

/** 
 *
 * This class implements all the business logic behind the Opal dashboard.
 * 
 * @author clem
 *
 */
public class OpalInfoServlet extends HttpServlet {

    protected static Log log = LogFactory.getLog(OpalInfoServlet.class.getName());
    private static final String SERVERINFO_JSP = "/dashboard-jsp/serverInfo.jsp";
    private static final String STATISTICS_JSP = "/dashboard-jsp/statistics.jsp";
    private static final String SYSINFO_JSP = "/dashboard-jsp/sysinfo.jsp";
    private static final String DOC_JSP = "/dashboard-jsp/documentation.jsp";
    private static final String HOME_JSP = "/dashboard-jsp/home.jsp";
    private static final String SERVICELIST_JSP = "/dashboard-jsp/serviceList.jsp";
    
    //private static final String ERROR_JSP = "/dashboard-jsp/error.jsp";
    //this is used only when the DB is not available
    private static final String ERROR_JSP = "/dashboard-jsp/statistics_noDB.jsp";

    private String opalVersion = null;
    private String opalDataLifetime = null;
    private String opalUptimeCommand = "uptime";
    private String opalBuildDateCommand = "uname -a";
    private String opalWebsite = null;
    private String opalDocumentation = null;
    private String opalJobManager = null;
    private static String tomcatUrl = null;
    private String opalUrl = null;
    private Boolean dbUsed = Boolean.FALSE;
    private DBManager dbManager = null;
    

    public final void init(ServletConfig config) throws ServletException {
        super.init(config);
        log.info("Loading OpalInfoServlet (init method).");
        try {
            opalVersion = config.getServletContext().getInitParameter("OPAL_VERSION");
            opalBuildDateCommand = config.getServletContext().getInitParameter("OPAL_BUILDDATE_COMMAND");
            opalUptimeCommand = config.getServletContext().getInitParameter("OPAL_UPTIME_COMMAND");
            opalWebsite = config.getServletContext().getInitParameter("OPAL_WEB_SITE");
            opalDocumentation = config.getServletContext().getInitParameter("OPAL_DOC");
            opalUrl = config.getServletContext().getInitParameter("OPAL_URL");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An error occurred while initializing the OpalInfoServlet, impossible to load web.xml: " + e.getMessage());
            dbManager = new DBManager();
            return;
        }
        
        
        //-------     initializing the DB connection    -----
        java.util.Properties props = new java.util.Properties();
        String propsFileName = "opal.properties";
        String databaseUrl = null;
        boolean initialized = false;
        try {
            // load runtime properties from properties file
            props.load(PlotterServlet.class.getClassLoader().getResourceAsStream(propsFileName));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Impossible to load opal.properties: " + e.getMessage());
            dbManager = new DBManager();
            //pointless to go on!!
            return;
        }
        
        
        //getting some more informations
        if ( props.getProperty("opal.jobmanager") != null ) {
            opalJobManager = props.getProperty("opal.jobmanager");
        }
        if (props.getProperty("opal.datalifetime") != null) {
            opalDataLifetime = props.getProperty("opal.datalifetime");
        } else opalDataLifetime = null;
        
        if (props.getProperty("tomcat.url") != null ) {
            tomcatUrl = props.getProperty("tomcat.url");
        }
        //not necessary anymore we use hibernate
        dbManager = new DBManager();
        config.getServletContext().setAttribute("dbManager", dbManager);
        initialized = true;
        
    }

    /**
     * 
     * @param req the <code>HttpServletRequest</code>
     * @param res the <code>HttpServletResponse</code>
     * @throws java.io.IOException if an I/O error occurs
     * @throws javax.servlet.ServletException if a servlet error occurs
     */
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
    public void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        String command = req.getParameter("command");
        RequestDispatcher dispatcher;
       
        if (dbManager == null ) {
            String errorMsg = "There is a problem conecting to the Data base.<br/>";
            log.error("We had an error: " + errorMsg);
            req.setAttribute("error", errorMsg);
            dispatcher = getServletContext().getRequestDispatcher(ERROR_JSP);
            try { dispatcher.forward(req, res); }
            catch (Exception e ) {
               log.error("Impossible to forward to the error page...Don't know what else I can do....", e);
            }
            return;
        }


        if ("statistics".equals(command)) {
            //let's check if the DB connection is OK
            if ( (dbManager == null ) || ( !dbManager.isConnected()) ) {
                String errorMsg = "The connection to the Data Base is not present.<br/> " +
                        "Either you are not using a DB or there are some problem in your configuration file. <br/>" +
                        "Please have a look at the opal WEB_INF/web.xml and WEB-INF/classes/opal.properties. <br/>";
                log.error("We had an error: " + errorMsg);
                req.setAttribute("error", errorMsg);
                dispatcher = getServletContext().getRequestDispatcher(ERROR_JSP);
                try { dispatcher.forward(req, res); }
                catch (Exception e ) {
                   log.error("Impossible to forward to the error page...Don't know what else I can do....", e);
                }
                return;
            }
            //Begin and end date
            String startDateStr = req.getParameter("startDate");
            String endDateStr = req.getParameter("endDate");
            try { 
              DateHelper.parseDate(startDateStr);
              DateHelper.parseDate(endDateStr);
            } catch (java.text.ParseException e) {
                // the user typed some crap let's use the default
                endDateStr = DateHelper.formatDate( DateHelper.getEndDate() );
                startDateStr = DateHelper.formatDate( DateHelper.getStartDate() );
            }
            req.setAttribute("startDate", startDateStr);
            req.setAttribute("endDate", endDateStr);
            
            //list of services to display
            String [] servicesName = dbManager.getServicesList();
            String [] servicesNameSelected = req.getParameterValues("servicesName"); 
            if ( servicesNameSelected == null){
                log.info("there was no service name selected in the request parameters, selecting all of them");
                servicesNameSelected = dbManager.getServicesList();
            }
            req.setAttribute("servicesNameSelected", servicesNameSelected);
            req.setAttribute("servicesName", servicesName);
            dispatcher = getServletContext().getRequestDispatcher(STATISTICS_JSP);
            dispatcher.forward(req, res);
        } else if ("sysinfo".equals(command)) {
            //this doesn't exist anymore... Now there is the opal GUI
            dispatcher = getServletContext().getRequestDispatcher(SYSINFO_JSP);
            dispatcher.forward(req, res);
        } else if ("docs".equals(command)) {
            //this doesn't exist anymore... Now there is the opal GUI
            //res.sendRedirect(opalDocumentation);
            req.setAttribute("opalDocumentation", opalDocumentation);
            req.setAttribute("opalWebsite", opalWebsite);
            dispatcher = getServletContext().getRequestDispatcher(DOC_JSP);
            dispatcher.forward(req, res);
        } else if ("home".equals(command)) {
            req.setAttribute("opalWebsite", opalWebsite);
            dispatcher = getServletContext().getRequestDispatcher(HOME_JSP);
            dispatcher.forward(req, res);
        } else if ("serviceList".equals(command)) {
            req.setAttribute("tomcatUrl", tomcatUrl);
	        req.setAttribute("opalUrl", opalUrl);
            dispatcher = getServletContext().getRequestDispatcher(SERVICELIST_JSP);
            dispatcher.forward(req, res);
        } else if ("serverInfo".equals(command)) {
            // need to gather a bunch of information regarding the opal
            // installation
            req.setAttribute("systemIPAddress", req.getLocalAddr());
            req.setAttribute("systemUptime", getUptime());
            req.setAttribute("systemBuildDate", getBuildDate());
            req.setAttribute("opalVersion", opalVersion);
            req.setAttribute("opalWebsite", opalWebsite);
            req.setAttribute("opalDocumentation", opalDocumentation);
            //req.setAttribute("dbURL", dbManager.getDatabaseUrl());
            //req.setAttribute("dbUsername", dbManager.getDbUserName());
            req.setAttribute("dbDriver", dbManager.getDriver());
            
            req.setAttribute("submissionSystem", opalJobManager );
            req.setAttribute("opalDataLifetime", opalDataLifetime);
            
            dispatcher =
			getServletContext().getRequestDispatcher(SERVERINFO_JSP);
            dispatcher.forward(req, res);
        } else {
            req.setAttribute("opalWebsite", opalWebsite);
            dispatcher = getServletContext().getRequestDispatcher(HOME_JSP);
            dispatcher.forward(req, res);
        }
    }

    /**
     * exec the command on the local system
     * @param command the command to be executed 
     * @param error the log string to be printed in case of error
     * @return the output of the command
     */
    public String exec(String command, String error) {
        String r = new String();
        try {
            Process child = Runtime.getRuntime().exec(command);
            child.waitFor();
            if (child.exitValue() != 0) {
                // error
                r = new String(error);
            } else {
                InputStream in = child.getInputStream();
                int c;
                while ((c = in.read()) != -1) {
                    r = r.concat(Character.toString((char) c));
                }
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            r = new String(error);
        }
        log.debug("Exec: command = " + command + " result = " + r);
        return r;
    }
	
    public String getUptime() {
        return exec(opalUptimeCommand, "error, unable to determine uptime");
    }

    public String getBuildDate() {
        return exec (opalBuildDateCommand, "error, unable to determine build date");
    }

    public static String getTomcatUrl(){
        return tomcatUrl;
    }
	
    public void destroy() {
        dbManager.close();
    }

}
