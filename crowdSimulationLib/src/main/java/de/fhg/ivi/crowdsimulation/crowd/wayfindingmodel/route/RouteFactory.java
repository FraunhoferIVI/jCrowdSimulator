package de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route;

import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.JTSFactoryFinder;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.math.Vector2D;

import de.fhg.ivi.crowdsimulation.CrowdSimulator;
import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.ForceModel;
import de.fhg.ivi.crowdsimulation.geom.GeometryTools;
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
public class RouteFactory
{
    /**
     * The {@link Quadtree} object allows (efficient) access to {@link Boundary} objects and
     * {@link Pedestrian} objects belonging to the same {@link CrowdSimulator} and makes
     * Pedestrians-Boundary and Pedestrian-Pedestrian interaction possible.
     */
    protected Quadtree quadtree;

    /**
     * Creates a new {@link RouteFactory} using the given {@link Quadtree}
     *
     * @param quadtree {@link Quadtree} object allows (efficient) access to {@link Boundary} objects
     *            and {@link Pedestrian} objects belonging to the same {@link CrowdSimulator}
     */
    public RouteFactory(Quadtree quadtree)
    {
        this.quadtree = quadtree;
    }

    /**
     * Creates a {@link Route} from a {@link List} of {@link Geometry} objects. In case these
     * objects are not {@link Point}, there centers will be taken as {@link Coordinate} for the
     * {@link WayPoint} of the {@link Route}.
     * <p>
     * All {@link WayPoint} objects of the resulting {@link Route} have
     * {@value WayPoint#DEFAULT_WIDTH} meters width and
     *
     * @param wayPointGeometries a {@link List} of {@link Geometry} objects
     * @return the {@link Route} object resulting from {@code wayPointGeometries}
     */
    public Route createRouteFromGeometries(List<Geometry> wayPointGeometries)
    {
        List<WayPoint> wayPoints = createWayPointsFromGeometries(wayPointGeometries);
        return new Route(wayPoints);
    }

    /**
     * Creates a {@link Route} from a {@link List} of {@link Geometry} objects.
     *
     * @param wayPointCoordinates a {@link List} of {@link Coordinate}s
     * @return the {@link Route} object resulting from {@code wayPointGeometries}
     */
    public Route createRouteFromCoordinates(List<? extends Coordinate> wayPointCoordinates)
    {
        List<WayPoint> wayPoints = createWayPointsFromCoordinates(wayPointCoordinates);
        return new Route(wayPoints);
    }

    /**
     * Create a {@link List} of complete {@link WayPoint}s using the given
     * {@code wayPointGeometries}.
     * <p>
     * The target lines and the passing areas of the {@link WayPoint}s and the connection lines
     * between two consecutive {@link WayPoint}s are also computed.
     *
     * @param wayPointGeometries object as a {@link List} of {@link Coordinate}s which describe
     *            their position.
     * @return a {@link List} of {@link WayPoint} objects created from {@code wayPointGeometries}
     */
    private List<WayPoint> createWayPointsFromGeometries(List<Geometry> wayPointGeometries)
    {
        if (wayPointGeometries == null || wayPointGeometries.isEmpty())
            return null;

        List<Coordinate> coordinateList = new ArrayList<>();

        // gets the coordinates from the given wayPointGeometries
        for (Geometry wayPointGeometry : wayPointGeometries)
        {

            coordinateList.add(wayPointGeometry.getCoordinate());
        }

        return createWayPointsFromCoordinates(coordinateList);
    }

    /**
     * Create a {@link List} of complete {@link WayPoint}s using the given {@code coordinateList}.
     * <p>
     * The target lines and the passing areas of the {@link WayPoint}s and the connection lines
     * between two consecutive {@link WayPoint}s are also computed.
     *
     * @param coordinateList a {@link List} of {@link Coordinate} objects representing the
     *            {@link WayPoint} object to be created in the required order.
     *
     * @return a {@link List} of {@link WayPoint} objects created from {@code coordinateList}
     */
    private List<WayPoint> createWayPointsFromCoordinates(List<? extends Coordinate> coordinateList)
    {

        if (coordinateList == null || coordinateList.isEmpty())
            return null;

        ArrayList<WayPoint> wayPointList = new ArrayList<>();

        // create waypoints
        for (int i = 0; i < coordinateList.size(); i++ )
        {
            Coordinate otherWayPoint = i == 0 ? coordinateList.get(i + 1)
                : coordinateList.get(i - 1);
            WayPoint wayPoint = new WayPoint(i, coordinateList.get(i));
            boolean hasConnectionLineToPredecessor = i > 0;

            // avoid duplicated consecutive waypoints
            if (wayPoint.equals(otherWayPoint))
                continue;

            // direction of waypoint
            Vector2D normalizedDirectionOfWayPoint = GeometryTools.getNormalizedDirection(wayPoint,
                otherWayPoint);
            wayPoint.setNormalizedDirectionVector(normalizedDirectionOfWayPoint);

            // computes target line and passing area
            wayPoint.setWidth(wayPoint.getWidth(), quadtree);

            // connection line between waypoints
            LineString connectionLineToPredecessor = hasConnectionLineToPredecessor
                ? RouteFactory.computeConnectionLineToPredecessor(wayPoint, otherWayPoint) : null;
            wayPoint.setConnectionLineToPredecessor(connectionLineToPredecessor);

            wayPointList.add(wayPoint);
        }

        return wayPointList;
    }

    /**
     * Generates a {@link LineString} between the given {@code wayPoint} and the preceding
     * {@link WayPoint}
     *
     * @param wayPoint a {@link WayPoint} object
     * @param precedingWayPoint contains the {@link Coordinate} of the preceding {@link WayPoint} in
     *            the same {@link Route}
     *
     * @return a {@link LineString}, which is the connection line between the given {@code } lying
     *         next to each other.
     */
    private static LineString computeConnectionLineToPredecessor(Coordinate wayPoint,
        Coordinate precedingWayPoint)
    {
        if (precedingWayPoint == null)
            return null;
        GeometryFactory factory = JTSFactoryFinder.getGeometryFactory();

        Point firstPoint = factory.createPoint(new Coordinate(wayPoint.x, wayPoint.y));
        Point lastPoint = factory
            .createPoint(new Coordinate(precedingWayPoint.x, precedingWayPoint.y));

        Coordinate[] coordinates = new Coordinate[] {
            new Coordinate(firstPoint.getX(), firstPoint.getY()),
            new Coordinate(lastPoint.getX(), lastPoint.getY()) };

        LineString lineString = JTSFactoryFinder.getGeometryFactory().createLineString(coordinates);

        return lineString;
    }
}
