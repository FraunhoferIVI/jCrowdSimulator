package de.fhg.ivi.crowdsimulation.ui.gui.control.actions.file;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JFrame;

import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.AbstractAction;
import jiconfont.icons.FontAwesome;

/**
 * Action, which resets the program to the default mode.
 *
 * @author hahmann/meinert
 */
public class Restart extends AbstractAction
{
    /**
     * default serial version uid
     */
    private static final long   serialVersionUID = 1L;

    /**
     * the name and the short description of this action
     */
    private final static String name             = "Restart";

    /**
     * Creates a new {@link Restart}, which can be used by the user to reset the program,
     * which sets it back to the default mode
     */
    public Restart(CrowdSimulation crowdSimulation)
    {
        super(name, crowdSimulation, FontAwesome.WINDOW_RESTORE, Color.black);
    }

    /**
     * Overrides the same method in {@link AbstractAction} and invokes a specific {@link Action}.
     *
     * @see de.fhg.ivi.crowdsimulation.ui.gui.control.actions.AbstractAction#actionPerformed(ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        crowdSimulation.dispose();
        CrowdSimulation cs = new CrowdSimulation();
        cs.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        cs.setVisible(true);
    }
}
