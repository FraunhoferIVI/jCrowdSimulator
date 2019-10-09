package de.fhg.ivi.crowdsimulation;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opensphere.geometry.algorithm.ConcaveHull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.ivi.crowdsimulation.analysis.Grid;
import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.boundaries.GeometryNotValidException;
import de.fhg.ivi.crowdsimulation.crowd.Crowd;
import de.fhg.ivi.crowdsimulation.crowd.CrowdFactory;
import de.fhg.ivi.crowdsimulation.crowd.ICrowd;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.ForceModel;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingBuznaModel;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingModel;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.NumericIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.RungeKuttaIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.SemiImplicitEulerIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.SimpleEulerIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.Route;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.RouteFactory;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.WayPoint;
import de.fhg.ivi.crowdsimulation.geom.Quadtree;
import de.fhg.ivi.crowdsimulation.time.FastForwardClock;
import de.fhg.ivi.crowdsimulation.validate.ValidationTools;

/**
 * This class serves as container for the crowd simulation, i.e. that all objects that are part of
 * the crowd simulation are managed in this class. This includes e.g. the {@link Boundary},
 * {@link Crowd} and {@link WayPoint} objects.
 * <p>
 * Furthermore the default calibration of the applied implementation of the Force Model (see
 * {@link ForceModel} and the algorithm of the numerical integration method, which is necessary to
 * process the Force Model, (cf. {@link NumericIntegrator} is set in this class.
 * <p>
 * Also this class has methods for calculating the outline around a {@link Crowd}, an
 * {@link Envelope} around all objects and the {@link CoordinateReferenceSystem} that all geometric
 * objects that are of the crowd simulation should have.
 * <p>
 * This class implements {@link Runnable} and thus contains the actual simulation loop.
 * <p>
 * Furthermore, it is possible to validate the {@link CrowdSimulator}.
 *
 * @see CrowdSimulatorNotValidException
 *
 * @author hahmann/meinert
 *
 *         TODO: build exe with https://gerardnico.com/maven/exe
 */
public class CrowdSimulator<T extends ICrowd> implements Runnable
{

    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger       logger                                                 = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Time between 2 consecutive Iteration/Update steps. Given in seconds.
     */
    private double                    simulationUpdateInterval                               = 0.0f;

    /**
     * Average time between two consecutive Iteration/Update steps. Given in seconds.
     */
    private double                    averageSimulationUpdateInterval                        = 0.0f;

    /**
     * Total number of simulation steps calculated in this {@link CrowdSimulator}
     */
    private int                       totalSimulationSteps                                   = 0;

    /**
     * Keeps the time (in simulation time), when the simulation was started. Given in milliseconds.
     */
    private long                      startSimulationTime;

    /**
     * Keeps the last time the simulation was executed. The parameter is given in milliseconds.
     */
    private long                      lastSimulationTime                                     = 0;

    /**
     * The minimum interval between two consecutive refreshes in milliseconds
     */
    private int                       minimumRefreshInterval                                 = 1;

    /**
     * The time difference between the {@code startSimulationTime} and the
     * {@code lastSimulationTime}.
     */
    private long                      simulatedTime;

    /**
     * Boolean parameter which denotes whether the thread for the simulation is running or not.
     */
    private final AtomicBoolean       isSimulationRunning;

    /**
     * Thread that runs the individual simulation steps.
     */
    private Thread                    simulationControlThread;

    /**
     * A "Clock" which can speed up (or speed down) the simulation.
     */
    protected FastForwardClock        fastForwardClock;

    /**
     * {@link Long} number which denotes how much the simulation is speed up or down
     */
    private int                       fastForwardFactor                                      = 1;

    /**
     * An {@link Integer} storage for the set {@code fastForwardFactor}
     */
    private int                       savedFastForwardFactor                                 = 0;

    /**
     * {@code true}, if simulation computations are currently in progress, {@code false} otherwise
     */
    private boolean                   isSimulatingInProgress;

    /**
     * Can be one of the following methods of numerical mathematics to compute {@link Pedestrian}
     * movement - Simple Euler {@link SimpleEulerIntegrator}, Semi Implicit Euler
     * {@link SemiImplicitEulerIntegrator} or Runge Kutta {@link RungeKuttaIntegrator}.
     */
    protected NumericIntegrator       numericIntegrator;

    /**
     * {@link ForceModel} objects, which represents the force model for pedestrian movement
     */
    protected ForceModel              forceModel;

    /**
     * The Bounding Box of {@link #getBoundaries()}, the initial positions of all
     * {@link Pedestrian}s contained in the {@link #getCrowds()} and their respective {@link Route}s
     */
    private Envelope                  boundingBox;

    /**
     * {@link List} of {@link Crowd}s, which represents all crowds participating in this simulation
     * and also encapsulate all {@link Pedestrian} objects.
     */
    private List<T>                   crowds;

    /**
     * {@link Integer} to create a unique id for each {@link Crowd} crowd, which is created.
     */
    private int                       crowdId                                                = 0;

    /**
     * {@link Integer} to create a unique id for each {@link Pedestrian}, which is created.
     */
    private int                       pedestrianId                                           = 0;

    /**
     * {@link Grid} object
     */
    private Grid                      grid;

