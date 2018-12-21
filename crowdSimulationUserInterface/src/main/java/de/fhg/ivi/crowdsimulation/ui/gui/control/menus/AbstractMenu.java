package de.fhg.ivi.crowdsimulation.ui.gui.control.menus;

import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

import de.fhg.ivi.crowdsimulation.CrowdSimulator;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.gui.control.menus.view.CrowdMenu;
import de.fhg.ivi.crowdsimulation.ui.gui.visualisation.Map;

/**
 * TODO doc
 *
 * @author hahmann
 *
 */
public abstract class AbstractMenu extends JMenu implements ActionListener
{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Reference to main application.
     */
    protected CrowdSimulation crowdSimulation;

    public AbstractMenu(String menuTitle, CrowdSimulation crowdSimulation)
    {
        super(menuTitle);
        this.crowdSimulation = crowdSimulation;
        createElements();
        setInitialValues();
        addListeners();
        addElements();
    }

    /**
     * Creates all UI elements (i.e. all {@link JCheckBoxMenuItem}, {@link JRadioButtonMenuItem} to
     * this {@link CrowdMenu}
     */
    protected abstract void createElements();

    /**
     * Adds all UI elements (i.e. all {@link JCheckBoxMenuItem}, {@link JRadioButtonMenuItem} to
     * this {@link CrowdMenu}
     */
    protected abstract void addElements();

    /**
     * Sets the initial values of all UI elements (i.e. all {@link JCheckBoxMenuItem},
     * {@link JRadioButtonMenuItem} objects) according to the initial states of {@link Map} and
     * {@link CrowdSimulator}.
     */
    protected abstract void setInitialValues();

    /**
     * Adds {@link ActionListener} to {@link JCheckBoxMenuItem}s and {@link JRadioButtonMenuItem}.
     */
    protected abstract void addListeners();
}
