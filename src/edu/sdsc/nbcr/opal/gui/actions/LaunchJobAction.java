package edu.sdsc.nbcr.opal.gui.actions;


import java.net.URL;
import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionRedirect;
import org.apache.struts.actions.MappingDispatchAction;
import org.apache.struts.upload.FormFile;
import org.apache.struts.upload.CommonsMultipartRequestHandler;

import org.apache.commons.fileupload.DefaultFileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;

import edu.sdsc.nbcr.opal.AppServiceLocator;
import edu.sdsc.nbcr.opal.AppServicePortType;
import edu.sdsc.nbcr.opal.InputFileType;
import edu.sdsc.nbcr.opal.JobInputType;
import edu.sdsc.nbcr.opal.JobSubOutputType;
import edu.sdsc.nbcr.opal.StatusOutputType;
import edu.sdsc.nbcr.opal.FaultType;
import edu.sdsc.nbcr.opal.gui.common.AppMetadata;
import edu.sdsc.nbcr.opal.gui.common.ArgFlag;
import edu.sdsc.nbcr.opal.gui.common.ArgParam;
import edu.sdsc.nbcr.opal.gui.common.Constants;
import edu.sdsc.nbcr.opal.gui.common.OPALService;

/**
 * This action is invoked to launch a job.
 * It expects the appMetadata in the ActionForm with the values 
 * inserted by the user in the submission form.
 * 
 * @author clem
 *
 */
public class LaunchJobAction extends MappingDispatchAction{
    
    protected Log log = LogFactory.getLog(Constants.PACKAGE);
    protected ArrayList errors;
    protected HttpServletRequest request;
    protected ActionMapping mapping;
    
