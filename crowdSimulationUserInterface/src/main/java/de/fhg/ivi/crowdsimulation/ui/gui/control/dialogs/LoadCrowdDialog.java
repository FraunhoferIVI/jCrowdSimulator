package de.fhg.ivi.crowdsimulation.ui.gui.control.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.ivi.crowdsimulation.CrowdSimulatorNotValidException;
import de.fhg.ivi.crowdsimulation.crowd.Crowd;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.Route;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.extension.VisualCrowd;
import de.fhg.ivi.crowdsimulation.ui.extension.VisualCrowdSimulator;
import de.fhg.ivi.crowdsimulation.ui.gui.tools.FileTools;
import jiconfont.icons.FontAwesome;
import jiconfont.swing.IconFontSwing;

/**
 * This class contains a {@link JDialog} that allows selecting a file for a crowd and a file for
 * route to be added to the CrowdSimulation.
 * <p>
 * Furthermore the color of a newly added Crowd can be selected.
 *
 * @author hahmann
 *
 */
public class LoadCrowdDialog extends JDialog implements ActionListener
{

    /**
     * default serial version ID
     */
    private static final long   serialVersionUID = 1L;

    /**
     * Uses the object logger for printing specific messages in the console.
     */
    @SuppressWarnings("unused")
    private static final Logger logger           = LoggerFactory.getLogger(LoadCrowdDialog.class);

    /**
     * The main {@link CrowdSimulation} frame.
     */
    private CrowdSimulation     crowdSimulation;

    /**
     * The selected file for to contain the coordinates for the pedestrians for the new crowd
     */
    private File                pedestriansFile;

    /**
     * A {@link JTextField} to show the absolute file path of {@link #pedestriansFile}
     */
    private JTextField          pedestriansFileName;

    /**
     * The selected file for to contain the coordinates for the route for the new crowd
     */
    private File                routeFile;

    /**
     * A {@link JTextField} to show the absolute file path of {@link #routeFileName}
     */
    private JTextField          routeFileName;

    /**
     * A {@link JButton} to open a {@link JFileChooser} to select the {@link Pedestrian} input file
     */
    private JButton             choosePedestriansButton;

    /**
     * A {@link JButton} to open a {@link JColorChooser} to select the color for the new crowd
     * object
     */
    private JButton             chooseColorButton;

    /**
     * A {@link JButton} to open a {@link JFileChooser} to select the {@link Route} input file
     */
    private JButton             chooseRouteButton;

    /**
     * A {@link JButton} to leave this {@link JDialog} and add the {@link Crowd} to the crowd
     * simulation
     */
    private JButton             okButton;

    /**
     * A {@link JButton} to leave this {@link JDialog} and without any changes to the crowd
     * simulation
     */
    private JButton             cancelButton;

    /**
     * The {@link Color} of the new crowd object to be added
     */
    private Color               crowdColor;

    /**
     * The default color to be shown defaults to {@link Color#BLUE}
     */
    private final static Color  DEFAULT_COLOR    = Color.BLUE;

    /**
     * Creates new {@link JDialog} allowing to add new {@link Crowd} object with a {@link Route} to
     * the {@link CrowdSimulation}.
     * <p>
     * The dialog dialog blocks user input to other top-level windows when shown.
     *
     * @param crowdSimulation the Frame from which the dialog is displayed
     */
    public LoadCrowdDialog(CrowdSimulation crowdSimulation)
    {
        super(crowdSimulation, true);
        // set window size, title and icon
        setSize(400, 150);
        setTitle("Load Crowd...");
        Icon icon = IconFontSwing.buildIcon(FontAwesome.USERS, 12, Color.black);
        if (icon instanceof ImageIcon)
            setIconImage(((ImageIcon) icon).getImage());

        // set box layout to content pane
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        crowdColor = DEFAULT_COLOR;
        this.crowdSimulation = crowdSimulation;

        // add input fields
        addInputs();

        // add ok/cancel button
        addButtons();

        // center and make visible
        setLocationRelativeTo(crowdSimulation);
    }

    /**
     * Helper method to add the input ui elements to this dialog
     */
    private void addInputs()
    {
        JPanel inputs = new JPanel();
        inputs.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 0, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = 0;
        inputs.add(new JLabel("Pedestrians: "), gbc);

        gbc.gridy = 1;
        gbc.weightx = 0.90;
        inputs.add((pedestriansFileName = new JTextField()), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.05;
        inputs.add((choosePedestriansButton = new JButton("...")), gbc);
        choosePedestriansButton.addActionListener(this);
        gbc.gridx = 2;
        gbc.weightx = 0.05;
        chooseColorButton = new JButton();
        chooseColorButton.setBackground(crowdColor);
        chooseColorButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        chooseColorButton.setPreferredSize(new Dimension(20, 20));
        chooseColorButton.addActionListener(this);
        inputs.add(chooseColorButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        inputs.add(new JLabel("Route:"), gbc);
        gbc.gridy = 3;
        inputs.add((routeFileName = new JTextField()), gbc);
        gbc.gridx = 1;
        inputs.add((chooseRouteButton = new JButton("...")), gbc);
        chooseRouteButton.addActionListener(this);

        add(inputs);
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
     * {@inheritDoc}
     * <p>
     * Handles actions performed on {@link #chooseColorButton}, {@link #choosePedestriansButton},
     * {@link #chooseRouteButton}, {@link #okButton} and {@link #cancelButton}
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == chooseColorButton)
        {
            Color chosenColor = JColorChooser.showDialog(this, "Choose Color...", crowdColor);
            if (chosenColor != null)
            {
                crowdColor = chosenColor;
                chooseColorButton.setBackground(crowdColor);
            }
        }
        else if (e.getSource() == choosePedestriansButton)
        {
            pedestriansFile = FileTools.chooseFile(this);
            if (pedestriansFile != null)
                pedestriansFileName.setText(pedestriansFile.getAbsolutePath());
        }
        else if (e.getSource() == chooseRouteButton)
        {
            routeFile = FileTools.chooseFile(this);
            if (routeFile != null)
                routeFileName.setText(routeFile.getAbsolutePath());
        }
        else if (e.getSource() == okButton)
        {
            if (pedestriansFile == null)
            {
                // dialog to inform user that no pedestrian file is given
                JOptionPane.showMessageDialog(this, "No Pedestrian File given", "Warning",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            try
            {
                crowdSimulation.loadCrowdAndRoute(pedestriansFile, routeFile, crowdColor, false);
            }
            catch (CrowdSimulatorNotValidException ex)
            {
                VisualCrowdSimulator cs = crowdSimulation.getCrowdSimulator();
                boolean validateStrict = true;
                validateStrict &= cs.getBoundaries() != null && !cs.getBoundaries().isEmpty();
                validateStrict &= cs.getCrowds() != null && !cs.getCrowds().isEmpty();
                boolean crowdWithRoute = false;
                if (cs.getCrowds() != null && !cs.getCrowds().isEmpty())
                {
                    for (VisualCrowd crowd : cs.getCrowds())
                    {
                        crowdWithRoute |= crowd.getRoute() != null;
                    }
                }
                validateStrict &= crowdWithRoute;

                if (validateStrict)
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Warning",
                        JOptionPane.WARNING_MESSAGE);
            }
            dispose();
        }
        else if (e.getSource() == cancelButton)
        {
            dispose();
        }
    }
}
