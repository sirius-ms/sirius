package de.unijena.bioinf.sirius.gui.mainframe.molecular_formular;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 31.01.17.
 */

import de.unijena.bioinf.sirius.gui.table.ActiveElementChangedListener;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaListHeaderPanel extends JPanel {
    public FormulaListHeaderPanel(FormulaList table, JComponent center) {
        this(table,center,false);
    }

    public FormulaListHeaderPanel(FormulaList table, JComponent center, boolean detailed) {
        super(new BorderLayout());
        if (detailed){
            add(new FormulaListDetailView(table),BorderLayout.NORTH);
        }else{
            add(new FormulaListCompactView(table),BorderLayout.NORTH);
        }
        add(center,BorderLayout.CENTER);
        if (center instanceof ActiveElementChangedListener)
            table.addActiveResultChangedListener((ActiveElementChangedListener) center);
    }
}
