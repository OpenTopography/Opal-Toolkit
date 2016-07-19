package edu.sdsc.nbcr.opal.manager;

/**
 * An exception class specific used by the Opal JobManagers
 */
public class JobManagerException extends Exception {
    
    /**
     * Constructor
     * 
     * @param message the exception message
     */
    public JobManagerException(String message) {
	super(message);
    }
}