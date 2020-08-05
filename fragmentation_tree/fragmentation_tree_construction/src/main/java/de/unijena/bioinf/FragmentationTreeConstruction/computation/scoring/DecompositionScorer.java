
package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

/**
 * @author Kai Dührkop
 */
public interface DecompositionScorer<S> extends Parameterized {

    S prepare(ProcessedInput input);

    double score(MolecularFormula formula, Ionization ion, ProcessedPeak peak, ProcessedInput input, S precomputed);

}
