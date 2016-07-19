package edu.sdsc.nbcr.opal.util;

import junit.framework.TestSuite;
import junit.framework.Test;

import edu.sdsc.nbcr.opal.state.HibernateUtil;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;


/**
 * TestSuite that runs all the Opal tests
 *
 * @author Sriram Krishnan
 */
public class Tests {

    // get an instance of the log4j Logger
    private static Logger logger = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
        System.exit(0);
    }
  
    public static void setUp() {
        // initialize hibernate
        logger.info("Initializing hibernate");
        HibernateUtil.setConfFile("hibernate-opal.cfg.tests.xml");
        SessionFactory session = HibernateUtil.getSessionFactory();
    }


    public static Test suite ( ) {
        TestSuite suite= new TestSuite("All JUnit Tests for Opal2");
        setUp();
        suite.addTest(edu.sdsc.nbcr.opal.state.PackageTest.suite());
        suite.addTest(edu.sdsc.nbcr.opal.util.PackageTest.suite());
        suite.addTest(edu.sdsc.nbcr.opal.dashboard.util.PackageTest.suite());

        return suite;
    }
}
