package de.fhg.ivi.crowdsimulation.ui.gui.control.actions.file;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ConcurrentModificationException;

import javax.swing.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.ivi.crowdsimulation.CrowdSimulator;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.extension.VisualCrowd;
import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.AbstractAction;
import de.fhg.ivi.crowdsimulation.ui.gui.visualisation.Map;
import jiconfont.icons.FontAwesome;

/**
 * Action, which stops the simulation thread and the graphic thread. Furthermore, it clears all
 * objects, which are part of the simulation.
 *
 * @author hahmann/meinert
 */
public class StopAndClear extends AbstractAction
{
    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger logger           = LoggerFactory.getLogger(StopAndClear.class);

    /**
     * default serial version uid
     */
    private static final long   serialVersionUID = 1L;

    /**
     * the name and the short description of this action
     */
    private final static String name             = "Stop&Clear";

    /**
     * Creates a new {@link StopAndClear}, which can be used by the user to stop and to clear the
     * program.
     *
     * @param crowdSimulation object of the main class of the simulation {@link CrowdSimulation}
     */
    public StopAndClear(CrowdSimulation crowdSimulation)
    {
        super(name, crowdSimulation, FontAwesome.STOP_CIRCLE_O, Color.black);
    }

    /**
     * Overrides the same method in {@link AbstractAction} and invokes a specific {@link Action}.
     *
     * @see de.fhg.ivi.crowdsimulation.ui.gui.control.actions.AbstractAction#actionPerformed(ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        CrowdSimulator<VisualCrowd> crowdSimulator = crowdSimulation.getCrowdSimulator();
        Map map = crowdSimulation.getPanelManager().getComponent(Map.class);

        try
        {
            crowdSimulation.stopGraphicsThread();
            crowdSimulation.stopSimulationThread();

            while (map.isRenderingInProgress() || crowdSimulator.isSimulatingInProgress())
            {
                try
                {
                    Thread.sleep(10);
                }
                catch (InterruptedException ex)
                {
                    logger.debug("Thread interrupted.", ex);
                }
            }

            crowdSimulator.reset();
            map.clear();

            map.repaint();

        }
        catch (ConcurrentModificationException ex)
        {
            logger.error("actionPerformed(), Concurrent Modifikation Exception" + ex);
        }

        crowdSimulation.getPanelManager().getComponent(Map.class).clear();
        map.repaint();
    }
}
