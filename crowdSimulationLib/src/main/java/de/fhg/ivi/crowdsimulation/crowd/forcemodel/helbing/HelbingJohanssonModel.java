package de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing;

import de.fhg.ivi.crowdsimulation.boundaries.Boundary;
import de.fhg.ivi.crowdsimulation.crowd.Pedestrian;

/**
 * The HelbingJohansson model is another empirical approach of the Social Force Model, based on the
 * publications of Helbing and Molnár (1995) and Helbing et al. (2005). This model itself based on
 * the publication Johansson et al. (2007) (see the last hyperlink below).
 * <p>
 * Implements the 10 parameters of the {@link HelbingModel}: A1, B1, A2, B2 for
 * pedestrian-pedestrian interaction and A1, B1, A2, B2 for pedestrian-boundary (obstacle)
 * interaction. Values for pedestrian-pedestrian interaction as well as tau and lambda are empirical
 * values as described in Johansson et al. (2007). The remaining values are described in Helbing et
 * al. (2005).
 *
 * TODO test implementation
 * <p>
 * <b>Implementation does not seem to work properly at the moment!.</b>
 *
 * @see <a href=
 *      "https://pubsonline.informs.org/doi/abs/10.1287/trsc.1040.0108">https://pubsonline.informs.org/doi/abs/10.1287/trsc.1040.0108</a>
 * @see <a href=
 *      "http://www.worldscientific.com/doi/pdf/10.1142/S0219525907001355">http://www.worldscientific.com/doi/pdf/10.1142/S0219525907001355</a>
 *
 * @see <a href=
 *      "http://nbn-resolving.de/urn:nbn:de:bsz:14-qucosa-20900">http://nbn-resolving.de/urn:nbn:de:bsz:14-qucosa-20900</a>
 *
 *
 *
 *
 * @author hahmann/meinert
 *
 */
public class HelbingJohanssonModel extends HelbingModel
{
    /**
     * Gets the strength parameter A2 of the acceleration resulting from the interaction of a
     * {@link Pedestrian} with another {@link Pedestrian}. Given in m/s². Cf. Johansson et al.
     * (2007) p. 10 for Elliptical II and Lambda.
     *
     * @return the non-empirical strength parameter A1 of the acceleration resulting from the
     *         interaction of a {@link Pedestrian} with another {@link Pedestrian} given in m/s²
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingModel#getParameterBoundaryA1()
     */
    @Override
    public float getParameterPedestrianA1()
    {
        return 0.04f;
    }

    /**
     * Gets the B1 Parameter of the interaction range range with another {@link Pedestrian}. Given
     * in meters. Parameterizes the private zone of a {@link Pedestrian}. Cf. Johansson et al.
     * (2007) p. 10 for Elliptical II and Lambda.
     *
     * @return the non-empirical B1 Parameter of the interaction range. Given in meters.
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingModel#getParameterBoundaryB1()
     */
    @Override
    public float getParameterPedestrianB1()
    {
        return 3.22f;
    }

    /**
     * Gets the strength parameter A2 of the acceleration resulting from the interaction of a
     * {@link Pedestrian} with another {@link Pedestrian}. Given in m/s².
     * <p>
     * Is set to zero in Johansson model, because this part of the SFM Model has no influence in
     * Johansson's approach.
     *
     * @return the non-empirical strength parameter A1 of the acceleration resulting from the
     *         interaction of a {@link Pedestrian} with another {@link Pedestrian}. Given in m/s�
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingModel#getParameterBoundaryA2()
     */
    @Override
    public float getParameterPedestrianA2()
    {
        return 0;
    }

    /**
     * Gets the B1 Parameter of the interaction range range with another {@link Pedestrian}. Given
     * in meters. Parameterizes the private zone of a {@link Pedestrian}.
     * <p>
     * Is set to zero in Johansson model, because this part of the SFM Model has no influence in
     * Johansson's approach.
     *
     * @return the B2 Parameter of the interaction range. Given in meters.
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingModel#getParameterBoundaryB2()
     */
    @Override
    public float getParameterPedestrianB2()
    {
        return 0;
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
     * {@link Pedestrian} with a {@link Boundary}. Given in m/s�. Cf. Johansson uses the value from
     * cf. Helbing et al (2005) p. 12.
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
     * Parameterizes the comfort zone of a {@link Pedestrian}. Cf. Johansson uses the value from cf.
     * Helbing et al (2005) p. 12.
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
        return 1.0f;
    }

    /**
     * Gets the parameter that takes into account the anisotropic character of pedestrian
     * interactions, as the situation in front of a pedestrian has a larger impact on his or her
     * behavior than things happening behind. Cf. Johansson (2007) p. 10.
     *
     * @return the parameter that takes into account the anisotropic character of pedestrian
     *         interactions
     * @see de.fhg.ivi.crowdsimulation.crowd.forcemodel.helbing.HelbingModel#getLambda()
     */
    @Override
    public float getLambda()
    {
        return 0.06f;
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
        return 0.5f;
    }
}
