package edu.sdsc.nbcr.opal.manager.condorAPI;

import edu.sdsc.nbcr.opal.manager.condorAPI.event.*;
import java.util.*;

/** 
 * this class represent a Job.
 * It stores list of events it gets.
 */ 

public class Job implements Status, EventType {
    /** 
     * Job ID
     */
    JobId   jobId;

    /**
     * Nodes in the Job
     */
    Node [] nodes;

    /**
     * Status of the job
     */
    int     status = UNEXPANDED;

    /**
     * Handlers for this job
     */
    HandlerSet handlers = new HandlerSet();

    /**
     * The Condor this job belongs to.
     */ 
    Condor  condor;

    /**
     * A list of events the job got.
     */
    List eventList = new ArrayList();

    /**
     * Creates a Job object.
     * @param jobId  jobID of the job
     * @param condor the Condor
     */
    Job(JobId jobId, Condor condor){
	this.jobId = jobId;
	this.condor = condor;
    }

    /** 
     * Returns a copy of the jobId
     */
    public JobId getJobId(){
	return (JobId)(jobId.clone());
    }

    /**
     * setup handlerset
     */
    void setHandlerSet(HandlerSet handlers){
	this.handlers = (HandlerSet)handlers.clone();
    }

    /**
     * Waits for the job to be done.
     */
    public synchronized void waitFor() throws CondorException {
	while (status == UNEXPANDED ||
	       status == RUNNING || 
	       status == IDLE    ||
	       status == HELD    ){
	    try { wait(); } catch (InterruptedException e){/* ignore */ }
	}
    }

    /**
     * Waits for the job to be running
     */
    public synchronized void waitForRun() throws CondorException {
	while (status == UNEXPANDED ||
	       status == IDLE    ||
	       status == HELD    ){
	    try { 
		wait(); 
	    } catch (InterruptedException e){/* ignore */ }
	}
    }

    /**
     * return if the job is held
     */
    public boolean isHeld() {
	return status == HELD;
    }

    /**
     * return if the job is running
     */
    public boolean isRunning() {
	return status == RUNNING;
    }

    /**
     * return if the job is idle
     */
    public boolean isIdle() {
	return status == IDLE;
    }

    /**
     * return if the job is removed
     */
    public boolean isRemoved() {
	return status == REMOVED;
    }

    /**
     * return if the job is completed
     */
    public boolean isCompleted() {
	return status == COMPLETED;
    }
  
  
    /**
     * Sets a handler that will be called on failure.
     * @param handler - Handler to be invoked
     */ 
    public void setHandlerOnFailure(Handler handler){
	handlers.setHandlerOnFailure(handler);
    }
  
    /**
     * Sets a handler that will be called on success.
     * @param handler - Handler to be invoked
     */ 
    public void setHandlerOnSuccess(Handler handler){
	handlers.setHandlerOnSuccess(handler);
    }

    /**
     * Sets a handler that will be called on a specified event.
     * @param eventType - the Event Type
     * @param handler   - Handler to be invoked
     */ 
    public void setHandler(int eventType, Handler handler){
	handlers.setHandler(eventType, handler);
    }



    /***************************************************
     *              private functions                  *
     ****************************************************/

    private void invokeCompletedHandler(Event e){
	EventJobTerminated event = (EventJobTerminated)e;
	if (event.returnValue == 0){
	    Handler handler = handlers.getHandlerOnSuccess();
	    if (handler != null)
		handler.handle(e);
	} else {
	    Handler handler = handlers.getHandlerOnFailure();
	    if (handler != null)
		handler.handle(e);
	}
    }

    public String toString(){
	return jobId.toString() + " " + names[status];
    }


    synchronized void setStatus(int status, Event e){
	if (this.status != status) {
	    this.status = status;
	    if (status == COMPLETED)
		condor.jobTerminated(this);
	    if (e.type == JOB_TERMINATED)
		invokeCompletedHandler(e);
	    notifyAll();
	}
    }

    public int getStatus() {
	return status;
    }
  
    void event(Event e){
	eventList.add(e);
	switch (e.type){
	case SUBMIT:
	    setStatus(IDLE, e); break;
	case EXECUTE:
	    setStatus(RUNNING, e); break;
	case EXECUTABLE_ERROR:
	    setStatus(IDLE, e); break;
	case CHECKPOINTED:
	    setStatus(IDLE, e); break;
	case JOB_EVICTED:
	    if (((EventJobEvicted)e).terminatedAndRequeued){
		setStatus(IDLE, e); break;
	    } else {
		setStatus(REMOVED, e); break;
	    }
	case JOB_TERMINATED:
	    setStatus(COMPLETED, e); break;
	case SHADOW_EXCEPTION:
	    setStatus(IDLE, e); break;
	case GENERIC:
	    break;
	case JOB_ABORTED:
	    setStatus(COMPLETED, e); break;
	case JOB_SUSPENDED:
	    break;
	case JOB_UNSUSPENDED:
	    break;
	case JOB_HELD:
	    setStatus(HELD, e); break;
	case JOB_RELEASED:
	    setStatus(IDLE, e); break;
	case IMAGE_SIZE:
	case NODE_EXECUTE:
	case NODE_TERMINATED:
	case POST_SCRIPT_TERMINATED:
	case GLOBUS_SUBMIT:
	case GLOBUS_SUBMIT_FAILED:
	case GLOBUS_RESOURCE_UP:
	case GLOBUS_RESOURCE_DOWN:
	case REMOTE_ERROR:
	case JOB_DISCONNECTED:
	case JOB_RECONNECTED:
	case JOB_RECONNECT_FAILED:
	}
	Handler handler = handlers.getHandler(e.type);
	if (handler != null)
	    handler.handle(e);

    }
}

