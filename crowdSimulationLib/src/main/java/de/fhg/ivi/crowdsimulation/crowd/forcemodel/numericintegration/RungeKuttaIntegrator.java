package de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration;

import java.util.List;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.math.Vector2D;

import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.boundaries.BoundarySegment;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.ForceModel;
import de.fhg.ivi.crowdsimulation.geom.Quadtree;
import de.fhg.ivi.crowdsimulation.math.MathTools;

/**
 * Runge-Kutta is one of many possible algorithms of numerical integration which can dissolve
 * ordinary differential equations, like the Social Force Model {@link ForceModel}. In this class
 * the Runge-Kutta algorithm of 4. order is used. For explanation see the links below.
 * <p>
 * Runge-Kutta explanation:<br>
 * http://pubsonline.informs.org/doi/pdf/10.1287/trsc.1040.0108<br>
 * http://itp.uni-frankfurt.de/~gros/StudentProjects/Applets_2014_PedestrianCrowdDynamics/PedestrianApplet.html<br>
 * https://scicomp.stackexchange.com/questions/20172/why-are-runge-kutta-and-eulers-method-so-different<br>
 *
 * @author hahmann/meinert
 */
public class RungeKuttaIntegrator extends NumericIntegrator
{

    /**
     * Overrides the abstract method in {@link NumericIntegrator} and sets this
     * {@link RungeKuttaIntegrator} as algorithm of numeric integration, which dissolves the
     * {@link ForceModel}.
     * <p>
     * Basically this class calculates the movement of a specific {@link Pedestrian}. This means
     * his/her new position and velocity, in dependence to his/her old velocity and the terms of the
     * Social Force Model (see Helbing et al. 2005), is computed.
     *
     * @param time the time in simulated time, given in milliseconds
     * @param simulationInterval the time between two consecutive moves, given in seconds
     * @param pedestrian the one {@link Pedestrian}, whose movement is calculated
     * @param quadtree the {@link Quadtree} object allowing to access {@link Boundary} objects
     *
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.NumericIntegrator#move(long,
     *      double, Pedestrian, Quadtree)
     */
    @Override
    public void move(long time, double simulationInterval, Pedestrian pedestrian, Quadtree quadtree)
    {
        double[] integrationIntervals = { 0, simulationInterval / 2, simulationInterval / 2,
            simulationInterval };
        Vector2D[] temporaryForces = new Vector2D[integrationIntervals.length];
        Vector2D[] temporaryVelocities = new Vector2D[integrationIntervals.length];

        // current position
        Vector2D currentPosition = pedestrian.getCurrentPositionVector();
        // current velocity
        Vector2D currentVelocity = pedestrian.getCurrentVelocityVector();

        // iterate over 4 Runge-Kutta steps
        for (int k = 0; k < 4; k++ )
        {
            // temporary position during this Runge-Kutta step
            Vector2D temporaryPosition;
            // temporary velocity during this Runge-Kutta step
            Vector2D temporaryVelocity = pedestrian.getCurrentVelocityVector();

            if (k == 0)
            {
                temporaryPosition = currentPosition;
            }
            // k > 0
            else
            {
                temporaryPosition = currentPosition
                    .add(temporaryVelocity.multiply(integrationIntervals[k]))
                    .add(temporaryForces[k - 1].multiply(0.5)
                        .multiply(integrationIntervals[k])
                        .multiply(integrationIntervals[k]));
                temporaryVelocity = temporaryVelocity
                    .add(temporaryForces[k - 1].multiply(integrationIntervals[k]));
            }

            // total acceleration on current pedestrian
            Vector2D resultingForce = pedestrian.getForces(temporaryPosition, temporaryVelocity,
                time);

            // set velocity to zero, if the intrinsic force is null
            if (MathTools.isZeroVector(resultingForce))
            {
                temporaryVelocity = new Vector2D(0d, 0d);
            }

            // add resulting temporary forces to array
            temporaryForces[k] = resultingForce;
            temporaryVelocities[k] = temporaryVelocity
                .add(resultingForce.multiply(integrationIntervals[k]));
        }

        // updatedVelocity = v(n+1)
        Vector2D updatedVelocity = currentVelocity
            .add((temporaryForces[0].add(temporaryForces[1].multiply(2d))
                .add(temporaryForces[2].multiply(2d))
                .add(temporaryForces[3])).multiply(1d / 6d).multiply(simulationInterval));

        // validate velocity
        updatedVelocity = NumericIntegrationTools.getValidatedVelocity(pedestrian, updatedVelocity);

        // update velocity
        pedestrian.setCurrentVelocityVector(updatedVelocity);

        // updatedPosition = x(n+1)
        Vector2D updatedPosition = currentPosition
            .add((temporaryVelocities[0].add(temporaryVelocities[1].multiply(2d))
                .add(temporaryVelocities[2].multiply(2d))
                .add(temporaryVelocities[3])).multiply(simulationInterval).multiply(1d / 6d));

        // validated updated position - guaranteed not to go through a boundary
        List<BoundarySegment> boundaries = null;
        if (quadtree != null)
            boundaries = quadtree.getBoundarySegments(
                new Envelope(currentPosition.toCoordinate(), updatedPosition.toCoordinate()));
        updatedPosition = NumericIntegrationTools.validateMove(pedestrian, boundaries,
            currentPosition, updatedPosition);

        // update position
        pedestrian.setCurrentPositionVector(updatedPosition);

        // update the mental model (e.g. check if the current WayPoint has been passed)
        pedestrian.getActiveWayFindingModel().updateModel(time, currentPosition, updatedPosition);

        // check for course deviations
        pedestrian.getActiveWayFindingModel().checkCourse(pedestrian, time);
    }
}
