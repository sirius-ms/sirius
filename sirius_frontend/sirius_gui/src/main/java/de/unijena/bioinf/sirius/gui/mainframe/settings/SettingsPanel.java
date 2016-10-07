package de.unijena.bioinf.sirius.gui.mainframe.settings;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 07.10.16.
 */

import javax.swing.*;
import java.util.Properties;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface SettingsPanel {
//    public Properties getProperties();
    public void refreshValues ();
    public void saveProperties();
    public String name();
}
