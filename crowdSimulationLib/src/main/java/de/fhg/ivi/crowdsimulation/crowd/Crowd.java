package de.fhg.ivi.crowdsimulation.crowd;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opensphere.geometry.algorithm.ConcaveHull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;

import de.fhg.ivi.crowdsimulation.CrowdSimulatorNotValidException;
import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.ForceModel;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.NumericIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.RungeKuttaIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.SemiImplicitEulerIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.SimpleEulerIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.Route;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.WayPoint;
import de.fhg.ivi.crowdsimulation.geom.GeometryTools;
import de.fhg.ivi.crowdsimulation.geom.Quadtree;
import de.fhg.ivi.crowdsimulation.math.MathTools;
import de.fhg.ivi.crowdsimulation.validate.ValidationTools;

/**
 * A {@link Crowd} consists of a {@link List} of {@link Group} objects, which in turn consist of a
 * {@link List} of {@link Pedestrian} objects.
 * <p>
 * When creating the {@link Crowd}, for each {@link Pedestrian} belonging to this {@link Crowd}, a
 * normal and a maximum walking speed is selected from Gaussian distribution given by the velocities
 * and standard deviations.
 * <p>
 * Moreover a {@link List} of {@link WayPoint}s defines the direction, in which the crowd and hence
 * all {@link Pedestrian}s of this {@link Crowd} will walk. Moreover, the applied {@link ForceModel}
 * for pedestrian movement is stored in this class.
 * <p>
 * This class also encapsulates all objects / methods that are relevant to compute the outlines
 * (clustered or non-clustered, convex or concave) of the {@link List} of {@link Pedestrian} objects
 *
 * @author hahmann/meinert
 */
