package de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing;

import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;

/**
 * According to our state of knowledge the HelbingBuznaModel or Helbing et al. 2005 (take a look @
 * see below) is the first model which takes an empirical calibration of the Helbing and Molnár
 * (1995) Social Force Model into account.
 * <p>
 * This model class implements the 10 parameters of the {@link HelbingModel}: A1, B1, A2, B2 for
 * pedestrian-pedestrian interaction and A1, B1, A2, B2 for pedestrian-boundary (obstacle)
 * interaction. Furthermore values for lambda and tau are implemented. All values are take on as
 * described in Helbing et al. (2005).
 *
 * @see <a href=
 *      "https://pubsonline.informs.org/doi/abs/10.1287/trsc.1040.0108">https://pubsonline.informs.org/doi/abs/10.1287/trsc.1040.0108</a>
 *
 * @author hahmann/meinert
 *
 */
public class HelbingBuznaModel extends HelbingModel
{

    /**
     * Gets the strength parameter A1 of the acceleration resulting from the interaction of a
     * {@link Pedestrian} with another {@link Pedestrian} given in m/s�.
     * <p>
     * Is set to zero to speed up the simulation in crowded situations, cf. Helbing et al (2005) p.
     * 12.
     *
     * @return the non-empirical strength parameter A1 of the acceleration resulting from the
     *         interaction of a {@link Pedestrian} with another {@link Pedestrian} given in m/s�
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingModel#getParameterPedestrianA1()
     */
    @Override
    public float getParameterPedestrianA1()
    {
        return 0;
    }

    /**
     * Gets the B1 Parameter of the interaction range range with another {@link Pedestrian}. Given
     * in meters. Parameterizes the private zone of a {@link Pedestrian}.
     *
     * This has no influence on the calculation because {@link #getParameterPedestrianA1()} is set
     * to zero in this {@link HelbingModel}
     *
     * @return the non-empirical B1 Parameter of the interaction range. Given in meters.
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingModel#getParameterPedestrianB1()
     */
    @Override
    public float getParameterPedestrianB1()
    {
        return 0;
    }

    /**
     * Gets the strength parameter A2 of the acceleration resulting from the interaction of a
     * {@link Pedestrian} with another {@link Pedestrian}. Given in m/s�. cf. Helbing et al (2005)
     * p. 12.
     *
     * @return the non-empirical strength parameter A1 of the acceleration resulting from the
     *         interaction of a {@link Pedestrian} with another {@link Pedestrian}. Given in m/s�
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingModel#getParameterPedestrianA2()
     */
    @Override
    public float getParameterPedestrianA2()
    {
        return 3f;
    }

    /**
     * Gets the B2 Parameter of the interaction range with another {@link Pedestrian}. Given in
     * meters. Parameterizes the private zone of a {@link Pedestrian}
     *
     * @return the B2 Parameter of the interaction range. Given in meters.
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingModel#getParameterPedestrianB2()
     */
    @Override
    public float getParameterPedestrianB2()
    {
        return 0.2f;
    }

    /**
     * Gets the strength parameter A1 of the acceleration resulting from the interaction of a
     * {@link Pedestrian} with a {@link Boundary} given in m/s�.
     *
     * @return the strength parameter A1 of the acceleration resulting from the interaction of a
     *         {@link Pedestrian} with a {@link Boundary} given in m/s�
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingModel#getParameterBoundaryA1()
     */
    @Override
    public float getParameterBoundaryA1()
    {
        return 0f;
    }

    /**
     * Gets the B2 Parameter of the interaction range with a {@link Boundary}. Given in meters.
     * Parameterizes the comfort zone of a {@link Pedestrian}.
     *
     * @return the B2 Parameter of the interaction range. Given in meters.
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingModel#getParameterBoundaryB1()
     */
    @Override
    public float getParameterBoundaryB1()
    {
        return 0f;
    }

    /**
     * Gets the strength parameter A2 of the acceleration resulting from the interaction of a
     * {@link Pedestrian} with a {@link Boundary}. Given in m/s�. cf. Helbing et al (2005) p. 12.
     *
     * @return the strength parameter A1 of the acceleration resulting from the interaction of a
     *         {@link Pedestrian} with a {@link Boundary}. Given in m/s�
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingModel#getParameterBoundaryA2()
     */
    @Override
    public float getParameterBoundaryA2()
    {
        return 5f;
    }

    /**
     * Gets the B2 Parameter of the interaction range with a {@link Boundary}. Given in meters.
     * Parameterizes the comfort zone of a {@link Pedestrian}
     *
     * @return the B2 Parameter of the interaction range. Given in meters.
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingModel#getParameterBoundaryB2()
     */
    @Override
    public float getParameterBoundaryB2()
    {
        return 0.1f;
    }

    /**
     * "Relaxation time" of a {@link Pedestrian}. Given in seconds.
     *
     * @return the "Relaxation time" of a {@link Pedestrian}. Given in seconds.
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingModel#getTau()
     */
    @Override
    public float getTau()
    {
        return 1f;
    }

    /**
     * Gets the parameter that takes into account the anisotropic character of pedestrian
     * interactions, as the situation in front of a pedestrian has a larger impact on his or her
     * behavior than things happening behind. Cf. Helbing (2005), p. 13.
     *
     * In this model, this has no influence, since {@link #getParameterPedestrianA1()} is set to
     * zero, with the consequence that {@link Pedestrian} react the same way to other
     * {@link Pedestrian} no matter, whether they are in front of them or behind
     *
     * @return the parameter that takes into account the anisotropic character of pedestrian
     *         interactions
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingModel#getLambda()
     */
    @Override
    public float getLambda()
    {
        return 0.75f;
    }

    /**
     * Gets the parameter, which denotes the step size of a {@link Pedestrian}. It's used in the
     * elliptical specification of the HelbingJohansson Model (not in the HelbingBuzna Model). Cf.
     * Johansson (2007).
     *
     * @return the parameter which denotes the step size of a {@link Pedestrian}
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingModel#getDeltaT()
     */
    @Override
    public float getDeltaT()
    {
        return 0f;
    }
}
