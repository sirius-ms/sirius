package de.unijena.bioinf.ms.gui.utils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Created by fleisch on 18.05.17.
 */
public class SearchTextField extends TwoColumnPanel {
    public final JTextField textField;

    public SearchTextField() {
        super();
        setBorder(new EmptyBorder(0, 0, 0, 0));
        textField = new JTextField();
        textField.setPreferredSize(new Dimension(100, textField.getPreferredSize().height));
        add(new JLabel("Filter"), textField);
    }
}
