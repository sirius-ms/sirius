package de.unijena.bioinf.sirius.gui.table;

import ca.odell.glazedlists.gui.TableFormat;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

/**
 * Created by fleisch on 15.05.17.
 */
public interface SiriusTableFormat<E> extends TableFormat<E> {
    int highlightColumn();
}