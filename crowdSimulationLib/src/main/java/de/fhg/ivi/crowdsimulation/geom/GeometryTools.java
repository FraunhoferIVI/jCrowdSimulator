package de.fhg.ivi.crowdsimulation.geom;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.math.Vector2D;
import com.vividsolutions.jts.operation.linemerge.LineMerger;

import de.fhg.ivi.crowdsimulation.math.MathTools;

/**
 * This class encapsulates a collection of static methods for calculations related to JTS geometry
 * objects and vector2d objects.
 * <p>
 *
 * There are several methods implemented in this class:
 *
 * <li>Calculation of the nearest {@link Coordinate} in relation to a {@link Geometry}</li>
 * <li>Calculation of a perpendicular line based on two {@link Coordinate} and a width</li>
 * <li>Sorting of a {@link Map} in dependence to a {@link Double} value</li>
 * <li>Computes a normalized {@link Vector2D} between a {@link Geometry} and a
 * {@link Coordinate}</li>
 * <li>Tests whether a {@link Vector2D} lies inside an {@link Envelope}</li>
 * <li>Tests whether a bounding box of two {@link Vector2D}s intersects an {@link Envelope}</li>
 *
 * @author hahmann/meinert
 */
public class GeometryTools
{
    /**
     * Uses the object logger for printing specific messages in the console.
     */
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Calculates a {@link Coordinate} on the boundary of a {@link Geometry} object that has the
     * shortest distance shortest distance between the given {@link Coordinate} and the given
     * {@link Geometry}. <b>Due to rounding errors the resulting {@link Coordinate} is NOT
     * guaranteed to intersect with the {@link Geometry}</b>
     *
     * @param coordinate a {@link Coordinate} which describes a x, y position
     * @param geometry a {@link Geometry} object
     *
     * @return a {@link Coordinate} representing the position on the boundary of {@code geometry}
     *         that is nearest to {@code coordinate} or {@code null}, if one of the input parameter
     *         is {@code null} or the geometry has no coordinates.
     */
    public static Coordinate getNearestCoordinateOnGeometry(Coordinate coordinate,
        Geometry geometry)
    {
        if (coordinate == null || geometry == null)
        {
            return null;
        }
        Coordinate[] coordinates = geometry.getCoordinates();

        // null geometry or empty geometry
        if (coordinates == null || coordinates.length == 0)
        {
            return null;
        }
        // trivial case of a point
        else if (coordinates.length == 1)
        {
            return coordinates[0];
        }
        else
        {
            double distanceSquaredClosestCoordinate = Double.MAX_VALUE;
            Coordinate closestCoordinate = null;
            // loop over lineSegments from a Polygon/LineString
            for (int i = 0; i < coordinates.length - 1; i++ )
            {
                LineSegment lineSegment = new LineSegment(coordinates[i], coordinates[i + 1]);
                // compute the closest point on the lineSegment to the given coordinate
                Coordinate closestCoordinateCandidate = lineSegment.closestPoint(coordinate);

                // computes squared distance between lineSegment and point
                double distanceSquared = GeometryTools.distanceSquared(closestCoordinateCandidate,
                    coordinate);
                if (distanceSquared >= distanceSquaredClosestCoordinate)
                {
                    continue;
                }

                distanceSquaredClosestCoordinate = distanceSquared;
                closestCoordinate = closestCoordinateCandidate;
            }

            if (closestCoordinate != null)
            {
                return closestCoordinate;
            }
            return null;
        }
    }

