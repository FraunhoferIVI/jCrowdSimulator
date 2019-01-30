package de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing;

import java.util.List;

import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.math.Vector2D;

import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.boundaries.BoundarySegment;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.ForceModel;
import de.fhg.ivi.crowdsimulation.geom.GeometryTools;
import de.fhg.ivi.crowdsimulation.math.MathTools;

/**
 * Implements formulas for the 3 forces described in the Social Forces model for pedestrian dynamics
 * by Helbing and Molnár (1995), Helbing et al. (2005):
 * <li>Pedestrian-Pedestrian interaction</li>
 * <li>Pedestrian-Boundary (obstacle) interaction</li>
 * <li>Intrinsic Force of a Pedestrian to move towards a certain target at a desired speed</li>
 * <p>
 * The Helbing Model requires 8 parameters: A1, B1, A2, B2 for pedestrian-pedestrian interaction and
 * A1, B1, A2, B2 for pedestrian-boundary (obstacle) interaction that need to be implemented by
 * sub-classes
 *
 * @see <a href=
 *      "https://journals.aps.org/pre/abstract/10.1103/PhysRevE.51.4282">https://journals.aps.org/pre/abstract/10.1103/PhysRevE.51.4282</a>
 * @see <a href=
 *      "https://pubsonline.informs.org/doi/abs/10.1287/trsc.1040.0108">https://pubsonline.informs.org/doi/abs/10.1287/trsc.1040.0108</a>
 * 
 *      TODO: add support for headed social force model (=extension of classic social force model)
 * @see <a href=
 *      "https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0169734">https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0169734</a>
 *
 * @author hahmann/meinert
 *
 */
public abstract class HelbingModel extends ForceModel
{

    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger logger = LoggerFactory.getLogger(HelbingModel.class);

    /**
     * Computes the force resulting from pedestrian-pedestrian interaction if the {@link Pedestrian}
     * is under the {@code Pedestrian#getMaxPedestrianInteractionDistance()} to other
     * {@link Pedestrian}.
     *
     * @param currentPosition denotes the approximated x, y position as {@link Vector2D} of the
     *            {@link Pedestrian}
     * @param currentVelocity denotes the velocity {@link Vector2D} of the {@link Pedestrian}
     * @param pedestrian the {@link Pedestrian} object
     *
     * @return the {@link Vector2D} vector which resulting from the interaction of the current
     *         {@link Pedestrian} with another {@code pedestrian}
     */
    @Override
    public Vector2D interactPedestrian(Vector2D currentPosition, Vector2D currentVelocity,
        Pedestrian pedestrian)
    {
        Vector2D forceDelta = new Vector2D(0, 0);

        // the following checks are useful, since the quadtree used to select the relevant
        // may return some outliers that are outside the interaction distance
        {
            if (Math.abs(pedestrian.getCurrentPositionVector().getX()
                - currentPosition.getX()) > getMaxPedestrianInteractionDistance())
            {
                return forceDelta;
            }
            if (Math.abs(pedestrian.getCurrentPositionVector().getY()
                - currentPosition.getY()) > getMaxPedestrianInteractionDistance())
            {
                return forceDelta;
            }
        }

        // exact check if a pedestrian is within the interaction distance of this pedestrian
        double distanceSquared = MathTools
            .normSquared(currentPosition.subtract(pedestrian.getCurrentPositionVector()));

        // 2 pedestrians lying exactly on top of each - this should be a rare case
        if (currentPosition.equals(pedestrian.getCurrentPositionVector()))
            distanceSquared = Double.MIN_VALUE;

        // TODO: perhaps quick check if other pedestrian is behind this pedestrian:
        // ep12 = distp12.Normalized();
        // double tmpv = ped1->GetV().ScalarProduct(ep12); // < v^0_i , e_ij >
        // double ped2IsBehindv = (tmpv <= 0) ? 0 : 1;

        // compare square product of distances to gain some performance - this ensures that no
        // unnecessary cases are computed even though and is a more precise detection of quadtree
        // outliers than above
        if (distanceSquared < getMaxPedestrianInteractionDistance()
            * getMaxPedestrianInteractionDistance())
        {

            // calculation of nVector
            Vector2D nVector = currentPosition.subtract(pedestrian.getCurrentPositionVector());

            // distance
            double distance = MathTools.norm(nVector);

            // 2 pedestrians lying exactly on top of each - this should be a rare case
            if (MathTools.isZeroVector(nVector))
                distance = Double.MIN_VALUE;

            forceDelta = interact(currentVelocity, nVector, getParameterPedestrianA1(),
                getParameterPedestrianB1(), getParameterPedestrianA2(), getParameterPedestrianB2(),
                getPedestrianRadius(), getPedestrianRadius(), distance,
                pedestrian.getCurrentVelocityVector());

            logger.trace("pedestrian force: " + forceDelta);
        }
        return forceDelta;
    }

