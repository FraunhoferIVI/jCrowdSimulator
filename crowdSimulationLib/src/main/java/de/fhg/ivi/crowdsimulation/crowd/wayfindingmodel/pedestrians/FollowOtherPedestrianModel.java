package de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.pedestrians;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.math.Vector2D;

import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.WayFindingModel;
import de.fhg.ivi.crowdsimulation.geom.Quadtree;

/**
 *
 *
 * @author hahmann
 *
 */
public class FollowOtherPedestrianModel extends WayFindingModel
{

    private final static int    minimumSearchRadius = 50;

    /**
     * Uses the object logger for printing specific messages in the console.
     */
    @SuppressWarnings("unused")
    private static final Logger logger              = LoggerFactory
        .getLogger(FollowOtherPedestrianModel.class);

    /**
     * The other {@link Pedestrian} that will be followed by this Model
     */
    private Pedestrian          otherPedestrian;

    /**
     * Creates a new {@link FollowOtherPedestrianModel} using the given {@code quadtree} for spatial
     * searches
     *
     * @param quadtree the {@link Quadtree} object for spatial searches
     */
    public FollowOtherPedestrianModel(Quadtree quadtree)
    {
        super(quadtree);
    }

    @Override
    public float getAverageVelocity(Vector2D position, long currentTime, boolean useCache)
    {
        return otherPedestrian.getCurrentNormalDesiredVelocity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateNormalizedDirectionVector(Vector2D position, long time,
        float normalDesiredVelocity)
    {
        findPedestrianToFollow(position, new Envelope(position.toCoordinate()));
        if (otherPedestrian == null)
            return;
        if (otherPedestrian != null)
        {
            // set the targetCoordinateVector to the position of the other Pedestrian
            targetPosition = otherPedestrian.getCurrentPositionVector();

            // compute direction vector between current position and target and normalize length to
            // 1
            updateNormalizedDirectionVector(position);
        }
    }

    @Override
    public void updateModel(long time, Vector2D currentPosition, Vector2D updatedPosition)
    {

    }

    @Override
    public void checkCourse(Pedestrian pedestrian, long timestamp)
    {

    }

    /**
     * @param position
     * @param searchArea
     */
    private void findPedestrianToFollow(Vector2D position, Envelope searchArea)
    {
        List<Pedestrian> pedestrians = quadtree.getPedestrians(searchArea);
        for (Pedestrian pedestrian : pedestrians)
        {
            // do not follow other pedestrian that also do not know where to go
            if (pedestrian.getActiveWayFindingModel().needsOrientation())
                continue;

            boolean isOtherPedestrianVisible = isTargetVisible(position,
                pedestrian.getCurrentPositionCoordinate());

            // follow first other visible pedestrian in the list, no matter if the other pedestrian
            // is the nearest
            if (isOtherPedestrianVisible
                && !pedestrian.getActiveWayFindingModel().needsOrientation())
            {
                otherPedestrian = pedestrian;
                break;
            }
        }
        // try to increase search radius, if no other pedestrian has been found
        if (otherPedestrian == null)
        {
            needsOrientation = true;
            if (pedestrians.size() < quadtree.getNumberOfPedestrians())
            {
                double increaseSearchArea;
                if (searchArea.getHeight() <= 0)
                {
                    increaseSearchArea = minimumSearchRadius;
                }
                else
                {
                    increaseSearchArea = searchArea.getHeight() > searchArea.getWidth()
                        ? searchArea.getHeight() * 2 : searchArea.getWidth() * 2;
                }
                searchArea.expandBy(increaseSearchArea);
                findPedestrianToFollow(position, searchArea);
            }
            // TODO WayFindingModelNotValidException
            // else
            // throw new WayFindingModelNotValidException(
            // "No other Pedestrian with orientation can be seen.");
        }
        else
            needsOrientation = false;
    }
}
