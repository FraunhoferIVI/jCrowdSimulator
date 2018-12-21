package de.fhg.ivi.crowdsimulation.ui.gui.tools;

import java.util.concurrent.TimeUnit;

/**
 * Class for providing helping methods in case of time depending functions. At the moment there are
 * one method in this class:
 *
 * <li>Convert a time from {@link Long} format into a specific {@link String} format</li>
 *
 * <p>
 *
 * @author hahmann/meinert
 */
public class TimeTools
{
    /**
     * Converts a given {@link Long} {@code time} to a {@link String} representation that show this
     * {@code time} as minutes:seconds, e.g. 5.000 is converted to 00:05.
     *
     * @param time a time stamp in {@link Long} format in milliseconds
     *
     * @return the minutes and seconds representation of {@code time} as {@link String}
     */
    public static String getMinutesAndSecondsTimeRepresentation(long time)
    {
        String simulatedTimespanString = String.format("%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(time), TimeUnit.MILLISECONDS.toSeconds(time)
                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time)));

        return simulatedTimespanString;
    }
}
