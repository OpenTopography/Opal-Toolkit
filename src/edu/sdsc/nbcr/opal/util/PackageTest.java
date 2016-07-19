package edu.sdsc.nbcr.opal.util;

import org.hibernate.Session;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import org.apache.log4j.Logger;

import edu.sdsc.nbcr.opal.AppConfigType;
import edu.sdsc.nbcr.opal.ArgumentsType;
import edu.sdsc.nbcr.common.TypeDeserializer;
import edu.sdsc.nbcr.opal.FaultType;

/**
 * Tests for the edu.sdsc.nbcr.opal.state package
 *
 * @author Sriram Krishnan
 */
public class PackageTest extends TestCase {

    // get an instance of the log4j Logger
    private static Logger logger = 
	Logger.getLogger(PackageTest.class.getName());

    public static void main(String[] args) {
	junit.textui.TestRunner.run(suite());
	System.exit(0);
    }


    public PackageTest(String name) {
	super(name);
    }

    public static Test suite() {
	return new TestSuite(PackageTest.class);
    }

    protected void setUp() {
    }
    
    protected void tearDown() {
    }

    public void testArgValidator() {
	String configFile = "configs/pdb2pqr_config.xml";
	String workingDir = "samples";

	System.out.println("Reading application configuration from file");
	AppConfigType appConfig = null;
	try {
	    appConfig = (AppConfigType) TypeDeserializer.getValue(configFile,
								  new AppConfigType());
	    assertNotNull("Can't create appConfig from configuration file",
			  appConfig);
	} catch (Exception e) {
	    logger.error(e.getMessage());
	    fail("Can't create appConfig from configuration file");
	    return;
	}

	ArgumentsType argsDesc = appConfig.getMetadata().getTypes();
	if (argsDesc == null) {
	    fail("Opal configuration file: " + configFile +
		 " does not include argument description");
	    return;
	}

	ArgValidator av = new ArgValidator(argsDesc);

	System.out.println("Validating correctness of arguments");
	String cmdArgs = "--noopt --verbose --ff=AMBER sample.pdb output.pqr";
	try {
	    boolean success = av.validateArgList(workingDir, 
						 cmdArgs);
	    if (success) {
		System.out.println("Argument validation successful");
	    } else {
		String msg = "Argument validation unsuccessful";
		System.err.println(msg);
		fail(msg);
	    }
	} catch (FaultType f) {
	    logger.error(f.getMessage1());
	    fail("Argument validation unsuccessful: " + f.getMessage1());
	}

	System.out.println("Validating incorrectness of arguments");
	cmdArgs = "--foo --verbose --ff=AMBER sample.pdb output.pqr";
	try {
	    boolean success = av.validateArgList(workingDir, 
						 cmdArgs);
	    // argument validation should fail
	    fail("Argument validation incorrectly returned successfully");
	} catch (FaultType f) {
	    String msg = f.getMessage1();
	    System.out.println("Argument validation unsuccessful as expected: " + 
			       msg);
	}
    }
}

