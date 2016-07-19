package edu.sdsc.nbcr.opal.manager.condorAPI.event;
import edu.sdsc.nbcr.opal.manager.condorAPI.*;

import condor.classad.*;

public class EventGeneric extends Event {
  public EventGeneric(RecordExpr expr){
	super(expr);
	type = ((Constant)expr.lookup("EventTypeNumber")).intValue();
  }
}

















