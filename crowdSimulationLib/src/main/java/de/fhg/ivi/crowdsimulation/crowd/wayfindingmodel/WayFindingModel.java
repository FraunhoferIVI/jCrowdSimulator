package de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.TopologyException;
import com.vividsolutions.jts.math.Vector2D;

import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.boundaries.BoundarySegment;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.WayPoint;
import de.fhg.ivi.crowdsimulation.geom.Quadtree;
import de.fhg.ivi.crowdsimulation.geom.QuadtreeAccess;
import de.fhg.ivi.crowdsimulation.math.MathTools;

/**
 * This class is the super class (abstract class) in case of the way finding algorithm of the
 * {@link Pedestrian}s.
 * <p>
 * If this class shall calculate a new way for the {@link Pedestrian}s, some preconditions must be
 * fulfilled. It must be {@code true} that a {@link Pedestrian} has extrinsic forces, needs
 * orientation generally or his/her last movement calculation is longer ago than 5 seconds or 5
 * meters in x-, y-direction.
 * <p>
 * Furthermore this class checks whether a {@link Pedestrian} has passed a {@link WayPoint},
 * calculates the normalized direction vector of the {@link Pedestrian} movement and calculates the
 * total distance on the route of the {@link Pedestrian}s since his/her starting point.
 *
 * @author hahmann/meinert
 */
public abstract class WayFindingModel implements QuadtreeAccess
{

