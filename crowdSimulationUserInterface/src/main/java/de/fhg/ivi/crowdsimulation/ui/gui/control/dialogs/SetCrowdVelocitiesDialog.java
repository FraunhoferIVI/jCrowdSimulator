package de.fhg.ivi.crowdsimulation.ui.gui.control.dialogs;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.ivi.crowdsimulation.CrowdSimulator;
import de.fhg.ivi.crowdsimulation.crowd.Crowd;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.Route;
import de.fhg.ivi.crowdsimulation.math.MathTools;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.extension.VisualCrowd;

/**
 * This class contains a {@link JDialog} that allows selecting a file for a crowd and a file for
 * route to be added to the CrowdSimulation.
 * <p>
 * Furthermore the color of a newly added Crowd can be selected.
 *
 * @author hahmann
 *
 */
public class SetCrowdVelocitiesDialog extends JDialog implements ChangeListener, ActionListener
{

    /**
     * default serial version ID
     */
    private static final long                          serialVersionUID = 1L;

    /**
     * Uses the object logger for printing specific messages in the console.
     */
    @SuppressWarnings("unused")
    private static final Logger                        logger           = LoggerFactory
        .getLogger(SetCrowdVelocitiesDialog.class);

    /**
     * The main {@link CrowdSimulation} frame.
     */
    private CrowdSimulation                            crowdSimulation;

    private float                                      tempMeanNormalVelocity;

    private float                                      tempSdMeanNormalVelocity;

    private float                                      tempMeanMaxVelocity;

    private float                                      tempSdMaxVelocity;

    /**
     * Contains the set of values, which is used for the {@link #meanNormalDesiredVelocitySlider}
     */
    private static final LinkedHashMap<Integer, Float> desiredVelocityValues;
    static
    {
        desiredVelocityValues = new LinkedHashMap<>();
        desiredVelocityValues.put(0, 0f);
        desiredVelocityValues.put(1, 0.5f);
        desiredVelocityValues.put(2, 1f);
        desiredVelocityValues.put(3, 1.5f);
        desiredVelocityValues.put(4, 2f);
        desiredVelocityValues.put(5, 2.5f);
        desiredVelocityValues.put(6, 3f);
        desiredVelocityValues.put(7, 3.5f);
        desiredVelocityValues.put(8, 4f);
        desiredVelocityValues.put(9, 4.5f);
        desiredVelocityValues.put(10, 5f);
        desiredVelocityValues.put(11, 5.5f);
        desiredVelocityValues.put(12, 6f);
        desiredVelocityValues.put(13, 6.5f);
        desiredVelocityValues.put(14, 7f);
        desiredVelocityValues.put(15, 7.5f);
        desiredVelocityValues.put(16, 8.0f);
    }

    /**
     * {@link JSlider} which manipulates the mean of the normal desired velocities of the
     * {@link Pedestrian}s.
     */
    private JSlider                                    meanNormalDesiredVelocitySlider = new JSlider(
        JSlider.HORIZONTAL, 0, desiredVelocityValues.size() - 1, 8);

    /**
     * {@link JLabel} which symbolizes the name for the {@code meanDesiredVelocitySlider}
     * {@link JSlider}.
     */
    private JLabel                                     meanDesiredVelocityLabel        = new JLabel(
        "Mean desired velocity of all pedestrians [km/h]");

    /**
     * Contains the set of values, which is used for the
     * {@link #standardDeviationOfNormalDesiredVelocitySlider}
     */
    private static final LinkedHashMap<Integer, Float> standardDeviationOfDesiredVelocityValues;
    static
    {
        standardDeviationOfDesiredVelocityValues = new LinkedHashMap<>();
        standardDeviationOfDesiredVelocityValues.put(0, 0f);
        standardDeviationOfDesiredVelocityValues.put(1, 0.5f);
        standardDeviationOfDesiredVelocityValues.put(2, 1f);
        standardDeviationOfDesiredVelocityValues.put(3, 1.5f);
        standardDeviationOfDesiredVelocityValues.put(4, 2f);
        standardDeviationOfDesiredVelocityValues.put(5, 2.5f);
        standardDeviationOfDesiredVelocityValues.put(6, 3f);
    }

    /**
     * {@link JSlider} which manipulates the standard deviation of the the normal desired velocities
     * of the {@link Pedestrian}s.
     */
    private JSlider                                    standardDeviationOfNormalDesiredVelocitySlider = new JSlider(
        JSlider.HORIZONTAL, 0, standardDeviationOfDesiredVelocityValues.size() - 1, 2);

