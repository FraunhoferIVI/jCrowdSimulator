package de.fhg.ivi.crowdsimulation.crowd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.opensphere.geometry.algorithm.ConcaveHull;

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.Geometry;

import de.fhg.ivi.crowdsimulation.CrowdSimulatorNotValidException;
import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.Route;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.WayPoint;
import de.fhg.ivi.crowdsimulation.geom.Quadtree;
import de.fhg.ivi.crowdsimulation.geom.QuadtreeAccess;

/**
 * Classes implementing this Interface represent Crowd objects containing {@link Pedestrian} objects
 * that are able to move following a given {@link Route} while interacting with other
 * {@link Pedestrian} objects and {@link Boundary} objects that are part of the same simulation.
 * <p>
 * Crowd objects can be given two velocities using {@link #setNormalDesiredVelocity(float, float)}
 * and {@link #setMaximumDesiredVelocity(float, float)}. It is assumed that the normal walking
 * velocity and the maximum walking velocity of all {@link Pedestrian}s are Gaussian distributed.
 *
 * @author hahmann
 *
 */
public interface ICrowd extends QuadtreeAccess, Identifiable
{

    /**
     * Gets a {@link List} of all {@link Pedestrian} objects belonging to this {@link Crowd}
     *
     * @return the {@link Pedestrian} object as an {@link List}.
     */
    public List<Pedestrian> getPedestrians();

    /**
     * Gets a {@link List} of all {@link Pedestrian} objects belonging to this {@link Crowd}. If
     * {@code clone} parameter is set to {@code true}, a new {@link ArrayList} is returned
     * containing cloned {@link Pedestrian} objects
     *
     * @param clone if {@code true} a new {@link ArrayList} is returned containing cloned
     *            {@link Pedestrian} objects otherwise a list of {@link Pedestrian} objects that is
     *            contained in all {@link Group}s of this {@link Crowd} is returned directly
     * @return the list of all {@link Pedestrian} objects contained in this Crowd
     */
    public List<Pedestrian> getPedestrians(boolean clone);

    /**
     * Moves all {@link Group}s belonging to this {@link Crowd} (and thus all {@link Pedestrian}s
     * belonging to this {@link Crowd}). Implementing classes should consider parallel processing,
     * i.e. move multiple Groups in parallel due to the usage {@link ThreadPoolExecutor}s.
     * <p>
     * <b>Important: {@link #getQuadtree()} needs to be updated before this method is called using
     * {@link Quadtree#updateCrowds(List)} otherwise {@link Pedestrian}s will interact with old
     * positions of {@link Pedestrian} but not with the current ones.</b>
     * <p>
     *
     * @param time the current absolute time in simulated time (given in milliseconds)
     * @param simulationUpdateInterval the time between this call of the method and the last one
     *            (i.e. the time between 2 consecutive simulation steps given in seconds)
     */
    public void move(long time, double simulationUpdateInterval);

    /**
     * Sets the {@link List} of {@link WayPoint} objects that all {@link Pedestrian}s of all
     * {@link Group}s of this {@link Crowd} will follow during their movement.
     * <p>
     * Furthermore, the {@link #getQuadtree()} allows validating {@link Route} objects, which
     * ensures that all {@link Pedestrian}s belonging to implementing classes are able to follow the
     * given {@link Route} from start to end.
     * <p>
     *
     * @param route the route object that contains the {@link List} of {@link WayPoint} objects that
     *            indicates the route, which the {@link Pedestrian}s of this {@link Crowd} will
     *            follow during locomotion.
     * @param time the time when this {@link Crowd} gets a the newList of {@link WayPoint} objects,
     *            assuming that this {@link Crowd} starts moving towards the new route, immediately
     *            after this method has been called.
     * @param ignoreInvalid if {@code true} in case validation checks yield validation errors, only
     *            a warning is printed into the log output, otherwise
     *            {@link CrowdSimulatorNotValidException} is thrown
     *
     * @throws CrowdSimulatorNotValidException
     *
     *             Following conditions will cause throwing of
     *             {@link CrowdSimulatorNotValidException} (only in case {@code ingoreInvalid} is
     *             {@code false}
     *             <ul>
     *             <li>There is no direct line of sight between all pairs of consecutive
     *             {@link WayPoint}s</li>
     *             <li>There is a {@link Pedestrian} that cannot see its first {@link WayPoint} on
     *             its route</li>
     *             </ul>
     */
    public void setRoute(Route route, long time, boolean ignoreInvalid)
        throws CrowdSimulatorNotValidException;

