package edu.sdsc.nbcr.opal.util;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.FilenameUtils;

import edu.sdsc.nbcr.opal.state.HibernateUtil;
import edu.sdsc.nbcr.opal.state.ServiceStatus;
import edu.sdsc.nbcr.opal.gui.common.OPALService;
import edu.sdsc.nbcr.opal.gui.common.GetServiceListHelper;
import edu.sdsc.nbcr.opal.gui.common.Constants;
import edu.sdsc.nbcr.opal.types.AppConfigType;

import java.io.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.Deflater;

/**
 * This class is a Tomcat Servlet which automatically deploys Opal
 * service descriptors when they are copyed inside the folder pointed
 * by opal.deploy.path.
 * <p>
 * This class uses apache commons to get notified of changes inside the
 * folder pointed by opal.deploy.path and it deply or undeploy AXIS services
 * as the files appear or disappear
 *
 * @author luca.clementi@gmail.com, Choonhan Youn
 */
public class OpalDeployService extends HttpServlet {

    protected static Log logger = LogFactory.getLog(OpalDeployService.class.getName());
    protected static Deployer deplo = null;
    protected static String wsddDirectory = null;
    protected static String serviceDirectory = null;

    public final void init(ServletConfig config) throws ServletException {
        super.init(config);

        logger.info("Loading OpalDeployService (init method).");

        //-------     initializing the DB connection    -----
        java.util.Properties props = new java.util.Properties();
        String propsFileName = "opal.properties";
        String axisServicesUrl;
        String deployPath = "";
        try {
            // load runtime properties from properties file
            props.load(OpalDeployService.class.getClassLoader().getResourceAsStream(propsFileName));
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Unable to load opal.properties: " + e.getMessage());
            //pointless to go on!!;
            return;
        }

        axisServicesUrl = getServletContext().getInitParameter("OPAL_URL");
        if (axisServicesUrl == null) {
            logger.warn("OPAL_URL not found in web.xml. Using default.");
            axisServicesUrl = Constants.OPALDEFAULT_URL;
        }

        if (props.getProperty("opal.deploy.path") != null) {
            deployPath = props.getProperty("opal.deploy.path");
        }
        if (deployPath.length() == 0) {
            logger.error("opal.deploy.path is not present! Fix your opal.properties.");
            return;
        }

        wsddDirectory = config.getServletContext().getRealPath("/WEB-INF/wsdd/");
        serviceDirectory = config.getServletContext().getRealPath("/WEB-INF/services/");
        File deployPathFile = new File(deployPath);
        if (!deployPathFile.isAbsolute()) {
            // make deploy path relative to CATALINA_HOME as specified in the documentation
            deployPathFile = new File(System.getProperty("catalina.home"), deployPath);
        }
        if (!deployPathFile.exists()) {
            //let's create it
            if (!deployPathFile.mkdirs()) {
                logger.error("Could not make directory specified by opal.deploy.path \"" + deployPathFile + "\"!");
                return;
            }
        } else if (!deployPathFile.isDirectory()) {
            logger.error("opal.deploy.path \"" + deployPathFile + "\" does not point at a directory! Fix your opal.properties.");
            return;
        }

        deplo = new Deployer();
        deplo.setValues(axisServicesUrl, deployPathFile);
        deplo.setName("OpalDeployer");
        deplo.start();

    }

    /**
     * this is how the monitoring thread it takes care of deploying and undeploying
     * opal services while tomcat is running
     */
    public class Deployer extends Thread {

        // protected static Log logger = LogFactory.getLog(OpalDeployService.class.getName());
        private String axisServicesUrl;
        private File deployPathFile;
        private boolean run;

        /**
         * terminates this thread so tomcat can shut down properly
         */
        public void stopThread() {
            run = false;
        }

        public void setValues(String axisServicesUrl, File path) {
            this.axisServicesUrl = axisServicesUrl;
            deployPathFile = path;
        }

        public void run() {
            GetServiceListHelper helper = new GetServiceListHelper();
            helper.setServiceUrl(axisServicesUrl);
            OPALService[] servicesList = helper.getServiceList();
            if (servicesList == null) {
                logger.error("Unable to parse the service list from the server");
                return;
            }

            for (OPALService service : servicesList) {
                logger.info("undeploying service: " + service.getServiceName());
                undeploy(service.getServiceName());
            }

            //now deploy new services
            File[] deployFileList = deployPathFile.listFiles();
            for (File configFile : deployFileList) {
                logger.info("deploying service: " + configFile);
                try {
                    deploy(configFile);
                } catch (Exception e) {
                    //this should never happen
                    logger.error("configFile does not exist: " + e);
                }
            }

            run = true;
            FileAlterationObserver observer = new FileAlterationObserver(deployPathFile);
            observer.addListener(new DeployListener());
            try {
                observer.initialize();
                while (run) {
                    observer.checkAndNotify();
                    try {
                        this.sleep(1000);
                    } catch (InterruptedException e) {
                        /* so we can terminate this thread without waiting the
                         * 1000 ms of the sleep (TODO possible race condition)
                         */
                    }
                }//while
            } catch (Exception e) {
                logger.error("Deployer failed to monitor the deployment directory: " + e);
            }
            // destroy all resources
            try {
                observer.destroy();
            } catch (Exception e) {
                logger.error("Problem while destroying Opal Deployer");
            }
            logger.info("Opal Deployer thread terminated");
        }

