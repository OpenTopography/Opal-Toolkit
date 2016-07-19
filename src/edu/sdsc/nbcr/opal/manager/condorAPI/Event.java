package edu.sdsc.nbcr.opal.manager.condorAPI;

import condor.classad.*;
import edu.sdsc.nbcr.opal.manager.condorAPI.event.*;
import java.text.*;
import java.util.*;

/** 
 * Event object
 */

public class Event implements EventType {
  /**
   * Job Id
   */
  JobId jobId;

  /**
   * the Job 
   */
  Job   job;

  /**
   * EventType is defined in EventType
   */
  protected int   type;

  /**
   * Date and Time of the event occurs
   */
  Date  date;

  static DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");

  protected Event(RecordExpr expr){
	jobId = new JobId(((Constant)expr.lookup("Cluster")).intValue(),
							((Constant)expr.lookup("Proc")).intValue());
	String eventTime = ((Constant)expr.lookup("EventTime")).stringValue();
	try {
	  date = format.parse(eventTime);
	} catch (java.text.ParseException e){
	  date = new Date();
	}
  }

  /** 
   * returns a copy of the jobId
   */
  public JobId getJobId(){
	return (JobId)(jobId.clone());
  }

  /**
   * return the job
   */
  public Job getJob(){
	return job;
  }


  static Event getEvent(RecordExpr expr){
	int   type  = ((Constant)expr.lookup("EventTypeNumber")).intValue();
	Event tmpEvent = null;
	switch (type){
	case SUBMIT:
	  tmpEvent = new EventSubmit(expr);
	  break;
	case EXECUTE:
	  tmpEvent = new EventExecute(expr);
	  break;
	case EXECUTABLE_ERROR:
	  tmpEvent = new EventExecutableError(expr);
	  break;
	case CHECKPOINTED:
	  tmpEvent = new EventCheckpointed(expr);
	  break;
	case JOB_EVICTED:
	  tmpEvent = new EventJobEvicted(expr);
	  break;
	case JOB_TERMINATED:
	  tmpEvent = new EventJobTerminated(expr);
	  break;
	case IMAGE_SIZE:
	  tmpEvent = new EventImageSize(expr);
	  break;
	case SHADOW_EXCEPTION:
	  tmpEvent = new EventShadowException(expr);
	  break;
	case GENERIC:
	  tmpEvent = new EventGeneric(expr);
	  break;
	case JOB_ABORTED:
	  tmpEvent = new EventJobAborted(expr);
	  break;
	case JOB_SUSPENDED:
	  tmpEvent = new EventJobSuspended(expr);
	  break;
	case JOB_UNSUSPENDED:
	  tmpEvent = new EventJobUnsuspended(expr);
	  break;
	case JOB_HELD:
	  tmpEvent = new EventJobHeld(expr);
	  break;
	case JOB_RELEASED:
	  tmpEvent = new EventJobReleased(expr);
	  break;
	case NODE_EXECUTE:
	  tmpEvent = new EventNodeExecute(expr);
	  break;
	case NODE_TERMINATED:
	  tmpEvent = new EventNodeTerminated(expr);
	  break;
	case POST_SCRIPT_TERMINATED:
	  tmpEvent = new EventPostScriptTerminated(expr);
	  break;
	case GLOBUS_SUBMIT:
	  tmpEvent = new EventGlobusSubmit(expr);
	  break;
	case GLOBUS_SUBMIT_FAILED:
	  tmpEvent = new EventGlobusSubmitFailed(expr);
	  break;
	case GLOBUS_RESOURCE_UP:
	  tmpEvent = new EventGlobusResourceUp(expr);
	  break;
	case GLOBUS_RESOURCE_DOWN:
	  tmpEvent = new EventGlobusResourceDown(expr);
	  break;
	case REMOTE_ERROR:
	  tmpEvent = new EventRemoteError(expr);
	  break;
	case JOB_DISCONNECTED:
	  tmpEvent = new EventJobDisconnected(expr);
	  break;
	case JOB_RECONNECTED:
	  tmpEvent = new EventJobReconnected(expr);
	  break;
	case JOB_RECONNECT_FAILED:
	  tmpEvent = new EventJobReconnectFailed(expr);
	  break;
	}
	return tmpEvent;
  }

  public String toString(){
	return jobId + " " + date + " " + EventNames[type];
  }
}
 
