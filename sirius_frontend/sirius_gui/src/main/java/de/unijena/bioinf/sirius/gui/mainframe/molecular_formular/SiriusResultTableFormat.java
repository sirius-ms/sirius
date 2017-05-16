package de.unijena.bioinf.sirius.gui.mainframe.molecular_formular;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 25.01.17.
 */

import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.SiriusTableFormat;

/**
 * Display issues in a tabular form.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusResultTableFormat implements SiriusTableFormat<SiriusResultElement> {
    private static final int COL_COUNT = 5;


    @Override
    public int highlightColumn() {
        return 5;
    }

    @Override
    public int getColumnCount() {
        return COL_COUNT;
    }

    public String getColumnName(int column) {
        if (column == 0) return "Rank";
        else if (column == 1) return "Molecular Formula";
        else if (column == 2) return "Score";
        else if (column == 3) return "Isotope Score";
        else if (column == 4) return "Tree Score";
        else if (column == 5) return "Best Hit";


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
        else if (column == 5) return result.isBestHit();

        throw new IllegalStateException();
    }


}

