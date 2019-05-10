package de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.math.Vector2D;

import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.boundaries.BoundarySegment;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.NumericIntegrationTools;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.WayFindingModel;
import de.fhg.ivi.crowdsimulation.geom.GeometryTools;
import de.fhg.ivi.crowdsimulation.geom.Quadtree;
import de.fhg.ivi.crowdsimulation.math.MathTools;

/**
 * This class implements a way finding model that allows a {@link Pedestrian} to follow a series of
 * {@link WayPoint} one after another.
 * <p>
 * This model knows the position and sequence of the {@link WayPoint}s, which define the intended
 * route.
 * <p>
 * Furthermore, there are some validation routines, which ensure that the way finding process works
 * well, including cases, when a {@link Pedestrian} can't see its next {@link WayPoint}, since it is
 * hidden behind a {@link Boundary} object. In such cases, this Model will head towards the
 * connecting line between the previous and the current target waypoint or, if this is also not
 * possible, to back to a position where the current target {@link WayPoint} is visible.
 *
 * @author hahmann/meinert
 */
public class FollowRouteModel extends WayFindingModel
{

    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger logger                                 = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The {@link Route} of this {@link FollowRouteModel}
     */
    private Route               route;

    /**
     * {@link List} of {@link Coordinate} which have already been visited by the {@link Pedestrian}
     */
    private List<WayPoint>      visitedWayPoints;

    /**
     * The current destination in form of a {@link WayPoint} of this {@link Pedestrian}.
     */
    private WayPoint            currentDestinationWayPoint;

    /**
     * Describes the resolution value in which the {@link WayPoint#getTargetLine()} is investigated
     * in the search for an alternative target point
     */
    public static final float   SEARCH_RESOLUTION_ALTERNATIVE_POSITION = WayPoint.DEFAULT_WIDTH
        / 10f;

    /**
     * Defines the last time a {@link Pedestrian} has got a new orientation. Given in milliseconds.
     */
    private long                lastOrientationUpdateTime              = 0;

    /**
     * Gets the position of the last valid orientation update. Valid means either visible point on
     * {@link WayPoint#getTargetLine()} or visible point on
     * {@link WayPoint#getConnectionLineToPredecessor()} was seen from that position.
     */
    private Vector2D            lastOrientationUpdatePostion           = null;

    /**
     * Defines the last time a {@link Pedestrian} has got a new course. Given in milliseconds.
     */
    private long                lastCourseUpdateTime                   = Long.MIN_VALUE;

    /**
     * Defines the last position a {@link Pedestrian} has got a new course.
     */
    private Vector2D            lastCourseUpdatePostion                = null;

    /**
     * Defines the maximum distance threshold, after which a {@link Pedestrian} checks for a new
     * orientation/course at the latest. Given in meters.
     */
    private final static float  COURSE_UPDATE_DISTANCE                 = 5f;

    /**
     * Defines the time interval threshold, after which a {@link Pedestrian} checks for a new
     * orientation/course at the latest. Given in milliseconds.
     */
    private final static int    COURSE_UPDATE_INTERVAL                 = 5000;

    /**
     * Maximum allowed course deviation. If the difference between the actual course and the
     * required course is above this threshold,
     * {@link #updateNormalizedDirectionVector(Vector2D, long, float)} should be called.
     *
     * Hint: 1 degree = 0.0175 radians, 5 degree = 0.0873 radians
     */
    public static final float   MAXIMUM_COURSE_DEVIATION               = 0.0175f;

    /**
     * Starting x, y position as {@link Vector2D} of the {@link Pedestrian}. This is updated, when
     * {@link #setRoute(Route, Vector2D)} is called.
     */
    private Vector2D            startPosition;

    /**
     * {@code true} if this {@link Pedestrian} has passed its {@link #currentDestinationWayPoint}
     * during its last move
     */
    private boolean             hasPassedWayPoint                      = false;

    /**
     * Distance, which the {@link Pedestrian} has already move on its desired route. Given in
     * meters.
     */
    private float               totalDistanceOnRoute;

    /**
     * The sum of the distance between all {@link WayPoint} objects, which have been visited by a
     * {@link Pedestrian} at a specific time stamp.
     */
    private float               routeOffset;

    /**
     * Indicates the current end time of the waiting period in milliseconds and in simulated time.
     */
    private long                waitingEndTime;

    /**
     * Indicates if this {@link FollowRouteModel} is currently in the state of waiting (after having
     * passed a {@link WayPoint} with a waiting period > 0.
     */
    private boolean             isWaiting;

