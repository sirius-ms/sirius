package de.unijena.bioinf.sirius.gui.mainframe.results;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 30.01.17.
 */

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface ActiveResults {
    void addActiveResultChangedListener(ActiveResultChangedListener listener);
    void removeActiveResultChangedListener(ActiveResultChangedListener listener);
}
