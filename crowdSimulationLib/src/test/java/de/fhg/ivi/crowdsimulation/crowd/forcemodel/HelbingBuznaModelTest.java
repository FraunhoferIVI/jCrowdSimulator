package de.fhg.ivi.crowdsimulation.crowd.forcemodel;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.math.Vector2D;

import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.crowd.CrowdFactory;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingBuznaModel;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.NumericIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.SemiImplicitEulerIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.Route;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.RouteFactory;
import de.fhg.ivi.crowdsimulation.geom.Quadtree;

public class HelbingBuznaModelTest
{

    // Logger
    private static final Logger   logger                    = LoggerFactory
        .getLogger(HelbingBuznaModelTest.class);

    // Geometry-depending parameters
    private Pedestrian            pedestrianOne             = null;

    private Pedestrian            pedestrianTwo             = null;

    private Pedestrian            pedestrianThree           = null;

    private ArrayList<Pedestrian> pedestrians               = null;

    private Boundary              boundary                  = null;

    private Vector2D              normalizedDirectionVector = new Vector2D(0, 1);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception
    {
    }

    @Before
    public void setUp() throws Exception
    {

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

        // create geometry for waypoint target line
        // WKTReader reader = new WKTReader(geometryFactory);
        // Geometry verticalGeometryOne = reader.read("LINESTRING(9 10, 11 10)");
        // Geometry verticalGeometryTwo = reader.read("LINESTRING(19 20, 21 20)");

        // set waypoints and wayPointList
        Coordinate coordOne = new Coordinate(0, 10);
        Coordinate coordTwo = new Coordinate(0, 20);
        List<Coordinate> coordList = new ArrayList<>();
        coordList.add(coordOne);
        coordList.add(coordTwo);
        HelbingBuznaModel hbm = new HelbingBuznaModel();
        NumericIntegrator ni = new SemiImplicitEulerIntegrator();
        Quadtree quadtree = new Quadtree();

        RouteFactory rf = new RouteFactory(quadtree);
        Route route = rf.createRouteFromCoordinates(coordList);
        CrowdFactory cf = new CrowdFactory(ni, hbm, quadtree, null);

        // set three pedestrians
        pedestrianOne = cf.createPedestrian(0, 0, 1.3f, 2);
        pedestrianTwo = cf.createPedestrian(0.5, 0, 1.3f, 2);
        pedestrianThree = cf.createPedestrian(1, 0, 1.3f, 2);

        pedestrianOne.setRoute(route, System.currentTimeMillis());
        pedestrianTwo.setRoute(route, System.currentTimeMillis());
        pedestrianThree.setRoute(route, System.currentTimeMillis());

        pedestrianOne.setId(1);
        pedestrianTwo.setId(2);
        pedestrianThree.setId(3);

        // add this peds above to an arraylist
        pedestrians = new ArrayList<>();
        pedestrians.add(pedestrianOne);
        pedestrians.add(pedestrianTwo);
        pedestrians.add(pedestrianThree);

        // create geometry for boundary
        Coordinate[] coords = new Coordinate[] { new Coordinate(1.1, 10), new Coordinate(1.1, -10),
            new Coordinate(3, -10), new Coordinate(3, 10), new Coordinate(1.1, 10) };
        LinearRing ring = geometryFactory.createLinearRing(coords);
        LinearRing holes[] = null;
        Polygon polygon = geometryFactory.createPolygon(ring, holes);
        Geometry geom = polygon;

        // create boundary based on geometry
        boundary = new Boundary(geom, 0);
    }

    @After
    public void tearDown() throws Exception
    {
    }

    /**
     * Tests whether
     * {@link HelbingBuznaModel#intrinsicForce(Vector2D, Vector2D, Vector2D, float, float, float)}
     * works like expected.
     */
    @Test
    public void testIntrinsicForce()
    {

        float averageVelocityOnRoute = (float) 1.3;
        float actualDesiredVelocity = (float) 1.3;
        HelbingBuznaModel hbm = new HelbingBuznaModel();

        Vector2D resultingForce = hbm.intrinsicForce(pedestrianOne.getCurrentPositionVector(),
            pedestrianOne.getCurrentVelocityVector(), normalizedDirectionVector,
            averageVelocityOnRoute, actualDesiredVelocity,
            pedestrianOne.getMaximumDesiredVelocity());

        assertEquals(0.000, resultingForce.getX(), 0.001);
        assertEquals(1.299, resultingForce.getY(), 0.001);

        logger
            .trace("testIntrinsicForce(), " + resultingForce.getX() + "  " + resultingForce.getY());
    }

    // /**
    // * Tests whether
    // * {@link HelbingBuznaModel#interactPedestrian(Vector2D, Vector2D, Vector2D, Pedestrian)}
    // works
    // * like expected.
    // */
    // @Test
    // public void testInteractPedestrian()
    // {
    //
    // HelbingBuznaModel hbm = new HelbingBuznaModel();
    //
    // Vector2D resultingForce = hbm.interactPedestrian(pedestrianOne.getCurrentPositionVector(),
    // pedestrianOne.getCurrentVelocityVector(), normalizedDirectionVector, pedestrianTwo);
    //
    // assertEquals( -4.946, resultingForce.x(), 0.001);
    // assertEquals(0.000, resultingForce.y(), 0.001);
    //
    // logger.info("testInteractPedestrian(), " + resultingForce.x() + " " + resultingForce.y());
    // }

    @Test
    public void testInteractBoundary()
    {

        HelbingBuznaModel hbm = new HelbingBuznaModel();

        Vector2D resultingForce = hbm.interactBoundary(pedestrianThree.getCurrentPositionVector(),
            boundary);

        // TODO test why boundaries and pedestrians don't interact with each other
        assertEquals(0.000, resultingForce.getX(), 0.001);
        assertEquals(0.000, resultingForce.getY(), 0.001);

        logger.trace(
            "testInteractBoundary(), " + resultingForce.getX() + "  " + resultingForce.getY());
    }
}
