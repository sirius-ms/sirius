package de.unijena.bioinf.ms.rest.model.canopus;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ms.rest.model.Job;
import de.unijena.bioinf.ms.rest.model.JobState;
import de.unijena.bioinf.ms.rest.model.JobTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CanopusJob extends Job<CanopusJobOutput> {
    protected String formula;
    protected byte[] fingerprint; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES
    protected byte[] compoundClasses; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES
    protected Long predictors;

    public CanopusJob() {
        this(null, null, null);
    }

    public CanopusJob(String workerPrefix, String ip, String cid, @NotNull CanopusJobInput input) {
        this(workerPrefix, JobState.SUBMITTED);
        setIp(ip);
        setCid(cid);
        setFingerprint(input.fingerprint);
        setFormula(input.formula);
        setPredictors(input.predictor.toBits());
    }

    //worker Constructor
    public CanopusJob(String workerPrefix, long lockedByWorker) {
        this(workerPrefix, null);
        setLockedByWorker(lockedByWorker);
    }

    public CanopusJob(String workerPrefix, JobState state) {
        this(workerPrefix, null, state);
    }

    public CanopusJob(String workerPrefix, Long jobId, JobState state) {
        super(workerPrefix, jobId, state, JobTable.JOBS_CANOPUS);
    }

    public byte[] getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(byte[] fingerprint) {
        this.fingerprint = fingerprint;
    }

    public byte[] getCompoundClasses() {
        return compoundClasses;
    }

    public void setCompoundClasses(byte[] compoundClasses) {
        this.compoundClasses = compoundClasses;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public MolecularFormula getMolecularFormula() {
        return MolecularFormula.parseOrThrow(getFormula());
    }

    @Override
    @Nullable
    public CanopusJobOutput extractOutput() {
        return compoundClasses != null ? new CanopusJobOutput(compoundClasses) : null;
    }

    public Long getPredictors() {
        return predictors;
    }

    public void setPredictors(Long predictors) {
        this.predictors = predictors;
    }

    /*
    @Override
    public void setOutput(CanopusJobOutput output) {
        setCompoundClasses(output.compoundClasses);
    }

    @Override
    public CanopusJobInput asInput() {
        return new CanopusJobInput(fingerprint);
    }

    @Override
    public void setIntput(CanopusJobInput input) {
        setFingerprint(input.fingerprint);
    }*/
}