    /**
     * The {@link Quadtree} object allows (efficient) access to {@link Boundary} objects and
     * {@link Pedestrian} objects belonging to the same {@link CrowdSimulator} and makes
     * Pedestrians-Boundary and Pedestrian-Pedestrian interaction possible.
     */
    protected Quadtree                quadtree;

    /**
     * Object for saving a coordinate reference system of {@link Geometry}s.
     */
    private CoordinateReferenceSystem crs;

    /**
     * Thread Pool for parallelization of Pedestrian movement computation
     */
    private ThreadPoolExecutor        threadPool;

    /**
     * Object used to generate {@link WayPoint} objects.
     */
    private RouteFactory              routeFactory;

    /**
     * Object used to generate {@link Crowd} objects.
     */
    private CrowdFactory              crowdFactory;

    /**
     * Indicates, if the crowd outlines should be updated each time the crowds have moved. Usually,
     * it is unnecessary to do so, if the crowd outline is not visible in the Graphical User
     * Interface, since it is currently unused elsewhere.
     */
    private boolean                   isUpdatingCrowdOutlines;

    /**
     * Indicates, if the {@link Pedestrian} objects are clustered into groups using
     * {@link DBSCANClusterer} before the crowd outlines are computed
     */
    private boolean                   isClusteringCrowdOutlines;

    /**
     * Indicates, if calculation of the crowd outline use a {@link ConvexHull} or a
     * {@link ConcaveHull} algorithm
     */
    private boolean                   isCrowdOutlinesConvex;

    /**
     * Default value, if the {@link Pedestrian} objects are clustered into groups using
     * {@link DBSCANClusterer} before the crowd outlines are computed
     */
    public static final boolean       DEFAULT_IS_CLUSTERING_CROWD_OUTLINES                   = false;

    /**
     * Default value, if the calculation of the crowd outlines uses a {@link ConvexHull} or a
     * {@link ConcaveHull} algorithm
     */
    public static final boolean       DEFAULT_IS_CROWD_OUTLINE_CONVEX                        = false;

    /**
     * Default Average normal velocity, i.e. the average normal walking velocity of a
     * {@link Pedestrian}. This is the velocity that a pedestrian would choose for walking, when not
     * being delayed. The velocity is given in m/s. Cf. Helbing et al (2005) p. 11.
     */
    public static final float         DEFAULT_MEAN_NORMAL_DESIRED_VELOCITY                   = 1.2f;

    /**
     * Default Standard deviation of {@link #DEFAULT_MEAN_NORMAL_DESIRED_VELOCITY}, i.e. the
     * standard deviation of the average normal walking velocity, which is used to pick a normal
     * walking velocity for each {@link Pedestrian} from the resulting Gaussian distribution.
     * Outliers that would not be within the 95% of all values are avoided. The value is given in
     * m/s. Cf. Helbing et al (2005) p. 11.
     */
    public static final float         DEFAULT_STANDARD_DEVIATION_OF_NORMAL_DESIRED_VELOCITY  = 0.3f;

    /**
     * Default average maximum velocity, i.e. the average maximum walking velocity of all
     * pedestrians. This is used to derive the maximum velocity that an individual
     * {@link Pedestrian} is mable to walk, e.g. when being delayed. The velocity is given in m/s.
     * The value is defined in Helbing, Molnar (1995) p. 4284. It is 30% above
     * {@link #DEFAULT_MEAN_NORMAL_DESIRED_VELOCITY}. Unfortunately there is no value for that in
     * Helbing et al (2005).
     */
    public static final float         DEFAULT_MEAN_MAXIMUM_DESIRED_VELOCITY                  = 1.3f
        * DEFAULT_MEAN_NORMAL_DESIRED_VELOCITY;

    /**
     * Default standard deviation of {@link #DEFAULT_MEAN_MAXIMUM_DESIRED_VELOCITY}, i.e. the
     * standard deviation of the maximum walking velocity, which is used to compensate for delays by
     * each {@link Pedestrian} from the resulting Gaussian distribution. Outliers that would not be
     * within the 95% of all values are avoided. The value is given in m/s.
     * <p>
     * This value is chosen arbitrarily. In Helbing, Molnar (1995) as well as in Helbing et al
     * (2005) there is no value defined.
     */
    public static final float         DEFAULT_STANDARD_DEVIATION_OF_MAXIMUM_DESIRED_VELOCITY = 0.3f;

    /**
     * Average normal velocity, i.e. the average normal walking velocity of all {@link Pedestrian}s.
     * From this the velocity that a pedestrian would choose for walking (when not being delayed) is
     * derived. The velocity is given in m/s.
     */
    private float                     meanNormalDesiredVelocity;

    /**
     * Standard deviation of {@link #meanNormalDesiredVelocity}, i.e. the standard deviation of the
     * average normal walking velocity, which is used to pick a normal walking velocity for each
     * {@link Pedestrian} of this Crowd from the resulting Gaussian distribution. Outliers that
     * would not be within the 95% of all values are avoided.
     */
    private float                     standardDeviationOfNormalDesiredVelocity;

    /**
     * Average maximum velocity, i.e. the average maximum walking velocity of all pedestrians. From
     * this the maximum velocity that an individual {@link Pedestrian} is able to walk (e.g. when
     * being delayed) is derived. The velocity is given in m/s.
     */
    private float                     meanMaximumDesiredVelocity;

