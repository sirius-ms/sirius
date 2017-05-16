package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.sirius.gui.fingerid.FingerIdData;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.SiriusTableFormat;

/**
 * Created by fleisch on 15.05.17.
 */
public class CandidateTableFormat implements SiriusTableFormat<CompoundCandidate> {
    private static final int COL_COUNT = 7;


    @Override
    public int highlightColumn() {
        return -1; //todo enable highlighting
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

//        else if (column == col++) return "Best Hit";


        throw new IllegalStateException();
    }

    public Object getColumnValue(CompoundCandidate result, int column) {
        return getValue(result, column);
    }

    public static Object getValue(CompoundCandidate result, int column) {
        int col = 0;
        if (column == col++) return result.rank;
        if (column == col++) return result.getMolecularFormulaString();
        if (column == col++) return result.tanimotoScore;
        if (column == col++) return result.compound.name!= null ? result.compound.name  : "";
        if (column == col++) return result.score;
        if (column == col++) return result.compound.xlogP;
        if (column == col++) return result.compound.inchi.key;

        throw new IllegalStateException();
    }
}
