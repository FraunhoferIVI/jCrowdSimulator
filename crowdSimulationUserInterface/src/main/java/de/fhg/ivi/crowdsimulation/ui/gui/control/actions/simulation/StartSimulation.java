package de.fhg.ivi.crowdsimulation.ui.gui.control.actions.simulation;

import java.awt.event.ActionEvent;

import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.extension.VisualCrowdSimulator;
import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.AbstractAction;
import de.fhg.ivi.crowdsimulation.ui.gui.visualisation.Map;
import jiconfont.icons.FontAwesome;

/**
 * Action, which starts the simulation thread and the graphics thread and thereby the pedestrian
 * movement.
 *
 * @author hahmann/meinert
 */
public class StartSimulation extends AbstractAction
{
    /**
     * default serial version uid
     */
    private static final long   serialVersionUID = 1L;

    /**
     * the name and the short description of this action
     */
    private final static String name             = "Start";

    /**
     * Creates a new {@link StartSimulation}, which can be used by the user to starts the simulation
     * thread and the graphics thread and thereby the pedestrian movement.
     *
     * @param crowdSimulation object of the main class of the simulation {@link CrowdSimulation}
     */
    public StartSimulation(CrowdSimulation crowdSimulation)
    {
        super(name, crowdSimulation, FontAwesome.PLAY_CIRCLE_O, greenColor);
    }

    /**
     * (Re-)starts the simulation.
     *
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        VisualCrowdSimulator crowdSimulator = crowdSimulation.getCrowdSimulator();
        Map map = crowdSimulation.getPanelManager().getComponent(Map.class);

        // resume to simulation after simulation was paused
        crowdSimulator.resumeSimulation();

        // fresh start of simulation if the simulation is not running
        if ( !crowdSimulator.isSimulationRunning())
        {
            crowdSimulation.startSimulation();
        }
        if ( !map.isGraphicsThreadRunning())
        {
            crowdSimulation.startGraphicsThread();
        }
    }
}
