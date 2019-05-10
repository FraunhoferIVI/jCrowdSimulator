package de.fhg.ivi.crowdsimulation.crowd.forcemodel;

import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.math.Vector2D;

import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.boundaries.BoundarySegment;
import de.fhg.ivi.crowdsimulation.crowd.Group;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingBuznaModel;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingJohanssonModel;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.moussaid.MoussaidModel;

/**
 * The {@link ForceModel} represents a force-based approach for modeling microscopic pedestrian
 * simulation. It is a mixture of empirical parameters and Newton's 2nd law of motion. The original
 * model or Social Force Model is based on the publication
 * <a href= "https://doi.org/10.1103/PhysRevE.51.4282">Helbing and Molnár (1995)</a>. Because of the
 * empirical parameters, there are different possible solutions.
 * <p>
 * Each ForceModel should implement forces for following interactions:
 * <ul>
 * <li>Pedestrian - Boundary (Obstacle),
 * {@link ForceModel#interactBoundary(Vector2D, BoundarySegment)}</li>
 * <li>Pedestrian - Pedestrian,
 * {@link ForceModel#interactPedestrian(Vector2D, Vector2D, Pedestrian)}</li>
 * <li>Pedestrian - Group,
 * {@link ForceModel#interactGroup(Vector2D, Vector2D, Coordinate, List)}</li>
 * </ul>
 * Moreover a ForceModel should implement methods to model the intrinsic force of a
 * {@link Pedestrian} towards a specific destination.
 * <p>
 * Currently, it is possible to choose between the classical Helbing model {@link HelbingBuznaModel}
 * <a href="https://doi.org/10.1287/inte.2017.090610.1287/trsc.1040.0108">(Helbing et al., 2005)</a>
 * and Johansson model {@link HelbingJohanssonModel}
 * <a href="https://doi.org/10.1103/PhysRevE.75.046109">(Johansson et al., 2007)</a>. <b>The
 * {@link HelbingJohanssonModel} currently needs more numerical testing</b>
 * <p>
 * The difference between the models lies in the assumed force fields of the pedestrians and the
 * force parameters (i.e. strength of force field and size). Helbing assumes a circular field and
 * does not consider the a difference between the interaction of Pedestrians that are in front of or
 * behind another Pedestrian. Johansson uses an elliptical force field and considers these
 * differences.
 * <p>
 * The modeling of Group behaviour is not yet fully tested. It follows the approach of
 * <a href="https://doi.org/10.1371/journal.pone.0010047">Moussaïd et al. (2010).</a>
 *
 * <p>
 * Further ForceModels are under construction e.g. a model of Moussaïd {@link MoussaidModel}
 * <a href="https://doi.org/10.1098/rspb.2009.0405">(Moussaïd et al., 2009)</a>.
 *
 * @see <a href= "https://doi.org/10.1103/PhysRevE.51.4282">Helbing and Molnár (1995)</a>
 * @see <a href="https://doi.org/10.1287/inte.2017.090610.1287/trsc.1040.0108">Helbing et al.
 *      (2005)</a>
 * @see <a href="https://doi.org/10.1103/PhysRevE.75.046109">Johansson et al. (2007)</a>
 * @see <a href="https://doi.org/10.1098/rspb.2009.0405">Moussaïd et al. (2009)</a>
 * @see <a href="https://doi.org/10.1371/journal.pone.0010047">Moussaïd et al. (2010).</a>
 *
 *
 * @author hahmann/meinert
 */
public abstract class ForceModel
{
    /**
     * Maximum distance within the interaction between two {@link Pedestrian} is regarded (to
     * accelerate simulation). Given in meters.
     */
    protected float           maxPedestrianInteractionDistance;

    /**
     * Maximum distance within the interaction between a pedestrian and a boundary is regarded (to
     * accelerate simulation). Given in meters.
     */
    protected float           maxBoundaryInteractionDistance;

    /**
     * Minimal force threshold which is assumed to have an effect on the {@link Pedestrian}.
     *
     * <li>if this threshold is too small, too it will slow down performance, since too many
     * calculations for Pedestrian-Pedestrian and Pedestrian-Boundary interaction need be done that
     * actually have no significant effect on the pedestrians' movement
     * <li>if this threshold is too high, the simulation will get inaccurate, i.e. the Pedestrian
     * will only react to close boundaries / pedestrians we it gets very near to them
     */
    public static final float limitResultingForce = 0.01f;

