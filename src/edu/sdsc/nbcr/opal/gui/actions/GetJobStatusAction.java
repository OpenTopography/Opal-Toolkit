package edu.sdsc.nbcr.opal.gui.actions;

import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.sdsc.nbcr.opal.AppServiceStub;
import edu.sdsc.nbcr.opal.types.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.MappingDispatchAction;

import edu.sdsc.nbcr.opal.OpalFaultResponse;
import edu.sdsc.nbcr.opal.gui.common.Constants;

/**
 * This action is used to get the job status, it requires a jobId parameters in the request.
 * Given the ID of a job it returns a page displaying its status.
 *
 * @author clem, Choonhan Youn
 *
 */
public class GetJobStatusAction extends MappingDispatchAction{

    protected Log log = LogFactory.getLog(Constants.PACKAGE);

    // See superclass for Javadoc
    public ActionForward execute(ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        log.info("Action: GetJobStatusAction");
        String jobId = (String) request.getParameter("jobId");
        String serviceID = (String) request.getParameter("serviceID");
        ArrayList<String> errors = new ArrayList<>();
        if ((jobId == null) || (serviceID == null) || (jobId.length() == 0) || (serviceID.length() == 0)) {
            if ((jobId == null) || (jobId.length() == 0)) log.error("Error jobId can not be retrived.");
            if ((serviceID == null) || (serviceID.length() == 0)) log.error("Error serviceID can not be retrived.");
            //something went wrong return an error
            errors = new ArrayList<>();
            errors.add("I could not find the jobId and the serviceID");
            errors.add("Please return to the welcome page...");
            request.setAttribute(Constants.ERROR_MESSAGES, errors);
            return mapping.findForward("Error");
        }//if */

        String url = getServlet().getServletContext().getInitParameter("OPAL_URL");
        if (url == null) {
            log.warn("the OPAL_URL was not found in the WEB-INF/web.xml file.\nUsing the default...");
            url = Constants.OPALDEFAULT_URL;
        }
        StatusOutputType status = null;
        try {
            AppServiceStub appServicePort = new AppServiceStub(url + "/" + serviceID);
            QueryStatusInput qs_in = new QueryStatusInput();
            qs_in.setQueryStatusInput(jobId);
            QueryStatusOutput qs_out = appServicePort.queryStatus(qs_in);
            status = qs_out.getQueryStatusOutput();
        } catch (OpalFaultResponse e) {
            log.error("A remote error occurred while querying status.");
            log.error("The remote error message is: " + e.getMessage(), e);

            errors.add("A remote error occured while querying status");
            errors.add("the remote error message is: " + e.getMessage());
            request.setAttribute(Constants.ERROR_MESSAGES, errors);
            return mapping.findForward("Error");
        } catch (Exception e) {
            log.error("An error occurred while querying status");
            log.error("The error message is: " + e.getMessage(), e);

            errors.add("An error occured while querying status");
            errors.add("the error message is: " + e.getMessage());
            errors.add("Please go back to the List of Application page and try to resubmit your job");
            request.setAttribute(Constants.ERROR_MESSAGES, errors);
            return mapping.findForward("Error");
        }

        request.setAttribute("serviceID", serviceID);
        request.setAttribute("jobId", jobId);
        request.setAttribute("status", status);
        return mapping.findForward("JobStatus");
    }
}
