package de.fhg.ivi.crowdsimulation.ui.gui.control.actions.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;

import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.gui.visualisation.Map;

/**
 * Toggles the visibilities of {@link Boundary} objects in the {@link Map}
 *
 * @author hahmann
 *
 */
public class ToggleObstacles extends JCheckBoxMenuItem implements ActionListener
{

    /**
     *
     */
    private CrowdSimulation   crowdSimulation;

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * The {@link ToggleObstacles} Action.
     * <p>
     * Toggles the visibilities of {@link Boundary} objects in the {@link Map}
     *
     * @param crowdSimulation the {@link CrowdSimulation}
     */
    public ToggleObstacles(CrowdSimulation crowdSimulation)
    {
        super("Obstacles");
        this.crowdSimulation = crowdSimulation;
        addActionListener(this);
        Map map = crowdSimulation.getPanelManager().getComponent(Map.class);
        setSelected(map.isBoundariesVisible());
    }

    /**
     * Toggles the visibility of Boundaries.
     *
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        Map map = crowdSimulation.getPanelManager().getComponent(Map.class);
        // add check box for obstacles to view menu
        map.setBoundariesVisible(isSelected());
        map.repaint();
    }
}
