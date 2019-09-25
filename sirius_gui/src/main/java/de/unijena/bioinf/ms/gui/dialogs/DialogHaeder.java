package de.unijena.bioinf.ms.gui.dialogs;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 13.10.16.
 */

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class DialogHaeder extends JPanel {

    public DialogHaeder(Icon icon) {
        this(icon, null);
    }

    public DialogHaeder(Icon icon, String headline) {
        super(new FlowLayout(FlowLayout.CENTER));
        JLabel l = new JLabel();
        l.setIcon(icon);
        add(l);
        if (headline != null) {
            JLabel intro = new JLabel(headline);
            intro.setFont(intro.getFont().deriveFont(36f));
            add(intro);
        }
    }
}
