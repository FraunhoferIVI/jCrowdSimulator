package de.fhg.ivi.crowdsimulation.crowd;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import de.fhg.ivi.crowdsimulation.CrowdSimulator;
import de.fhg.ivi.crowdsimulation.CrowdSimulatorNotValidException;
import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.ForceModel;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.NumericIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.RungeKuttaIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.SemiImplicitEulerIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.SimpleEulerIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.Route;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.WayPoint;
import de.fhg.ivi.crowdsimulation.geom.Quadtree;

/**
 * Class used to generate {@link Route} from {@link Coordinate}s.
 * <p>
 * This class has a {@link ForceModel} and a {@link Quadtree} needed to create complete
 * {@link WayPoint} objects. Thus the {@link WayPoint} implementation does not need these fields,
 * since they are not needed again after the creation of these objects.
 *
 * @author hahmann
 *
 */
public class CrowdFactory
{
    /**
     * Uses the object logger for printing specific messages in the console.
     */
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Can be one of the following methods of numerical mathematics to compute {@link Pedestrian}
     * movement - Simple Euler {@link SimpleEulerIntegrator}, Semi Implicit Euler
     * {@link SemiImplicitEulerIntegrator} or Runge Kutta {@link RungeKuttaIntegrator}.
     */
    private NumericIntegrator   numericIntegrator;

    /**
     * {@link ForceModel} objects, which represents the force model for pedestrian movement
     */
    private ForceModel          forceModel;

    /**
     * The {@link Quadtree} object allows (efficient) access to {@link Boundary} objects and
     * {@link Pedestrian} objects belonging to the same {@link CrowdSimulator} and makes
     * Pedestrians-Boundary and Pedestrian-Pedestrian interaction possible.
     */
    private Quadtree            quadtree;

    /**
     * Thread Pool for parallelization of Pedestrian movement computation
     */
    private ThreadPoolExecutor  threadPool;

    /**
     * @param numericIntegrator
     * @param forceModel
     * @param quadtree
     * @param threadPool
     */
    public CrowdFactory(NumericIntegrator numericIntegrator, ForceModel forceModel,
        Quadtree quadtree, ThreadPoolExecutor threadPool)
    {
        this.numericIntegrator = numericIntegrator;
        this.forceModel = forceModel;
        this.quadtree = quadtree;
        this.threadPool = threadPool;
    }

    /**
     * Creates a new {@link Crowd} object including the the {@link Pedestrian}s which are contained
     * by the {@link Crowd}, the {@link #numericIntegrator}, the {@link #forceModel}, the
     * {@link #threadPool} of this crowd.
     * <p>
     * Uses {@code wayPointList} as {@link WayPoint} objects for the {@link Pedestrian} objects
     *
     * @param pedestrians pedestrianPositions a {@link List} of {@link Coordinate}s, which describe
     *            the starting positions (x,y) of the {@link Pedestrian}s
     * @param ignoreInvalid if {@code true} in case Validation checks yield validation errors, only
     *            a warning is printed into the log output, otherwise
     *            {@link CrowdSimulatorNotValidException} is thrown
     *
     *
     * @return the created Crowd object
     * @throws CrowdSimulatorNotValidException in case 2 or more {@link Pedestrian} objects of the
     *             created {@link Crowd} object have identical positions.
     */
    private Crowd createCrowd(List<Coordinate> pedestrians, boolean ignoreInvalid)
        throws CrowdSimulatorNotValidException
    {
        // create new crowd
        Crowd crowd = new Crowd(forceModel, numericIntegrator);
        crowd.setQuadtree(quadtree);
        crowd.setThreadPool(threadPool);
        crowd.setPedestrians(pedestrians);
        return crowd;
    }

    /**
     * Creates a new {@link Crowd} object including the {@link Pedestrian}s which are contained by
     * the {@link Crowd}, the id of the Crowd, the {@link #numericIntegrator}, the
     * {@link #forceModel}, the {@link #threadPool} used by this crowd.
     * <p>
     *
     * @param pedestrians pedestrianPositions a {@link HashMap} of {@link Integer} (used as
     *            Pedestrian Ids) and {@link Coordinate}s which describe the starting positions
     *            (x,y) of the {@link Pedestrian}s
     *
     * @param ignoreInvalid if {@code true} in case Validation checks yield validation errors, only
     *            a warning is printed into the log output, otherwise
     *            {@link CrowdSimulatorNotValidException} is thrown
     *
     * @throws CrowdSimulatorNotValidException
     * @return the created Crowd object
     */
    public Crowd createCrowdFromGeometries(List<Geometry> pedestrians, boolean ignoreInvalid)
        throws CrowdSimulatorNotValidException
    {
        List<Coordinate> pedestrianCoordinates = new ArrayList<>();
        for (Geometry pedestrian : pedestrians)
        {
            pedestrianCoordinates.add(pedestrian.getCoordinate());
        }
        return createCrowdFromCoordinates(pedestrianCoordinates, ignoreInvalid);
    }

    /**
     * Creates a new {@link Crowd} object including the {@link Pedestrian}s which are contained by
     * the {@link Crowd}. Method takes care that {@link Crowd} ids and {@link Pedestrian} ids are
     * unique
     * <p>
     * Sets the current {@link ForceModel} ({@link #forceModel}, the current
     * {@link NumericIntegrator} ({@link #numericIntegrator} and the {@link ThreadPoolExecutor} to
     * the newly created {@link Crowd} object.
     *
     * @param pedestrians pedestrianPositions a {@link HashMap} of {@link Integer} (used as
     *            Pedestrian Ids) and {@link Coordinate}s which describe the starting positions
     *            (x,y) of the {@link Pedestrian}s
     * @param ignoreInvalid if {@code true} in case Validation checks yield validation errors, only
     *            a warning is printed into the log output, otherwise
     *            {@link CrowdSimulatorNotValidException} is thrown
     *
     * @throws CrowdSimulatorNotValidException
     * @return the created Crowd object
     */
    public Crowd createCrowdFromCoordinates(List<Coordinate> pedestrians, boolean ignoreInvalid)
        throws CrowdSimulatorNotValidException
    {
        return createCrowd(pedestrians, ignoreInvalid);
    }

    /**
     * Creates a new {@link Pedestrian} object and initializes the variables the {@link Pedestrian}
     * needs to know to realize his/her movement.
     * <p>
     * Sets the current {@link ForceModel} ({@link #forceModel}, the current
     * {@link NumericIntegrator} ({@link #numericIntegrator} and the {@link ThreadPoolExecutor} to
     * the newly created {@link Pedestrian} object.
     *
     * @param initialPositionX x component of the position of a {@link Pedestrian} at the time of
     *            its creation
     * @param initialPositionY y component of the position of a {@link Pedestrian} at the time of
     *            its creation
     * @param normalDesiredVelocity the desired velocity which a {@link Pedestrian} wants to reach
     *            (without being "delayed") in normal situations. Given in m/s
     * @param maximumDesiredVelocity the maximal desired velocity a {@link Pedestrian} can reach
     *            (when being "delayed") given in m/s
     */
    public Pedestrian createPedestrian(double initialPositionX, double initialPositionY,
        float normalDesiredVelocity, float maximumDesiredVelocity)
    {
        return new Pedestrian(initialPositionX, initialPositionY, normalDesiredVelocity,
            maximumDesiredVelocity, forceModel, numericIntegrator, quadtree);
    }
}
