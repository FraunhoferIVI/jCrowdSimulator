package de.fhg.ivi.crowdsimulation.ui.gui.control.actions.simulation;

import java.awt.event.ActionEvent;

import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.AbstractAction;
import de.fhg.ivi.crowdsimulation.ui.gui.control.dialogs.SetCrowdVelocitiesDialog;

public class SetCrowdVelocities extends AbstractAction
{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public SetCrowdVelocities(CrowdSimulation crowdSimulation)
    {
        super("Set Crowd Velocities...", crowdSimulation, null, null);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        SetCrowdVelocitiesDialog scv = new SetCrowdVelocitiesDialog(crowdSimulation);
        scv.setVisible(true);
    }

}
