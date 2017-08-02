package de.unijena.bioinf.sirius.gui.table;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 30.01.17.
 */

import org.jdesktop.beans.AbstractBean;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface ActiveElements<E extends AbstractBean, D> {
    void addActiveResultChangedListener(ActiveElementChangedListener<E, D> listener);
    void removeActiveResultChangedListener(ActiveElementChangedListener<E, D> listener);
}
