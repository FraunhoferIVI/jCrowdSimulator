package de.fhg.ivi.crowdsimulation.ui.gui.control.menus.simulation;

import java.awt.event.ActionEvent;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;

import de.fhg.ivi.crowdsimulation.CrowdSimulator;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.extension.VisualCrowd;
import de.fhg.ivi.crowdsimulation.ui.gui.control.menus.AbstractMenu;

public class SimulationSpeedMenu extends AbstractMenu
{

    /**
     * default serial version uid
     */
    private static final long    serialVersionUID = 1L;

    /**
     * {@link JRadioButtonMenuItem} for simulation speed 0 (i.e. pause)
     */
    private JRadioButtonMenuItem zero;

    /**
     * {@link JRadioButtonMenuItem} for simulation speed 1 (i.e. normal speed)
     */
    private JRadioButtonMenuItem one;

    /**
     * {@link JRadioButtonMenuItem} for simulation speed 2 (i.e. fast forward factor 2)
     */
    private JRadioButtonMenuItem two;

    /**
     * {@link JRadioButtonMenuItem} for simulation speed 3 (i.e. fast forward factor 5)
     */
    private JRadioButtonMenuItem three;

    /**
     * {@link JRadioButtonMenuItem} for simulation speed 4 (i.e. fast forward factor 5)
     */
    private JRadioButtonMenuItem four;

    /**
     * {@link JRadioButtonMenuItem} for simulation speed 5 (i.e. fast forward factor 5)
     */
    private JRadioButtonMenuItem five;

    /**
     * @param crowdSimulation
     */
    public SimulationSpeedMenu(CrowdSimulation crowdSimulation)
    {
        super("Simulation Speed", crowdSimulation);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        CrowdSimulator<VisualCrowd> crowdSimulator = crowdSimulation.getCrowdSimulator();
        if (e.getSource() == zero)
        {
            crowdSimulator.setFastForwardFactor(0);
        }

        if (e.getSource() == one)
        {
            crowdSimulator.setFastForwardFactor(1);
        }

        if (e.getSource() == two)
        {
            crowdSimulator.setFastForwardFactor(2);
        }
        if (e.getSource() == three)
        {
            crowdSimulator.setFastForwardFactor(3);
        }

        if (e.getSource() == four)
        {
            crowdSimulator.setFastForwardFactor(4);
        }

        if (e.getSource() == five)
        {
            crowdSimulator.setFastForwardFactor(5);
        }

    }

    @Override
    protected void createElements()
    {
        zero = new JRadioButtonMenuItem("0 (i.e. pause)");
        one = new JRadioButtonMenuItem("1 (i.e. real time)");
        two = new JRadioButtonMenuItem("2 (i.e. fast forward factor 2)");
        three = new JRadioButtonMenuItem("3 (i.e. fast forward factor 3)");
        four = new JRadioButtonMenuItem("4 (i.e. fast forward factor 4)");
        five = new JRadioButtonMenuItem("5 (i.e. fast forward factor 5)");
    }

    @Override
    protected void addElements()
    {
        // radio buttons for numeric integrators
        ButtonGroup numericIntegratorChooser = new ButtonGroup();
        numericIntegratorChooser.add(zero);
        numericIntegratorChooser.add(one);
        numericIntegratorChooser.add(two);
        numericIntegratorChooser.add(three);
        numericIntegratorChooser.add(four);
        numericIntegratorChooser.add(five);
        add(zero);
        add(one);
        add(two);
        add(three);
        add(four);
        add(five);
    }

    @Override
    protected void setInitialValues()
    {
        zero.setSelected(crowdSimulation.getCrowdSimulator().getFastForwardFactor() == 0);
        one.setSelected(crowdSimulation.getCrowdSimulator().getFastForwardFactor() == 1);
        two.setSelected(crowdSimulation.getCrowdSimulator().getFastForwardFactor() == 2);
        three.setSelected(crowdSimulation.getCrowdSimulator().getFastForwardFactor() == 3);
        four.setSelected(crowdSimulation.getCrowdSimulator().getFastForwardFactor() == 4);
        five.setSelected(crowdSimulation.getCrowdSimulator().getFastForwardFactor() == 5);
    }

    @Override
    protected void addListeners()
    {
        // add actionListeners
        zero.addActionListener(this);
        one.addActionListener(this);
        two.addActionListener(this);
        three.addActionListener(this);
        four.addActionListener(this);
        five.addActionListener(this);
    }
}
