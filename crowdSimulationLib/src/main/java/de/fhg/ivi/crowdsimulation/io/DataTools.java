package de.fhg.ivi.crowdsimulation.io;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.csvreader.CsvReader;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

/**
 * Class for providing helping methods in case of data-processing use cases. There are several
 * functions implemented in this class:
 *
 * <li>Analysis whether a {@link File} has a "csv" or "shp" format</li>
 * <li>Extraction of a {@link Geometry} out of a "csv" {@link File}</li>
 * <li>Extraction of a {@link Geometry} out of a "shp" {@link File}</li>
 * <li>Extraction of a {@link CoordinateReferenceSystem} out of a "shp" {@link File}</li>
 * <li>Extraction of a {@link CoordinateReferenceSystem} out of a {@link Geometry}</li>
 * <li>Transformation respectively mirroring of a {@link List} of {@link Geometry}s</li>
 *
 * <p>
 *
 * @author hahmann/meinert
 */
public class DataTools
{
    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger logger = LoggerFactory.getLogger(DataTools.class);

    /**
     * Parses a {@link List} of {@link Geometry}s from {@code file}. {@link File} {@code file} is
     * assumed to be in CSV or Shape Format for later processing.
     *
     * @param file the CSV/Shape {@link File} object to be parsed
     *
     * @return {@link List} of {@link Geometry} objects
     */
    public static List<Geometry> loadGeometriesFromFile(File file)
    {
        if (file == null)
            return null;

        List<Geometry> geometries = null;

        try
        {
            if (FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("csv"))
            {
                geometries = loadGeometriesFromCsvFile(file);
            }
            else if (FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("shp"))
            {
                geometries = loadGeometriesFromShapeFile(file);
            }
            else
            {
                logger.error("Unsupported Filetype: " + FilenameUtils.getExtension(file.getName()));
            }
        }
        catch (NullPointerException e)
        {
            logger.error("Given file is null.", e);
        }

        return geometries;
    }

    /**
     * Parses a {@link List} of {@link Geometry}s from {@code file}. {@link File} {@code file} is
     * assumed to be in CSV format (separator = ';') and to contain a column with header 'WKT'.
     *
     * @param file the CSV {@link File} object to be parsed
     *
     * @return {@link List} of {@link Geometry} objects
     */
    public static List<Geometry> loadGeometriesFromCsvFile(File file)
    {
        ArrayList<Geometry> loadedGeometries = new ArrayList<>();
        WKTReader reader = new WKTReader();
        CsvReader csvReader = null;

        try (FileReader fileReader = new FileReader(file))
        {
            csvReader = new CsvReader(fileReader, ';');
            csvReader.readHeaders();
            logger.trace("loadGeometriesFromCsvFile(), " + file);

            while (csvReader.readRecord())
            {
                try
                {
                    loadedGeometries.add(reader.read(csvReader.get("WKT")));
                }
                catch (ParseException ex)
                {
                    logger.error("loadGeometriesFromCsvFile(), ", ex);
                }
            }
            csvReader.close();
        }
        catch (IOException e)
        {
            logger.debug("loadGeometriesFromCsvFile(), " + "no file found", e);
        }
        finally
        {
            if (csvReader != null)
            {
                csvReader.close();
            }
        }

        return loadedGeometries;
    }

    /**
     * Parses a {@link List} of {@link Geometry}s from {@code file}. {@link File} {@code file} is
     * assumed to be in ESRI's Shape format.
     *
     * @param file the Shape {@link File} object to be parsed
     *
     * @return {@link List} of {@link Geometry} objects
     */
    public static List<Geometry> loadGeometriesFromShapeFile(File file)
    {
        ArrayList<Geometry> loadedGeometries = null;
        try
        {
            loadedGeometries = new ArrayList<>();
            Map<String, Object> map = new HashMap<>();
            map.put("url", file.toURI().toURL());
            DataStore dataStore = DataStoreFinder.getDataStore(map);
            String typeName = dataStore.getTypeNames()[0];

            FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore
                .getFeatureSource(typeName);
            // int srid = CRS.lookupEpsgCode(source.getSchema().getCoordinateReferenceSystem(),
            // false);
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();

            try (FeatureIterator<SimpleFeature> features = collection.features())
            {
                while (features.hasNext())
                {
                    SimpleFeature feature = features.next();
                    Object object = feature.getAttribute("the_geom");
                    Geometry geometry = (Geometry) object;
                    loadedGeometries.add(geometry);
                }
            }
        }
        catch (Exception e)
        {
            logger.debug("loadGeometriesFromShapeFile(), Exception=", e);
        }

        return loadedGeometries;
    }

    /**
     * Extracts the {@link CoordinateReferenceSystem} from a {@link File}.
     *
     * @param file the Shape or CSV {@link File} object to be parsed
     *
     * @return the {@link CoordinateReferenceSystem} of the {@link File}
     */
    public static CoordinateReferenceSystem getCRSFromFile(File file)
    {
        if (FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("shp"))
        {
            try
            {
                Map<String, Object> map = new HashMap<>();
                map.put("url", file.toURI().toURL());
                DataStore dataStore = DataStoreFinder.getDataStore(map);
                String typeName = dataStore.getTypeNames()[0];

                FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore
                    .getFeatureSource(typeName);
                return source.getSchema().getCoordinateReferenceSystem();
            }
            catch (Exception e)
            {
                logger.debug("loadGeometriesFromShapeFile(), Exception=", e);
            }
        }

        return null;
    }

    /**
     * Decodes the {@link CoordinateReferenceSystem} from the a SRID obtained from {@code geometry}
     *
     * @param geometry the geometry object to get the {@link CoordinateReferenceSystem} from
     *
     * @return the {@link CoordinateReferenceSystem} from the a SRID obtained from {@code geometry}
     *         or {@code null} if not matching {@link CoordinateReferenceSystem} has been found
     */
    public static CoordinateReferenceSystem getCRSFromGeometry(Geometry geometry)
    {
        int srid = geometry.getSRID();
        CoordinateReferenceSystem crs = null;
        try
        {
            crs = CRS.decode("EPSG:" + srid);
        }
        catch (FactoryException e)
        {
            logger.error("DataTools.getCRSFromGeometry(), unknown srid=" + srid, e);
        }

        return crs;
    }
}
