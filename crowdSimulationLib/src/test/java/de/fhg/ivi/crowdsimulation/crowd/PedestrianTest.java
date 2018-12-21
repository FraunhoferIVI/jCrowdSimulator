package de.fhg.ivi.crowdsimulation.crowd;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class PedestrianTest extends TestCase
{

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public PedestrianTest(String testName)
    {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static TestSuite suite()
    {
        return new TestSuite(PedestrianTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testPedestrian()
    {
        assertTrue(true);
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception
    {
    }

    @Override
    @Before
    public void setUp() throws Exception
    {
    }

    @Override
    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void test()
    {
        // fail("Not yet implemented");

        assertTrue(true);

    }
}
