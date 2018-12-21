package de.fhg.ivi.crowdsimulation.boundaries;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;

/**
 * A {@link Boundary} consists on a {@link Geometry}, which could be e.g. points, lines or polygons,
 * which are obstacles for the movement of the {@link Pedestrian}s.
 * <p>
 * Based on this a {@link Boundary} is a repulsive force in case of the Social Force Model. In the
 * formula of Helbing et al. (2005) this is represented through the second term of the common
 * formula. Look at p. 11, 12 and formula 3, 7 (in this publication) for understanding.
 * <p>
 * Otherwise this class encapsulates the {@link Geometry} object and the caches the bounding box of
 * it for fast access.
 *
 * @author hahmann/meinert
 */
public class Boundary
{
    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger logger = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The {@link Geometry} object of this {@link Boundary}.
     */
    private Geometry            geometry;

    /**
     * The {@link Geometry} object containing a version of {@link #geometry} with an applied buffer
     * of .
     */
    private Geometry            bufferedGeometry;

    private List<LineString>    lineStrings;

    /**
     * Distance value to be applied on {@link #geometry} to get {@link #bufferedGeometry}.
     * <p>
     * Defines the minimum distance to a Boundary that {@link Pedestrian} will use to look for
     * target points. Given in meters.
     */
    private double              boundaryDistance;

    /**
     * Sets the distance value to be applied on {@link #geometry} to get {@link #bufferedGeometry}.
     * <p>
     * Reason behind this is to provide a version of the {@link #geometry} of this Boundary that
     * allows to consider some distance (={@code boundaryDistance}) to the Boundary. Given in
     * meters.
     *
     * @param boundaryDistance the distance given in meters
     */
    public void setBoundaryDistance(double boundaryDistance)
    {
        this.boundaryDistance = boundaryDistance;
    }

    /**
     * Caches {@link Geometry#getEnvelopeInternal()} of {@link Boundary#geometry} expanded by the
     * maximum distance of interaction between a {@link Pedestrian} and a {@link Boundary}
     */
    private Envelope boundingBox;

    /**
     * Creates a new {@link Geometry} of a {@link Boundary} object.
     * <p>
     * Furthermore computes an {@link Envelope}, which is computed from the {@code geometry} plus
     * the given {@code maxBoundaryInteractionDistance}.
     *
     * @param geometry defines the outline of the {@link Boundary}
     * @param maxBoundaryInteractionDistance maximum distance of interaction between a
     *            {@link Pedestrian} and a {@link Boundary}
     */
    public Boundary(Geometry geometry, double maxBoundaryInteractionDistance)
        throws GeometryNotValidException
    {
        this(geometry, maxBoundaryInteractionDistance, 0);
    }

    /**
     * Creates a new {@link Geometry} of a {@link Boundary} object.
     * <p>
     * Furthermore computes an {@link Envelope}, which is computed from the {@code geometry} plus
     * the given {@code maxBoundaryInteractionDistance}.
     *
     * @param geometry defines the outline of the {@link Boundary}
     * @param maxBoundaryInteractionDistance maximum distance of interaction between a
     *            {@link Pedestrian} and a {@link Boundary}
     * @param boundaryDistance the minimum distance to a Boundary that {@link Pedestrian} will use
     *            to look for target points. Given in meters.
     */
    public Boundary(Geometry geometry, double maxBoundaryInteractionDistance,
        double boundaryDistance) throws GeometryNotValidException
    {
        // can happen, if POLYGON EMPTY or MULTIPOLYGON EMPTY geometries are given
        if (geometry == null)
            throw new GeometryNotValidException("Geometry is null, which is not supported.");
        // can happen, if POLYGON EMPTY or MULTIPOLYGON EMPTY geometries are given
        if (geometry.getNumPoints() == 0)
            throw new GeometryNotValidException(
                "Geometries with 0 are points are not supported. geometry = " + geometry);
        if ( !geometry.isValid())
        {
            logger
                .info("geometry is invalid. trying to apply buffer with zero size to fix. geometry="
                    + geometry);
            geometry = geometry.buffer(0);
        }
        this.geometry = geometry;

        // split geometry into individual linestrings
        // TODO this could be a method in GeometryTools
        for (int i = 1; i < geometry.getCoordinates().length; i++ )
        {
            LineString lineString = JTSFactoryFinder.getGeometryFactory()
                .createLineString(new Coordinate[] { geometry.getCoordinates()[i - 1],
                    geometry.getCoordinates()[i] });
            // lineStrings.add(lineString);
        }
        this.boundaryDistance = boundaryDistance;
        this.boundingBox = geometry.getEnvelopeInternal();
        this.boundingBox.expandBy(maxBoundaryInteractionDistance);
    }

    /**
     * Gets the {@link Geometry} of this {@link Boundary} object.
     *
     * @return the {@link Geometry} of this {@link Boundary}
     */
    public Geometry getGeometry()
    {
        return geometry;
    }

    /**
     * Gets a version of {@link #geometry} buffered by {@link #boundaryDistance}.
     * <p>
     * Reason behind this is to provide a version of the {@link Geometry} of this Boundary that
     * allows to considers some distance (={@code boundaryDistance}) to the Boundary. Given in
     * meters.
     */
    public Geometry getBufferedGeometry()
    {
        if (bufferedGeometry == null && boundaryDistance > 0)
        {
            bufferedGeometry = geometry.buffer(boundaryDistance);
            boundingBox.expandToInclude(bufferedGeometry.getEnvelopeInternal());
        }
        return bufferedGeometry;
    }

    /**
     * Sets the {@link Geometry} of this {@link Boundary} object.
     *
     * @param geometry the {@link Geometry} of this {@link Boundary}
     */
    public void setGeometry(Geometry geometry)
    {
        this.geometry = geometry;
    }

    /**
     * Gets a cached version of {@link Geometry#getEnvelopeInternal()} of {@link Boundary#geometry}
     * expanded by the maximum distance of interaction between a {@link Pedestrian} and a
     * {@link Boundary}.
     *
     * @return a cached version of {@link Geometry#getEnvelopeInternal()} of
     *         {@link Boundary#geometry} expanded by the maximum distance of interaction between a
     *         {@link Pedestrian} and a {@link Boundary}
     */
    public Envelope getBoundingBox()
    {
        return boundingBox;
    }
}