    /**
     * Standard deviation of {@link #meanMaximumDesiredVelocity}, i.e. the standard deviation of the
     * maximum walking velocity, which is used to compensate for delays by each {@link Pedestrian}
     * from the resulting Gaussian distribution. Outliers that would not be within the 95% of all
     * values are avoided. The value is given in m/s.
     */
    private float                     standardDeviationOfMaximumDesiredVelocity;

    /**
     * Constructor.
     * <p>
     * Initializes the {@link FastForwardClock}, {@link ForceModel}, {@link NumericIntegrator} and
     * {@link Quadtree} and the {@link #threadPool} that allows parallel processing of the movement
     * calculation of the {@link Pedestrian} objects.
     */
    public CrowdSimulator()
    {
        fastForwardClock = new FastForwardClock();

        forceModel = new HelbingBuznaModel();
        // forceModel = new HelbingJohanssonModel();
        // forceModel = new JohanssonHelbingTestModel();

        numericIntegrator = new SemiImplicitEulerIntegrator();
        quadtree = new Quadtree();
        routeFactory = new RouteFactory(quadtree);
        crowdFactory = new CrowdFactory(numericIntegrator, forceModel, quadtree, threadPool);

        isClusteringCrowdOutlines = DEFAULT_IS_CLUSTERING_CROWD_OUTLINES;
        isCrowdOutlinesConvex = DEFAULT_IS_CROWD_OUTLINE_CONVEX;

        meanNormalDesiredVelocity = DEFAULT_MEAN_NORMAL_DESIRED_VELOCITY;
        standardDeviationOfNormalDesiredVelocity = DEFAULT_STANDARD_DEVIATION_OF_NORMAL_DESIRED_VELOCITY;
        meanMaximumDesiredVelocity = DEFAULT_MEAN_MAXIMUM_DESIRED_VELOCITY;
        standardDeviationOfMaximumDesiredVelocity = DEFAULT_STANDARD_DEVIATION_OF_MAXIMUM_DESIRED_VELOCITY;

        isSimulationRunning = new AtomicBoolean(false);

        logger.info("crowdsimlib version=" + getVersion());
    }

    /**
     * Gets the time between 2 consecutive simulation steps.
     *
     * @return the simulation interval given in seconds.
     */
    public double getSimulationUpdateInterval()
    {
        return simulationUpdateInterval;
    }

    /**
     * Gets the average time between 2 consecutive simulation steps.
     *
     * @return the average simulation interval given in seconds.
     */
    public double getAverageSimulationUpdateInterval()
    {
        return averageSimulationUpdateInterval;
    }

    /**
     * Calculates the time the simulation is running, which results of the difference between
     * {@link #lastSimulationTime} and {@link #startSimulationTime}.
     *
     * @return the time the simulation is running in milliseconds
     */
    public long getSimulatedTimeSpan()
    {
        this.simulatedTime = this.lastSimulationTime - this.startSimulationTime;
        return simulatedTime;
    }

    /**
     * Gets the {@link #fastForwardFactor}.
     *
     * @return denotes the number with which the simulation is speed up or down
     */
    public double getFastForwardFactor()
    {
        return fastForwardFactor;
    }

    /**
     * Sets the factor with which the simulation is speed up or down.
     *
     * @param fastForwardFactor the speed up/down factor as number
     */
    public void setFastForwardFactor(int fastForwardFactor)
    {
        this.fastForwardFactor = fastForwardFactor;
    }

    /**
     * Sets the {@link #fastForwardFactor} to the current value of {@link #savedFastForwardFactor}.
     *
     */
    public void restoreFastForwardFactor()
    {
        fastForwardFactor = savedFastForwardFactor;
    }

    /**
     * Saves the {@link #fastForwardFactor} into the variable {@link #savedFastForwardFactor}.
     */
    public void saveFastForwardFactor()
    {
        savedFastForwardFactor = fastForwardFactor;
    }

    /**
     * Starts the simulation
     */
    public synchronized void start()
    {
        if (simulationControlThread != null && simulationControlThread.isAlive())
            return;

        // If necessary - how to prioritize Threads + give highest priority to ui thread using this
        // (only necessary, if graphics thread seems to get slow due to parallelization):
        // https://funofprograming.wordpress.com/2016/10/08/priorityexecutorservice-for-java/
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        logger.info("crowdsimlib version = " + getVersion() + ", available processors="
            + availableProcessors);
        this.threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(availableProcessors);

        simulationControlThread = new Thread(this);
        isSimulationRunning.set(true);
        simulationControlThread.start();
    }

    /**
     * Stops the simulations
     */
    public void stop()
    {
        isSimulationRunning.set(false);
        if (threadPool != null)
            threadPool.shutdown();
    }

    /**
     * Test, if the simulation that is performing the individual simulation steps is currently
     * running.
     *
     * @return {@code true}, if the simulation that is performing the individual simulation steps is
     *         currently running, {@code false} otherwise.
     */
    public boolean isSimulationRunning()
    {
        return isSimulationRunning.get();
    }

    /**
     * Checks, if computations needed for an individual simulation step are currently in progress.
     *
     * @return {@code true}, if computations needed for an individual simulation step are currently
     *         in progress, {@code false} otherwise
     */
    public boolean isSimulatingInProgress()
    {
        return isSimulatingInProgress;
    }

