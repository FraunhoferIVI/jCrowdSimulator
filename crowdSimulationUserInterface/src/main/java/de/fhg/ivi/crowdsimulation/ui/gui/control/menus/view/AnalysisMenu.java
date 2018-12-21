package de.fhg.ivi.crowdsimulation.ui.gui.control.menus.view;

import java.awt.event.ActionEvent;

import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;

import de.fhg.ivi.crowdsimulation.CrowdSimulator;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.gui.control.menus.AbstractMenu;
import de.fhg.ivi.crowdsimulation.ui.gui.visualisation.Map;

public class AnalysisMenu extends AbstractMenu
{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * {@link JCheckBox} which makes the {@link CrowdSimulator#getGrid()} visible or not.
     */
    private JCheckBoxMenuItem checkBoxGrid;

    /**
     * {@link JCheckBox} which makes the {@link CrowdSimulator#getGrid()} visible or not.
     */
    private JCheckBoxMenuItem checkBoxGridLabels;

    public AnalysisMenu(CrowdSimulation crowdSimulation)
    {
        super("Analysis", crowdSimulation);
    }

    @Override
    protected void createElements()
    {

        checkBoxGrid = new JCheckBoxMenuItem("Grid (local crowd densities)");
        checkBoxGridLabels = new JCheckBoxMenuItem("Grid labels (pedestrians/m²)");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInitialValues()
    {
        Map map = crowdSimulation.getPanelManager().getComponent(Map.class);
        checkBoxGrid.setSelected(map.isGridVisible());
        checkBoxGridLabels.setSelected(map.isGridLabelsVisible());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addElements()
    {
        add(checkBoxGrid);
        add(checkBoxGridLabels);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addListeners()
    {
        checkBoxGrid.addActionListener(this);
        checkBoxGridLabels.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Map map = crowdSimulation.getPanelManager().getComponent(Map.class);
        if (e.getSource() == checkBoxGrid)
        {
            map.setGridVisible(checkBoxGrid.isSelected());
        }

        if (e.getSource() == checkBoxGridLabels)
        {
            map.setGridLabelsVisible(checkBoxGridLabels.isSelected());
        }
    }
}
