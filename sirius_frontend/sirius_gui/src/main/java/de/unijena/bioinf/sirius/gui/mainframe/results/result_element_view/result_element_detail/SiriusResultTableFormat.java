package de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.result_element_detail;
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
    public static final int COL_COUNT = 5;

    public int getColumnCount() {
        return COL_COUNT;
    }

    public String getColumnName(int column) {
        if (column == 0) return "Rank";
        else if (column == 1) return "Molecular Formula";
        else if (column == 2) return "Score";
        else if (column == 3) return "Isotope Score";
        else if (column == 4) return "Tree Score";

        throw new IllegalStateException();
    }

    public Object getColumnValue(SiriusResultElement result, int column) {

        return getValue(result,column);
    }

    public static Object getValue(SiriusResultElement result, int column) {
        if (column == 0) return result.getRank();
        else if (column == 1) return result.getFormulaAndIonText();
        else if (column == 2) return result.getScore();
        else if (column == 3) return result.getResult().getIsotopeScore();
        else if (column == 4) return result.getResult().getTreeScore();

        throw new IllegalStateException();
    }
}