public class Crowd implements ICrowd, Identifiable
{
    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger logger                 = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());

    /**
     * {@link List} object, which contains all {@link Group}s within this {@link Crowd}, each
     * {@link Group} containing 1 to n {@link Pedestrian}s
     */
    private List<Group>         groups;

    /**
     * {@link Route} object, which contains all {@link WayPoint}
     */
    private Route               route;

    /**
     * {@link Geometry} object, which denotes a outline of all {@link Pedestrian}
     */
    private List<Geometry>      crowdOutlines;

    /**
     * Crowd ID
     */
    private int                 id;

    /**
     * Can be one of the following methods of numerical mathematics to compute {@link Pedestrian}
     * movement - Simple Euler {@link SimpleEulerIntegrator}, Semi Implicit Euler
     * {@link SemiImplicitEulerIntegrator} or Runge Kutta {@link RungeKuttaIntegrator}.
     */
    private NumericIntegrator   numericIntegrator;

    /**
     * {@link ForceModel} object, which represents the pedestrian modeling approach.
     */
    private ForceModel          forceModel;

    /**
     * {@link Quadtree} object, which can be use to do spatial queries
     */
    private Quadtree            quadtree;

    /**
     * Thread Pool for parallelization of Pedestrian movement computation
     */
    private ThreadPoolExecutor  threadPool;

    /**
     * One out of two parameters of the {@link DBSCANClusterer}.
     * <p>
     * This parameter denotes the maximum radius (meters) of the neighborhood, in which a cluster
     * can be build up.
     */
    private static final double DBSCAN_EPSILON         = 10d;

    /**
     * One out of two parameters of the {@link DBSCANClusterer}.
     * <p>
     * MinPts denotes the minimal number of points, which is necessary to build up a cluster.
     */
    private static final int    DBSCAN_MIN_PTS         = 4;

    /**
     * Parameter for the form of the concave hull.
     * <p>
     * For further informations look into Duckham (2008).
     *
     * @see <a href=
     *      "http://geosensor.net/papers/duckham08.PR.pdf">http://geosensor.net/papers/duckham08.PR.pdf</a>
     */
    private static final double CONCAVE_HULL_THRESHOLD = 20;

    /**
     * Creates a new {@link Crowd} object.
     * <p>
     * Users are requested to create {@link Crowd} objects using
     * {@link CrowdFactory#createCrowdFromCoordinates(List, boolean)} method.
     * <p>
     *
     * @param forceModel the {@link ForceModel} objects, which represents the pedestrian modeling
     *            approach
     * @param numericIntegrator object, which represents on of the following classes: Simple Euler
     *            {@link SimpleEulerIntegrator}, Semi Implicit Euler
     *            {@link SemiImplicitEulerIntegrator} or Runge Kutta {@link RungeKuttaIntegrator}
     *
     * @see CrowdFactory#createCrowdFromCoordinates(List, boolean)
     */
    Crowd(ForceModel forceModel, NumericIntegrator numericIntegrator)
    {
        this(0, forceModel, numericIntegrator, null);
    }

    /**
     * Creates a new {@link Crowd} object.
     * <p>
     * Users are requested to create {@link Crowd} objects using
     * {@link CrowdFactory#createCrowdFromCoordinates(List, boolean)} method.
     * <p>
     *
     * @param id the id of this {@link Crowd}
     * @param forceModel the {@link ForceModel} objects, which represents the pedestrian modeling
     *            approach
     * @param numericIntegrator object, which represents on of the following classes: Simple Euler
     *            {@link SimpleEulerIntegrator}, Semi Implicit Euler
     *            {@link SemiImplicitEulerIntegrator} or Runge Kutta {@link RungeKuttaIntegrator}
     * @param threadPool the Thread Pool for parallelization of Pedestrian movement computation
     *
     * @see CrowdFactory#createCrowdFromCoordinates(List, boolean)
     */
    private Crowd(int id, ForceModel forceModel, NumericIntegrator numericIntegrator,
        ThreadPoolExecutor threadPool)
    {
        this.id = id;
        this.numericIntegrator = numericIntegrator;
        this.forceModel = forceModel;
        this.threadPool = threadPool;

        groups = new ArrayList<>();
        crowdOutlines = new ArrayList<>();
    }

    /**
     * Creates a new {@link Crowd} using values .
     * <p>
     * The new {@link Crowd} objects gets the following values from the given existing {@link Crowd}
     * object:
     * <ul>
     * <li>{@link #id}
     * <li>{@link #numericIntegrator}
     * <li>{@link #forceModel}
     * <li>{@link #threadPool}
     * <li>{@link #groups} (containing all {@link Pedestrian} of the existing crowd)
     * <li>{@link #crowdOutlines}
     * <li>{@link #quadtree}
     * <li>{@link #route}
     * </ul>
     *
     * @param crowd
     */
    public Crowd(Crowd crowd)
    {
        this(crowd.id, crowd.forceModel, crowd.numericIntegrator, crowd.threadPool);

        this.groups = crowd.groups;
        this.crowdOutlines = crowd.crowdOutlines;

        this.quadtree = crowd.quadtree;
        this.route = crowd.route;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId()
    {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setId(int id)
    {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pedestrian> getPedestrians()
    {
        // return java.util.Collections.unmodifiableList(pedestrians);
        return getPedestrians(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pedestrian> getPedestrians(boolean clone)
    {
        List<Pedestrian> pedestrians = new ArrayList<>();
        for (Group group : groups)
        {
            pedestrians.addAll(group.getPedestrians(clone));
        }
        return pedestrians;
    }

    /**
     * Gets a {@link List} of all {@link Pedestrian} objects belonging to this {@link Crowd}
     * transformed to {@link Coordinate} objects indicating their current position. For this purpose
     * all {@link Pedestrian} objects are cloned before the transformation into a {@link Coordinate}
     * object.
     *
     * @return a {@link List} of {@link Coordinate} objects indicating all {@link Pedestrian}
     *         position in this {@link Crowd}.
     *
     */
    public List<Coordinate> getCoordinates()
    {
        List<Pedestrian> pedestrians = getPedestrians(true);
        List<Coordinate> coordinates = new ArrayList<>();
        for (Pedestrian pedestrian : pedestrians)
        {
            coordinates.add(pedestrian.getCurrentPositionCoordinate());
        }
        return coordinates;
    }

    /**
     * Converts a {@link HashMap} with {@link Integer} key and {@link Coordinate} values into a of
     * {@link Pedestrian} objects belonging to this {@link Crowd} and set the initial velocity
     * parameters of all {@link Pedestrian}.
     * <p>
     * <b>{@link Pedestrian} objects are initialized without velocities.
     * {@link #setNormalDesiredVelocity(float, float)} and
     * {@link #setMaximumDesiredVelocity(float, float)} need to be called to set velocities</b>
     *
     *
     * @param pedestrianPositions a list of the start positions of {@link Pedestrian} objects. Each
     *            list element will translate into a new {@link Pedestrian} object
     */
    public void setPedestrians(List<Coordinate> pedestrianPositions)
    {
        // removes all groups if they are already existing
        if (groups != null && !groups.isEmpty())
            groups.clear();

        // sets Pedestrians in HashMap Style
        for (Coordinate pedestrianPosition : pedestrianPositions)
        {
            // using MathTools for the first time after starting the programme may take some time,
            // since some pre-computations are done
            float normalDesiredVelocity = 0;
            float maximumDesiredVelocity = 0;

            Pedestrian pedestrian = new Pedestrian(pedestrianPosition.x, pedestrianPosition.y,
                normalDesiredVelocity, maximumDesiredVelocity, forceModel, numericIntegrator,
                getQuadtree());

            Group group = new Group();
            group.add(pedestrian);
            groups.add(group);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Geometry> getCrowdOutlines()
    {
        if (crowdOutlines == null)
            return null;
        ArrayList<Geometry> currentCrowdOutlines = new ArrayList<>();
        currentCrowdOutlines.addAll(crowdOutlines);
        return currentCrowdOutlines;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCrowdOutlines(List<Geometry> crowdOutlines)
    {
        this.crowdOutlines = crowdOutlines;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNormalDesiredVelocity(float meanNormalDesiredVelocity,
        float standardDeviationOfNormalDesiredVelocity)
    {
        for (Pedestrian pedestrian : getPedestrians())
        {
            float normalDesiredVelocity = MathTools.getRandomGaussianValue(
                meanNormalDesiredVelocity, standardDeviationOfNormalDesiredVelocity);
            pedestrian.setCurrentNormalDesiredVelocity(normalDesiredVelocity);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMaximumDesiredVelocity(float meanMaximumDesiredVelocity,
        float standardDeviationOfMaximumDesiredVelocity)
    {
        for (Pedestrian pedestrian : getPedestrians())
        {
            float maximumDesiredVelocity = MathTools.getRandomGaussianValue(
                meanMaximumDesiredVelocity, standardDeviationOfMaximumDesiredVelocity);
            pedestrian.setMaximumDesiredVelocity(maximumDesiredVelocity);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Route getRoute()
    {
        return this.route;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateCrowdOutline(boolean isClusteringCrowdOutlines, boolean isCrowdOutlineConvex)
    {
        if (groups == null)
            return;

        List<Pedestrian> pedestrians = getPedestrians();

        if (isClusteringCrowdOutlines)
        {
            DBSCANClusterer<Clusterable> dbscan = new DBSCANClusterer<>(DBSCAN_EPSILON,
                DBSCAN_MIN_PTS);
            List<Cluster<Clusterable>> clusters = dbscan
                .cluster(new ArrayList<Clusterable>(pedestrians));
            ArrayList<Geometry> tempCrowdOutlines = new ArrayList<>();
            for (Cluster<Clusterable> cluster : clusters)
            {
                List<Clusterable> points = cluster.getPoints();
                Coordinate[] coordinates = new Coordinate[points.size()];
                for (int i = 0; i < points.size(); i++ )
                {
                    coordinates[i] = new Coordinate(points.get(i).getPoint()[0],
                        points.get(i).getPoint()[1]);
                }
                MultiPoint multiPointCluster = JTSFactoryFinder.getGeometryFactory()
                    .createMultiPoint(coordinates);

                Geometry crowdOutline = createOutline(multiPointCluster, isCrowdOutlineConvex,
                    CONCAVE_HULL_THRESHOLD);
                tempCrowdOutlines.add(crowdOutline);
            }
            crowdOutlines = tempCrowdOutlines;
        }
        else
        {
            // gets position of all pedestrians as MultiPoint object
            MultiPoint multiPoint = JTSFactoryFinder.getGeometryFactory()
                .createMultiPoint(Crowd.getCoordinatesFromPedestrians(pedestrians));
            Geometry crowdOutline = createOutline(multiPoint, isCrowdOutlineConvex,
                CONCAVE_HULL_THRESHOLD);
            ArrayList<Geometry> tempCrowdOutlines = new ArrayList<>();
            tempCrowdOutlines.add(crowdOutline);
            crowdOutlines = tempCrowdOutlines;
        }
    }

    /**
     * Computes the number of {@link Pedestrian} per m².
     *
     * @return the {@link Double} value of persons / m²
     */
    public double getCrowdDensity()
    {
        double density = Double.NaN;
        List<Geometry> tempCrowdOutlines = getCrowdOutlines();

        if (tempCrowdOutlines == null)
            return density;

        double totalCrowdArea = 0;
        for (Geometry crowdOutline : tempCrowdOutlines)
        {
            if (crowdOutline != null)
                totalCrowdArea += crowdOutline.getArea();
        }
        if (totalCrowdArea == 0)
            density = Double.NaN;
        else
            density = MathTools.round(getSize() / totalCrowdArea, 2);
        return density;
    }

    /**
     * Gets the size of this {@link Crowd}, i.e. the number of {@link Pedestrian} objects that
     * belong to this crowd
     *
     * @return the number of {@link Pedestrian} objects belonging to this {@link Crowd}
     */
    public int getSize()
    {
        return getPedestrians().size();
    }

    /**
     * Gets the current bounding box of this {@link Crowd} containing all {@link Pedestrian} objects
     * at their current positions.
     *
     * @return the current bounding box of this {@link Crowd}
     */
    public Envelope getBoundingBox()
    {
        List<Coordinate> pedestrianCoordinates = new ArrayList<>(
            Arrays.asList(Crowd.getCoordinatesFromPedestrians(getPedestrians())));
        return GeometryTools.getEnvelopeFromCoordinates(pedestrianCoordinates);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRoute(Route route, long time, boolean ignoreInvalid)
        throws CrowdSimulatorNotValidException
    {
        if (route == null || route.getWayPoints() == null || route.getWayPoints().isEmpty())
            return;

        List<WayPoint> wayPoints = route.getWayPoints();
        try
        {
            if (getQuadtree() == null)
            {
                ignoreInvalid = true;
                throw new CrowdSimulatorNotValidException(
                    "Quadtree for Crowd is null. Route cannot be checked for validity. This may result in an invalid Route set to the Crowd, which may lead to Pedestrians that are not able to follow the Route from start to end. For a valid Simulation of the Crowd for each Route that is set to a Crowd, it needs to be checked, if all Pedestrians can see the first WayPoint of the Route and if there is a direct line of sight between all pairs of consecutive WayPoints of the route.");
            }
            ValidationTools.checkInterWayPointVisibility(wayPoints,
                getQuadtree().getBoundaries(route.getBoundingBox()));
            Envelope boundingBoxPedestriansAndRoute = route.getBoundingBox();
            boundingBoxPedestriansAndRoute.expandToInclude(getBoundingBox());
            ValidationTools.checkPedestrianWayPointVisibility(this, wayPoints,
                getQuadtree().getBoundaries(boundingBoxPedestriansAndRoute));
        }
        catch (CrowdSimulatorNotValidException e)
        {
            if (ignoreInvalid)
            {
                logger.warn(e.getMessage());
            }
            else
                throw e;
        }

        this.route = route;
        if (groups != null && !groups.isEmpty())
        {
            for (Group group : groups)
            {
                group.setRoute(route, time);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(long time, double simulationUpdateInterval)
    {
        if (getQuadtree() == null)
            logger.warn(
                "No Quadtree given! This results in no pedestrian-pedestrian-interaction and no pedestrian-boundary-interaction during movement of Pedestrians");

        AtomicInteger processedGroups = new AtomicInteger(0);

        // https://stackoverflow.com/questions/27319446/does-multithreading-always-yield-better-performance-than-single-threading?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
        // So my lessons learned from highly parrallel multithreading have been:
        //
        // If possible use single threaded, shared-nothing processes to be more efficient
        // If threads are required, decouple the shared data access as much as possible
        // Don't try to allocate more loaded worker threads than available cores if possible
        for (Group group : groups)
        {
            if (threadPool != null)
            {
                // ensure that all threads for moving a single pedestrian are finished before this
                // method is left to remain consistency
                try
                {
                    threadPool.execute(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                group.move(time, simulationUpdateInterval);
                                processedGroups.getAndIncrement();
                            }
                        });
                }
                catch (RejectedExecutionException e)
                {
                    logger.trace("", e);
                }
            }
            else
            {
                group.move(time, simulationUpdateInterval);
                processedGroups.getAndIncrement();
            }
        }

        long startTimeOfParallelProcessing = System.currentTimeMillis();
        boolean isParallelProcessingFinished = false;
        while (processedGroups.get() < groups.size())
        {
            try
            {
                // wait 0.1 milliseconds for pedestrian threads to finish
                Thread.sleep(0, 100000);
            }
            catch (InterruptedException e)
            {
                logger.trace("sleep interrupted", e);
            }
            long threadProcessingTime = System.currentTimeMillis() - startTimeOfParallelProcessing;
            // inform user, if threads are not finishing within reasonable time (> 2 seconds)
            if (threadProcessingTime > 2000 && threadProcessingTime % 1000 == 0)
            {
                logger.info(
                    "threads take too long too finish, totalTime=" + threadProcessingTime / 1000
                        + "s, crowd id=" + id + " moved groups=" + processedGroups.get()
                        + ", total groups=" + groups.size() + ", getQuadtree()=" + getQuadtree());
                isParallelProcessingFinished = true;
            }
        }
        if (isParallelProcessingFinished)
            logger.info("threads are finished now. moved crowd (crowd id=" + id
                + "). total processing time="
                + (System.currentTimeMillis() - startTimeOfParallelProcessing) / 1000
                + "s, simulationUpdateInterval=" + simulationUpdateInterval + ", moved groups="
                + processedGroups.get());
    }

    /**
     * Sets {@link #crowdOutlines} to {@code null} and clears {@link #groups}.
     *
     */
    public void clear()
    {
        crowdOutlines = null;
        groups.clear();
    }

    /**
     * Gets the Centroid of this {@link Crowd}. For this purpose all {@link Pedestrian} positions of
     * this Crowd are converted to {@link Coordinate} objects and added to a {@link MultiPoint}
     * object.
     *
     * @see com.vividsolutions.jts.geom.Geometry#getCentroid()
     *
     * @return the centroid of this {@link Crowd} as {@link MultiPoint}
     */
    public Geometry getCentroid()
    {
        List<Pedestrian> pedestrians = getPedestrians(true);
        Coordinate[] coordinates = getCoordinates().toArray(new Coordinate[pedestrians.size()]);
        MultiPoint mp = JTSFactoryFinder.getGeometryFactory().createMultiPoint(coordinates);
        return mp.getCentroid();
    }

    /**
     * Creates the outline as {@link Geometry} of a given {@link MultiPoint} {@code multiPoint}
     * object, the boolean {@code isConvex} flag and the {@code concaveCrowdOutlineThreshold} (only
     * required for {@code isConvex} equals {@code false}
     *
     * @param multiPoint {@link MultiPoint} to create an outline for
     * @param isConvex flag to decide if the outline should be convex or concave
     * @param concaveCrowdOutlineThreshold a threshold to be used in the {@link ConcaveHull}
     *            algorithm (cf. Duckham et al. 2008). "For n points, concaveCrowdOutlineThreshold
     *            denotes the largest threshold distance such that all n points can still fit inside
     *            the shape"
     *
     * @return an outline of the {@code multiPoint} as a {@link Geometry}
     */
    private Geometry createOutline(MultiPoint multiPoint, boolean isConvex,
        double concaveCrowdOutlineThreshold)
    {
        Geometry outline = null;
        if (isConvex)
        {
            outline = multiPoint.convexHull();
        }
        else
        {
            try
            {
                ConcaveHull concaveHull = new ConcaveHull(multiPoint, concaveCrowdOutlineThreshold);

                // create a separate thread for computing the concave hull interrupt, if concave
                // hull computation takes too long
                ConcaveHullSolver chs = new ConcaveHullSolver(concaveHull);
                Thread solveThread = new Thread(chs);
                long startTime = System.currentTimeMillis();
                solveThread.start();

                // wait for result either max. 500 ms or until chs returns a valid hull geometry
                while (System.currentTimeMillis() - startTime < 500
                    && chs.getConcaveHullGeometry() == null)
                    try
                    {
                        // check every 0.1ms
                        Thread.sleep(0, 100000);
                        // Thread.sleep(1);
                    }
                    catch (InterruptedException e)
                    {
                        logger.trace("GeometryTools.createOutline(), ", e);
                    }
                // interrupt solve thread, if computation takes too long
                solveThread.interrupt();
                long endTime = System.currentTimeMillis();
                logger.trace("time for concave hull=" + (endTime - startTime) + "ms.");

                // get computed concave outline
                outline = chs.getConcaveHullGeometry();

                // compute convex hull if no concave hull could be computed
                if (outline == null)
                    outline = multiPoint.convexHull();
            }
            catch (IndexOutOfBoundsException | TopologyException e)
            {
                // compute convex hull in case of exceptions during concave hull computation
                outline = multiPoint.convexHull();
            }
        }
        if (outline != null && getQuadtree() != null)
        {
            List<Boundary> boundaries = getQuadtree().getBoundaries(outline.getEnvelopeInternal());
            if (outline instanceof Polygon && outline.isValid() && boundaries != null
                && !boundaries.isEmpty())
            {
                for (Boundary boundary : boundaries)
                {
                    Geometry geometry = boundary.getGeometry();
                    if (geometry.isValid())
                        outline = outline.difference(boundary.getGeometry());
                }
            }
        }

        if (outline != null && outline instanceof LineString)
        {
            LineString ls = (LineString) outline;
            if (ls.getNumPoints() > 2)
                outline = GeometryTools.closeLineString(ls);
        }

        if (outline == null)
        {
            logger.info("outline null:" + outline);
        }
        else if ( !outline.isValid())
        {
            logger.info("outline invalid:" + outline);
        }
        return outline;
    }

    /**
     * Gets the x and y position of every {@link Pedestrian} out of a {@link List} of
     * {@link Pedestrian}s and collects them in a {@link Coordinate}[] object.
     *
     * @return a {@link Coordinate}[] object with all positions of all {@link Pedestrian}
     */
    static Coordinate[] getCoordinatesFromPedestrians(List<Pedestrian> pedestrians)
    {
        Coordinate[] coordinates = null;
        synchronized (pedestrians)
        {
            coordinates = new Coordinate[pedestrians.size()];
            for (int i = 0; i < coordinates.length; i++ )
            {
                coordinates[i] = pedestrians.get(i).getCurrentPositionCoordinate();
            }
        }

        return coordinates;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Quadtree getQuadtree()
    {
        return quadtree;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setQuadtree(Quadtree quadtree)
    {
        this.quadtree = quadtree;
        if (groups == null || groups.isEmpty())
            return;
        for (Group group : groups)
        {
            List<Pedestrian> pedestrians = group.getPedestrians(false);
            if (pedestrians == null || pedestrians.isEmpty())
                continue;
            for (Pedestrian pedestrian : pedestrians)
            {
                if (pedestrian == null)
                    continue;
                pedestrian.setQuadtree(quadtree);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setThreadPool(ThreadPoolExecutor threadPool)
    {
        this.threadPool = threadPool;
    }
}

/**
 * A class for solving {@link ConcaveHull} computations and safely interrupt them in case of them
 * taking too long
 *
 * @author hahmann
 */
class ConcaveHullSolver implements Runnable
{
    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger logger              = LoggerFactory
        .getLogger(ConcaveHullSolver.class);

    /**
     * The {@link ConcaveHull} object
     */
    private final ConcaveHull   concaveHull;

    /**
     * The {@link Geometry} of the {@link ConcaveHull}
     */
    private Geometry            concaveHullGeometry = null;

    /**
     * Creates a new {@link ConcaveHullSolver} with the given {@code concaveHull} object
     *
     * @param concaveHull the {@link ConcaveHull} object to be solved
     */
    public ConcaveHullSolver(ConcaveHull concaveHull)
    {
        this.concaveHull = concaveHull;
    }

    @Override
    public void run()
    {
        while ( !Thread.currentThread().isInterrupted() && concaveHullGeometry == null)
        {
            try
            {
                concaveHullGeometry = concaveHull.getConcaveHull();
            }
            catch (Exception e)
            {
                logger.trace("concave hull generation failed", e);
                concaveHullGeometry = null;
            }

        }
    }

    /**
     * Gets the {@link Geometry} object of the {@link ConcaveHull} object or {@code null}, if the
     * computation has not finished yet
     *
     * @return the {@link Geometry} of the {@link ConcaveHull}
     */
    public Geometry getConcaveHullGeometry()
    {
        return concaveHullGeometry;
    }
}