    /**
     * {@link JLabel} which symbolizes the description for the
     * {@code standardDeviationOfDesiredVelocitySlider} {@link JSlider}.
     */
    private JLabel                                     standardDeviationOfDesiredVelocityLabel        = new JLabel(
        "Standard deviation of mean desired velocity of all pedestrians [km/h]");

    /**
     * Contains the set of values, which is used for the {@link #meanMaximumVelocitySlider}
     */
    private static final LinkedHashMap<Integer, Float> maximumVelocityValues;
    static
    {
        maximumVelocityValues = new LinkedHashMap<>();
        maximumVelocityValues.put(0, 0f);
        maximumVelocityValues.put(1, 0.5f);
        maximumVelocityValues.put(2, 1f);
        maximumVelocityValues.put(3, 1.5f);
        maximumVelocityValues.put(4, 2f);
        maximumVelocityValues.put(5, 2.5f);
        maximumVelocityValues.put(6, 3f);
        maximumVelocityValues.put(7, 3.5f);
        maximumVelocityValues.put(8, 4f);
        maximumVelocityValues.put(9, 4.5f);
        maximumVelocityValues.put(10, 5f);
        maximumVelocityValues.put(11, 5.5f);
        maximumVelocityValues.put(12, 6f);
        maximumVelocityValues.put(13, 6.5f);
        maximumVelocityValues.put(14, 7f);
        maximumVelocityValues.put(15, 7.5f);
        maximumVelocityValues.put(16, 8.0f);
    }

    /**
     * {@link JSlider} which manipulates the mean of the {@code maximumDesiredVelocity}.
     */
    private JSlider                                    meanMaximumVelocitySlider = new JSlider(
        JSlider.HORIZONTAL, 0, maximumVelocityValues.size() - 1, 12);

    /**
     * {@link JLabel} which symbolizes the description for the {@code meanMaximalVelocitySlider}
     * {@link JSlider}.
     */
    private JLabel                                     meanMaximumVelocityLabel  = new JLabel(
        "Mean maximum velocity of all pedestrians [km/h]");

    /**
     * Contains the set of values, which is used for the
     * {@link #standardDeviationOfNormalDesiredVelocitySlider}
     */
    private static final LinkedHashMap<Integer, Float> standardDeviationOfMaximumVelocityValues;
    static
    {
        standardDeviationOfMaximumVelocityValues = new LinkedHashMap<>();
        standardDeviationOfMaximumVelocityValues.put(0, 0f);
        standardDeviationOfMaximumVelocityValues.put(1, 0.5f);
        standardDeviationOfMaximumVelocityValues.put(2, 1f);
        standardDeviationOfMaximumVelocityValues.put(3, 1.5f);
        standardDeviationOfMaximumVelocityValues.put(4, 2f);
        standardDeviationOfMaximumVelocityValues.put(5, 2.5f);
        standardDeviationOfMaximumVelocityValues.put(6, 3f);
    }

    /**
     * {@link JSlider} which manipulates the standard deviation of the
     * {@code maximumDesiredVelocity}.
     */
    private JSlider standardDeviationOfMaximumVelocitySlider = new JSlider(JSlider.HORIZONTAL, 0,
        standardDeviationOfMaximumVelocityValues.size() - 1, 2);

    /**
     * {@link JLabel} which symbolizes the description for the
     * {@code standardDeviationOfMaximalVelocitySlider} {@link JSlider}.
     */
    private JLabel  standardDeviationOfMaximumVelocityLabel  = new JLabel(
        "Standard deviation of maximum velocity of all pedestrians [km/h]");

    /**
     * A {@link JButton} to leave this {@link JDialog} and add the {@link Crowd} to the crowd
     * simulation
     */
    private JButton okButton;

    /**
     * A {@link JButton} to leave this {@link JDialog} and without any changes to the crowd
     * simulation
     */
    private JButton cancelButton;

