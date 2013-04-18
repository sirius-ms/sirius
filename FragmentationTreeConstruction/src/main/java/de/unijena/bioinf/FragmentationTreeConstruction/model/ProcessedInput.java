package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;

import java.util.Collections;
import java.util.List;

public class ProcessedInput {

    private final MSExperimentInformation experimentInformation;
    private final MSInput originalInput;
    private final List<ProcessedPeak> mergedPeaks;
    private final ProcessedPeak parentPeak;
    private final List<ScoredMolecularFormula> parentMassDecompositions;

    public ProcessedInput(MSExperimentInformation experimentInformation, MSInput originalInput,
                          List<ProcessedPeak> mergedPeaks, ProcessedPeak parentPeak, List<ScoredMolecularFormula> parentMassDecompositions) {
        this.experimentInformation = experimentInformation;
        this.originalInput = originalInput;
        this.mergedPeaks = mergedPeaks;
        this.parentPeak = parentPeak;
        this.parentMassDecompositions = parentMassDecompositions;
    }

    public MSExperimentInformation getExperimentInformation() {
        return experimentInformation;
    }

    public MSInput getOriginalInput() {
        return originalInput;
    }

    public List<ProcessedPeak> getMergedPeaks() {
        return Collections.unmodifiableList(mergedPeaks);
    }

    public ProcessedPeak getParentPeak() {
        return parentPeak;
    }

    public List<ScoredMolecularFormula> getParentMassDecompositions() {
        return Collections.unmodifiableList(parentMassDecompositions);
    }
}
