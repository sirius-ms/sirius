package de.unijena.bioinf.fingerid.jjobs;

import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.canopus.Canopus;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.sirius.IdentificationResult;

public class CanopusJJob extends FingerprintDependentJJob<CanopusResult> {

    private final Canopus canopus;

    public CanopusJJob(Canopus canopus) {
        this(canopus, null, null);
    }

    public CanopusJJob(Canopus canopus, IdentificationResult result, ProbabilityFingerprint fp) {
        super(JobType.CPU, result, fp); //todo what do we need here??
        this.canopus = canopus;
    }


    @Override
    protected CanopusResult compute() throws Exception {
        initInput();

        progressInfo("Predict compound categories for " + identificationResult.getMolecularFormula() + ": \nid\tname\tprobability");
        final ProbabilityFingerprint fingerprint = canopus.predictClassificationFingerprint(identificationResult.getMolecularFormula(), fp);
        for (FPIter category : fingerprint.iterator()) {
            if (category.getProbability() >= 0.333) {
                ClassyfireProperty prop = ((ClassyfireProperty) category.getMolecularProperty());
                progressInfo(prop.getChemontIdentifier() + "\t" + prop.getName() + "\t" + ((int) Math.round(100d * category.getProbability())) + " %");
            }
        }

        return new CanopusResult(fingerprint);
    }
}