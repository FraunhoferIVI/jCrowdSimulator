package de.fhg.ivi.crowdsimulation.ui.gui.tools;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;

public class FileTools
{

    /**
     * Opens a {@link JFileChooser} to select a {@link File}.
     *
     * @return the {@link File} selected by the user
     */
    public static File chooseFile(Component parent)
    {
        JFileChooser chooser = new JFileChooser();
        File file = null;
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
        {
            file = chooser.getSelectedFile();
        }
        return file;
    }
}
