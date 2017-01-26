package de.unijena.bioinf.sirius.gui.utils;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 11.10.16.
 */

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class Colors {
    public static Color ICON_BLUE = new Color(17,145,187);
    public static Color ICON_GREEN = new Color(0,191,48);
    public static Color ICON_RED = new Color(204,71,41);
    public static Color ICON_YELLOW = new Color(255,204,0);

    public static Color LIST_SELECTED_BACKGROUND = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].background");
    public static Color LIST_SELECTED_FOREGROUND = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].textForeground");
    public static Color LIST_EVEN_BACKGROUND = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\".background");
    public static Color LIST_DISABLED_BACKGROUND = UIManager.getColor("ComboBox.background");
    public static Color LIST_UNEVEN_BACKGROUND = new Color(213, 227, 238);
    public static Color LIST_ACTIVATED_FOREGROUND = UIManager.getColor("List.foreground");
    public static Color LIST_DEACTIVATED_FOREGROUND = Color.GRAY;
}
