package de.fhg.ivi.crowdsimulation.crowd;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.math.Vector2D;

import de.fhg.ivi.crowdsimulation.CrowdSimulator;
import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.boundaries.BoundarySegment;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.ForceModel;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.NumericIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.RungeKuttaIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.SemiImplicitEulerIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.SimpleEulerIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.WayFindingModel;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.FollowRouteModel;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.Route;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.WayPoint;
import de.fhg.ivi.crowdsimulation.geom.Quadtree;
import de.fhg.ivi.crowdsimulation.geom.QuadtreeAccess;
import de.fhg.ivi.crowdsimulation.math.MathTools;

/**
 * A {@link Pedestrian} is initially represented through his/her initial x- and y-position, initial
 * desired velocity, maximum desired velocity, starting time and a {@link List} of {@link WayPoint},
 * which defines his/her route in dependence to the {@link WayFindingModel}.
 * <p>
 * Farther this class calculates the forces which have an impact on the movement behavior of a
 * {@link Pedestrian}. These forces are:
 *
 * <li>intrinsic forces of each {@link Pedestrian}, see
 * {@link Pedestrian#intrinsicForce(Vector2D, Vector2D, long)}</li>
 * <li>an extrinsic force resulting of the repulsion between {@link Pedestrian} and
 * {@link Boundary}, see {@link Pedestrian#interactBoundaries(Vector2D)}</li>
 * <li>an extrinsic force resulting of the repulsion between a {@link Pedestrian} and another
 * {@link Pedestrian}, see {@link Pedestrian#interactPedestrians(Vector2D, Vector2D)}</li>
 *
 * <p>
 *
 * @author hahmann/meinert
 */
public class Pedestrian implements Clusterable, Cloneable, QuadtreeAccess, Identifiable
{

    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger logger = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Identification of this {@link Pedestrian}. Required for {@link Cloneable} interface.
     */
    private int                 id;

    /**
     * The {@link Group} that this {@link Pedestrian} belongs to.
     */
    private Group               group;

    /**
     * Current x, y position as {@link Vector2D} of the {@link Pedestrian}.
     */
    private Vector2D            currentPositionVector;

    /**
     * Current velocity as {@link Vector2D} of the {@link Pedestrian}.
     */
    private Vector2D            currentVelocityVector;

    /**
     * The desired velocity that this {@link Pedestrian} initially wants to reach (without being
     * "delayed") given in m/s.
     */
    private float               currentNormalDesiredVelocity;

    /**
     * The maximal desired velocity the {@link Pedestrian} can reach (when being "delayed") given in
     * m/s.
     */
    private float               maximumDesiredVelocity;

    /**
     * Is the resulting force, of all acting forces, which denotes the {@link Pedestrian} movement.
     */
    private Vector2D            totalForce;

    /**
     * Force vector that results from the intrinsic behavior of the {@link Pedestrian} to move
     * towards its current target with a certain speed.
     */
    private Vector2D            intrinsicForce;

    /**
     * Force vector that results from the interaction of this {@link Pedestrian} with all
     * surrounding {@link Pedestrian} objects.
     */
    private Vector2D            forceInteractionWithPedestrians;

    /**
     * Force Vector that results from the interaction of this {@link Pedestrian} with all
     * surrounding {@link Boundary} objects.
     */
    private Vector2D            forceInteractionWithBoundaries;

    /**
     * Force Vector that result from the interaction of this {@link Pedestrian} with all members of
     * the {@link Group} it belongs to.
     *
     * <a href=
     * "http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0010047">http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0010047</a>
     */
    private Vector2D            forceInteractionWithGroup;

    /**
     * Sum of all {@link #totalForce} extrinsic forces (currently
     * {@link #forceInteractionWithBoundaries} and {@link #forceInteractionWithPedestrians}) that
     * influence the movement of this {@link Pedestrian}.
     */
    private Vector2D            totalExtrinsicForces;

    /**
     * The currently active of the {@link WayFindingModel}
     */
    private WayFindingModel     activeWayFindingModel;

    /**
     * Can be one of the following methods of numerical mathematics to compute {@link Pedestrian}
     * movement - Simple Euler {@link SimpleEulerIntegrator}, Semi Implicit Euler
     * {@link SemiImplicitEulerIntegrator} or Runge Kutta {@link RungeKuttaIntegrator}.
     */
    private NumericIntegrator   numericIntegrator;

    /**
     * {@link ForceModel} objects, which represents the force model for pedestrian movement
     */
    private ForceModel          forceModel;

