package de.fhg.ivi.crowdsimulation.ui.gui.visualisation;

import java.awt.Color;
import java.awt.Dimension;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.ivi.crowdsimulation.CrowdSimulator;
import de.fhg.ivi.crowdsimulation.crowd.Crowd;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.math.MathTools;
import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;
import de.fhg.ivi.crowdsimulation.ui.extension.VisualCrowd;
import de.fhg.ivi.crowdsimulation.ui.gui.tools.TimeTools;

/**
 * This class represents the {@link JPanel} at the bottom of the application. It shows general and
 * technical information about the simulation (such as time of simulation, frame rate, crowd density
 * and so on).
 *
 * @author hahmann/meinert
 */
public class InfoBar extends JPanel implements Runnable
{
    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger logger              = LoggerFactory.getLogger(InfoBar.class);

    /**
     * default serial version ID
     */
    private static final long   serialVersionUID    = 1L;

    /**
     * Class for executing the application.
     */
    private CrowdSimulation     crowdSimulation;

    /**
     * Storage variable for the last time than the simulation was refreshed. Given in nanoseconds.
     */
    private long                lastInfoRefreshTime = 0;

    /**
     * The minimum interval between two consecutive refreshs in nanoseconds
     */
    private int                 refreshInterval     = 200_000_000;

    /**
     * Checks if the thread are running or not.
     */
    private boolean             infoThreadRunning;

    /**
     * {@link JLabel}, which shows the version of crowdsimui and crowdsimlib.
     */
    private JLabel              versionLabel        = new JLabel(" ");

    /**
     * {@link JLabel} which shows whether the simulation is running or not.
     */
    private JLabel              runningLabel        = new JLabel(" ");

    /**
     * {@link JLabel} which shows how is the density of {@link Pedestrian} per m².
     */
    private JLabel              crowdDensityLabel   = new JLabel(" ");

    /**
     * {@link JLabel} which shows how long the simulation is running in simulation time.
     */
    private JLabel              simulatedTimeLabel  = new JLabel(" ");

    /**
     * {@link JLabel} which shows how much simulation steps were executed in real time.
     */
    private JLabel              actualTimeLabel     = new JLabel(" ");

    /**
     * {@link JLabel} which shows how much simulation steps were executed in simulation time.
     */
    private JLabel              simulationTimeLabel = new JLabel(" ");

    /**
     * {@link JLabel} which shows how often the graphic is refreshing per second.
     */
    private JLabel              graphicRefreshLabel = new JLabel(" ");

    /**
     * Constructor.
     * <p>
     * Sets layout characteristics of this panel and adds the {@link JLabel}s to this panel.
     */
    public InfoBar(CrowdSimulation crowdSimulation)
    {
        this.crowdSimulation = crowdSimulation;

        // sets width and height of this panel
        this.setSize(20, 88);

        // if BoxLayouut is set on Y-Axis one can only manipulate the height
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(0, (int) (getSize().getHeight() + 10)));

        versionLabel.setText(
            crowdSimulation.getVersion() + ", " + crowdSimulation.getCrowdSimulator().getVersion());

        // add JLabels to this JPanel
        add(runningLabel);
        add(crowdDensityLabel);
        add(simulatedTimeLabel);
        add(actualTimeLabel);
        add(simulationTimeLabel);
        add(graphicRefreshLabel);
        add(versionLabel);