    /**
     * Computes the force resulting from pedestrian-geometry interaction if the {@link Pedestrian}
     * is under the {@code Pedestrian#getMaxBoundaryInteractionDistance()} to {@link Geometry}.
     *
     * @param currentPosition denotes the approximated x, y position as {@link Vector2D} of the
     *            {@link Pedestrian}
     * @param boundary the {@link Geometry} object
     *
     * @return the {@link Vector2D} vector which resulting from the interaction of the current
     *         {@link Pedestrian} with a {@code geometry}
     */
    @Override
    public Vector2D interactBoundary(Vector2D currentPosition, BoundarySegment boundary)
    {
        Vector2D forceDelta = new Vector2D(0, 0);

        // bounding box based check, if the boundary is roughly within the interaction distance of
        // this pedestrian. useful, even if boundaries are queried using quadtree, since quadtree
        // based queries may return boundary objects outside the relevant interaction radius
        Envelope boundingBox = boundary.getBoundingBox();
        if ( !GeometryTools.isInside(currentPosition, boundingBox))
        {
            return forceDelta;
        }

        Coordinate currentPositionCoordinate = currentPosition.toCoordinate();
        Coordinate relevantInteractionCoordinate = GeometryTools
            .getNearestCoordinateOnGeometry(currentPositionCoordinate, boundary.getGeometry());

        double distanceSquared = GeometryTools.distanceSquared(currentPositionCoordinate,
            relevantInteractionCoordinate);

        // a pedestrian lying exactly on top of a boundary - this should be a rare case
        if (currentPositionCoordinate.equals(relevantInteractionCoordinate))
            distanceSquared = Double.MIN_VALUE;

        // compare distance squared to gain some performance
        if (distanceSquared < getMaxBoundaryInteractionDistance()
            * getMaxBoundaryInteractionDistance())
        {
            // actual distance
            double distance = GeometryTools.distance(currentPositionCoordinate,
                relevantInteractionCoordinate);

            // a pedestrian lying exactly on top of a boundary - this should be a rare case
            if (currentPositionCoordinate.equals(relevantInteractionCoordinate))
                distance = Double.MIN_VALUE;

            // calculation of nVector
            Vector2D vectorInteractingObject = new Vector2D(relevantInteractionCoordinate.x,
                relevantInteractionCoordinate.y);
            Vector2D nVector = (currentPosition.subtract(vectorInteractingObject));

            // forceDelta = interact(nVector, nUnitVector, 0.0f, getParameterBoundaryA2(),
            // getParameterBoundaryB2(), getPedestrianRadius(), distance, new Vector2D(0, 0),
            // new Vector2D(0, 0));

            forceDelta = interact(null, nVector, getParameterBoundaryA1(), getParameterBoundaryB1(),
                getParameterBoundaryA2(), getParameterBoundaryB2(), getPedestrianRadius(), 0,
                distance, null);
            logger.trace("boundary force: " + forceDelta);
        }

        return forceDelta;
    }