    /**
     * Creates new {@link JDialog} allowing to add new {@link Crowd} object with a {@link Route} to
     * the {@link CrowdSimulation}.
     * <p>
     * The dialog dialog blocks user input to other top-level windows when shown.
     *
     * @param crowdSimulation the Frame from which the dialog is displayed
     */
    public SetCrowdVelocitiesDialog(CrowdSimulation crowdSimulation)
    {
        super(crowdSimulation, true);
        // set window size, title and icon
        setSize(450, 350);
        setTitle("Set Crowd Velocities...");

        // set box layout to content pane
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        this.crowdSimulation = crowdSimulation;

        // set temporary velocities
        this.tempMeanNormalVelocity = crowdSimulation.getCrowdSimulator()
            .getMeanNormalDesiredVelocity();
        this.tempSdMeanNormalVelocity = crowdSimulation.getCrowdSimulator()
            .getStandardDeviationOfNormalDesiredVelocity();
        this.tempMeanMaxVelocity = crowdSimulation.getCrowdSimulator()
            .getMeanMaximumDesiredVelocity();
        this.tempSdMaxVelocity = crowdSimulation.getCrowdSimulator()
            .getStandardDeviationOfMaximumVelocity();

        // configure sliders
        configureSliders();

        // sets the initial values of ui elements
        setInitialValues();

        // add input fields
        addSliders();

        // add ok/cancel button
        addButtons();

        // add actionlisteners and changelisteners
        addListeners();

        // center and make visible
        setLocationRelativeTo(crowdSimulation);
    }

    /**
     * Helper method to add the input ui elements to this dialog
     */
    private void addSliders()
    {
        // add sliders and associated labels
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(meanDesiredVelocityLabel);
        add(meanNormalDesiredVelocitySlider);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(standardDeviationOfDesiredVelocityLabel);
        add(standardDeviationOfNormalDesiredVelocitySlider);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(meanMaximumVelocityLabel);
        add(meanMaximumVelocitySlider);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(standardDeviationOfMaximumVelocityLabel);
        add(standardDeviationOfMaximumVelocitySlider);
    }

    /**
     * Calls {@link #sliderSettings(JSlider, HashMap, int)} for {@link JSlider} elements in this
     * {@link JPanel}
     */
    private void configureSliders()
    {
        sliderSettings(meanNormalDesiredVelocitySlider, desiredVelocityValues, 2);
        sliderSettings(standardDeviationOfNormalDesiredVelocitySlider,
            standardDeviationOfDesiredVelocityValues, 2);
        sliderSettings(meanMaximumVelocitySlider, maximumVelocityValues, 2);
        sliderSettings(standardDeviationOfMaximumVelocitySlider,
            standardDeviationOfMaximumVelocityValues, 2);
    }

    /**
     * Sets the initial values of all UI elements (i.e. all {@link JCheckBox}, {@link JRadioButton}
     * and {@link JSlider} objects) according to the initial states of {@link Map} and
     * {@link CrowdSimulator}.
     */
    private void setInitialValues()
    {
        CrowdSimulator<VisualCrowd> crowdSimulator = crowdSimulation.getCrowdSimulator();
        if (crowdSimulator == null)
        {
            return;
        }

        // set velocities
        meanNormalDesiredVelocitySlider.setValue(
            getIndex(desiredVelocityValues, MathTools.convertMsToKmh(tempMeanNormalVelocity)));
        standardDeviationOfNormalDesiredVelocitySlider
            .setValue(getIndex(standardDeviationOfDesiredVelocityValues,
                MathTools.convertMsToKmh(tempSdMeanNormalVelocity)));
        meanMaximumVelocitySlider.setValue(
            getIndex(maximumVelocityValues, MathTools.convertMsToKmh(tempMeanMaxVelocity)));
        standardDeviationOfMaximumVelocitySlider.setValue(getIndex(
            standardDeviationOfMaximumVelocityValues, MathTools.convertMsToKmh(tempSdMaxVelocity)));
    }

    /**
     * Adds {@link ActionListener} to {@link JCheckBox} and {@link JRadioButton} and
     * {@link ChangeListener} to {@link JSlider} objects of this {@link JPanel}
     */
    private void addListeners()
    {
        // add ChangeListener to all JSliders
        meanNormalDesiredVelocitySlider.addChangeListener(this);
        standardDeviationOfNormalDesiredVelocitySlider.addChangeListener(this);
        meanMaximumVelocitySlider.addChangeListener(this);
        standardDeviationOfMaximumVelocitySlider.addChangeListener(this);
    }

    /**
     * Configures the {@link JSlider} {@code slider}. Sets the its possible values according to
     * {@code entries} and the major tick spacing according to {@code majorTickSpacing}
     *
     * @param slider the slider to be configured
     * @param entries the possible values of the slider
     * @param majorTickSpacing the major tick spacing
     */
    private void sliderSettings(JSlider slider, HashMap<Integer, Float> entries,
        int majorTickSpacing)
    {
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        for (Map.Entry<Integer, Float> entry : entries.entrySet())
        {
            if (entry.getKey() % majorTickSpacing == 0)
                labelTable.put(entry.getKey(), new JLabel(entry.getValue().toString()));
        }
        slider.setLabelTable(labelTable);
        slider.setSize(this.getWidth() - 20, 10);
        slider.setMajorTickSpacing(majorTickSpacing);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setSnapToTicks(false);
        slider.setPaintLabels(true);
        slider.setMaximumSize(new Dimension(600, 50));
    }

