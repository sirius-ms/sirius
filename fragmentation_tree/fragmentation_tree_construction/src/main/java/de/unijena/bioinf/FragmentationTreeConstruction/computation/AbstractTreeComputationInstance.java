package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.MasterJJob;

import java.util.List;

public abstract class AbstractTreeComputationInstance extends MasterJJob<AbstractTreeComputationInstance.FinalResult> {


    public abstract ProcessedInput validateInput();

    public abstract ProcessedInput precompute();


    public final static class FinalResult {
    protected final boolean canceledDueToLowScore;
    protected final List<FTree> results;

    public FinalResult(List<FTree> results) {
        this.canceledDueToLowScore = false;
        this.results = results;
    }

    public FinalResult() {
        this.canceledDueToLowScore = true;
        this.results = null;
    }

    public List<FTree> getResults() {
        return results;
    }
}

    public AbstractTreeComputationInstance() {
        super(JJob.JobType.CPU);
    }
}
