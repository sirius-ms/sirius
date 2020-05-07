package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.jjobs.BasicDependentMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.annotations.DataAnnotation;

public abstract class FingerprintDependentJJob<R extends DataAnnotation> extends BasicDependentMasterJJob<R> {
    protected ProbabilityFingerprint fp;
    protected MolecularFormula formula;
    protected FTree ftree;

    public FingerprintDependentJJob(JobType type, ProbabilityFingerprint fp, MolecularFormula formula, FTree ftree) {
        super(type);
        this.fp = fp;
        this.formula = formula;
        this.ftree = ftree;
    }

    public FingerprintDependentJJob<R> setFingerprint(ProbabilityFingerprint fp) {
        notSubmittedOrThrow();
        this.fp = fp;
        return this;
    }

    public FingerprintDependentJJob<R> setFormula(MolecularFormula formula) {
        notSubmittedOrThrow();
        this.formula = formula;
        return this;
    }

    public FingerprintDependentJJob<R> setFtree(FTree ftree) {
        notSubmittedOrThrow();
        this.ftree = ftree;
        return this;
    }

    @Override
    public synchronized void handleFinishedRequiredJob(JJob required) {
        if (fp == null) {
            if (required instanceof FingerprintPredictionJJob) {
                FingerprintPredictionJJob job = ((FingerprintPredictionJJob) required);
                if (job.input.identificationResult != null && job.result() != null) {
                    fp = job.result().fingerprint;
                    ftree = job.input.ftree;
                    formula = job.input.ftree.getRoot().getFormula();
                }
            }
        }
    }

    /*protected void checkInput() {
        if (fp == null || ftree == null || formula == null)
            throw new IllegalArgumentException("No Input Data found in: " + LOG().getName());
    }*/
}
