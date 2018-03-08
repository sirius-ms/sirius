package de.unijena.bioinf.sirius.gui.mainframe.molecular_formular;


import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.SiriusTableFormat;
import de.unijena.bioinf.sirius.gui.table.list_stats.ListStats;

/**
 * Display issues in a tabular form.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusResultTableFormat extends SiriusTableFormat<SiriusResultElement> {
    private static final int COL_COUNT = 5;

    protected SiriusResultTableFormat(ListStats stats) {
        super(stats);
    }


    @Override
    public int highlightColumnIndex() {
        return COL_COUNT;
    }

    @Override
    protected boolean isBest(SiriusResultElement element) {
        return stats.getMax() <= element.getScore();
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
        else if (column == 5) return "Best";


        throw new IllegalStateException();
    }

    public Object getColumnValue(SiriusResultElement result, int column) {
        if (column == 0) return result.getRank();
        else if (column == 1) return result.getFormulaAndIonText();
        else if (column == 2) return result.getScore();
        else if (column == 3) return result.getResult().getIsotopeScore();
        else if (column == 4) return result.getResult().getTreeScore();
        else if (column == 5) return isBest(result);

        throw new IllegalStateException();
    }
}

