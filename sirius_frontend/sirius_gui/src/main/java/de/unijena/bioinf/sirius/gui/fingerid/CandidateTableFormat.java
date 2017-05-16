package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.sirius.gui.fingerid.FingerIdData;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.SiriusTableFormat;

/**
 * Created by fleisch on 15.05.17.
 */
public class CandidateTableFormat implements SiriusTableFormat<CompoundCandidate> {


    private static final int COL_COUNT = 5;

    @Override
    public int primaryColumn() {
        return 0; //todo
    }

    public int getColumnCount() {
        return COL_COUNT;
    }

    public String getColumnName(int column) {
        int col = 0;
        if (column == col++) return "Rank";
        else if (column == col++) return "Name";
        else if (column == col++) return "Inchi";
        else if (column == col++) return "Similarity";
        else if (column == col++) return "FingerID Score";
        else if (column == col++) return "XLogP";
//        else if (column == col++) return "Formula Score";
        else if (column == col++) return "Molecular Formula";
//        else if (column == col++) return "Best Hit";


        throw new IllegalStateException();
    }

    public Object getColumnValue(CompoundCandidate result, int column) {
        return getValue(result,column);
    }

    public static Object getValue(CompoundCandidate result, int column) {
        int col = 0;
        if (column == col++) return false;
        else if (column == col++) return result.rank;
        else if (column == col++) return result.compound.name;
        else if (column == col++) return result.compound.inchi.in2D;
        else if (column == col++) return result.tanimotoScore;
        else if (column == col++) return result.score;
        else if (column == col++) return result.compound.xlogP;
//        else if (column == col++) return result.;
        else if (column == col++) return result.getMolecularFormulaString();
//        else if (column == col++) return result.;

        throw new IllegalStateException();
    }
}
