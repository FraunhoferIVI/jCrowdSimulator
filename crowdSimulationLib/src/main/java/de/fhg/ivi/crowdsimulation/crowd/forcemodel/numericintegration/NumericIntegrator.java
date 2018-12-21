package de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration;

import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.ForceModel;
import de.fhg.ivi.crowdsimulation.geom.Quadtree;

/**
 * Algorithms of numeric integrations are necessary to compute ordinary differential equations as
 * exists the the Social Force Model (SFM) respectively {@link ForceModel}. There are many
 * algorithms, which can compute the SFM, e.g. Runge-Kutta, Semi-Implicit Euler and Simple Euler.
 * <p>
 * This class is an abstract. Sub-classes need to implement the
 * {@link #move(long, double, Pedestrian, Quadtree)} method.
 *
 * @author hahmann/meinert
 */
public abstract class NumericIntegrator
{

    /**
     * Calculates the new position and velocity of the given {@code pedestrian} based on the given
     * {@code time}, {@code simulationInterval}, other Pedestrians ({@code pedestrians}, boundaries
     * and {@link ForceModel}
     *
     * @param time the time in simulated time, given in milliseconds
     * @param simulationInterval the time between two consecutive moves, given in seconds
     * @param pedestrian the one {@link Pedestrian}, whose movement is calculated * @param quadtree
     * @param quadtree {@link Quadtree} object allowing to access {@link Boundary} objects
     */
    public abstract void move(long time, double simulationInterval, Pedestrian pedestrian,
        Quadtree quadtree);
}
