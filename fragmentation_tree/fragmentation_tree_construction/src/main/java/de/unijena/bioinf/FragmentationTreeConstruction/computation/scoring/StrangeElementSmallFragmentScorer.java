
package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

/**
 * Add additional score to small fragments to compensate the StrangeElementRootScorer
 * The idea behind it is, that it is very unlikely to find a small fragment with a strange element by random,
 * as the allowed mass deviation in small fragments is much smaller than in huge fragments
 */
public class StrangeElementSmallFragmentScorer implements DecompositionScorer {

    private double scoreCompensation, maximalMass;

    public StrangeElementSmallFragmentScorer() {
        this(Math.log(1.5), 100d);
    }

    public StrangeElementSmallFragmentScorer(double scoreCompensation, double maximalMass) {
        this.scoreCompensation = scoreCompensation;
        this.maximalMass = maximalMass;
    }

    @Override
    public Object prepare(ProcessedInput input) {
        return null;
    }

    @Override
    public double score(MolecularFormula formula, Ionization ion, ProcessedPeak peak, ProcessedInput input, Object precomputed) {
        if (formula.getMass() > maximalMass || formula.isCHNO()) return 0d;
        else return scoreCompensation;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        scoreCompensation = document.getDoubleFromDictionary(dictionary, "score");
        maximalMass = document.getDoubleFromDictionary(dictionary, "maximalMass");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "score", scoreCompensation);
        document.addToDictionary(dictionary, "maximalMass", maximalMass);
    }
}
