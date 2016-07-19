package edu.sdsc.nbcr.opal.manager.condorAPI;

/**
 * Condor Job Status constants
 */ 
public interface Status {
  static final int UNEXPANDED      = 0;
  static final int IDLE            = 1;
  static final int RUNNING         = 2;
  static final int REMOVED         = 3;
  static final int COMPLETED       = 4;
  static final int HELD            = 5;
  static final int SUBMISSION_ERR  = 6;

  static final String names[] = {
    "UNEXPANDED",
    "IDLE",
    "RUNNING",
    "REMOVED",
    "COMPLETED",
    "HELD",
    "SUBMISSION_ERR"
  };
}



