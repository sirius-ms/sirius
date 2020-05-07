package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;

/**
 * Created by martin on 08.08.18.
 */
public class ConfidenceResult implements ResultAnnotation {
    public static final ConfidenceResult NaN = new ConfidenceResult(Double.NEGATIVE_INFINITY, null);

    // bio confidence
    // pubchem confidence
    public final ConfidenceScore score;
    public Scored<FingerprintCandidate> top_hit;


    public ConfidenceResult(ConfidenceScore score) {
        this.score = score;
    }

    public ConfidenceResult(double confidence, Scored<FingerprintCandidate> top_hit){
        //this is just to supress the warning
        this.score = Double.isNaN(confidence) ? FormulaScore.NA(ConfidenceScore.class) : new ConfidenceScore(confidence);
        this.top_hit=top_hit;
    }
}
