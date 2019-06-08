package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.Set;

public class CSVOutputWriter {

    public static void writeHits(Writer w, Iterable<IdentificationResult> results) throws IOException {
        final StringBuilder builder = new StringBuilder();
        builder.append("formula\tadduct\trank\trankingScore\ttreeScore\tisoScore");

        //searching for annotated score types
        Set<Class<? extends ResultScore>> scores = new LinkedHashSet<>();
        for (IdentificationResult result : results) {
            result.forEachAnnotation((k, v) -> {
                if (ResultScore.class.isAssignableFrom(k) && scores.add((Class<? extends ResultScore>) v.getClass()))
                    builder.append("\t").append(((ResultScore) v).name());
            });
        }

        builder.append("\texplainedPeaks\texplainedIntensity\n");


        w.write(builder.toString());
        for (IdentificationResult r : results) {
            PrecursorIonType ion = r.getPrecursorIonType();
            w.write(r.getMolecularFormula().toString());
            w.write('\t');
            w.write(ion != null ? ion.toString() : "?");
            w.write('\t');
            w.write(String.valueOf(r.getRank()));
            w.write('\t');
            w.write(String.valueOf(r.getRankingScore()));
            w.write('\t');
            w.write(String.valueOf(r.getTreeScore()));
            w.write('\t');
            w.write(String.valueOf(r.getIsotopeScore()));
            w.write('\t');
            //writing different Scores to file e.g. sirius and zodiac
            for (Class<? extends ResultScore> score : scores) {
                w.write(String.valueOf(r.getScore(score)));
                w.write('\t');
            }
            w.write(r.getRawTree() == null ? "" : String.valueOf(r.getRawTree().numberOfVertices()));
            w.write('\t');
            w.write(String.valueOf(r.getExplainedIntensityRatio()));
            w.write('\n');
        }
    }

}
