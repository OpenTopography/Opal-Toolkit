package edu.sdsc.nbcr.opal.gui.actions;

import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.actions.MappingDispatchAction;

import edu.sdsc.nbcr.opal.AppMetadataInputType;
import edu.sdsc.nbcr.opal.AppMetadataType;
import edu.sdsc.nbcr.opal.AppServiceLocator;
import edu.sdsc.nbcr.opal.AppServicePortType;
import edu.sdsc.nbcr.opal.FaultType;
import edu.sdsc.nbcr.opal.FlagsArrayType;
import edu.sdsc.nbcr.opal.FlagsType;
import edu.sdsc.nbcr.opal.GroupsArrayType;
import edu.sdsc.nbcr.opal.GroupsType;
import edu.sdsc.nbcr.opal.ImplicitParamsType;
import edu.sdsc.nbcr.opal.ParamType;
import edu.sdsc.nbcr.opal.ParamsArrayType;
import edu.sdsc.nbcr.opal.ParamsType;
import edu.sdsc.nbcr.opal.gui.common.AppMetadataParser;
import edu.sdsc.nbcr.opal.gui.common.AppMetadata;
import edu.sdsc.nbcr.opal.gui.common.ArgFlag;
import edu.sdsc.nbcr.opal.gui.common.ArgParam;
import edu.sdsc.nbcr.opal.gui.common.Constants;
import edu.sdsc.nbcr.opal.gui.common.Group;
import edu.sdsc.nbcr.opal.gui.common.OPALService;

/**
 * This struts action is called to prepare the data necessary 
 * to create the submission form.
 * 
 * Given a serviceURL it fetches the metadata from the specified service and 
 * it returns a web page displaying the complex submission form or a simple
 * submission form depending on the availability of the metadata on the specified 
 * service.
 * 
 * @author clem
 *
 */
public class CreateSubmissionFormAction extends MappingDispatchAction{
    
    private static Log log = LogFactory.getLog(Constants.PACKAGE);
    
    // See superclass for Javadoc
    public ActionForward execute(ActionMapping mapping, ActionForm form, 
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        
        log.debug("Action: CreateSubmissionForm");
        ArrayList errors = new ArrayList();
        DynaActionForm serviceForm = (DynaActionForm)form;
        // get the service url and name  
        if (((String) serviceForm.get("serviceURL")).equals("")) {
            errors.add("Ne Service URL provided.");
            errors.add("Please insert a Service URL.");
        }
        if ( ! errors.isEmpty()) {
            request.setAttribute(Constants.ERROR_MESSAGES, errors);
            return mapping.findForward("Error");           
        }
        String serviceBaseURL = (String) serviceForm.get("serviceURL");
        //String serviceName = (String) serviceForm.get("serviceName");       
        log.debug("The serviceBaseURL is: " + serviceBaseURL );
        //log.debug("The serviceName is: " + serviceName );
        //let's parse the serviceBaseURL appMetadata file
        AppMetadata app = AppMetadataParser.parseAppMetadata(serviceBaseURL );
        if (app == null){
            //something went wrong return an error
            errors = new ArrayList();
            errors.add("Can not get application configuration file from remote server");
            errors.add("Remote server is: " + serviceBaseURL);
            request.setAttribute(Constants.ERROR_MESSAGES, errors);
            return mapping.findForward("Error");
        }
        request.getSession(false).setAttribute("appMetadata", app);
        if ( app.isArgMetadataEnable() ) {
            log.debug("Metadata parsed correctly, forwarding to the submission form");
            return mapping.findForward("DisplayForm");
        }
        else {
            log.debug("Metadata parsed correctly, forwarding to the simple submission form");
            return mapping.findForward("DisplaySimpleForm");
        }
    }//execute

}
