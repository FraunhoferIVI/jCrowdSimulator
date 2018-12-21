package de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel;

/**
 * This class inherits from {@code Exception} and is thrown, if conditions occur, where
 * {@link WayFindingModel} is not valid (anymore).
 *
 * @author hahmann
 */
public class WayFindingModelNotValidException extends Exception
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
    public WayFindingModelNotValidException(String message)
    {
        super(message);
    }
}
