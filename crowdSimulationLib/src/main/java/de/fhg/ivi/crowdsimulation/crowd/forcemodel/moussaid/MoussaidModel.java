package de.fhg.ivi.crowdsimulation.crowd.forcemodel.moussaid;

import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.math.Vector2D;

import de.fhg.ivi.crowdsimulation.boundaries.BoundarySegment;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.ForceModel;

/**
 * TODO: This implementation is incomplete at the moment. Check the paper and adapt this class. Also
 * compare and check with PEDSIM (because they are using Moussaid as well)
 * <p>
 * This MoussaidModel is another empirical approach of the Social Force Model, based on the
 * publications of Helbing and Molnár (1995) and Helbing et al. (2005). This model itself based on
 * the publication Moussaïd et al. (2009) (see the hyperlink below).
 * <p>
 * A description of the necessary parameters will follow, when the model will be implemented
 * completely.
 *
 * @see <a href=
 *      "https://www.ncbi.nlm.nih.gov/pmc/articles/PMC2839952/">https://www.ncbi.nlm.nih.gov/pmc/articles/PMC2839952/</a>
 *
 * @author hahmann/meinert
 */
public class MoussaidModel extends ForceModel
{

    @Override
    public Vector2D intrinsicForce(Vector2D currentPosition, Vector2D currentVelocity,
        Vector2D normalizedDirectionVector, float averageVelocityOnRoute,
        float actualDesiredVelocity, float maximumDesiredVelocity)
    {
        return null;
    }

    @Override
    public Vector2D interactPedestrian(Vector2D currentPosition, Vector2D currentVelocity,
        Pedestrian pedestrian)
    {
        return null;
    }

    @Override
    public Vector2D interactBoundary(Vector2D currentPosition, BoundarySegment boundary)
    {
        return null;
    }

    @Override
    public float getMaxBoundaryInteractionDistance()
    {
        return 0;
    }

    @Override
    public float getMaxPedestrianInteractionDistance()
    {
        return 0;
    }

    @Override
    public float getPedestrianRadius()
    {
        return 0.2f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector2D interactGroup(Vector2D currentPosition, Vector2D currentVelocity,
        Coordinate groupCentroid, List<Coordinate> groupPositions)
    {
        return new Vector2D(0, 0);
    }

    /**
     * Computes the force resulting from pedestrian-geometry interaction if the {@link Pedestrian}
     * is under a certain {@code distance} to {@link Geometry}.
     * <p>
     * The algorithm which is used in the following method is used in the agent based simulation
     * software PedSIM.
     * <p>
     * Note: It seems to be that this calculation doesn't work for the force generation in this
     * simulation. The reason could be that the other force calculators, like interactPedestrians,
     * based on other parameters than this.
     *
     * @param boundary the {@link Geometry} object
     * @param currentPosition denotes the approximated x, y position as {@link Vector2D} of the
     *            {@link Pedestrian}
     *
     * @return the {@link Vector2D} vector which resulting from the interaction of the current
     *         {@link Pedestrian} with a {@code geometry}
     */
    /*
     * private Vector2D interactBoundaryPedSim(Geometry boundary, Vector2D currentPosition) {
     * Vector2D forceDelta = new Vector2D(0, 0); Coordinate currentPositionCoordinate = new
     * Coordinate(currentPosition.x(), currentPosition.y()); Coordinate
     * relevantInteractionCoordinate = GeometryTools .getNearestPoint(currentPositionCoordinate,
     * boundary);
     *
     * // calculation of nVector Vector2D vectorInteractingObject = new
     * Vector2D(relevantInteractionCoordinate.x, relevantInteractionCoordinate.y); Vector2D nVector
     * = (currentPosition.minus(vectorInteractingObject));
     *
     * double distance = currentPositionCoordinate.distance(relevantInteractionCoordinate);
     *
     * ////////////////// PEDSIM Formula for Boundaries //////////////////////////
     *
     * // there is no great difference in the formula itself, but definitely in the outcome // //
     * not forget, that the part of forceDelta after the .plus is the only part of this equation //
     * which is used at the moment // // the 2 in front of the radius (forceDelta) makes no sense //
     * // so in the end. The formulas are nearly the same. The only difference are the parameters.
     * // but that is a very big difference.
     *
     * double parameterObstacle = 0.8;
     *
     * Vector2D forceDeltaPedSim = nVector .times(Math.exp((PedestrianConstants.radius - distance) /
     * parameterObstacle));
     *
     * logger.debug("Pedestrian.interact(), forceDelta: " + forceDelta);
     * logger.info("Pedestrian.interact(), forceDeltaPedSim: " + forceDeltaPedSim);
     *
     * // if you want a summary of the effect from the viewpoint of the pedsim formula // (1) the
     * force is higher if a big distance is between the Pedestrian and the Boundary // (2) but in
     * the near of the Boundary the force is not almost that high that the forceDelta // can be //
     * all in all one could say the pedsim formula is more balanced // --> in my opinion this is a
     * nice effect (better choice)
     *
     * ////////////////////////////////////////////////////////////////////////////
     *
     * return forceDeltaPedSim; }
     */
}