        // start values for JLabels
        crowdDensityLabel.setText("average crowd density=");
        simulatedTimeLabel.setText("simulated time span=");
        actualTimeLabel.setText("simulation steps/second (real time)=");
        simulationTimeLabel.setText("simulation steps/second (simulated time)=");
        graphicRefreshLabel.setText("graphic refreshs/second=");
    }

    /**
     * Checks if the {@code infoThread} is running or not.
     *
     * @return true or false in case the {@code infoThreadRunning} is running or not
     */
    public boolean isInfoThreadRunning()
    {
        return infoThreadRunning;
    }

    /**
     * Sets the {@code infoThreadRunning} as running (true) or not (false).
     *
     * @param infoThreadRunning could be true or false
     */
    public void setInfoThreadRunning(boolean infoThreadRunning)
    {
        this.infoThreadRunning = infoThreadRunning;
    }

    /**
     * Thread for {@link InfoBar} class. Thread is running if {@link InfoBar#infoThreadRunning}} is
     * true.
     * <p>
     * Through the thread the values for the {@link JLabel} can be updated automatically.
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        CrowdSimulator<VisualCrowd> crowdSimulator = crowdSimulation.getCrowdSimulator();
        Map map = crowdSimulation.getPanelManager().getComponent(Map.class);

        // infinite loop runs until infoThreadRunning is false
        while (infoThreadRunning)
        {
            // don't refresh more than 5 times per second / only every 200ms
            long timeSinceLastRefresh = System.nanoTime() - lastInfoRefreshTime;
            if (timeSinceLastRefresh < refreshInterval)
            {
                try
                {
                    long sleepInterval = refreshInterval - timeSinceLastRefresh;
                    long sleepIntervalMs = TimeUnit.NANOSECONDS.toMillis(sleepInterval);
                    long sleepIntervalremainingNanos = (sleepInterval) % 1000000;
                    Thread.sleep(sleepIntervalMs, (int) sleepIntervalremainingNanos);
                }
                catch (InterruptedException e)
                {
                    logger.debug("sleep interrupted", e);
                }
            }

            long currentTime = System.nanoTime();

            // write whether simulation thread is running
            boolean simulationRunning = crowdSimulator.isSimulationRunning();

            if (simulationRunning)
            {
                if (crowdSimulator.getFastForwardFactor() == 0)
                {
                    runningLabel.setForeground(new Color(217, 95, 2));
                    runningLabel.setText("The simulation is paused");
                }
                else
                {
                    runningLabel.setForeground(new Color(0, 128, 1));
                    runningLabel.setText("The simulation is running");
                }
            }
            else
            {
                runningLabel.setForeground(Color.RED);
                runningLabel.setText("The simulation is not running");
            }

            // write crowd density
            double density = 0;
            int size = 0;

            List<? extends Crowd> currentCrowds = crowdSimulator.getCrowds();
            if (currentCrowds != null && !currentCrowds.isEmpty())
            {
                for (Crowd crowd : currentCrowds)
                {
                    density += crowd.getCrowdDensity();
                    size += crowd.getSize();
                }
                crowdDensityLabel.setText(
                    "average crowd density= " + MathTools.round(density / currentCrowds.size(), 2)
                        + " persons/m², total pedestrians=" + size + " in " + currentCrowds.size()
                        + " crowd(s)");
            }

            // draw simulated time
            long simulatedTimespan = crowdSimulator.getSimulatedTimeSpan();
            simulatedTimeLabel.setText("simulated time span="
                + TimeTools.getMinutesAndSecondsTimeRepresentation(simulatedTimespan));

            if (crowdSimulator.getSimulationUpdateInterval() > 0)
            {
                double simulationStepsPerSecond = 1
                    / (crowdSimulator.getSimulationUpdateInterval());
                double averageSimulationStepsPerSecond = 1
                    / (crowdSimulator.getAverageSimulationUpdateInterval());

                // write simulation steps per second in real time
                actualTimeLabel.setText("simulation steps/second (real time)="
                    + Math.round(simulationStepsPerSecond * crowdSimulator.getFastForwardFactor()));

                // write simulation steps per second in simulated time
                simulationTimeLabel.setText("simulation steps/second (simulated time)="
                    + Math.round(simulationStepsPerSecond) + ", average="
                    + Math.round(averageSimulationStepsPerSecond) + ", average time/step="
                    + MathTools.round(crowdSimulator.getAverageSimulationUpdateInterval() * 1000, 1)
                    + " ms");

                // write graphic refresh per second
                graphicRefreshLabel.setText(
                    "graphic refreshs/second=" + Math.round(map.getGraphicRefreshsPerSecond()));
            }

            // set last info refresh time
            lastInfoRefreshTime = currentTime;
        }
    }
}
