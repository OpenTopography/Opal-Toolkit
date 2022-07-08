package edu.sdsc.nbcr.opal.gui.actions;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.actions.MappingDispatchAction;
import edu.sdsc.nbcr.opal.gui.common.AppMetadataParser;
import edu.sdsc.nbcr.opal.gui.common.AppMetadata;
import edu.sdsc.nbcr.opal.gui.common.Constants;

/**
 * This struts action is called to prepare the data necessary
 * to create the submission form.
 *
 * Given a serviceURL it fetches the metadata from the specified service and
 * it returns a web page displaying the complex submission form or a simple
 * submission form depending on the availability of the metadata on the specified
 * service.
 *
 * @author clem, Choonhan Youn
 *
 */
public class CreateSubmissionFormAction extends MappingDispatchAction{

    private static Log log = LogFactory.getLog(Constants.PACKAGE);

    // See superclass for Javadoc
    public ActionForward execute(ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        log.info("Action: CreateSubmissionForm");
        ArrayList<String> errors = new ArrayList<>();
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
        log.info("The serviceBaseURL is: " + serviceBaseURL );
        //log.info("The serviceName is: " + serviceName );
        //let's parse the serviceBaseURL appMetadata file
        AppMetadata app = AppMetadataParser.parseAppMetadata(serviceBaseURL );
        if (app == null){
            //something went wrong return an error
            errors = new ArrayList<String>();
            errors.add("Can not get application configuration file from remote server");
            errors.add("Remote server is: " + serviceBaseURL);
            request.setAttribute(Constants.ERROR_MESSAGES, errors);
            return mapping.findForward("Error");
        }
        request.getSession().setAttribute("appMetadata", app);
        if ( app.isArgMetadataEnable() ) {
            log.info("Metadata parsed correctly, forwarding to the submission form");
            return mapping.findForward("DisplayForm");
        }
        else {
            log.info("Metadata parsed correctly, forwarding to the simple submission form");
            return mapping.findForward("DisplaySimpleForm");
        }
    }//execute

}
