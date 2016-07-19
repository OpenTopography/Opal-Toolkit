package edu.sdsc.nbcr.opal.manager.condorAPI;

import java.io.*;
import condor.classad.*;

/** 
 * Log monitor for xml formatted condor log
 */ 
class LogMonitor implements Runnable {
  Condor condor;
  String filename;
  
  LineNumberReader lnr;
  ClassAdParser parser = new ClassAdParser(ClassAdParser.XML);

  int counter = 0;
  int maxError = 10;
  int sleepSec = 5;
  Thread t;
  
  LogMonitor(Condor condor, String filename, int sleepSec){
	this.condor   = condor;
	this.filename = filename;
	this.sleepSec = sleepSec;
  }

  void setInterval(int sleepSec){
	this.sleepSec = sleepSec;
	if (t != null)
	  t.interrupt();  // to make effictive the change of interval
  }

  void openLnr(){
	if (lnr == null) {
	  try {
		FileReader reader = new FileReader(filename);
		lnr = new LineNumberReader(reader);
	  } catch (IOException e){
		System.err.println("Failed to open filename " + filename + ", retry.");
		counter++;
		return;
	  }
	} 
  }

  StringBuffer sb = new StringBuffer();

  RecordExpr readExpr() throws IOException{
	String tmp;
	while (true){
	  tmp = lnr.readLine();
	  if (tmp == null) 
		return null;
	  sb.append(tmp);
	  if (tmp.trim().equals("</c>"))
		break;
	}
	String adString = sb.toString();
	sb = new StringBuffer();  // set the new string buffer for the next time;

	//	System.out.println(adString);


	// this line should not be required, 
    // this is here just to avoid a bug in classad library.
	parser = new ClassAdParser(ClassAdParser.XML);

	parser.reset(adString);
	return (RecordExpr) (parser.parse());
  }

  void readLog(){
	try {
	  RecordExpr expr;
	  while ((expr = readExpr()) != null){
		Event event = Event.getEvent(expr);
		if (condor != null)
		  condor.informEvent(event);
	  }
	} catch (IOException e){
	  e.printStackTrace();
	  counter++;
	}
  }

  public void start(){
	t = new Thread(this, "log Monitor for " + "filname");
	t.setDaemon(true);
	t.start();
  }

  public void run() {
	while (counter < maxError){
	  openLnr();
	  if (lnr != null){
		readLog();
	  }
	  try {
		Thread.sleep(sleepSec * 1000);
	  } catch (InterruptedException e){
		// ignore;
	  }
	}
	System.err.println("LogMonitor for " + filename + " exits: too many errors");
  }  

  /**
   * just for test
   */
  public static void main(String [] args){
	String filename = args[0];
	LogMonitor monitor = new LogMonitor(null, filename, 10);
	monitor.start();
  }
}

