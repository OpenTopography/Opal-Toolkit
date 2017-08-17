package edu.sdsc.nbcr.opal.manager.condorAPI;

import java.util.AbstractCollection;


/**
 * stands for the job cluster
 */

public class Cluster extends AbstractCollection{
  /** 
   * cluster id
   */
  public int id;

  /**
   * jobs in the cluster
   */
  Job [] jobs;
  HandlerSet handlers;


  /**
   * creates a cluster.  will be called by JobDescription
   */
  Cluster(int clusterId, int noOfJobs, Condor condor){
	id = clusterId;
	jobs = new Job[noOfJobs];
	for (int i = 0; i < noOfJobs; i++)
	  jobs[i] = new Job(new JobId(clusterId, i), condor);
  }

  /**
   * Sets a HandlerSet. will be called by JobDescription
   */
  void setHandlerSet(HandlerSet handlers){
	this.handlers = handlers;
	for (int i = 0; i < jobs.length; i++)
	  jobs[i].setHandlerSet(handlers);
  }

  /** 
   * Blocks until all the node in the cluster to finish.
   */
  public void waitFor() throws CondorException {
	for (int i = 0; i < jobs.length; i++)
	  jobs[i].waitFor();
  }

  /**
   * Returns a String that describes the content of this cluster briefly.
   */ 
  public String toString(){
	return "Cluster " + id + " : " + jobs.length + " jobs";
  }

  /**
   * Returns a String that describes the content of this cluster.
   */
  public String dump(){
	StringBuffer sb = new StringBuffer();
	for (int i = 0; i < jobs.length; i++){
	  sb.append(" ");
	  sb.append(jobs[i].toString());
	}
	return toString() + " -  [" +sb.toString() + " ]";
  }

  /**
   * return job
   */
  public Job getJob(int i){
	  return jobs[i];
  }
  
  /**
   * 
   */
  public int length(){
	  return jobs.length;
  }
  
  
  
  /**
   * Returns an iterator on the  cluster
   */
  public java.util.Iterator iterator(){
	return new Iterator();
  }


  /**
   * iterator returns each jobs in the cluster
   */ 
  public class Iterator implements java.util.Iterator{
	int index = 0;
	public Object next() throws java.util.NoSuchElementException{
	  if (index >= jobs.length)
		throw new java.util.NoSuchElementException();
	  return jobs[index++];
	}

	public boolean hasNext(){
	  if (index >= jobs.length)
		return false;
	  return true;
  	}

	public void remove() throws UnsupportedOperationException {
	  throw new UnsupportedOperationException();
	}
  }


  public int size() {
	  return jobs.length;
  }


}

