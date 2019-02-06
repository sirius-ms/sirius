package de.unijena.bioinf.ms.gui.utils;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 10.10.16.
 */

import javax.swing.*;
import java.awt.*;

public class ToolbarToggleButton extends JToggleButton {
    public ToolbarToggleButton(String text, Icon icon, String tooltip) {
        super(text, icon);
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setMargin(new Insets(1, 2, 1, 2));
        setToolTipText(tooltip);
    }

    public ToolbarToggleButton(Icon icon) {
        this(null,icon,null);
    }

    public ToolbarToggleButton(Icon icon, String tooltip) {
        this(null,icon,tooltip);
    }

    public ToolbarToggleButton(String text, Icon icon) {
        this(text,icon,null);
    }
}
