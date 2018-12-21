package de.fhg.ivi.crowdsimulation.ui.gui;

import java.util.LinkedHashMap;

import javax.swing.JPanel;

import de.fhg.ivi.crowdsimulation.ui.CrowdSimulation;

/**
 * Management class, where all {@link JPanel}s were set and get in a {@link LinkedHashMap}.
 *
 * @author hahmann/meinert
 */
public class ComponentManager
{
    /**
     * {@link LinkedHashMap} of all {@link JPanel} added to this {@link CrowdSimulation} object
     */
    private final LinkedHashMap<Class<? extends JPanel>, JPanel> components;

    /**
     * Constructor. Creates a new {@link LinkedHashMap}.
     */
    public ComponentManager()
    {
        components = new LinkedHashMap<>();
    }

    /**
     * Gets the class name of the {@link JPanel}
     *
     * @param className name of the class to be obtained
     *
     * @return an object of the class with this {@code className}
     */
    @SuppressWarnings("unchecked")
    public <T extends JPanel> T getComponent(Class<T> className)
    {
        return (T) components.get(className);
    }

    /**
     * Adds a {@link JPanel} class to this class.
     *
     * @param jPanel object for a class, which extends {@link JPanel}
     */
    public void addComponent(JPanel jPanel)
    {
        if (jPanel == null)
            throw new IllegalArgumentException("The jPanel must not be null!");
        if (components.containsKey(jPanel.getClass()))
        {
            // do not add a JPanel of the same class twice
            throw new IllegalArgumentException(
                "The class " + jPanel.getClass().getName() + " has already been added!");
        }
        components.put(jPanel.getClass(), jPanel);
    }
}
