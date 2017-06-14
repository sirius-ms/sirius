package de.unijena.bioinf.sirius.gui.table;

import javax.swing.*;

/**
 * Created by fleisch on 22.05.17.
 */
public abstract class ActionListView<T extends ActionList> extends JPanel {
    protected final T source;

    public ActionListView(T source) {
        super();
        this.source = source;
    }

    public T getSource() {
        return source;
    }
}