    /**
     * Constructor. Initializes an {@link ArrayList} and a {@link Vector2D}. Set class variables.
     *
     * @param route the route object that contains the {@link List} of {@link WayPoint} objects to
     *            be followed by this {@link FollowRouteModel}
     * @param startPosition the position of a {@link Pedestrian} when starting to move according to
     *            this {@link FollowRouteModel}
     * @param quadtree the quadtree for spatial searches
     */
    public FollowRouteModel(Route route, Vector2D startPosition, Quadtree quadtree)
    {
        super(quadtree);
        this.normalizedDirectionVector = new Vector2D(0, 0);
        this.visitedWayPoints = new ArrayList<>();
        // to avoid NullPointerExceptions, which could happen if the pedestrians are loaded before
        // the wayPoints
        if (route != null && route.getWayPoints() != null && !route.getWayPoints().isEmpty())
        {
            this.currentDestinationWayPoint = route.getWayPoints().get(0);
        }
        this.route = route;
        this.startPosition = startPosition;
    }

    /**
     * Sets the {@link #startPosition} of this Mental Model
     *
     * @param initialPosition the initial position vector
     */
    public void setStartPosition(Vector2D initialPosition)
    {
        this.startPosition = initialPosition;
    }

    /**
     * Gets the position of the last valid orientation update. Valid means either visible point on
     * {@link WayPoint#getTargetLine()} or visible point on
     * {@link WayPoint#getConnectionLineToPredecessor()} was seen from that position.
     *
     * @return the last successful orientation update position
     */
    public Vector2D getLastOrientationUpdatePostion()
    {
        return lastOrientationUpdatePostion;
    }

    /**
     * Sets the {@link WayPoint} objects of the {@link #route} of this {@link FollowRouteModel}.
     * {@link WayFindingModel#needsOrientation} is set to {@code true} so that re-orientation is
     * required.
     * <p>
     * The {@link List} of {@link #visitedWayPoints} is cleared and afterwards {@link WayPoint} that
     * on the series of {@link WayPoint}s are behind the given {@code position} are added to the
     * list of {@link #visitedWayPoints}. The {@link #currentDestinationWayPoint} will be the first
     * {@link WayPoint} in the given {@link List} of {@link WayPoint}s that is not behind the given
     * {@code position}.
     * <p>
     * If {@code wayPointList} is {@code null} the method returns immediately without any update to
     * {@link #route}
     *
     * @param route the route object that contains the {@link List} of {@link WayPoint} objects to
     *            be followed by this {@link WayFindingModel}
     * @param position the given position
     */
    public void setRoute(Route route, Vector2D position)
    {
        if (route == null || route.getWayPoints() == null || route.getWayPoints().isEmpty())
            return;

        this.route = route;

        // this computes a new normlizedDirectionVector if wayPoints are loaded
        needsOrientation = true;

        // clears the list of visited wayPoints if a new list of waypoints has been loaded
        visitedWayPoints.clear();

        Coordinate positionOnRoute = route.getPositionOnRoute(position);
        double relativePositionOnRoute = route.getRelativePositionOnRoute(position);
        for (WayPoint wayPoint : route.getWayPoints())
        {
            // evaluates to false also in case if one of the 2 values is Double.NaN
            if (wayPoint.getRelativePositionOnRoute() < relativePositionOnRoute)
            {
                visitedWayPoints.add(wayPoint);
            }
            else
            {
                currentDestinationWayPoint = wayPoint;
                break;
            }
        }
        routeOffset = 0;
        // sum up all distances between waypoints that have already been visited before starting
        for (int i = 1; i < visitedWayPoints.size(); i++ )
        {
            if (Float.isNaN(visitedWayPoints.get(i).getDistanceToPredecessor()))
                routeOffset += visitedWayPoints.get(i).getDistanceToPredecessor();
        }
        // add distance from last visited waypoint to current position on route
        if ( !Double.isNaN(relativePositionOnRoute) && relativePositionOnRoute > 0
            && relativePositionOnRoute < 1)
        {
            routeOffset += GeometryTools.distance(visitedWayPoints.get(visitedWayPoints.size() - 1),
                positionOnRoute);

        }
        logger.trace("routeOffset=" + routeOffset);
    }

    /**
     * Gets the current destination in form of a {@link WayPoint} of this {@link Pedestrian}.
     *
     * @return the current destination in form of a {@link WayPoint} of this {@link Pedestrian}.
     */
    public WayPoint getCurrentDestinationWayPoint()
    {
        return currentDestinationWayPoint;
    }

