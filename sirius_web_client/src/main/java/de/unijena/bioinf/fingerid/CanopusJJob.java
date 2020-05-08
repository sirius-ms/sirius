package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.canopus.Canopus;
import de.unijena.bioinf.canopus.CanopusResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CanopusJJob extends FingerprintDependentJJob<CanopusResult> {

    private final Canopus canopus;

    public CanopusJJob(@NotNull Canopus canopus) {
        this(canopus, null, null);
    }

    public CanopusJJob(@NotNull Canopus canopus, @Nullable ProbabilityFingerprint fp, @Nullable MolecularFormula formula) {
        super(JobType.CPU, fp, formula, null); //todo what do we need here??
        this.canopus = canopus;
    }


    @Override
    protected CanopusResult compute() throws Exception {
        progressInfo("Predict compound categories for " + formula + ": \nid\tname\tprobability");
        final ProbabilityFingerprint fingerprint = canopus.predictClassificationFingerprint(formula, fp);
        for (FPIter category : fingerprint.iterator()) {
            if (category.getProbability() >= 0.333) {
                ClassyfireProperty prop = ((ClassyfireProperty) category.getMolecularProperty());
                progressInfo(prop.getChemontIdentifier() + "\t" + prop.getName() + "\t" + ((int) Math.round(100d * category.getProbability())) + " %");
            }
        }

        return new CanopusResult(fingerprint);
    }
}