    /**
     * Gets a line at {@code centerPoint} perpendicular to the direction defined by
     * {@link com.vividsolutions.jts.math.Vector2D} normalized direction vector.
     * <p>
     * The width of this line is specified by the {@code length}.
     *
     * @param centerPoint is the first {@link Coordinate}, which serves as the center point of the
     *            perpendicular line
     * @param normalizedDirectionVector a {@link com.vividsolutions.jts.math.Vector2D} pointing
     * @param length defines the length of the perpendicular line
     *
     * @return a perpendicular line as {@link Geometry}
     */
    public static Geometry getPerpendicularLine(Coordinate centerPoint,
        com.vividsolutions.jts.math.Vector2D normalizedDirectionVector, double length)
    {
        if (centerPoint == null || normalizedDirectionVector == null || length == 0)
            return null;
        length /= 2d;
        // rotate with 90 degrees + set length to specified meters
        com.vividsolutions.jts.math.Vector2D perpendicularVector90 = normalizedDirectionVector
            .multiply(length).rotateByQuarterCircle(1);
        // rotate with 270 degrees + set length to specified meters
        com.vividsolutions.jts.math.Vector2D perpendicularVector270 = normalizedDirectionVector
            .multiply(length).rotateByQuarterCircle(3);

        // create coordinate[] for creating LineStrings
        Coordinate[] rightLineCoordinates = new Coordinate[] { centerPoint,
            new Coordinate(centerPoint.x + perpendicularVector90.getX(),
                centerPoint.y + perpendicularVector90.getY()) };
        Coordinate[] leftLineCoordinates = new Coordinate[] { centerPoint,
            new Coordinate(centerPoint.x + perpendicularVector270.getX(),
                centerPoint.y + perpendicularVector270.getY()) };

        // create lineStrings in each direction
        LineString rightLine = JTSFactoryFinder.getGeometryFactory()
            .createLineString(rightLineCoordinates);
        LineString leftLine = JTSFactoryFinder.getGeometryFactory()
            .createLineString(leftLineCoordinates);

        // merge the 90 degrees line and the 270 degrees line to one line
        LineMerger lineMerger = new LineMerger();
        lineMerger.add(rightLine);
        lineMerger.add(leftLine);
        Collection<LineString> mergedLineStrings = lineMerger.getMergedLineStrings();
        GeometryCollection gc = new GeometryCollection(mergedLineStrings.toArray(new Geometry[] {}),
            JTSFactoryFinder.getGeometryFactory());
        Geometry perpendicularLine = gc.union();

        return perpendicularLine;
    }

    /**
     * Gets a line at {@code centerPoint} perpendicular to the direction defined by the given two
     * {@link Coordinate}s {@code centerPoint} and {@code orientationPoint}.
     * <p>
     * The width of this line is specified by the {@code length}.
     *
     * @param centerPoint is the first {@link Coordinate}, which serves as the center point of the
     *            perpendicular line
     * @param orientationPoint is a {@link Coordinate}, which serves as an orientation point for the
     *            perpendicular line
     * @param length defines the length of the perpendicular line
     *
     * @return a perpendicular line as {@link Geometry}
     */
    public static Geometry getPerpendicularLine(Coordinate centerPoint, Coordinate orientationPoint,
        double length)
    {
        com.vividsolutions.jts.math.Vector2D normalizedDirectionVector = getNormalizedDirection(
            centerPoint, orientationPoint);
        return getPerpendicularLine(centerPoint, normalizedDirectionVector, length);
    }

    /**
     * Gets a normalized direction {@link com.vividsolutions.jts.math.Vector2D} indicating the
     * direction from {@code centerPoint} to {@code orientationPoint}
     *
     * @param centerPoint
     * @param orientationPoint
     * @return
     */
    public static com.vividsolutions.jts.math.Vector2D getNormalizedDirection(
        Coordinate centerPoint, Coordinate orientationPoint)
    {
        if (centerPoint.equals(orientationPoint))
            return null;
        return new com.vividsolutions.jts.math.Vector2D(centerPoint, orientationPoint).normalize();
    }

    /**
     * Tests whether a {@code vector} is inside a {@code boundingBox}.
     *
     * @param vector is a {@link Vector2D}, which defines a x, y position
     * @param boundingBox is an {@link Envelope} object
     *
     * @return {@code true} if the {@code vector} lying inside the {@code boundingBox}. If not it's
     *         {@code false}.
     */
    public static boolean isInside(Vector2D vector, Envelope boundingBox)
    {
        if (vector == null || boundingBox == null)
        {
            return false;
        }
        return boundingBox.getMinX() <= vector.getX() && boundingBox.getMaxX() >= vector.getX()
            && boundingBox.getMinY() <= vector.getY() && boundingBox.getMaxY() >= vector.getY();
    }