    /**
     * Gets an object of the {@link FastForwardClock}.
     *
     * @return Object of a "Clock" which can speed up (or speed down) the simulation.
     */
    public FastForwardClock getFastForwardClock()
    {
        return fastForwardClock;
    }

    /**
     * Sets the time, as unix timestamp, for the last timepoint when the simulation was computed.
     * The time is given in milliseconds.
     *
     * @param lastSimulationTime the last timepoint of the simulation
     */
    public void setLastSimulationTime(long lastSimulationTime)
    {
        this.lastSimulationTime = lastSimulationTime;
    }

    /**
     * Pause the simulation after by setting the {@link #fastForwardFactor} to zero
     */
    public void pauseSimulation()
    {
        if (getFastForwardFactor() > 0)
        {
            saveFastForwardFactor();
            setFastForwardFactor(0);
        }
    }

    /**
     * Resumes the simulation after the simulation was paused by restoring the fastForwardFactor
     * from before the simulation was paused
     */
    public void resumeSimulation()
    {
        if (getFastForwardFactor() == 0)
        {
            restoreFastForwardFactor();
        }
    }

    /**
     * Sets the {@link #numericIntegrator} to {@code NumericIntegrator}
     *
     * @param numericIntegrator the {@link NumericIntegrator} should be one of
     *            {@link SimpleEulerIntegrator}, {@link SemiImplicitEulerIntegrator},
     *            {@link RungeKuttaIntegrator}
     */
    public void setNumericIntegrator(NumericIntegrator numericIntegrator)
    {
        synchronized (this.numericIntegrator)
        {
            this.numericIntegrator = numericIntegrator;
        }
    }

    /**
     * Gets the {@link NumericIntegrator} currently used by implementing classes.
     *
     * @return the {@link NumericIntegrator}, one of {@link SimpleEulerIntegrator},
     *         {@link SemiImplicitEulerIntegrator}, {@link RungeKuttaIntegrator}
     */
    public NumericIntegrator getNumericIntegrator()
    {
        return numericIntegrator;
    }

    /**
     * Gets the {@link ForceModel} object, which represents the current force model for pedestrian
     * movement
     *
     * @return the current {@link ForceModel}
     */
    public ForceModel getForceModel()
    {
        return forceModel;
    }

    /**
     * Gets {@link ThreadPoolExecutor} for parallelization of Pedestrian movement computation.
     *
     * @return the {@link ThreadPoolExecutor}
     */
    public ThreadPoolExecutor getThreadPool()
    {
        return threadPool;
    }

    /**
     * Gets the complete {@link List} of {@link Boundary} objects.
     *
     * @return the complete {@link List} of {@link Boundary} objects.
     */
    public List<Boundary> getBoundaries()
    {
        return getBoundaries(boundingBox);
    }

    /**
     * Sets, if the outlines of {@link Crowd} objects should be computed each time the crowd has
     * moved. Usually, it is unnecessary to do so, if the crowd outlines are not visible in a
     * Graphical User Interface. In this case skipping the computation of outlines significantly
     * improves performance.
     * <p>
     *
     * @param isUpdatingCrowdOutlines {@code true} if crowd outlines should be computed,
     *            {@code false} otherwise
     */
    public void setUpdatingCrowdOutlines(boolean isUpdatingCrowdOutlines)
    {
        this.isUpdatingCrowdOutlines = isUpdatingCrowdOutlines;
        if (isUpdatingCrowdOutlines)
            for (T crowd : crowds)
            {
                crowd.updateCrowdOutline(isClusteringCrowdOutlines, isCrowdOutlinesConvex);
            }
        else
        {
            for (T crowd : crowds)
            {
                crowd.setCrowdOutlines(null);
            }
        }
    }

    /**
     * Tests, if the outlines of {@link Crowd} objects are be computed each time the crowd has
     * moved. Usually, it is unnecessary to do so, if the crowd outlines are not visible in a
     * Graphical User Interface. In this case skipping the computation of outlines significantly
     * improves performance.
     * <p>
     *
     * @return {@code true} if crowd outlines are computed, {@code false} otherwise
     */
    public boolean isUpdatingCrowdOutlines()
    {
        return isUpdatingCrowdOutlines;
    }

    /**
     * Gets the {@link #meanNormalDesiredVelocity}.
     *
     * @return the {@link #meanNormalDesiredVelocity}.
     */
    public float getMeanNormalDesiredVelocity()
    {
        return meanNormalDesiredVelocity;
    }

    /**
     * Tests if the {@link Pedestrian} objects are clustered into groups using
     * {@link DBSCANClusterer} before the crowd outlines are computed.
     *
     * @return {@code true} for clustering = yes, {@code false} for clustering = no
     */
    public boolean isClusteringCrowdOutlines()
    {
        return isClusteringCrowdOutlines;
    }

