package de.unijena.bioinf.sirius.gui.table;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 31.01.17.
 */

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class ActionListView<T extends ActionList> extends JPanel {
    protected JToolBar toolBar;
    protected final T source;

    public ActionListView(T source, JToolBar toolbar) {
        super();
        this.source = source;
        this.toolBar = toolbar;
        setLayout(new BorderLayout());
        if (toolbar != null)
            add(buildNorth(), BorderLayout.NORTH);
    }

    protected JComponent buildNorth(){
        return toolBar;
    }
}