    /**
     * Contains a specific set of values, which is used for {@link JSlider} components.
     *
     * @param values name and values of the specific {@link LinkedHashMap}, which could be used.
     * @param value of the {@link JSlider}, which should be used.
     *
     * @return a {@link Integer} value, which is set to be the new number of a {@link JSlider}.
     */
    private static int getIndex(LinkedHashMap<Integer, Float> values, float value)
    {
        float difference = Float.MAX_VALUE;
        int index = 0;
        int i = 0;

        // look for an appropriate class value depending on the value
        for (Map.Entry<Integer, Float> entry : values.entrySet())
        {
            float tempDifference = Math.abs(entry.getValue() - value);
            if (tempDifference < difference)
            {
                index = i;
                difference = tempDifference;
            }
            i++ ;
        }
        return index;
    }

    /**
     * Helper method to add the input ui elements to this dialog
     */
    private void addButtons()
    {
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 10);

        buttons.add((okButton = new JButton("Ok")), gbc);
        okButton.addActionListener(this);
        gbc.gridx = 1;
        cancelButton = new JButton("cancel");
        cancelButton.addActionListener(this);
        buttons.add(cancelButton, gbc);

        add(buttons);
    }

    /**
     * Changes the value of an object if a {@link JSlider} will be manipulated.
     *
     * @param changeEvent is the {@link ChangeEvent} which occurs if a {@link JSlider} or so is
     *            changed.
     *
     * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
     */
    @Override
    public void stateChanged(ChangeEvent changeEvent)
    {
        CrowdSimulator<VisualCrowd> crowdSimulator = crowdSimulation.getCrowdSimulator();
        List<VisualCrowd> currentCrowds = crowdSimulator.getCrowds();
        if (currentCrowds != null && !currentCrowds.isEmpty())
        {
            if (changeEvent.getSource() == meanNormalDesiredVelocitySlider)
            {
                if ( !meanNormalDesiredVelocitySlider.getValueIsAdjusting())
                {
                    float desiredVelocityKmh = desiredVelocityValues
                        .get(meanNormalDesiredVelocitySlider.getValue());
                    tempMeanNormalVelocity = MathTools.convertKmhToMs(desiredVelocityKmh);
                }
            }

            if (changeEvent.getSource() == standardDeviationOfNormalDesiredVelocitySlider)
            {
                if ( !standardDeviationOfNormalDesiredVelocitySlider.getValueIsAdjusting())
                {
                    float standardDeviationOfNormalDesiredVelocityKmh = standardDeviationOfDesiredVelocityValues
                        .get(standardDeviationOfNormalDesiredVelocitySlider.getValue());
                    tempSdMeanNormalVelocity = MathTools
                        .convertKmhToMs(standardDeviationOfNormalDesiredVelocityKmh);
                }
            }

            if (changeEvent.getSource() == meanMaximumVelocitySlider)
            {
                if ( !meanMaximumVelocitySlider.getValueIsAdjusting())
                {
                    float maximumVelocityKmh = maximumVelocityValues
                        .get(meanMaximumVelocitySlider.getValue());
                    tempMeanMaxVelocity = MathTools.convertKmhToMs(maximumVelocityKmh);
                }
            }

            if (changeEvent.getSource() == standardDeviationOfMaximumVelocitySlider)
            {
                if ( !standardDeviationOfMaximumVelocitySlider.getValueIsAdjusting())
                {
                    float standardDeviationOfMaximumDesiredVelocityKmh = standardDeviationOfMaximumVelocityValues
                        .get(standardDeviationOfMaximumVelocitySlider.getValue());
                    tempSdMaxVelocity = MathTools
                        .convertKmhToMs(standardDeviationOfMaximumDesiredVelocityKmh);
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == okButton)
        {
            crowdSimulation.getCrowdSimulator()
                .setMeanNormalDesiredVelocity(tempMeanNormalVelocity);
            crowdSimulation.getCrowdSimulator()
                .setStandardDeviationOfNormalDesiredVelocity(tempSdMeanNormalVelocity);
            crowdSimulation.getCrowdSimulator().setMeanMaximumDesiredVelocity(tempMeanMaxVelocity);
            crowdSimulation.getCrowdSimulator()
                .setStandardDeviationOfMaximumVelocity(tempSdMaxVelocity);
        }
        dispose();
    }
}
