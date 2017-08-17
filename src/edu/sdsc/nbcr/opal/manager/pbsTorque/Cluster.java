
package edu.sdsc.nbcr.opal.manager.pbsTorque;

import java.io.BufferedInputStream;
import java.util.ArrayList;

/**
 *
 * @author Mohamed M. El-Kalioby
 * @since  Nov. 8, 2009
 * @version 1.0
 *
 * This class is a representation of a cluster.
 */
public class Cluster {

    private Machine[] Nodes;

    public Machine[] getNodes() {
        return Nodes;
    }

    public void setNodes(Machine[] Nodes) {
        this.Nodes = Nodes;
    }

    public static Cluster getCluster() throws Exception
    {
        Process p = Runtime.getRuntime().exec("qnodes");
        BufferedInputStream errStream = new BufferedInputStream(p.getErrorStream());
        p.waitFor();
        if (errStream.available()>0)
        {
            byte[] errdata = new byte[errStream.available()];
            errStream.read(errdata, 0, errStream.available());
            //String st = new String(errdata);
            throw new Exception(new String(errdata));
          }
        BufferedInputStream out = new BufferedInputStream(p.getInputStream());
        byte[] data = new byte[out.available()];
        out.read(data,0,out.available());
        p.getErrorStream().close();
        p.getOutputStream().close();
        String Result = new String(data);
        out.close();
        String[] Nodes = Result.split("\n");
        String[] line;
        String header,value;
        Machine Node;
        Core[] Cores=null;
        ArrayList<Machine> Machines = new ArrayList<Machine>();
        int i=0;
        while(i<Nodes.length)
        {
            Node = new Machine();
            Node.setName(Nodes[i]);
            i++;

            while (i<Nodes.length )
            {
                if (!Nodes[i].startsWith(" "))
                {
                    i++;
                    break;
                }
            
                line  = Nodes[i].split("=");
                header = line[0].trim();
                value = line[1].trim();

                if ("state".equals(header))
                    Node.setState(value);
                else if ("np".equals(header))
                {
                    Node.setNp(value);
                    int np= Integer.parseInt(Node.getNp());
                    Cores= new Core[np];
                    for (int n=0; n<np; n++)
                    {
                        Cores[n] = new Core(""+n);
                    }
                }
                else if ("ntype".equals(header))
                    Node.setNtype(value);
                else if ("jobs".equals(header))
                {
                    String[] jobs = value.split(", ");
                    Job jo;
                    //Job[] Jobs = new Job[jobs.length];
                    for (int j=0; j<jobs.length; j++)
                    {
                        String[] c  = jobs[j].split("/");
                        String Jid = c[1];
                        jo=Job.getJobById(Jid);
                        int core = Integer.parseInt(c[0]);
                        Cores[core].setJob(jo);

                    }
                    
                    
                }
                i++;
                }
                Node.setCores(Cores);
                Machines.add(Node);
            }
        Cluster c = new Cluster();
        c.setNodes(Machines.toArray(new Machine[Machines.size()]));
        return c;

        }


        
    }
    

