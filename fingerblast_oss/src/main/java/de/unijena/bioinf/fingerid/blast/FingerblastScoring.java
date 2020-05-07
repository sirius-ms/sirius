package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

public interface FingerblastScoring {

    void prepare(ProbabilityFingerprint fingerprint);

    double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry);

    // TODO: abstract class?

    double getThreshold();

    void setThreshold(double threshold);

    double getMinSamples();
    void setMinSamples(double minSamples);

}