    // See superclass for Javadoc
    public ActionForward execute(ActionMapping mapping, ActionForm form, 
				 HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
	request = request;
	mapping = mapping;
        log.debug("Action: LaunchJob");

	// session timeout 
	if(request.getSession(false) == null || request.getSession(false).getAttribute("appMetadata") == null) {
            log.error("*** Session has timed out ***");
            errors = new ArrayList();
            errors.add("Session timed out");
            request.setAttribute(Constants.ERROR_MESSAGES, errors);
            return mapping.findForward("Timeout");
	}
        
        AppMetadata app = (AppMetadata) form;

        if (app == null){
            log.error("Error the appMetadata is not present.");
            errors.add("We could not find the input values please go back to the welcome page");
            errors.add("appMetadata is not available");
            request.setAttribute(Constants.ERROR_MESSAGES, errors);
            return mapping.findForward("Error");
        }

        InputFileType [] files; 

        String debug = "";
	if ( app.isArgMetadataEnable()) { // processing compelx form
            ArgFlag [] flags = app.getArgFlags();
            if (flags != null ) { 
                for (int i = 0; i < flags.length; i++ ) {
                    debug += "for flags " + flags[i].getId() + " the user has entered: " + flags[i].isSelected() + "\n";
                }
            }else { debug += "no falgs found"; }
            ArgParam [] params = app.getArgParams();
            if ( params != null) {
                for (int i = 0; i < params.length; i++ ) {
                    debug += "for flags " + params[i].getId() + " the user has entered: " + params[i].getSelectedValue() + "\n";
                }
            } else { debug += "no parameters found\n"; }
            files = getFiles(app);
        } else { // processing simple form
            files = getDynamicFiles(app);
        }

        log.debug("the following parameters has been posted:\n" + debug);
        // build the command line
        String cmd = makeCmdLine(app);
	if ( cmd == null){
            request.setAttribute(Constants.ERROR_MESSAGES, errors);
            return mapping.findForward("Error");
        }

        //now we could validate the cmd line
        //let's invoke the remote opal service
        JobInputType in = new JobInputType();
        in.setArgList(cmd);
        in.setExtractInputs(app.isExtractInputs());

        int numCpu = -1;
        if ( (app.getNumCpu() != null) && (app.getNumCpu().length() >= 1) ) {
            //let's get the number of CPUs
            try { 
                numCpu = Integer.parseInt( app.getNumCpu() ); 
                if ( numCpu <= 0 ) throw new NumberFormatException();
            }
            catch (NumberFormatException e) {
                log.error("the user has entered wrong number of cpu");
                errors.add("the number of cpu is worng.");
                errors.add("Number of cpu should be a positive integer number and not a \"" + app.getNumCpu() + "\"");
                request.setAttribute(Constants.ERROR_MESSAGES, errors);
                return mapping.findForward("Error");
            }
        }
        if ( numCpu != -1 ) {
            in.setNumProcs(numCpu);
        }

	if ((app.getUserEmail() != null) && (app.getUserEmail().length() >= 1)) {
	    in.setUserEmail(app.getUserEmail());
	    in.setSendNotification(true);
	}

        // preparing the input files
        if ( files != null ) {            
            in.setInputFile(files);
        } else{
	    //TODO improve this, it could be that the input file has been lost in the way
	    log.debug("No file has been submitted.");
        }

        //finally invoke opal service!
        JobSubOutputType subOut = null;
        try {
            AppServiceLocator asl = new AppServiceLocator();
            AppServicePortType appServicePort = asl.getAppServicePort(new URL(app.getURL()));

	    // TODO: fix this to use attachments, if need be
	    subOut = appServicePort.launchJob(in);
            if ( subOut == null ) {
                throw new Exception("launchJob returned null");
            }
        }catch (FaultType e){
            log.error("A remote error occurred while submitting the job.");
            log.error("The remote error message is: " + e.getMessage1(), e);

            errors.add("A remote error occured while submitting the job to the remote server");
            errors.add("The remote error message is: " + e.getMessage1());
            request.setAttribute(Constants.ERROR_MESSAGES, errors);
            return mapping.findForward("Error");
        }catch (Exception e){
	    if ( e == null) {
                log.error("*** Session has timed out 2 ***");
                return mapping.findForward("Timeout");
	    };
            if ( e.getMessage() == null ) {
                log.error("*** Session has timed out 3 ***");
                errors.add("Session timed out");
                request.setAttribute(Constants.ERROR_MESSAGES, errors);
                return mapping.findForward("Timeout");
            }
            log.error("An error occurred while submitting the job.");
            log.error("The error message is: " + e.getMessage(), e);

            errors.add("An error occured while submitting the job to the remote server");
            errors.add("The error message is: " + e.getMessage());
            errors.add("Please go back to the List of Applications page and try resubmitting the job");
            request.setAttribute(Constants.ERROR_MESSAGES, errors);
            return mapping.findForward("Error");
        }
        app.setJobId( subOut.getJobID() );
        //Let's do some logging
        log.debug("Submitted job received jobID: " + subOut.getJobID());
        StatusOutputType status = subOut.getStatus();
        log.debug("Current Status:\n" +
		  "\tCode: " + status.getCode() + "\n" +
		  "\tMessage: " + status.getMessage() + "\n" +
		  "\tOutput Base URL: " + status.getBaseURL());
        log.debug("redirecting to the status page...");

        // everything went allright redirect to the status page
        // put the jobId in the URL coz we are redirecting and not forwarding 
        return new ActionRedirect(mapping.findForward("JobStatus").getPath() + "?jobId=" +  subOut.getJobID() + "&serviceID=" + app.getServiceID());
    }//exectue
    
    
    /**
     * Given an appMetadata we build the command line
     * 
     * This method can also be static...
     * 
     * @param app
     * @return a string representing the command line
     */
    private  String makeCmdLine(AppMetadata app){
        String cmd = "";
	errors = new ArrayList();
        if ( app.isArgMetadataEnable() ) { // have the configuration paramters
            if (app.getArgFlags() != null ) {
                ArgFlag [] flags = app.getArgFlags();
                for ( int i = 0; i < flags.length; i++){
		    if ( flags[i].isSelected() )
                        cmd += " " + flags[i].getTag();
                }
            }

            if (app.getArgParams() != null ){ // process tagged and untagged
                ArgParam [] params = app.getArgParams();
                String taggedParams = "";
                String separator = app.getSeparator();
                if ( separator == null ) { 
                    separator =  " ";
                }
                String [] untaggedParams = new String[app.getNumUnttagedParams()];
                for ( int i = 0; i < untaggedParams.length; i++ )
                    untaggedParams[i] = "";
                log.debug("We have " + app.getNumUnttagedParams() + " untaggged parameters.");
                for( int i = 0; i < params.length; i++ ) {
		    log.debug("Analizing param: " + params[i].getId());
                    if (params[i].getTag() != null) { //tagged params
                        if ( params[i].isFileUploaded() ) {
                            //we have a file!
                            taggedParams += " " + params[i].getTag() + separator + params[i].getFile().getFileName();
                        }else if ( (params[i].getSelectedValue() != null) && ( params[i].getSelectedValue().length() > 0) )
                            taggedParams += " " + params[i].getTag() + separator + params[i].getSelectedValue();
                    } else { //untagged parameters
                        if (params[i].isFileUploaded() ) { //we have a file
                            untaggedParams[params[i].getPosition()] = " " + params[i].getFile().getFileName();
                        } else if ( (params[i].getSelectedValue() != null) && (params[i].getSelectedValue().length() > 0 ) ) {
                            //untagged params this is a bit unreadable!!
                            untaggedParams[params[i].getPosition()] = " " + params[i].getSelectedValue();
                            log.debug("Adding the " + i + " untagged paramters with: " + untaggedParams[params[i].getPosition()]);
                        }
                    }
                }

                if (taggedParams.length() > 0)
                    cmd += taggedParams;
                for (int i = 0; i < app.getNumUnttagedParams(); i ++) 
                    cmd += untaggedParams[i];
            }
        } else { //no configuration parameters
            cmd = app.getCmdLine();
        }

        if (cmd == null) {
            log.error("The command line is null!");
            errors.add("We could not built the command line from your input parameters");
        }
        log.info("Submitted job command line: " + cmd);
        return cmd;
    }
    
    
    /**
     * logs uploaded file info 
     * 
     * @param pos
     * @param FormFile
     */
    private void  logFileInfo(int pos, FormFile ff){
	String fileInfo;
        String fname = ff.getFileName();

        if(ff == null) {
            fileInfo = "(" + pos + ") file name : there was an error uploading file"; 
	    return;
        }

        if(fname.length()==0) {
            fileInfo = "(" + pos + ") file name has zero length"; 
	} else {
            fileInfo = "(" + pos + ") file name :" + ff.getFileName();
            fileInfo += " type: " + ff.getContentType();
	    fileInfo += " size: " + ff.getFileSize();
	}

        log.debug(fileInfo);
    }

