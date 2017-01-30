package de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 30.01.17.
 */

import de.unijena.bioinf.sirius.gui.mainframe.results.ActiveResultChangedListener;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class FormulaSelectionResultPanel extends JPanel {
    protected final FormulaTable resultElementTable;

    public FormulaSelectionResultPanel(FormulaTable resultElementTable) {
        super(new BorderLayout());
        this.resultElementTable = resultElementTable;
        add(resultElementTable, BorderLayout.NORTH);
    }

    protected void buildPanel(){
        addElement(createCenter(), BorderLayout.CENTER);
        addElement(createWest(), BorderLayout.WEST);
        addElement(createEast(), BorderLayout.EAST);
        addElement(createSouth(), BorderLayout.SOUTH);
    }

    protected abstract JComponent createSouth();

    protected abstract JComponent createEast();

    protected abstract JComponent createWest();

    protected abstract JComponent createCenter();

    protected void addElement(JComponent panel, String pos) {
        if (panel != null) {
            if (panel instanceof ActiveResultChangedListener)
                resultElementTable.addActiveResultChangedListener((ActiveResultChangedListener) panel);
            add(panel,pos);
        }
    }
}
