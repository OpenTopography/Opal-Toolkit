package edu.sdsc.nbcr.opal.state;

import org.hibernate.Session;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.util.Date;

import edu.sdsc.nbcr.opal.StatusOutputType;
import edu.sdsc.nbcr.opal.JobStatisticsType;
import edu.sdsc.nbcr.opal.JobOutputType;
import edu.sdsc.nbcr.opal.OutputFileType;

import org.apache.axis.types.URI;
import org.apache.axis.types.URI.MalformedURIException;

import org.apache.log4j.Logger;

/**
 * Tests for the edu.sdsc.nbcr.opal.state package
 *
 * @author Sriram Krishnan
 */
public class PackageTest extends TestCase {

    // get an instance of the log4j Logger
    private static Logger logger = 
        Logger.getLogger(PackageTest.class.getName());

    // the hibernate session
    private static Session session = null;

    // initialize and use the same job id
    String jobID = "app" + System.currentTimeMillis();

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
        // initialize hibernate
        if (session == null) {
            logger.info("Initializing hibernate");
            session = HibernateUtil.getSessionFactory().openSession();
        }
    }

    protected void tearDown() {
    }

    public void testSaveJob() {

        // initialize info
        System.out.println("Testing insertion of job state into database");
        JobInfo info = new JobInfo();
        info.setJobID(jobID);
        info.setCode(0);
        info.setMessage("This is a test");
        info.setBaseURL("http://localhost/test");
        Date currentDate = new Date();
        info.setStartTimeDate(new java.sql.Date( currentDate.getTime() ) );
        info.setStartTimeTime(new java.sql.Time( currentDate.getTime() ) );
        info.setActivationTimeDate(new java.sql.Date( currentDate.getTime() ) );
        info.setActivationTimeTime(new java.sql.Time( currentDate.getTime() ) );
        info.setCompletionTimeDate(new java.sql.Date( currentDate.getTime() ) );
        info.setCompletionTimeTime(new java.sql.Time( currentDate.getTime() ) );
        info.setLastUpdateTime(new java.sql.Time(currentDate.getTime()));
        info.setLastUpdateDate(new java.sql.Date(currentDate.getTime()));
        info.setClientDN("CN=Test");
        info.setClientIP("127.0.0.1");
        info.setServiceName("Command-line");

        // save job info
        try {
            boolean status = HibernateUtil.saveJobInfoInDatabase(info);
            assertTrue("Insertion of job state failed with status: ",
                    status);
        } catch (Exception e) {
            logger.error(e.getMessage());
            fail("Insertion of job state failed");
        }
    }

    public void testSaveOutput() {
        // save output files
        System.out.println("Testing insertion of job outputs into database");
        JobOutputType outputs = new JobOutputType();
        try {
            outputs.setStdOut(new URI("http://localhost/test/stdout.txt"));
            outputs.setStdErr(new URI("http://localhost/test/stderr.txt"));
            OutputFileType[] files = new OutputFileType[1];
            files[0] = new OutputFileType();
            files[0].setName("foo.txt");
            files[0].setUrl(new URI("http://localhost/test/foo.txt"));
            outputs.setOutputFile(files);
            boolean status = HibernateUtil.saveOutputsInDatabase(jobID, outputs);
            assertTrue("Insertion of job outputs failed with status: ",
                    status);
        } catch (Exception e) {
            logger.error(e.getMessage());
            fail("Insertion of job outputs failed");
        }
    }

    public void testJobUpdate() {
        System.out.println("Testing update of job state");
        try {
            int rows = HibernateUtil.updateJobInfoInDatabase(jobID,
                    1,
                    "This is a test update",
                    "http://localhost/test",
                    "testHandle");
            assertEquals("Update of job status failed - number of rows",
                    1,
                    rows);
        } catch (Exception e) {
            logger.error(e.getMessage());
            fail("Update of job state failed");
        }
    }

    public void testSearchJob() {
        // do some searches
        System.out.println("Testing search for jobs");
        try {
            StatusOutputType status = HibernateUtil.getStatus(jobID);
            assertNotNull("Job search failed",
                    status);

            System.out.println("Job Status: " + jobID +
                    " - {" + status.getCode() +
                    ", " + status.getMessage() +
                    ", " + status.getBaseURL() + "}");
        } catch (Exception e) {
            logger.error(e.getMessage());
            fail("Job search failed");
        }
    }

    public void testSearchJobStats() {
        // do some searches
        System.out.println("Testing search for job statistics");
        try {
            JobStatisticsType stats = HibernateUtil.getStatistics(jobID);
            assertNotNull("Search for job statistics failed",
                    stats);

            System.out.println("Job Statistics: " + jobID +
                    " - {" + stats.getStartTime().getTime() +
                    ", " + stats.getActivationTime().getTime() +
                    ", " + stats.getCompletionTime().getTime() + "}");
        } catch (Exception e) {
            logger.error(e.getMessage());
            fail("Search for job statistics failed");
        }
    }

    public void testSearchOutput() {
        System.out.println("Testing search for job output");

        JobOutputType outputs = null;
        try {
            outputs = HibernateUtil.getOutputs(jobID);
            assertNotNull("Job output search failed",
                    outputs);

            System.out.println("Standard output: " + outputs.getStdOut());
            System.out.println("Standard error: " + outputs.getStdErr());
            OutputFileType[] files = outputs.getOutputFile();
            for (int i = 0; i < files.length; i++) {
                System.out.println(files[i].getName() + ": " + files[i].getUrl());
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            fail("Job output search failed");
        }
    }
}