    /**
     * Tests whether a bounding box defined by two {@link Vector2D}s intersects a given
     * {@link Envelope}.
     *
     * @param vector1 is a {@link Vector2D}, which defines a x, y position
     * @param vector2 is a {@link Vector2D}, which defines a x, y position
     * @param boundingBox the {@link Envelope} object
     *
     * @return
     *         <li>{@code false} if {@code vector1} is {@code null}, {@code vector2} is {@code null}
     *         or {@code BoundingBox} is {@code null}
     *         <li>{@code true} if the bounding box given by {@code vector1} and {@code vector2}
     *         intersects {@code boundingBox},
     *         <li>{@code false} otherwise
     */
    public static boolean intersects(Vector2D vector1, Vector2D vector2, Envelope boundingBox)
    {
        if (vector1 == null || vector2 == null || boundingBox == null)
        {
            return false;
        }
        return !(boundingBox.getMinX() > Math.max(vector1.getX(), vector2.getX())
            || boundingBox.getMaxX() < Math.min(vector1.getX(), vector2.getX())
            || boundingBox.getMinY() > Math.max(vector1.getY(), vector2.getY())
            || boundingBox.getMaxY() < Math.min(vector1.getY(), vector2.getY()));
    }

    /**
     * Transforms the given {@code lineString} to a polygon. This is implemented by ensuring the
     * {@code lineString} is a ring (by adding the start point if necessary).
     *
     * @param lineString the lineString to be transformed
     * @return the Polygon created from the given lineString
     */
    public static Polygon closeLineString(LineString lineString)
    {
        CoordinateList list = new CoordinateList(lineString.getCoordinates());
        list.closeRing();
        LinearRing ring = JTSFactoryFinder.getGeometryFactory()
            .createLinearRing(list.toCoordinateArray());
        Polygon polygon = JTSFactoryFinder.getGeometryFactory().createPolygon(ring, null);
        return polygon;
    }

    /**
     * Creates a {@link Point} from a given {@link Coordinate} object
     *
     * @param c the {@link Coordinate} object
     * @return the {@link Point} object
     */
    public static Point coordinateToPoint(Coordinate c)
    {
        return JTSFactoryFinder.getGeometryFactory().createPoint(c);
    }

    /**
     * Computes a relative position (between 0 and 1) of {@code coordinateOnTheLine} on the given
     * {@link LineString} under the assumption that the given {@link Coordinate} exactly lies on
     * somewhere on the {@link LineString}. If this assumption is not fulfilled {@link Double#NaN}
     * will be returned.
     *
     * @param theLine the {@link LineString}
     * @param coordinateOnTheLine the {@link Coordinate} assumed to be on the {@link LineString}
     * @return a relative position (between 0 and 1) of the {@link Coordinate} on the
     *         {@link LineString}, if the given {@link Coordinate} is on the {@link LineString} or
     *         {@link Double#NaN}, if this assumption is not fulfilled.
     */
    public static double getRelativePositionOnLineString(LineString theLine,
        Coordinate coordinateOnTheLine)
    {
        GeometryFactory factory = JTSFactoryFinder.getGeometryFactory();
        double length = 0;
        // create point to check for intersection with line
        Point pointOnTheLine = factory.createPoint(coordinateOnTheLine);
        Coordinate[] theLineCoordinates = theLine.getCoordinates();
        // iterate over linestring and create sub-lines for each coordinate pair
        for (int i = 1; i < theLineCoordinates.length; i++ )
        {
            LineString currentLine = factory.createLineString(
                new Coordinate[] { theLineCoordinates[i - 1], theLineCoordinates[i] });
            // check if coordinateOnTheLine is on currentLine
            // if (currentLine.intersects(pointOnTheLine))
            // intersects does not work due to rounding errors therefore distance is used
            if (currentLine.distance(pointOnTheLine) < 0.000001d)
            {
                // create new currentLine with coordinateOnTheLine as endpoint and calculate length
                currentLine = factory.createLineString(
                    new Coordinate[] { theLineCoordinates[i - 1], coordinateOnTheLine });
                length += currentLine.getLength();
                // return result length
                return length / theLine.getLength();
            }
            length += currentLine.getLength();
        }
        // coordinate was not on the line
        return Double.NaN;
    }

