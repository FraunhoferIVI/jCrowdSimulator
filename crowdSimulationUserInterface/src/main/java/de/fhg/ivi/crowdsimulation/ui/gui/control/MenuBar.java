package de.fhg.ivi.crowdsimulation.ui.gui.control;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JSeparator;

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
import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.simulation.SetCrowdVelocities;
import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.simulation.StartSimulation;
import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.view.ToggleObstacles;
import de.fhg.ivi.crowdsimulation.ui.gui.control.menus.simulation.NumericIntegrationMethodMenu;
import de.fhg.ivi.crowdsimulation.ui.gui.control.menus.simulation.SimulationSpeedMenu;
import de.fhg.ivi.crowdsimulation.ui.gui.control.menus.view.AnalysisMenu;
import de.fhg.ivi.crowdsimulation.ui.gui.control.menus.view.CrowdMenu;
import de.fhg.ivi.crowdsimulation.ui.gui.control.menus.view.ForceMenu;
import de.fhg.ivi.crowdsimulation.ui.gui.control.menus.view.PedestrianMenu;
import de.fhg.ivi.crowdsimulation.ui.gui.control.menus.view.WayFindingMenu;
import jiconfont.icons.FontAwesome;
import jiconfont.swing.IconFontSwing;

/**
 * This class contains a set of {@link Action}s that can be performed by the user after the
 * {@link CrowdSimulation} has been started. This includes starting, pausing, stopping and resetting
 * the simulation as well as loading alternative {@link Boundary}, {@link Pedestrian} and
 * {@link WayPoint} data.
 * <p>
 * Hint: This class contains the same actions like the class {@link Toolbar}.
 *
 * @author hahmann/meinert
 */
public class MenuBar extends JMenuBar
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
    public MenuBar(CrowdSimulation crowdSimulation)
    {
        // register font awesome font for icons
        IconFontSwing.register(FontAwesome.getIconFont());

        // menu File (package file)
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new Restart(crowdSimulation));
        fileMenu.add(new StopAndClear(crowdSimulation));
        fileMenu.add(new Exit());
        add(fileMenu);

        // menu Import (package load)
        JMenu importMenu = new JMenu("Import");
        importMenu.add(new LoadCrowdAndRoute(crowdSimulation));
        importMenu.add(new LoadBoundary(crowdSimulation));
        add(importMenu);

        // menu Simulation (package simulation)
        JMenu simulationMenu = new JMenu("Simulation");
        simulationMenu.add(new StartSimulation(crowdSimulation));
        simulationMenu.add(new PauseSimulation(crowdSimulation));
        simulationMenu.add(new JSeparator());
        simulationMenu.add(new SetCrowdVelocities(crowdSimulation));
        simulationMenu.add(new SimulationSpeedMenu(crowdSimulation));
        simulationMenu.add(new NumericIntegrationMethodMenu(crowdSimulation));
        add(simulationMenu);

        // menu View (package view)
        JMenu viewMenu = new JMenu("View");
        viewMenu.add(new PedestrianMenu(crowdSimulation));
        viewMenu.add(new CrowdMenu(crowdSimulation));
        viewMenu.add(new ForceMenu(crowdSimulation));
        viewMenu.add(new WayFindingMenu(crowdSimulation));
        viewMenu.add(new AnalysisMenu(crowdSimulation));
        viewMenu.add(new ToggleObstacles(crowdSimulation));
        add(viewMenu);
    }
}
