package de.fhg.ivi.crowdsimulation.ui.gui.control.actions;

import java.awt.Color;

import javax.swing.Action;
import javax.swing.Icon;

import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.gui.control.MenuBar;
import de.fhg.ivi.crowdsimulation.ui.gui.control.Toolbar;
import jiconfont.IconCode;
import jiconfont.swing.IconFontSwing;

/**
 * {@link AbstractAction}, which starts the {@link Action}s, which are invoked by their sub-classes.
 *
 * @author hahmann/meinert
 */
public abstract class AbstractAction extends javax.swing.AbstractAction
{

    /**
     * default serial version uid
     */
    private static final long       serialVersionUID = 1L;

    /**
     * Denotes the size of the icons in {@link MenuBar} and {@link Toolbar}.
     */
    private final static int        iconSize         = 16;

    /**
     * Defines a green color for the icons in the sub-classes.
     */
    protected final static Color    greenColor       = new Color(0, 128, 1);

    /**
     * Defines a black color for the icons in the sub-classes.
     */
    protected final static Color    blackColor       = new Color(0, 0, 0);

    /**
     * object of the main class of the simulation {@link CrowdSimulation}
     */
    protected final CrowdSimulation crowdSimulation;

    /**
     * Creates a new {@link AbstractAction}, which is invoked by the sub-classes
     *
     * @param name is the name and a short description of this action
     * @param crowdSimulation object of the main class of the simulation
     * @param iconCode defines a specific font of an icon
     * @param iconColor defines the color of the icon
     */
    public AbstractAction(String name, CrowdSimulation crowdSimulation, IconCode iconCode,
        Color iconColor)
    {
        super(name);
        if (iconCode != null && iconColor != null)
        {
            Icon icon = IconFontSwing.buildIcon(iconCode, iconSize, iconColor);
            putValue(Action.SMALL_ICON, icon);
        }

        putValue(Action.SHORT_DESCRIPTION, name);
        this.crowdSimulation = crowdSimulation;
    }
}
