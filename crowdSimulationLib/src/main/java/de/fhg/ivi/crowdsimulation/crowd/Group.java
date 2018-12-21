package de.fhg.ivi.crowdsimulation.crowd;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPoint;

import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.Route;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.WayPoint;
import de.fhg.ivi.crowdsimulation.geom.Quadtree;

/**
 * A Group is a representation of a {@link List} of {@link Pedestrian} objects that belong together
 * within a {@link Crowd}. A group can consist of 1 to n {@link Pedestrian} objects. While the group
 * is moving, it creates forces between the group members so that they tend to stay/walk together.
 *
 * @author hahmann
 *
 */
public class Group
{
    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger logger = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());

    /**
     * A {@link List} of {@link Pedestrian} objects that belong to this {@link Group}
     */
    private List<Pedestrian>    pedestrians;

    /**
     * The current geometrical centroid of this {@link Group}
     */
    private Coordinate          centroid;

    /**
     * Creates a new {@link Group} object with a new and empty {@link List} of {@link Pedestrian}
     * objects.
     *
     */
    public Group()
    {
        pedestrians = new ArrayList<>();
    }

    /**
     * Adds a {@link Pedestrian} object to the {@link List} of {@link Pedestrian} objects of this
     * {@link Group}
     *
     * @param pedestrian the {@link Pedestrian} object to be added.
     */
    public void add(Pedestrian pedestrian)
    {
        pedestrians.add(pedestrian);

        // tell each pedestrian to which group it belongs
        for (Pedestrian p : pedestrians)
        {
            p.setGroup(this);
        }
    }

    /**
     * Sets a {@link List} of {@link WayPoint} objects to all members of this {@link Group}
     *
     * @param route the route object that contains the {@link List} of {@link WayPoint} objects to
     *            be followed by this {@link Group}
     * @param time the time when a this {@link Crowd} get a the new List of {@link WayPoint}
     *            assuming that it starts immediately towards the new route.
     */
    public void setRoute(Route route, long time)
    {
        for (Pedestrian pedestrian : pedestrians)
        {
            pedestrian.setRoute(route, time);
        }
    }

    /**
     * Gets a {@link List} of all {@link Pedestrian} objects belonging to this {@link Group}. If
     * {@code clone} parameter is set to {@code true}, a new {@link ArrayList} is returned
     * containing cloned {@link Pedestrian} objects
     *
     * @param clone if {@code true} a new {@link ArrayList} is returned containing cloned
     *            {@link Pedestrian} objects otherwise a {@link #pedestrians} is returned directly
     * @return the list of all {@link Pedestrian} objects contained in this {@link Group}
     */
    public List<Pedestrian> getPedestrians(boolean clone)
    {
        if (clone)
        {
            List<Pedestrian> pedestriansDeepCopy = new ArrayList<>();
            for (Pedestrian pedestrian : pedestrians)
            {
                pedestriansDeepCopy.add(pedestrian.clone());
            }
            return pedestriansDeepCopy;
        }
        return pedestrians;
    }

    /**
     * Moves all the {@link Pedestrian} objects belonging to this {@link Group}. Prior to moving the
     * individual {@link Pedestrian} objects, the centroid of the {@link Group} is computed and set
     * to the {@link Pedestrian} object so that they are able to reflect this when computing the
     * group forces.
     *
     * <b>Important the global {@link Quadtree} needs to be updated before this method is called
     * using {@link Quadtree#updateCrowds(List)}</b>
     *
     * @param time the current simulated time (given in milliseconds)
     * @param simulationUpdateInterval the time between this call of the method and the last one
     *            (i.e. the time between 2 consecutive simulation steps given in seconds)
     */
    public void move(long time, double simulationUpdateInterval)
    {
        try
        {
            updateCentroid();
            for (Pedestrian pedestrian : pedestrians)
            {
                pedestrian.move(time, simulationUpdateInterval);
            }
        }
        // in case of any other exception ensure that the thread does not die here
        catch (Exception e)
        {
            logger.info("Error moving Group. Keeping old positions of all pedestrians.", e);
        }

    }

    /**
     * Updates the {@link #centroid} of this {@link Group}. If the {@link Group} consists of only 1
     * {@link Pedestrian}, the current position of this {@link Pedestrian} is used. Otherwise the
     * mathematical centroid of all {@link Pedestrian}'s current positions is computed.
     */
    private void updateCentroid()
    {
        // handle trivial case
        if (pedestrians.size() == 1)
            this.centroid = pedestrians.get(0).getCurrentPositionCoordinate();

        // compute centroid if more than one pedestrian belongs to the group
        MultiPoint mp = JTSFactoryFinder.getGeometryFactory()
            .createMultiPoint(Crowd.getCoordinatesFromPedestrians(pedestrians));
        this.centroid = mp.getCentroid().getCoordinate();
    }

    /**
     * Gets the number of {@link Pedestrian} objects belonging to this {@link Group}
     *
     * @return the number of {@link Pedestrian} belonging to this {@link Group}.
     */
    public int getSize()
    {
        return pedestrians.size();
    }

    /**
     * Gets the centroid of this {@link Group}.
     * <p>
     * <b>Attention: this method only returns {@link #centroid} but does NOT re-compute the
     * centroid. If re-computation is required, {@link #updateCentroid()} needs to be called before
     * calling this method.</b>
     *
     * @return the centroid of this {@link Group}
     */
    public Coordinate getCentroid()
    {
        return centroid;
    }

    /**
     * Gets a {@link List} of {@link Coordinate}s representing the current positions of
     * {@link Pedestrian}s belonging to this {@link Group}
     *
     * @return the positions of all {@link Pedestrian}s belonging to this {@link Group}.
     */
    public List<Coordinate> getPositions()
    {
        ArrayList<Coordinate> positions = new ArrayList<>();
        for (Pedestrian pedestrian : pedestrians)
        {
            positions.add(pedestrian.getCurrentPositionCoordinate());
        }
        return positions;
    }
}
