package de.unijena.bioinf.sirius.gui.table;

import ca.odell.glazedlists.Filterator;
import ca.odell.glazedlists.matchers.RangeMatcherEditor;

/**
 * Created by fleisch on 18.05.17.
 */
public class MinMaxMatcherEditor<E> extends RangeMatcherEditor<Double, E> {


    public MinMaxMatcherEditor(final FilterRangeSlider slider, final Filterator<Double, E> filterator) {
        super(filterator);
        slider.addRangeListener(evt -> {
            final FilterRangeSlider s = (FilterRangeSlider) evt.getSource();
            setRange(s.getMinSelected(), s.getMaxSelected());
        });
    }
}


