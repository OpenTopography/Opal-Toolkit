package edu.sdsc.nbcr.opal.manager.condorAPI;

import java.io.*;
import java.util.*;
import java.util.regex.*;

class Queue{
  int times;
  Queue(int times){
	this.times = times;
  }
  public String toString(){
	return "queue " + times;
  }
}


class Pair{
  String key;
  String val;
  Pair(String key, String val){
	this.key = key;
	this.val = val;
  }
  public String toString(){
	return key + " = " + val;
  }
}

/**
 * This class stands for a submit file for condor.
 * Users can create this object ether from an existing file
 * or from scratch within his/her programs.
 */

public class JobDescription {
  List attributes = new ArrayList();
  HandlerSet handlers = new HandlerSet();

  String filename; 
  boolean fileGiven = false;

  /**
   * Creates a blank job description to be filled by addXXX.
   */
  public JobDescription(){
  }

  /**
   * Creates a description file from an existing submit file.
   * @param filename filename for the submit file
   */
  public JobDescription(String filename) throws CondorException {
	if (!(new File(filename)).exists())
	  throw new CondorException("File " + filename + " does not exist");
	this.filename = filename;
	fileGiven = true;
  }

  /**
   *
   */
  public void addAttribute(String name, String val) throws CondorException {
	if (fileGiven) throw new CondorException("The submit file is given");
	attributes.add(new Pair(name, val));
  }

  /** 
   * Adds "queue TIMES" line to the submit file.
   * @param val - times to be queued
   */
  public void addQueue(int val) throws CondorException {
	if (fileGiven) throw new CondorException("The submit file is given");
	attributes.add(new Queue(val));
  }

  /** 
   * Adds "queue" line to the submit file.
   */
  public void addQueue() throws CondorException {
	if (fileGiven) throw new CondorException("The submit file is given");
	attributes.add(new Queue(1));
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
   * Sets a handler for a specific kind of events
   * @param eventType - the Event Type
   * @param handler   - Handler for the event
   */
  public void setHandler(int eventType, Handler handler){
	handlers.setHandler(eventType, handler);
  }
  


  /*****************************************************************
	  private or package access functions
  *****************************************************************/

  byte [] getByteArray(){
	ByteArrayOutputStream bs = new ByteArrayOutputStream();
	PrintWriter pw = new PrintWriter(new OutputStreamWriter(bs));
	Iterator iterator = attributes.iterator();
	while (iterator.hasNext())
	  pw.println(iterator.next());
	pw.close();
	return bs.toByteArray();
  }

  Cluster submit(Condor condor) throws CondorException{
	String tmpFilename = null;
	InputStream is;
	try {
	  if (!fileGiven) 
		is = new ByteArrayInputStream(this.getByteArray());		
	  else 
		is = new FileInputStream(filename);
	  tmpFilename = createTmpSubmit(is);
	}
	catch (IOException e){
	  e.printStackTrace();
	  throw new CondorException(e.toString());
	}	
	Cluster cluster = condor.submitFile(tmpFilename);
	// start log monitoring after submission when the log file becomes available
	condor.startLogMonitor();
	cluster.setHandlerSet(handlers);
	return cluster;
  }

  private String createTmpSubmit(InputStream is) throws IOException {
	LineNumberReader lnr = new LineNumberReader(new InputStreamReader(is));

	File tmpFile = File.createTempFile("/condor", ".submit");
	PrintWriter pw = new PrintWriter(new FileWriter(tmpFile));

	String patternString = "^(\\S+)\\s*=\\s*(\\S+)$";
	Pattern pattern = Pattern.compile(patternString);
	
	String tmp;
	while ((tmp = lnr.readLine()) != null){
	  Matcher matcher = pattern.matcher(tmp);
	  if (!matcher.matches()){ // does not match XXX = YYY form; ignore
		pw.println(tmp);
		continue;
	  }
	  String key = matcher.group(1);
	  pw.println(tmp);
	}
	pw.close();
	return tmpFile.getAbsolutePath();
  }
}
