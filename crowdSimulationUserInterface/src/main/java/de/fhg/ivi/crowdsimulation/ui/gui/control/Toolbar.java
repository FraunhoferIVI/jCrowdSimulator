package de.fhg.ivi.crowdsimulation.ui.gui.control;

import javax.swing.Action;
import javax.swing.JToolBar;

import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.WayPoint;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.file.Exit;
import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.file.Restart;
import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.file.StopAndClear;
import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.io.LoadBoundary;
import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.io.LoadCrowdAndRoute;
import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.simulation.PauseSimulation;
import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.simulation.StartSimulation;
import jiconfont.icons.FontAwesome;
import jiconfont.swing.IconFontSwing;

/**
 * This class contains a set of {@link Action}s that can be performed by the user after the
 * {@link CrowdSimulation} has been started. This includes starting, pausing, stopping and resetting
 * the simulation as well as loading alternative {@link Boundary}, {@link Pedestrian} and
 * {@link WayPoint} data.
 * <p>
 * Hint: This class contains the same actions like the class {@link MenuBar}.
 *
 * @author hahmann/meinert
 */
public class Toolbar extends JToolBar
{

    /**
     * default serial version uid
     */
    private static final long serialVersionUID = 1L;

    /**
     * Adds the functionality of all {@link Action}s from the actions package to the
     * {@link Toolbar}.
     *
     * @param crowdSimulation object of the main class of the simulation {@link CrowdSimulation}
     */
    public Toolbar(CrowdSimulation crowdSimulation)
    {
        // register font awesome font for icons
        IconFontSwing.register(FontAwesome.getIconFont());

        add(new Restart(crowdSimulation));
        add(new StopAndClear(crowdSimulation));
        add(new Exit());
        addSeparator();
        add(new LoadCrowdAndRoute(crowdSimulation));
        add(new LoadBoundary(crowdSimulation));
        addSeparator();
        add(new StartSimulation(crowdSimulation));
        add(new PauseSimulation(crowdSimulation));
    }
}
