package de.fhg.ivi.crowdsimulation.ui.gui.control.menus.view;

import java.awt.event.ActionEvent;

import javax.swing.JCheckBoxMenuItem;

import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.gui.control.menus.AbstractMenu;
import de.fhg.ivi.crowdsimulation.ui.gui.visualisation.Map;

public class ForceMenu extends AbstractMenu
{
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * {@link JCheckBoxMenuItem} which makes the vector of current intrinsic forces (i.e. forces
     * caused by the aim to move towards the current target) of all {@link Pedestrian} visible or
     * not.
     */
    private JCheckBoxMenuItem checkBoxCurrentIntrinsicForces;

    /**
     * {@link JCheckBoxMenuItem} which makes the vector of forces caused by other {@link Pedestrian}
     * objects of all {@link Pedestrian} visible or not.
     */
    private JCheckBoxMenuItem checkBoxCurrentPedestrianForces;

    /**
     * {@link JCheckBoxMenuItem} which makes the the vector of current forces caused by
     * {@link Boundary} objects) of all {@link Pedestrian} visible or not.
     */
    private JCheckBoxMenuItem checkBoxCurrentBoundaryForces;

    /**
     * {@link JCheckBoxMenuItem} which makes the vector of current total extrinsic forces (i.e.
     * forces caused by other {@link Pedestrian} and {@link Boundary}) of all {@link Pedestrian}
     * visible or not.
     */
    private JCheckBoxMenuItem checkBoxCurrentExtrinsicForces;

    /**
     * {@link JCheckBoxMenuItem} which makes the vector of current total current total forces (i.e.
     * sum of extrinsic and intrinsic forces) of all {@link Pedestrian} visible or not.
     */
    private JCheckBoxMenuItem checkBoxCurrentTotalForces;

    public ForceMenu(CrowdSimulation crowdSimulation)
    {
        super("Forces", crowdSimulation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createElements()
    {
        checkBoxCurrentIntrinsicForces = new JCheckBoxMenuItem(
            "Intrinsic forces vectors (i.e. forces by the aim to move towards the current target)");
        checkBoxCurrentPedestrianForces = new JCheckBoxMenuItem(
            "Pedestrian forces vectors (i.e. forces caused by other pedestrians only)");
        checkBoxCurrentBoundaryForces = new JCheckBoxMenuItem(
            "Boundary forces vectors (i.e. forces caused by boundaries only)");
        checkBoxCurrentExtrinsicForces = new JCheckBoxMenuItem(
            "Extrinsic forces vectors (i.e. forces caused by other pedestrians and boundaries)");
        checkBoxCurrentTotalForces = new JCheckBoxMenuItem(
            "Total forces vectors (i.e. sum of all forces)");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInitialValues()
    {
        Map map = crowdSimulation.getPanelManager().getComponent(Map.class);
        if (map == null)
            return;
        checkBoxCurrentIntrinsicForces.setSelected(map.isCurrentIntrinsicForcesVisible());
        checkBoxCurrentPedestrianForces.setSelected(map.isCurrentPedestrianForceVisible());
        checkBoxCurrentBoundaryForces.setSelected(map.isCurrentBoundaryForceVisible());
        checkBoxCurrentExtrinsicForces.setSelected(map.isCurrentExtrinsicForcesVisible());
        checkBoxCurrentTotalForces.setSelected(map.isCurrentTotalForcesVisible());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addElements()
    {
        add(checkBoxCurrentIntrinsicForces);
        add(checkBoxCurrentPedestrianForces);
        add(checkBoxCurrentBoundaryForces);
        add(checkBoxCurrentExtrinsicForces);
        add(checkBoxCurrentTotalForces);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addListeners()
    {
        checkBoxCurrentIntrinsicForces.addActionListener(this);
        checkBoxCurrentPedestrianForces.addActionListener(this);
        checkBoxCurrentBoundaryForces.addActionListener(this);
        checkBoxCurrentExtrinsicForces.addActionListener(this);
        checkBoxCurrentTotalForces.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Map map = crowdSimulation.getPanelManager().getComponent(Map.class);

        if (e.getSource() == checkBoxCurrentIntrinsicForces)
        {
            map.setCurrentIntrinsicForcesVisible(checkBoxCurrentIntrinsicForces.isSelected());
        }

        if (e.getSource() == checkBoxCurrentPedestrianForces)
        {
            map.setCurrentPedestrianForceVisible(checkBoxCurrentPedestrianForces.isSelected());
        }

        if (e.getSource() == checkBoxCurrentBoundaryForces)
        {
            map.setCurrentBoundaryForceVisible(checkBoxCurrentBoundaryForces.isSelected());
        }

        if (e.getSource() == checkBoxCurrentExtrinsicForces)
        {
            map.setCurrentExtrinsicForcesVisible(checkBoxCurrentExtrinsicForces.isSelected());
        }

        if (e.getSource() == checkBoxCurrentTotalForces)
        {
            map.setCurrentTotalForcesVisible(checkBoxCurrentTotalForces.isSelected());
        }
        map.repaint();
    }
}
