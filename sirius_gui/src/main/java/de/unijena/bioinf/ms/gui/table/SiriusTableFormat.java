package de.unijena.bioinf.ms.gui.table;

import ca.odell.glazedlists.gui.TableFormat;
import de.unijena.bioinf.ms.gui.table.list_stats.ListStats;

/**
 * Created by fleisch on 15.05.17.
 */
public abstract class SiriusTableFormat<E> implements TableFormat<E> {
    protected final ListStats stats;

    protected SiriusTableFormat(ListStats stats) {
        this.stats = stats;
    }

    protected abstract int highlightColumnIndex();
    protected abstract boolean isBest(E element);
}