    /**
     * {@link Quadtree} object, which can be use to do spatial queries
     */
    private Quadtree            quadtree;

    /**
     * Creates a new {@link Pedestrian} object and initializes the variables the {@link Pedestrian}
     * needs to know to realize his/her movement.
     *
     * @param initialPositionX x component of the position of a {@link Pedestrian} at the time of
     *            its creation
     * @param initialPositionY y component of the position of a {@link Pedestrian} at the time of
     *            its creation
     * @param normalDesiredVelocity the desired velocity which a {@link Pedestrian} wants to reach
     *            (without being "delayed") in normal situations. Given in m/s
     * @param maximumDesiredVelocity the maximal desired velocity a {@link Pedestrian} can reach
     *            (when being "delayed") given in m/s
     * @param forceModel objects, which represents the force model for pedestrian movement
     * @param numericIntegrator Can be one of the following methods of numerical mathematics to
     *            compute {@link Pedestrian} movement - Simple Euler {@link SimpleEulerIntegrator},
     *            Semi Implicit Euler {@link SemiImplicitEulerIntegrator} or Runge Kutta
     *            {@link RungeKuttaIntegrator}
     * @param quadtree the quadtree object
     */
    Pedestrian(double initialPositionX, double initialPositionY, float normalDesiredVelocity,
        float maximumDesiredVelocity, ForceModel forceModel, NumericIntegrator numericIntegrator,
        Quadtree quadtree)
    {
        this(0, initialPositionX, initialPositionY, normalDesiredVelocity, maximumDesiredVelocity,
            forceModel, numericIntegrator, quadtree);
    }

    /**
     * Creates a new {@link Pedestrian} object and initializes the variables the {@link Pedestrian}
     * needs to know to realize his/her movement.
     *
     * @param id the id of this {@link Pedestrian} <b>(required to be unique amongst all
     *            {@link Pedestrian} objects within the same {@link CrowdSimulator}</b>
     * @param initialPositionX x component of the position of a {@link Pedestrian} at the time of
     *            its creation
     * @param initialPositionY y component of the position of a {@link Pedestrian} at the time of
     *            its creation
     * @param normalDesiredVelocity the desired velocity which a {@link Pedestrian} wants to reach
     *            (without being "delayed") in normal situations. Given in m/s
     * @param maximumDesiredVelocity the maximal desired velocity a {@link Pedestrian} can reach
     *            (when being "delayed") given in m/s
     * @param forceModel objects, which represents the force model for pedestrian movement
     * @param numericIntegrator Can be one of the following methods of numerical mathematics to
     *            compute {@link Pedestrian} movement - Simple Euler {@link SimpleEulerIntegrator},
     *            Semi Implicit Euler {@link SemiImplicitEulerIntegrator} or Runge Kutta
     *            {@link RungeKuttaIntegrator}
     * @param quadtree the quadtree object
     */
    protected Pedestrian(int id, double initialPositionX, double initialPositionY,
        float normalDesiredVelocity, float maximumDesiredVelocity, ForceModel forceModel,
        NumericIntegrator numericIntegrator, Quadtree quadtree)
    {
        this.id = id;
        // set velocities
        setCurrentNormalDesiredVelocity(normalDesiredVelocity);
        setMaximumDesiredVelocity(maximumDesiredVelocity);

        // set initial position, current position, current velocity
        setCurrentPositionVector(new Vector2D(initialPositionX, initialPositionY));
        setCurrentVelocityVector(new Vector2D(0, 0));

        // way finding model
        activeWayFindingModel = new FollowRouteModel(null, getCurrentPositionVector(), quadtree);

        // force model and numeric integrator
        this.forceModel = forceModel;
        this.numericIntegrator = numericIntegrator;
        this.quadtree = quadtree;
    }

    /**
     * Implementation of {@link Cloneable} interface. Creates new instances of
     * {@link #currentPositionVector}, {@link #currentVelocityVector}.
     *
     * Does not create deep clones of force vectors, {@link #activeWayFindingModel}
     *
     * @see java.lang.Object#clone()
     */
    @Override
    public Pedestrian clone()
    {
        try
        {
            Pedestrian clone = (Pedestrian) super.clone();
            clone.setCurrentPositionVector(new Vector2D(this.getCurrentPositionVector()));
            clone.setCurrentVelocityVector(new Vector2D(this.getCurrentVelocityVector()));
            clone.setCurrentNormalDesiredVelocity(this.getCurrentNormalDesiredVelocity());
            clone.setMaximumDesiredVelocity(this.getMaximumDesiredVelocity());
            return clone;
        }
        catch (CloneNotSupportedException e)
        {
            logger.error("Pedestrian.clone(), ", e);
        }
        return null;
    }