    /**
     * Gets the acceleration that is needed to reach the desired velocity and go in the desired
     * direction.
     *
     * @param currentPosition the current x, y position as {@link Vector2D} of the
     *            {@link Pedestrian}
     * @param currentVelocity the current velocity {@link Vector2D} of the {@link Pedestrian}
     * @param normalizedDirectionVector the direction {@link Vector2D} in which the
     *            {@link Pedestrian} wants to walk.
     * @param averageVelocity the average velocity of the {@link Pedestrian} into its desired
     *            direction of motion
     * @param preferredVelocity preferredVelocity the preferred velocity of the {@link Pedestrian},
     *            i.e. the velocity that the {@link Pedestrian} intrinsically would like to have
     * @param maximumDesiredVelocity the maximal desired velocity the {@link Pedestrian}
     *
     * @return the {@link Vector2D} force component that is needed to reach the desired velocity in
     *         the desired direction.
     */
    public abstract Vector2D intrinsicForce(Vector2D currentPosition, Vector2D currentVelocity,
        Vector2D normalizedDirectionVector, float averageVelocity, float preferredVelocity,
        float maximumDesiredVelocity);

    /**
     * Computes the force resulting from pedestrian-pedestrian interaction. Checks, if the distance
     * between {@code currentPosition} and the {@link Pedestrian} is smaller than
     * {@link #getMaxPedestrianInteractionDistance()} before.
     *
     * @param currentPosition denotes the approximated x, y position as {@link Vector2D} of the
     *            {@link Pedestrian}
     * @param currentVelocity denotes the velocity {@link Vector2D} of the {@link Pedestrian}
     * @param pedestrian the {@link Pedestrian} object
     *
     * @return the {@link Vector2D} vector which resulting from the interaction of the current
     *         {@link Pedestrian} with another {@code pedestrian}
     */
    public abstract Vector2D interactPedestrian(Vector2D currentPosition, Vector2D currentVelocity,
        Pedestrian pedestrian);

    /**
     * Computes the force resulting from pedestrian-boundary interaction. Checks, if the distance
     * between {@code currentPosition} and the {@link Pedestrian} is smaller than
     * {@link #getMaxPedestrianInteractionDistance()} before.
     *
     * @param currentPosition denotes the approximated x, y position as {@link Vector2D} of the
     *            {@link Pedestrian}
     * @param boundary the {@link Geometry} object
     *
     * @return the {@link Vector2D} vector which resulting from the interaction of the current
     *         {@link Pedestrian} with a {@code geometry}
     */
    public abstract Vector2D interactBoundary(Vector2D currentPosition, BoundarySegment boundary);

    /**
     * <b>Unfinished! - Computes wrong results!</b> Computes the force that results from the
     * interaction of a {@link Pedestrian} with all members of the {@link Group} it belongs to so
     * that the {@link Group} members stay/walk together.
     *
     * @param currentPosition denotes the approximated x, y position as {@link Vector2D} of the
     *            {@link Pedestrian}
     * @param currentVelocity the current velocity of the {@link Pedestrian}
     * @param groupCentroid the centroid of the group the {@link Pedestrian} belongs to
     * @param groupPositions a {@link List} of {@link Coordinate}s representing the positions of all
     *            members of the Group
     * @return
     */
    public abstract Vector2D interactGroup(Vector2D currentPosition, Vector2D currentVelocity,
        Coordinate groupCentroid, List<Coordinate> groupPositions);

    /**
     * Gets the radius of a {@link Pedestrian}. Given in meters.
     *
     * @return the radius of a {@link Pedestrian}. Given in meters.
     */
    public abstract float getPedestrianRadius();

    /**
     * Computes the distance, in which a {@link Pedestrian} interacts with a {@link Boundary}. The
     * calculation formula is the formula of the interact() converted to the distance.
     * <p>
     * This distance depends on given constants and the resulting force
     * {@link ForceModel#limitResultingForce}. It is assumed that the force effect on the
     * pedestrians is negligible below this value.
     *
     * @return distance, given in meters, in which the {@link Pedestrian} interacts with a
     *         {@link Boundary}
     */
    public abstract float getMaxBoundaryInteractionDistance();

    /**
     * Computes the distance, in which a {@link Pedestrian} interacts with another
     * {@link Pedestrian}. The calculation formula is the formula of the
     * {@link #interactPedestrian(Vector2D, Vector2D, Pedestrian)} converted to the distance.
     * <p>
     * This distance depends on given constants and the resulting force
     * {@link ForceModel#limitResultingForce}. It is assumed that the force effect on the
     * pedestrians is negligible below this value.
     *
     * @return distance, given in meters, in which the {@link Pedestrian} interacts with another
     *         {@link Pedestrian}
     */
    public abstract float getMaxPedestrianInteractionDistance();
}
