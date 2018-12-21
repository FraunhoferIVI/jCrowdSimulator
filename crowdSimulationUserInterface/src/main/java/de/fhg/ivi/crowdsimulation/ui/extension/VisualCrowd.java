package de.fhg.ivi.crowdsimulation.ui.extension;

import java.awt.Color;

import de.fhg.ivi.crowdsimulation.crowd.Crowd;

/**
 * This class extends {@link Crowd}.
 * <p>
 * Adds setting/getting a {@link Color} for a {@link Crowd} object
 *
 * @author hahmann
 *
 */
public class VisualCrowd extends Crowd
{

    /**
     * The {@link Color} of this {@link VisualCrowd}
     */
    private Color             color;

    /**
     * A default color value that is used if this {@link VisualCrowd} is created using a constructor
     * without color assignment.
     */
    public static final Color DEFAUT_COLOR = Color.BLACK;

    /**
     * Creates a new {@link VisualCrowd} object from a given {@link Crowd} object, extending this
     * {@link Crowd} object with a given {@code color}.
     * <p>
     * Users a requested to create {@link VisualCrowd} objects using
     * {@link VisualCrowdSimulator#createVisualCrowd(java.util.List, boolean, Color)} method.
     *
     * @param crowd the {@link Crowd} to be used as basis for this {@link VisualCrowd}
     * @param color the {@link Color} to be associated with this {@link VisualCrowd}
     *
     * @see VisualCrowdSimulator#createVisualCrowd(java.util.List, boolean, Color)
     */
    public VisualCrowd(Crowd crowd, Color color)
    {
        super(crowd);
        setColor(color);
    }

    /**
     * Gets the {@link Color} of this {@link VisualCrowd}
     *
     * @return the {@link Color} of this {@link VisualCrowd}
     */
    public Color getColor()
    {
        return color;
    }

    /**
     * Sets the {@link Color} of this {@link VisualCrowd}.
     *
     * @param color the Color of this {@link VisualCrowd}
     */
    public void setColor(Color color)
    {
        this.color = color;
    }
}
