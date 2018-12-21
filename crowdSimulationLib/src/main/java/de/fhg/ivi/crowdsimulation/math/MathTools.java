package de.fhg.ivi.crowdsimulation.math;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.math.Vector2D;

/**
 * Class for providing helping methods in case of mathematical functions. There are several
 * functions implemented in this class:
 *
 * <li>Calculation of a gaussian distribution</li>
 * <li>Rounding of a {@link Double} value after a specific number of digits</li>
 * <li>Convert from m/s to km/h</li>
 * <li>Convert from km/h to m/s</li>
 * <li>Calculation of a normalized version of a {@link Vector2D}</li>
 *
 * <p>
 *
 * @author hahmann/meinert
 */
/**
 * @author meinert
 *
 */
public class MathTools
{

    /**
     * Uses the object logger for printing specific messages in the console.
     */
    private static final Logger logger       = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Defines the {@code byte} or mode of the {@link #norm(Vector2D, byte)}. In this case the
     * calculation will be executed with the highest accuracy of the {@link Math} library.
     */
    public final static byte    STRICT_MATH  = 0;

    /**
     * Defines the {@code byte} or mode of the {@link #norm(Vector2D, byte)}. In this case the
     * calculation will be executed with the medium accuracy of the {@link FastMath} library
     */
    public final static byte    FAST_MATH    = 1;

    /**
     * Defines the {@code byte} or mode of the {@link #norm(Vector2D, byte)}. In this case the
     * calculation will be executed with the lower accuracy of a lookup table.
     */
    public final static byte    LUT          = 2;

    /**
     * The magnitude of the length of the {@link #sqrtLUT}. A magnitude of 2 leads to length of 100
     * values of the LUT
     */
    private static final int    LUTMagnitude = 4;

    /**
     * The length of the {@link #sqrtLUT}. The longer, the better the resolution of the square root
     * LUT. Depends on {@link #LUTMagnitude}.
     */
    private static int          LUTLength    = 0;

    /**
     * The square root Lookup table
     */
    private static double[]     sqrtLUT      = null;

    /** The constant for 2*PI, equivalent to 360 degrees. */
    public final static double  M_2PI        = Math.PI * 2;

    /**
     * For Icecore's atan2
     */
    private static final int    Size_Ac      = 100000;

    /**
     * For Icecore's atan2
     */
    private static final int    Size_Ar      = Size_Ac + 1;

    /**
     * For Icecore's atan2
     */
    private static final float  Pi           = (float) Math.PI;

    /**
     * For Icecore's atan2
     */
    private static final float  Pi_H         = Pi / 2;

    /**
     * For Icecore's atan2
     */
    private static final float  Atan2[]      = new float[Size_Ar];

    /**
     * For Icecore's atan2
     */
    private static final float  Atan2_PM[]   = new float[Size_Ar];

    /**
     * For Icecore's atan2
     */
    private static final float  Atan2_MP[]   = new float[Size_Ar];

    /**
     * For Icecore's atan2
     */
    private static final float  Atan2_MM[]   = new float[Size_Ar];

    /**
     * For Icecore's atan2
     */
    private static final float  Atan2_R[]    = new float[Size_Ar];

    /**
     * For Icecore's atan2
     */
    private static final float  Atan2_RPM[]  = new float[Size_Ar];

    /**
     * For Icecore's atan2
     */
    private static final float  Atan2_RMP[]  = new float[Size_Ar];

    /**
     * For Icecore's atan2
     */
    private static final float  Atan2_RMM[]  = new float[Size_Ar];

    /**
     * For Icecore's atan2
     */
    static
    {
        for (int i = 0; i <= Size_Ac; i++ )
        {
            double d = (double) i / Size_Ac;
            double x = 1;
            double y = x * d;
            float v = (float) Math.atan2(y, x);
            Atan2[i] = v;
            Atan2_PM[i] = Pi - v;
            Atan2_MP[i] = -v;
            Atan2_MM[i] = -Pi + v;

            Atan2_R[i] = Pi_H - v;
            Atan2_RPM[i] = Pi_H + v;
            Atan2_RMP[i] = -Pi_H + v;
            Atan2_RMM[i] = -Pi_H - v;
        }
    }

    /**
     * For Acos LUT
     */
    private static final int   Size_Acos      = 100000;

    /**
     * For Acos LUT
     */
    private static final int   Size_Acos_Half = 50000;

    /**
     * For Acos LUT
     */
    private static final int   Size_AcosR     = Size_Acos + 1;

    /**
     * Magnitude of 5 leads to a size of 100.000 of Acos.
     */
    private static final int   AcosMagnitude  = 5;

    /**
     * For Acos LUT
     */
    private static final float Acos[]         = new float[Size_AcosR];

    /**
     * For Acos LUT
     */
    static
    {
        for (int i = 0; i < Size_Acos_Half; i++ )
        {
            double d = (double) i / Size_Acos_Half;
            double x = -1;
            double y = x + d;
            float acos = (float) Math.acos(y);
            Acos[i] = acos;
        }
        for (int i = 0; i <= Size_Acos_Half; i++ )
        {
            double y = (double) i / Size_Acos_Half;
            float acos = (float) Math.acos(y);
            Acos[i + Size_Acos_Half] = acos;
        }
    }

    /**
     * For Riven's sin/cos
     */
    private static final int     SIN_BITS, SIN_MASK, SIN_COUNT;

    /**
     * For Riven's sin/cos
     */
    private static final float   radFull, radToIndex;

    /**
     * For Riven's sin/cos
     */
    private static final float   degFull, degToIndex;

    /**
     * For Riven's sin/cos
     */
    private static final float[] sin, cos;

    /**
     * For Riven's sin/cos
     */
    static
    {
        SIN_BITS = 12;
        SIN_MASK = ~( -1 << SIN_BITS);
        SIN_COUNT = SIN_MASK + 1;

        radFull = (float) (Math.PI * 2.0);
        degFull = (float) (360.0);
        radToIndex = SIN_COUNT / radFull;
        degToIndex = SIN_COUNT / degFull;

        sin = new float[SIN_COUNT];
        cos = new float[SIN_COUNT];

        for (int i = 0; i < SIN_COUNT; i++ )
        {
            sin[i] = (float) Math.sin((i + 0.5f) / SIN_COUNT * radFull);
            cos[i] = (float) Math.cos((i + 0.5f) / SIN_COUNT * radFull);
        }

        // Four cardinal directions (credits: Nate)
        for (int i = 0; i < 360; i += 90)
        {
            sin[(int) (i * degToIndex) & SIN_MASK] = (float) Math.sin(i * Math.PI / 180.0);
            cos[(int) (i * degToIndex) & SIN_MASK] = (float) Math.cos(i * Math.PI / 180.0);
        }

    }

    static
    {
        precomputeSqrtLUT();
    }

    /**
     * Computes a random value depending on the given {@code mean} value and the given
     * {@code standardDeviation}, guaranteed to be in the 95% quantile of the normal distribution
     * (aka Gaussian Distribution) to avoid extreme outliers.
     *
     * @param mean the mean value of the distribution
     * @param standardDeviation the standard Deviation
     *
     * @return a value which is guaranteed to be within the 95% quantile of the normal distribution
     *         to avoid extreme outliers
     */
    public static float getRandomGaussianValue(float mean, float standardDeviation)
    {
        float randomGaussianValue = (float) ThreadLocalRandom.current().nextGaussian()
            * standardDeviation + mean;

        // avoid values greater than or lower than mean +/- 2 * standardDeviation
        float twoSigma = 1.960f * standardDeviation;
        if (randomGaussianValue > mean + twoSigma)
        {
            randomGaussianValue = mean + twoSigma;
        }
        else if (randomGaussianValue < mean - twoSigma)
        {
            randomGaussianValue = mean - twoSigma;
        }
        return randomGaussianValue;
    }

    /**
     * Method for rounding {@link Double} numbers.
     *
     * @param value which is to be rounded
     * @param digits determines the number of digits of the to be rounded {@code value}
     *
     * @return a {@link Double} number of the {@code value} which is rounded with {@code digits}
     *         numbers of digits
     */
    public static double round(double value, int digits)
    {
        return Math.round(value * Math.pow(10d, digits)) / Math.pow(10d, digits);
    }

    /**
     * Converts a {@link Double} from meter per second to kilometer per hour.
     *
     * @param inputValue describes the velocity in meter per second
     *
     * @return the {@code inputValue} converted in kilometer per hour
     */
    public static float convertMsToKmh(float inputValue)
    {
        return inputValue * 3.6f;
    }

    /**
     * Converts a {@link Double} from kilometer per hour to meter per second.
     *
     * @param inputValue describes the velocity in kilometer per hour
     *
     * @return the {@code inputValue} converted in meter per second
     */
    public static float convertKmhToMs(float inputValue)
    {
        return inputValue / 3.6f;
    }

    /**
     * Returns the {@link Vector2D} with same direction as the input x and y values of a
     * {@link Vector2D}, but with norm equal to 1.
     *
     * @param x the x value of a {@link Vector2D}
     * @param y the y value of a {@link Vector2D}
     *
     * @return normalized {@link Vector2D}
     */
    public static Vector2D normalize(double x, double y)
    {
        double r = hypot(x, y, FAST_MATH);
        return new Vector2D((x / r), (y / r));
    }

    /**
     * Computes the norm (squared) of the given {@link Vector2D} as a double value.
     *
     * @param vector {@link Vector2D}
     *
     * @return the euclidean norm (squared) of the {@link Vector2D} as a double value
     */
    public static double normSquared(Vector2D vector)
    {
        return vector.getX() * vector.getX() + vector.getY() * vector.getY();
    }

    /**
     * Computes the norm of the given {@link Vector2D} as a double value.
     *
     * @param vector {@link Vector2D}
     *
     * @return the euclidean norm of the {@link Vector2D} as a double value or {@link Double#NaN},
     *         if {@code vector} is either {@code null} or a zero vector.
     */
    public static double norm(Vector2D vector)
    {
        if (vector == null)
            return Double.NaN;
        if (isZeroVector(vector))
            return Double.NaN;
        // return normalized(vector, STRICT_MATH);
        return norm(vector, FAST_MATH);
        // return normalized(vector, LUT);
    }

    /**
     * Computes the norm of the given {@link Vector2D} as a double value. For this calculation
     * exists three different approaches, which differ in their accuracy, and can be invoked in this
     * method. The highest accuracy method based on the {@link StrictMath} library, the medium one
     * based on the {@link FastMath} library and the lower accuracy method on a lookup table.
     *
     * @param vector {@link Vector2D}
     * @param mode defines the mode, which should be executed in this method
     *
     * @return the euclidean norm of the {@link Vector2D} as a double value
     */
    private static double norm(Vector2D vector, byte mode)
    {
        return hypot(vector.getX(), vector.getY(), mode);
    }

    /**
     * Returns the angle of the given {@code vector} with the horizontal axis, in radians.
     *
     * @return the horizontal angle of the vector
     */
    public static double angle(Vector2D vector)
    {
        return (atan2((float) vector.getY(), (float) vector.getX()) + M_2PI) % (M_2PI);
    }

    /**
     * Tests, if the given vector is a zero vector (x==0 AND y==0)
     *
     * @param vector the vector to test
     * @return {@code true} if the given vector is a zero vector {@code false} otherwise.
     */
    public static boolean isZeroVector(Vector2D vector)
    {
        return vector.getX() == 0 && vector.getY() == 0;
    }

    /**
     * Returns the hypotenuse of a triangle with sides {@code x} and {@code y} -
     * sqrt(<i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup>)<br/>
     * .
     *
     * <ul>
     * <li>If either argument is infinite, then the result is positive infinity.</li>
     * <li>else, if either argument is NaN then the result is NaN.</li>
     * </ul>
     *
     * In case of 0 <= <i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup> <= 1 the result is not
     * computed but only looked up in {@link #sqrtLUT}. In all other cases
     * {@link FastMath#hypot(double, double)} is used for computation
     *
     * @param x a value
     * @param y a value
     * @return sqrt(<i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup>)
     */
    public static double hypot(double x, double y, byte mode)
    {
        double value = Double.NaN;
        switch (mode)
        {
            // highest accuracy
            case STRICT_MATH:
                value = Math.hypot(x, y);
                break;
            // medium accuracy
            case FAST_MATH:
                value = FastMath.hypot(x, y);
                break;
            // low accuracy
            case LUT:
                value = hypotLUT(x, y);
                break;

            default:
                break;
        }
        // for testing the actual range of input values to this function
        logger.trace("MathTools.normalized(), result=" + value + ", input=" + Math.pow(value, 2));
        return value;
    }

    /**
     * Precomputes the square root Lookup table
     */
    private static void precomputeSqrtLUT()
    {
        LUTLength = (int) Math.pow(10, LUTMagnitude);
        sqrtLUT = new double[LUTLength + 1];
        for (int i = 0; i < LUTLength + 1; i++ )
        {
            sqrtLUT[i] = Math.sqrt(i * Math.pow(10, -LUTMagnitude));
        }
    }

    /**
     * Returns the hypotenuse of a triangle with sides {@code x} and {@code y} -
     * sqrt(<i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup>)<br/>
     * .
     *
     * <ul>
     * <li>If either argument is infinite, then the result is positive infinity.</li>
     * <li>else, if either argument is NaN then the result is NaN.</li>
     * </ul>
     *
     * In case of 0 <= <i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup> <= 1 the result is not
     * computed but only looked up in {@link #sqrtLUT}. In all other cases
     * {@link FastMath#hypot(double, double)} is used for computation
     *
     * @param x a value
     * @param y a value
     * @return sqrt(<i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup>)
     */
    private static double hypotLUT(double x, double y)
    {
        double radicand = x * x + y * y;
        if (radicand > 1 || radicand < 0)
            return FastMath.hypot(x, y);
        double roundedRadicand = round(radicand, LUTMagnitude);
        return sqrtLUT[(int) (roundedRadicand * LUTLength)];
    }

    /**
     * Icecore's atan2 </br>
     *
     * http://www.java-gaming.org/index.php?topic=36467.0 </br>
     * http://www.java-gaming.org/topics/extremely-fast-atan2/36467/msg/346145/view.html#msg346145
     * </br>
     *
     * Returns the angle <i>theta</i> from the conversion of rectangular coordinates
     * ({@code x},&nbsp;{@code y}) to polar coordinates (r,&nbsp;<i>theta</i>). This method computes
     * the phase <i>theta</i> by computing an arc tangent of {@code y/x} in the range of -<i>pi</i>
     * to <i>pi</i>. Special cases:
     * <ul>
     * <li>If either argument is NaN, then the result is NaN.
     * <li>If the first argument is positive zero and the second argument is positive, or the first
     * argument is positive and finite and the second argument is positive infinity, then the result
     * is positive zero.
     * <li>If the first argument is negative zero and the second argument is positive, or the first
     * argument is negative and finite and the second argument is positive infinity, then the result
     * is negative zero.
     * <li>If the first argument is positive zero and the second argument is negative, or the first
     * argument is positive and finite and the second argument is negative infinity, then the result
     * is the {@code double} value closest to <i>pi</i>.
     * <li>If the first argument is negative zero and the second argument is negative, or the first
     * argument is negative and finite and the second argument is negative infinity, then the result
     * is the {@code double} value closest to -<i>pi</i>.
     * <li>If the first argument is positive and the second argument is positive zero or negative
     * zero, or the first argument is positive infinity and the second argument is finite, then the
     * result is the {@code double} value closest to <i>pi</i>/2.
     * <li>If the first argument is negative and the second argument is positive zero or negative
     * zero, or the first argument is negative infinity and the second argument is finite, then the
     * result is the {@code double} value closest to -<i>pi</i>/2.
     * <li>If both arguments are positive infinity, then the result is the {@code double} value
     * closest to <i>pi</i>/4.
     * <li>If the first argument is positive infinity and the second argument is negative infinity,
     * then the result is the {@code double} value closest to 3*<i>pi</i>/4.
     * <li>If the first argument is negative infinity and the second argument is positive infinity,
     * then the result is the {@code double} value closest to -<i>pi</i>/4.
     * <li>If both arguments are negative infinity, then the result is the {@code double} value
     * closest to -3*<i>pi</i>/4.
     * </ul>
     *
     * <p>
     * The computed result must be within 2 ulps of the exact result. Results must be
     * semi-monotonic.
     *
     * @param y the ordinate coordinate
     * @param x the abscissa coordinate
     * @return the <i>theta</i> component in radians of the point (<i>r</i>,&nbsp;<i>theta</i>) in
     *         polar coordinates that corresponds to the point (<i>x</i>,&nbsp;<i>y</i>) in
     *         Cartesian coordinates.
     */
    public static final float atan2(float y, float x)
    {
        if (y < 0)
        {
            if (x < 0)
            {
                // (y < x) because == (-y > -x)
                if (y < x)
                {
                    return Atan2_RMM[(int) (x / y * Size_Ac)];
                }
                return Atan2_MM[(int) (y / x * Size_Ac)];
            }
            y = -y;
            if (y > x)
            {
                return Atan2_RMP[(int) (x / y * Size_Ac)];
            }
            return Atan2_MP[(int) (y / x * Size_Ac)];
        }
        if (x < 0)
        {
            x = -x;
            if (y > x)
            {
                return Atan2_RPM[(int) (x / y * Size_Ac)];
            }
            return Atan2_PM[(int) (y / x * Size_Ac)];
        }
        if (y > x)
        {
            return Atan2_R[(int) (x / y * Size_Ac)];
        }
        return Atan2[(int) (y / x * Size_Ac)];
    }

    /**
     * Returns the arc cosine of a value; the returned angle is in the range 0.0 through <i>pi</i>.
     *
     * The computation is based on a look up table, which is accurate up to the 5th digit of the
     * input value.
     *
     * Special case:
     * <ul>
     * <li>If the argument is NaN or its absolute value is greater than 1, then the result is NaN.
     * </ul>
     *
     * @param y the value whose arc cosine is to be returned.
     * @return the arc cosine of the argument in radiant.
     */
    public static final float acos(float y)
    {
        if (y > 1f)
            return Float.NaN;
        if (y < -1f)
            return Float.NaN;
        if (Float.isNaN(y))
            return Float.NaN;
        float roundedY = (float) round(y, AcosMagnitude);
        float temp = Math.signum(y) * roundedY * Size_Acos_Half;
        float index = Size_Acos_Half + Math.signum(y) * temp;

        return Acos[(int) index];
    }

    /**
     * Computes sine value of the given ({@code deg} value, in degree.
     *
     * @see <a href=
     *      "http://www.java-gaming.org/topics/fast-math-sin-cos-lookup-tables/24191/view.html">http://www.java-gaming.org/topics/fast-math-sin-cos-lookup-tables/24191/view.html</a>
     *
     * @param deg the angle in degree
     * @return the sine value of the {@code deg} angle
     */
    public static final float sinDeg(double deg)
    {
        return sin[(int) (deg * degToIndex) & SIN_MASK];
    }

    /**
     * Computes cosines value of the given ({@code deg} value, in degree.
     *
     * @see <a href=
     *      "http://www.java-gaming.org/topics/fast-math-sin-cos-lookup-tables/24191/view.html">http://www.java-gaming.org/topics/fast-math-sin-cos-lookup-tables/24191/view.html</a>
     *
     * @param deg the angle in degree
     * @return the cosines value of the {@code deg} angle
     */
    public static final float cosDeg(double deg)
    {
        return cos[(int) (deg * degToIndex) & SIN_MASK];
    }

    /**
     * Computes sine value of the given ({@code rad} value, in radiant.
     *
     * @see <a href=
     *      "http://www.java-gaming.org/topics/fast-math-sin-cos-lookup-tables/24191/view.html">http://www.java-gaming.org/topics/fast-math-sin-cos-lookup-tables/24191/view.html</a>
     *
     * @param rad the angle in degree
     * @return the sine value of the {@code rad} angle
     */
    public static final float sin(double rad)
    {
        return sin[(int) (rad * radToIndex) & SIN_MASK];
    }

    /**
     * Computes cosines value of the given ({@code rad} value, in radiant.
     *
     * @see <a href=
     *      "http://www.java-gaming.org/topics/fast-math-sin-cos-lookup-tables/24191/view.html">http://www.java-gaming.org/topics/fast-math-sin-cos-lookup-tables/24191/view.html</a>
     *
     * @param rad the angle in degree
     * @return the cosines value of the {@code rad} angle
     */
    public static final float cos(double rad)
    {
        return cos[(int) (rad * radToIndex) & SIN_MASK];
    }

    /**
     * Main only for test purposes
     *
     * @param args not used
     */
    public static void main(String[] args)
    {
        // Tests for Riven's sin/cos LUT for degree values
        float sinDegTest1 = 45;
        float sinDegTest2 = 90;
        float sinDegTest3 = 225;
        float sinDegTest4 = 315;

        float cosDegTest1 = 45;
        float cosDegTest2 = 90;
        float cosDegTest3 = 180;
        float cosDegTest4 = 204;

        logger.info("main(), sin1 " + sinDeg(sinDegTest1));
        logger.info("main(), sin2 " + sinDeg(sinDegTest2));
        logger.info("main(), sin3 " + sinDeg(sinDegTest3));
        logger.info("main(), sin4 " + sinDeg(sinDegTest4));

        logger.info("main(), cos1 " + cosDeg(cosDegTest1));
        logger.info("main(), cos2 " + cosDeg(cosDegTest2));
        logger.info("main(), cos3 " + cosDeg(cosDegTest3));
        logger.info("main(), cos4 " + cosDeg(cosDegTest4));

        // Tests for Riven's sin/cos LUT for radiant values
        float sinRadTest1 = Pi_H / 2;
        float sinRadTest2 = Pi_H;
        float sinRadTest3 = Pi;
        float sinRadTest4 = 0;

        float cosRadTest1 = Pi_H / 2;
        float cosRadTest2 = Pi_H;
        float cosRadTest3 = Pi;
        float cosRadTest4 = 0;

        logger.info("main(), sin1 " + sin(sinRadTest1));
        logger.info("main(), sin2 " + sin(sinRadTest2));
        logger.info("main(), sin3 " + sin(sinRadTest3));
        logger.info("main(), sin4 " + sin(sinRadTest4));

        logger.info("main(), cos1 " + cos(cosRadTest1));
        logger.info("main(), cos2 " + cos(cosRadTest2));
        logger.info("main(), cos3 " + cos(cosRadTest3));
        logger.info("main(), cos4 " + cos(cosRadTest4));

        // Tests for the Acos LUT
        float acos1 = -1;
        float acos2 = 0;
        float acos3 = 1;
        float acos4 = -0.6f;
        float acos5 = 0.67f;

        logger.info("main(), acos1: " + acos1 + ":  " + acos(acos1));
        logger.info("main(), acos2: " + acos2 + ":  " + acos(acos2));
        logger.info("main(), acos3: " + acos3 + ":  " + acos(acos3));
        logger.info("main(), acos4: " + acos4 + ":  " + acos(acos4));
        logger.info("main(), acos5: " + acos5 + ":  " + acos(acos5));
    }
}