        public void deploy(File appConfigFile) {

            // get the location of the application configuration
            logger.info("deploy with appconfig: " + appConfigFile);

            //create service directory
            String tmp_serviceDir = wsddDirectory + "/opalservice";
            boolean servicedir_flag = new File(tmp_serviceDir + "/META-INF").mkdirs();
            if (servicedir_flag) {
                logger.info("The directory, "+tmp_serviceDir + "/META-INF, created successfully.");
            } else {
                logger.info("The directory, "+tmp_serviceDir + "/META-INF, was not created.");
            }
            //reading service template
            String serviceTemplate = wsddDirectory + "/opal_services.xml";
            // check to make sure that the WSDD template exists
            String templateData = null;
            try {
                File f = new File(serviceTemplate);
                byte[] data = new byte[(int) f.length()];
                FileInputStream fIn = new FileInputStream(f);
                fIn.read(data);
                fIn.close();
                templateData = new String(data);
            } catch (Exception e) {
                logger.error("unable to read wsdd template file: " + e);
                return;
            }

            // check to make sure that the application configuration exists
            if (!appConfigFile.exists()) {
                logger.error("Application configuration file " +
                        appConfigFile + " does not exist");
            }

            // check to make sure that the file points to valid application
            // configuration
            try {
                InputStream in = new FileInputStream(appConfigFile);
                XMLInputFactory factory = XMLInputFactory.newInstance();
                XMLStreamReader parser = factory.createXMLStreamReader(in);
                AppConfigType config = AppConfigType.Factory.parse(parser);
                logger.info("the parallel status in App config type = " + config.getParallel());
            } catch (Exception e) {
                logger.error(e);
                logger.error("appConfiguration is invalid: " + appConfigFile);
                return;
            }

            // service name is now filename - .xml
            String serviceName = FilenameUtils.getBaseName(appConfigFile.toString());
            logger.info("Service name used for deployment: " + serviceName);

            // replace SERVICE_NAME with actual service name
            String finalData = templateData.replaceAll("@SERVICE_NAME@", serviceName);

            // set the location of the config file
            finalData = finalData.replaceAll("@CONFIG_LOCATION@", appConfigFile.toString());
            logger.info("Using location of config file: " + appConfigFile);

            // location of final service file
            try {
                File serviceFinal = new File(tmp_serviceDir + "/META-INF/services.xml");
                FileOutputStream fOut = new FileOutputStream(serviceFinal);
                fOut.write(finalData.getBytes());
                fOut.close();
                String jar_file = serviceDirectory + "/" + serviceName +".aar";

                if (createJAR(jar_file, serviceFinal)) {
                    // updating service status in database
                    logger.info("Updating service status in database to ACTIVE");
                    ServiceStatus serviceStatus = new ServiceStatus();
                    serviceStatus.setServiceName(serviceName);
                    serviceStatus.setStatus(ServiceStatus.STATUS_ACTIVE);
                    HibernateUtil.saveServiceStatus(serviceStatus);
                    serviceFinal.delete();
                }
            } catch (Exception e) {
                logger.error("Deploy: failing while writing service file " + e);
            }
        }

        public void undeploy(String serviceName) {
            // get the service name
            logger.info("Undeploy called. ServiceName set to: " + serviceName);

            try {
                File jar_file = new File(serviceDirectory + "/" + serviceName + ".aar");
                if (jar_file.delete()) {
                    logger.info("Updating service status in database to INACTIVE");
                    ServiceStatus serviceStatus = new ServiceStatus();
                    serviceStatus.setServiceName(serviceName);
                    serviceStatus.setStatus(ServiceStatus.STATUS_INACTIVE);
                    HibernateUtil.saveServiceStatus(serviceStatus);
                }
            } catch (Exception e) {
                logger.error("Failing while writing wsdd file " + e);
            }
        }

        boolean createJAR(String jarFileName, File entryFile) {
            logger.info("Create service file:" + jarFileName);
            try (JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(
                    new FileOutputStream(jarFileName)), new Manifest())) {
                jos.setLevel(Deflater.BEST_COMPRESSION);

                JarEntry je = new JarEntry("META-INF/"+entryFile.getName());
                jos.putNextEntry(je);

                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(entryFile));
                byte[] buffer = new byte[1024];
                int count = -1;
                while ((count = bis.read(buffer)) != -1) {
                    jos.write(buffer, 0, count);
                }
                bis.close();

                jos.closeEntry();

                return true;
            } catch (IOException e) {
                // e.printStackTrace();
                logger.error("Creating service file error: " + e.getMessage());
                return false;
            }
        }

    }

    /**
     * this class implements the action that will be taken
     * when a file is modifed in the deploy directory
     */
    public class DeployListener implements FileAlterationListener {

        public void onStart(final FileAlterationObserver observer) {
        }

        public void onStop(final FileAlterationObserver observer) {
        }

        public void onDirectoryCreate(final File directory) {
        }

        public void onDirectoryChange(final File directory) {
        }

        public void onDirectoryDelete(final File directory) {
        }

        public void onFileCreate(final File file) {
            logger.info("Autodeploy file: " + file);
            deplo.deploy(file);
        }

        public void onFileChange(final File file) {
            logger.debug("AppConfig modified (nothing will be done): " + file);
        }

        public void onFileDelete(final File file) {
            logger.info("Undeploying file: " + file);
            String serviceName = FilenameUtils.getBaseName(file.toString());
            if (serviceName != null) {
                deplo.undeploy(serviceName);
                logger.info("Service " + serviceName + " undeployed");
            }
        }

    }

    public void destroy() {
        deplo.stopThread();
        deplo.interrupt();
    }

}
