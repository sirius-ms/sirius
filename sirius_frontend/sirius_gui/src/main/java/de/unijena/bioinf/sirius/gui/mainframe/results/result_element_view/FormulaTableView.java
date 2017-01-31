package de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 31.01.17.
 */

import javax.swing.*;
import java.awt.event.MouseListener;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class FormulaTableView extends JPanel {
    protected final FormulaTable source;

    public FormulaTableView(FormulaTable source) {
        super();
        this.source = source;
    }

}


