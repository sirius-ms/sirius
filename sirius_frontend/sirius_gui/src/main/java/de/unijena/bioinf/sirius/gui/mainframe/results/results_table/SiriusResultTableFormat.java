package de.unijena.bioinf.sirius.gui.mainframe.results.results_table;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 25.01.17.
 */

import ca.odell.glazedlists.gui.TableFormat;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

/**
 * Display issues in a tabular form.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusResultTableFormat implements TableFormat<SiriusResultElement> {
    public static final int COL_COUNT = 6;

    public int getColumnCount() {
        return COL_COUNT;
    }

    public String getColumnName(int column) {
        if (column == 0) return "Rank";
        else if (column == 1) return "Molecular Formula";
        else if (column == 2) return "Score";
        else if (column == 3) return "Isotope Score";
        else if (column == 4) return "Tree Score";
        else if (column == 5) return "Summary";

        throw new IllegalStateException();
    }

    public Object getColumnValue(SiriusResultElement result, int column) {

        return getValue(result,column);
    }

    public static Object getValue(SiriusResultElement result, int column) {
        if (column == 0) return result.getRank();
        else if (column == 1) return result.getMolecularFormula().toString();
        else if (column == 2) return Math.round(result.getScore() * 100d)/100d;
        else if (column == 3) return Math.round(result.getResult().getIsotopeScore() * 100d)/100d;
        else if (column == 4) return Math.round(result.getResult().getTreeScore() * 100d)/100d;
        else if (column == 5) return result.getFormulaAndIonText();

        throw new IllegalStateException();
    }
}