    /**
     * Creates and return a {@link LineString} from a given {@link List} of {@link Geometry}s
     * representing WayPoints.
     *
     * @param wayPointGeometries the {@link List} of {@link Geometry}s representing WayPoints
     * @return the {@link LineString} derived from the {@link List} of {@link Geometry}s
     *         representing WayPoints
     */
    public static LineString createLineStringFromGeometries(List<Geometry> wayPointGeometries)
    {
        Geometry[] wayPointGeometriesArray = wayPointGeometries.toArray(new Geometry[0]);
        Coordinate[] wayPointCoordinates = new Coordinate[wayPointGeometriesArray.length];
        for (int i = 0; i < wayPointCoordinates.length; i++ )
        {
            wayPointCoordinates[i] = wayPointGeometriesArray[i].getCoordinate();
        }
        return JTSFactoryFinder.getGeometryFactory().createLineString(wayPointCoordinates);
    }

    /**
     * Gets an {@link Envelope} that contains all {@link Envelope} objects of all {@link Geometry}
     * objects given by {@code geometries}
     *
     * @param geometries the {@link List} of {@link Geometry} objects
     * @return the bounding box {@link Envelope} object of all {@link Geometry} objects contained in
     *         {@code geometry}
     */
    public static Envelope getEnvelope(List<Geometry> geometries)
    {
        if (geometries == null || geometries.isEmpty())
            return null;
        Envelope envelope = new Envelope();

        // sums up the bounding boxes of all geometries
        for (Geometry geometry : geometries)
        {
            envelope.expandToInclude(geometry.getEnvelopeInternal());
        }
        return envelope;
    }

    /**
     * Gets an {@link Envelope} that contains all {@link Envelope} objects of all {@link Geometry}
     * objects given by {@code geometries}
     *
     * @param coordinates the {@link List} of {@link Coordinate} objects
     * @return the bounding box {@link Envelope} object of all {@link Geometry} objects contained in
     *         {@code geometry}
     */
    public static Envelope getEnvelopeFromCoordinates(List<? extends Coordinate> coordinates)
    {
        if (coordinates == null || coordinates.isEmpty())
            return null;
        Envelope envelope = new Envelope();

        // sums up the bounding boxes of all geometries
        for (Coordinate coordinate : coordinates)
        {
            envelope.expandToInclude(coordinate);
        }
        return envelope;
    }

    /**
     * Creates and returns a {@link LineString} from a given {@link List} of {@link Coordinate}s.
     *
     * @param coordinates the {@link List} of {@link Coordinate}s
     * @return the {@link LineString} derived from the {@link List} of {@link Coordinate}s
     *
     */
    public static LineString createLineStringFromCoordinates(List<? extends Coordinate> coordinates)
    {
        Coordinate[] coordinatesArray = coordinates.toArray(new Coordinate[0]);
        return JTSFactoryFinder.getGeometryFactory().createLineString(coordinatesArray);
    }

    /**
     * Computes the 2-dimensional Euclidean distance between 2 {@link Coordinate} objects. The
     * Z-ordinate is ignored.
     *
     * @param c1 a point
     * @param c2 a point
     *
     * @return the 2-dimensional Euclidean distance between the locations
     */
    public static double distance(Coordinate c1, Coordinate c2)
    {
        double dx = c1.x - c2.x;
        double dy = c1.y - c2.y;
        return MathTools.hypot(dx, dy, MathTools.FAST_MATH);
    }

    /**
     * Computes the 2-dimensional Euclidean distance between 2 {@link Point} objects. The Z-ordinate
     * is ignored.
     *
     * @param c1 a point
     * @param c2 a point
     *
     * @return the 2-dimensional Euclidean distance between the locations
     */
    public static double distance(Point c1, Point c2)
    {
        double dx = c1.getX() - c2.getX();
        double dy = c1.getY() - c2.getY();
        return MathTools.hypot(dx, dy, MathTools.FAST_MATH);
    }

