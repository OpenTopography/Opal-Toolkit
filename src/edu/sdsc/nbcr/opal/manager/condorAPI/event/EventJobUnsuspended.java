package edu.sdsc.nbcr.opal.manager.condorAPI.event;
import edu.sdsc.nbcr.opal.manager.condorAPI.*;

import condor.classad.*;

public class EventJobUnsuspended extends Event {
  public EventJobUnsuspended(RecordExpr expr){
	super(expr);
	type = ((Constant)expr.lookup("EventTypeNumber")).intValue();
  }
}

















