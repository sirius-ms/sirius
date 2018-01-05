package de.unijena.bioinf.fingerid.jjobs;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.fingerid.net.WebAPI;
import de.unijena.bioinf.jjobs.DependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.IdentificationResult;

public abstract class FingerprintDependentJJob<R> extends DependentJJob<R> implements IdentificationResult.AnnotationJJob<R> {
    protected IdentificationResult identificationResult;
    protected ProbabilityFingerprint fp;
    protected MolecularFormula formula;
    protected FTree resolvedTree;

    protected FingerprintDependentJJob(JobType type, IdentificationResult result, ProbabilityFingerprint fp) {
        super(type);
        this.identificationResult = result;
        this.fp = fp;
    }

    protected void initInput() {
        if (identificationResult == null || fp == null) {
            for (JJob j : requiredJobs) {
                if (j instanceof WebAPI.PredictionJJob) {
                    WebAPI.PredictionJJob job = ((WebAPI.PredictionJJob) j);
                    if (job.result != null && job.takeResult() != null) {
                        identificationResult = job.result;
                        fp = job.takeResult();
                        resolvedTree = job.ftree;
                        formula = job.ftree.getRoot().getFormula();

                        return;
                    }
                }
            }
            throw new IllegalArgumentException("No Input Data found. " + requiredJobs.toString());
        }
    }

    @Override
    public IdentificationResult getIdentificationResult() {
        return identificationResult;
    }
}
