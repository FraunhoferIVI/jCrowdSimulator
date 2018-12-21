package de.fhg.ivi.crowdsimulation.ui.gui.control.menus.view;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

import de.fhg.ivi.crowdsimulation.CrowdSimulator;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.extension.VisualCrowd;
import de.fhg.ivi.crowdsimulation.ui.gui.control.menus.AbstractMenu;
import de.fhg.ivi.crowdsimulation.ui.gui.visualisation.Map;

/**
 * TODO doc
 *
 * @author hahmann
 *
 */
public class CrowdMenu extends AbstractMenu
{
    /**
     * Default serial version id
     */
    private static final long    serialVersionUID = 1L;

    /**
     * {@link JCheckBoxMenuItem} which makes the {@link CrowdSimulator#getCrowds()} visible or not.
     */
    private JCheckBoxMenuItem    checkBoxCrowdOutlines;

    /**
     * {@link JCheckBoxMenuItem} which enables/disables clustering for crowd outlines
     */
    private JCheckBoxMenuItem    checkBoxClusterCrowdOutline;

    /**
     * {@link JRadioButtonMenuItem} sets the crowd outlines to use a convex hull algorithm
     */
    private JRadioButtonMenuItem radioButtonConvex;

    /**
     * {@link JRadioButtonMenuItem} sets the crowd outlines to use a concave hull algorithm
     * (chi-shape, Duckham et al.)
     */
    private JRadioButtonMenuItem radioButtonConcave;

    /**
     * Creates a new {@link CrowdMenu} that allows the user to define the visibilities of crowd
     * outlines
     *
     * @param crowdSimulation reference to the main application
     */
    public CrowdMenu(CrowdSimulation crowdSimulation)
    {
        super("Crowds", crowdSimulation);
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
        CrowdSimulator<VisualCrowd> crowdSimulator = crowdSimulation.getCrowdSimulator();
        if (crowdSimulator == null)
            return;

        checkBoxCrowdOutlines.setSelected(map.isCrowdOutlinesVisible());
        checkBoxClusterCrowdOutline.setSelected(crowdSimulator.isClusteringCrowdOutlines());
        radioButtonConcave.setSelected( !crowdSimulator.isCrowdOutlineConvex());
        radioButtonConvex.setSelected(crowdSimulator.isCrowdOutlineConvex());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addElements()
    {
        add(checkBoxCrowdOutlines);
        add(checkBoxClusterCrowdOutline);

        JMenu crowdOutlineShapeMenu = new JMenu("Shape of Crowd Outlines");
        crowdOutlineShapeMenu.add(radioButtonConvex);
        crowdOutlineShapeMenu.add(radioButtonConcave);
        ButtonGroup crowdOutlineShape = new ButtonGroup();
        crowdOutlineShape.add(radioButtonConvex);
        crowdOutlineShape.add(radioButtonConcave);
        add(crowdOutlineShapeMenu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addListeners()
    {
        // add actionListeners to JCheckBoxMenuItem/JRadioButtonMenuItem
        checkBoxCrowdOutlines.addActionListener(this);
        checkBoxClusterCrowdOutline.addActionListener(this);
        radioButtonConcave.addActionListener(this);
        radioButtonConvex.addActionListener(this);

    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Map map = crowdSimulation.getPanelManager().getComponent(Map.class);
        if (map == null)
            return;
        CrowdSimulator<VisualCrowd> crowdSimulator = crowdSimulation.getCrowdSimulator();
        if (crowdSimulator == null)
            return;
        List<VisualCrowd> currentCrowds = crowdSimulator.getCrowds();

        // visibility of crowd outline
        if (e.getSource() == checkBoxCrowdOutlines)
        {
            boolean selected = checkBoxCrowdOutlines.getModel().isSelected();
            map.setCrowdOutlinesVisible(selected);
        }

        // clustering on/off + shape of clustering (concave/convex)
        if (currentCrowds != null && !currentCrowds.isEmpty())
        {
            if (e.getSource() == checkBoxClusterCrowdOutline)
            {
                crowdSimulator.setClusteringCrowdOutlines(checkBoxClusterCrowdOutline.isSelected());
            }

            if (e.getSource() == radioButtonConcave)
            {
                crowdSimulator.setCrowdOutlinesConvex(false);
            }

            if (e.getSource() == radioButtonConvex)
            {
                crowdSimulator.setCrowdOutlinesConvex(true);
            }
        }

        // repaint the map panel
        map.repaint();
    }

    @Override
    protected void createElements()
    {
        // init items
        checkBoxCrowdOutlines = new JCheckBoxMenuItem("Show Crowd Outlines");
        checkBoxClusterCrowdOutline = new JCheckBoxMenuItem("Cluster Crowd Outlines");
        radioButtonConvex = new JRadioButtonMenuItem("Convex Hulls");
        radioButtonConcave = new JRadioButtonMenuItem("Concave Hulls");
    }
}