    /**
     * Sets if the {@link Pedestrian} objects are clustered into groups using
     * {@link DBSCANClusterer} before the crowd outlines are computed. {@code true} for clustering =
     * yes, {@code false} for clustering = no
     *
     * @param isClusteringCrowdOutlines Sets if the {@link Pedestrian} objects are clustered into
     *            groups using {@link DBSCANClusterer} before the crowd outlines are computed.
     *            {@code true} for clustering = yes, {@code false} for clustering = no
     */
    public void setClusteringCrowdOutlines(boolean isClusteringCrowdOutlines)
    {
        this.isClusteringCrowdOutlines = isClusteringCrowdOutlines;
    }

    /**
     * Tests, if calculation of the crowd outline uses a {@link ConvexHull} or a {@link ConcaveHull}
     * algorithm.
     *
     * @return {@code true} if {@link ConvexHull} is used, if {@code false} {@link ConcaveHull} is
     *         used
     */
    public boolean isCrowdOutlineConvex()
    {
        return this.isCrowdOutlinesConvex;
    }

    /**
     * Set, if calculation of the crowd outline uses a {@link ConvexHull} or a {@link ConcaveHull}
     * algorithm.
     *
     * @param isCrowdOutlineConvex Indicates, if calculation of the crowd outline use a
     *            {@link ConvexHull} or a {@link ConcaveHull} algorithm. If {@code true} the
     *            {@link ConvexHull} is used, if {@code false} {@link ConcaveHull} is used
     */
    public void setCrowdOutlinesConvex(boolean isCrowdOutlineConvex)
    {
        this.isCrowdOutlinesConvex = isCrowdOutlineConvex;
    }

    /**
     * Sets normal desired velocities that all {@link Pedestrian} want to reach (without being
     * "delayed") given in m/s. Cf. Helbing et al (2005) p. 11 and subsequently updates the normal
     * desired velocities of all {@link Pedestrian}
     *
     * @param meanNormalDesiredVelocity the mean of the velocities that all {@link Pedestrian} want
     *            to reach (without being "delayed")
     */
    public void setMeanNormalDesiredVelocity(float meanNormalDesiredVelocity)
    {
        this.meanNormalDesiredVelocity = meanNormalDesiredVelocity;
        for (T crowd : getCrowds())
        {
            crowd.setNormalDesiredVelocity(meanNormalDesiredVelocity,
                standardDeviationOfNormalDesiredVelocity);
        }
    }

    /**
     * Gets the {@link #standardDeviationOfNormalDesiredVelocity} based on the
     * {@link #meanNormalDesiredVelocity}.
     *
     * @return the {@link #standardDeviationOfNormalDesiredVelocity}
     */
    public float getStandardDeviationOfNormalDesiredVelocity()
    {
        return standardDeviationOfNormalDesiredVelocity;
    }

    /**
     * Sets the standard deviation of the velocities that all {@link Pedestrian} want to reach
     * (without being "delayed") given in m/s. Cf. Helbing et al (2005) p. 11 and subsequently
     * updates the normal desired velocities of all {@link Pedestrian}
     *
     * @param standardDeviationOfNormalDesiredVelocity the standard deviation of the velocities that
     *            all {@link Pedestrian} want to reach (without being "delayed")
     */
    public void setStandardDeviationOfNormalDesiredVelocity(
        float standardDeviationOfNormalDesiredVelocity)
    {
        this.standardDeviationOfNormalDesiredVelocity = standardDeviationOfNormalDesiredVelocity;
        for (T crowd : getCrowds())
        {
            crowd.setNormalDesiredVelocity(meanNormalDesiredVelocity,
                standardDeviationOfNormalDesiredVelocity);
        }
    }

    /**
     * Gets the {@link #meanMaximumDesiredVelocity}.
     *
     * @return the {@link #meanMaximumDesiredVelocity}
     */
    public float getMeanMaximumDesiredVelocity()
    {
        return meanMaximumDesiredVelocity;
    }

    /**
     * Sets the mean of the velocities that all {@link Pedestrian} can reach (in case "delayed")
     * given in m/s. Cf. Helbing et al (2005) p. 11 and subsequently updates the maximum desired
     * velocities of all {@link Pedestrian}
     *
     * @param meanMaximumDesiredVelocity the mean of the velocities that all {@link Pedestrian} can
     *            reach (in case of being "delayed")
     */
    public void setMeanMaximumDesiredVelocity(float meanMaximumDesiredVelocity)
    {
        this.meanMaximumDesiredVelocity = meanMaximumDesiredVelocity;
        for (T crowd : getCrowds())
        {
            crowd.setMaximumDesiredVelocity(meanMaximumDesiredVelocity,
                standardDeviationOfMaximumDesiredVelocity);
        }
    }

    /**
     * Gets the {@link #standardDeviationOfMaximumDesiredVelocity} based on the
     * {@link #meanMaximumDesiredVelocity}.
     *
     * @return the {@link #standardDeviationOfMaximumDesiredVelocity}
     */
    public float getStandardDeviationOfMaximumVelocity()
    {
        return standardDeviationOfMaximumDesiredVelocity;
    }

