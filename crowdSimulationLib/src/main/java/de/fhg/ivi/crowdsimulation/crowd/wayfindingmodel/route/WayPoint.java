package de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.math.Vector2D;

import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.geom.GeometryTools;
import de.fhg.ivi.crowdsimulation.geom.Quadtree;

/**
 * A {@link WayPoint} is represented through a {@link Coordinate} on a 2-dimensional level.
 * <p>
 * The {@link WayPoint}s are the target points of the {@link Pedestrian} movement. The sequence in
 * which the {@link WayPoint}s are loaded, which is represented through the {@link #id}, describes
 * also the sequence in which the pedestrian will pass all {@link WayPoint}s.
 *
 * @author hahmann/meinert
 */
public class WayPoint extends Coordinate
{
    /**
     * Uses the object logger for printing specific messages in the console.
     */
    @SuppressWarnings("unused")
    private static final Logger logger                 = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());

    /**
     * default serial version ID
     */
    private static final long   serialVersionUID       = 1L;

    /**
     * The default width of the perpendicular line computed for {@link WayPoint#targetLine}.
     * <p>
     */
    public static final float   DEFAULT_WIDTH          = 10f;

    /**
     * The default time period a {@link Pedestrian} will stop when passing this {@link WayPoint}.
     * Given in milliseconds.
     */
    public static final long    DEFAULT_WAITING_PERIOD = 0;

    /**
     * The ratio for computing the extended "radius" of the perpendicular line
     */
    public static final float   EXTENSION_RATIO        = 0.2f;

    /**
     * Denotes a specific number for a {@link WayPoint} which distinguishes them from other
     * {@link WayPoint} objects.
     */
    private int                 id;

    /**
     * {@link Geometry} (should usually be a {@link LineString} but can also be a
     * {@link MultiLineString} that represents a line that is vertical to the line going from this
     * {@link WayPoint} to its next {@link WayPoint} in the {@link Route} and which goes exactly
     * through this {@link WayPoint}
     * <p>
     * It has a length of {@link #width}
     * <p>
     * The purpose of this {@link Geometry} is that a {@link Pedestrian} can get a target point
     * somewhere on this {@link Geometry}, which is nearest to them so that not all
     * {@link Pedestrian} exactly need to go to the center of this {@link WayPoint}.
     */
    private Geometry            targetLine;

    /**
     * {@link Geometry} (should usually be a {@link Polygon} but can also be a {@link MultiPolygon}
     * that represents a buffer around the {@link #targetLine} going through this {@link WayPoint}.
     * <p>
     * This buffer is built the following way: {@link #targetLine} is extended in length by
     * {@link #EXTENSION_RATIO} and additionally buffered by the same size
     * <p>
     * The purpose of this {@link Geometry} is that a {@link Pedestrian} can consider a
     * {@link WayPoint} as being reached also in the near of the {@link WayPoint} instead only
     * directly on the {@link #targetLine}.
     */
    private Geometry            passingArea;

    /**
     * The Bounding Box of {@link #passingArea}
     */
    private Envelope            boundingBox;

    /**
     * {@link LineString} representing the connecting line to the preceding {@link WayPoint} in the
     * same {@link Route}.
     * <p>
     * If this {@link WayPoint} is the first {@link WayPoint} in a {@link Route} this is
     * {@code null}.
     * <p>
     * The background of this is, that a {@link Pedestrian} could use this connecting line as a
     * reference to get back to the route, if necessary.
     */
    private LineString          connectionLineToPredecessor;

    /**
     * The length of the {@link #connectionLineToPredecessor} in meters. If
     * {@link #connectionLineToPredecessor} is {@code null} this is {@value Float#NaN}
     */
    private float               distanceToPredecessor;

    /**
     * Describes the relative position of this {@link WayPoint} within the series/list this
     * {@link WayPoint} belongs to. This is a value between 0 and 1, while 0 means, this
     * {@link WayPoint} is start point of the series and 1 means this {@link WayPoint} is the last
     * {@link WayPoint} of the series.
     */
    private double              relativePositionOnRoute;

    /**
     * The time period a {@link Pedestrian} will stop when passing this {@link WayPoint}. Given in
     * milliseconds.
     */
    private long                waitingPeriod;

    /**
     * A {@link Vector2D} pointing from this {@link WayPoint} to the previous {@link WayPoint}
     * within the same {@link Route}. If this {@link WayPoint} is the first {@link WayPoint} of a
     * {@link Route}, it points to the next {@link WayPoint} in that {@link Route}
     */
    private Vector2D            normalizedDirectionVector;

    /**
     * Defines the total length of {@link #targetLine}, a line that is vertical to the line going
     * from this {@link WayPoint} to its next {@link WayPoint} in the {@link Route} and which goes
     * exactly through this {@link WayPoint}.
     */
    private double              width;

    /**
     * Creates a new {@link WayPoint}, which inherits from {@link Coordinate}.
     * <p>
     * Users are requested to create {@link WayPoint} objects using
     * {@link RouteFactory#createRouteFromCoordinates(List)}
     * <p>
     * {@link #waitingPeriod} is set to {@value #DEFAULT_WAITING_PERIOD} milliseconds and
     * {@link #width} is set to {@value #DEFAULT_WIDTH} meters.
     *
     * @param id of this {@link WayPoint}
     * @param coordinate {@link Coordinate} of the {@link WayPoint}
     */
    WayPoint(int id, Coordinate coordinate)
    {
        this(id, coordinate, DEFAULT_WAITING_PERIOD, DEFAULT_WIDTH);
    }

    /**
     * Creates a new {@link WayPoint}, which inherits from {@link Coordinate}.
     * <p>
     * Users are requested to create {@link WayPoint} objects using
     * {@link RouteFactory#createRouteFromCoordinates(List)}
     * <p>
     * {@link #waitingPeriod} is set to {@value #DEFAULT_WAITING_PERIOD} milliseconds.
     *
     * @param id of this {@link WayPoint}
     * @param coordinate {@link Coordinate} of the {@link WayPoint}
     * @param width the width of this {@link WayPoint}, i.e. the total length of
     *            {@link #targetLine}, a line that is vertical to the line going from this
     *            {@link WayPoint} to its next {@link WayPoint} in the {@link Route} and which goes
     *            exactly through this {@link WayPoint}.
     */
    WayPoint(int id, Coordinate coordinate, float width)
    {
        this(id, coordinate, DEFAULT_WAITING_PERIOD, width);
    }

    /**
     * Creates a new {@link WayPoint}, which inherits from {@link Coordinate}.
     * <p>
     * Users are requested to create {@link WayPoint} objects using
     * {@link RouteFactory#createRouteFromCoordinates(List)}
     * <p>
     * {@link #width} is set to {@value #DEFAULT_WIDTH} meters.
     *
     * @param id of this {@link WayPoint}
     * @param coordinate {@link Coordinate} of the {@link WayPoint}
     * @param waitingPeriod the time period a {@link Pedestrian} will stop when passing this
     *            {@link WayPoint}. Given in milliseconds.
     */
    WayPoint(int id, Coordinate coordinate, long waitingPeriod)
    {
        this(id, coordinate, waitingPeriod, DEFAULT_WIDTH);
    }

    /**
     * Creates a new {@link WayPoint}, which inherits from {@link Coordinate}.
     * <p>
     * Users are requested to create {@link WayPoint} objects using
     * {@link RouteFactory#createRouteFromCoordinates(List)}
     * <p>
     *
     * @param id of this {@link WayPoint}
     * @param coordinate {@link Coordinate} of the {@link WayPoint}
     * @param waitingPeriod the time period a {@link Pedestrian} will stop when passing this
     *            {@link WayPoint}. Given in milliseconds.
     * @param width the width of this {@link WayPoint}, i.e. the total length of
     *            {@link #targetLine}, a line that is vertical to the line going from this
     *            {@link WayPoint} to its next {@link WayPoint} in the {@link Route} and which goes
     *            exactly through this {@link WayPoint}.
     */
    WayPoint(int id, Coordinate coordinate, long waitingPeriod, float width)
    {
        super(coordinate);

        this.id = id;
        this.waitingPeriod = waitingPeriod;
        this.width = width;
    }

    /**
     * Gets the Id of this {@link WayPoint}.
     *
     * @return the id of this {@link WayPoint}
     */
    public int getId()
    {
        return id;
    }

    /**
     * Gets the {@link #targetLine}.
     * <p>
     * {@link Geometry} (should usually be a {@link LineString} but can also be a
     * {@link MultiLineString} that represents a line that is vertical to the line going from this
     * {@link WayPoint} to its next {@link WayPoint} in the {@link Route} and which goes exactly
     * through this {@link WayPoint}
     * <p>
     * It has a length of {@link #DEFAULT_WIDTH}.
     * <p>
     * The purpose of this {@link Geometry} is that a {@link Pedestrian} can get a target point
     * somewhere on this {@link Geometry}, which is nearest to them so that not all
     * {@link Pedestrian} exactly need to go to the center of this {@link WayPoint}.
     */
    public Geometry getTargetLine()
    {
        return targetLine;
    }

    /**
     * Sets a {@link Geometry} (should usually be a {@link LineString} but can also be a
     * {@link MultiLineString} that represents a line that is vertical to the line going from this
     * {@link WayPoint} to its next {@link WayPoint} in the {@link Route} and which goes exactly
     * through this {@link WayPoint}
     * <p>
     * It has a length of {@link #DEFAULT_WIDTH}.
     * <p>
     * The purpose of this {@link Geometry} is that a {@link Pedestrian} can get a target point
     * somewhere on this {@link Geometry}, which is nearest to it so that not all {@link Pedestrian}
     * exactly need to go to the center of this {@link WayPoint}.
     *
     * @param targetLine the target line geometry
     */
    public void setTargetLine(Geometry targetLine)
    {
        this.targetLine = targetLine;
    }

    /**
     * Gets the {@link #passingArea}.
     * <p>
     * A {@link Geometry} (should usually be a {@link Polygon} but can also be a
     * {@link MultiPolygon} that represents a buffer around the {@link #targetLine} going through
     * this {@link WayPoint}.
     * <p>
     * This buffer is built the following way: {@link #targetLine} is extended in length by
     * {@link #EXTENSION_RATIO} and additionally buffered by the same size
     * <p>
     * The purpose of this {@link Geometry} is that a {@link Pedestrian} can check off this
     * {@link WayPoint} also in the near of the {@link WayPoint} instead only directly on the
     * {@link #targetLine}.
     *
     * @return the {@link #passingArea}.
     */
    public Geometry getPassingArea()
    {
        return passingArea;
    }

    /**
     * Sets a {@link Geometry} (should usually be a {@link Polygon} but can also be a
     * {@link MultiPolygon} that represents a buffer around the {@link #targetLine} going through
     * this {@link WayPoint}.
     * <p>
     * This buffer is built the following way: {@link #targetLine} is extended in length by
     * {@link #EXTENSION_RATIO} and additionally buffered by the same size
     * <p>
     * The purpose of this {@link Geometry} is that a {@link Pedestrian} can check off this
     * {@link WayPoint} also in the near of the {@link WayPoint} instead only directly on the
     * {@link #targetLine}.
     *
     * @param passingArea the passingArea {@link Geometry}
     */
    public void setPassingArea(Geometry passingArea)
    {
        this.passingArea = passingArea;
        boundingBox = passingArea.getEnvelopeInternal();
    }

    /**
     * Gets the Bounding Box of {@link #passingArea}
     *
     * @return he Bounding Box of {@link #passingArea}
     */
    public Envelope getBoundingBox()
    {
        return boundingBox;
    }

    /**
     * Gets a {@link LineString}, which represents the {@link #connectionLineToPredecessor}.
     *
     * @return the {@link #connectionLineToPredecessor}
     */
    public LineString getConnectionLineToPredecessor()
    {
        return connectionLineToPredecessor;
    }

    /**
     * Sets a {@link LineString} representing the connecting line to the preceding {@link WayPoint}
     * in the same {@link Route}
     *
     * @param connectionLineToPredecessor the {@link LineString} connecting this {@link WayPoint} to
     *            the previous {@link WayPoint} in the {@link Route}
     */
    public void setConnectionLineToPredecessor(LineString connectionLineToPredecessor)
    {
        this.connectionLineToPredecessor = connectionLineToPredecessor;
        if (connectionLineToPredecessor != null)
            distanceToPredecessor = (float) connectionLineToPredecessor.getLength();
        else
            distanceToPredecessor = Float.NaN;
    }

    /**
     * Gets the distance of this {@link WayPoint} to the previous {@link WayPoint} in the same
     * {@link Route}. If {@link #connectionLineToPredecessor} is {@code null} this is
     * {@value Float#NaN}
     *
     * @return the distance in meters
     */
    float getDistanceToPredecessor()
    {
        return distanceToPredecessor;
    }

    /**
     * Gets the relative position of this {@link WayPoint} within the series/list this
     * {@link WayPoint} belongs to. This is a value between 0 and 1, while 0 means, this
     * {@link WayPoint} is start point of the series and 1 means this {@link WayPoint} is the last
     * {@link WayPoint} of the series.
     *
     * @return a value between 0 and 1
     */
    public double getRelativePositionOnRoute()
    {
        return relativePositionOnRoute;
    }

    /**
     * Sets the relative position of this {@link WayPoint} within the series/list this
     * {@link WayPoint} belongs to.
     * <p>
     * The value is expected to be between 0 and 1, while 0 means, this {@link WayPoint} is start
     * point of the series and 1 means this {@link WayPoint} is the last {@link WayPoint} of the
     * series.
     *
     * @param relativePositionOnRoute
     */
    public void setRelativePositionOnRoute(double relativePositionOnRoute)
    {
        this.relativePositionOnRoute = relativePositionOnRoute;
    }

    /**
     * Gets the time period a {@link Pedestrian} will stop when passing this {@link WayPoint}. Given
     * in milliseconds.
     *
     * @return the time period a {@link Pedestrian} will stop when passing this {@link WayPoint}.
     *         Given in milliseconds.
     */
    long getWaitingPeriod()
    {
        return waitingPeriod;
    }

    /**
     * Gets the width of this {@link WayPoint}, i.e. the total length of {@link #targetLine}, a line
     * that is vertical to the line going from this {@link WayPoint} to its next {@link WayPoint} in
     * the {@link Route} and which goes exactly through this {@link WayPoint}.
     *
     * @return the width of this {@link WayPoint} in meters.
     */
    public double getWidth()
    {
        return width;
    }

    /**
     * Sets the width of this {@link WayPoint}, i.e. the total length of {@link #targetLine}, a line
     * that is vertical to the line going from this {@link WayPoint} to its next {@link WayPoint} in
     * the {@link Route} and which goes exactly through this {@link WayPoint}.
     * <p>
     * Recomputes the {@link #targetLine} and {@link #passingArea} using the new width value.
     *
     * @param width the width of the {@link #targetLine} of this {@link WayPoint}
     * @param quadtree a {@link Quadtree} object allowing (efficient) access to {@link Boundary}
     *            objects and {@link Pedestrian} objects
     */
    public void setWidth(double width, Quadtree quadtree)
    {
        if (normalizedDirectionVector == null)
            return;
        this.width = width;
        setTargetLine(computeVertical(false, quadtree));
        setPassingArea(computeVertical(true, quadtree));
    }

    /**
     * Sets the time period a {@link Pedestrian} will stop when passing this {@link WayPoint}. Given
     * in milliseconds.
     *
     * @param waitingPeriod sets the time period a {@link Pedestrian} will stop when passing this
     *            {@link WayPoint}. Given in milliseconds.
     */
    public void setWaitingPeriod(long waitingPeriod)
    {
        this.waitingPeriod = waitingPeriod;
    }

    /**
     * Sets a {@link Vector2D} pointing from this {@link WayPoint} to the previous {@link WayPoint}
     * within the same {@link Route}. If this {@link WayPoint} is the first {@link WayPoint} of a
     * {@link Route}, it points to the next {@link WayPoint} in that {@link Route}
     *
     * @param normalizedDirectionVector the direction vector
     */
    void setNormalizedDirectionVector(Vector2D normalizedDirectionVector)
    {
        this.normalizedDirectionVector = normalizedDirectionVector;
    }

    /**
     * Gets this WayPoint as a {@link Point} object
     *
     * @return the {@link Point} representation of this {@link WayPoint}
     */
    Point toGeometry()
    {
        return GeometryTools.coordinateToPoint(this);
    }

    /**
     * Computes a {@link LineString} that is perpendicular to {@link #normalizedDirectionVector} and
     * which goes exactly through this {@link WayPoint}.
     * <p>
     * Its length is defined by {@link #getWidth()}
     *
     * @param computePassingArea if {@code true} an the passing area is computed
     * @param quadtree a {@link Quadtree} object allowing (efficient) access to {@link Boundary}
     *            objects and {@link Pedestrian} objects
     *
     * @return the perpendicular line going through this {@link WayPoint} of a vertical through
     *         every {@link WayPoint} in dependence of the location of {@link WayPoint}s lying side
     *         by side
     */
    private Geometry computeVertical(boolean computePassingArea, Quadtree quadtree)
    {
        double length = computePassingArea ? getWidth() * (1 + EXTENSION_RATIO) : getWidth();

        // calculates vertical with the predefined width
        Geometry vertical = GeometryTools.getPerpendicularLine(this, normalizedDirectionVector,
            length);

        // compute passing area by buffering target line so that the waypoint can be marked as
        // crossed even before the actual waypoint target line is crossed
        if (computePassingArea)
            vertical = vertical.buffer(length * EXTENSION_RATIO);

        List<Boundary> boundaries = null;
        if (quadtree != null)
            boundaries = quadtree.getBoundaries(vertical.getEnvelopeInternal());
        // check if vertical intersects boundaries
        if (boundaries != null && !boundaries.isEmpty())
        {
            if ( !computePassingArea)
            {
                for (Boundary boundary : boundaries)
                {
                    Geometry boundaryGeometry = boundary.getBufferedGeometry() == null
                        ? boundary.getGeometry() : boundary.getBufferedGeometry();
                    vertical = vertical.difference(boundaryGeometry);
                }
            }
            else
            {
                for (Boundary boundary : boundaries)
                {
                    vertical = vertical.difference(boundary.getGeometry());
                }
            }
        }
        return vertical;
    }
}
