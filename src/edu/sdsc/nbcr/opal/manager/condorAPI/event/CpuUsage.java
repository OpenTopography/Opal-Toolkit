package edu.sdsc.nbcr.opal.manager.condorAPI.event;
import edu.sdsc.nbcr.opal.manager.condorAPI.*;
import java.util.regex.*;

public class CpuUsage{
  /** User cpu usage in seconds */
  int user;
  /** System cpu usage in seconds */
  int system;

  /**
   * parse string like 'Usr 0 00:00:00, Sys 0 00:00:00'
   * @param str initializing string;
   */ 
  public CpuUsage(String str) {
	Pattern pattern = Pattern.compile("Usr (\\d*) (\\d\\d):(\\d\\d):(\\d\\d), " +
									  "Sys (\\d*) (\\d\\d):(\\d\\d):(\\d\\d)" );
	Matcher matcher = pattern.matcher(str);
	if (!matcher.matches()){
	  System.err.println("faied to parse usage. ignore :" + str);
	  return;
	}
	int day, hour, min, sec;
	day  = Integer.parseInt(matcher.group(1));
	hour = Integer.parseInt(matcher.group(2));
	min  = Integer.parseInt(matcher.group(3));
	sec  = Integer.parseInt(matcher.group(4));
	user = ((day * 24 + hour) * 60 + min) * 60 + sec;

	day  = Integer.parseInt(matcher.group(5));
	hour = Integer.parseInt(matcher.group(6));
	min  = Integer.parseInt(matcher.group(7));
	sec  = Integer.parseInt(matcher.group(8));
	system = ((day * 24 + hour) * 60 + min) * 60 + sec;
  }

  public String toString(){
	return "(Usr, Sys = " + user + ", " + system  + ")";
  }

  /**
   * main routine for tests
   * @param args command line arguments
   */
  public static void main(String [] args){
	System.out.println(new CpuUsage("Usr 0 00:10:20, Sys 0 01:10:00"));
  }


}