    /**
     * Return an array of InputFileType with all the user dynamically uploaded files 
     * 
     * @param app
     * @return the dynamic files from simple form submitted by the user
     */
    private InputFileType [] getDynamicFiles(AppMetadata app){
        InputFileType [] files = null;
        try {
	    ArrayList al = app.getFormFiles();
            ArrayList filesArrayReturn = new ArrayList();
            log.debug("User uploads " + al.size() + " dynamic input file(s)");
            for (int i = 0; i<al.size(); i++) { // get files from the bean
                FormFile ff = (FormFile) al.get(i);
                String fname = ff.getFileName();
                if(fname.length()==0) {
                    continue;
                }
		logFileInfo(i+1, ff);

                InputFileType file = new InputFileType();
                file.setName(fname);
		File osFile = getStoredFile(ff);
		if (osFile != null) {
		    DataHandler dh = new DataHandler(new FileDataSource(osFile));
		    file.setAttachment(dh);
		} else {
		    file.setContents(ff.getFileData());
		}
                filesArrayReturn.add( file );
            }
            files = (InputFileType[]) filesArrayReturn.toArray(new InputFileType[filesArrayReturn.size()]);
        } catch (Exception e){
            log.error("There was an error reading uploaded files!");
            log.error("The exception is: " + e.getMessage(), e.getCause());
            e.printStackTrace();
            return null;
        }
        return files;
    }

    /**
     * Given an appMetadata we return an array of InputFileType with all the files 
     * submitted by the user
     * 
     * @param app
     * @return the files submitted by the user
     */
    private InputFileType [] getFiles(AppMetadata app){
        InputFileType [] files = null;
        try {
            if ( app.getNumArgFileSubmitted() > 0 ) { //we have some files in the argParam array...
                int numFile = app.getNumArgFileSubmitted();
                log.info("User uploads " + numFile + " input file(s)");
                files = new InputFileType[numFile];
                for ( int i = 0; i < numFile; i++ ){
                    ArgParam param = app.getArgFileSubmitted(i);
                    files[i] = new InputFileType();
		    FormFile ff = param.getFile();
                    if (ff != null) {
                        files[i].setName(ff.getFileName());
                        File osFile = getStoredFile(ff);
                        if (osFile != null) {
                            DataHandler dh = new DataHandler(new FileDataSource(osFile));
                            files[i].setAttachment(dh);
                        } else { // data is in memory already - reuse it
                            files[i].setContents(ff.getFileData());
                        }
                    } else {
                        log.error("This is very nasty... Contact developers!!\n The arg: " + param + "lost the file...");
                        return null;
                    }
                    logFileInfo(i+1, ff);
                }
            } 
        } catch (Exception e){
            log.error("There was an error reading uploaded files!");
            log.error("The exception is: " + e.getMessage(), e.getCause());
            e.printStackTrace();
            return null;
        }
        return files;
    }

    /**
     * Method to get the absolute path from FormFile
     *
     * @param file FormFile whose path is desired
     * @return null if FormFile is in memory, else absolute path
     */
    private File getStoredFile(FormFile file) {
	try {
	    File osFile = null;

	    // returned class should be CommonsMultipartRequestHandler.CommonsFormFile
	    Class formClass = file.getClass();
	    Field fileItemField = formClass.getDeclaredField("fileItem");
	    fileItemField.setAccessible(true);
	    Object o = fileItemField.get(file);
	    if (o instanceof DiskFileItem) {
		DiskFileItem df = (DiskFileItem) o;
		if (df.isInMemory()) {
		    log.debug("FormFile is actually in memory - " + "no need to write it out to File");
		    return null;
		} else {
		    osFile = df.getStoreLocation();
		    String path = osFile.getAbsolutePath();
		    log.debug("FormFile found at absolute path: " + path);
		    return osFile;
		}
	    } else {
		log.error("Can't retrieve stored File for FormFile");
		return null;
	    }
	} catch (Exception e) {
	    log.error("Can't retrieve stored File for FormFile");
	    log.error(e);
	    return null;
	}
    }
}