    /**
     * Computes and updates the total distance which a {@link Pedestrian} has moved, since leaving
     * it's initial position.
     *
     * @param currentPositionVector the current position of a {@link Pedestrian}
     *
     * @return the {@link Float} distance the pedestrian is moved since his initial position
     */
    private float getTotalDistanceOnRoute(Vector2D currentPositionVector)
    {
        Coordinate currentPositionCoordinate = currentPositionVector.toCoordinate();
        Coordinate lastVisitedWayCoordinate;
        if (visitedWayPoints.size() == 0)
        {
            lastVisitedWayCoordinate = startPosition.toCoordinate();
        }
        else
        {
            lastVisitedWayCoordinate = visitedWayPoints.get(visitedWayPoints.size() - 1);
        }

        float tempTotalDistanceOnRoute = 0;

        // sum of all already visited line segments
        if (visitedWayPoints.size() > 0)
        {
            for (int i = 1; i < visitedWayPoints.size(); i++ )
            {
                if (Float.isNaN(visitedWayPoints.get(i).getDistanceToPredecessor()))
                    continue;
                tempTotalDistanceOnRoute += visitedWayPoints.get(i).getDistanceToPredecessor();
            }
        }

        // part of the current segment
        if (currentDestinationWayPoint != null)
        {
            LineSegment lineSegment = new LineSegment(lastVisitedWayCoordinate,
                currentDestinationWayPoint);
            Coordinate currentPositionOnLineSegment = lineSegment
                .project(currentPositionCoordinate);

            double distanceOnCurrentSegment = GeometryTools.distance(currentPositionOnLineSegment,
                lastVisitedWayCoordinate);

            double projectionFactor = lineSegment.projectionFactor(currentPositionCoordinate);
            // These cases can happen if the position is not between the last and the next waypoint,
            // but before or behind them
            if (projectionFactor < 0)
            {
                distanceOnCurrentSegment = -distanceOnCurrentSegment;
            }
            else if (projectionFactor > 1)
            {
                distanceOnCurrentSegment = -(distanceOnCurrentSegment
                    - GeometryTools.distance(lineSegment.p0, lineSegment.p1));
            }
            tempTotalDistanceOnRoute += distanceOnCurrentSegment;
        }

        totalDistanceOnRoute = tempTotalDistanceOnRoute - routeOffset;

        logger.trace("total distance=" + totalDistanceOnRoute + ", temp distance="
            + tempTotalDistanceOnRoute + ", routeOffset=" + routeOffset);

        return totalDistanceOnRoute;
    }

    /**
     * Checks, if the {@link Pedestrian} this {@link WayFindingModel} belongs to has passed its
     * {@link #currentDestinationWayPoint}. If so, the {@link #currentDestinationWayPoint} is added
     * to the {@link List} of {@link #visitedWayPoints}} and the next {@link WayPoint} is set as
     * {@link #currentDestinationWayPoint}. After that the method
     * {@link #computeNormalizedDirectionVector(Vector2D, long)} is invoked.
     * <p>
     * In case of the {@link Pedestrian} not having passed its {@link #currentDestinationWayPoint},
     * but one of the two re-orientation parameters {@link #needsOrientation} or
     * {@link #hasCourseDeviation} is {@code true} the
     * {@link #computeNormalizedDirectionVector(Vector2D, long)} is also invoked.
     *
     * @see de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.WayFindingModel#updateNormalizedDirectionVector(Vector2D,
     *      long, float)
     */
    @Override
    public void updateNormalizedDirectionVector(Vector2D position, long time,
        float normalDesiredVelocity)
    // throws WayFindingModelNotValidException
    {
        if (time < waitingEndTime)
        {
            reset();
            isWaiting = true;
        }
        else if (isWaiting)
        {
            isWaiting = false;
            resume(position);
        }

        // if there are no waypoints return immediately
        if (currentDestinationWayPoint == null || route == null || route.getWayPoints() == null
            || route.getWayPoints().isEmpty())
        {
            return;
        }

        // updates current waypoint if a waypoint has been reached
        if (hasPassedWayPoint)
            checkNextWayPoint(position);

        // check, if the pedestrian is suspiciously slow
        if (getAverageVelocity(position, time, true) < normalDesiredVelocity / 10f
            && !isTargetVisible(position, currentDestinationWayPoint) && !needsOrientation)
        {
            needsOrientation = true;
        }

        // updates the direction, in which the Pedestrian wants to go, if needed
        if (hasPassedWayPoint || hasCourseDeviation || needsOrientation)
            computeNormalizedDirectionVector(position, time);

        if (hasPassedWayPoint)
            hasPassedWayPoint = false;

        // course should now be as required again
        if (hasCourseDeviation)
            hasCourseDeviation = false;

        return;
    }

