package edu.sdsc.nbcr.opal.manager.condorAPI.event;
import edu.sdsc.nbcr.opal.manager.condorAPI.*;

import condor.classad.*;

public class EventJobTerminated extends Event {
  public boolean terminatedNormally;
  public int      returnValue;
  public CpuUsage runLocalUsage;
  public CpuUsage runRemoteUsage;
  public CpuUsage totalLocalUsage;
  public CpuUsage totalRemoteUsage;
  public double   sentBytes;
  public double   receivedBytes;
  public double   totalSentBytes;
  public double   totalReceivedBytes;

  public EventJobTerminated(RecordExpr expr){
	super(expr);
	type = 
	  ((Constant)expr.lookup("EventTypeNumber")).intValue();

	terminatedNormally = 
	  ((Constant)expr.lookup("TerminatedNormally")).booleanValue();
	returnValue = 
	  ((Constant)expr.lookup("ReturnValue")).intValue();
	runLocalUsage = new CpuUsage(
	  ((Constant)expr.lookup("RunLocalUsage")).stringValue());
	runRemoteUsage = new CpuUsage(
	  ((Constant)expr.lookup("RunRemoteUsage")).stringValue());
	totalLocalUsage = new CpuUsage(
	  ((Constant)expr.lookup("TotalLocalUsage")).stringValue());
	totalRemoteUsage = new CpuUsage(
	  ((Constant)expr.lookup("TotalRemoteUsage")).stringValue());
	sentBytes = 
	  ((Constant)expr.lookup("SentBytes")).realValue();
	receivedBytes = 
	  ((Constant)expr.lookup("ReceivedBytes")).realValue();
	totalSentBytes = 
	  ((Constant)expr.lookup("TotalSentBytes")).realValue();
	totalReceivedBytes = 
	  ((Constant)expr.lookup("TotalReceivedBytes")).realValue();
	
  }

  public String toString(){
	return super.toString() + " " + returnValue + "\n\t" + totalRemoteUsage;
  }

}

















