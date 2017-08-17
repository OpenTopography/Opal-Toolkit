/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.sdsc.nbcr.opal.manager.pbsTorque;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author Mohamed M. El-Kalioby
 * @since  Nov. 8, 2009
 * @version 1.0
 *
 * This class handles the server's communication.
 */
public class Server {
/**
 * Return all the jobs done by the Server
 * @return Array of Jobs
 * @throws IOException
 * @throws InterruptedException
 */

    public static Job[] Jobs() throws IOException, InterruptedException
    {
        String[] res;
        int NameIndex, UserIndex,TimeIndex,StatusIndex,QueueIndex;
        Process p = Runtime.getRuntime().exec("qstat");
                p.waitFor();

               BufferedInputStream ef = new BufferedInputStream(p.getInputStream());
               byte[] data = new byte[ef.available()];
               ef.read(data, 0, ef.available());
               ef.close();
               p.getOutputStream().close();
                p.getErrorStream().close();
               String Result = new String(data);
               String[] Jobs  = Result.split("\n");
               String JobName;
               //res=new String[Jobs.length -2];
               NameIndex = Jobs[0].indexOf("Name");
               UserIndex = Jobs[0].indexOf("User");
               TimeIndex = Jobs[0].indexOf("Time");
               StatusIndex=Jobs[0].indexOf("S");
               QueueIndex=Jobs[0].indexOf("Queue");
               Job[] js= new Job[Jobs.length-2];

               for (int i=2;i<Jobs.length;i++)
               {
                   js[i-2]=new Job();
                   js[i-2].setId(Jobs[i].substring(0, Jobs[i].indexOf(" ",0)));
                   js[i-2].setName(Jobs[i].substring(NameIndex, Jobs[i].indexOf(" ",NameIndex)));
                   js[i-2].setOwner(Jobs[i].substring(UserIndex, Jobs[i].indexOf(" ",UserIndex)));
                   js[i-2].setWallTime(Jobs[i].substring(TimeIndex, Jobs[i].indexOf(" ",TimeIndex)));
                   js[i-2].setStatus(Jobs[i].substring(StatusIndex, Jobs[i].indexOf(" ",StatusIndex)));
                   js[i-2].setQueue(Jobs[i].substring(QueueIndex, Jobs[i].indexOf(" ",QueueIndex)));
               }

               return js;
    }


    public static Job[] Jobs(String Filter) throws IOException, InterruptedException
    {
        String[] res;
        int NameIndex, UserIndex,TimeIndex,StatusIndex,QueueIndex;
        Process p = Runtime.getRuntime().exec("qstat");
                p.waitFor();

               BufferedInputStream ef = new BufferedInputStream(p.getInputStream());
               byte[] data = new byte[ef.available()];
               ef.read(data, 0, ef.available());
               ef.close();
               p.getOutputStream().close();
                p.getErrorStream().close();
               String Result = new String(data);
               String[] Jobs  = Result.split("\n");
               String JobName;
               //res=new String[Jobs.length -2];
               NameIndex = Jobs[0].indexOf("Name");
               UserIndex = Jobs[0].indexOf("User");
               TimeIndex = Jobs[0].indexOf("Time");
               StatusIndex=Jobs[0].indexOf("S");
               QueueIndex=Jobs[0].indexOf("Queue");
               ArrayList<Job> js= new ArrayList<Job>();
               Job job;
               for (int i=2;i<Jobs.length;i++)
               {
                   String Name= Jobs[i].substring(NameIndex, Jobs[i].indexOf(" ",NameIndex));
                   int j=Name.indexOf(Filter);
                   if (j >-1)
                   {

                   job=new Job();
                   job.setId(Jobs[i].substring(0, Jobs[i].indexOf(" ",0)));
                   job.setName(Name);
                   job.setOwner(Jobs[i].substring(UserIndex, Jobs[i].indexOf(" ",UserIndex)));
                   job.setWallTime(Jobs[i].substring(TimeIndex, Jobs[i].indexOf(" ",TimeIndex)));
                   job.setStatus(Jobs[i].substring(StatusIndex, Jobs[i].indexOf(" ",StatusIndex)));
                   job.setQueue(Jobs[i].substring(QueueIndex, Jobs[i].indexOf(" ",QueueIndex)));
                   js.add(job);
                   }
               }

               return js.toArray(new Job[js.size()]);
    }

    public static Job[] QueuedJobs() throws IOException, InterruptedException
    {
        String[] res;
        int NameIndex, UserIndex,TimeIndex,StatusIndex,QueueIndex;
        Process p = Runtime.getRuntime().exec("qstat");
                p.waitFor();
p.getOutputStream().close();
                p.getErrorStream().close();
               BufferedInputStream ef = new BufferedInputStream(p.getInputStream());
               byte[] data = new byte[ef.available()];
               ef.read(data, 0, ef.available());
               ef.close();
               String Result = new String(data);
               String[] Jobs  = Result.split("\n");
               String JobName;
               //res=new String[Jobs.length -2];
               NameIndex = Jobs[0].indexOf("Name");
               UserIndex = Jobs[0].indexOf("User");
               TimeIndex = Jobs[0].indexOf("Time");
               StatusIndex=Jobs[0].indexOf("S");
               QueueIndex=Jobs[0].indexOf("Queue");
               ArrayList<Job> js= new ArrayList<Job>();
               Job job;
               for (int i=2;i<Jobs.length;i++)
               {
                   String Status= Jobs[i].substring(StatusIndex, Jobs[i].indexOf(" ",StatusIndex));
                   if ("H".equals(Status) || "Q".equals(Status))
                   {

                   job=new Job();
                   job.setId(Jobs[i].substring(0, Jobs[i].indexOf(" ",0)));
                   job.setName(Jobs[i].substring(NameIndex, Jobs[i].indexOf(" ",NameIndex)));
                   job.setOwner(Jobs[i].substring(UserIndex, Jobs[i].indexOf(" ",UserIndex)));
                   job.setWallTime(Jobs[i].substring(TimeIndex, Jobs[i].indexOf(" ",TimeIndex)));
                   job.setStatus(Status);
                   job.setQueue(Jobs[i].substring(QueueIndex, Jobs[i].indexOf(" ",QueueIndex)));
                   js.add(job);
                   }
               }

               return js.toArray(new Job[js.size()]);
    }
/**
 * To delete a job from the Queue
 * @param JobID
 * @throws Exception
 */
    public static void deleteJob(String JobID) throws Exception
    {
            Process p = Runtime.getRuntime().exec("qdel " + JobID);
                p.waitFor();
p.getOutputStream().close();
                p.getErrorStream().close();
                BufferedInputStream errStream = new BufferedInputStream(p.getErrorStream());
               if (errStream.available()>0)
               {
               byte[] errdata = new byte[errStream.available()];
               errStream.read(errdata, 0, errStream.available());
               errStream.close();
               //String st = new String(errdata);
               throw new Exception(new String(errdata));

               }

    }
    public static Cluster State() throws Exception
    {
        return Cluster.getCluster();
    }

}
