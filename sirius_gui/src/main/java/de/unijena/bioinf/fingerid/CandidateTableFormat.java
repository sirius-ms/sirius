package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.sirius.gui.table.SiriusTableFormat;
import de.unijena.bioinf.sirius.gui.table.list_stats.ListStats;

/**
 * Created by fleisch on 15.05.17.
 */
public class CandidateTableFormat extends SiriusTableFormat<CompoundCandidate> {
    protected CandidateTableFormat(ListStats stats) {
        super(stats);
    }

    protected static String[] columns = new String[]{
            "Rank",
            "Molecular Formula",
//            "Formula Score",
            "Adduct",
            "Similarity",
            "Name",
            "CSI:FingerID Score",
            "XLogP",
            "InChIKey",
            "SMILES",
            "Best"
    };

    @Override
    public int highlightColumnIndex() {
        return columns.length - 1;
    }

    @Override
    protected boolean isBest(CompoundCandidate element) {
        return stats.getMax() <= element.getScore();
    }

    public int getColumnCount() {
        return highlightColumnIndex();
    }

    public String getColumnName(int column) {
        return columns[column];
    }

    public Object getColumnValue(CompoundCandidate result, int column) {
        int col = 0;
        if (column == col++) return result.rank;
        if (column == col++) return result.getMolecularFormula();
        if (column == col++) return result.adduct;
        if (column == col++) return result.getTanimotoScore();
        if (column == col++) return result.compound.name != null ? result.compound.name : "";
        if (column == col++) return result.getScore();
        if (column == col++) return result.compound.xlogP;
        if (column == col++) return result.compound.inchi.key;
        if (column == col++) return result.compound.getSmiles();
        if (column == col) return isBest(result);

        throw new IllegalStateException();
    }
}
