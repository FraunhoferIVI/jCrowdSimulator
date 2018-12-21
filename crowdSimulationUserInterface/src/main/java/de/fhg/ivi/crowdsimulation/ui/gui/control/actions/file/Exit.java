package de.fhg.ivi.crowdsimulation.ui.gui.control.actions.file;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.Action;

import de.fhg.ivi.crowdsimulation.ui.gui.control.actions.AbstractAction;
import jiconfont.icons.FontAwesome;

/**
 * Action, which exits the program.
 *
 * @author hahmann/meinert
 */
public class Exit extends AbstractAction
{

    /**
     * default serial version uid
     */
    private static final long   serialVersionUID = 1L;

    /**
     * the name and the short description of this action
     */
    private final static String name             = "Exit";

    /**
     * Creates a new {@link Exit}, which can be used by the user to exit the program.
     */
    public Exit()
    {
        super(name, null, FontAwesome.SIGN_OUT, Color.black);
    }

    /**
     * Overrides the same method in {@link AbstractAction} and invokes a specific {@link Action}.
     *
     * @see de.fhg.ivi.crowdsimulation.ui.gui.control.actions.AbstractAction#actionPerformed(ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        System.exit(0);
    }
}
