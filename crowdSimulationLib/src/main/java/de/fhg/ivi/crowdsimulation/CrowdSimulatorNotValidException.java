package de.fhg.ivi.crowdsimulation;

import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.WayPoint;

/**
 * The class {@link CrowdSimulatorNotValidException} inherits from {@link Exception} and indicates
 * conditions that a reasonable application might want to catch, particularly cases, when a
 * {@link CrowdSimulator} is not valid anymore.
 * <p>
 * Following conditions are intended use case of the {@link CrowdSimulatorNotValidException}:
 * <ul>
 * <li>Not all Pedestrians are at distinct position.</li>
 * <li>There is no direct line of sight between all pairs of consecutive {@link WayPoint}s</li>
 * <li>There is a {@link Pedestrian} that cannot see its first {@link WayPoint} on its route</li>
 * <li>Pedestrian Ids are non-unique within the {@link CrowdSimulator}</li>
 * </ul>
 *
 * @author hahmann
 */
public class CrowdSimulatorNotValidException extends Exception
{
    /**
     * default serial version ID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the
     *            {@link #getMessage()} method.
     */
    public CrowdSimulatorNotValidException(String message)
    {
        super(message);
    }
}
