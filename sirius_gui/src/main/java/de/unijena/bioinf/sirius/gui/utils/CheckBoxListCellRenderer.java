package de.unijena.bioinf.sirius.gui.utils;

import javax.swing.*;
import java.awt.*;

public class CheckBoxListCellRenderer extends JCheckBox implements ListCellRenderer<String> {
    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
        setComponentOrientation(list.getComponentOrientation());
        setFont(list.getFont());
        setBackground(list.getBackground());
        setForeground(list.getForeground());
        setSelected(isSelected);
        setEnabled(list.isEnabled());

        setText(value == null ? "" : value);

        return this;
    }
}
