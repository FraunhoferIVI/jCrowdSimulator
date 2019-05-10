package de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.math.Vector2D;

import de.fhg.ivi.crowdsimulation.geom.GeometryTools;

/**
 * A {@link Route} encapsulates a {@link List} of {@link WayPoint} objects and the
 * {@link LineString} representation of that sequence of {@link Coordinate} objects
 *
 * @author hahmann
 *
 */
public class Route
{
    /**
     * Uses the object logger for printing specific messages in the console.
     */
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The {@link List} of {@link WayPoint} objects this route consists of
     */
    private List<WayPoint>      wayPoints;

    /**
     * The {@link LineString} representation of the sequence of {@link WayPoint} objects
     */
    private LineString          geometry;

    /**
     * The Bounding Box of this Route, which is the union of all bounding boxes of the
     * {@link WayPoint} objects (also considering the {@link WayPoint#getPassingArea()} belonging to
     * this {@link Route}
     */
    private Envelope            boundingBox;

    /**
     * Creates a new {@link Route} object and calls
     * {@link WayPoint#setRelativePositionOnRoute(double)} for all {@link WayPoint} objects that are
     * part of this {@link Route}
     *
     * @param wayPoints the {@link List} of {@link WayPoint} objects this route consists of
     */
    public Route(List<WayPoint> wayPoints)
    {
        setWayPoints(wayPoints);
    }

    /**
     * Gets the {@link LineString} representation of the sequence of {@link WayPoint} objects
     *
     * @return the {@link LineString} representation of the sequence of {@link WayPoint} objects
     */
    public LineString getGeometry()
    {
        return geometry;
    }

    /**
     * Updates {@link #wayPoints} and calls {@link WayPoint#setRelativePositionOnRoute(double)} for
     * all {@link WayPoint} objects that are part of this {@link Route}
     *
     * @param wayPoints the updated {@link List} of {@link WayPoint} objects this route consists of
     */
    public void setWayPoints(List<WayPoint> wayPoints)
    {
        this.wayPoints = wayPoints;
        if (this.wayPoints != null && !this.wayPoints.isEmpty())
        {
            // creates a new route linestring containing all waypoints of the route
            geometry = GeometryTools.createLineStringFromCoordinates(wayPoints);
            for (WayPoint wayPoint : wayPoints)
            {
                wayPoint.setRelativePositionOnRoute(
                    GeometryTools.getRelativePositionOnLineString(geometry, wayPoint));
            }
        }
    }

    /**
     * Gets the {@link List} of {@link WayPoint} objects this route consists of
     *
     * @return the {@link List} of {@link WayPoint} objects this route consists of
     */
    public List<WayPoint> getWayPoints()
    {
        return wayPoints;
    }

    /**
     * Gets the nearest {@link WayPoint} (out of a {@link List} of {@link WayPoint} objects
     * belonging to the current route) in relation to a given position.
     *
     * @param position a given position
     *
     * @return the first {@link WayPoint} on the route from the viewpoint of the {@code position}
     */
    public WayPoint getNearestWayPointOnRoute(Vector2D position)
    {
        Coordinate positionOnRoute = GeometryTools
            .getNearestCoordinateOnGeometry(position.toCoordinate(), geometry);
        double relativePositionOnRoute = GeometryTools.getRelativePositionOnLineString(geometry,
            positionOnRoute);
        for (WayPoint wayPoint : wayPoints)
        {
            // evaluates to false also in case if one of the 2 values is Double.NaN
            if ( !(wayPoint.getRelativePositionOnRoute() < relativePositionOnRoute))
                return wayPoint;
        }

        return null;
    }

    /**
     * Calculates a {@link Coordinate} on this {@link Route} object that has the shortest distance
     * shortest distance between the given {@link Coordinate} and this {@link Route}. <b>Due to
     * rounding errors the resulting {@link Coordinate} is NOT guaranteed to intersect with the
     * {@link LineString} representation of this {@link Route}</b>
     *
     * @param position a {@link Vector2D} which describes a x, y position
     *
     * @return a {@link Coordinate} representing the position on the sequence of {@link WayPoint}
     *         objects of this {@link Route} that is nearest to {@code coordinate} or {@code null},
     *         if {@code position} is {@code null}
     */
    public Coordinate getPositionOnRoute(Vector2D position)
    {
        return GeometryTools.getNearestCoordinateOnGeometry(position.toCoordinate(), geometry);
    }

    /**
     * Gets a relative position (between 0 and 1) of {@code position} on the sequence of
     * {@link WayPoint} objects of this {@link Route} under the assumption that the given
     * {@link Coordinate} exactly lies on the {@link LineString}. If this assumption is not
     * fulfilled {@link Double#NaN} will be returned.
     *
     * @param position the {@link Coordinate} assumed to be on the {@link LineString}
     * @return a relative position (between 0 and 1) of the given {@link Vector2D} on the
     *         {@link LineString} representation of this route (if the given {@link Vector2D} is on
     *         the {@link LineString}) or {@link Double#NaN}, if this assumption is not fulfilled.
     */
    public double getRelativePositionOnRoute(Vector2D position)
    {
        return GeometryTools.getRelativePositionOnLineString(geometry,
            getPositionOnRoute(position));
    }

    /**
     * Gets the Bounding Box of this Route, which is the union of all bounding boxes of the
     * {@link WayPoint} object (also considering the {@link WayPoint#getPassingArea()} belonging to
     * this {@link Route}
     *
     * @return the bounding box of this {@link Route}
     */
    public Envelope getBoundingBox()
    {
        if (boundingBox == null)
            boundingBox = new Envelope();
        for (WayPoint wayPoint : wayPoints)
        {
            boundingBox.expandToInclude(wayPoint.getBoundingBox());
        }
        return boundingBox;
    }

    /**
     * Converts a route object containing a {@link List} of {@link WayPoint} objects to a
     * {@link List} of {@link Point} objects using the {@link Coordinate} values of the
     * {@link WayPoint}s
     *
     * @return a {@link List} of {@link Point} objects having the same coordinates as the
     *         {@link WayPoint} objects of the given route or {@code null} if the {@link Route}
     *         object is {@code null}, the {@link List} of {@link WayPoint} objects is {@code null}
     *         or {@code empty}.
     */
    List<Point> toPointList()
    {
        if (wayPoints == null || wayPoints.isEmpty())
            return null;
        List<Point> wayPointsList = new ArrayList<>();
        for (WayPoint wayPoint : wayPoints)
        {
            wayPointsList.add(wayPoint.toGeometry());

        }
        return wayPointsList;
    }
}
