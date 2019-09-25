package de.unijena.bioinf.ms.gui.utils;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 10.10.16.
 */

import javax.swing.*;
import java.awt.*;

public class ToolbarButton extends JButton {
    public ToolbarButton(String text, Icon icon, String tooltip) {
        super(text, icon);
        configureTButton();
        setToolTipText(tooltip);
    }

    public ToolbarButton(Action action) {
        super(action);
        configureTButton();

    }

    public ToolbarButton(Icon icon) {
        this(null, icon, null);
    }

    public ToolbarButton(Icon icon, String tooltip) {
        this(null, icon, tooltip);
    }

    public ToolbarButton(String text, Icon icon) {
        this(text, icon, null);
    }

    private void configureTButton() {
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setMargin(new Insets(1, 2, 1, 2));
    }
}
