package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.Inspectable;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

/**
 * @author Kai Dührkop
 */
public interface DecompositionScorer<S> {

    public S prepare(ProcessedInput input);

    public double score(MolecularFormula formula, ProcessedPeak peak, ProcessedInput input, S precomputed);

}
