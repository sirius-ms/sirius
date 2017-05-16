package de.unijena.bioinf.sirius.gui.table;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 31.01.17.
 */

import de.unijena.bioinf.sirius.gui.fingerid.CandidateList;

import javax.swing.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class ActionListView<T extends ActionList> extends JPanel {
    protected final T source;

    public ActionListView(T source) {
        super();
        this.source = source;
    }
}


