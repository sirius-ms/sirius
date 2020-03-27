package de.unijena.bioinf.ms.gui.utils;

import de.unijena.bioinf.ms.gui.table.FilterRangeSlider;

import javax.swing.*;

/**
 * Created by fleisch on 18.05.17.
 */
public class NameFilterRangeSlider extends TwoColumnPanel {
    public NameFilterRangeSlider(String name, FilterRangeSlider slider) {
        super(new JLabel(name), slider);
    }
}