    /**
     * Sets the standard deviation of the mean of the maximum velocities that all {@link Pedestrian}
     * can reach (in case of being "delayed") given in m/s. Cf. Helbing et al (2005) p. 11 and
     * subsequently updates the maximum velocities of all {@link Pedestrian}
     *
     * @param standardDeviationOfMaximumVelocity the standard deviation of the mean of the maximum
     *            velocities that all {@link Pedestrian} can reach (in case of being "delayed")
     */
    public void setStandardDeviationOfMaximumVelocity(float standardDeviationOfMaximumVelocity)
    {
        this.standardDeviationOfMaximumDesiredVelocity = standardDeviationOfMaximumVelocity;
        for (T crowd : getCrowds())
        {
            crowd.setMaximumDesiredVelocity(meanMaximumDesiredVelocity,
                standardDeviationOfMaximumVelocity);
        }
    }

    /**
     * Gets the complete {@link List} of {@link Boundary} objects.
     *
     * @param searchEnvelope the area to return {@link Boundary} objects
     * @return the complete {@link List} of {@link Boundary} objects.
     */
    public List<Boundary> getBoundaries(Envelope searchEnvelope)
    {
        List<Boundary> tempBoundaries = null;
        if (quadtree != null)
            tempBoundaries = quadtree.getBoundaries(searchEnvelope);
        return tempBoundaries;
    }

    /**
     * Updates {@link WayPoint} objects.
     * <p>
     * This is recommended in case boundaries are loaded after {@link Route}s have been loaded or
     * boundaries have been reloaded in general, it's necessary that {@link WayPoint} objects of
     * {@link Route} objects are updated, because the target lines and or passing areas of the
     * {@link WayPoint} could have changed as well.
     */
    private void updateRoutes() throws CrowdSimulatorNotValidException
    {
        List<T> currentCrowds = getCrowds();
        if (currentCrowds != null && !currentCrowds.isEmpty())
        {
            for (T crowd : currentCrowds)
            {
                Route route = crowd.getRoute();
                if (route != null && route.getWayPoints() != null
                    && !route.getWayPoints().isEmpty())
                {
                    route = routeFactory.createRouteFromCoordinates(route.getWayPoints());
                    crowd.setRoute(route, fastForwardClock.currentTimeMillis(), false);
                }
            }
        }
    }

    /**
     * Gets a copy of the current bounding box of this {@link CrowdSimulator}. The bounding box is
     * guaranteed to contain all {@link Boundary} objects and {@link WayPoint} objects and before
     * the Simulation is running also all {@link Pedestrian} objects. After the simulation has
     * started it is not guaranteed anymore that all {@link Pedestrian} objects are contained.
     *
     * @return a copy of the current bounding box of this {@link CrowdSimulator}.
     */
    public Envelope getBoundingBox()
    {
        return new Envelope(boundingBox);
    }

    /**
     * Expands a bounding box serving as bounding box for all {@link Boundary} objects and
     * {@link Crowd} objects contained in this {@link CrowdSimulator}.
     * <p>
     * If {@link #boundingBox} is {@code null}, it is initialized with the value of the given
     * {@code envelope}
     *
     * @param envelope an {@link Envelope} to expand the {@link #boundingBox}
     */
    public void expandBoundingBox(Envelope envelope)
    {
        if (envelope == null)
            return;

        if (boundingBox == null)
        {
            boundingBox = envelope;
        }
        else
        {
            boundingBox.expandToInclude(envelope);
        }
        if (grid != null)
        {
            ReferencedEnvelope gridBounds = new ReferencedEnvelope(boundingBox, crs);
            grid = new Grid(gridBounds);
        }
    }

    /**
     * Gets the {@link List} of {@link Crowd}s which includes all {@link Pedestrian}s and therefore
     * their movement. Also this encapsulates all objects / methods that are relevant to compute the
     * outlines (clustered/non-clustered, convex/concave) of the given List of {@link Pedestrian}
     * objects.
     *
     * @return the {@link List} of all {@link Crowd}s participating in this simulation.
     */
    public List<T> getCrowds()
    {
        return crowds;
    }

    /**
     * Gets the object used to generate {@link WayPoint} objects
     *
     * @return the {@link RouteFactory} of this {@link CrowdSimulator}
     */
    public RouteFactory getRouteFactory()
    {
        return routeFactory;
    }

    /**
     * Gets the object used to generate {@link Crowd} objects
     *
     * @return the {@link CrowdFactory} of this {@link CrowdSimulator}
     */
    public CrowdFactory getCrowdFactory()
    {
        return crowdFactory;
    }

    /**
     * Adds a {@link Crowd} object to the list of {@link #crowds}.
     * <p>
     * Creates a new {@link List} object for {@link #crowds} if {@link #crowds} is {@code null}
     *
     *
     * @param crowd the {@link Crowd} object to be added
     *
     * @throws CrowdSimulatorNotValidException in case 2 or more {@link Pedestrian} objects of the
     *             created {@link Crowd} object have identical positions or if 2 more
     *             {@link Pedestrian} objects in the whole {@link CrowdSimulator} have identical
     *             Ids.
     */
    public void addCrowd(T crowd) throws CrowdSimulatorNotValidException
    {
        addCrowd(crowd, true);
    }