    /**
     * Computes the force resulting of interaction of a {@link Pedestrian} with either another
     * {@link Pedestrian} or a {@link Boundary} object.
     *
     * @param currentVelocity the current velocity {@link Vector2D} of the {@link Pedestrian}
     * @param nVector the {@link Vector2D} pointing from the other {@link Pedestrian} or
     *            {@link Boundary} to the current {@link Pedestrian}
     * @param parameterA1 parameter A1 in formular (8) Helbing et al. (2005)
     * @param parameterB1 parameter A1 in formular (8) Helbing et al. (2005)
     * @param parameterA2 parameter A1 in formular (8) Helbing et al. (2005)
     * @param parameterB2 parameter A1 in formular (8) Helbing et al. (2005)
     * @param interactionRadiusAlpha the radius of the {@link Pedestrian}
     * @param interactionRadiusBeta the radius of the other {@link Pedestrian} or the
     *            {@link Boundary}
     * @param distance the distance between the {@link Pedestrian} and the interacting object
     *            (either other {@link Pedestrian} or {@link Boundary})
     * @param currentVelocityBeta the velocity of the interacting object
     * @return the force {@link Vector2D} resulting from the interaction
     */
    private Vector2D interact(Vector2D currentVelocity, Vector2D nVector, double parameterA1,
        double parameterB1, double parameterA2, double parameterB2, double interactionRadiusAlpha,
        double interactionRadiusBeta, double distance, Vector2D currentVelocityBeta)
    {
        Vector2D forceDelta = new Vector2D(0, 0);
        Vector2D johanssonComponent = new Vector2D(0, 0);
        Vector2D buznaComponent = new Vector2D(0, 0);

        // normalized nVector
        Vector2D nUnitVector = MathTools.normalize(nVector.getX(), nVector.getY());
        if (MathTools.isZeroVector(nVector))
            nUnitVector = new Vector2D(Double.MIN_VALUE, Double.MIN_VALUE);

        // parameterA1 and parameterB1 are defined for johansson model, but they are 0 for buzna
        // model
        if (parameterA1 != 0 && parameterB1 != 0)
        {
            //
            Vector2D normalizedVelocityVector = MathTools.normalize(currentVelocity.getX(),
                currentVelocity.getY());

            // cosine value of the angle between the normalized distance vector pointing from
            // pedestrian beta to pedestrian alpha and the direction of motion of pedestrian alpha
            double cosinePhi = normalizedVelocityVector.dot(nUnitVector.multiply(1));

            // omega reflects that the reaction of pedestrians to what happens in front of them is
            // strong to what happens behind them
            double omega = getLambda() + (1d - getLambda()) * ((1d + cosinePhi) / 2d);

            // vector y in johansson elliptical II specification - representing the (relative) step
            // lengths of the pedestrians according to their current velocities
            Vector2D y = currentVelocityBeta.subtract(currentVelocity).multiply(getDeltaT());
            // euclidian length of y - can be assumed to be 0 in case of velocities are 0
            double normY = MathTools.isZeroVector(y) ? 0 : MathTools.norm(y);
            // euclidian length of nVector - yVector (intermediate result that is used several
            // times)
            double normNMinusY = MathTools.norm(nVector.subtract(y));

            // b = the minor axis of the ellipse
            double b = FastMath.sqrt(Math.pow(distance + normNMinusY, 2d) - Math.pow(normY, 2d))
                / 2d;

            // vector g in johansson elliptical II specification
            double g1 = parameterA1 * Math.exp( -b / parameterB1);
            double g2 = (distance + normNMinusY) / (2d * b);
            Vector2D g3 = nUnitVector.add(nVector.subtract(y).multiply(1d / normNMinusY))
                .multiply(1d / 2d);
            Vector2D g = g3.multiply(g1).multiply(g2);

            // the johansson component
            johanssonComponent = g.multiply(omega);
        }
        // parameterA2 and parameterB2 are defined for buzna model, but they are 0 for johansson
        // model
        if (parameterA2 != 0 && parameterB2 != 0)
        {
            buznaComponent = nUnitVector.multiply(parameterA2 * Math
                .exp((interactionRadiusAlpha + interactionRadiusBeta - distance) / parameterB2));
        }

        forceDelta = buznaComponent.add(johanssonComponent);
        logger.trace("interact2 force: " + forceDelta);

        return forceDelta;
    }

