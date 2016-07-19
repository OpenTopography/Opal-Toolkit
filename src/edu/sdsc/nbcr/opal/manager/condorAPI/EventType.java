package edu.sdsc.nbcr.opal.manager.condorAPI;
/**
 * Condor Event Type constants
 */

public interface EventType {
    /** Job submitted             */  static final int SUBMIT                   = 0;
    /** Job now running           */  static final int EXECUTE                  = 1;
    /** Error in executable       */  static final int EXECUTABLE_ERROR         = 2;
    /** Job was checkpointed      */  static final int CHECKPOINTED             = 3;
    /** Job evicted from machine  */  static final int JOB_EVICTED              = 4;
    /** Job terminated            */  static final int JOB_TERMINATED           = 5;
    /** Image size of job updated */  static final int IMAGE_SIZE               = 6;
    /** Shadow threw an exception */  static final int SHADOW_EXCEPTION         = 7;
    /** Generic Log Event         */  static final int GENERIC                  = 8;
    /** Job Aborted               */  static final int JOB_ABORTED              = 9;
    /** Job was suspended         */  static final int JOB_SUSPENDED            = 10;
    /** Job was unsuspended       */  static final int JOB_UNSUSPENDED          = 11;
    /** Job was held              */  static final int JOB_HELD                 = 12;
    /** Job was released          */  static final int JOB_RELEASED             = 13;
    /** Parallel Node executed    */  static final int NODE_EXECUTE             = 14;
    /** Parallel Node terminated  */  static final int NODE_TERMINATED          = 15;
    /** POST script terminated    */  static final int POST_SCRIPT_TERMINATED   = 16;
    /** Job Submitted to Globus   */  static final int GLOBUS_SUBMIT            = 17;
    /** Globus Submit failed      */  static final int GLOBUS_SUBMIT_FAILED     = 18;
    /** Globus Resource Up        */  static final int GLOBUS_RESOURCE_UP       = 19;
    /** Globus Resource Down      */  static final int GLOBUS_RESOURCE_DOWN     = 20;
    /** Remote Error              */  static final int REMOTE_ERROR             = 21;
    /** RSC socket lost           */  static final int JOB_DISCONNECTED         = 22;
    /** RSC socket re-established */  static final int JOB_RECONNECTED          = 23;
    /** RSC reconnect failure     */  static final int JOB_RECONNECT_FAILED     = 24;

  static final String [] EventNames = {
"SUBMIT",
"EXECUTE",
"EXECUTABLE_ERROR",
"CHECKPOINTED",
"JOB_EVICTED",
"JOB_TERMINATED",
"IMAGE_SIZE",
"SHADOW_EXCEPTION",
"GENERIC",
"JOB_ABORTED",
"JOB_SUSPENDED",
"JOB_UNSUSPENDED",
"JOB_HELD",
"JOB_RELEASED",
"NODE_EXECUTE",
"NODE_TERMINATED",
"POST_SCRIPT_TERMINATED",
"GLOBUS_SUBMIT",
"GLOBUS_SUBMIT_FAILED",
"GLOBUS_RESOURCE_UP",
"GLOBUS_RESOURCE_DOWN",
"REMOTE_ERROR",
"JOB_DISCONNECTED",
"JOB_RECONNECTED",
"JOB_RECONNECT_FAILED"
  };
}