    private static final Logger logger             = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());

    /**
     * {@link Quadtree} object, which can be use to do spatial queries
     */
    protected Quadtree          quadtree;

    /**
     * Indicates the desired movement direction of this {@link WayFindingModel}. This translates to
     * "the {@link Pedestrian} wants to move into this direction".
     *
     * This {@link Vector2D} is assumed to be always normalized to a length of 1.
     */
    protected Vector2D          normalizedDirectionVector;

    /**
     * The position as {@link Vector2D} object that serves as the current target position of this
     * {@link WayFindingModel}
     */
    protected Vector2D          targetPosition;

    /**
     * {@code true} if this {@link Pedestrian} cannot find a target coordinate on the
     * {@link WayPoint#getTargetLine()} and thus tries to either head to the connecting line between
     * the current and the previous waypoint or the last position, where the next waypoint could be
     * seen, and also in the case of the start of the simulation. Else {@code false}.
     */
    protected boolean           needsOrientation   = true;

    /**
     * The time stamp when the {@link Pedestrian} starts moving towards its destination - this
     * should be updated, when this {@link Pedestrian} starts moving according to this
     * {@link WayFindingModel}. Given in milliseconds.
     */
    protected long              startTime;

    /**
     * {@code true} if the actual course deviates from the required course to reach the current
     * WayPoints. Else {@code false}.
     */
    protected boolean           hasCourseDeviation = false;

    /**
     * Creates a new {@link WayFindingModel} using the given {@code quadtree}.
     *
     * @param quadtree the quadtree for spatial searches
     */
    public WayFindingModel(Quadtree quadtree)
    {
        this.quadtree = quadtree;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Quadtree getQuadtree()
    {
        return this.quadtree;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setQuadtree(Quadtree quadtree)
    {
        this.quadtree = quadtree;
    }

    /**
     * Gets the average velocity of the into the desired direction of motion at the current position
     * and the current time
     *
     * @param position the current position
     * @param currentTime the current time
     * @param useCache if {@code true} cached distance values are taken for velocity computation,
     *            otherwise the distance values are forced to be calculated prior to velocity
     *            computation
     * @return the average velocity
     */
    public abstract float getAverageVelocity(Vector2D position, long currentTime, boolean useCache);

    /**
     * Updates the {@link Vector2D} {@link #normalizedDirectionVector} that indicates the desired
     * movement direction of this {@link WayFindingModel}. This translates to "The
     * {@link Pedestrian} wants to move into this direction".
     *
     * While updating, the {@link #normalizedDirectionVector} is normalized to a length of 1)
     *
     * @param position the {@link Vector2D} position at which the normalized direction vector should
     *            be derived
     * @param time the time, when this method is called (in simulation time)
     * @param normalDesiredVelocity the normal desired velocity of the pedestrian - used for
     *            checking, if the Pedestrian is not moving anymore
     */
    public abstract void updateNormalizedDirectionVector(Vector2D position, long time,
        float normalDesiredVelocity);

    /**
     * Updates this {@link WayFindingModel} for a given {@link Pedestrian} and given current
     * position ({@code currentPosition}) as well as a given updated position
     * ({@code updatedPosition}), which could be the position before and after a move
     *
     * @param time the current simulated time (in ms)
     * @param currentPosition the current position of the {@link WayFindingModel}
     * @param updatedPosition an updated position of the {@link WayFindingModel}
     */
    public abstract void updateModel(long time, Vector2D currentPosition, Vector2D updatedPosition);

    /**
     * Checks if the actual course (i.e. angle of {@link #normalizedDirectionVector}) deviates from
     * the required course (i.e. angle between current position of the pedestrian and its current
     * target point) above a defined threshold and mark this as such.
     *
     * @param pedestrian the pedestrian to be checked for course deviation
     * @param time the current simulated time (in ms)
     */
    public abstract void checkCourse(Pedestrian pedestrian, long time);

    /**
     * Gets the desired movement direction of this {@link WayFindingModel}. This translates to "the
     * {@link Pedestrian} wants to move into this direction".
     *
     * This {@link Vector2D} is assumed to be always normalized to a length of 1.
     * </p>
     *
     * <b>Please note: this methods only returns {@link #normalizedDirectionVector} without any
     * recalculation. If recalculation is required, call
     * {@link #updateNormalizedDirectionVector(Vector2D, long, float)}</b>
     * </p>
     *
     * @return the current normalized direction vector
     */
    public Vector2D getNormalizedDirectionVector()
    {
        return normalizedDirectionVector;
    }

    /**
     * Get whether this {@link Pedestrian} to {@link #needsOrientation}.
     *
     * @return {@code true} if this {@link Pedestrian} cannot find a {@link WayPoint} on the
     *         {@link WayPoint#getTargetLine()}, and must use an alternative wayPoint, and also in
     *         the case of the start of the simulation. Else {@code false}.
     */
    public boolean needsOrientation()
    {
        return needsOrientation;
    }

    /**
     * Set this {@link Pedestrian} to {@link #needsOrientation}.
     *
     * @param needsOrientation {@code true} if this {@link Pedestrian} cannot find a position on the
     *            {@link WayPoint#getTargetLine()}, and must use an alternative wayPoint, and also
     *            in the case of the start of the simulation. Else {@code false}.
     */
    public void setNeedsOrientation(boolean needsOrientation)
    {
        this.needsOrientation = needsOrientation;
    }

    /**
     * Get {@link #startTime}.
     *
     * @return the start time, when the {@link Pedestrian} started moving. Given in milliseconds in
     *         simulation time
     */
    public long getStartTime()
    {
        return startTime;
    }

    /**
     * Sets the {@link #startTime}
     *
     * @param startTime the time, when the {@link Pedestrian} starts moving towards the route given
     *            in this {@link WayFindingModel}. Given in milliseconds.
     */
    public void setStartTime(long startTime)
    {
        this.startTime = startTime;
    }

    /**
     * Checks whether extrinsic forces impact a {@link Pedestrian}.
     *
     * @return {@code true} if extrinsic forces are not null. Else {@code false}
     */
    public boolean hasCourseDeviation()
    {
        return hasCourseDeviation;
    }

    /**
     * Gets the {@link #targetPosition}.
     *
     * @return the {@link #targetPosition}
     */
    public Vector2D getTargetPosition()
    {
        return targetPosition;
    }

    /**
     * Updates the {@link #normalizedDirectionVector} using {@link #targetPosition} by computing the
     * vector between the {@link #targetPosition} and the given {@code position} and normalizing the
     * length of this vector to 1 afterwards.
     *
     * This is the implementation of formula (1) of Helbing et al. (1995)
     *
     * @param position the given position
     */
    public void updateNormalizedDirectionVector(Vector2D position)
    {
        if (position.equals(targetPosition))
            normalizedDirectionVector = new Vector2D(0, 0);
        else
        {
            // this is the implementation of formula(1) of Helbing et al. (1995)
            Vector2D directionVector = targetPosition.subtract(position);
            normalizedDirectionVector = MathTools.normalize(directionVector.getX(),
                directionVector.getY());
        }
        if (Double.isNaN(normalizedDirectionVector.getX()))
            logger.info("normalizedDirectionVector=" + normalizedDirectionVector
                + ", targetPosition=" + targetPosition + ", position=" + position);
    }

    /**
     * Checks whether there is a direct line of sight between the {@link Vector2D} {@code position}
     * and the {@link Coordinate} {@code target} or whether it is blocked by {@link Boundary}
     * object.
     *
     * @param pedestrianPosition the position to test
     * @param targetPositionToTest the target to test
     *
     * @return {@code true} if there is a direct line of sight between the {@code position} and
     *         {@code target}, {@code false} otherwise. Also returns {@code false}, if
     *         {@code target} is {@code null}
     */
    protected boolean isTargetVisible(Vector2D pedestrianPosition, Coordinate targetPositionToTest)
    {
        // target in not visible, if target is not even set
        if (targetPositionToTest == null)
        {
            return false;
        }

        // computes LineString between Pedestrian and next Waypoint
        Coordinate[] coordinates = new Coordinate[] { pedestrianPosition.toCoordinate(),
            targetPositionToTest };
        LineString lineOfSight = JTSFactoryFinder.getGeometryFactory()
            .createLineString(coordinates);

        // checks if the Pedestrian sees its next Waypoint.
        List<BoundarySegment> boundaries = null;
        if (quadtree != null)
            boundaries = quadtree.getBoundarySegments(lineOfSight.getEnvelopeInternal());

        boolean lineOfSightIntersectsBoundary = false;
        if (boundaries != null && !boundaries.isEmpty())
        {
            for (BoundarySegment boundary : boundaries)
            {
                // accurate check
                try
                {
                    lineOfSightIntersectsBoundary = lineOfSight.intersects(boundary.getGeometry());
                    if (lineOfSightIntersectsBoundary)
                        break;
                }
                catch (TopologyException e)
                {
                    lineOfSightIntersectsBoundary = false;
                    logger.info("pedestrianPosition=" + pedestrianPosition + ", target="
                        + targetPositionToTest + ", lineOfSight=" + lineOfSight, e);
                }

            }

        }
        return !lineOfSightIntersectsBoundary;
    }

}
