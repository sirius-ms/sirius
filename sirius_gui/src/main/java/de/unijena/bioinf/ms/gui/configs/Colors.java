package de.unijena.bioinf.ms.gui.configs;
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


    public final static Color ICON_BLUE = new Color(17, 145, 187);
    public final static Color ICON_GREEN = new Color(0, 191, 48);
    public final static Color ICON_RED = new Color(204, 71, 41);
    public final static Color ICON_YELLOW = new Color(255, 204, 0);

    public final static Color LIST_SELECTED_BACKGROUND = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].background");
    public final static Color LIST_SELECTED_FOREGROUND = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].textForeground");
    public final static Color LIST_EVEN_BACKGROUND = Color.WHITE;
    public final static Color LIST_DISABLED_BACKGROUND = UIManager.getColor("ComboBox.background");
    public final static Color LIST_UNEVEN_BACKGROUND = new Color(213, 227, 238);
    public final static Color LIST_ACTIVATED_FOREGROUND = UIManager.getColor("List.foreground");
    public final static Color LIST_DEACTIVATED_FOREGROUND = Color.GRAY;
    public final static Color LIST_LIGHT_GREEN = new Color(161, 217, 155);
    public final static Color LIST_SELECTED_GREEN = new Color(49, 163, 84);

    public final static Color DB_LINKED = new Color(155, 166, 219);
    public final static Color DB_UNLINKED = Color.GRAY;
    public final static Color DB_CUSTOM = ICON_YELLOW;
    public final static Color DB_TRAINING = Color.BLACK;
    public final static Color DB_UNKNOWN = new Color(178,34,34);
}