    /**
     * Adds the {@link #currentDestinationWayPoint} to the list of visited WayPoints and checks, if
     * there is a next unvisited {@link WayPoint}. In this case the next WayPoint is set as the
     * {@link #currentDestinationWayPoint}
     */
    private void checkNextWayPoint(Vector2D position)
    {
        // TODO check if either connection to predecessor or target line of next waypoint would be
        // visible and handle and if not

        // add current destination way point to list of already visited waypoints
        visitedWayPoints.add(currentDestinationWayPoint);

        // this happens, if the pedestrian has reached the last waypoint
        if (visitedWayPoints.size() == route.getWayPoints().size())
        {
            currentDestinationWayPoint = null;
        }
        else
        {
            currentDestinationWayPoint = getNextWayPoint();
        }
    }

    /**
     * Checks if the current destination {@link WayPoint} intersects with a {@link LineString}
     * between {@code currentPosition} and {@code updatedPosition}. If this happens the
     * {@link #currentDestinationWayPoint} is assumed to be past.
     * <p>
     * If the current destination {@link WayPoint} has been passed the next {@link WayPoint} within
     * the route is searched.
     * <p>
     * It is also checked, if the current destination has a waiting time.
     *
     * @param time the current simulated time (in ms)
     * @param currentPosition the current position of the {@link WayFindingModel}
     * @param updatedPosition an updated position of the {@link WayFindingModel}
     */
    private void checkWayPointPassing(long time, Vector2D currentPosition, Vector2D updatedPosition)
    {
        if (currentDestinationWayPoint == null)
            return;
        Geometry geometryForChecking = currentDestinationWayPoint.getPassingArea();
        if (geometryForChecking == null)
            geometryForChecking = currentDestinationWayPoint.getTargetLine();
        if (geometryForChecking == null)
        {
            Envelope alternativeWayPointBoundingBox = new Envelope(currentDestinationWayPoint);
            alternativeWayPointBoundingBox.expandBy(currentDestinationWayPoint.getWidth() <= 0
                ? WayPoint.DEFAULT_WIDTH : currentDestinationWayPoint.getWidth());
            // check if the current WayPoint has been passed
            hasPassedWayPoint = NumericIntegrationTools.moveInvadesBoundingBox(
                alternativeWayPointBoundingBox, currentPosition, updatedPosition);
        }
        else
        {
            // check if the current WayPoint has been passed
            hasPassedWayPoint = NumericIntegrationTools.moveIntersectsGeometry(
                currentDestinationWayPoint.getBoundingBox(), geometryForChecking, currentPosition,
                updatedPosition);
        }
        checkWaiting(time);
    }

