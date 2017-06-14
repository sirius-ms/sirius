package de.unijena.bioinf.sirius.gui.table;

import ca.odell.glazedlists.Filterator;
import ca.odell.glazedlists.matchers.RangeMatcherEditor;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Created by fleisch on 18.05.17.
 */
public class MinMaxMatcherEditor<E> extends RangeMatcherEditor<Double,E> {


    public MinMaxMatcherEditor(final FilterRangeSlider slider, final Filterator<Double, E> filterator) {
        super(filterator);
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setRange(slider.getMinSelected(),slider.getMaxSelected());
            }
        });
    }
}


