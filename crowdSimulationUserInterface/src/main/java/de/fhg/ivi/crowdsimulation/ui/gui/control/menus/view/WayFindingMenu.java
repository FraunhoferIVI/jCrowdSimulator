package de.fhg.ivi.crowdsimulation.ui.gui.control.menus.view;

import java.awt.event.ActionEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JSeparator;

import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.WayFindingModel;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.WayPoint;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.gui.control.menus.AbstractMenu;
import de.fhg.ivi.crowdsimulation.ui.gui.visualisation.Map;

public class WayFindingMenu extends AbstractMenu
{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * {@link JCheckBoxMenuItem} to toggle visualization of next {@link WayPoint} for all
     * {@link Pedestrian} objects on/off, i.e. lines connecting the {@link Pedestrian} objects and
     * their current target points.
     */
    private JCheckBoxMenuItem checkBoxCurrentTargetWayPoint;

    /**
     * {@link JCheckBoxMenuItem} to toggle visualization of
     * {@link WayFindingModel#getTargetPosition()} for all {@link Pedestrian} objects on/off, i.e.
     * lines connecting the {@link Pedestrian} objects and their current target points.
     */
    private JCheckBoxMenuItem checkBoxCurrentTargetPoint;

    /**
     * {@link JCheckBoxMenuItem} to toggle visualization of
     * {@link WayFindingModel#getNormalizedDirectionVector()} for all {@link Pedestrian} objects
     * on/off, i.e. vectors normalized to length of 1m pointing from the {@link Pedestrian} object
     * into the direction of its target point
     */
    private JCheckBoxMenuItem checkBoxCurrentTargetVector;

    /**
     * {@link JCheckBoxMenuItem} to toggle visualization of
     * {@link Pedestrian#getCurrentVelocityVector()} on/off.
     */
    private JCheckBoxMenuItem checkBoxCurrentVelocityVector;

    /**
     * {@link JCheckBoxMenuItem} to toggle visualization of the last successful orientation points
     * of all {@link Pedestrian}.
     */
    private JCheckBoxMenuItem checkBoxLastSuccesfulOrientation;

    public WayFindingMenu(CrowdSimulation crowdSimulation)
    {
        super("Wayfinding", crowdSimulation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createElements()
    {
        checkBoxCurrentTargetWayPoint = new JCheckBoxMenuItem(
            "Next Waypoint (i.e. center of next waypoints of each pedestrian)");
        checkBoxCurrentTargetPoint = new JCheckBoxMenuItem(
            "Target Points (i.e. target points of the pedestrians)");
        checkBoxCurrentTargetVector = new JCheckBoxMenuItem(
            "Normalized Target vectors (i.e. where the pedestrians want to go)");
        checkBoxCurrentVelocityVector = new JCheckBoxMenuItem(
            "Velocity vectors (i.e. where the pedestrian actually go)");
        checkBoxLastSuccesfulOrientation = new JCheckBoxMenuItem(
            "Last successful orientation (i.e. where the pedestrians successfully saw their targets)");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInitialValues()
    {
        Map map = crowdSimulation.getPanelManager().getComponent(Map.class);
        checkBoxCurrentTargetWayPoint.setSelected(map.isCurrentTargetWayPointVisible());
        checkBoxCurrentTargetVector.setSelected(map.isCurrentTargetVectorVisible());
        checkBoxCurrentTargetPoint.setSelected(map.isCurrentTargetPointVisible());
        checkBoxCurrentVelocityVector.setSelected(map.isCurrentVelocityVectorVisible());
        checkBoxLastSuccesfulOrientation.setSelected(map.isLastSuccessfulOrientationPointVisible());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addElements()
    {
        add(new RouteMenu(crowdSimulation));
        add(new JSeparator());
        add(checkBoxCurrentTargetWayPoint);
        add(checkBoxCurrentTargetPoint);
        add(checkBoxCurrentTargetVector);
        add(checkBoxCurrentVelocityVector);
        add(checkBoxLastSuccesfulOrientation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addListeners()
    {
        checkBoxCurrentTargetWayPoint.addActionListener(this);
        checkBoxCurrentTargetPoint.addActionListener(this);
        checkBoxCurrentTargetVector.addActionListener(this);
        checkBoxCurrentVelocityVector.addActionListener(this);
        checkBoxLastSuccesfulOrientation.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Map map = crowdSimulation.getPanelManager().getComponent(Map.class);

        if (e.getSource() == checkBoxCurrentTargetWayPoint)
        {
            map.setCurrentTargetWayPointVisible(checkBoxCurrentTargetWayPoint.isSelected());
        }

        if (e.getSource() == checkBoxCurrentTargetPoint)
        {
            map.setCurrentTargetPointVisible(checkBoxCurrentTargetPoint.isSelected());
        }

        if (e.getSource() == checkBoxCurrentTargetVector)
        {
            map.setCurrentTargetVectorVisible(checkBoxCurrentTargetVector.isSelected());
        }

        if (e.getSource() == checkBoxCurrentVelocityVector)
        {
            map.setCurrentVelocityVectorVisible(checkBoxCurrentVelocityVector.isSelected());
        }

        if (e.getSource() == checkBoxLastSuccesfulOrientation)
        {
            map.setLastSuccessfulOrientationPointVisible(
                checkBoxLastSuccesfulOrientation.isSelected());
        }
    }
}
