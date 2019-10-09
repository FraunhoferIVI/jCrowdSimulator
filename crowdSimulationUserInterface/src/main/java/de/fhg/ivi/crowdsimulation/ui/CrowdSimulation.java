package de.fhg.ivi.crowdsimulation.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ScrollPane;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.ivi.crowdsimulation.CrowdSimulator;
import de.fhg.ivi.crowdsimulation.CrowdSimulatorNotValidException;
import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.crowd.Crowd;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.Route;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.WayPoint;
import de.fhg.ivi.crowdsimulation.geom.GeometryTools;
import de.fhg.ivi.crowdsimulation.io.DataTools;
import de.fhg.ivi.crowdsimulation.ui.extension.VisualCrowd;
import de.fhg.ivi.crowdsimulation.ui.extension.VisualCrowdSimulator;
import de.fhg.ivi.crowdsimulation.ui.gui.ComponentManager;
import de.fhg.ivi.crowdsimulation.ui.gui.control.MenuBar;
import de.fhg.ivi.crowdsimulation.ui.gui.control.Toolbar;
import de.fhg.ivi.crowdsimulation.ui.gui.tools.FileTools;
import de.fhg.ivi.crowdsimulation.ui.gui.visualisation.InfoBar;
import de.fhg.ivi.crowdsimulation.ui.gui.visualisation.Map;
import de.fhg.ivi.crowdsimulation.validate.ValidationTools;

/**
 * Main class of the simulation.
 * <p>
 * This class creates 2 {@link Thread}s to run parallel activities. The {@link #graphicsThread}
 * providing a thread for the display of the graphic, see {@link Map}, and the {@link #infoThread},
 * which provides a thread for the output of informations on the state of the simulation, see
 * {@link InfoBar}.
 * <p>
 * Besides that this class can control a {@link CrowdSimulator} object that is capable to run a
 * simulation.
 * <p>
 * Furthermore all parts of the GUI are invoked in this class and added to the
 * {@link ComponentManager}, which manages these classes.
 * <p>
 * Farther importing and editing of all kinds of necessary geometries in case of this crowd
 * simulation is done implemented here. The necessary geometry are represented through the classes
 * {@link Pedestrian}, {@link Boundary} and {@link WayPoint}.
 *
 * @author hahmann/meinert
 */
