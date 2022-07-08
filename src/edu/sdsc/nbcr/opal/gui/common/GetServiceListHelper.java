package edu.sdsc.nbcr.opal.gui.common;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import edu.sdsc.nbcr.opal.AppServiceStub;
import edu.sdsc.nbcr.opal.types.GetAppMetadataInput;
import edu.sdsc.nbcr.opal.types.GetAppMetadataOutput;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.sdsc.nbcr.opal.types.AppMetadataInputType;
import edu.sdsc.nbcr.opal.types.AppMetadataType;

/**
 * this class is in charge of getting the list of service from an opal service
 * Just use this code:
 *
 * GetServiceListHelper helper = new GetServiceListHelper();
 * helper.setServiceUrl(url);
 * OPALService [] servicesList = helper.getServiceList();
 * helper.setSerivceName(serviceList);
 *
 * @author Choonhan Youn
 *
 */

public class GetServiceListHelper {

    protected static Log log = LogFactory.getLog(GetServiceListHelper.class.getName());

    private String serviceUrl;

    /**
     * default constructor
     */
    public GetServiceListHelper() {
        //empty constructor
    }

    /**
     * given the return value of the axis admin service it parses it and
     * returns an array of OPALSerivce
     */
    public OPALService[] getServiceList() {
        try {
            ArrayList<OPALService> list = new ArrayList<>();

            URL url = new URL(serviceUrl + "/listServices");
            System.out.println("service list = " + serviceUrl + "/listServices");
            URLConnection uc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String inputLine;
            OPALService service = new OPALService();
            String serviceID = null;
            String serviceURL = null;
            while ((inputLine = in.readLine()) != null) {
                // System.out.println("line = "+inputLine);
                if (inputLine.contains("Service Description")) {
                    // System.out.println("service desc = "+inputLine);
                    try {
                        serviceID = filterService(inputLine);
                    } catch (java.lang.ArrayIndexOutOfBoundsException aioutof_ex) {
                        // System.out.println(aioutof_ex.getMessage());
                        log.info(aioutof_ex.getMessage());
                    }
                    //system.out.println(filterService(inputLine));
                    continue;
                }
                if (inputLine.contains("Service EPR")) {
                    serviceURL = filterService(inputLine);
                    //system.out.println(filterService(inputLine));
                    continue;
                }
                if (inputLine.contains("getAppMetadata")) {
                    service.setServiceID(serviceID);
                    service.setURL(serviceURL);
                    list.add(service);
                    log.info("added -> " + service);
                    service = new OPALService();
                }
            }
            in.close();

            return list.toArray(new OPALService[list.size()]);
        } catch (Exception e) {
            //log.error("Something happen when parsing the list of services: " + e.getMessage(), e);
            e.printStackTrace();
            return null;
        }
    }

    //filter html tags
    private String filterService(String line) {
        line = line.replaceAll("\\<[^>]*>", "");
        line = line.split(" ")[3];
        return line;
    }

    /**
     * given the service list it check the metadata and if present it set change the service name
     * with value contained appName attribute of the metadata tag
     *
     * @param serviceList
     */
    public boolean setServiceName(OPALService[] serviceList) {
        AppServiceStub appMetadata = null;
        GetAppMetadataOutput amt_output = null;
        AppMetadataType amt = null;
        GetAppMetadataInput m_in = new GetAppMetadataInput();
        AppMetadataInputType m_in_type = new AppMetadataInputType();
        m_in.setGetAppMetadataInput(m_in_type);

        for (OPALService aServiceList : serviceList) {
            String url = aServiceList.getURL();
            try {
                appMetadata = new AppServiceStub(url);
                amt_output = appMetadata.getAppMetadata(m_in);
                amt = amt_output.getGetAppMetadataOutput();
            } catch (Exception e) {
                log.error("Error retrieving the Service name", e);
                return false;
            }
            //setting general info
            String serviceName = amt.getAppName();
            String description = amt.getUsage();
            aServiceList.setDescription(description);
            if (serviceName != null) {
                aServiceList.setServiceName(serviceName);

            } else {
                // if the service name is not specified let's use the service ID
                aServiceList.setServiceName(aServiceList.getServiceID());
            }

            if ((amt.getTypes() == null) || ((amt.getTypes().getTaggedParams() == null) && (amt.getTypes().getUntaggedParams() == null)))
                aServiceList.setComplexForm(Boolean.FALSE);
            else
                aServiceList.setComplexForm(Boolean.TRUE);


        }
        return true;
    }

    public void setServiceUrl(String url) {
        this.serviceUrl = url;
    }

    /**
     * Just for testing purpouse. not used
     *
     * @param argv
     */
    public static void main(String[] argv) {
        GetServiceListHelper servicelist = new GetServiceListHelper();
        servicelist.setServiceUrl("http://localhost:8080/opal2/services/listServices");
        OPALService[] serviceList = servicelist.getServiceList();

        for (int i = 0; i < serviceList.length; i++) {
            System.out.println("the service " + i + " is: " + serviceList[i]);
        }

    }

}
