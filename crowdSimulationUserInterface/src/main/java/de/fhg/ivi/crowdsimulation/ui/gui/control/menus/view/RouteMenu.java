package de.fhg.ivi.crowdsimulation.ui.gui.control.menus.view;

import java.awt.event.ActionEvent;

import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;

import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.WayPoint;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.gui.control.menus.AbstractMenu;
import de.fhg.ivi.crowdsimulation.ui.gui.visualisation.Map;

public class RouteMenu extends AbstractMenu
{
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * {@link JCheckBox} which makes the {@code wayPoints} visible or not.
     */
    private JCheckBoxMenuItem checkBoxWayPoints;

    /**
     * {@link JCheckBox} which makes the {@link WayPoint#getTargetLine()} visible or not.
     */
    private JCheckBoxMenuItem checkBoxWayPointTargetLines;

    /**
     * {@link JCheckBox} which makes the {@link WayPoint#getPassingArea()} visible or not.
     */
    private JCheckBoxMenuItem checkBoxWayPointPassingAreas;

    /**
     * {@link JCheckBox} which makes the {@link WayPoint#getConnectionLineToPredecessor()} visible
     * or not.
     */
    private JCheckBoxMenuItem checkBoxWayPointConnectionLines;

    /**
     * {@link JCheckBox} which makes the {@code wayPointLabels} visible or not.
     */
    private JCheckBoxMenuItem checkBoxWayPointLabels;

    public RouteMenu(CrowdSimulation crowdSimulation)
    {
        super("Route(s)", crowdSimulation);
    }

    @Override
    protected void createElements()
    {
        checkBoxWayPoints = new JCheckBoxMenuItem("Waypoints");
        checkBoxWayPointLabels = new JCheckBoxMenuItem("Waypoint Labels");
        checkBoxWayPointTargetLines = new JCheckBoxMenuItem("Waypoint Target Lines");
        checkBoxWayPointPassingAreas = new JCheckBoxMenuItem("Waypoint Passing Areas");
        checkBoxWayPointConnectionLines = new JCheckBoxMenuItem("Waypoint Connection Lines");
    }

    @Override
    protected void addElements()
    {
        add(checkBoxWayPoints);
        add(checkBoxWayPointLabels);
        add(checkBoxWayPointTargetLines);
        add(checkBoxWayPointPassingAreas);
        add(checkBoxWayPointConnectionLines);
    }

    @Override
    protected void setInitialValues()
    {
        Map map = crowdSimulation.getPanelManager().getComponent(Map.class);
        checkBoxWayPoints.setSelected(map.isWayPointsVisible());
        checkBoxWayPointLabels.setSelected(map.isWayPointLabelsVisible());
        checkBoxWayPointTargetLines.setSelected(map.isWayPointTargetLinesVisible());
        checkBoxWayPointPassingAreas.setSelected(map.isWayPointTargetLinesVisible());
        checkBoxWayPointConnectionLines.setSelected(map.isWayPointConnectionLinesVisible());

    }

    @Override
    protected void addListeners()
    {
        checkBoxWayPoints.addActionListener(this);
        checkBoxWayPointLabels.addActionListener(this);
        checkBoxWayPointTargetLines.addActionListener(this);
        checkBoxWayPointPassingAreas.addActionListener(this);
        checkBoxWayPointConnectionLines.addActionListener(this);

    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Map map = crowdSimulation.getPanelManager().getComponent(Map.class);
        if (e.getSource() == checkBoxWayPoints)
        {
            map.setWayPointsVisible(checkBoxWayPoints.isSelected());
        }

        if (e.getSource() == checkBoxWayPointLabels)
        {
            map.setWayPointLabelsVisible(checkBoxWayPointLabels.isSelected());
        }

        if (e.getSource() == checkBoxWayPointTargetLines)
        {
            map.setWayPointTargetLinesVisible(checkBoxWayPointTargetLines.isSelected());
        }

        if (e.getSource() == checkBoxWayPointPassingAreas)
        {
            map.setWayPointPassingAreasVisible(checkBoxWayPointPassingAreas.isSelected());
        }

        if (e.getSource() == checkBoxWayPointConnectionLines)
        {
            map
                .setWayPointConnectionLinesVisible(checkBoxWayPointConnectionLines.isSelected());
        }
        map.repaint();
    }
}
