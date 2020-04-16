package de.unijena.bioinf.ms.gui.table;

import ca.odell.glazedlists.gui.TableFormat;
import de.unijena.bioinf.ms.gui.table.list_stats.ListStats;

import java.util.function.Function;

/**
 * Created by fleisch on 15.05.17.
 */
public abstract class SiriusTableFormat<E> implements TableFormat<E> {
    protected final Function<E,Boolean> isBest;

    protected SiriusTableFormat(Function<E,Boolean> isBest) {
        this.isBest = isBest;
    }

    protected abstract int highlightColumnIndex();
}