package de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration;

import java.util.List;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.math.Vector2D;

import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.boundaries.BoundarySegment;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.geom.GeometryTools;
import de.fhg.ivi.crowdsimulation.math.MathTools;

/**
 * This class is used to validate the calculation of the used numeric integration class. This could
 * be one out of the classes {@link SimpleEulerIntegrator}, {@link SemiImplicitEulerIntegrator} or
 * {@link RungeKuttaIntegrator}.
 * <p>
 * The validation is split into three parts:
 * <li>Validation of the velocity of the {@link Pedestrian}s</li>
 * <li>Validation of the movement, represented through the position, of the {@link Pedestrian}s</li>
 * <li>Validation whether the {@link Pedestrian} movement intersects a {@link Geometry}</li>
 * <p>
 *
 * @author hahmann/meinert
 */
public class NumericIntegrationTools
{

    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger logger     = LoggerFactory.getLogger(NumericIntegrationTools.class);

    /**
     * mode for checking crossing in
     * {@link #moveRelatesGeometry(Envelope, Geometry, Vector2D, Vector2D, byte)}
     */
    private static final byte   CROSSES    = 0;

    /**
     * mode for checking intersecting in
     * {@link #moveRelatesGeometry(Envelope, Geometry, Vector2D, Vector2D, byte)}
     */
    private static final byte   INTERSECTS = 1;

    /**
     * mode for checking invading (i.e. old position outside and new position inside geometry) in
     * {@link #moveRelatesGeometry(Envelope, Geometry, Vector2D, Vector2D, byte)}
     */
    private static final byte   INVADES    = 2;

    /**
     * Checks whether the current velocity of a {@link Pedestrian} is bigger than the maximum
     * desired velocity, which could be defined as a speed limit for pedestrians. If the
     * {@code updatedVelocity} is bigger than the maximal, the velocity is re-computed using of the
     * maximum desired velocity.
     *
     * @param velocity is the current velocity of a {@link Pedestrian}
     * @param pedestrian an object of the {@link Pedestrian}
     *
     * @return the updated velocity of a {@link Pedestrian}
     */
    static Vector2D getValidatedVelocity(Pedestrian pedestrian, Vector2D velocity)
    {
        // check if updatedVelocity is bigger than maximal desired velocity
        double updatedVelocitySquared = MathTools.normSquared(velocity);

        // compare square products to gain some performance
        if (updatedVelocitySquared > pedestrian.getMaximumDesiredVelocity()
            * pedestrian.getMaximumDesiredVelocity())
        {
            velocity = MathTools.normalize(velocity.getX(), velocity.getY())
                .multiply(pedestrian.getMaximumDesiredVelocity());
        }

        return velocity;
    }

    /**
     * Checks whether the {@link Pedestrian} movement crosses any {@link Geometry}s of
     * {@link Boundary}s. This would mean that the {@link Pedestrian} would pass a wall during this
     * move. If this would be the case, the {@code oldPosition} is returned, otherwise the
     * {@code newPosition}
     *
     * @param pedestrian an object of the {@link Pedestrian}
     * @param boundaries {@link List} that contains all {@link Boundary} objects
     * @param oldPosition current position of the {@link Pedestrian}
     * @param newPosition next position of the {@link Pedestrian}, after the move
     *
     * @return the validated position of the {@link Pedestrian} (either {@code oldPosition} or
     *         {@code newPosition}).
     */
    static Vector2D validateMove(Pedestrian pedestrian, List<BoundarySegment> boundaries,
        Vector2D oldPosition, Vector2D newPosition)
    {
        if (boundaries != null && !boundaries.isEmpty())
        {
            for (BoundarySegment boundary : boundaries)
            {
                // this move would cross a boundary
                if (moveCrossesGeometry(boundary.getBoundingBox(), boundary.getGeometry(),
                    oldPosition, newPosition))
                {
                    logger.trace(
                        "NumericIntegrator.validateMove(), move intersects boundary, oldPosition="
                            + oldPosition + ", newPosition=" + newPosition);
                    pedestrian.getActiveWayFindingModel().setNeedsOrientation(true);
                    return oldPosition;
                }
            }
        }

        return newPosition;
    }

