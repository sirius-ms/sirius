package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

/**
 * Created by ge28quv on 10/07/17.
 */
public class IOKRScoring implements FingerblastScoring {

    public IOKRScoring(double[][] matrix){

    }


    @Override
    public void prepare(ProbabilityFingerprint fingerprint) {

    }

    @Override
    public double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry) {
        return 0;
    }

    @Override
    public double getThreshold() {
        return 0;
    }

    @Override
    public void setThreshold(double threshold) {

    }

    @Override
    public double getMinSamples() {
        return 0;
    }

    @Override
    public void setMinSamples(double minSamples) {

    }
}