public class CrowdSimulation extends JFrame
{
    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger  logger           = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());

    /**
     * default serial version ID
     */
    private static final long    serialVersionUID = 1L;

    /**
     * Main simulation object that contains the lists of {@link Pedestrian} objects, boundaries and
     * waypoints and computes the actual simulation.
     */
    private VisualCrowdSimulator crowdSimulator;

    /**
     * Threading for implementation/drawing of graphics.
     */
    private Thread               graphicsThread;

    /**
     * Threading for informations in the {@link InfoBar}.
     */
    private Thread               infoThread;

    /**
     * This object represents the "Map" class, this means the part in which buildings, waypoints,
     * pedestrians and so on are drawn, and in which the simulation will be visible.
     */
    private Map                  map;

    /**
     * This object represents the {@link JPanel} for logging informations over the simulation.
     */
    private InfoBar              infoBar;

    /**
     * Object of this management class, which gets and sets all {@link JPanel} classes.
     */
    private ComponentManager     componentManager;

    /**
     * Constructor of {@link CrowdSimulation}
     * <p>
     * Calls {@link #loadInitialData()} to load default data into the simulation.
     * <p>
     * Definitions for all parts of the GUI are set here, which includes the size of the screen, the
     * {@link ImageIcon}, a {@link ScrollPane} and so on.
     * <p>
     * Starts the {@link #infoThread}.
     */
    public CrowdSimulation()
    {
        // initialize crowd simulator
        crowdSimulator = new VisualCrowdSimulator();

        // creating geometries for GUI
        loadInitialData();

        // background, size and layout
        setBackground(Color.WHITE);
        setSize(800, 1040);
        setLayout(new BorderLayout());

        // the panel manager
        componentManager = new ComponentManager();

        // the application panel (contains map and info panel)
        JPanel application = new JPanel();
        application.setLayout(new BorderLayout());

        // the map showing the simulation
        map = new Map(crowdSimulator);
        application.add(BorderLayout.CENTER, map);
        componentManager.addComponent(map);

        // Panel for informations about the simulation
        infoBar = new InfoBar(this);
        application.add(BorderLayout.SOUTH, infoBar);
        componentManager.addComponent(infoBar);

        // The Toolbar and MenuBar (at the top)
        Toolbar toolbar = new Toolbar(this);
        setJMenuBar(new MenuBar(this));

        add(BorderLayout.NORTH, toolbar);
        add(application);

        // make JFrame visible
        setVisible(true);

        // set window icon and title
        setIconImage(new ImageIcon("src/main/resources/icon/TargetIcon_32.png").getImage());
        setTitle("jCrowdSimulator");

        // start info thread
        startInfoThread();

        // request focus in map
        map.requestFocus();

        // stop all thread, when JFrame is closed
        addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent e)
                {
                    stopGraphicsThread();
                    stopInfoThread();
                    stopSimulation();
                    System.exit(0);
                }
            });
    }

    /**
     * Gets an object of {@link CrowdSimulator}.
     *
     * @return an object of the class {@link CrowdSimulator}
     */
    public VisualCrowdSimulator getCrowdSimulator()
    {
        return crowdSimulator;
    }

    /**
     * Proofs whether all preconditions for the start of the simulation are fulfilled.
     * <p>
     * If conditions are fulfilled the {@link #crowdSimulator} is started to start the simulation.
     */
    public void startSimulation()
    {
        try
        {
            // checks, if the crowd simulator is valid
            ValidationTools.validate(crowdSimulator.getCrowds(), crowdSimulator.getBoundaries());

            // set current system time as starting time for pedestrians
            for (Crowd crowd : crowdSimulator.getCrowds())
            {
                for (Pedestrian pedestrian : crowd.getPedestrians())
                {
                    pedestrian.getActiveWayFindingModel()
                        .setStartTime(crowdSimulator.getFastForwardClock().currentTimeMillis());
                }
            }

            // starts the thread for the simulation itself
            crowdSimulator.start();
        }
        catch (CrowdSimulatorNotValidException e)
        {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Warning",
                JOptionPane.WARNING_MESSAGE);
            logger.debug("CrowdSimulation.startSimulationThread(), ", e);
        }
    }

    /**
     * Stops the {@link #crowdSimulator} to run the simulation.
     */
    public void stopSimulation()
    {
        crowdSimulator.stop();
        crowdSimulator.setLastSimulationTime(0);
    }

    /**
     * Starts the {@link #graphicsThread}, but only in the case, that the simulation is running. is
     * running too.
     */
    public void startGraphicsThread()
    {
        // only start the graphics
        if (crowdSimulator.isSimulationRunning())
        {
            graphicsThread = new Thread(map);
            graphicsThread.start();
            map.setGraphicsThreadRunning(true);
        }
    }

    /**
     * Stops the {@link #graphicsThread}.
     */
    public void stopGraphicsThread()
    {
        map.setGraphicsThreadRunning(false);
    }

    /**
     * Starts the {@link #infoThread}.
     */
    public void startInfoThread()
    {
        infoThread = new Thread(infoBar);
        infoThread.start();
        infoBar.setInfoThreadRunning(true);
    }

    /**
     * Stops the {@link #infoThread}.
     */
    public void stopInfoThread()
    {
        infoBar.setInfoThreadRunning(false);
    }

    /**
     * Gets an object of {@link ComponentManager}.
     *
     * @return an object of the {@link ComponentManager}
     */
    public ComponentManager getPanelManager()
    {
        return componentManager;
    }

    /**
     * Sets the {@link ComponentManager} object.
     *
     * @param panelManager object of the {@link ComponentManager} which organizes all
     *            {@link JPanel}s.
     */
    public void setPanelManager(ComponentManager panelManager)
    {
        this.componentManager = panelManager;
    }

    /**
     * Loads the default data for {@link Boundary}s, {@link WayPoint}s and {@link Pedestrian}s
     * objects.
     */
    private void loadInitialData()
    {
        try
        {

            // debug data
            // loadCrowdAndRoute(new
            // File("src/main/resources/data/debuggingFiles/debugFivePeds.shp"),
            // new File("src/main/resources/data/wayPointFiles/wayPointsDetour.shp"), Color.GREEN,
            // false);
            // loadCrowdAndRoute(
            // new File("src/main/resources/data/debuggingFiles/debugTwentyPeds.shp"),
            // new File("src/main/resources/data/wayPointFiles/wayPointDetour.shp"), Color.GREEN,
            // false);

            // debug data for johansson's modeling approach
            // loadCrowdAndRoute(new File("src/main/resources/data/debug/debugOnePedTop.csv"),
            // new File("src/main/resources/data/dresden/waypoints1.shp"), Color.BLUE, false);

            // test data dresden prager strasse
            loadCrowdAndRoute(new File("src/main/resources/data/dresden/crowd1.shp"),
                new File("src/main/resources/data/dresden/waypoints1.shp"), Color.BLUE, false);
            loadCrowdAndRoute(new File("src/main/resources/data/dresden/crowd2.shp"),
                new File("src/main/resources/data/dresden/waypoints2.shp"), Color.RED, false);
            loadBoundaries(new File("src/main/resources/data/dresden/boundaries.shp"), false);

            // test data berlin airport schoenefeld
            // loadCrowdAndRoute(new File("src/main/resources/data/berlin/crowd1.shp"),
            // new File("src/main/resources/data/berlin/waypoints1.shp"), Color.BLUE, false);
            // loadCrowdAndRoute(new File("src/main/resources/data/berlin/crowd2.shp"),
            // new File("src/main/resources/data/berlin/waypoints2.shp"), Color.RED, false);
            // loadBoundaries(
            // new File(
            // "src/main/resources/data/berlin/boundaries_GK5_dissolve_singlepart_lines_simplified_split.shp"),
            // false);
        }
        catch (CrowdSimulatorNotValidException e)
        {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Warning",
                JOptionPane.WARNING_MESSAGE);
            logger.debug("CrowdSimulation.loadInitialData(), ", e);
        }
        catch (RuntimeException e)
        {
            if (e.getCause() instanceof FileNotFoundException)
                logger.debug("default data not completely available.");
        }
        catch (FileNotFoundException e)
        {
            logger.debug("default data not completely available.");
        }
        catch (IOException e)
        {
            logger.debug("error reading default data.", e);
        }
    }

    /**
     * Loads all {@link Boundary} objects as a {@link List} of {@link Geometry} objects from the
     * {@code file}, adds them to the {@link CrowdSimulator} and draws them in the {@link Map}.
     * <p>
     * Furthermore this method sets a {@link BoundingBox} in {@link CrowdSimulator} as some kind of
     * reference region for the whole model. This bounding box based on the extent of the
     * {@link Boundary}s (the extent of the bounding box is also based on the {@link Geometry} of
     * the {@link Pedestrian}s).
     *
     * @param file {@link File} as WKT data or .shp data
     * @param ignoreInvalid if {@code true} in case Validation checks yield validation errors, only
     *            a warning is printed into the log output, otherwise
     *            {@link CrowdSimulatorNotValidException} is thrown
     *
     * @throws CrowdSimulatorNotValidException
     */
    public void loadBoundaries(File file, boolean ignoreInvalid)
        throws CrowdSimulatorNotValidException, RuntimeException, FileNotFoundException, IOException
    {
        if (file == null)
        {
            file = FileTools.chooseFile(this);
        }

        // do nothing if no file has been chosen
        if (file == null)
            return;

        List<Geometry> geometries = DataTools.loadGeometriesFromFile(file);

        // only use boundaries if the List<> isn't null
        if (geometries != null && !geometries.isEmpty())
        {
            // set crs
            CoordinateReferenceSystem crs = DataTools.getCRSFromFile(file);
            crowdSimulator.setCrs(crs);

            // set Boundaries
            crowdSimulator.addBoundaries(geometries, ignoreInvalid);

            // set bounding box
            crowdSimulator.expandBoundingBox(GeometryTools.getEnvelope(geometries));

            // paint
            if (map != null)
            {
                map.resetMapExtent();
                map.repaint();
            }
        }
    }

    /**
     * Loads {@link Pedestrian} objects by converting {@link Coordinate} objects read from a given
     * {@code pedestriansFile} that contains {@link Geometry} objects (usually they should be
     * {@link Point} objects, but other {@link Geometry} types work as well using their centroids)
     * into {@link Pedestrian} objects.
     * <p>
     * Additionally loads a {@link Route} from a second given file that contains {@link Geometry}
     * objects
     *
     *
     * converts them to V adds them to the {@link CrowdSimulator} and draws them in the {@link Map}.
     * <p>
     * Based on these {@link Pedestrian}s a single {@link Crowd} is created and added to the
     * CrowdSimulator.
     *
     * @param pedestriansFile {@link File} as WKT data or .shp data
     * @param routeFile {@link File} as WKT data or .shp data
     * @param ignoreInvalid if {@code true} in case Validation checks yield validation errors, only
     *            a warning is printed into the log output, otherwise
     *            {@link CrowdSimulatorNotValidException} is thrown
     *
     * @throws CrowdSimulatorNotValidException
     */
    public void loadCrowdAndRoute(File pedestriansFile, File routeFile, Color crowdColor,
        boolean ignoreInvalid)
        throws CrowdSimulatorNotValidException, RuntimeException, FileNotFoundException, IOException
    {
        // do nothing if no file has been chosen
        if (pedestriansFile == null)
            return;

        List<Geometry> pedestrians = DataTools.loadGeometriesFromFile(pedestriansFile);

        // only use pedestrians if the List<> isn't null
        if (pedestrians != null && !pedestrians.isEmpty())
        {
            // get crs of pedestrians file
            CoordinateReferenceSystem crsPedestriansFile = DataTools
                .getCRSFromFile(pedestriansFile);

            // create crowd object
            VisualCrowd crowd = crowdSimulator.createVisualCrowd(pedestrians, ignoreInvalid,
                crowdColor);

            if (routeFile != null)
            {
                // waypoint geometries
                List<Geometry> wayPointGeometries = DataTools.loadGeometriesFromFile(routeFile);

                // only use wayPoints if the List<> isn't null or empty
                if (wayPointGeometries != null && !wayPointGeometries.isEmpty())
                {
                    // get crs of route file
                    CoordinateReferenceSystem crsRouteFile = DataTools.getCRSFromFile(routeFile);

                    if (crsPedestriansFile != null && crsRouteFile != null
                        && !crsPedestriansFile.equals(crsRouteFile))
                    {
                        throw new CrowdSimulatorNotValidException(
                            "Datasets with different Coordinate Reference Systems are loaded, pedestrians:"
                                + crsPedestriansFile + " != route:" + crsRouteFile);
                    }

                    Route route = crowdSimulator.getRouteFactory()
                        .createRouteFromGeometries(wayPointGeometries);
                    crowd.setRoute(route, crowdSimulator.getFastForwardClock().currentTimeMillis(),
                        ignoreInvalid);
                }
            }

            // set crs
            crowdSimulator.setCrs(crsPedestriansFile);

            // add crowd
            crowdSimulator.addCrowd(crowd, false);

            // expand bounding box by crowd
            Envelope e = crowd.getBoundingBox();
            crowdSimulator.expandBoundingBox(e);

            // expand bounding box by route of crowd
            if (crowd.getRoute() != null)
                crowdSimulator.expandBoundingBox(crowd.getRoute().getBoundingBox());
        }

        // paint
        if (map != null)
        {
            map.resetMapExtent();
            map.repaint();
        }

    }

    /**
     * Invokes the constructor of this class.
     *
     * @param args not used
     */
    public static void main(String[] args)
    {
        // set system look and feel (adapts the look of java to the systems default look)
        System.out.println("CrowdSimulation.main()");
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException
            | UnsupportedLookAndFeelException e)
        {
            logger.debug("Could not set Look and Feel.", e);
        }
        @SuppressWarnings("unused")
        CrowdSimulation ps = new CrowdSimulation();
    }

    /**
     * Gets the the artifact and version of this {@link CrowdSimulator} from a properties file that
     * is filled during build process.
     *
     * @return artifact and version
     */
    public String getVersion()
    {
        String version = "";
        Properties properties = new Properties();
        try
        {
            properties.load(
                this.getClass().getClassLoader().getResourceAsStream("crowdsimui.properties"));
            version += properties.getProperty("artifactId");
            version += "-" + properties.getProperty("version");
        }
        catch (IOException | NullPointerException e)
        {
            logger.info("Can't read UI version");
        }
        return version;
    }
}
