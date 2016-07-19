package edu.sdsc.nbcr.opal.state;

/**
 * An exception class specific thrown by classes that manage state
 */
public class StateManagerException extends Exception {

    /**
     * Constructor
     * 
     * @param message the exception message
     */
    public StateManagerException(String message) {
        super(message);
    }
}
