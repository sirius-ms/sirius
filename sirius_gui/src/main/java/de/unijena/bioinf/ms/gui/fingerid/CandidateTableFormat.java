package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.ms.gui.table.SiriusTableFormat;
import de.unijena.bioinf.ms.gui.table.list_stats.ListStats;

/**
 * Created by fleisch on 15.05.17.
 */
public class CandidateTableFormat extends SiriusTableFormat<FingerprintCandidatePropertyChangeSupport> {
    protected CandidateTableFormat(ListStats stats) {
        super(stats);
    }

    protected static String[] columns = new String[]{
            "Rank",
            "Name",
            "SMILES",
            "Molecular Formula",
            "Adduct",
            "CSI:FingerID Score",
            "Similarity",
            "#PubMed IDs",
            "XLogP",
            "InChIKey",
            "Best"
    };

    @Override
    public int highlightColumnIndex() {
        return columns.length - 1;
    }

    @Override
    protected boolean isBest(FingerprintCandidatePropertyChangeSupport element) {
        return stats.getMax() <= element.getScore();
    }

    public int getColumnCount() {
        return highlightColumnIndex();
    }

    public String getColumnName(int column) {
        return columns[column];
    }

    public Object getColumnValue(FingerprintCandidatePropertyChangeSupport result, int column) {
        int col = 0;
        if (column == col++) return result.rank;
        if (column == col++) return result.compound.getName() != null ? result.compound.getName() : "";
        if (column == col++) return result.compound.getSmiles();
        if (column == col++) return result.getMolecularFormula();
        if (column == col++) return result.adduct;
        if (column == col++) return result.getScore();
        if (column == col++) return result.getTanimotoScore();
        if (column == col++) return result.getFingerprintCandidate().getPubmedIDs();
        if (column == col++) return result.compound.getXlogp();
        if (column == col++) return result.compound.getInchi().key;
        if (column == col) return isBest(result);

        throw new IllegalStateException();
    }
}
