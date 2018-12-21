package de.fhg.ivi.crowdsimulation.ui.gui.control.menus.view;

import java.awt.event.ActionEvent;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.gui.control.menus.AbstractMenu;
import de.fhg.ivi.crowdsimulation.ui.gui.visualisation.Map;

/**
 * TODO doc
 *
 * @author hahmann
 *
 */
public class PedestrianMenu extends AbstractMenu
{

    /**
     *
     */
    private static final long    serialVersionUID = 1L;

    /**
     * {@link JCheckBoxMenuItem} making {@link Pedestrian} objects visible or not.
     */
    private JCheckBoxMenuItem    checkBoxPedestrians;

    /**
     * {@link JCheckBoxMenuItem} to toggle visualization of {@link Pedestrian#getCurrentVelocity}
     * on/off.
     */
    private JCheckBoxMenuItem    checkBoxVelocity;

    /**
     * {@link JRadioButtonMenuItem}, which enables default fill mode of {@link Pedestrian} objects
     */
    private JRadioButtonMenuItem radioButtonPedestrianDefaultFillMode;

    /**
     * {@link JRadioButtonMenuItem}, which enables orientation+forces fill mode of
     * {@link Pedestrian} objects.
     */
    private JRadioButtonMenuItem radioButtonPedestrianForcesAndOrientationFillMode;

    /**
     * {@link JRadioButtonMenuItem}, which enables quantitative forces fill mode of
     * {@link Pedestrian} objects.
     */
    private JRadioButtonMenuItem radioButtonPedestrianExtrinsicForcesFillMode;

    /**
     * Creates a new {@link PedestrianMenu} that allows the user to define the visibilities of
     * {@link Pedestrian} objects and their properties (i.e. forces, velocities, target points etc.)
     * outlines
     *
     * @param crowdSimulation reference to the main application
     */
    public PedestrianMenu(CrowdSimulation crowdSimulation)
    {
        super("Pedestrians", crowdSimulation);
    }

    @Override
    protected void createElements()
    {
        checkBoxPedestrians = new JCheckBoxMenuItem("Pedestrians");
        checkBoxVelocity = new JCheckBoxMenuItem("Velocities (km/h)");
        radioButtonPedestrianDefaultFillMode = new JRadioButtonMenuItem("Crowd Color");
        radioButtonPedestrianExtrinsicForcesFillMode = new JRadioButtonMenuItem(
            "Crowd Color + Quantitative Forces");
        radioButtonPedestrianForcesAndOrientationFillMode = new JRadioButtonMenuItem(
            "Crowd Color + Orientation/Force");

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
        checkBoxPedestrians.setSelected(map.isPedestriansVisible());
        checkBoxVelocity.setSelected(map.isVelocityVisible());
        radioButtonPedestrianDefaultFillMode
            .setSelected(map.getPedestriansFillMode() == Map.PEDESTRIAN_PAINT_MODE_DEFAULT);
        radioButtonPedestrianForcesAndOrientationFillMode.setSelected(
            map.getPedestriansFillMode() == Map.PEDESTRIAN_PAINT_MODE_FORCES_AND_ORIENTATION);
        radioButtonPedestrianExtrinsicForcesFillMode.setSelected(map
            .getPedestriansFillMode() == Map.PEDESTRIAN_PAINT_MODE_EXTRINSIC_FORCES_QUANTITATIVE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addElements()
    {
        add(checkBoxPedestrians);
        JMenu pedestrianFillModeMenu = new JMenu("Visualisation mode of Pedestrians");
        pedestrianFillModeMenu.add(radioButtonPedestrianDefaultFillMode);
        pedestrianFillModeMenu.add(radioButtonPedestrianExtrinsicForcesFillMode);
        pedestrianFillModeMenu.add(radioButtonPedestrianForcesAndOrientationFillMode);
        ButtonGroup pedestrianFillModes = new ButtonGroup();
        pedestrianFillModes.add(radioButtonPedestrianDefaultFillMode);
        pedestrianFillModes.add(radioButtonPedestrianExtrinsicForcesFillMode);
        pedestrianFillModes.add(radioButtonPedestrianForcesAndOrientationFillMode);
        add(pedestrianFillModeMenu);

        add(checkBoxVelocity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addListeners()
    {
        checkBoxPedestrians.addActionListener(this);
        checkBoxVelocity.addActionListener(this);

        radioButtonPedestrianDefaultFillMode.addActionListener(this);
        radioButtonPedestrianForcesAndOrientationFillMode.addActionListener(this);
        radioButtonPedestrianExtrinsicForcesFillMode.addActionListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        Map map = crowdSimulation.getPanelManager().getComponent(Map.class);

        if (e.getSource() == checkBoxPedestrians)
        {
            map.setPedestriansVisible(checkBoxPedestrians.isSelected());
        }

        if (e.getSource() == checkBoxVelocity)
        {
            map.setVelocityVisible(checkBoxVelocity.isSelected());
        }

        if (e.getSource() == radioButtonPedestrianDefaultFillMode)
        {
            map.setPedestriansFillMode(Map.PEDESTRIAN_PAINT_MODE_DEFAULT);
        }

        if (e.getSource() == radioButtonPedestrianForcesAndOrientationFillMode)
        {
            map.setPedestriansFillMode(Map.PEDESTRIAN_PAINT_MODE_FORCES_AND_ORIENTATION);
        }

        if (e.getSource() == radioButtonPedestrianExtrinsicForcesFillMode)
        {
            map.setPedestriansFillMode(Map.PEDESTRIAN_PAINT_MODE_EXTRINSIC_FORCES_QUANTITATIVE);
        }

        map.repaint();
    }

}
