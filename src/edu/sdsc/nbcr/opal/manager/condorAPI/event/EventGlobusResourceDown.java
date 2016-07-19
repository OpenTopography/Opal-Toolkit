package edu.sdsc.nbcr.opal.manager.condorAPI.event;
import edu.sdsc.nbcr.opal.manager.condorAPI.*;

import condor.classad.*;

public class EventGlobusResourceDown extends Event {
  public EventGlobusResourceDown(RecordExpr expr){
	super(expr);
	type = ((Constant)expr.lookup("EventTypeNumber")).intValue();
  }
}

















