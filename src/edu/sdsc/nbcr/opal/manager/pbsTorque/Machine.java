/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.sdsc.nbcr.opal.manager.pbsTorque;

import java.util.HashMap;

/**
 *
 * @author Mohamed M. El-Kalioby
 * @since  Nov. 8, 2009
 * @version 1.0
 *
 * This class is a representation of a Computing Machine.
 */
public class Machine {
    private String Name;
    private Core[] Cores;
    private String state;
    private HashMap<String, String> status;
    private String np;
    private String ntype;

    /**
     * @return the Name
     */
    public String getName() {
        return Name;
    }

    /**
     * @param Name the Name to set
     */
    public void setName(String Name) {
        this.Name = Name;
    }

    public Core[] getCores() {
        return Cores;
    }

    public void setCores(Core[] Cores) {
        this.Cores = Cores;
    }

   

    /**
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * @return the status
     */
    public HashMap<String, String> getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(HashMap<String, String> status) {
        this.setStatus(status);
    }

    
    /**
     * @return the np
     */
    public String getNp() {
        return np;
    }

    /**
     * @param np the np to set
     */
    public void setNp(String np) {
        this.np = np;
    }

    /**
     * @return the ntype
     */
    public String getNtype() {
        return ntype;
    }

    /**
     * @param ntype the ntype to set
     */
    public void setNtype(String ntype) {
        this.ntype = ntype;
    }

    




}
