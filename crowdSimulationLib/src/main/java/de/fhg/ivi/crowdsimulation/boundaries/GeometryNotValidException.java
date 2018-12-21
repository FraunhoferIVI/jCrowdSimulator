package de.fhg.ivi.crowdsimulation.boundaries;

import de.fhg.ivi.crowdsimulation.CrowdSimulator;

/**
 * This class inherits from {@link Exception} and is thrown for Geometries that are not valid for
 * usage in the {@link CrowdSimulator}.
 *
 * @author hahmann
 */
public class GeometryNotValidException extends Exception
{
    /**
     * default serial version ID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@link GeometryNotValidException} with the specified detail message.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the
     *            {@link #getMessage()} method.
     */
    public GeometryNotValidException(String message)
    {
        super(message);
    }
}
