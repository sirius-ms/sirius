package de.unijena.bioinf.ms.gui.table;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 30.01.17.
 */

import de.unijena.bioinf.ms.frontend.core.SiriusPCS;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface ActiveElements<E extends SiriusPCS, D> {
    void addActiveResultChangedListener(ActiveElementChangedListener<E, D> listener);

    void removeActiveResultChangedListener(ActiveElementChangedListener<E, D> listener);
}
