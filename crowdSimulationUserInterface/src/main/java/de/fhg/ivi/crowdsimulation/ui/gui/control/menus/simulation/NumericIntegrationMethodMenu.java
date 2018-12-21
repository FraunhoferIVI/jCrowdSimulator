package de.fhg.ivi.crowdsimulation.ui.gui.control.menus.simulation;

import java.awt.event.ActionEvent;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;

import de.fhg.ivi.crowdsimulation.CrowdSimulator;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.NumericIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.RungeKuttaIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.SemiImplicitEulerIntegrator;
import de.fhg.ivi.crowdsimulation.crowd.forcemodel.numericintegration.SimpleEulerIntegrator;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.extension.VisualCrowd;
import de.fhg.ivi.crowdsimulation.ui.gui.control.menus.AbstractMenu;

public class NumericIntegrationMethodMenu extends AbstractMenu
{

    /**
     * default serial version uid
     */
    private static final long    serialVersionUID = 1L;

    /**
     * {@link JRadioButtonMenuItem} for setting {@link NumericIntegrator} of {@link CrowdSimulator}
     * to {@link SimpleEulerIntegrator}
     */
    private JRadioButtonMenuItem radioButtonSimpleEuler;

    /**
     * {@link JRadioButtonMenuItem} for setting {@link NumericIntegrator} of {@link CrowdSimulator}
     * to {@link SemiImplicitEulerIntegrator}
     */
    private JRadioButtonMenuItem radioButtonSemiImplicitEuler;

    /**
     * {@link JRadioButtonMenuItem} for setting {@link NumericIntegrator} of {@link CrowdSimulator}
     * to {@link RungeKuttaIntegrator}
     */
    private JRadioButtonMenuItem radioButtonRungeKutta;

    /**
     * @param crowdSimulation
     */
    public NumericIntegrationMethodMenu(CrowdSimulation crowdSimulation)
    {
        super("Numeric Integration Method", crowdSimulation);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        CrowdSimulator<VisualCrowd> crowdSimulator = crowdSimulation.getCrowdSimulator();
        if (e.getSource() == radioButtonSimpleEuler)
        {
            crowdSimulator.setNumericIntegrator(new SimpleEulerIntegrator());
        }

        if (e.getSource() == radioButtonSemiImplicitEuler)
        {
            crowdSimulator.setNumericIntegrator(new SemiImplicitEulerIntegrator());
        }

        if (e.getSource() == radioButtonRungeKutta)
        {
            crowdSimulator.setNumericIntegrator(new RungeKuttaIntegrator());
        }

    }

    @Override
    protected void createElements()
    {
        radioButtonSemiImplicitEuler = new JRadioButtonMenuItem("Semi Implicit Euler");
        radioButtonSimpleEuler = new JRadioButtonMenuItem("Simple Euler");
        radioButtonRungeKutta = new JRadioButtonMenuItem("Runge Kutta");
    }

    @Override
    protected void addElements()
    {
        // radio buttons for numeric integrators
        ButtonGroup numericIntegratorChooser = new ButtonGroup();
        numericIntegratorChooser.add(radioButtonSemiImplicitEuler);
        numericIntegratorChooser.add(radioButtonSimpleEuler);
        numericIntegratorChooser.add(radioButtonRungeKutta);
        add(radioButtonSemiImplicitEuler);
        add(radioButtonSimpleEuler);
        add(radioButtonRungeKutta);
    }

    @Override
    protected void setInitialValues()
    {
        radioButtonSemiImplicitEuler.setSelected(crowdSimulation.getCrowdSimulator()
            .getNumericIntegrator() instanceof SemiImplicitEulerIntegrator);
        radioButtonSimpleEuler.setSelected(crowdSimulation.getCrowdSimulator()
            .getNumericIntegrator() instanceof SimpleEulerIntegrator);
        radioButtonRungeKutta.setSelected(crowdSimulation.getCrowdSimulator()
            .getNumericIntegrator() instanceof RungeKuttaIntegrator);
    }

    @Override
    protected void addListeners()
    {
        // add actionListeners
        radioButtonSimpleEuler.addActionListener(this);
        radioButtonSemiImplicitEuler.addActionListener(this);
        radioButtonRungeKutta.addActionListener(this);
    }
}
