package edu.sdsc.nbcr.opal.manager.condorAPI;

import java.util.*;

class HandlerSet implements Cloneable {
  protected Handler onSuccess = null;
  protected Handler onFailure = null;  
  protected Map map = new HashMap();
  
  void setHandlerOnSuccess(Handler handler){
	onSuccess = handler;
  }
  void setHandlerOnFailure(Handler handler){
	onFailure = handler;
  }

  void setHandler(int event, Handler handler){
	map.put(new Integer(event), handler);
  }

  Handler getHandlerOnSuccess(){
	return onSuccess;
  }

  Handler getHandlerOnFailure(){
	return onFailure;
  }

  Handler getHandler(int event){
	return (Handler)map.get(new Integer(event));
  }

  public Object clone(){
	HandlerSet tmp = new HandlerSet();
	tmp.setHandlerOnSuccess(onSuccess);
	tmp.setHandlerOnFailure(onFailure);
	tmp.map = (HashMap)(((HashMap)map).clone());
	return tmp;
  }
}
