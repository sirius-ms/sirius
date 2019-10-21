package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

/**
 * Gives some score bonus to formulas consist only of C,H,O, because it seems that they are very frequent and
 * our model wrongly favours CHNO compounds instead, because they will result in more possibilities and lower mass
 * errors.
 */
public abstract class CarbohydrogenScorer {

    public static void main(String[] args) {
        for (double p : new double[]{0.01,0.015, 0.02, 0.05,0.1,0.15, 0.2,0.5,1.0}) {
            System.out.println(p + "\t" + (new ParetoDistribution.EstimateByMedian(0.02).extimateByMedian(0.5).getCumulativeProbability(p))*2);
        }
    }

    @Called("CarbohydrogenCompound")
    public static class CarbohydrogenRootScorer implements DecompositionScorer<Object> {

        @Override
        public Object prepare(ProcessedInput input) {
            return null;
        }

        @Override
        public double score(MolecularFormula formula, Ionization ion, ProcessedPeak peak, ProcessedInput input, Object precomputed) {
            return formula.isCHO() ? 5 : 0d;
        }

        @Override
        public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }
    }

    @Called("CarbohydrogenFragment")
    public static class CarbohydrogenFragmentScorer implements DecompositionScorer<ParetoDistribution> {

        @Override
        public ParetoDistribution prepare(ProcessedInput input) {
            return new ParetoDistribution.EstimateByMedian(0.02).extimateByMedian(0.5);
        }

        @Override
        public double score(MolecularFormula formula, Ionization ion, ProcessedPeak peak, ProcessedInput input, ParetoDistribution precomputed) {
            return peak.getRelativeIntensity()>0.02 && formula.isCHO() ? 2*precomputed.getCumulativeProbability(peak.getRelativeIntensity()) : 0d;
        }

        @Override
        public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }
    }
    @Called("CarbohydrogenLoss")
    public static class CarbohydrogenLossScorer implements LossScorer<Object> {

        @Override
        public Object prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
            return null;
        }

        @Override
        public double score(Loss loss, ProcessedInput input, Object precomputed) {
            if (loss.getFormula().isCHO() && loss.getFormula().getMass() > Math.exp(LossSizeScorer.LEARNED_MEAN)) {
                // reduce loss size penalty by 50%
                return -Math.min(0, new LossSizeScorer().score(loss.getFormula()))*0.5;
            } else return 0d;
        }

        @Override
        public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }
    }

}
