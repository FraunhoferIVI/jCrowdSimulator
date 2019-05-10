package de.fhg.ivi.crowdsimulation.geom;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.boundaries.BoundarySegment;
import de.fhg.ivi.crowdsimulation.crowd.Crowd;
import de.fhg.ivi.crowdsimulation.crowd.ICrowd;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;

/**
 * A Quadtree is a spatial index structure for efficient range querying of items bounded by 2D
 * rectangles. {@link Geometry}s can be indexed by using their {@link Envelope}s. Any type of Object
 * can also be indexed as long as it has an extent that can be represented by an {@link Envelope}.
 * <p>
 * This Quadtree index provides a <b>primary filter</b> for range rectangle queries. The various
 * query methods return a list of all items which <i>may</i> intersect the query rectangle. Note
 * that it may thus return items which do <b>not</b> in fact intersect the query rectangle. A
 * secondary filter is required to test for actual intersection between the query rectangle and the
 * envelope of each candidate item. The secondary filter may be performed explicitly, or it may be
 * provided implicitly by subsequent operations executed on the items (for instance, if the index
 * query is followed by computing a spatial predicate between the query geometry and tree items, the
 * envelope intersection check is performed automatically.
 * <p>
 * This implementation does not require specifying the extent of the inserted items beforehand. It
 * will automatically expand to accomodate any extent of dataset.
 * <p>
 * This data structure is also known as an <i>MX-CIF quadtree</i> following the terminology of Samet
 * and others.
 * <p>
 * This class encapsulates two independent tree structures. One ({@link #quadtreeBoundaries}) is
 * intended two keep {@link Boundary} objects and another one ({@link #quadtreePedestrians}) is
 * intended to keep {@link Pedestrian} objects
 *
 * @author hahmann
 *
 */
public class Quadtree
{

