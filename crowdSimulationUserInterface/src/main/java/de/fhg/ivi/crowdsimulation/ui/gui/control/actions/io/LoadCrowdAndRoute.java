package de.fhg.ivi.crowdsimulation.ui.gui.control.actions.io;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.AbstractAction;
import de.fhg.ivi.crowdsimulation.ui.gui.control.dialogs.LoadCrowdDialog;
import jiconfont.icons.FontAwesome;

public class LoadCrowdAndRoute extends AbstractAction
{

    /**
     * default serial version uid
     */
    private static final long   serialVersionUID = 1L;

    /**
     * Uses the object logger for printing specific messages in the console.
     */
    @SuppressWarnings("unused")
    private static final Logger logger           = LoggerFactory.getLogger(LoadCrowdAndRoute.class);

    /**
     * the name and the short description of this action
     */
    private final static String name             = "Import Crowd...";

    /**
     * Creates a new {@link LoadCrowdAndRoute}, which imports {@link Pedestrian} data.
     *
     * @param crowdSimulation object of the main class of the simulation {@link CrowdSimulation}
     */
    public LoadCrowdAndRoute(CrowdSimulation crowdSimulation)
    {
        super(name, crowdSimulation, FontAwesome.USERS, blackColor);
    }

    /**
     * Overrides the same method in {@link AbstractAction} and invokes a specific {@link Action}.
     *
     * @see de.fhg.ivi.crowdsimulation.ui.gui.control.actions.AbstractAction#actionPerformed(ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        LoadCrowdDialog lcd = new LoadCrowdDialog(crowdSimulation);
        lcd.setVisible(true);
    }
}