    public static void main(String[] args)
    {
        Vector2D forceDelta = new Vector2D(0, 0);
        Vector2D johanssonComponent = new Vector2D(0, 0);
        Vector2D buznaComponent = new Vector2D(0, 0);

        // alpha minus beta
        Vector2D nVector = new Vector2D(1, 0);

        double parameterA1 = 0.04d;
        double parameterB1 = 3.22d;
        double lambda = 0.06d;
        Vector2D currentVelocityAlpha = new Vector2D( -1, -1);
        Vector2D currentVelocityBeta = new Vector2D( -1, -1);
        Vector2D normalizedVelocityVector = currentVelocityAlpha.normalize();
        double deltaT = 0.5d;
        double distance = 1;
        double parameterA2 = 0d;
        double parameterB2 = 0d;
        double interactionRadiusAlpha = 0.3f;
        double interactionRadiusBeta = 0.3f;

        // normalized nVector
        Vector2D nUnitVector = nVector.multiply(1 / MathTools.norm(nVector));

        // parameterA1 and parameterB1 are defined for johansson model, but they are 0 for buzna
        // model
        if (parameterA1 != 0 && parameterB1 != 0)
        {
            // cosine value of the angle between the normalized distance vector point from
            // pedestrian beta to pedestrian alpha and the direction of motion of pedestrian
            // alpha
            double cosinePhi = normalizedVelocityVector.dot(nUnitVector);

            // omega reflects that the reaction of pedestrians to what happens in front of them
            // is
            // strong to what happens behind them
            double omega = lambda + (1d - lambda) * ((1d + cosinePhi) / 2d);

            // vector y in johansson elliptical II specification - representing the (relative)
            // step
            // lengths of the pedestrians according to their current velocities
            Vector2D y = currentVelocityBeta.subtract(currentVelocityAlpha).multiply(deltaT);
            // euclidian length of y - can be assumed to be 0 in case of velocities are 0
            double normY = MathTools.isZeroVector(y) ? 0 : MathTools.norm(y);
            // euclidian length of nVector - yVector (intermediate result that is used several
            // times)
            double normNMinusY = MathTools.norm(nVector.subtract(y));

            // b = the minor axis of the ellipse
            double b = FastMath.sqrt(Math.pow(distance + normNMinusY, 2d) - Math.pow(normY, 2d))
                / 2d;

            // vector g in johansson elliptical II specification
            double g1 = parameterA1 * Math.exp( -b / parameterB1);
            double g2 = (distance + normNMinusY) / (2d * b);
            Vector2D g3 = nUnitVector.add(nVector.subtract(y).multiply(1d / normNMinusY))
                .multiply(1d / 2d);
            Vector2D g = g3.multiply(g1).multiply(g2);

            // the johansson component
            johanssonComponent = g.multiply(omega);
            // TODO multiply(1) ???
            johanssonComponent = johanssonComponent.multiply(1);
        }
        // parameterA2 and parameterB2 are defined for buzna model, but they are 0 for johansson
        // model
        if (parameterA2 != 0 && parameterB2 != 0)
        {
            buznaComponent = nUnitVector.multiply(parameterA2 * Math
                .exp((interactionRadiusAlpha + interactionRadiusBeta - distance) / parameterB2));
        }

        forceDelta = buznaComponent.add(johanssonComponent);

        System.out.println("HelbingModel.main(), forceDelta=" + forceDelta);
    }