    /**
     * Gets the route object of this {@link Crowd} that contains the {@link List} of
     * {@link WayPoint} objects that indicates the route, which the {@link Pedestrian}s of this
     * {@link Crowd} will follow during locomotion.
     *
     * @return the route object of this {@link Crowd} that contains the {@link List} of
     *         {@link WayPoint} objects that indicates the route, which the {@link Pedestrian}s of
     *         this {@link Crowd} will follow during locomotion.
     */
    public Route getRoute();

    /**
     * Updates {@link Geometry} objects that can be accessed via {@link #getCrowdOutlines()} and
     * represent outlines of classes implementing this interface based on the current position of
     * {@link Pedestrian} objects ({@link #getPedestrians()}.
     * <p>
     * Implementing classes are expected to difference {@link Boundary} objects from the computed
     * {@link Geometry} objects, if such {@link Boundary} objects are accessible via
     * {@link #getQuadtree()}. with {@link Boundary} objects.
     * <p>
     *
     * @param isClusteringCrowdOutlines Indicates, if the {@link Pedestrian} objects are clustered
     *            into groups using {@link DBSCANClusterer} before the crowd outlines are computed
     * @param isCrowdOutlinesConvex indicates, if the calculation of the crowd outline use a
     *            {@link ConvexHull} or a {@link ConcaveHull} algorithm
     */
    public void updateCrowdOutline(boolean isClusteringCrowdOutlines,
        boolean isCrowdOutlinesConvex);

    /**
     * Gets a {@link List} of {@link Geometry} objects that represent the outlines of the
     * {@link Pedestrian}s contained in this {@link ICrowd}.
     *
     * @return an a {@link List} of {@link Geometry} that represent the outlines of this
     *         {@link ICrowd}.
     */
    public List<Geometry> getCrowdOutlines();

    /**
     * Sets a {@link List} of {@link Geometry} objects that represent the outlines of the
     * {@link Pedestrian}s contained in this {@link ICrowd}.
     *
     * @param crowdOutlines a {@link List} of {@link Geometry} that represent the outlines of this
     *            {@link ICrowd}
     */
    public void setCrowdOutlines(List<Geometry> crowdOutlines);

    /**
     * Set the {@link ThreadPoolExecutor} object. A {@link ThreadPoolExecutor} may significantly
     * improve performance of {@link #move(long, double)}, since it allows parallelization of Crowd
     * movement computations.
     * <p>
     *
     * @param threadPool the {@link ThreadPoolExecutor} for parallelization of Pedestrian movement
     *            computation
     */
    public void setThreadPool(ThreadPoolExecutor threadPool);

    /**
     * Sets the normal desired velocities that all {@link Pedestrian} being part of this
     * {@link ICrowd} want to reach (without being "delayed") given in m/s. Thus updates the normal
     * desired velocities of all {@link Pedestrian}. Cf. Helbing et al. (2005) p. 11.
     *
     * @param meanNormalDesiredVelocity the mean of the velocities that all {@link Pedestrian} want
     *            to reach (without being "delayed")
     * @param standardDeviationOfNormalDesiredVelocity the standard deviation of mean normal desired
     *            velocity
     */
    public abstract void setNormalDesiredVelocity(float meanNormalDesiredVelocity,
        float standardDeviationOfNormalDesiredVelocity);

    /**
     * Sets the maximum possible velocities that all {@link Pedestrian} being part of this
     * {@link ICrowd} can reach (in case of being "delayed") given in m/s. Thus updates the maximum
     * desired velocities of all {@link Pedestrian}. Cf. Helbing et al. (2005) p. 11.
     *
     * @param meanMaximumDesiredVelocity the mean of the velocities that all {@link Pedestrian} can
     *            reach (in case of being "delayed")
     * @param standardDeviationOfMaximumDesiredVelocity the standard deviation of mean maximum
     *            desired velocity
     */
    public abstract void setMaximumDesiredVelocity(float meanMaximumDesiredVelocity,
        float standardDeviationOfMaximumDesiredVelocity);
}