    /**
     * Checks, whether a move from {@code oldPosition} to {@code newPosition} crosses the
     * {@link Geometry} {@code geometry}. A quick check is performed using {@code boundingBox}
     * before an accurate check.
     *
     * @param boundingBox the bounding box of the given {@code geometry}
     * @param geometry the geometry of a {@link Boundary}
     * @param oldPosition current position of the {@link Pedestrian}
     * @param newPosition next position of the {@link Pedestrian}, after the current time step
     *
     * @return {@code true}, if a move from {@code oldPosition} to {@code newPosition} intersects
     *         with the {@link Geometry} {@code geometry}, {@code false} otherwise
     */
    private static boolean moveCrossesGeometry(Envelope boundingBox, Geometry geometry,
        Vector2D oldPosition, Vector2D newPosition)
    {
        return moveRelatesGeometry(boundingBox, geometry, oldPosition, newPosition, CROSSES);
    }

    /**
     * Checks, whether a move from {@code oldPosition} to {@code newPosition} intersects with the
     * {@link Geometry} {@code geometry}. A quick check is performed using {@code boundingBox}
     * before an accurate check.
     *
     * @param boundingBox the bounding box of the given {@code geometry}
     * @param geometry the geometry of a {@link Boundary}
     * @param oldPosition current position of the {@link Pedestrian}
     * @param newPosition next position of the {@link Pedestrian}, after the current time step
     *
     * @return {@code true}, if a move from {@code oldPosition} to {@code newPosition} intersects
     *         with the {@link Geometry} {@code geometry}, {@code false} otherwise
     */
    public static boolean moveIntersectsGeometry(Envelope boundingBox, Geometry geometry,
        Vector2D oldPosition, Vector2D newPosition)
    {
        return moveRelatesGeometry(boundingBox, geometry, oldPosition, newPosition, INTERSECTS);
    }

    /**
     * Checks, whether a move from {@code oldPosition} to {@code newPosition} will lead to the
     * {@link Pedestrian} to be invading into the given {@code boundingBox} (i.e. outside before and
     * inside after).
     *
     * @param boundingBox the bounding box of the given {@code geometry}
     * @param oldPosition previous position of the {@link Pedestrian}
     * @param newPosition next position of the {@link Pedestrian}
     *
     * @return {@code true}, if a move from {@code oldPosition} to {@code newPosition} intersects
     *         with the {@link Geometry} {@code geometry}, {@code false} otherwise
     */
    public static boolean moveInvadesBoundingBox(Envelope boundingBox, Vector2D oldPosition,
        Vector2D newPosition)
    {
        return moveRelatesGeometry(boundingBox, null, oldPosition, newPosition, INVADES);
    }

    /**
     * Checks, whether a move from {@code oldPosition} to {@code newPosition} relates with the
     * {@link Geometry} {@code geometry}. A quick check is performed using {@code boundingBox}
     * before an accurate check.
     *
     * @param boundingBox the bounding box of the given {@code geometry}
     * @param geometry the geometry of a {@link Boundary}
     * @param oldPosition current position of the {@link Pedestrian}
     * @param newPosition next position of the {@link Pedestrian}, after the current time step
     * @param mode {@link #INTERSECTS} for testing intersection, {@link #CROSSES} for testing
     *            crossing, {@link #INVADES} for testing invading the given {@code boundingBox}
     *
     * @return {@code true}, if a move from {@code oldPosition} to {@code newPosition} intersects
     *         with the {@link Geometry} {@code geometry}, {@code false} otherwise
     */
    private static boolean moveRelatesGeometry(Envelope boundingBox, Geometry geometry,
        Vector2D oldPosition, Vector2D newPosition, byte mode)
    {
        if (boundingBox == null)
            boundingBox = geometry.getEnvelopeInternal();
        if ( !(GeometryTools.intersects(oldPosition, newPosition, boundingBox)))
            return false;
        LineString path = JTSFactoryFinder.getGeometryFactory().createLineString(
            new Coordinate[] { oldPosition.toCoordinate(), newPosition.toCoordinate() });

        switch (mode)
        {
            case CROSSES:
                return geometry != null && path.crosses(geometry);
            case INTERSECTS:
                return geometry != null && path.intersects(geometry);
            case INVADES:
            {
                return !boundingBox.contains(oldPosition.getX(), oldPosition.getY())
                    && boundingBox.contains(newPosition.getX(), newPosition.getY());
            }
            default:
                return path.intersects(geometry);
        }
    }
}
