package edu.sdsc.nbcr.opal.dashboard.util;

import org.hibernate.Session;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.util.Date;
import org.apache.log4j.Logger;

import edu.sdsc.nbcr.opal.dashboard.persistence.DBManager;
import edu.sdsc.nbcr.opal.dashboard.util.DateHelper;

/**
 * Tests for the edu.sdsc.nbcr.opal.dashboard.util package
 *
 * @author Luca Clementi
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
        //String configFile = "configs/pdb2pqr_config.xml";
        //String workingDir = "samples";

        DBManager dbManager = new DBManager();

        //test the getServicesList
        assertTrue("We could not get connection to the database.", dbManager.isConnected());
        String [] servicesList = dbManager.getServicesList();
        assertNotNull("The list of services should not be null", servicesList);

        //prepare the data to run the query and test some date procedure
        String startDateStr = "01/01/2009";
        String endDateStr = "02/10/2009";
        int numDays = 41;
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = DateHelper.parseDate(startDateStr);
            endDate = DateHelper.parseDate(endDateStr);
        }catch (Exception e) {
            e.printStackTrace();
            fail("Impossible to parse the date! That's bad...");
        }
        assertNotNull("Impossible to parse the date! It returned null...", startDate);
        assertNotNull("Impossible to parse the date! It returned null...", endDate);

        //testing the queries
        double [] resultHolder = null;
        String [] queryTypes = {"hits", "exectime", "error"};
        for(String serviceName :  servicesList ){
            for (String queryType : queryTypes ) {
                resultHolder = dbManager.getResultsTimeseries(startDate, endDate, serviceName, queryType);
                assertNotNull("The result for " + queryType + " with start date " + startDateStr + 
                    " and endDate " + endDateStr + " returned null!", resultHolder);
                assertTrue("The query " + queryType + " with start date " + startDateStr +
                    " and endDate " + endDateStr + " returned a wrong number of results\nExpected " + 
                    numDays + " but returned " + resultHolder.length, resultHolder.length == numDays);
            }//for
        }//for

        //testing the queries with wrong dates 
        //start is bigger than end
        startDateStr = "03/01/2009";
        endDateStr = "02/10/2009";
        try {
            startDate = DateHelper.parseDate(startDateStr);
            endDate = DateHelper.parseDate(endDateStr);
        }catch (Exception e) {
            e.printStackTrace();
            fail("Impossible to parse the date! That's bad...");
        }
        assertNotNull("Impossible to parse the date! It returned null...", startDate);
        assertNotNull("Impossible to parse the date! It returned null...", endDate);
        //testing the queries with wrong arguments
        for(String serviceName :  servicesList ){
            for (String queryType : queryTypes ) {
                resultHolder = dbManager.getResultsTimeseries(startDate, endDate, serviceName, queryType);
                assertNull("The result for " + queryType + " with start date " + startDateStr +
                    " and endDate " + endDateStr + " did not return null!", resultHolder);
            }//for
        }//for

    }
}

