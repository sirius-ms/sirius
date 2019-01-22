package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class CSVOutputWriter {

    public static void writeHits(Writer w, List<IdentificationResult> results) throws IOException {
        w.write("formula\tadduct\trank\tscore\ttreeScore\tisoScore\texplainedPeaks\texplainedIntensity\n");
        for (IdentificationResult r : results) {
            PrecursorIonType ion = r.getPrecursorIonType();
            w.write(r.getMolecularFormula().toString());
            w.write('\t');
            w.write(ion != null ? ion.toString() : "?");
            w.write('\t');
            w.write(String.valueOf(r.getRank()));
            w.write('\t');
            w.write(String.valueOf(r.getScore()));
            w.write('\t');
            w.write(String.valueOf(r.getTreeScore()));
            w.write('\t');
            w.write(String.valueOf(r.getIsotopeScore()));
            w.write('\t');
            final TreeScoring scoring = r.getRawTree() == null ? null : r.getRawTree().getAnnotationOrNull(TreeScoring.class);
            w.write(r.getRawTree() == null ? "" : String.valueOf(r.getRawTree().numberOfVertices()));
            w.write('\t');
            w.write(scoring == null ? "" : String.valueOf(scoring.getExplainedIntensity()));
            w.write('\n');
        }
    }

}
