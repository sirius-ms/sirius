package de.unijena.bioinf.sirius.gui.utils;

import de.unijena.bioinf.sirius.gui.table.FilterRangeSlider;

import javax.swing.*;

/**
 * Created by fleisch on 18.05.17.
 */
public class NameFilterRangeSlider extends TwoCloumnPanel {
    public NameFilterRangeSlider(String name, FilterRangeSlider slider) {
        super(new JLabel(name), slider);
    }
}