    /**
     * Computes the 2-dimensional Euclidean distance (squared) between 2 {@link Coordinate} objects.
     * The Z-ordinate is ignored.
     *
     * @param c1 a point
     * @param c2 a point
     *
     * @return the 2-dimensional Euclidean distance (squared) between the locations
     */
    public static double distanceSquared(Coordinate c1, Coordinate c2)
    {
        return (c2.x - c1.x) * (c2.x - c1.x) + (c2.y - c1.y) * (c2.y - c1.y);
    }

    /**
     * https://stackoverflow.com/questions/33549915/how-to-split-linestring-into-parts-every-x-meters-with-java-jts
     *
     * @param ls
     * @param length
     * @return
     */
    public static ArrayList<LineString> splitLineStringIntoParts(LineString ls, double length)
    {
        // result list for linestrings
        ArrayList<LineString> resultList = new ArrayList<>();
        // list for linesegments from input linestring
        ArrayList<LineSegment> lineSegmentList = new ArrayList<>();
        // create LineSegment objects from input linestring and add them to list
        for (int i = 1; i < ls.getCoordinates().length; i++ )
        {
            lineSegmentList
                .add(new LineSegment(ls.getCoordinates()[i - 1], ls.getCoordinates()[i]));
        }
        LineString currentLineString = null;
        double neededLength = length;
        for (LineSegment s : lineSegmentList)
        {
            while (s.getLength() > 0)
            {
                // case: current segment is small enough to be added to the linestring
                if (s.getLength() <= neededLength)
                {
                    // create linestring if it does not exist
                    if (currentLineString == null)
                    {
                        currentLineString = new GeometryFactory().createLineString(
                            new Coordinate[] { new Coordinate(s.p0), new Coordinate(s.p1) });
                        // just add the new endpoint otherwise
                    }
                    else
                    {
                        Coordinate[] coords = new Coordinate[currentLineString
                            .getCoordinates().length + 1];
                        // copy old coordinates
                        System.arraycopy(currentLineString.getCoordinates(), 0, coords, 0,
                            currentLineString.getCoordinates().length);
                        // add new coordinate at the end
                        coords[coords.length - 1] = new Coordinate(s.p1);
                        // create new linestring
                        currentLineString = new GeometryFactory().createLineString(coords);
                    }
                    neededLength -= s.getLength();
                    s.setCoordinates(s.p1, s.p1);
                    // add linestring to result list if needed length is 0
                    if (neededLength == 0)
                    {
                        resultList.add(currentLineString);
                        currentLineString = null;
                        neededLength = length;
                    }
                    // current segment needs to be cut and added to the linestring
                }
                else
                {
                    // get coordinate at desired distance (endpoint of linestring)
                    Coordinate endPoint = s.pointAlong(neededLength / s.getLength());
                    // create linestring if it does not exist
                    if (currentLineString == null)
                    {
                        currentLineString = new GeometryFactory()
                            .createLineString(new Coordinate[] { new Coordinate(s.p0), endPoint });
                        // just add the new endpoint otherwise
                    }
                    else
                    {
                        // add new coordinate to linestring
                        Coordinate[] coords = new Coordinate[currentLineString
                            .getCoordinates().length + 1];
                        // copy old coordinates
                        System.arraycopy(currentLineString.getCoordinates(), 0, coords, 0,
                            currentLineString.getCoordinates().length);
                        // add new coordinate at the end
                        coords[coords.length - 1] = endPoint;
                        currentLineString = new GeometryFactory().createLineString(coords);
                    }
                    // add linestring to result list
                    resultList.add(currentLineString);
                    // reset needed length
                    neededLength = length;
                    // reset current linestring
                    currentLineString = null;
                    // adjust segment (calculated endpoint is the new startpoint)
                    s.setCoordinates(endPoint, s.p1);
                }
            }
        }
        // add last linestring if there is a rest
        if (neededLength < length)
        {
            resultList.add(currentLineString);
        }
        return resultList;
    }

