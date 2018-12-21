package de.fhg.ivi.crowdsimulation.geom;

import de.fhg.ivi.crowdsimulation.CrowdSimulator;
import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;

/**
 * Interface that allows implementing classes to get and set {@link Quadtree} objects.
 * <p>
 * A {@link Quadtree} object allows (efficient) access to {@link Boundary} objects and
 * {@link Pedestrian} objects belonging to the same {@link CrowdSimulator} and makes
 * Pedestrians-Boundary and Pedestrian-Pedestrian interaction possible.
 *
 * @author hahmann
 *
 */
public interface QuadtreeAccess
{

    /**
     * Gets the {@link Quadtree} object from classes implementing this interface. The
     * {@link Quadtree} object allows (efficient) access to {@link Boundary} objects and
     * {@link Pedestrian} objects belonging to the same {@link CrowdSimulator} and makes
     * Pedestrians-Boundary and Pedestrian-Pedestrian interaction possible.
     * <p>
     *
     * @return the {@link Quadtree} object
     */
    public Quadtree getQuadtree();

    /**
     * Sets the {@link Quadtree} object assigned to classes implementing this interface. The
     * {@link Quadtree} object allows (efficient) access to {@link Boundary} objects and
     * {@link Pedestrian} objects belonging to the same {@link CrowdSimulator} and makes
     * Pedestrians-Boundary and Pedestrian-Pedestrian interaction possible.
     * <p>
     *
     * @param quadtree the {@link Quadtree} object
     */
    void setQuadtree(Quadtree quadtree);
}
