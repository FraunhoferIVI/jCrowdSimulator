package de.fhg.ivi.crowdsimulation.analysis;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.math3.util.FastMath;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.Grids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import de.fhg.ivi.crowdsimulation.crowd.Crowd;
import de.fhg.ivi.crowdsimulation.crowd.ICrowd;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;
import de.fhg.ivi.crowdsimulation.geom.GeometryTools;

/**
 * A {@link Grid} is a raster of cells with specifiable {@link #cellSize}. It can be used to store
 * and access the {@link Pedestrian} objects within each cell at a given point of time
 *
 * @author hahmann/meinert
 */
public class Grid
{

    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger             logger          = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());

    /**
     * A map of {@link Envelope} objects (i.e. representing the cells of this Grid) that store a
     * {@link List} of {@link Pedestrian} objects that are currently contained in each grid cell
     */
    private Map<Envelope, List<Pedestrian>> gridCells;

    /**
     * Unix time stamp of the last full update of the aggregate value associated to each grid cell.
     */
    private long                            lastUpdateTime;

    /**
     * The cell size of the grid. Given in meters.
     */
    private double                          cellSize;

    /**
     * The default cell size of the grid. Given in meters.
     */
    private double                          defaultCellSize = 10d;

    /**
     * The maximum amount of cells this grid should have.
     */
    private double                          maxCells        = 1000000;

    /**
     * Indicates, if the {@link #gridCells} should be updated each time {@link #update(List, long)}
     * is called. Usually, it is unnecessary to do so, if the {@link Grid} is not visible in the
     * Graphical User Interface, since it is currently unused elsewhere.
     */
    private boolean                         isUpdating      = false;

    /**
     * Creates a new grid with a cell size of {@link #defaultCellSize} that fits into
     * {@code gridBounds}.
     *
     * @param gridBounds the bounding box to fit the {@link Grid} in.
     */
    public Grid(ReferencedEnvelope gridBounds)
    {
        gridCells = new HashMap<>();
        lastUpdateTime = 0;
        double dynamicCellSize = FastMath.sqrt(gridBounds.getArea() / maxCells);
        double staticCellSize = defaultCellSize;
        this.cellSize = gridBounds.getArea() / (defaultCellSize * defaultCellSize) < maxCells
            ? staticCellSize : dynamicCellSize;
        SimpleFeatureSource grid = Grids.createSquareGrid(gridBounds, cellSize);
        @SuppressWarnings("resource")
        SimpleFeatureIterator features = null;
        try
        {
            features = grid.getFeatures().features();
            while (features.hasNext())
            {
                Geometry gridCell = (Geometry) features.next().getDefaultGeometry();
                gridCells.put(gridCell.getEnvelopeInternal(), new ArrayList<>());
            }
        }
        catch (NoSuchElementException e)
        {
            logger.error("Grid.createGrid(), ", e);

        }
        catch (IOException e)
        {
            logger.error("Grid.createGrid(), ", e);
        }
        finally
        {
            if (features != null)
                features.close();
        }
    }

    /**
     * Gets the map of {@link Envelope} objects (i.e. representing the cells of this Grid) that
     * store a {@link List} of {@link Pedestrian} objects that are currently contained in each grid
     * cell
     *
     *
     * @return the map of grid cells and associated lists of Pedestrian objects for this
     *         {@link Grid} object
     */
    public Map<Envelope, List<Pedestrian>> getGridCells()
    {
        return gridCells;
    }

    /**
     * The cell size of the grid. Given in meters
     *
     * @return the cell size of the grid.
     */
    public double getCellSize()
    {
        return cellSize;
    }

    /**
     * Indicate, if this {@link Grid} is currently updating all its cell values
     *
     * @param isUpdating {@code true} if this {@link Grid} is currently updating all its cell
     *            values, {@code false} otherwise
     */
    public void setUpdating(boolean isUpdating)
    {
        this.isUpdating = isUpdating;
    }

    /**
     * Updates this {@link Grid} object, i.e. all cell values are updated by the aggregated number
     * of {@link Pedestrian} objects contained in each cell.
     *
     * @param crowds a {@link List} of {@link Crowd} whose {@link Pedestrian} objects that should be
     *            counted per grid cell
     * @param currentTime the current time stamp <b>(the Grid is only updated if last update was
     *            more than 1000ms ago)</b>
     */
    public void update(List<? extends ICrowd> crowds, long currentTime)
    {
        if ( !isUpdating)
            return;

        boolean doRefresh = currentTime - lastUpdateTime > 1000;

        if (doRefresh)
        {
            update(crowds);
            lastUpdateTime = currentTime;
        }
    }

    /**
     * Updates this {@link Grid} object, i.e. all cell values are updated by the aggregated number
     * of {@link Pedestrian} objects contained in each cell.
     *
     * @param crowds a {@link List} of {@link Crowd} whose {@link Pedestrian} objects that should be
     *            counted per grid cell
     */
    public void update(List<? extends ICrowd> crowds)
    {
        // reset grid cell values
        for (Map.Entry<Envelope, List<Pedestrian>> gridCell : gridCells.entrySet())
        {
            gridCell.getValue().clear();
        }
        // set new grid cell values
        for (Map.Entry<Envelope, List<Pedestrian>> gridCell : gridCells.entrySet())
        {
            for (ICrowd crowd : crowds)
            {
                for (Pedestrian pedestrian : crowd.getPedestrians())
                {
                    if (GeometryTools.isInside(pedestrian.getCurrentPositionVector(),
                        gridCell.getKey()))
                    {
                        gridCell.getValue().add(pedestrian);
                    }
                }
            }

        }
    }

    /**
     * Sets {@link #gridCells} to {@code null}.
     */
    public void reset()
    {
        gridCells = null;
    }
}