    /**
     * Tests if two Pedestrians equal each other using {@link #getId()} method
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if ( !Pedestrian.class.isAssignableFrom(obj.getClass()))
        {
            return false;
        }
        final Pedestrian other = (Pedestrian) obj;
        if ((this.id == 0) ? (other.getId() != 0) : this.id != other.getId())
        {
            return false;
        }
        return true;
    }

    /**
     * Generates a hash value of this {@link Pedestrian}.
     *
     * @return the hash value of this pedestrian
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Quadtree getQuadtree()
    {
        return this.quadtree;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setQuadtree(Quadtree quadtree)
    {
        this.quadtree = quadtree;
        if (activeWayFindingModel == null)
            return;
        activeWayFindingModel.setQuadtree(quadtree);
    }

    /**
     * Gets the {@link #currentPositionVector} as {@link Clusterable}.
     *
     * @see org.apache.commons.math3.ml.clustering.Clusterable#getPoint()
     */
    @Override
    public double[] getPoint()
    {
        return new double[] { currentPositionVector.getX(), currentPositionVector.getY() };
    }

    /**
     * {@inheritDoc}
     * <p>
     * Required for the implementation of the {@link Cloneable} interface.
     */
    @Override
    public int getId()
    {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setId(int id)
    {
        this.id = id;
    }

    /**
     * Gets the current position of this {@link Pedestrian} as an {@link Envelope} object
     *
     * @return the {@link Envelope} of the current position of the {@link Pedestrian}
     */
    public Envelope getEnvelope()
    {
        return new Envelope(getCurrentPositionCoordinate());
    }

    /**
     * Gets the {@link #currentPositionVector} of a {@link Pedestrian}.
     *
     * @return the {@link #currentPositionVector}
     */
    public Vector2D getCurrentPositionVector()
    {
        return currentPositionVector;
    }

    /**
     * Gets the current position of a {@link Pedestrian} as {@link Coordinate}
     *
     * @return the current position of this {@link Pedestrian} as {@link Coordinate}
     */
    public Coordinate getCurrentPositionCoordinate()
    {
        return currentPositionVector.toCoordinate();
    }

    /**
     * Sets the current x, y position as {@link Vector2D} of the {@link Pedestrian}.
     *
     * @param currentPositionVector denotes the approximated x, y position as {@link Vector2D} of
     *            the {@link Pedestrian}
     */
    public void setCurrentPositionVector(Vector2D currentPositionVector)
    {
        // if (currentPositionVector == null || Double.isNaN(currentPositionVector.x()))
        // {
        // Thread.dumpStack();
        // System.exit(0);
        // }
        this.currentPositionVector = currentPositionVector;
    }

    /**
     * Gets the {@link #currentVelocityVector}.
     *
     * @return the {@link #currentVelocityVector}
     */
    public Vector2D getCurrentVelocityVector()
    {
        return currentVelocityVector;
    }

    /**
     * Gets the current velocity of this {@link Pedestrian} in m/s. The velocity is derived from
     * {@link #getCurrentVelocityVector()}.
     *
     * @return the current velocity of {@link Pedestrian}.
     */
    public double getCurrentVelocity()
    {
        return MathTools.norm(getCurrentVelocityVector());
    }

    /**
     * Sets the {@link #currentVelocityVector}, which describes the current velocity
     * {@link Vector2D} of the {@link Pedestrian}.
     *
     * @param currentVelocity the current velocity {@link Vector2D} of the {@link Pedestrian}
     */
    public void setCurrentVelocityVector(Vector2D currentVelocity)
    {
        this.currentVelocityVector = currentVelocity;
    }

    /**
     * Sets the velocity that this {@link Pedestrian} currently wants to reach (without being
     * "delayed") given in m/s. Cf. Helbing et al (2005) p. 11.
     *
     * @param currentNormalDesiredVelocity the velocity that this {@link Pedestrian} currently wants
     *            to reach (without being "delayed")
     */
    public void setCurrentNormalDesiredVelocity(float currentNormalDesiredVelocity)
    {
        if (currentNormalDesiredVelocity < 0)
            currentNormalDesiredVelocity = 0;
        this.currentNormalDesiredVelocity = currentNormalDesiredVelocity;
    }

    /**
     * Gets the velocity that this {@link Pedestrian} currently wants to reach (without being
     * "delayed") given in m/s. Cf. Helbing et al (2005) p. 11.
     *
     * @return the velocity that this {@link Pedestrian} currently wants to reach (without being
     *         "delayed")
     */
    public float getCurrentNormalDesiredVelocity()
    {
        return currentNormalDesiredVelocity;
    }

    /**
     * Gets the {@link #maximumDesiredVelocity}.
     *
     * @return the {@link #maximumDesiredVelocity}
     */
    public float getMaximumDesiredVelocity()
    {
        return maximumDesiredVelocity;
    }

    /**
     * Sets the {@link #maximumDesiredVelocity}, i.e. the velocity that this {@link Pedestrian} can
     * reach (in case "delayed") given in m/s.
     *
     * @param maximumDesiredVelocity the maximum velocity the {@link Pedestrian} can reach
     */
    public void setMaximumDesiredVelocity(float maximumDesiredVelocity)
    {
        if (maximumDesiredVelocity < 0)
            maximumDesiredVelocity = 0;
        this.maximumDesiredVelocity = maximumDesiredVelocity;
    }

    /**
     * Sets the {@link List} of {@link WayPoint} objects this {@link Pedestrian} should follow.
     *
     * @param route the route object that contains the {@link List} of {@link WayPoint} objects to
     *            be followed by this {@link Pedestrian}
     * @param time the time when a this {@link Crowd} get a the new List of {@link WayPoint}
     *            assuming that it starts immediately towards the new route.
     */
    public void setRoute(Route route, long time)
    {
        if (activeWayFindingModel instanceof FollowRouteModel)
        {
            ((FollowRouteModel) activeWayFindingModel).setStartPosition(getCurrentPositionVector());
            ((FollowRouteModel) activeWayFindingModel).setRoute(route, getCurrentPositionVector());
            activeWayFindingModel.setStartTime(time);
        }
    }

    /**
     * Gets the force vector that results from the intrinsic behavior of the {@link Pedestrian} to
     * move towards its current target with a certain speed
     *
     * <b>Please note: this methods only returns the value of the force, without re-computing it.
     * For re-computing {@link #getForces(Vector2D, Vector2D, long)} or {@link #getForces(long)}
     * needs to be called.</b>
     *
     * @return the current intrinsic vector.
     */
    public Vector2D getIntrinsicForce()
    {
        return intrinsicForce;
    }

    /**
     * Gets the current force vector that results from the interaction of this {@link Pedestrian}
     * with all surrounding {@link Pedestrian} objects.
     *
     * <b>Please note: this methods only returns the value of the force, without re-computing it.
     * For re-computing {@link #getForces(Vector2D, Vector2D, long)} or {@link #getForces(long)}
     * needs to be called.</b>
     *
     * @return the current force vector for Pedestrian-Pedestrian interaction.
     */
    public Vector2D getForceInteractionWithPedestrians()
    {
        return forceInteractionWithPedestrians;
    }

    /**
     * Gets the current force Vector that results from the interaction of this {@link Pedestrian}
     * with all surrounding {@link Boundary} objects
     *
     * <b>Please note: this methods only returns the value of the force, without re-computing it.
     * For re-computing {@link #getForces(Vector2D, Vector2D, long)} or {@link #getForces(long)}
     * needs to be called.</b>
     *
     * @return the current force Vector for Pedestrian-Boundary interaction.
     */
    public Vector2D getForceInteractionWithBoundaries()
    {
        return forceInteractionWithBoundaries;
    }

    /**
     * Gets the Sum of all extrinsic forces (i.e. {@link #forceInteractionWithBoundaries} and
     * {@link #forceInteractionWithPedestrians}) that influence the movement of this
     * {@link Pedestrian}.
     *
     * Please note: this methods only returns the value of the force, without re-computing it. For
     * re-computing {@link #getForces(Vector2D, Vector2D, long)} needs to be called.
     *
     * @return the sum of all extrinsic forces from Pedestrian-Pedestrian and Pedestrian-Boundary
     *         interaction.
     */
    public Vector2D getTotalExtrinsicForces()
    {
        return totalExtrinsicForces;
    }

    /**
     * Gets the Sum of all forces (i.e. {@link #totalExtrinsicForces} and {@link #intrinsicForce})
     * that influence the movement of this {@link Pedestrian}.
     *
     * Please note: this methods only returns the value of the force, without re-computing it. For
     * re-computing {@link #getForces(Vector2D, Vector2D, long)} needs to be called.
     *
     * @return the sum of all forces from Pedestrian-Pedestrian and Pedestrian-Boundary interaction
     *         and intrinsic force.
     */
    public Vector2D getTotalForce()
    {
        return totalForce;
    }

    /**
     * Gets the currently active {@link WayFindingModel}
     *
     * @return the active {@link WayFindingModel}
     */
    public WayFindingModel getActiveWayFindingModel()
    {
        return activeWayFindingModel;
    }

    /**
     * Sets the {@link Group} to which this {@link Pedestrian} belongs
     *
     * @param group the {@link Group} to which this {@link Pedestrian} belongs
     */
    public void setGroup(Group group)
    {
        this.group = group;
    }

    /**
     * Computes and updates acceleration that is needed to reach the desired velocity and go in the
     * desired direction.
     *
     * @param currentPosition denotes the approximated x, y position as {@link Vector2D} of the
     *            {@link Pedestrian}
     * @param currentVelocity the approximated velocity of the {@link Pedestrian}
     * @param currentTime current system time in milliseconds
     *
     * @return the {@link Vector2D} vector which resulting from the self-driven velocity and
     *         direction of pedestrian
     */
    private Vector2D intrinsicForce(Vector2D currentPosition, Vector2D currentVelocity,
        long currentTime)
    {
        float tempAverageVelocity = activeWayFindingModel.getAverageVelocity(currentPosition,
            currentTime, false);
        // at the beginning no average velocity cannot be computed due to division by zero - this
        // leads to Float.NaN
        float averageVelocity = !Float.isNaN(tempAverageVelocity) ? tempAverageVelocity
            : currentNormalDesiredVelocity;

        Vector2D normalizedDirectionVector = activeWayFindingModel.getNormalizedDirectionVector();

        // the actual force computation
        Vector2D resultingForce = forceModel.intrinsicForce(currentPosition, currentVelocity,
            normalizedDirectionVector, averageVelocity, currentNormalDesiredVelocity,
            maximumDesiredVelocity);

        return resultingForce;
    }

    /**
     * Computes and returns the force resulting from pedestrian-pedestrian interaction.
     *
     * @param currentPosition denotes the approximated x, y position as {@link Vector2D} of the
     *            {@link Pedestrian}
     * @param currentVelocity denotes the velocity {@link Vector2D} of the {@link Pedestrian}
     *
     * @return the {@link Vector2D} vector which describes the force resulting of
     *         pedestrian-pedestrian interaction
     */
    private Vector2D interactPedestrians(Vector2D currentPosition, Vector2D currentVelocity)
    {
        Vector2D resultingForce = new Vector2D(0, 0);

        Envelope searchEnvelope = getEnvelope();
        searchEnvelope.expandBy(forceModel.getMaxPedestrianInteractionDistance());
        List<Pedestrian> pedestrians = null;
        if (quadtree != null)
        {
            pedestrians = quadtree.getPedestrians(searchEnvelope);
        }

        if (pedestrians != null && !pedestrians.isEmpty())
        {
            for (Pedestrian pedestrian : pedestrians)
            {
                if ( !this.equals(pedestrian))
                {
                    resultingForce = resultingForce.add(forceModel
                        .interactPedestrian(currentPosition, currentVelocity, pedestrian));
                }
            }
        }
        return resultingForce;
    }

    /**
     * Computes and returns the force resulting from pedestrian-boundary interaction.
     *
     * @param currentPosition denotes the approximated x, y position as {@link Vector2D} of the
     *            {@link Pedestrian}
     *
     * @return the {@link Vector2D} vector which describes the force resulting of
     *         pedestrian-geometry interaction
     */
    private Vector2D interactBoundaries(Vector2D currentPosition)
    {
        Vector2D resultingForce = new Vector2D(0, 0);

        Envelope searchEnvelope = getEnvelope();
        searchEnvelope.expandBy(forceModel.getMaxPedestrianInteractionDistance());
        List<BoundarySegment> boundaries = null;
        if (quadtree != null)
            boundaries = quadtree.getBoundarySegments(searchEnvelope);

        if (boundaries != null && !boundaries.isEmpty())
        {
            for (BoundarySegment boundary : boundaries)
            {
                // original calculation method
                resultingForce = resultingForce
                    .add(forceModel.interactBoundary(currentPosition, boundary));
            }
        }

        return resultingForce;
    }

    /**
     * Gets the force Vector that results from the interaction of this {@link Pedestrian} with all
     * members of the {@link Group} it belongs to.
     *
     * @param currentPosition the current position of this Pedestrian
     * @param currentVelocity the current velocity of this Pedestrian
     * @return the force resulting from group interaction
     *
     *         <a href=
     *         "http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0010047">http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0010047</a>
     */
    private Vector2D interactGroup(Vector2D currentPosition, Vector2D currentVelocity)
    {
        return forceModel.interactGroup(currentPosition, currentVelocity, group.getCentroid(),
            group.getPositions());
    }

    /**
     * Computes and adds the resulting forces out of all forces which influence the behavior of the
     * {@link Pedestrian}s. Part of this forces are the own forces to go with a certain velocity and
     * the two repulsive forces in dependency of the {@code boundaries} and other
     * {@link Pedestrian}s.
     *
     * @param currentTime is the current system time stamp
     *
     * @return the resulting force out of all involved forces as an {@link Vector2D}
     */
    public Vector2D getForces(long currentTime)
    {
        return getForces(getCurrentPositionVector(), getCurrentVelocityVector(), currentTime);
    }

    /**
     * Computes and adds the resulting forces out of all forces which influence the behavior of the
     * {@link Pedestrian}s. Part of this forces are the own forces to go with a certain velocity and
     * the two repulsive forces in dependency of the {@code boundaries} and other
     * {@link Pedestrian}s.
     *
     * @param currentPosition describes the current position of a {@link Pedestrian}
     * @param currentVelocity the current velocity of the {@link Pedestrian}
     * @param currentTime is the current system time stamp
     *
     * @return the resulting force out of all involved forces as an {@link Vector2D}
     */
    public Vector2D getForces(Vector2D currentPosition, Vector2D currentVelocity, long currentTime)
    {
        // "self-interaction" to reach desired velocity
        intrinsicForce = intrinsicForce(currentPosition, currentVelocity, currentTime);

        // interaction with other pedestrians
        forceInteractionWithPedestrians = interactPedestrians(currentPosition, currentVelocity);

        // interaction with boundaries
        forceInteractionWithBoundaries = interactBoundaries(currentPosition);

        // interaction with group members
        forceInteractionWithGroup = interactGroup(currentPosition, currentVelocity);

        // total extrinsic forces
        totalExtrinsicForces = forceInteractionWithPedestrians.add(forceInteractionWithBoundaries)
            .add(forceInteractionWithGroup);

        // total acceleration on current pedestrian
        totalForce = intrinsicForce.add(totalExtrinsicForces);

        if (Double.isNaN(totalForce.getX()))
        {
            logger.info("forces: intrinsicForce=" + intrinsicForce
                + ", forceInteractionWithPedestrians=" + forceInteractionWithPedestrians
                + ", forceInteractionWithBoundaries=" + forceInteractionWithBoundaries
                + ", forceInteractionWithGroup=" + forceInteractionWithGroup);
        }

        return totalForce;
    }

    /**
     * Updates movement direction by calling
     * {@link WayFindingModel#updateNormalizedDirectionVector(Vector2D, long, float)} and
     * subsequently moves the {@link Pedestrian} using the {@link NumericIntegrator}.
     * <p>
     * One of the {@link NumericIntegrator}s is chosen: {@link SemiImplicitEulerIntegrator} or
     * {@link SimpleEulerIntegrator} or {@link RungeKuttaIntegrator} for calculation of the velocity
     * and position of this {@link Pedestrian} objects in the next time step.
     *
     * <b>If {@code simulationInterval} equals zero the method return immediately</b>
     *
     * @param time the current time (simulated) time, given in milliseconds
     * @param simulationInterval the time between this method invocation and the last one, given in
     *            seconds
     */
    public void move(long time, double simulationInterval)
    {
        if (simulationInterval == 0)
        {
            logger.debug("simulationInterval==0, return without moving pedestrian.");
            return;
        }

        try
        {
            // update movement direction
            activeWayFindingModel.updateNormalizedDirectionVector(currentPositionVector, time,
                getCurrentNormalDesiredVelocity());

            // actual move (including update of velocity and position of this pedestrian)
            numericIntegrator.move(time, simulationInterval, this, quadtree);
        }
        // in case of any other exception ensure that the thread does not die here
        catch (Exception e)
        {
            logger.info("Error moving Pedestrian: " + this.getId() + ", keep old position", e);
        }

    }
}
