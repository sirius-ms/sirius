package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.sirius.gui.table.SiriusTableFormat;
import de.unijena.bioinf.sirius.gui.table.list_stats.ListStats;

/**
 * Created by fleisch on 15.05.17.
 */
public class CandidateTableFormat extends SiriusTableFormat<CompoundCandidate> {
    private static final int COL_COUNT = 7;

    protected CandidateTableFormat(ListStats stats) {
        super(stats);
    }


    @Override
    public int highlightColumnIndex() {
        return COL_COUNT;
    }

    @Override
    protected boolean isBest(CompoundCandidate element) {
        return stats.getMax() <= element.getScore();
    }

    public int getColumnCount() {
        return COL_COUNT;
    }

    public String getColumnName(int column) {
        int col = 0;
        if (column == col++) return "Rank";
        if (column == col++) return "Molecular Formula";
//      if (column == col++) return "Formula Score";
        if (column == col++) return "Similarity";
        if (column == col++) return "Name";
        if (column == col++) return "FingerID Score";
        if (column == col++) return "XLogP";
        if (column == col++) return "InChi";
        if (column == col++) return "Best";

//        else if (column == col++) return "Best Hit";


        throw new IllegalStateException();
    }

    public Object getColumnValue(CompoundCandidate result, int column) {
        int col = 0;
        if (column == col++) return result.rank;
        if (column == col++) return result.getMolecularFormula();
        if (column == col++) return result.getTanimotoScore();
        if (column == col++) return result.compound.name != null ? result.compound.name : "";
        if (column == col++) return result.getScore();
        if (column == col++) return result.compound.xlogP;
        if (column == col++) return result.compound.inchi.key;
        if (column == col++) return isBest(result);

        throw new IllegalStateException();
    }
}
