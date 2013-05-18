package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

import java.util.Collections;
import java.util.List;

public class ProcessedInput {

    private final Ms2Experiment experiment;
    private final List<ProcessedPeak> mergedPeaks;
    private final ProcessedPeak parentPeak;
    private final List<ScoredMolecularFormula> parentMassDecompositions;
    private final double[] peakScores;
    private final double[][] peakPairScores;

    public ProcessedInput(Ms2Experiment experiment,
                          List<ProcessedPeak> mergedPeaks, ProcessedPeak parentPeak, List<ScoredMolecularFormula> parentMassDecompositions) {
        this(experiment, mergedPeaks, parentPeak, parentMassDecompositions, null, null);

    }

    public ProcessedInput(Ms2Experiment experiment,
                          List<ProcessedPeak> mergedPeaks, ProcessedPeak parentPeak, List<ScoredMolecularFormula> parentMassDecompositions,
                          double[] peakScores, double[][] peakPairScores) {
        this.experiment = experiment;
        this.mergedPeaks = mergedPeaks;
        this.parentPeak = parentPeak;
        this.parentMassDecompositions = parentMassDecompositions;
        this.peakPairScores = peakPairScores;
        this.peakScores = peakScores;
    }

    public Ms2Experiment getExperimentInformation() {
        return experiment;
    }

    public List<ProcessedPeak> getMergedPeaks() {
        return mergedPeaks == null ? null : Collections.unmodifiableList(mergedPeaks);
    }

    public ProcessedPeak getParentPeak() {
        return parentPeak;
    }

    public List<ScoredMolecularFormula> getParentMassDecompositions() {
        return parentMassDecompositions == null ? null : Collections.unmodifiableList(parentMassDecompositions);
    }

    public double[][] getPeakPairScores() {
        return peakPairScores;
    }

    public double[] getPeakScores() {
        return peakScores;
    }
}
