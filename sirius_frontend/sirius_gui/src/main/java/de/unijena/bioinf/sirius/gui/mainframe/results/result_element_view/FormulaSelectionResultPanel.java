package de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 30.01.17.
 */

import de.unijena.bioinf.sirius.gui.mainframe.results.ActiveResultChangedListener;
import de.unijena.bioinf.sirius.gui.mainframe.results.results_table.SiriusResultTablePanel;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

import javax.swing.*;
import java.awt.*;

import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class FormulaSelectionResultPanel extends JPanel {
    private FormulaTable  source;
    protected final JList<SiriusResultElement> resultElementTable;

    public FormulaSelectionResultPanel(FormulaTable source) {
        this(source,true);
    }
    public FormulaSelectionResultPanel(FormulaTable source, boolean clone) {
        super(new BorderLayout());
        this.source = source;
        if (clone) {
            this.resultElementTable = new JList<>(source.getResultListView().getModel());
            JScrollPane p = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(1, 5, 0, 0), "Molecular formulas"));
            p.add(resultElementTable);
            add(p, BorderLayout.NORTH);
        } else {
            resultElementTable = null;
            add(source, BorderLayout.NORTH);
        }
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
                source.addActiveResultChangedListener((ActiveResultChangedListener) panel);
            add(panel,pos);
        }
    }
}
