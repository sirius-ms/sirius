package de.unijena.bioinf.sirius.gui.utils;

import org.jdesktop.swingx.JXTitledSeparator;

import javax.swing.*;
import java.awt.*;

public class TextHeaderBoxPanel extends JPanel {
    private JPanel body;

    public TextHeaderBoxPanel(final String headerText) {
        this(headerText, false);
    }

    public TextHeaderBoxPanel(final String headerText, boolean horizontal) {
        super(new BorderLayout());
        body = new JPanel();
        body.setLayout(new BoxLayout(body, horizontal ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS));
        super.add(new JXTitledSeparator(headerText), BorderLayout.NORTH);
        super.add(body);
    }

    @Override
    public Component add(Component comp) {
        return body.add(comp);
    }

    @Override
    public Component add(String name, Component comp) {
        return body.add(name, comp);
    }

    @Override
    public Component add(Component comp, int index) {
        return body.add(comp, index);
    }

    @Override
    public void add(Component comp, Object constraints) {
        body.add(comp, constraints);
    }

    @Override
    public void add(Component comp, Object constraints, int index) {
        body.add(comp, constraints, index);
    }

    @Override
    public void remove(int index) {
        body.remove(index);
    }

    @Override
    public void remove(Component comp) {
        body.remove(comp);
    }

    @Override
    public void removeAll() {
        body.removeAll();
    }

    public JPanel getBody() {
        return body;
    }
}
