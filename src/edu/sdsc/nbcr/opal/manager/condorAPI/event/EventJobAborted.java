package edu.sdsc.nbcr.opal.manager.condorAPI.event;
import edu.sdsc.nbcr.opal.manager.condorAPI.*;

import condor.classad.*;

public class EventJobAborted extends Event {
  public String reason;

  public EventJobAborted(RecordExpr expr){
	super(expr);
	type = ((Constant)expr.lookup("EventTypeNumber")).intValue();
	reason = ((Constant)expr.lookup("Reason")).stringValue();
  }
}

















