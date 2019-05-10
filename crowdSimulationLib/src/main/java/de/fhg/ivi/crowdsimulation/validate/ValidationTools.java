package de.fhg.ivi.crowdsimulation.validate;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.math.Vector2D;

import de.fhg.ivi.crowdsimulation.CrowdSimulatorNotValidException;
import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.boundaries.BoundarySegment;
import de.fhg.ivi.crowdsimulation.crowd.Crowd;
import de.fhg.ivi.crowdsimulation.crowd.ICrowd;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.FollowRouteModel;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.Route;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.WayPoint;
import de.fhg.ivi.crowdsimulation.geom.Quadtree;

/**
 * Contains methods to validate the pre-conditions for crowd simulations
 *
 * @author hahmann
 *
 */
public class ValidationTools
{

    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger logger = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());

    /**
     * First the method checks whether all necessary data, i.e. {@link WayPoint}s,
     * {@link Pedestrian}s and {@link Boundary}s are loaded.
     * <p>
     * Then it will be checked that the initial position of a {@link Pedestrian}s don't intersect
     * the initial position of any other {@link Pedestrian}, regardless of which {@link Crowd} the
     * {@link Pedestrian} is a part of. Nearly the same is checked in case of the {@link Pedestrian}
     * id. Here the id of a {@link Pedestrian} must be unique over all {@link Pedestrian}s of all
     * {@link Crowd}s.
     * <p>
     * Then this method checks two preconditions for a valid crowd simulator: first one is that the
     * all {@link Pedestrian}s must see the first {@link WayPoint}. The second one includes that
     * every {@link WayPoint} can see the {@link WayPoint} which lies next to it.
     * <p>
     *
     * @param crowdsToCheck the list of {@link Crowd} objects to be validated
     * @param boundariesToCheck the list of {@link Boundary} object to be validated
     *
     * @return {@code true} if all preconditions are fulfilled and all dates are loaded. Otherwise
     *         {@code false}.
     *
     * @throws CrowdSimulatorNotValidException, if one ore more preconditions are not fulfilled
     */
    public static boolean validate(List<? extends ICrowd> crowdsToCheck,
        List<Boundary> boundariesToCheck) throws CrowdSimulatorNotValidException
    {
        // checks that there are no intersection between initial start positions of all pedestrians
        // in all crowds
        boolean distinctPedestrianPositions = checkDistinctPedestrianPositions(crowdsToCheck);

        // checks that pedestrian id's are unique over all crowds
        boolean uniquePedestrianIds = checkUniquePedestrianIds(crowdsToCheck);

        // checks whether the first precondition is fulfilled
        boolean pedestrianWayPointVisibility = true;

        // checks whether the second precondition is fulfilled
        boolean interWayPointVisibility = true;

        for (ICrowd crowdToCheck : crowdsToCheck)
        {
            Route route = crowdToCheck.getRoute();
            List<WayPoint> wayPointsToCheck = null;
            if (route != null)
                wayPointsToCheck = route.getWayPoints();

            pedestrianWayPointVisibility |= checkPedestrianWayPointVisibility(crowdToCheck,
                wayPointsToCheck, boundariesToCheck);

            interWayPointVisibility |= checkInterWayPointVisibility(wayPointsToCheck,
                boundariesToCheck);
        }

        logger.debug("validate(), uniquePedestrianIds=" + uniquePedestrianIds
            + ", distinctPedestrianPositions=" + distinctPedestrianPositions
            + ", pedestrian-waypoint-visibility=" + pedestrianWayPointVisibility
            + ", inter-waypoint-visibility=" + interWayPointVisibility);

        // TODO: this solution is a little bit inconvenient for users:
        // in the case that more than condition is not fulfilled the user will only be informed
        // about one unfulfilled conditions

        // if one of the preconditions isn't fulfilled the validation failed
        return pedestrianWayPointVisibility & interWayPointVisibility & distinctPedestrianPositions
            & uniquePedestrianIds;
    }

    /**
     * Checks whether the all {@link Pedestrian} of all given crowds ({@code crowdsToCheck} have
     * distinct position, i.e. there is no set of two (different) Pedestrians that are exactly at
     * the same position.
     *
     * @param crowdsToCheck {@link List} of {@link Crowd} objects to be checked
     * @return {@code true} if all {@link Pedestrian}s are at distinct positions. Also returns
     *         {@code true}, if crowdsToCheck is {@code null} or empty.
     * @throws CrowdSimulatorNotValidException if there is at least one set of initial positions of
     *             two pedestrians that intersect each other
     */
    public static boolean checkDistinctPedestrianPositions(List<? extends ICrowd> crowdsToCheck)
        throws CrowdSimulatorNotValidException
    {
        if (crowdsToCheck == null || crowdsToCheck.isEmpty())
            return true;
        // Two loops over crowds to compare every crowd with every crowd
        for (ICrowd crowd : crowdsToCheck)
        {
            for (Pedestrian pedestrian : crowd.getPedestrians())
            {
                for (ICrowd otherCrowd : crowdsToCheck)
                {
                    for (Pedestrian otherPedestrian : otherCrowd.getPedestrians())
                    {
                        // if the pedestrian is compared with itself coordinate are allowed to be
                        // identical
                        if (pedestrian.equals(otherPedestrian))
                        {
                            continue;
                        }
                        Vector2D pedestrianPosition = pedestrian.getCurrentPositionVector();
                        Vector2D otherPedestrianPosition = otherPedestrian
                            .getCurrentPositionVector();
                        if (pedestrianPosition.equals(otherPedestrianPosition))
                        {
                            throw new CrowdSimulatorNotValidException(
                                "At least 2 pedestrians are at the same initial positions, which is not allowed");
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Checks whether the id the {@link Pedestrian}s of contained in all {@link Crowd}s in the given
     * list of crowds ({@code crowdsToCheck} are unique. If there is at least one pair of
     * pedestrians to have a common id an exception is thrown.
     *
     * @param crowdsToCheck {@link List} of all {@link Crowd} object to be checked
     * @return {@code true} if all {@link Pedestrian}s ids are unique. Also returns {@code true}, if
     *         crowdsToCheck is {@code null} or empty.
     * @throws CrowdSimulatorNotValidException if pedestrian ids are non-unique within all crowds
     */
    public static boolean checkUniquePedestrianIds(List<? extends ICrowd> crowdsToCheck)
        throws CrowdSimulatorNotValidException
    {
        if (crowdsToCheck == null || crowdsToCheck.isEmpty())
            return true;
        // Two loops over crowds to compare every crowd with every crowd
        for (ICrowd crowd : crowdsToCheck)
        {
            for (Pedestrian pedestrian : crowd.getPedestrians())
            {
                int numberOfPedestriansWithSameId = 0;
                for (ICrowd otherCrowd : crowdsToCheck)
                {
                    for (Pedestrian otherPedestrian : otherCrowd.getPedestrians())
                    {
                        if (pedestrian.getId() == otherPedestrian.getId())
                            numberOfPedestriansWithSameId++ ;
                    }
                }
                if (numberOfPedestriansWithSameId > 1)
                {
                    throw new CrowdSimulatorNotValidException(
                        "There are at least 2 pedestrians that have the same id. ids of pedestrians have to be unique.");
                }
            }
        }
        return true;
    }

    /**
     * Checks if all {@link Pedestrian}s contained in {@code crowdToCheck} can see their first
     * {@link WayPoint}.
     *
     * @param crowdToCheck the {@link Crowd} object to be validated
     * @param wayPointsGeometriesToCheck the list of {@link Geometry} objects representing the
     *            {@link WayPoint}s to be validated
     * @param boundariesToCheck the list of {@link Boundary} object to be validated
     *
     * @return {@code true} if all {@link Pedestrian} can see their first {@link WayPoint}. If
     *         either {@code wayPointsToCheck} or {@code boundariesToCheck} or {@code crowdsToCheck}
     *         are {@code null} or empty {@code true} will be returned.
     *
     * @throws CrowdSimulatorNotValidException, if the precondition is not fulfilled
     */
    public static boolean checkPedestrianWayPointVisibility(ICrowd crowdToCheck,
        List<WayPoint> wayPointsGeometriesToCheck, List<Boundary> boundariesToCheck)
        throws CrowdSimulatorNotValidException
    {
        if (crowdToCheck == null || wayPointsGeometriesToCheck == null
            || wayPointsGeometriesToCheck.isEmpty() || boundariesToCheck == null
            || boundariesToCheck.isEmpty())
            return true;
        if (crowdToCheck.getPedestrians() == null || crowdToCheck.getPedestrians().isEmpty())
            return true;

        Quadtree tempQuadtree = new Quadtree();
        tempQuadtree.addBoundaries(boundariesToCheck);

        Route route = new Route(wayPointsGeometriesToCheck);
        for (Pedestrian pedestrian : crowdToCheck.getPedestrians())
        {
            if (pedestrian.getActiveWayFindingModel() instanceof FollowRouteModel)
            {
                Coordinate positionOfFirstWayPoint = route
                    .getNearestWayPointOnRoute(pedestrian.getCurrentPositionVector());

                Coordinate pedestrianPosition = pedestrian.getCurrentPositionCoordinate();
                Coordinate[] coordinates = new Coordinate[] { pedestrianPosition,
                    positionOfFirstWayPoint };
                LineString lineString = JTSFactoryFinder.getGeometryFactory()
                    .createLineString(coordinates);
                List<BoundarySegment> relevantBoundaries = tempQuadtree
                    .getBoundarySegments(lineString.getEnvelopeInternal());
                boolean visible = true;

                for (BoundarySegment boundary : relevantBoundaries)
                {
                    visible &= !lineString.intersects(boundary.getGeometry());
                    if ( !visible)
                        break;
                }
                if ( !visible)
                {
                    throw new CrowdSimulatorNotValidException(
                        "Not all Pedestrians see the first WayPoint, i.e. there is at least one Pedestrian, which has a boundary object on the direct line of sight between itself and the first WayPoint. The input data needs to be changed so that this condition is fulfilled.");
                }
            }
        }
        return true;
    }

    /**
     * Checks if each {@link WayPoint} can see its following {@link WayPoint}.
     *
     * @param wayPointsToCheck the list of {@link Geometry} objects representing the
     *            {@link WayPoint}s to be validated
     * @param boundariesToCheck the list of {@link Boundary} object to be validated
     *
     * @return {@code true} if every {@link WayPoint} can see the {@link WayPoint} which lying next
     *         to it. If either {@code wayPointsToCheck} or {@code boundariesToCheck} are
     *         {@code null} or empty {@code true} will be returned.
     *
     * @throws CrowdSimulatorNotValidException, if the precondition is not fulfilled
     */
    public static boolean checkInterWayPointVisibility(List<WayPoint> wayPointsToCheck,
        List<Boundary> boundariesToCheck) throws CrowdSimulatorNotValidException
    {
        if (wayPointsToCheck == null || wayPointsToCheck.isEmpty() || boundariesToCheck == null
            || boundariesToCheck.isEmpty())
            return true;
        GeometryFactory factory = JTSFactoryFinder.getGeometryFactory();

        Quadtree tempQuadtree = new Quadtree();
        tempQuadtree.addBoundaries(boundariesToCheck);

        for (int i = 0; i < (wayPointsToCheck.size() - 1); i++ )
        {
            Coordinate firstPoint = wayPointsToCheck.get(i);
            Coordinate secondPoint = wayPointsToCheck.get(i + 1);
            Coordinate[] coordinates = new Coordinate[] { firstPoint, secondPoint };

            LineString lineString = factory.createLineString(coordinates);

            boolean visible = true;
            List<BoundarySegment> relevantBoundaries = tempQuadtree
                .getBoundarySegments(lineString.getEnvelopeInternal());
            for (BoundarySegment boundary : relevantBoundaries)
            {
                visible &= !lineString.intersects(boundary.getGeometry());
                if ( !visible)
                    break;
            }
            if ( !visible)
            {
                throw new CrowdSimulatorNotValidException(
                    "Not every waypoint can see its neighboring waypoint, i.e. there is at least one pair of consecutive WayPoints that have a boundary element on the direct line of sight between them. The input data needs to be changed so that this condition is fulfilled.");
            }
        }
        return true;
    }
}
