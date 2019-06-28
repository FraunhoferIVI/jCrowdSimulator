package de.fhg.ivi.crowdsimulation.ui.gui.control.actions.io;

import java.awt.event.ActionEvent;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.ivi.crowdsimulation.CrowdSimulatorNotValidException;
import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.extension.VisualCrowd;
import de.fhg.ivi.crowdsimulation.ui.extension.VisualCrowdSimulator;
import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.AbstractAction;
import jiconfont.icons.FontAwesome;

/**
 * Action, which allows to import data for {@link Boundary}s as Shapefile or Well-known text.
 *
 * @author meinert
 */
public class LoadBoundary extends AbstractAction
{
    /**
     * default serial version uid
     */
    private static final long   serialVersionUID = 1L;

    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger logger           = LoggerFactory.getLogger(LoadBoundary.class);

    /**
     * the name and the short description of this action
     */
    private final static String name             = "Import Boundaries...";

    /**
     * Creates a new {@link LoadBoundary}, which imports {@link Boundary} data.
     *
     * @param crowdSimulation object of the main class of the simulation {@link CrowdSimulation}
     */
    public LoadBoundary(CrowdSimulation crowdSimulation)
    {
        super(name, crowdSimulation, FontAwesome.GLOBE, blackColor);
    }

    /**
     * Overrides the same method in {@link AbstractAction} and invokes a specific {@link Action}.
     *
     * @see de.fhg.ivi.crowdsimulation.ui.gui.control.actions.AbstractAction#actionPerformed(ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        try
        {
            crowdSimulation.loadBoundaries(null, false);
        }
        catch (CrowdSimulatorNotValidException ex)
        {
            VisualCrowdSimulator cs = crowdSimulation.getCrowdSimulator();
            boolean validateStrict = true;
            validateStrict &= cs.getCrowds() != null && !cs.getCrowds().isEmpty();
            boolean crowdWithRoute = false;
            if (cs.getCrowds() != null && !cs.getCrowds().isEmpty())
            {
                for (VisualCrowd crowd : cs.getCrowds())
                {
                    crowdWithRoute |= crowd.getRoute() != null;
                }
            }
            validateStrict &= crowdWithRoute;
            if (validateStrict)
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Warning",
                    JOptionPane.WARNING_MESSAGE);
            logger.debug("BoundaryLoadAction.actionPerformed(), ", ex);
        }
        catch (RuntimeException ex)
        {
            if (ex.getCause() instanceof FileNotFoundException)
                logger.debug("file not found.");
        }
        catch (FileNotFoundException ex)
        {
            logger.debug("shapefile not found.");
        }
        catch (IOException ex)
        {
            logger.debug("error reading file.", ex);
        }
    }
}
