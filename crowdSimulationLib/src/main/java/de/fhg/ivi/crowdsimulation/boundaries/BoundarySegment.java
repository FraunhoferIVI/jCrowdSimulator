package de.fhg.ivi.crowdsimulation.boundaries;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;

/**
 * A {@link BoundarySegment} is a part of a {@link Boundary} object. It references a
 * {@link Geometry} object, which can be a {@link Point} or a {@link LineString} and a bounding box
 *
 * @author hahmann
 *
 */
public class BoundarySegment
{

    /**
     * Uses the object logger for printing specific messages in the console.
     */
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The {@link Geometry} object of this {@link Boundary}.
     */
    private Geometry            geometry;

    /**
     * Caches {@link Geometry#getEnvelopeInternal()} of {@link BoundarySegment#geometry} expanded by
     * the maximum distance of interaction between a {@link Pedestrian} and a {@link Boundary}
     */
    private Envelope            boundingBox;

    /**
     * Creates a new {@link BoundarySegment} object.
     * <p>
     * Furthermore computes an {@link Envelope}, which is computed from the {@code geometry}
     * expanded by the given {@code maxBoundaryInteractionDistance}.
     *
     * @param geometry defines the geometry of the {@link BoundarySegment}
     * @param maxBoundaryInteractionDistance maximum distance of interaction between a
     *            {@link Pedestrian} and a {@link Boundary}
     */
    public BoundarySegment(Geometry geometry, double maxBoundaryInteractionDistance)
    {
        this.geometry = geometry;
        this.boundingBox = geometry.getEnvelopeInternal();
        this.boundingBox.expandBy(maxBoundaryInteractionDistance);
    }

    /**
     * Gets the {@link Geometry} of this {@link BoundarySegment} object.
     *
     * @return the {@link Geometry} of this {@link BoundarySegment}
     */
    public Geometry getGeometry()
    {
        return geometry;
    }

    /**
     * Gets a cached version of {@link Geometry#getEnvelopeInternal()} of {@link #geometry} expanded
     * by the maximum distance of interaction between a {@link Pedestrian} and a
     * {@link BoundarySegment}.
     *
     * @return a cached version of {@link Geometry#getEnvelopeInternal()} of {@link #geometry}
     *         expanded by the maximum distance of interaction between a {@link Pedestrian} and a
     *         {@link BoundarySegment}
     */
    public Envelope getBoundingBox()
    {
        return boundingBox;
    }

}
