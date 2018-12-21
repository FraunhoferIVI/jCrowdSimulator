package de.fhg.ivi.crowdsimulation.crowd;

/**
 * A simple interface defining methods to identify objects.
 *
 * @author hahmann
 *
 */
public interface Identifiable
{
    /**
     * Gets the the Id of objects implementing this interface.
     *
     * @return the Id of the object
     */
    public int getId();

    /**
     * Sets the the Id of objects implementing this interface.
     *
     * @param id the Id of the object
     */
    public void setId(int id);
}
