package de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration;

import java.util.List;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.math.Vector2D;

import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.ForceModel;
import de.fhg.ivi.crowdsimulation.geom.Quadtree;

/**
 * Simple Euler or Forward Euler is one of many possible algorithms of numerical integration which
 * can dissolve ordinary differential equations, like the Social Force Model {@link ForceModel}. The
 * difference between this class and the {@link SemiImplicitEulerIntegrator} is, that the position
 * of the {@link Pedestrian} is updated in the next time step in this class. For further explanation
 * see the links below.
 * <p>
 * Euler explanation: <br>
 * http://people.physik.hu-berlin.de/~mitdank/dist/scriptenm/Eulerintegration.htm<br>
 * https://en.wikipedia.org/wiki/Euler_method<br>
 * http://tutorial.math.lamar.edu/Classes/DE/EulersMethod.aspx<br>
 * also look into the PedSim library<br>
 * https://scicomp.stackexchange.com/questions/20172/why-are-runge-kutta-and-eulers-method-so-different<br>
 *
 * @author hahmann/meinert
 *
 */
public class SimpleEulerIntegrator extends NumericIntegrator
{

    /**
     * Overrides the abstract method in {@link NumericIntegrator} and sets this
     * {@link SimpleEulerIntegrator} as algorithm of numeric integration, which dissolves the
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
        // old position
        Vector2D currentPosition = pedestrian.getCurrentPositionVector();

        // update position = x(n+1)
        Vector2D updatedPosition = pedestrian.getCurrentPositionVector()
            .add(pedestrian.getCurrentVelocityVector().multiply(simulationInterval));

        // validated updated position - guaranteed not to go through a boundary
        List<Boundary> boundaries = null;
        if (quadtree != null)
            boundaries = quadtree.getBoundaries(
                new Envelope(currentPosition.toCoordinate(), updatedPosition.toCoordinate()));
        updatedPosition = NumericIntegrationTools.validateMove(pedestrian, boundaries,
            currentPosition, updatedPosition);

        // update current Position
        pedestrian.setCurrentPositionVector(updatedPosition);

        // update the mental model (e.g. check if the current WayPoint has been passed)
        pedestrian.getActiveWayFindingModel().updateModel(time, currentPosition, updatedPosition);

        // check for course deviations
        pedestrian.getActiveWayFindingModel().checkCourse(pedestrian, time);

        // compute resulting force
        Vector2D resultingForce = pedestrian.getForces(time);

        // set velocity to zero, if the intrinsic force is null
        Vector2D currentVelocity = pedestrian.getCurrentVelocityVector();

        // updatedVelocity = v(n+1) (t+1)
        Vector2D updatedVelocity = currentVelocity.add(resultingForce.multiply(simulationInterval));

        // check, if velocity is not too high
        updatedVelocity = NumericIntegrationTools.getValidatedVelocity(pedestrian, updatedVelocity);

        // update current Velocity
        pedestrian.setCurrentVelocityVector(updatedVelocity);
    }
}