    /**
     * Uses the object logger for printing specific messages in the console.
     */
    @SuppressWarnings("unused")
    private static final Logger                            logger = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());

    /**
     * A {@link Quadtree} to index {@link Pedestrian} objects
     */
    private org.locationtech.jts.index.quadtree.Quadtree quadtreePedestrians;

    /**
     * A {@link Quadtree} to index {@link Boundary} objects
     */
    private org.locationtech.jts.index.quadtree.Quadtree quadtreeBoundaries;

    /**
     * A {@link Quadtree} to index segments of {@link Boundary} objects
     */
    private org.locationtech.jts.index.quadtree.Quadtree quadtreeBoundarySegments;

    /**
     * Creates a new {@link Quadtree} object
     */
    public Quadtree()
    {

    }

    /**
     * Updates this {@link Quadtree}, i.e. all {@link Pedestrian} contained in the given list of
     * {@code pedestrians} are added to a newly created tree structure, which means all
     * {@link Pedestrian} objects previously contained will not be available anymore after calling
     * this method.
     *
     * @param pedestrians a {@link List} of {@link Pedestrian} objects are added to this
     *            {@link Quadtree}
     */
    private void updatePedestrians(List<Pedestrian> pedestrians)
    {
        quadtreePedestrians = new org.locationtech.jts.index.quadtree.Quadtree();
        if (pedestrians != null && !pedestrians.isEmpty())
        {
            for (Pedestrian pedestrian : pedestrians)
            {
                quadtreePedestrians.insert(pedestrian.getEnvelope(), pedestrian);
            }
        }
    }

    /**
     * Updates this {@link Quadtree}, i.e. all {@link Pedestrian} contained in the given list of
     * {@code crowds} are added to a newly created tree structure, which means all
     * {@link Pedestrian} objects previously contained will not be available anymore after calling
     * this method. <b>This only add cloned {@link Pedestrian} objects to the quadtree</b>
     *
     * @param crowds a {@link List} of {@link Crowd} objects whose {@link Pedestrian} objects are
     *            added to tree structure for {@link Pedestrian} objects
     *            ({@link #quadtreePedestrians})
     */
    public void updateCrowds(List<? extends ICrowd> crowds)
    {
        // collect all pedestrians from all crowds, make copies of them and then add to the quadtree
        // of pedestrians
        List<Pedestrian> allPedestrians = new ArrayList<>();
        if (crowds != null && !crowds.isEmpty())
        {
            for (ICrowd crowd : crowds)
            {
                allPedestrians.addAll(crowd.getPedestrians(true));
            }
        }
        updatePedestrians(allPedestrians);
    }

    /**
     * Adds all {@link Boundary} objects contained in the given list of {@code boundaries} to the
     * existing tree structure of boundaries ({@link #quadtreeBoundaries}). {@link Boundary} objects
     * that are already contained in the existing tree structure will not be removed from the tree
     * structure. Boundary objects with invalid {@link Geometry} are tried to be fixed by applying
     * {@link Geometry#buffer(double)} with zero size buffer.
     *
     * @param boundaries a {@link List} of {@link Boundary} objects that are added to tree structure
     *            for {@link Boundary} objects
     */
    public void addBoundaries(List<Boundary> boundaries)
    {
        if (quadtreeBoundaries == null)
            quadtreeBoundaries = new org.locationtech.jts.index.quadtree.Quadtree();
        if (quadtreeBoundarySegments == null)
            quadtreeBoundarySegments = new org.locationtech.jts.index.quadtree.Quadtree();
        if (boundaries != null && !boundaries.isEmpty())
        {
            for (Boundary boundary : boundaries)
            {
                quadtreeBoundaries.insert(boundary.getBoundingBox(), boundary);
                if (boundary.getBoundarySegments() != null
                    && boundary.getBoundarySegments().size() > 0)
                {
                    for (BoundarySegment segment : boundary.getBoundarySegments())
                    {
                        quadtreeBoundarySegments.insert(segment.getBoundingBox(), segment);
                    }
                }
            }
        }
    }

    /**
     * Queries the {@link Quadtree} and returns {@link Pedestrian} objects which <b>may</b> lie in
     * the given search envelope. Precisely, the items that are returned are all items in the tree
     * whose envelope <b>may</b> intersect the search Envelope. Note that some items with
     * non-intersecting envelopes may be returned as well; the client is responsible for filtering
     * these out. In most situations there will be many items in the tree which do not intersect the
     * search envelope and which are not returned - thus providing improved performance over a
     * simple linear scan. <b>This only returns cloned {@link Pedestrian} objects</b>
     *
     * @param searchEnvelope the envelope of the desired query area.
     * @return a List of {@link Pedestrian} which may intersect the search envelope or {@code null},
     *         if {@link #quadtreePedestrians} is {@code null}.
     */
    public List<Pedestrian> getPedestrians(Envelope searchEnvelope)
    {
        if (quadtreePedestrians == null)
            return null;
        if (searchEnvelope == null)
            return null;
        return quadtreePedestrians.query(searchEnvelope);
    }

    /**
     * Gets the number of {@link Pedestrian} objects contained in this {@link Quadtree}
     *
     * @return the number of {@link Pedestrian} objects contained in this {@link Quadtree}
     */
    public int getNumberOfPedestrians()
    {
        return quadtreePedestrians.size();
    }

    /**
     * Queries the {@link Quadtree} and returns {@link Boundary} objects which <b>may</b> lie in the
     * given search envelope. Precisely, the items that are returned are all items in the tree whose
     * envelope <b>may</b> intersect the search Envelope. Note that some items with non-intersecting
     * envelopes may be returned as well; the client is responsible for filtering these out. In most
     * situations there will be many items in the tree which do not intersect the search envelope
     * and which are not returned - thus providing improved performance over a simple linear scan.
     *
     * @param searchEnvelope the envelope of the desired query area.
     * @return a List of {@link Boundary} which may intersect the search envelope or {@code null},
     */
    public List<Boundary> getBoundaries(Envelope searchEnvelope)
    {
        if (quadtreeBoundaries == null)
            return null;
        if (searchEnvelope == null)
            return null;
        return quadtreeBoundaries.query(searchEnvelope);
    }

    /**
     * Queries the {@link Quadtree} and returns segments of {@link Boundary} objects which
     * <b>may</b> lie in the given search envelope. Precisely, the items that are returned are all
     * items in the tree whose envelope <b>may</b> intersect the search Envelope. Note that some
     * items with non-intersecting envelopes may be returned as well; the client is responsible for
     * filtering these out. In most situations there will be many items in the tree which do not
     * intersect the search envelope and which are not returned - thus providing improved
     * performance over a simple linear scan.
     *
     * @param searchEnvelope the envelope of the desired query area.
     * @return a List of segments of {@link Boundary} objects, which may intersect the search
     *         envelope or {@code null},
     */
    public List<BoundarySegment> getBoundarySegments(Envelope searchEnvelope)
    {
        if (quadtreeBoundarySegments == null)
            return null;
        if (searchEnvelope == null)
            return null;
        return quadtreeBoundarySegments.query(searchEnvelope);
    }
}
