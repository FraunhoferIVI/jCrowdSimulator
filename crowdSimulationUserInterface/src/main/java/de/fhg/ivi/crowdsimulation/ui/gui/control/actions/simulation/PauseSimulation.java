package de.fhg.ivi.crowdsimulation.ui.gui.control.actions.simulation;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.ivi.crowdsimulation.CrowdSimulator;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.extension.VisualCrowdSimulator;
import de.fhg.ivi.crowdsimulation.ui.gui.control.Toolbar;
import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.AbstractAction;
import jiconfont.icons.FontAwesome;

/**
 * Action, which pauses the simulation thread and the graphics thread and thereby the pedestrian
 * movement.
 *
 * @author hahmann/meinert
 */
public class PauseSimulation extends AbstractAction
{

    /**
     * Uses the object logger for printing specific messages in the console.
     */
    @SuppressWarnings("unused")
    private static final Logger logger           = LoggerFactory.getLogger(Toolbar.class);

    /**
     * default serial version uid
     */
    private static final long   serialVersionUID = 1L;

    /**
     * the name and the short description of this action
     */
    private final static String name             = "Pause";

    /**
     * Creates a new {@link PauseSimulation}, which can be used by the user to pauses the simulation
     * thread and the graphics thread and thereby the pedestrian movement.
     *
     * @param crowdSimulation object of the main class of the simulation {@link CrowdSimulation}
     */
    public PauseSimulation(CrowdSimulation crowdSimulation)
    {
        super(name, crowdSimulation, FontAwesome.PAUSE_CIRCLE_O, greenColor);
    }

    /**
     * Overrides the same method in {@link AbstractAction} and invokes a specific {@link Action}.
     * <p>
     * This action pauses the simulation, saves the {@link CrowdSimulator#saveFastForwardFactor()}
     * and set {@link CrowdSimulator#setFastForwardFactor(int)} back to null.
     *
     * @see de.fhg.ivi.crowdsimulation.ui.gui.control.actions.AbstractAction#actionPerformed(ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        VisualCrowdSimulator crowdSimulator = crowdSimulation.getCrowdSimulator();
        crowdSimulator.pauseSimulation();
    }
}
