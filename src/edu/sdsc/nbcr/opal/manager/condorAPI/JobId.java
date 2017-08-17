package edu.sdsc.nbcr.opal.manager.condorAPI;

/**
 * @author nakada
 *
 * represent jobId
 * 
 */
public class JobId implements Cloneable{
  public int clusterNo;
  public int jobNo;
  
  public JobId(int clusterNo, int jobNo){ 
	this.clusterNo = clusterNo;
	this.jobNo = jobNo;
  }
  
  public int hashCode(){
	return clusterNo + jobNo;
  }
  
  public boolean equals(Object o ){
	if (!(o instanceof JobId)) 
	  return false;
	if ((clusterNo == ((JobId)o).clusterNo) && 
		(jobNo     == ((JobId)o).jobNo)){
	  return true;	
	}	
	return false;
  }

  public String toString(){
	return clusterNo + "." + jobNo;
  }

  public Object clone(){

	return new JobId(clusterNo, jobNo);
  }

}
