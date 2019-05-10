package de.fhg.ivi.crowdsimulation.ui.gui.visualisation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Panel;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JPanel;

import org.geotools.brewer.color.ColorBrewer;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.locationtech.jts.awt.ShapeWriter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.math.Vector2D;

import de.fhg.ivi.crowdsimulation.CrowdSimulator;
import de.fhg.ivi.crowdsimulation.analysis.Grid;
import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.crowd.Crowd;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.WayFindingModel;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.FollowRouteModel;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.Route;
import de.fhg.ivi.crowdsimulation.crowd.wayfindingmodel.route.WayPoint;
import de.fhg.ivi.crowdsimulation.math.MathTools;
import de.fhg.ivi.crowdsimulation.ui.extension.VisualCrowd;
import de.fhg.ivi.crowdsimulation.ui.extension.VisualCrowdSimulator;

/**
 * This class is a {@link JPanel}, which represents the actual "Map", i.e. where {@link Boundary},
 * {@link WayPoint}, {@link Crowd}, {@link Pedestrian} and {@link Grid} objects are painted and
 * which thereby visualizes the progress of the simulation.
 *
 * @author hahmann/meinert
 */
public class Map extends JPanel
    implements Runnable, ComponentListener, FocusListener, MouseWheelListener, KeyListener
{
    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger                       logger           = LoggerFactory
        .getLogger(Map.class);

    /**
     * default serial version ID
     */
    private static final long                         serialVersionUID = 1L;

    /**
     * The {@link VisualCrowdSimulator} object that is visualized in this {@link Map}.
     */
    private final CrowdSimulator<VisualCrowd>         crowdSimulator;

    /**
     * A {@link Map#currentScale} of 1 means that 1 pixel equals 1 meter in reality. A scale of 0.5
     * mean that 1 pixel equals 2 meters in reality. Default is 1.
     */
    private double                                    currentScale     = 0;

    /**
     * A rectangle, which defines the bounding box of the CrowdSimulator.
     */
    private Envelope                                  currentMapExtent;

    /**
     * A coordinate, which defines the map origin (i.e. upper left corner of the map).
     */
    private Coordinate                                currentMapOrigin;

    /**
     * Translator for GeoTools objects to java.awt objects
     */
    private ShapeWriter                               shapeWriter;

    /**
     * Variable to store if {@code wayPoints} are visible (true) or not (false).
     */
    private boolean                                   wayPointsVisible;

    /**
     * Variable to store if {@link WayPoint#getTargetLine()} are visible (true) or not (false).
     */
    private boolean                                   wayPointTargetLinesVisible;

    /**
     * Variable to store if {@link WayPoint#getPassingArea()} are visible (true) or not (false).
     */
    private boolean                                   wayPointPassingAreasVisible;

    /**
     * Variable to store if {@link WayPoint#getConnectionLineToPredecessor()} are visible (true) or
     * not (false).
     */
    private boolean                                   wayPointConnectionLinesVisible;

    /**
     * Variable to store if {@code wayPointLabels} are visible (true) or not (false).
     */
    private boolean                                   wayPointLabelsVisible;

    /**
     * Variable to store if {@code boundaries} are visible (true) or not (false).
     */
    private boolean                                   boundariesVisible;

    /**
     * Variable to store if {@code pedestrians} are visible (true) or not (false).
     */
    private boolean                                   pedestriansVisible;

    /**
     * Variable to store the offset, which is used for map navigation.
     */
    private double                                    offset           = 50;

    /**
     * The minimum interval between two consecutive refreshs in milliseconds
     */
    private int                                       refreshInterval  = 20;

    /**
     * Color Map containing 8 colors from {@link ColorBrewer} "RdYlGn" color scale and thresholds
     * assigning different colors to different classes.
     *
     * @see <a href=
     *      "http://colorbrewer2.org/#type=diverging&scheme=RdYlGn&n=8">http://colorbrewer2.org/#type=diverging&scheme=RdYlGn&n=8</a>
     */
    private static final LinkedHashMap<Double, Color> pedestrianColorMap;
    static
    {
        pedestrianColorMap = new LinkedHashMap<>();
        pedestrianColorMap.put(0.001d, ColorBrewer.instance().getPalette("RdYlGn").getColors(8)[7]);
        pedestrianColorMap.put(0.01d, ColorBrewer.instance().getPalette("RdYlGn").getColors(8)[6]);
        pedestrianColorMap.put(0.02d, ColorBrewer.instance().getPalette("RdYlGn").getColors(8)[5]);
        pedestrianColorMap.put(0.05d, ColorBrewer.instance().getPalette("RdYlGn").getColors(8)[4]);
        pedestrianColorMap.put(0.1d, ColorBrewer.instance().getPalette("RdYlGn").getColors(8)[3]);
        pedestrianColorMap.put(0.2d, ColorBrewer.instance().getPalette("RdYlGn").getColors(8)[2]);
        pedestrianColorMap.put(0.5d, ColorBrewer.instance().getPalette("RdYlGn").getColors(8)[1]);
        pedestrianColorMap.put(1d, ColorBrewer.instance().getPalette("RdYlGn").getColors(8)[0]);
    }

    /**
     * Color Map containing 8 colors from {@link ColorBrewer} "RdYlGn" color scale and thresholds
     * assigning different colors to different classes.
     *
     * @see <a href=
     *      "http://colorbrewer2.org/#type=diverging&scheme=RdYlGn&n=8">http://colorbrewer2.org/#type=diverging&scheme=RdYlGn&n=8</a>
     */
    private static final LinkedHashMap<Double, Color> gridCellColorMap;
    static
    {
        gridCellColorMap = new LinkedHashMap<>();
        gridCellColorMap.put(1d, ColorBrewer.instance().getPalette("RdYlGn").getColors(8)[0]);
        gridCellColorMap.put(0.5d, ColorBrewer.instance().getPalette("RdYlGn").getColors(8)[1]);
        gridCellColorMap.put(0.2d, ColorBrewer.instance().getPalette("RdYlGn").getColors(8)[2]);
        gridCellColorMap.put(0.1d, ColorBrewer.instance().getPalette("RdYlGn").getColors(8)[3]);
        gridCellColorMap.put(0.05d, ColorBrewer.instance().getPalette("RdYlGn").getColors(8)[4]);
        gridCellColorMap.put(0.02d, ColorBrewer.instance().getPalette("RdYlGn").getColors(8)[5]);
        gridCellColorMap.put(0.01d, ColorBrewer.instance().getPalette("RdYlGn").getColors(8)[6]);
        gridCellColorMap.put(0.005d, ColorBrewer.instance().getPalette("RdYlGn").getColors(8)[7]);
    }

    /**
     * Can be one of {@link Map#PEDESTRIAN_PAINT_MODE_DEFAULT},
     * {@link Map#PEDESTRIAN_PAINT_MODE_EXTRINSIC_FORCES_QUANTITATIVE},
     * {@link Map#PEDESTRIAN_PAINT_MODE_FORCES_AND_ORIENTATION}
     *
     **/
    private int             pedestriansFillMode                                 = PEDESTRIAN_PAINT_MODE_EXTRINSIC_FORCES_QUANTITATIVE;

    /**
     * Fills all pedestrians with green color
     */
    public static final int PEDESTRIAN_PAINT_MODE_DEFAULT                       = 0;

    /**
     * Fills the upper half of the Pedestrian with blue color, if the Pedestrian needs orientation
     * according to {@link WayFindingModel#needsOrientation()} and the lower half with red color if
     * the Pedestrian has extrinsic forces (boundary or other pedestrian) according to
     * {@link WayFindingModel#hasCourseDeviation()}. If the respective condition is not {@code true}
     * then the respective part is filled with green color.
     */
    public static final int PEDESTRIAN_PAINT_MODE_FORCES_AND_ORIENTATION        = 1;

    /**
     * Fills the pedestrian with a color the relates to the total amount of extrinsic forces. Green
     * means no extrinsic forces, red means high extrinsic forces.
     */
    public static final int PEDESTRIAN_PAINT_MODE_EXTRINSIC_FORCES_QUANTITATIVE = 2;

    /**
     * Variable to store if the {@code currentVelocity#Pedestrian} are visible (true) or not
     * (false).
     */
    private boolean         velocityVisible;

    /**
     * Indicates the visibility of the current velocity vectors of all {@link Pedestrian}
     */
    private boolean         currentVelocityVectorVisible;

    /**
     * Indicates the visibility of the current target vector (i.e. vector normalize to length of 1
     * to target point) of all {@link Pedestrian}
     */
    private boolean         currentTargetVectorVisible;

    /**
     * Indicates the visibility of the current target points of all {@link Pedestrian}
     */
    private boolean         currentTargetPointVisible;

    /**
     * Indicates the visibility of the current target {@link WayPoint} of all {@link Pedestrian}
     */
    private boolean         currentTargetWayPointVisible;

    /**
     * Indicates the visibility of the last successful orientation points of all {@link Pedestrian}
     */
    private boolean         lastSuccessfulOrientationPoint;

    /**
     * Indicates the visibility of the vector of current intrinsic forces (i.e. forces caused by the
     * aim to move towards the current target) of all {@link Pedestrian}
     */
    private boolean         currentIntrinsicForcesVisible;

    /**
     * Indicates whether force vector that results from the interaction of this {@link Pedestrian}
     * with all surrounding {@link Pedestrian} objects is visible
     */
    private boolean         currentPedestrianForceVisible;

    /**
     * Indicates whether force vector that results from the interaction of this {@link Pedestrian}
     * with all surrounding {@link Boundary} objects is visible
     */
    private boolean         currentBoundaryForceVisible;

    /**
     * Indicates the visibility of the vector of current total extrinsic forces (i.e. forces caused
     * by other {@link Pedestrian} and Boundaries) of all {@link Pedestrian}
     */
    private boolean         currentExtrinsicForcesVisible;

    /**
     * Indicates the visibility of the vector of current total forces (i.e. sum of extrinsic and
     * intrinsic forces) of all {@link Pedestrian}
     */
    private boolean         currentTotalForcesVisible;

    /**
     * Indicates if the {@link CrowdSimulator#getCrowds()} outlines are visible (true) or not
     * (false).
     */
    private boolean         crowdOutlinesVisible;

    /**
     * Indicates if the {@link CrowdSimulator#getGrid()} cells are visible (true) or not (false).
     */
    private boolean         gridVisible;

    /**
     * Indicates if the labels of {@link Grid} (i.e. local crowd density values in pedestrians/m²)
     * visible or not
     */
    private boolean         gridLabelsVisible;

    /**
     * Checks out if threads are running or not.
     */
    private boolean         graphicsThreadRunning;

    /**
     * {@code true}, if rendering is currently in progress, {@code false} otherwise
     */
    private boolean         isRenderingInProgress;

    /**
     * Storage variable for the last time than the graphic was refreshed. Given in milliseconds.
     */
    private long            lastGraphicsRefreshTime                             = 0;

    /**
     * Stores the number of updates per second of the graphic frame
     */
    private double          graphicRefreshsPerSecond                            = 0;

    /**
     * The {@link Graphics} object provides diverse methods for drawing and filling. This is
     * necessary for all things which should be visualized in the application window.
     */
    private Graphics2D      backbufferGraphics;

    /**
     * An object, with the expansion of {@code widthMap} and {@code heightMap}, for drawing elements
     * in the application window.
     */
    private BufferedImage   backbufferImage;

    /**
     * Constructor.
     * <p>
     * Adds {@link Component} elements to this class for map navigation purposes. Also initialize
     * objects which should be drawn at the start of the simulation.
     *
     * @param crowdSimulator the {@link CrowdSimulator} object that is visualized in this
     *            {@link Map}
     */
    public Map(CrowdSimulator<VisualCrowd> crowdSimulator)
    {
        super();
        this.crowdSimulator = crowdSimulator;
        shapeWriter = new ShapeWriter();
        addComponentListener(this);

        setBackground(Color.WHITE);
        addMouseWheelListener(this);
        addKeyListener(this);
        addFocusListener(this);

        // init objects which should be drawn at the start of the simulation
        setBoundariesVisible(true);
        setWayPointsVisible(true);
        setWayPointTargetLinesVisible(false);
        setWayPointLabelsVisible(true);
        setPedestriansVisible(true);
        setCrowdOutlinesVisible(false);
    }

    /**
     * Creates the {@code backbufferImage} as {@link BufferedImage} in which all objects of this
     * class can be drawn.
     */
    private void createBackBuffer()
    {
        try
        {
            backbufferImage = new BufferedImage(getWidth(), getHeight(),
                BufferedImage.TYPE_INT_RGB);
            backbufferGraphics = backbufferImage.createGraphics();
            backbufferGraphics.setBackground(Color.white);
            backbufferGraphics.clearRect(0, 0, getWidth(), getHeight());
        }
        catch (IllegalArgumentException e)
        {
            // do nothing if getHeight <= 0 or getWidth <= 0
            logger.debug("map width or height <= 0", e);
        }
    }

    /**
     * Sets {@link WayPoint} objects are visible or not.
     *
     * @param wayPointsVisible indicates if {@link WayPoint} are visible (true) or not (false)
     */
    public void setWayPointsVisible(boolean wayPointsVisible)
    {
        this.wayPointsVisible = wayPointsVisible;
    }

    /**
     * Tests if {@link WayPoint} objects are visible or not.
     *
     * @return {@code true} if {@link WayPoint} are visible, {@code false} otherwise
     */
    public boolean isWayPointsVisible()
    {
        return wayPointsVisible;
    }

    /**
     * Sets {@link WayPoint#getTargetLine()} visible or not.
     *
     * @param wayPointTargetLinesVisible indicates if {@link WayPoint#getTargetLine()} are visible
     *            (true) or not (false)
     */
    public void setWayPointTargetLinesVisible(boolean wayPointTargetLinesVisible)
    {
        this.wayPointTargetLinesVisible = wayPointTargetLinesVisible;
    }

    /**
     * Tests if {@link WayPoint#getTargetLine()} are visible or not.
     *
     * @return {@code true} if {@link WayPoint#getTargetLine()} are visible, {@code false} otherwise
     */
    public boolean isWayPointTargetLinesVisible()
    {
        return wayPointTargetLinesVisible;
    }

    /**
     * Sets {@link WayPoint#getPassingArea()} visible or not.
     *
     * @param wayPointPassingAreasVisible indicates if {@link WayPoint#getPassingArea()} are visible
     *            (true) or not (false)
     */
    public void setWayPointPassingAreasVisible(boolean wayPointPassingAreasVisible)
    {
        this.wayPointPassingAreasVisible = wayPointPassingAreasVisible;
    }

    /**
     * Tests if {@link WayPoint#getPassingArea()} are visible or not.
     *
     * @return {@code true} if {@link WayPoint#getPassingArea()} are visible, {@code false}
     *         otherwise
     */
    public boolean isWayPointPassingAreasVisible()
    {
        return wayPointPassingAreasVisible;
    }

    /**
     * Sets {@link WayPoint#getConnectionLineToPredecessor()} visible or not
     *
     * @param wayPointConnectionLineVisible indicates
     *            {@link WayPoint#getConnectionLineToPredecessor()} are visible or not
     */
    public void setWayPointConnectionLinesVisible(boolean wayPointConnectionLineVisible)
    {
        this.wayPointConnectionLinesVisible = wayPointConnectionLineVisible;
    }

    /**
     * Tests if {@link WayPoint#getConnectionLineToPredecessor()} are visible or not.
     *
     * @return {@code true} if {@link WayPoint#getConnectionLineToPredecessor()} are visible,
     *         {@code false} otherwise
     */
    public boolean isWayPointConnectionLinesVisible()
    {
        return wayPointConnectionLinesVisible;
    }

    /**
     * Sets {@link WayPoint} labels (i.e. WayPoint id) visible or not
     *
     * @param wayPointLabelsVisible indicates if labels (i.e. WayPoint id) are visible or not
     */
    public void setWayPointLabelsVisible(boolean wayPointLabelsVisible)
    {
        this.wayPointLabelsVisible = wayPointLabelsVisible;
    }

    /**
     * Tests if {@link WayPoint} labels are (i.e. WayPoint id) visible or not
     *
     * @return {@code true} if {@link WayPoint} labels are (i.e. WayPoint id) are visible,
     *         {@code false} otherwise
     */
    public boolean isWayPointLabelsVisible()
    {
        return wayPointLabelsVisible;
    }

    /**
     * Sets {@link Boundary} objects visible or not.
     *
     * @param boundariesVisible indicates if {@link Boundary} objects are visible ({@code true} or
     *            not {@code false}
     */
    public void setBoundariesVisible(boolean boundariesVisible)
    {
        this.boundariesVisible = boundariesVisible;
    }

    /**
     * Tests if {@link Boundary} objects are visible or not
     *
     * @return {@code true} if {@link Boundary} objects are visible, {@code false} otherwise
     */
    public boolean isBoundariesVisible()
    {
        return boundariesVisible;
    }

    /**
     * Sets all {@link Pedestrian}s visible or not.
     *
     * @param visible indicates if {@link Pedestrian} will be visible (true) or not (false) after
     *            calling this method
     */
    public void setPedestriansVisible(boolean visible)
    {
        this.pedestriansVisible = visible;
    }

    /**
     * Tests if all {@link Pedestrian}s are visible or not.
     *
     * @return indicates if all {@link Pedestrian} are visible {@code true} or not {@code false}
     */
    public boolean isPedestriansVisible()
    {
        return pedestriansVisible;
    }

    /**
     * Sets the fill mode of the {@link Pedestrian} objects. Can be one of
     * {@link Map#PEDESTRIAN_PAINT_MODE_DEFAULT},
     * {@link Map#PEDESTRIAN_PAINT_MODE_FORCES_AND_ORIENTATION},
     * {@link Map#PEDESTRIAN_PAINT_MODE_EXTRINSIC_FORCES_QUANTITATIVE}
     *
     * @param pedestriansFillMode can be one of {@link Map#PEDESTRIAN_PAINT_MODE_DEFAULT},
     *            {@link Map#PEDESTRIAN_PAINT_MODE_FORCES_AND_ORIENTATION},
     *            {@link Map#PEDESTRIAN_PAINT_MODE_EXTRINSIC_FORCES_QUANTITATIVE}
     */
    public void setPedestriansFillMode(int pedestriansFillMode)
    {
        this.pedestriansFillMode = pedestriansFillMode;
    }

    /**
     * Gets the fill mode of the Pedestrians.
     *
     * @return the fill mode. can be one of {@link Map#PEDESTRIAN_PAINT_MODE_DEFAULT},
     *         {@link Map#PEDESTRIAN_PAINT_MODE_FORCES_AND_ORIENTATION},
     *         {@link Map#PEDESTRIAN_PAINT_MODE_EXTRINSIC_FORCES_QUANTITATIVE}
     */
    public int getPedestriansFillMode()
    {
        return this.pedestriansFillMode;
    }

    /**
     * Sets the velocity labels of the {@link Pedestrian} visible or not
     *
     * @param velocityVisible indicates if {@link Pedestrian} velocities will be visible (true) or
     *            not (false) after calling this method
     */
    public void setVelocityVisible(boolean velocityVisible)
    {
        this.velocityVisible = velocityVisible;
    }

    /**
     * Tests if the velocity labels of all {@link Pedestrian} are visible or not
     *
     * @return indicates if all {@link Pedestrian} velocity labels are visible {@code true} or not
     *         {@code false}
     */
    public boolean isVelocityVisible()
    {
        return velocityVisible;
    }

    /**
     * Sets the current velocity vectors of all {@link Pedestrian} visible or not.
     *
     * @param visible indicates if the current velocity vectors of all {@link Pedestrian} are
     *            visible (true) or not (false).
     */
    public void setCurrentVelocityVectorVisible(boolean visible)
    {
        this.currentVelocityVectorVisible = visible;
    }

    /**
     * Tests if the current velocity vectors of all {@link Pedestrian} are visible or not.
     *
     * @return indicates if the current velocity vectors of all {@link Pedestrian} are visible
     *         {@code true} or not {@code false}
     */
    public boolean isCurrentVelocityVectorVisible()
    {
        return currentVelocityVectorVisible;
    }

    /**
     * Sets the current target vector (i.e. vector normalized to length of 1 to target point) of all
     * {@link Pedestrian} visible or not.
     *
     * @param visible indicates if the current target points of all {@link Pedestrian} are visible
     *            (true) or not (false).
     */
    public void setCurrentTargetVectorVisible(boolean visible)
    {
        this.currentTargetVectorVisible = visible;
    }

    /**
     * Tests, if the current target vector (i.e. vector normalized to length of 1 to target point)
     * of all {@link Pedestrian} visible or not.
     *
     * @return indicates if the current target vectors of all {@link Pedestrian} are visible (true)
     *         or not (false).
     */
    public boolean isCurrentTargetVectorVisible()
    {
        return currentTargetVectorVisible;
    }

    /**
     * Sets the current target points of all {@link Pedestrian} visible or not.
     *
     * @param visible indicates if the current target points of all {@link Pedestrian} are visible
     *            (true) or not (false).
     */
    public void setCurrentTargetPointVisible(boolean visible)
    {
        this.currentTargetPointVisible = visible;
    }

    /**
     * Tests if the current target points of all {@link Pedestrian} are visible or not
     *
     * @return indicates if the current target points of all {@link Pedestrian} are visible
     *         {@code true} or not {@code false}
     */
    public boolean isCurrentTargetPointVisible()
    {
        return currentTargetPointVisible;
    }

    /**
     * Sets the current target {@link WayPoint} of all {@link Pedestrian} visible or not.
     *
     * @param visible indicates if the current target points of all {@link Pedestrian} are visible
     *            (true) or not (false).
     */
    public void setCurrentTargetWayPointVisible(boolean visible)
    {
        this.currentTargetWayPointVisible = visible;
    }

    /**
     * Tests if the current target {@link WayPoint} of all {@link Pedestrian} are visible or not
     *
     * @return indicates if the current target points of all {@link Pedestrian} are visible
     *         {@code true} or not {@code false}
     */
    public boolean isCurrentTargetWayPointVisible()
    {
        return currentTargetWayPointVisible;
    }

    /**
     * Sets the last successful orientation points of all {@link Pedestrian} visible or not.
     *
     * @param visible indicates if the last successful orientation points of all {@link Pedestrian}
     *            are visible (true) or not (false).
     */
    public void setLastSuccessfulOrientationPointVisible(boolean visible)
    {
        this.lastSuccessfulOrientationPoint = visible;
    }

    /**
     * Tests if the last successful orientation points of all {@link Pedestrian} are visible or not
     *
     * @return indicates if the last successful orientation points of all {@link Pedestrian}
     *         {@code true} or not {@code false}
     */
    public boolean isLastSuccessfulOrientationPointVisible()
    {
        return lastSuccessfulOrientationPoint;
    }

    /**
     * Sets the vectors of current intrinsic forces (i.e. forces caused by the aim to move towards
     * the current target) of all {@link Pedestrian} visible or not.
     *
     * @param visible indicates if the current of all {@link Pedestrian} will be visible (true) or
     *            not (false).
     */
    public void setCurrentIntrinsicForcesVisible(boolean visible)
    {
        this.currentIntrinsicForcesVisible = visible;
    }

    /**
     * Tests if the vectors of the intrinsic forces (i.e. forces caused by other {@link Pedestrian}
     * and Boundaries) of all {@link Pedestrian} are visible or not.
     *
     * @return indicates if the current intrinsic forces of all {@link Pedestrian} are visible
     *         (true) or not (false).
     */
    public boolean isCurrentIntrinsicForcesVisible()
    {
        return currentIntrinsicForcesVisible;
    }

    /**
     * Sets the force vectors that results from the interaction a {@link Pedestrian} with all
     * surrounding {@link Pedestrian} objects visible
     *
     * @param currentPedestrianForceVisible {@code true} for visible, {@code false} for not visible
     */
    public void setCurrentPedestrianForceVisible(boolean currentPedestrianForceVisible)
    {
        this.currentPedestrianForceVisible = currentPedestrianForceVisible;
    }

    /**
     * Tests, if the force vectors that results from the interaction a {@link Pedestrian} with all
     * surrounding {@link Pedestrian} objects is visible
     *
     * @return {@code true} for visible, {@code false} for not visible
     */
    public boolean isCurrentPedestrianForceVisible()
    {
        return currentPedestrianForceVisible;
    }

    /**
     * Sets the force vector that results from the interaction of this {@link Pedestrian} with all
     * surrounding {@link Boundary} objects is visible
     *
     * @param currentBoundaryForceVisible {@code true} for visible, {@code false} for not visible
     */
    public void setCurrentBoundaryForceVisible(boolean currentBoundaryForceVisible)
    {
        this.currentBoundaryForceVisible = currentBoundaryForceVisible;
    }

    /**
     * Tests if the force vector that results from the interaction of this {@link Pedestrian} with
     * all surrounding {@link Boundary} objects is visible
     *
     * @return {@code true} for visible, {@code false} for not visible
     */
    public boolean isCurrentBoundaryForceVisible()
    {
        return currentBoundaryForceVisible;
    }

    /**
     * Sets the vectors of current total extrinsic forces (i.e. forces caused by other
     * {@link Pedestrian} and Boundaries) of all {@link Pedestrian} visible or not.
     *
     * @param visible indicates if the current extrinsic forces of all {@link Pedestrian} are
     *            visible (true) or not (false).
     */
    public void setCurrentExtrinsicForcesVisible(boolean visible)
    {
        this.currentExtrinsicForcesVisible = visible;
    }

    /**
     * Tests if the vectors of current total extrinsic forces (i.e. forces caused by other
     * {@link Pedestrian} and Boundaries) of all {@link Pedestrian} are visible or not.
     *
     * @return indicates if the current extrinsic forces vectors of all {@link Pedestrian} are
     *         visible (true) or not (false).
     */
    public boolean isCurrentExtrinsicForcesVisible()
    {
        return currentExtrinsicForcesVisible;
    }

    /**
     * Sets the vectors of current total forces (i.e. sum of extrinsic and intrinsic forces) of all
     * {@link Pedestrian} are visible or not.
     *
     * @param visible indicates if the current total forces (i.e. sum of extrinsic and intrinsic
     *            forces) of all {@link Pedestrian} are visible (true) or not (false).
     */
    public void setCurrentTotalForcesVisible(boolean visible)
    {
        this.currentTotalForcesVisible = visible;
    }

    /**
     * Tests if the vectors of current total forces (i.e. sum of extrinsic and intrinsic forces) of
     * all {@link Pedestrian} are visible or not.
     *
     * @return indicates if the current total forces (i.e. sum of extrinsic and intrinsic forces) of
     *         all {@link Pedestrian} are visible (true) or not (false).
     */
    public boolean isCurrentTotalForcesVisible()
    {
        return currentTotalForcesVisible;
    }

    /**
     * Sets {@link Crowd} visible or not.
     *
     * @param visible indicates if {@link Crowd} will be visible (true) or not (false) after calling
     *            this method
     */
    public void setCrowdOutlinesVisible(boolean visible)
    {
        crowdOutlinesVisible = visible;
        List<? extends Crowd> currentCrowds = crowdSimulator.getCrowds();
        if (currentCrowds != null && !currentCrowds.isEmpty())
        {
            crowdSimulator.setUpdatingCrowdOutlines(crowdOutlinesVisible);
        }
    }

    /**
     * Tests if {@link Crowd} is visible or not.
     *
     * @return indicates if {@link Crowd} is visible (true) or not (false)
     */
    public boolean isCrowdOutlinesVisible()
    {
        return crowdOutlinesVisible;
    }

    /**
     * Sets the {@link Grid} visible or not.
     *
     * @param visible indicates if {@link Grid} will be visible (true) or not (false) after calling
     *            this method
     */
    public void setGridVisible(boolean visible)
    {
        this.gridVisible = visible;
        Grid grid = crowdSimulator.getGrid(true);
        grid.setUpdating(this.gridVisible || this.gridLabelsVisible);
        if (gridVisible)
            grid.update(crowdSimulator.getCrowds());
        repaint();
    }

    /**
     * Tests if the {@link Grid} is visible or not.
     *
     * @return indicates if {@link Grid} is visible (true) or not (false)
     */
    public boolean isGridVisible()
    {
        return gridVisible;
    }

    /**
     * Sets the labels of {@link Grid} (i.e. local crowd density values in pedestrians/m²) visible
     * or not.
     *
     * @param visible the labels of {@link Grid} (i.e. local crowd density values in pedestrians/m²)
     *            will be visible (true) or not (false) after calling this method
     */
    public void setGridLabelsVisible(boolean visible)
    {
        this.gridLabelsVisible = visible;
        Grid grid = crowdSimulator.getGrid(true);
        grid.setUpdating(this.gridVisible || this.gridLabelsVisible);
        if (gridLabelsVisible)
            grid.update(crowdSimulator.getCrowds());
        repaint();
    }

    /**
     * Tests if the labels of {@link Grid} (i.e. local crowd density values in pedestrians/m²) are
     * visible or not.
     *
     * @return indicates if the labels of {@link Grid} (i.e. local crowd density values in
     *         pedestrians/m²) are visible (true) or not (false)
     */
    public boolean isGridLabelsVisible()
    {
        return gridLabelsVisible;
    }

    /**
     * Checks if the graphicThread is currently running.
     *
     * @return true or false in case the {@code graphicsThreadRunning} is running or not
     */
    public boolean isGraphicsThreadRunning()
    {
        return graphicsThreadRunning;
    }

    /**
     * Sets the {@code graphicsThreadRunning} as running (true) or not (false).
     *
     * @param graphicsThreadRunning could be true or false
     */
    public void setGraphicsThreadRunning(boolean graphicsThreadRunning)
    {
        this.graphicsThreadRunning = graphicsThreadRunning;
    }

    /**
     * Checks, if rendering is currently in progress.
     *
     * @return {@code true}, if rendering is currently in progress, {@code false} otherwise
     */
    public boolean isRenderingInProgress()
    {
        return isRenderingInProgress;
    }

    /**
     * Gets the current rate of graphic refresh per seconds.
     *
     * @return the current rate of graphic refresh per second
     */
    public double getGraphicRefreshsPerSecond()
    {
        return graphicRefreshsPerSecond;
    }

    /**
     * Paints the {@link #backbufferImage} in the {@link Map}.
     *
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        paintBackBuffer();
        g.drawImage(backbufferImage, 0, 0, this);
    }

    /**
     * Draws, scales and transforms the application window in dependence to the {@link Envelope}
     * object of the loaded geodata. Farther initiation of all objects, which should be drawn in the
     * application, is executed.
     *
     */
    private void paintBackBuffer()
    {
        Graphics2D g2 = backbufferGraphics;
        // skip, if the graphics context is unknown
        if (g2 == null)
            return;
        // skip, if rendering is currently in progress to avoid parallel/double rendering
        if (isRenderingInProgress)
            return;
        // try to set the currentMapOrigin from the bounding box of the crowd simulator, if unknown
        if (currentMapOrigin == null)
        {
            try
            {
                currentMapOrigin = new Coordinate(crowdSimulator.getBoundingBox().getMinX(),
                    crowdSimulator.getBoundingBox().getMinY());
            }
            catch (NullPointerException e)
            {
                logger.debug("bounding box=null");
            }
        }
        // skip, if the current map origin is unknown
        if (currentMapOrigin == null)
            return;
        // try to set currentScale from the bounding box of the crowd simulator, if unknown (so that
        // all geometries will be visible)
        if (currentScale == 0)
        {
            try
            {
                double scaleX = getSize().getWidth() / crowdSimulator.getBoundingBox().getWidth();
                double scaleY = getSize().getHeight() / crowdSimulator.getBoundingBox().getHeight();
                currentScale = scaleX < scaleY ? scaleX : scaleY;
            }
            catch (NullPointerException e)
            {
                logger.debug("map bounding box=null", e);
            }
        }
        // skip, if the current scale is unknown
        if (currentScale == 0)
            return;
        // try to set the currentMapExtent from the bounding box of the crowd simulator, if unknown
        if (currentMapExtent == null)
        {
            try
            {
                currentMapExtent = new Envelope(currentMapOrigin.x,
                    currentMapOrigin.x + (double) getWidth() * 1 / currentScale, currentMapOrigin.y,
                    currentMapOrigin.y + (double) getHeight() * 1 / currentScale);
            }
            catch (NullPointerException e)
            {
                logger.error("bounding box=null", e);
            }
        }
        // skip, if the current map extent is unknown
        if (currentMapExtent == null)
            return;

        // indicate that rendering is currently in progress
        isRenderingInProgress = true;

        // clear map
        g2.clearRect(0, 0, getWidth(), getHeight());

        double translateX = currentMapOrigin.x;
        double translateY = currentMapOrigin.y + currentMapExtent.getHeight();
        double scale = currentScale;

        // remember original transform
        // AffineTransform at = g2.getTransform();

        // set scale and offset
        g2.scale(scale, -scale);
        g2.translate( -translateX, -translateY);

        // paints a grid that visualizes local crowd densities
        paintGrid(g2);

        // draws boundaries and waypoints
        paintBackground(g2);

        // paint crowdOutline
        paintCrowd(g2);

        // draw pedestrians
        paintPedestrians(g2);

        // reset scale and translate
        // g2.setTransform(at);
        g2.translate(translateX, translateY);
        g2.scale(1 / scale, -1 / scale);

        isRenderingInProgress = false;

        logger.trace("currentMapOrigin=" + currentMapOrigin);
        logger.trace("currentMapExtent=" + currentMapExtent);
    }

    /**
     * Resets the {@link #currentMapExtent}, {@link #currentMapOrigin} and {@link #currentScale}.
     * These values will be set again during the next call of {@link #paintBackBuffer()}
     */
    public void resetMapExtent()
    {
        currentMapExtent = null;
        currentMapOrigin = null;
        currentScale = 0;
    }

    /**
     * Paints {@link CrowdSimulator#getBoundaries()}, and the {@link WayPoint}s of the {@link Crowd}
     * as {@link Shape} objects into this {@link Panel}
     *
     * @param g2 the {@link Graphics2D} graphics context
     */
    private void paintBackground(Graphics2D g2)
    {
        // get defaults
        Stroke defaultStroke = g2.getStroke();
        Color defaultColor = g2.getColor();

        g2.setStroke(new BasicStroke(0.1f));

        // draw boundaries in GUI
        if (boundariesVisible)
        {
            List<Boundary> boundaries = crowdSimulator.getBoundaries(currentMapExtent);
            if (boundaries != null && !boundaries.isEmpty())
                for (Boundary boundary : boundaries)
                {
                    paintBoundary(g2, boundary);
                }
        }

        // draw routes in GUI
        List<VisualCrowd> crowds = crowdSimulator.getCrowds();
        if (crowds != null && !crowds.isEmpty())
        {
            for (VisualCrowd visualCrowd : crowds)
            {
                Route route = visualCrowd.getRoute();
                if (route != null && route.getWayPoints() != null
                    && !route.getWayPoints().isEmpty())
                {
                    for (WayPoint wayPoint : route.getWayPoints())
                    {
                        paintWayPoint(g2, wayPoint, visualCrowd.getColor());
                    }
                }
            }
        }

        // reset defaults
        g2.setStroke(defaultStroke);
        g2.setColor(defaultColor);
    }

    /**
     * Paints a single {@link Boundary} object into the given {@link Graphics2D} context.
     *
     * @param g2 the graphics context
     * @param boundary the boundary object to be painted
     */
    private void paintBoundary(Graphics2D g2, Boundary boundary)
    {
        g2.setColor(Color.BLACK);
        g2.draw(shapeWriter.toShape(boundary.getGeometry()));

        // Envelope mapBoundingBox = new Envelope(currentMapExtent);
        // mapBoundingBox.expandBy( -100);
        // Polygon polygon = JTS.toGeometry(mapBoundingBox);
        // g2.draw(shapeWriter.toShape(polygon));
    }

    /**
     * Method for drawing a {@link WayPoint} object on the {@link Graphics2D} context g2
     *
     * @param g2 denotes the screen of the application in which drawing is possible
     * @param wayPoint the {@link WayPoint} object to be drawn
     * @param color the {@link Color} that should be used to paint this {@link WayPoint}
     */
    private void paintWayPoint(Graphics2D g2, WayPoint wayPoint, Color color)
    {
        Color defaultColor = g2.getColor();
        g2.setColor(color);

        // initialize shapes for drawing
        if (wayPointsVisible)
            g2.draw(
                shapeWriter.toShape(JTSFactoryFinder.getGeometryFactory().createPoint(wayPoint)));

        if (wayPointTargetLinesVisible)
        {
            g2.draw(shapeWriter.toShape(wayPoint.getTargetLine()));
        }

        if (wayPointPassingAreasVisible)
        {
            g2.draw(shapeWriter.toShape(wayPoint.getPassingArea()));
        }

        if (wayPointConnectionLinesVisible)
        {
            if (wayPoint.getConnectionLineToPredecessor() != null)
                g2.draw(shapeWriter.toShape(wayPoint.getConnectionLineToPredecessor()));
        }

        if (wayPointLabelsVisible)
            drawString(g2, "W" + wayPoint.getId(), (float) wayPoint.x + 5, (float) wayPoint.y);

        g2.setColor(defaultColor);
    }

    /**
     * Draws a {@link String} object into the given {@link Graphics2D} context in upright position.
     *
     * @param g2 the {@link Graphics2D} context
     * @param s the {@link String} object
     * @param x coordinate in real world coordinates
     * @param y coordinate in real world coordinates
     */
    private void drawString(Graphics2D g2, String s, float x, float y)
    {
        g2.scale(1, -1);
        g2.drawString(s, x, -y);
        g2.scale(1, -1);
    }

    /**
     * Draws the outline(s) of the {@link Crowd} object into the {@link Graphics2D} context g2.
     *
     * @param g2 parameter which contains the {@link Graphics2D} element
     */
    private void paintCrowd(Graphics2D g2)
    {
        if ( !crowdOutlinesVisible)
            return;

        List<VisualCrowd> currentCrowds = crowdSimulator.getCrowds();
        if (currentCrowds == null || currentCrowds.isEmpty())
            return;

        for (VisualCrowd crowd : currentCrowds)
        {
            List<Geometry> tempCrowdOutlines = crowd.getCrowdOutlines();
            if (tempCrowdOutlines != null)
            {
                // get defaults
                Color defaultColor = g2.getColor();
                Stroke defaultStroke = g2.getStroke();

                g2.setColor(crowd.getColor());
                g2.setStroke(new BasicStroke(0.5f));

                for (Geometry crowdOutline : tempCrowdOutlines)
                {
                    try
                    {
                        g2.draw(shapeWriter.toShape(crowdOutline));
                    }
                    catch (Exception e)
                    {
                        logger.trace("Crowd.paint(), crowdOutline=" + crowdOutline);
                    }
                }

                // reset defaults
                g2.setColor(defaultColor);
                g2.setStroke(defaultStroke);
            }
            else
                logger.trace("Crowd.paint(), " + "tempCrowdOutlines=null");
        }
    }

    /**
     * Draws {@code pedestrians} into the application window.
     *
     * @param g2 parameter which contains the {@link Graphics2D} element
     */
    private void paintPedestrians(Graphics2D g2)
    {
        List<VisualCrowd> currentCrowds = crowdSimulator.getCrowds();
        if (currentCrowds == null || currentCrowds.isEmpty())
            return;

        // get defaults
        RenderingHints defaultRenderingHints = g2.getRenderingHints();
        Stroke defaultStroke = g2.getStroke();
        Color defaultColor = g2.getColor();
        Font defaultFont = g2.getFont();

        // set pedestrian specific parameter
        // /* Enable anti-aliasing and pure stroke */
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        float strokeWidth = 0.1f;
        g2.setStroke(new BasicStroke(strokeWidth));
        g2.setColor(Color.BLUE);

        // paint pedestrians
        if (pedestriansVisible)
        {
            for (VisualCrowd crowd : currentCrowds)
            {
                for (Pedestrian pedestrian : crowd.getPedestrians())
                {
                    paintPedestrian(g2, pedestrian,
                        crowdSimulator.getForceModel().getPedestrianRadius(), crowd.getColor());
                }
            }

            // TODO: concurrent modification exception, when querying pedestrians from quadtree (to
            // only paint pedestrians which are visible on the current map extent)
            // Quadtree quadtree = crowdSimulator.getQuadtree();
            // List<Pedestrian> pedestrians = quadtree.getPedestrians(currentMapExtent);
            // if (pedestriansVisible && pedestrians != null && !pedestrians.isEmpty())
            // {
            // for (Pedestrian pedestrian : pedestrians)
            // {
            // paintPedestrian(g2, pedestrian,
            // crowdSimulator.getForceModel().getPedestrianRadius());
            // }
            // }
        }

        // reset defaults
        g2.setRenderingHints(defaultRenderingHints);
        g2.setStroke(defaultStroke);
        g2.setColor(defaultColor);
        g2.setFont(defaultFont);
    }

    /**
     * Gets a color that matches the class given in {@link #pedestrianColorMap} for the given
     * {@code value}.
     *
     * @param value the value to be used for finding a color in {@link #pedestrianColorMap}
     *
     * @return the appropriate color for the given {@code value}
     */
    private Color getPedestrianColor(double value)
    {
        // if value equals zero the return green
        if (value == 0d)
        {
            return pedestrianColorMap.entrySet().iterator().next().getValue();
        }
        // default value = green
        Color color = pedestrianColorMap.entrySet().iterator().next().getValue();

        // look for an appropriate class value depending on the value
        for (java.util.Map.Entry<Double, Color> entry : pedestrianColorMap.entrySet())
        {
            if (value > entry.getKey())
            {
                color = entry.getValue();
            }
        }
        return color;
    }

    /**
     * Paints the given {@link Pedestrian} {@code pedestrian} on the given {@link Graphics2D}
     * context {@code g2} depending on some visualization settings given by the further parameters.
     *
     * @param g2 denotes the screen of the application in which drawing is possible
     * @param pedestrian the {@link Pedestrian} object to be drawn.
     * @param radius the radius to be used for drawing
     */
    private void paintPedestrian(Graphics2D g2, Pedestrian pedestrian, double radius, Color color)
    {
        double x = pedestrian.getCurrentPositionVector().getX();
        double y = pedestrian.getCurrentPositionVector().getY();

        double fillRadius = radius;

        // fills pedestrians according to selected fill mode
        if (pedestriansFillMode == PEDESTRIAN_PAINT_MODE_DEFAULT
            || pedestriansFillMode == PEDESTRIAN_PAINT_MODE_FORCES_AND_ORIENTATION)
        {
            g2.setColor(color);
            Rectangle2D.Double square = new Rectangle2D.Double(x - fillRadius, y - fillRadius,
                2 * fillRadius, 2 * fillRadius);
            g2.draw(square);
        }
        if (pedestriansFillMode == PEDESTRIAN_PAINT_MODE_FORCES_AND_ORIENTATION)
        {
            // indicates need for orientation
            if (pedestrian.getActiveWayFindingModel().needsOrientation())
            {
                g2.setColor(Color.BLUE);
                Rectangle2D.Double square = new Rectangle2D.Double(x - fillRadius, y - fillRadius,
                    2 * fillRadius, 1 * fillRadius);
                g2.fill(square);
            }
            // indicates course deviation
            if (pedestrian.getActiveWayFindingModel().hasCourseDeviation())
            {
                g2.setColor(Color.RED);
                Rectangle2D.Double square = new Rectangle2D.Double(x - fillRadius, y,
                    2 * fillRadius, 1 * fillRadius);
                g2.fill(square);
            }
        }
        if (pedestriansFillMode == PEDESTRIAN_PAINT_MODE_EXTRINSIC_FORCES_QUANTITATIVE)
        {
            Vector2D totalExtrinsicForces = pedestrian.getTotalExtrinsicForces();
            if (totalExtrinsicForces != null)
            {
                g2.setColor(getPedestrianColor(MathTools.norm(totalExtrinsicForces)));
            }
            else
            {
                g2.setColor(color);
            }
            Rectangle2D.Double square = new Rectangle2D.Double(x - fillRadius, y - fillRadius,
                2 * fillRadius, 2 * fillRadius);
            // g2.fill(square);
            g2.draw(square);

            double smallRadius = fillRadius * 0.25d;
            Rectangle2D.Double squareCrowdColor = new Rectangle2D.Double(x - smallRadius,
                y - smallRadius, 2 * smallRadius, 2 * smallRadius);
            g2.setColor(color);
            g2.draw(squareCrowdColor);
        }

        // draws resulting velocity
        if (velocityVisible)
        {
            g2.setColor(Color.GREEN);
            float resultingVelocity = (float) pedestrian.getCurrentVelocity();
            int fontSize = 2;
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize));
            // id for debugging
            // drawString(g2, "id=" + pedestrian.getId(), (float) x - 1, (float) y - 1);
            drawString(g2,
                "v=" + Math.round(MathTools.convertMsToKmh(resultingVelocity)) + "km/h" + ", f="
                    + MathTools.round(MathTools.norm(pedestrian.getTotalExtrinsicForces()), 2),
                (float) x + 0.5f, (float) y + 0.5f);
        }

        // draws velocity vector (where the pedestrian actually goes)
        if (currentVelocityVectorVisible)
        {
            g2.setColor(Color.GREEN);
            Line2D.Double velocityVector = new Line2D.Double(
                pedestrian.getCurrentPositionVector().getX(),
                pedestrian.getCurrentPositionVector().getY(),
                pedestrian.getCurrentPositionVector().getX()
                    + pedestrian.getCurrentVelocityVector().getX() * 10,
                pedestrian.getCurrentPositionVector().getY()
                    + pedestrian.getCurrentVelocityVector().getY() * 10);
            g2.draw(velocityVector);
        }

        // draw line from pedestrian to current target point
        if (currentTargetPointVisible)
        {
            if (pedestrian.getActiveWayFindingModel() instanceof FollowRouteModel)
            {
                Vector2D targetPositionVector = pedestrian.getActiveWayFindingModel()
                    .getTargetPosition();
                if (targetPositionVector != null)
                {
                    g2.setColor(Color.LIGHT_GRAY);
                    Line2D.Double targetPointVector = new Line2D.Double(x, y,
                        targetPositionVector.getX(), targetPositionVector.getY());
                    g2.draw(targetPointVector);
                }
            }
        }

        // draw line from pedestrian to next waypoint
        if (currentTargetWayPointVisible)
        {
            if (pedestrian.getActiveWayFindingModel() instanceof FollowRouteModel)
            {
                Coordinate nextWayPoint = ((FollowRouteModel) pedestrian.getActiveWayFindingModel())
                    .getCurrentDestinationWayPoint();
                if (nextWayPoint != null)
                {
                    g2.setColor(Color.DARK_GRAY);
                    Line2D.Double nextWayPointVector = new Line2D.Double(x, y, nextWayPoint.x,
                        nextWayPoint.y);
                    g2.draw(nextWayPointVector);
                }
            }
        }

        // draw line from pedestrian to last successful orientation point
        if (lastSuccessfulOrientationPoint)
        {
            if (pedestrian.getActiveWayFindingModel() instanceof FollowRouteModel)
            {
                Vector2D lastPositionUpdate = ((FollowRouteModel) pedestrian
                    .getActiveWayFindingModel()).getLastOrientationUpdatePostion();
                if (lastPositionUpdate != null)
                {
                    g2.setColor(Color.MAGENTA);
                    Line2D.Double lastPositionVector = new Line2D.Double(x, y,
                        lastPositionUpdate.getX(), lastPositionUpdate.getY());
                    g2.draw(lastPositionVector);
                }
            }
        }

        // draws direction vector (where the pedestrian wants to go)
        if (currentTargetVectorVisible)
        {
            g2.setColor(Color.BLUE);
            Line2D.Double velocityVector = new Line2D.Double(
                pedestrian.getCurrentPositionVector().getX(),
                pedestrian.getCurrentPositionVector().getY(),
                pedestrian.getCurrentPositionVector().getX()
                    + pedestrian.getActiveWayFindingModel().getNormalizedDirectionVector().getX()
                        * 5,
                pedestrian.getCurrentPositionVector().getY()
                    + pedestrian.getActiveWayFindingModel().getNormalizedDirectionVector().getY()
                        * 5);
            g2.draw(velocityVector);
        }

        // draws intrinsic force vector (where the pedestrian is intrinsically accelerated to)
        if (currentIntrinsicForcesVisible)
        {
            Vector2D intrinsicForce = pedestrian.getIntrinsicForce();
            if (intrinsicForce != null)
            {
                g2.setColor(Color.PINK);
                Line2D.Double forceVector = new Line2D.Double(
                    pedestrian.getCurrentPositionVector().getX(),
                    pedestrian.getCurrentPositionVector().getY(),
                    pedestrian.getCurrentPositionVector().getX() + intrinsicForce.getX() * 5,
                    pedestrian.getCurrentPositionVector().getY() + intrinsicForce.getY() * 5);
                g2.draw(forceVector);
            }
        }

        // draws total force vector (where the pedestrian is intrinsically accelerated to)
        if (currentTotalForcesVisible)
        {
            Vector2D totalForce = pedestrian.getTotalForce();
            if (totalForce != null)
            {
                g2.setColor(Color.MAGENTA);
                Line2D.Double forceVector = new Line2D.Double(
                    pedestrian.getCurrentPositionVector().getX(),
                    pedestrian.getCurrentPositionVector().getY(),
                    pedestrian.getCurrentPositionVector().getX() + totalForce.getX() * 5,
                    pedestrian.getCurrentPositionVector().getY() + totalForce.getY() * 5);
                g2.draw(forceVector);
            }
        }

        // draws extrinsic forces vector (where the pedestrian is extrinsically accelerated to)
        if (currentExtrinsicForcesVisible)
        {
            Vector2D totalExtrinsicForces = pedestrian.getTotalExtrinsicForces();
            if (totalExtrinsicForces != null)
            {
                g2.setColor(Color.RED);
                Line2D.Double forceVector = new Line2D.Double(
                    pedestrian.getCurrentPositionVector().getX(),
                    pedestrian.getCurrentPositionVector().getY(),
                    pedestrian.getCurrentPositionVector().getX() + totalExtrinsicForces.getX() * 5,
                    pedestrian.getCurrentPositionVector().getY() + totalExtrinsicForces.getY() * 5);
                g2.draw(forceVector);
            }
        }

        // draws pedestrian forces vector
        if (currentPedestrianForceVisible)
        {
            Vector2D forceInteractionWithPedestrians = pedestrian
                .getForceInteractionWithPedestrians();
            if (forceInteractionWithPedestrians != null)
            {
                g2.setColor(new Color(0, 120, 0));
                Line2D.Double forceVector = new Line2D.Double(
                    pedestrian.getCurrentPositionVector().getX(),
                    pedestrian.getCurrentPositionVector().getY(),
                    pedestrian.getCurrentPositionVector().getX()
                        + forceInteractionWithPedestrians.getX() * 5,
                    pedestrian.getCurrentPositionVector().getY()
                        + forceInteractionWithPedestrians.getY() * 5);
                g2.draw(forceVector);
            }
        }

        // draws boundary forces vector
        if (currentBoundaryForceVisible)
        {
            Vector2D forceInteractionWithBoundaries = pedestrian
                .getForceInteractionWithBoundaries();
            if (forceInteractionWithBoundaries != null)
            {
                g2.setColor(new Color(120, 0, 0));
                Line2D.Double forceVector = new Line2D.Double(
                    pedestrian.getCurrentPositionVector().getX(),
                    pedestrian.getCurrentPositionVector().getY(),
                    pedestrian.getCurrentPositionVector().getX()
                        + forceInteractionWithBoundaries.getX() * 5,
                    pedestrian.getCurrentPositionVector().getY()
                        + forceInteractionWithBoundaries.getY() * 5);
                g2.draw(forceVector);
            }
        }
    }

    /**
     * Gets a color that matches the class given in {@link #gridCellColorMap} for the given
     * {@code value}.
     *
     * @param value the value to be used for finding a color in {@link #gridCellColorMap}
     *
     * @return the appropriate color for the given {@code value}
     */
    private Color getGridCellColor(double value)
    {
        // if value equals zero the return white
        if (value == 0d)
        {
            return Color.white;
        }
        // default value = highest possible value
        Color color = gridCellColorMap.entrySet().iterator().next().getValue();

        // look for an appropriate class value depending on the value
        for (java.util.Map.Entry<Double, Color> entry : gridCellColorMap.entrySet())
        {
            if (value < entry.getKey())
            {
                color = entry.getValue();
            }
        }
        return color;
    }

    /**
     * Paints the given {@link Grid} object into the given {@link Graphics2D} context.
     *
     * @param g2 the {@link Graphics2D} context to paint in
     */
    private void paintGrid(Graphics2D g2)
    {
        if ( !gridVisible && !gridLabelsVisible)
            return;
        Grid grid = crowdSimulator.getGrid();
        if (grid == null)
            return;
        java.util.Map<Envelope, List<Pedestrian>> gridCells = grid.getGridCells();
        if (gridCells == null || gridCells.isEmpty())
            return;

        // get defaults
        Stroke defaultStroke = g2.getStroke();
        Color defaultColor = g2.getColor();
        Font defaultFont = g2.getFont();

        for (java.util.Map.Entry<Envelope, List<Pedestrian>> gridCell : gridCells.entrySet())
        {
            // skip empty cells
            if (gridCell.getValue().size() == 0)
                continue;

            // create rectangle for painting
            Rectangle2D.Double cell = new Rectangle2D.Double(gridCell.getKey().getMinX(),
                gridCell.getKey().getMinY(), grid.getCellSize(), grid.getCellSize());

            // crowd density in grid cell (pedestrian / m²)
            double gridCellCrowdDensity = gridCell.getValue().size()
                / (grid.getCellSize() * grid.getCellSize());

            // paint cells
            if (gridVisible)
            {
                // set color depending on value and paint
                g2.setColor(getGridCellColor(gridCellCrowdDensity));
                g2.fill(cell);
            }

            // draw cell value as String
            if (gridLabelsVisible)
            {
                g2.setColor(Color.BLACK);
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 3));
                drawString(g2, String.valueOf(MathTools.round(gridCellCrowdDensity, 2)),
                    (float) gridCell.getKey().getMinX(), (float) gridCell.getKey().getMinY()
                        + (float) gridCell.getKey().getHeight() / 2f);
            }
        }

        // reset defaults
        g2.setStroke(defaultStroke);
        g2.setColor(defaultColor);
        g2.setFont(defaultFont);
    }

    /**
     * Implements run() of the interface {@link Runnable}. The Thread processed permanently, that
     * means that it's possible to send instructions to the thread (if the thread is running).
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        // infinite loop runs until application is closed or paused
        while (graphicsThreadRunning)
        {
            // don't refresh more than 50 times per second
            long timeSinceLastRefresh = System.currentTimeMillis() - lastGraphicsRefreshTime;
            if (timeSinceLastRefresh < refreshInterval)
            {
                try
                {
                    long sleepInterval = refreshInterval - timeSinceLastRefresh;
                    Thread.sleep(sleepInterval);
                }
                catch (InterruptedException e)
                {
                    logger.debug("sleep interrupted", e);
                }
            }

            long currentTime = System.currentTimeMillis();

            // update paintBackBuffer
            repaint();

            // update frame per second rate
            graphicRefreshsPerSecond = 1
                / ((currentTime - (double) lastGraphicsRefreshTime) / 1000d);

            // set last graphics refresh time
            lastGraphicsRefreshTime = currentTime;
        }
    }

    /**
     * Clears all elements which are inside the {@link Map}
     */
    public void clear()
    {
        backbufferGraphics.clearRect(0, 0, getWidth(), getHeight());
    }

    /**
     * No implementation (i.e. nothing happens).
     *
     * @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentResized(ComponentEvent e)
    {
        createBackBuffer();
        repaint();
    }

    /**
     * No implementation (i.e. nothing happens).
     *
     * @see java.awt.event.ComponentListener#componentMoved(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentMoved(ComponentEvent e)
    {

    }

    /**
     * No implementation (i.e. nothing happens).
     *
     * @see java.awt.event.ComponentListener#componentShown(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentShown(ComponentEvent e)
    {

    }

    /**
     * No implementation (i.e. nothing happens).
     *
     * @see java.awt.event.ComponentListener#componentHidden(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentHidden(ComponentEvent e)
    {

    }

    /**
     * No implementation (i.e. nothing happens).
     *
     * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
     */
    @Override
    public void focusGained(FocusEvent e)
    {

    }

    /**
     * No implementation (i.e. nothing happens).
     *
     * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
     */
    @Override
    public void focusLost(FocusEvent e)
    {
        this.requestFocus();
    }

    /**
     * No implementation (i.e. nothing happens).
     *
     * @see javax.swing.JComponent#processMouseEvent(java.awt.event.MouseEvent)
     */
    @Override
    public void processMouseEvent(MouseEvent e)
    {

    }

    /**
     * Implements mouseWheelMoved() of the interface {@link MouseWheelListener}. Changes the scale.
     *
     * @see MouseWheelListener#mouseWheelMoved(MouseWheelEvent)
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent event)
    {
        // screen coordinates
        zoom(event.getWheelRotation(), event.getX(), event.getY());
    }

    /**
     * No implementation (i.e. nothing happens).
     *
     * @param e object is the {@link KeyEvent} which occurs if a button is pressed
     *
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    @Override
    public void keyTyped(KeyEvent e)
    {

    }

    /**
     * No implementation (i.e. nothing happens).
     *
     * @param e object is the {@link KeyEvent} which occurs if a button is pressed
     *
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    @Override
    public void keyPressed(KeyEvent e)
    {

    }

    /**
     * Shifts the Map if one of the arrow keys is pressed and also released.
     *
     * @param e object is the {@link KeyEvent} which occurs if a button is released
     *
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    @Override
    public void keyReleased(KeyEvent e)
    {
        double moveDistance = offset / currentScale;
        Coordinate oldMapOrigin = currentMapOrigin;
        if (oldMapOrigin == null)
            return;
        if (e.getKeyCode() == KeyEvent.VK_UP)
        {
            Coordinate updatedMapOrigin = new Coordinate(oldMapOrigin.x,
                oldMapOrigin.y + moveDistance);
            move(updatedMapOrigin);
        }
        if (e.getKeyCode() == KeyEvent.VK_DOWN)
        {
            Coordinate updatedMapOrigin = new Coordinate(oldMapOrigin.x,
                oldMapOrigin.y - moveDistance);
            move(updatedMapOrigin);
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT)
        {
            Coordinate updatedMapOrigin = new Coordinate(oldMapOrigin.x + moveDistance,
                oldMapOrigin.y);
            move(updatedMapOrigin);
        }
        if (e.getKeyCode() == KeyEvent.VK_LEFT)
        {
            Coordinate updatedMapOrigin = new Coordinate(oldMapOrigin.x - moveDistance,
                oldMapOrigin.y);
            move(updatedMapOrigin);
        }
        if (e.getKeyCode() == KeyEvent.VK_PLUS || e.getKeyCode() == KeyEvent.VK_ADD)
        {
            zoom( -1, getWidth() / 2, getHeight() / 2);
        }
        if (e.getKeyCode() == KeyEvent.VK_MINUS || e.getKeyCode() == KeyEvent.VK_SUBTRACT)
        {
            zoom(1, getWidth() / 2, getHeight() / 2);
        }
        logger.trace("keyReleased(), " + e);
    }

    /**
     * Increases or decreases the current zoom level by {@code zoomDelta}. The {@link Map} will be
     * centered at {@code zoomX} / {@code zoomY} after the zoom.
     *
     * @param zoomDelta delta of zoom level (< 0 to zoom in, > 0 to zoom out)
     * @param zoomX the center of the Map after the zoom in screen coordinates
     * @param zoomY the center of the Map after the zoom in screen coordinates
     */
    private void zoom(int zoomDelta, int zoomX, int zoomY)
    {
        Envelope oldBoundingBox = currentMapExtent;
        Coordinate oldMapOrigin = currentMapOrigin;

        // reflect map origin
        zoomY = getHeight() - zoomY;

        logger.trace("zoom(), zoomDelta=" + zoomDelta + ", zoomX=" + zoomX + ", zoomY=" + zoomY);

        // zoomFactor
        double zoomFactor = 2;

        double updatedScale = 0;
        Envelope updatedBoundingBox = null;
        Coordinate updatedMapOrigin = null;

        // new map scale
        updatedScale = currentScale * Math.pow(zoomFactor, -1 * zoomDelta);

        // delta map width in meters
        double mapWidth = currentMapExtent.getMaxX() - currentMapExtent.getMinX();
        double mapHeight = currentMapExtent.getMaxY() - currentMapExtent.getMinY();

        // delta map origin in meters
        double deltaX = zoomX / updatedScale;
        double deltaY = zoomY / updatedScale;

        if (zoomDelta < 0)
        {
            logger.trace("zoom in");
            updatedMapOrigin = new Coordinate(oldMapOrigin.x + deltaX / 2 * zoomFactor,
                oldMapOrigin.y + deltaY / 2 * zoomFactor);

            updatedBoundingBox = new Envelope(updatedMapOrigin.x,
                updatedMapOrigin.x + mapWidth / zoomFactor, updatedMapOrigin.y,
                updatedMapOrigin.y + mapHeight / zoomFactor);
        }
        else
        {
            logger.trace("zoom out");
            updatedMapOrigin = new Coordinate(oldMapOrigin.x - deltaX / zoomFactor,
                oldMapOrigin.y - deltaY / zoomFactor);

            updatedBoundingBox = new Envelope(updatedMapOrigin.x,
                updatedMapOrigin.x + mapWidth * zoomFactor, updatedMapOrigin.y,
                updatedMapOrigin.y + mapHeight * zoomFactor);
        }

        logger.trace("oldBoundingBox=" + oldBoundingBox + ", area=" + oldBoundingBox.getArea());
        logger.trace(
            "updatedBoundingBox=" + updatedBoundingBox + ", area=" + updatedBoundingBox.getArea());

        currentScale = updatedScale;
        currentMapExtent = updatedBoundingBox;
        currentMapOrigin = updatedMapOrigin;

        repaint();
    }

    /**
     * Moves this {@link Map} to the new {@code mapOrigin} and updates {@link #currentMapExtent}.
     *
     * @param mapOrigin the new map origin (i.e. upper left corner of the map in real world
     *            coordinates)
     */
    private void move(Coordinate mapOrigin)
    {
        if (mapOrigin == null)
            return;
        double deltaX = mapOrigin.x - currentMapOrigin.x;
        double deltaY = mapOrigin.y - currentMapOrigin.y;

        currentMapOrigin = mapOrigin;
        currentMapExtent = new Envelope(currentMapExtent.getMinX() + deltaX,
            currentMapExtent.getMaxX() + deltaX, currentMapExtent.getMinY() + deltaY,
            currentMapExtent.getMaxY() + deltaY);
        repaint();
    }
}
