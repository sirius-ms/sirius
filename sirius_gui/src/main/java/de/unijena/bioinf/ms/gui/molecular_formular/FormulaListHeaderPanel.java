package de.unijena.bioinf.ms.gui.molecular_formular;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 31.01.17.
 */

import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaListHeaderPanel extends JPanel {
    public FormulaListHeaderPanel(FormulaList table, JComponent center) {
        this(table,center,false);
    }

    public FormulaListHeaderPanel(FormulaList formulaList, JComponent center, boolean detailed) {
        super(new BorderLayout());
        if (center instanceof ActiveElementChangedListener)
            formulaList.addActiveResultChangedListener((ActiveElementChangedListener) center);

        if (detailed){
            add(new FormulaListDetailView(formulaList),BorderLayout.NORTH);
        }else{
            add(new FormulaListCompactView(formulaList),BorderLayout.NORTH);
        }
        add(center,BorderLayout.CENTER);


    }
}