    /**
     * Checks if a {@link WayPoint} has been passed and if so sets the {@link #waitingEndTime}
     *
     * @param time the current time in simulated time
     */
    protected void checkWaiting(long time)
    {
        if (hasPassedWayPoint && currentDestinationWayPoint.getWaitingPeriod() > 0)
        {
            setWaitingEndTime(time + currentDestinationWayPoint.getWaitingPeriod());
            hasPassedWayPoint = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateModel(long time, Vector2D currentPosition, Vector2D updatedPosition)
    {
        checkWayPointPassing(time, currentPosition, updatedPosition);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkCourse(Pedestrian pedestrian, long timestamp)
    {
        // only check current course after a certain time/distance
        boolean needsCourseUpdate = false;
        try
        {
            needsCourseUpdate = timestamp - lastCourseUpdateTime > COURSE_UPDATE_INTERVAL
                || Math.abs(lastCourseUpdatePostion.getX()
                    - pedestrian.getCurrentPositionVector().getX()) > COURSE_UPDATE_DISTANCE
                || Math.abs(lastCourseUpdatePostion.getY()
                    - pedestrian.getCurrentPositionVector().getY()) > COURSE_UPDATE_DISTANCE;
        }
        // can happen, if lastCourseUpdatePostion is null (this is the case, if no waypoints are
        // set)
        catch (NullPointerException e)
        {
            needsCourseUpdate = false;
        }

        if (needsCourseUpdate)
        {
            // if there is no normalized direction vector (e.g. when Pedestrian is lost or when
            // simulation has finished), no angle can be computed
            if (normalizedDirectionVector != null
                && !MathTools.isZeroVector(normalizedDirectionVector) && targetPosition != null)
            {
                Vector2D requiredDirectionVector = targetPosition
                    .subtract(pedestrian.getCurrentPositionVector());
                if ( !MathTools.isZeroVector(requiredDirectionVector))
                {
                    double actualCourse = MathTools.angle(normalizedDirectionVector);
                    double requiredCourse = MathTools.angle(requiredDirectionVector);

                    // check of the difference between the actual course and the required course to
                    // reach the current targetCoordinate is above threshold
                    if (Math.abs(actualCourse - requiredCourse) > MAXIMUM_COURSE_DEVIATION)
                    {
                        hasCourseDeviation = true;
                        logger.trace("FollowWayPointsMentalModel.checkCourse(), deltaCourse="
                            + (actualCourse - requiredCourse));
                    }
                }
            }
        }
    }

    /**
     * Computes and updates the {@link Vector2D}, which is the normalized (i.e. length=1) target
     * vector of the {@link Pedestrian}, i.e. this vector indicates, in which direction this
     * Pedestrian currently wants to go. Usually this vector will point in the direction of the
     * {@link #currentDestinationWayPoint}.
     *
     * @param position for which the normalized direction vector should be updated
     * @param timestamp time when this method is called
     */
    private void computeNormalizedDirectionVector(Vector2D position, long timestamp)
    // throws WayFindingModelNotValidException
    {
        // if current destination waypoint is null set target vector to (0, 0)
        if (currentDestinationWayPoint == null)
        {
            resetTargetVectors();
            return;
        }

        Coordinate tempTargetCoordinate = null;
        // check, if the Pedestrian can still see its current target and re-use it
        if ((hasCourseDeviation || needsOrientation) && !hasPassedWayPoint)
        {
            // avoid null pointer exceptions at the beginning
            if (targetPosition != null)
            {
                Coordinate currentTargetCoordinate = targetPosition.toCoordinate();

                // always re-use the current target if not a certain distance or time has been
                // passed since the last check
                if (Math.abs(
                    lastOrientationUpdatePostion.getX() - position.getX()) < COURSE_UPDATE_DISTANCE
                    && Math.abs(lastOrientationUpdatePostion.getY()
                        - position.getY()) < COURSE_UPDATE_DISTANCE
                    && timestamp - lastOrientationUpdateTime < COURSE_UPDATE_INTERVAL)
                {
                    tempTargetCoordinate = currentTargetCoordinate;
                }
                // after a certain time or distance, check again, if target is still visible
                else if (isTargetVisible(position, currentTargetCoordinate))
                {
                    tempTargetCoordinate = currentTargetCoordinate;
                }
            }
        }

        // if the existing target point could not be re-used (since it is not seen anymore or it has
        // been passed) or if the the pedestrian needs orientation and a certain distance / certain
        // time has been passed since the last check (whichever condition is fulfilled first)
        if (tempTargetCoordinate == null
            || (needsOrientation && (timestamp - lastOrientationUpdateTime > COURSE_UPDATE_INTERVAL
                || Math.abs(
                    lastOrientationUpdatePostion.getX() - position.getX()) > COURSE_UPDATE_DISTANCE
                || Math.abs(lastOrientationUpdatePostion.getY()
                    - position.getY()) > COURSE_UPDATE_DISTANCE)))
        {
            boolean isSuccesfulOrientation = true;
            Coordinate currentPositionCoordinate = position.toCoordinate();

            // get nearest Coordinate on target line of WayPoint from pedestrian position
            Coordinate nearestCoordinateOnTargetLine = currentDestinationWayPoint
                .getTargetLine() == null ? currentDestinationWayPoint
                    : GeometryTools.getNearestCoordinateOnGeometry(currentPositionCoordinate,
                        currentDestinationWayPoint.getTargetLine());

            // checks if the nearestCoordinateOnWayPoint is visible for the pedestrian
            if (isTargetVisible(position, nearestCoordinateOnTargetLine))
            {
                tempTargetCoordinate = nearestCoordinateOnTargetLine;
                needsOrientation = false;
            }
            else
            {
                // check if an alternative point on the waypoint target line is visible (if the
                // nearest point on the target line was not visible)
                Coordinate alternativeOnTargetLine = getAlternativeOnTargetLine(position);
                if (alternativeOnTargetLine == null)
                {
                    needsOrientation = true;

                    // check if an alternative point on the connection line to the preceding
                    // waypoint is visible and try to head to there
                    tempTargetCoordinate = getAlternativeOnConnectionLineToPredecessor(position);

                    // if no such point could be found
                    if (tempTargetCoordinate == null)
                    {
                        // test, if the last successful orientation update position is visible
                        if (lastOrientationUpdatePostion != null && isTargetVisible(position,
                            lastOrientationUpdatePostion.toCoordinate()))
                        {
                            tempTargetCoordinate = lastOrientationUpdatePostion.toCoordinate();
                            isSuccesfulOrientation = false;
                        }
                        else
                        {
                            // TODO: in this case the Pedestrian is completely lost...
                            // throw new WayFindingModelNotValidException(
                            // "The pedestrian cannot find the WayPoint on its own anymore");
                        }
                    }
                }
                else
                {
                    tempTargetCoordinate = alternativeOnTargetLine;
                    needsOrientation = false;
                }
            }
            if (isSuccesfulOrientation)
            {
                if (timestamp != getStartTime())
                    lastOrientationUpdateTime = timestamp;
                lastOrientationUpdatePostion = position;
            }
        }
        if (tempTargetCoordinate != null)
        {
            targetPosition = new Vector2D(tempTargetCoordinate.x, tempTargetCoordinate.y);

            // compute direction vector between position and targetPosition and normalize length to
            // 1
            updateNormalizedDirectionVector(position);

            // save time and position of last course update
            lastCourseUpdatePostion = position;
            lastCourseUpdateTime = timestamp;
        }
        else
        {
            targetPosition = null;
            normalizedDirectionVector = new Vector2D(0, 0);
        }
    }

    /**
     * Sets {@link #getTargetPosition()} to {@code null}, {@link #getNormalizedDirectionVector()} to
     * a zero {@link Vector} and {@link #needsOrientation()} to false.
     */
    private void resetTargetVectors()
    {
        targetPosition = null;
        normalizedDirectionVector = new Vector2D(0, 0);
        needsOrientation = false;
    }

    /**
     * Sets the {@link #currentDestinationWayPoint} to {@code null} and calls
     * {@link #resetTargetVectors()}
     */
    protected void reset()
    {
        this.currentDestinationWayPoint = null;
        resetTargetVectors();
    }

    /**
     * Calls {@link #setRoute(Route, Vector2D)} at given position.
     *
     * @param position the position
     */
    protected void resume(Vector2D position)
    {
        setRoute(route, position);
    }

    /**
     * Sets the time when the waiting period of this {@link FollowRouteModel} will end.
     *
     * @param waitingEndTime the time when the waiting period of this {@link FollowRouteModel} will
     *            end. Given in milliseconds and in simulation time.
     */
    private void setWaitingEndTime(long waitingEndTime)
    {
        this.waitingEndTime = waitingEndTime;
    }

    /**
     * If this {@link Pedestrian} does not see its current target, this method tries to find an
     * alternative {@link WayPoint} which is seen by the {@link Pedestrian}s.
     * <p>
     * For this purpose of the {@link WayPoint#getTargetLine()} are divided into a specific amount
     * of parts, denoted by the {@link #SEARCH_RESOLUTION_ALTERNATIVE_POSITION}}. For all this parts
     * a coordinate is generated, where the method tests if the {@link Pedestrian} can see it. If
     * the {@link Pedestrian} sees more than one, the distance between them will be computed and the
     * nearest to {@code currentPositionVector} is returned.
     *
     * @param currentPositionVector the current position of this {@link Pedestrian}
     *
     * @return {@link Coordinate} on the {@link WayPoint#getTargetLine()} which is visible for the
     *         {@link Pedestrian}
     */
    private Coordinate getAlternativeOnTargetLine(Vector2D currentPositionVector)
    {
        if (currentDestinationWayPoint.getTargetLine() == null)
            return currentDestinationWayPoint;
        Map<Coordinate, Double> candidatePointsAndDistances = new HashMap<>();
        Coordinate pedestrianPosition = currentPositionVector.toCoordinate();
        Coordinate coordinateOnTargetLine = null;

        if (currentDestinationWayPoint.getTargetLine() instanceof LineString)
        {
            LineString lineString = (LineString) currentDestinationWayPoint.getTargetLine();
            Coordinate startingCoordinate = new Coordinate(lineString.getStartPoint().getX(),
                lineString.getStartPoint().getY());
            Coordinate endingCoordinate = new Coordinate(lineString.getEndPoint().getX(),
                lineString.getEndPoint().getY());
            LineSegment lineSegment = new LineSegment(startingCoordinate, endingCoordinate);

            // counts in how much parts a line will be split for searching for an alternative
            // waypoint
            double maxIterations = lineSegment.getLength() / SEARCH_RESOLUTION_ALTERNATIVE_POSITION;

            candidatePointsAndDistances = getCoordinatesOnLineSegment(lineSegment,
                pedestrianPosition, maxIterations, true);
        }
        else if (currentDestinationWayPoint.getTargetLine() instanceof MultiLineString)
        {
            int count = currentDestinationWayPoint.getTargetLine().getNumGeometries();

            for (int i = 0; i < count; i++ )
            {
                LineString lineString = (LineString) currentDestinationWayPoint.getTargetLine()
                    .getGeometryN(i);
                Coordinate startingCoordinate = new Coordinate(lineString.getStartPoint().getX(),
                    lineString.getStartPoint().getY());
                Coordinate endingCoordinate = new Coordinate(lineString.getEndPoint().getX(),
                    lineString.getEndPoint().getY());
                LineSegment lineSegment = new LineSegment(startingCoordinate, endingCoordinate);

                // counts in how much parts a line will be split for searching for an alternative
                // waypoint
                double maxIterations = lineSegment.getLength()
                    / SEARCH_RESOLUTION_ALTERNATIVE_POSITION;

                Map<Coordinate, Double> alternativePointsOnMultiLineSegments = getCoordinatesOnLineSegment(
                    lineSegment, pedestrianPosition, maxIterations, true);

                for (Entry<Coordinate, Double> alternativePoint : alternativePointsOnMultiLineSegments
                    .entrySet())
                {
                    candidatePointsAndDistances.put(alternativePoint.getKey(),
                        alternativePoint.getValue());
                }
            }
        }

        List<Map.Entry<Coordinate, Double>> sortedListOfCandidatePoints = sortMap(
            candidatePointsAndDistances);

        if ( !sortedListOfCandidatePoints.isEmpty())
        {
            coordinateOnTargetLine = new Coordinate(sortedListOfCandidatePoints.get(0).getKey());
        }

        return coordinateOnTargetLine;
    }

    /**
     * Gets a {@link Coordinate} on the {@link WayPoint#getConnectionLineToPredecessor()} that is
     * visible and as close as possible to the next {@link WayPoint}
     *
     * @param position the position to start searching
     * @return a {@link Coordinate} on the connection line to the preceding {@link WayPoint} that is
     *         as close as possible to the next {@link WayPoint} or {@code null}, if no such
     *         {@link Coordinate} could be found.
     */
    private Coordinate getAlternativeOnConnectionLineToPredecessor(Vector2D position)
    {
        LineSegment ls = null;
        Coordinate coordinateOnConnectionLineToPredecessor = null;
        // fallback to connection line between start position and first WayPoint - this should only
        // happen in front of the first waypoint, because there is no connection line between
        // WayPoints already
        if (currentDestinationWayPoint.getConnectionLineToPredecessor() == null)
        {
            ls = new LineSegment(currentDestinationWayPoint, startPosition.toCoordinate());
        }
        // connection line to predecessor
        else
        {
            LineString connectionLineToPredecessor = currentDestinationWayPoint
                .getConnectionLineToPredecessor();
            ls = new LineSegment(
                new Coordinate(connectionLineToPredecessor.getStartPoint().getX(),
                    connectionLineToPredecessor.getStartPoint().getY()),
                new Coordinate(connectionLineToPredecessor.getEndPoint().getX(),
                    connectionLineToPredecessor.getEndPoint().getY()));
        }
        // double maxIterations = ls == null ? 0
        // : ls.getLength() / searchResolutionAlternativePosition;
        double maxIterations = ls.getLength() / SEARCH_RESOLUTION_ALTERNATIVE_POSITION;
        Map<Coordinate, Double> candidatePointsAndDistances = getCoordinatesOnLineSegment(ls,
            position.toCoordinate(), maxIterations, false);
        List<Map.Entry<Coordinate, Double>> sortedListOfCandidatePoints = sortMap(
            candidatePointsAndDistances);
        if (candidatePointsAndDistances != null && !candidatePointsAndDistances.isEmpty())
        {
            coordinateOnConnectionLineToPredecessor = new Coordinate(
                sortedListOfCandidatePoints.get(0).getKey());
        }

        return coordinateOnConnectionLineToPredecessor;
    }

    /**
     * Computes a Map of {@link Coordinate}s and associated distances from the given
     * {@code pedestrianPosition} to some interpolated {@link Coordinate}s on the given
     * {@code lineSegment}.
     * <p>
     *
     * @param lineSegment the line segment to be used to interpolate Coordinates
     * @param pedestrianPosition describes the current position of a {@link Pedestrian}
     * @param maxIterations counts the number of segments in which the {@link LineSegment} is split
     *            for the search for an alternative wayPoint.
     * @param distanceOriginPosition if {@code true} the distance between {@code pedestrianPosition}
     *            and the interpolated {@link Coordinate}s are computed, otherwise the distance
     *            between the start point of the segment and the interpolated Coordinate is computed
     *
     * @return the Map of interpolated {@link Coordinate}s and associated distances.
     */
    private Map<Coordinate, Double> getCoordinatesOnLineSegment(LineSegment lineSegment,
        Coordinate pedestrianPosition, double maxIterations, boolean distanceOriginPosition)
    {
        if (lineSegment == null)
            return null;
        Map<Coordinate, Double> candidatePointsAndDistances = new HashMap<>();
        Coordinate coordinateOnLineSegment;
        double distance = 0d;

        double lengthOfLineSegment = lineSegment.getLength();
        for (int i = 0; i <= maxIterations; i++ )
        {
            // creates values between 0 and 1 (including 0 and 1) which divides the lineSegment in
            // specific number of equal parts of equal length
            double incrementOnLineSegment = i * SEARCH_RESOLUTION_ALTERNATIVE_POSITION
                / lengthOfLineSegment;
            // ensures that incrementOnLineSegment = 1 exist (otherwise it will be 0,9x)
            if (i == (int) maxIterations)
                incrementOnLineSegment = 1;

            coordinateOnLineSegment = lineSegment.pointAlong(incrementOnLineSegment);
            LineString lineOfSight = JTSFactoryFinder.getGeometryFactory()
                .createLineString(new Coordinate[] { pedestrianPosition, coordinateOnLineSegment });

            // only put into candidate points, if there is no intersection with boundaries
            // (if boundaries exist)
            List<BoundarySegment> boundaries = quadtree
                .getBoundarySegments(lineOfSight.getEnvelopeInternal());
            if (boundaries == null || boundaries.isEmpty())
            {
                if (distanceOriginPosition)
                    distance = GeometryTools.distance(coordinateOnLineSegment, pedestrianPosition);
                else
                    distance = GeometryTools.distance(coordinateOnLineSegment, lineSegment.p0);
                candidatePointsAndDistances.put(coordinateOnLineSegment, distance);
                if ( !distanceOriginPosition)
                    break;
            }
            else
            {
                boolean lineOfSightIntersectBoundaries = false;
                for (BoundarySegment boundary : boundaries)
                {
                    lineOfSightIntersectBoundaries = lineOfSight.intersects(boundary.getGeometry());
                    if (lineOfSightIntersectBoundaries)
                        break;
                }
                if ( !lineOfSightIntersectBoundaries)
                {
                    if (distanceOriginPosition)
                        distance = GeometryTools.distance(coordinateOnLineSegment,
                            pedestrianPosition);
                    else
                        distance = GeometryTools.distance(coordinateOnLineSegment, lineSegment.p0);
                    candidatePointsAndDistances.put(coordinateOnLineSegment, distance);
                    if ( !distanceOriginPosition)
                        break;
                }
            }
        }

        return candidatePointsAndDistances;
    }

    /**
     * Gets the next {@link WayPoint}, which is not already contained in {@link #visitedWayPoints}
     *
     * @return the next {@link WayPoint}, which is not already contained in
     *         {@link #visitedWayPoints} or {@code null}, if either no {@link WayPoint} exists or
     *         all {@link WayPoint} are already contained in {@link #visitedWayPoints}
     */
    private WayPoint getNextWayPoint()
    {
        List<WayPoint> wayPoints = route.getWayPoints();
        if (wayPoints != null && !wayPoints.isEmpty())
        {
            for (WayPoint wayPoint : wayPoints)
            {
                if ( !visitedWayPoints.contains(wayPoint))
                {
                    return wayPoint;
                }
            }
            return null;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getAverageVelocity(Vector2D position, long currentTime, boolean useCache)
    {
        float distance = useCache ? totalDistanceOnRoute : getTotalDistanceOnRoute(position);
        float timeSpan = (currentTime - getStartTime()) / 1000f;
        float velocity = distance / timeSpan;
        logger.trace("distance=" + distance + ", offset=" + routeOffset + ", velocity=" + velocity);
        return velocity;
    }

    /**
     * Sorts the {@link Coordinate}s in this {@link Map} depending on the smallest {@link Double}
     * value.
     *
     * @param map contains all {@link Coordinate}s, as key, and associated distances to a
     *            {@link Pedestrian}.
     *
     * @return a new map, with the same declaration, which is sorted by the {@link Double} value
     */
    private static List<Map.Entry<Coordinate, Double>> sortMap(Map<Coordinate, Double> map)
    {
        if (map == null)
            return null;
        List<Map.Entry<Coordinate, Double>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Coordinate, Double>>()
            {
                @Override
                public int compare(Map.Entry<Coordinate, Double> pointA,
                    Map.Entry<Coordinate, Double> pointB)
                {
                    return (pointA.getValue()).compareTo(pointB.getValue());
                }
            });
        return list;
    }
}