    /**
     * Adds a {@link Crowd} object to the list of {@link #crowds}.
     * <p>
     * Creates a new {@link List} object for {@link #crowds} if {@link #crowds} is {@code null}
     *
     *
     * @param crowd the {@link Crowd} object to be added
     * @param ignoreInvalid if {@code true} in case validation checks yield validation errors, only
     *            a warning is printed into the log output, otherwise
     *            {@link CrowdSimulatorNotValidException} is thrown
     *
     * @throws CrowdSimulatorNotValidException in case 2 or more {@link Pedestrian} objects of the
     *             created {@link Crowd} object have identical positions or if 2 more
     *             {@link Pedestrian} objects in the whole {@link CrowdSimulator} have identical
     *             Ids.
     */
    public void addCrowd(T crowd, boolean ignoreInvalid) throws CrowdSimulatorNotValidException
    {
        if (crowds == null)
            crowds = new ArrayList<>();
        int tempCrowdId = crowdId;
        int tempPedestrianId = pedestrianId;
        // generate and set a unique id for the new crowd
        crowd.setId(tempCrowdId++ );
        // generate and set unique ids for all pedestrians
        for (Pedestrian pedestrian : crowd.getPedestrians())
        {
            pedestrian.setId(tempPedestrianId++ );
        }
        // set velocities of crowd
        crowd.setNormalDesiredVelocity(meanNormalDesiredVelocity,
            standardDeviationOfNormalDesiredVelocity);
        crowd.setMaximumDesiredVelocity(meanMaximumDesiredVelocity,
            standardDeviationOfMaximumDesiredVelocity);

        //
        crowd.updateCrowdOutline(isClusteringCrowdOutlines, isCrowdOutlinesConvex);

        ArrayList<T> crowdsToValidate = new ArrayList<>();
        if (getCrowds() != null)
            crowdsToValidate.addAll(getCrowds());
        crowdsToValidate.add(crowd);

        try
        {
            ValidationTools.checkDistinctPedestrianPositions(crowdsToValidate);
            ValidationTools.checkUniquePedestrianIds(crowdsToValidate);
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

        crowds = crowdsToValidate;
        crowdId = tempCrowdId;
        pedestrianId = tempPedestrianId;
    }

    /**
     * Removes the given {@code crowd} from the list of all crowds in this {@link CrowdSimulator}.
     *
     * @param crowd the crowd to be removed
     */
    public void removeCrowd(T crowd)
    {
        if (crowds == null)
            return;
        crowds.remove(crowd);
    }

    /**
     * Gets an object of the {@link Grid}.
     *
     * @return an object of the {@link Grid}
     */
    public Grid getGrid()
    {
        return getGrid(false);
    }

    /**
     * Gets an object of the {@link Grid}.
     *
     * @param createGrid if {@code true} and {@link #grid} is {@code null} an new {@link Grid}
     *            object is created
     * @return an object of the {@link Grid}
     */
    public Grid getGrid(boolean createGrid)
    {
        if (createGrid && grid == null)
        {
            ReferencedEnvelope gridBounds = new ReferencedEnvelope(boundingBox, crs);
            this.grid = new Grid(gridBounds);
        }
        return grid;
    }

    /**
     * Gets the {@link #crs}.
     *
     * @return the {@link #crs}
     */
    public CoordinateReferenceSystem getCrs()
    {
        return crs;
    }

    /**
     * Sets the default coordinate reference system in dependence to the coordinate reference
     * systems of the imported data, e.g. {@link Boundary}s or {@link WayPoint}s.
     *
     * @param crs an identifier of a coordinate reference system
     * @throws CrowdSimulatorNotValidException
     */
    public void setCrs(CoordinateReferenceSystem crs) throws CrowdSimulatorNotValidException
    {
        if (crs == null && this.crs != null)
        {
            // do not override valid known crs by unknown crs
        }
        else if (crs != null && this.crs == null)
        {
            this.crs = crs;
        }
        else if (crs != null && this.crs != null)
        {
            if ( !crs.equals(this.crs))
            {
                throw new CrowdSimulatorNotValidException(
                    "Datasets with different Coordinate Reference Systems are loaded, new:" + crs
                        + " != existing:" + this.crs);
            }
        }
        this.crs = crs;
    }

    /**
     * Implements run() of the interface {@link Runnable}. The Thread can process In fact that means
     * that it's possible to send instructions to the thread (if the thread is running).
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        startSimulationTime = fastForwardClock.currentTimeMillis(fastForwardFactor);

        while (isSimulationRunning.get())
        {
            logger.trace("CrowdSimulator.run(), fastForwardFactor=" + fastForwardFactor);

            long currentTime = fastForwardClock.currentTimeMillis(fastForwardFactor);

            // don't refresh more than 1 time per second
            if (currentTime - lastSimulationTime < minimumRefreshInterval)
                continue;
            isSimulatingInProgress = true;
            moveCrowds(currentTime);
            isSimulatingInProgress = false;
            lastSimulationTime = currentTime;
        }
    }

    /**
     * Moves all {@link Pedestrian} objects of the {@link Crowd} belonging to this
     * {@link CrowdSimulator}. Updating the {@link Grid} with new aggregate values of
     * {@link Pedestrian} object count per cell is invoked as well.
     *
     * @param currentTime the current timestamp in simulation time in milliseconds
     */
    public void moveCrowds(long currentTime)
    {
        if (lastSimulationTime == 0)
        {
            return;
        }

        List<T> currentCrowds = getCrowds();
        if (currentCrowds != null && !currentCrowds.isEmpty())
        {
            // collect all pedestrians from all crowds and make copies of them before any
            // crowd/pedestrian is moved
            updateQuadtreeForCrowds();

            this.simulationUpdateInterval = (currentTime - lastSimulationTime) / 1000d;
            this.totalSimulationSteps++ ;
            this.averageSimulationUpdateInterval = (averageSimulationUpdateInterval
                * (totalSimulationSteps - 1) + this.simulationUpdateInterval)
                / totalSimulationSteps;

            // iterate over all crowds to move all crowds
            for (T crowd : currentCrowds)
            {
                crowd.move(currentTime, simulationUpdateInterval);
                if (isUpdatingCrowdOutlines)
                    crowd.updateCrowdOutline(isClusteringCrowdOutlines, isCrowdOutlinesConvex);
            }
            // update grid
            if (grid != null)
                grid.update(currentCrowds, currentTime);
        }
    }

    /**
     * Updates the {@link #quadtree} of this {@link CrowdSimulator}, i.e. all {@link Pedestrian}
     * contained list of {@link #getCrowds()} are added to a newly created tree structure, which
     * means all {@link Pedestrian} objects previously contained will not be available anymore after
     * calling this method. <b>This only adds cloned {@link Pedestrian} objects to the
     * {@link #quadtree}</b>
     * <p>
     */
    public void updateQuadtreeForCrowds()
    {
        if (quadtree != null)
        {
            quadtree.updateCrowds(getCrowds());
        }
    }

    /**
     * Adds a {@link List} of {@link Boundary} objects to the {@link #quadtree} of this
     * {@link CrowdSimulator}
     * <p>
     * Furthermore the method updateCrowdOutline is invoked.
     *
     *
     * @param geometries the {@link List} of {@link Geometry} objects to be added as
     *            {@link Boundary} objects
     * @param ignoreInvalid if {@code true} in case Validation checks yield validation errors, only
     *            a warning is printed into the log output, otherwise
     *            {@link CrowdSimulatorNotValidException} is thrown
     *
     * @throws CrowdSimulatorNotValidException
     */
    public void addBoundaries(List<Geometry> geometries, boolean ignoreInvalid)
        throws CrowdSimulatorNotValidException
    {

        // use buffer so that target points on target lines of WayPoints cannot be too
        // close to boundaries
        double boundaryBuffer = 0;
        if (forceModel != null && forceModel instanceof HelbingModel)
        {
            // allow an arm's length space to the boundaries
            boundaryBuffer = 2 * ((HelbingModel) forceModel).getPedestrianRadius();
        }
        List<Boundary> boundaries = new ArrayList<>();
        for (Geometry geometry : geometries)
        {
            try
            {
                Boundary boundary = new Boundary(geometry,
                    forceModel.getMaxBoundaryInteractionDistance());
                if (boundaryBuffer > 0)
                    boundary.setBoundaryDistance(boundaryBuffer);
                boundaries.add(boundary);
            }
            catch (GeometryNotValidException e)
            {
                // silently skip unvalid geometries
                logger.trace("Unvalid geometry: " + geometry, e);
            }
        }

        List<T> currentCrowds = getCrowds();
        try
        {
            if (currentCrowds != null && !currentCrowds.isEmpty())
            {
                for (ICrowd crowdToCheck : currentCrowds)
                {
                    Route route = crowdToCheck.getRoute();
                    List<WayPoint> wayPoints = null;
                    if (route != null)
                        wayPoints = route.getWayPoints();
                    ValidationTools.checkInterWayPointVisibility(wayPoints, boundaries);
                    ValidationTools.checkPedestrianWayPointVisibility(crowdToCheck, wayPoints,
                        boundaries);
                }
            }
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

        // adds boundaries to quadtree
        if (quadtree != null)
            quadtree.addBoundaries(boundaries);

        // re-compute waypoint target lines and passing areas, if necessary
        updateRoutes();

        // update and intersect crowd outlines with new boundaries
        if (currentCrowds != null && !currentCrowds.isEmpty())
        {
            for (ICrowd crowd : currentCrowds)
            {
                crowd.updateCrowdOutline(isClusteringCrowdOutlines, isCrowdOutlinesConvex);
            }
        }
    }

    /**
     * Resets this {@link CrowdSimulator}. This include clearing the {@link List} of {@link Crowd}
     * objects, resetting the {@link Grid}, the {@link #boundingBox}, the
     * {@link #averageSimulationUpdateInterval}, the {@link #totalSimulationSteps}, the
     * {@link #crowdId} and the {@link #pedestrianId}
     */
    public void reset()
    {
        if (crowds != null)
            crowds.clear();
        if (grid != null)
            grid.reset();
        boundingBox = null;
        averageSimulationUpdateInterval = 0;
        totalSimulationSteps = 0;
        crowdId = 0;
        pedestrianId = 0;
    }

    /**
     * Gets the the artifact and version of this {@link CrowdSimulator} from a properties file that
     * is filled during build process.
     *
     * @return artifact and version
     */
    public String getVersion()
    {
        String version = "";
        Properties properties = new Properties();
        try
        {
            properties.load(
                this.getClass().getClassLoader().getResourceAsStream("crowdsimlib.properties"));
            version += properties.getProperty("artifactId");
            version += "-" + properties.getProperty("version");
        }
        catch (IOException e)
        {
            logger.info("Can't read version");
        }
        return version;
    }
}
