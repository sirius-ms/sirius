package de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 31.01.17.
 */

import de.unijena.bioinf.sirius.gui.mainframe.results.ActiveResultChangedListener;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaTableHeaderPanel extends JPanel {
    public FormulaTableHeaderPanel(FormulaTable table, JComponent center) {
        this(table,center,false);
    }

    public FormulaTableHeaderPanel(FormulaTable table, JComponent center, boolean detailed) {
        super(new BorderLayout());
        if (detailed){
            add(new FormulaTableDetailView(table),BorderLayout.NORTH);
        }else{
            add(new FormulaTableCompactView(table),BorderLayout.NORTH);
        }
        add(center,BorderLayout.CENTER);
        if (center instanceof ActiveResultChangedListener)
            table.addActiveResultChangedListener((ActiveResultChangedListener) center);
    }
}
