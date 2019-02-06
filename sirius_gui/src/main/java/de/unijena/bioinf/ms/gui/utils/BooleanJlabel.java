package de.unijena.bioinf.ms.gui.utils;

import de.unijena.bioinf.ms.gui.configs.Icons;

import javax.swing.*;

/**
 * Created by fleisch on 05.06.17.
 */
public class BooleanJlabel extends JLabel {

    public void setState(boolean state) {
        setIcon(state ? Icons.YES_16:Icons.NO_16);
    }

    public boolean isTrue() {
        return getIcon().equals(Icons.YES_16);
    }

    public BooleanJlabel(boolean state) {
        setState(state);
    }

    public BooleanJlabel() {
        this(false);
    }
}
