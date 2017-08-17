package edu.sdsc.nbcr.opal.manager.condorAPI.event;
import edu.sdsc.nbcr.opal.manager.condorAPI.*;

import condor.classad.*;

public class EventJobEvicted extends Event {
  public boolean checkpointed;
  public CpuUsage runLocalUsage;
  public CpuUsage runRemoteUsage;
  public double   sentBytes;
  public double   receivedBytes;
  public boolean  terminatedAndRequeued;
  public boolean  terminatedNormally;

  public EventJobEvicted(RecordExpr expr){
	super(expr);
	type = ((Constant)expr.lookup("EventTypeNumber")).intValue();
	runLocalUsage = new CpuUsage(
	  ((Constant)expr.lookup("RunLocalUsage")).stringValue());
	runRemoteUsage = new CpuUsage(
	  ((Constant)expr.lookup("RunRemoteUsage")).stringValue());
	sentBytes = 
	  ((Constant)expr.lookup("SentBytes")).realValue();
	receivedBytes = 
	  ((Constant)expr.lookup("ReceivedBytes")).realValue();
	terminatedAndRequeued =
	  ((Constant)expr.lookup("TerminatedAndRequeued")).booleanValue();
	terminatedNormally =
	  ((Constant)expr.lookup("TerminatedNormally")).booleanValue();
  }
}

