    /**
     * Creates a {@link List} of {@link Geometry} objects (usually of type {@link LineString}) that
     * represents segments that are created using consecutive {@link Coordinate} objects from the
     * given {@code geometry} objects.
     * <p>
     * If {@code geometry} is {@code null} or contains zero points, {@code null} is returned.
     * <p>
     * If {@code geometry} contains only 1 {@link Coordinate}, a {@link List} containing only the
     * given {@code geometry} object is returned.
     *
     * @param geometry the Geometry object to be used to create line segments
     * @return a {@link List} of {@link Geometry} objects representing Line segments
     */
    public static List<? extends Geometry> toSegments(Geometry geometry)
    {
        if (geometry == null)
            return null;
        if (geometry.getCoordinates().length == 0)
            return null;
        if (geometry.getCoordinates().length == 1)
            return Arrays.asList(geometry);

        ArrayList<LineString> lineStrings = new ArrayList<>();
        for (int i = 1; i < geometry.getCoordinates().length; i++ )
        {
            LineString lineString = JTSFactoryFinder.getGeometryFactory()
                .createLineString(new Coordinate[] { geometry.getCoordinates()[i - 1],
                    geometry.getCoordinates()[i] });
            lineStrings.add(lineString);
        }
        return lineStrings;
    }

    /**
     * Creates a {@link List} of {@link LineString} objects created from the given
     * {@code lineString} object with each object guaranteed to be not longer than {@code length}
     * meters.
     * <p>
     * Taken from
     * https://stackoverflow.com/questions/33549915/how-to-split-linestring-into-parts-every-x-meters-with-java-jts
     *
     * @param lineString the {@link LineString} object to be split into small segments
     * @param length given in meters
     * @return a {@link List} of {@link LineString} objects
     */
    public static List<LineString> lineSplit(LineString lineString, double length)
    {
        List<LineString> results = new ArrayList<>();
        List<LineSegment> segments = new ArrayList<>();
        // first split linestring to segements[]
        for (int i = 1; i < lineString.getCoordinates().length; i++ )
        {
            segments.add(new LineSegment(lineString.getCoordinates()[i - 1],
                lineString.getCoordinates()[i]));
        }
        // remainLegnth means that last segment's length dont enough to split to one segement which
        // length is meters
        // neededLength means that current segment need how many meters to create a new segment
        double remainLength = 0D;
        double neededLength = 0D;
        // remainCoors means that if the last iteartor dont create a new segment,also mean last
        // segment
        // is too short ,even add remains length can't create a new segment;so, we should add this
        // segment's start
        // point and end point to remainCoors
        List<Coordinate> remainCoors = new ArrayList<>();
        // netxStartPoint to store the next segment's start point
        Coordinate netxStartPoint = null;
        for (int i = 0; i < segments.size(); i++ )
        {
            LineSegment seg = segments.get(i);
            neededLength = length - remainLength;
            remainLength += seg.getLength();
            netxStartPoint = seg.p0;
            while (remainLength >= length)
            {
                remainCoors.add(netxStartPoint);
                Coordinate endPoint = seg.pointAlong(neededLength / seg.getLength());
                // to remove the adjacent and same vertx
                for (int j = 0; j < remainCoors.size() - 1; j++ )
                {
                    if (remainCoors.get(j).equals(remainCoors.get(j + 1)))
                    {
                        remainCoors.remove(j);
                    }
                }
                remainCoors.add(endPoint);
                results.add(lineString.getFactory()
                    .createLineString(remainCoors.toArray(new Coordinate[remainCoors.size()])));
                remainCoors = new ArrayList<>();
                netxStartPoint = endPoint;
                remainLength -= length;
                neededLength += length;
            }
            remainCoors.add(netxStartPoint);
            remainCoors.add(seg.p1);
        }
        for (int j = 0; j < remainCoors.size() - 1; j++ )
        {
            if (remainCoors.get(j).equals(remainCoors.get(j + 1)))
            {
                remainCoors.remove(j);
            }
        }
        if (remainCoors.size() >= 2)
        {
            results.add(lineString.getFactory()
                .createLineString(remainCoors.toArray(new Coordinate[remainCoors.size()])));
        }
        return results;
    }
}