    /**
     * Gets the acceleration that is needed to reach the desired velocity and go in the desired
     * direction.
     *
     * @param currentPosition the current x, y position as {@link Vector2D} of the
     *            {@link Pedestrian}
     * @param currentVelocity the current velocity of the {@link Pedestrian}
     * @param normalizedDirectionVector the direction {@link Vector2D} in which the
     *            {@link Pedestrian} walks.
     * @param averageVelocity the average velocity of the {@link Pedestrian} into its desired
     *            direction of motion
     * @param preferredVelocity the preferred velocity of the {@link Pedestrian}, i.e. the velocity
     *            that the {@link Pedestrian} intrinsically would like to have
     * @param maximumDesiredVelocity the maximal desired velocity of the {@link Pedestrian}.
     *
     * @return the {@link Vector2D} force component that is needed to reach the desired velocity in
     *         the desired direction.
     */
    @Override
    public Vector2D intrinsicForce(Vector2D currentPosition, Vector2D currentVelocity,
        Vector2D normalizedDirectionVector, float averageVelocity, float preferredVelocity,
        float maximumDesiredVelocity)
    {
        // to avoid division by zero - intrinsic force is assumed to be a zero vector, if the
        // prefered velocity of the pedestrian is zero
        if (preferredVelocity == 0)
        {
            return new Vector2D(0, 0);
        }

        // this is the implementation of formula (5) and (6) of Helbing et al. (2005)
        float currentDesiredVelocity = averageVelocity
            + (1f - averageVelocity / preferredVelocity) * maximumDesiredVelocity;

        // the actual force computation
        Vector2D resultingForce = normalizedDirectionVector.multiply(currentDesiredVelocity)
            .subtract(currentVelocity)
            .multiply(1 / getTau());

        if (Double.isNaN(resultingForce.getX()))
        {
            logger.info("resultingForce=" + resultingForce + ", normalizedDirectionVector="
                + normalizedDirectionVector + ", currentDesiredVelocity=" + currentDesiredVelocity
                + ", currentVelocity=" + currentVelocity + ", preferredVelocity="
                + preferredVelocity);
        }

        return resultingForce;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector2D interactGroup(Vector2D currentPosition, Vector2D currentVelocity,
        Coordinate groupCentroid, List<Coordinate> groupPositions)
    {
        // the group force is irrelevant for groups of size 1 and if the pedestrian is not moving
        int groupSize = groupPositions.size();
        if (groupSize == 1 || MathTools.isZeroVector(currentVelocity))
            return new Vector2D(0, 0);

        Vector2D positionToCenter = new Vector2D(groupCentroid.x, groupCentroid.y)
            .subtract(currentPosition);
        Vector2D unitVectorPositionToCenter = MathTools.normalize(positionToCenter.getX(),
            positionToCenter.getY());

        // visual field force component
        double phi = 90;
        double beta1 = 4;
        // That is to say, the dot product of two vectors will be equal to the cosine of the angle
        // between the vectors, times the lengths of each of the vectors.
        double cosineAlpha = MathTools.normalize(currentVelocity.getX(), currentVelocity.getY())
            .dot(unitVectorPositionToCenter);
        float aCosAlpha = MathTools.acos((float) cosineAlpha);
        double alpha = Math.toDegrees(aCosAlpha);
        alpha -= phi;
        Vector2D forceVisualField = currentVelocity.multiply(alpha).multiply( -beta1);

        // group attraction force component
        double threshold = (groupSize - 1d) / 2d;
        double distancePositionToCenter = MathTools.norm(positionToCenter);
        Vector2D forceAttraction = new Vector2D(0, 0);
        if (distancePositionToCenter > threshold)
        {
            double beta2 = 3;
            forceAttraction = unitVectorPositionToCenter.multiply(beta2);
        }

        // group repulsion force component
        Vector2D forceRepulsion = new Vector2D(0, 0);
        double beta3 = 1;
        double minimumDistance = 0.8;
        for (Coordinate groupMemberPosition : groupPositions)
        {
            Vector2D groupMemberPositionVector = new Vector2D(groupMemberPosition.x,
                groupMemberPosition.y);
            if (currentPosition.equals(groupMemberPositionVector))
                continue;
            Vector2D positionToGroupMember = groupMemberPositionVector.subtract(currentPosition);
            double distanceToGroupMember = MathTools.norm(positionToGroupMember);
            if (distanceToGroupMember < minimumDistance)
            {
                Vector2D positionToGroupMemberUnitVector = MathTools
                    .normalize(positionToGroupMember.getX(), positionToGroupMember.getY());
                forceRepulsion.add(positionToGroupMemberUnitVector.multiply(beta3));
            }
        }

        Vector2D groupForce = forceVisualField.add(forceAttraction).add(forceRepulsion);

        logger.trace("groupForce=" + groupForce + ", forceVisualField=" + forceVisualField
            + ", forceAttraction=" + forceAttraction + ", forceRepulsion=" + forceRepulsion);

        return groupForce;
    }

    /**
     * Gets the strength parameter A1 of the acceleration resulting from the interaction of a
     * {@link Pedestrian} with another {@link Pedestrian} given in m/s�.
     *
     * @return the strength parameter A1 of the acceleration resulting from the interaction of a
     *         {@link Pedestrian} with another {@link Pedestrian} given in m/s�
     */
    public abstract float getParameterPedestrianA1();

    /**
     * Gets the B2 Parameter of the interaction range with another {@link Pedestrian}. Given in
     * meters. Parameterizes the private zone of a {@link Pedestrian}.
     *
     * @return the B2 Parameter of the interaction range. Given in meters.
     */
    public abstract float getParameterPedestrianB1();

    /**
     * Gets the strength parameter A2 of the acceleration resulting from the interaction of a
     * {@link Pedestrian} with another {@link Pedestrian}. Given in m/s². cf. Helbing et al (2005)
     * p. 12.
     *
     * @return the strength parameter A1 of the acceleration resulting from the interaction of a
     *         {@link Pedestrian} with another {@link Pedestrian}. Given in m/s²
     */
    public abstract float getParameterPedestrianA2();

    /**
     * Gets the B2 Parameter of the interaction range with another {@link Pedestrian}. Given in
     * meters. Parameterizes the private zone of a {@link Pedestrian}.
     *
     * @return the B2 Parameter of the interaction range. Given in meters.
     */
    public abstract float getParameterPedestrianB2();

    /**
     * Gets the strength parameter A1 of the acceleration resulting from the interaction of a
     * {@link Pedestrian} with a {@link Boundary} given in m/s�.
     *
     * @return the strength parameter A1 of the acceleration resulting from the interaction of a
     *         {@link Pedestrian} with a {@link Boundary} given in m/s�
     */
    public abstract float getParameterBoundaryA1();

    /**
     * Gets the B2 Parameter of the interaction range with a {@link Boundary}. Given in meters.
     * Parameterizes the comfort zone of a {@link Pedestrian}.
     *
     * @return the B2 Parameter of the interaction range. Given in meters.
     */
    public abstract float getParameterBoundaryB1();

    /**
     * Gets the strength parameter A2 of the acceleration resulting from the interaction of a
     * {@link Pedestrian} with a {@link Boundary}. Given in m/s�. cf. Helbing et al (2005) p. 12.
     *
     * @return the strength parameter A1 of the acceleration resulting from the interaction of a
     *         {@link Pedestrian} with a {@link Boundary}. Given in m/s�
     */
    public abstract float getParameterBoundaryA2();

    /**
     * Gets the B2 Parameter of the interaction range with a {@link Boundary}. Given in meters.
     * Parameterizes the comfort zone of a {@link Pedestrian}
     *
     * @return the B2 Parameter of the interaction range. Given in meters.
     */
    public abstract float getParameterBoundaryB2();

    /**
     * Gets the "relaxation time". Deviations of the actual velocity from the desired velocity due
     * to disturbances (by obstacles or avoidance maneuvers) are corrected within the so-called
     * �relaxation time� "Relaxation time" of a {@link Pedestrian}. Given in seconds. Cf. Helbing
     * (2005), p. 11
     *
     * @return the "Relaxation time" of a {@link Pedestrian}. Given in seconds.
     */
    public abstract float getTau();

    /**
     * Gets the parameter that takes into account the anisotropic character of pedestrian
     * interactions, as the situation in front of a pedestrian has a larger impact on his or her
     * behavior than things happening behind. Cf. Helbing (2005), p. 13.
     *
     * @return the parameter that takes into account the anisotropic character of pedestrian
     *         interactions
     */
    public abstract float getLambda();

    /**
     * Gets the parameter, which denotes the step size of a {@link Pedestrian}. It's used in the
     * elliptical specification of the HelbingJohansson Model (not in the HelbingBuzna Model). Cf.
     * Johansson (2007).
     *
     * @return the parameter which denotes the step size of a {@link Pedestrian}
     */
    public abstract float getDeltaT();

    /**
     * Gets the radius of a {@link Pedestrian}. Given in meters.
     *
     * @return the radius of a {@link Pedestrian}. Given in meters.
     */
    @Override
    public float getPedestrianRadius()
    {
        return 0.3f;
    }

    /**
     * Computes the distance, in which a {@link Pedestrian} interacts with another
     * {@link Pedestrian}. The calculation formula is the formula of the interact() converted to the
     * distance.
     * <p>
     * This distance depends on given constants and the resulting force limitResultingForce. This
     * variable is set to 0.01 because at this limit value is the force change on the pedestrians
     * negligible.
     *
     * @return distance, given in meters, in which the {@link Pedestrian} interacts with another
     *         {@link Pedestrian}
     *
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.ForceModel#getMaxPedestrianInteractionDistance()
     */
    @Override
    public float getMaxPedestrianInteractionDistance()
    {
        // do not repeat calculation, if value is already set
        if (maxPedestrianInteractionDistance != 0)
            return maxPedestrianInteractionDistance;

        // Distinction between the modeling approaches
        if (getParameterPedestrianA1() == 0 && getParameterPedestrianB1() == 0)
        {
            maxPedestrianInteractionDistance = getMaxInteractionDistance(getParameterPedestrianA2(),
                getParameterPedestrianB2());
        }
        else
        {
            maxPedestrianInteractionDistance = getMaxInteractionDistance(getParameterPedestrianA1(),
                getParameterPedestrianB1());
        }

        return maxPedestrianInteractionDistance;
    }

    /**
     * Computes the distance, in which a {@link Pedestrian} interacts with a {@link Boundary}. The
     * calculation formula is the formula of the interact() converted to the distance.
     * <p>
     * This distance depends on given constants and the resulting force limitResultingForce. This
     * variable is set to 0.01 because at this limit, it is assumed that the force effect on the
     * pedestrians negligible.
     *
     * @return distance, given in meters, in which the {@link Pedestrian} interacts with a
     *         {@link Boundary}
     *
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.ForceModel#getMaxBoundaryInteractionDistance()
     */
    @Override
    public float getMaxBoundaryInteractionDistance()
    {
        // do not repeat calculation, if value is already set
        if (maxBoundaryInteractionDistance != 0)
            return maxBoundaryInteractionDistance;

        // only boundary parameters A2 and B2 are used, independently of the modeling approach
        maxBoundaryInteractionDistance = getMaxInteractionDistance(getParameterBoundaryA2(),
            getParameterBoundaryB2());

        return maxBoundaryInteractionDistance;
    }

    /**
     * Calculates the maximal distance in which the interaction between a {@link Pedestrian} and a
     * {@link Boundary} or another {@link Pedestrian} has an appreciable effect.
     *
     * @param parameterA the strength parameter of the acceleration resulting from the interaction
     *            of a {@link Pedestrian} with a {@link Boundary} or another {@link Pedestrian}.
     * @param parameterB interaction range between a {@link Pedestrian} and another
     *            {@link Pedestrian} or a {@link Boundary}. Parameterizes the private zone of a
     *            {@link Pedestrian}.
     *
     * @return distance, given in meters, in which the {@link Pedestrian} interacts with a
     *         {@link Boundary} or another {@link Pedestrian}
     */
    private float getMaxInteractionDistance(float parameterA, float parameterB)
    {
        return getPedestrianRadius()
            - parameterB * (float) Math.log((limitResultingForce / parameterA));
    }
}
