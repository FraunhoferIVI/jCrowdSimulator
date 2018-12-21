package de.fhg.ivi.crowdsimulation.time;

/**
 * This class was created for the implementation of a time lapse. Generally the FastForwardClock
 * should speed up the simulation.
 *
 * @author Hahmann
 */
public class FastForwardClock
{
    /**
     * The current fake time in dependence by the {@code fastForwardFactor}. Given in milliseconds.
     */
    private long fakeTimeMillis     = 0;

    /**
     * The current system time. Given in milliseconds.
     */
    private long lastRealTimeMillis = 0;

    /**
     * The current fake time in dependence by the {@code fastForwardFactor}. Given in milliseconds.
     */
    private long fakeTimeNanos      = 0;

    /**
     * The current system time. Given in milliseconds.
     */
    private long lastRealTimeNanos  = 0;

    /**
     * Invokes the method currentTimeMillis() with a specific fastForwardFactor. This will speed up
     * (or speed down) the {@code simulationThread} by this factor.
     *
     * @return the current {@code fakeTimeStamp}
     */
    public long currentTimeMillis()
    {
        return currentTimeMillis(1);
    }

    /**
     * Computes a faked "real time" depending on a fast forward factor {@code fastForwardFactor}.
     *
     * @param fastForwardFactor the factor to speed up the resulting simulated time
     *
     * @return the new resulting simulated time
     */
    public long currentTimeMillis(long fastForwardFactor)
    {
        synchronized (this)
        {
            if (fakeTimeMillis == 0)
            {
                fakeTimeMillis = System.currentTimeMillis();
            }
            else
            {
                long fakeDeltaTime = (System.currentTimeMillis() - lastRealTimeMillis)
                    * fastForwardFactor;
                fakeTimeMillis += fakeDeltaTime;
            }
            lastRealTimeMillis = System.currentTimeMillis();
        }

        return fakeTimeMillis;
    }

    /**
     * Computes a faked "real time" depending on a fast forward factor {@code fastForwardFactor}.
     *
     * @param fastForwardFactor the factor to speed up the resulting simulated time
     *
     * @return the new resulting simulated time
     */
    public long nanoTime(long fastForwardFactor)
    {
        synchronized (this)
        {
            if (fakeTimeNanos == 0)
            {
                fakeTimeNanos = System.nanoTime();
            }
            else
            {
                long fakeDeltaTime = (System.nanoTime() - lastRealTimeNanos) * fastForwardFactor;
                fakeTimeNanos += fakeDeltaTime;
            }
            lastRealTimeNanos = System.nanoTime();
        }

        return fakeTimeNanos;
    }
}
