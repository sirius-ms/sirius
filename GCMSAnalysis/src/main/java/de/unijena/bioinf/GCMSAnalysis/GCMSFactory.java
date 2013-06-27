package de.unijena.bioinf.GCMSAnalysis;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.math.MathUtils;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.GCMSFragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoiseThresholdFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.GCMSMissingValueValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;


import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Marcus
 * Date: 11.06.13
 * Time: 12:11
 * To change this template use File | Settings | File Templates.
 */
public class GCMSFactory {

    double heteroAverage = 0.5886335;
    double heteroDev = 0.5550574;
    double heteroNonC = 0.8;

    public GCMSFragmentationPatternAnalysis getGCMSAnalysis(){

        final PeakScorer gcmsOriginalPeakIsNoiseScorer = new PeakScorer() {
            @Called("gcms noise")

            @Override
            public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores) {
                // score peak intensity
                for (int i = 0; i < scores.length; i++) {
                    if (peaks.get(i).getRelativeIntensity()!=0) scores[i] += Math.log(peaks.get(i).getRelativeIntensity() * 0.1); // 0-Model: pareto distribution of noise peaks
                    //todo but is this pareto?

                }
            }
        };

        final DecompositionScorer h2cDecompositionScorer = new DecompositionScorer() {
            @Called("h2c decomp")
            @Override
            public Object prepare(ProcessedInput input) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public double score(MolecularFormula formula, ProcessedPeak peak, ProcessedInput input, Object precomputed) {
                //todo Kai uses improved Hetero2CarbonScorer? --> heteroWithoutOxygenToCarbonRatio()
                final int atomCount = formula.atomCount();
                final int carbon = formula.numberOfCarbons();
                final int hydrogen = formula.numberOfHydrogens();
                int tms = 0;
                int dms = 0;
                int pfb = 0;
                final PeriodicTable periodicTable = formula.getTableSelection().getPeriodicTable();
                final Element tmsElement = periodicTable.getByName("Tms");
                if (tmsElement != null) tms = formula.numberOf(tmsElement);
                final Element dmsElement = periodicTable.getByName("Dms");
                if (dmsElement != null) dms = formula.numberOf(dmsElement);
                final Element pfbElement = periodicTable.getByName("Pfb");
                if (pfbElement != null) pfb = formula.numberOf(pfbElement);

                final int hetero = atomCount - carbon - hydrogen + 4 * pfb; //count pfb once in atomCount and 4 extra times (5 Flour atoms)
                final int carbonRate = carbon + 3 * tms + 2 * dms + 7 * pfb;
                //final double ratio = (double)hetero / (carbonRate == 0 ? heteroNonC : (double)carbonRate);
                //todo in GCMSTool Peaks.scoreDecompositions without heteroNonC and the special elements stuff.
                final double ratio = (double)hetero / ((double)carbonRate);
                if (formula.toString().equals("C6H6O2")) System.out.println("h2cDecompscoreC6H6O2 "+(ratio>3 ? Math.log(0.00005) : 0));
                return (ratio>3 ? Math.log(0.00005) : 0);
            }
        };
        final MolecularFormulaScorer h2cScorer = new MolecularFormulaScorer() {
            @Called("h2c MFScorer")
            //Parameter
            final double heteroNonC = 0.8;
            //
            @Override
            public double score(MolecularFormula formula) {
                //todo Kai uses improved Hetero2CarbonScorer? --> heteroWithoutOxygenToCarbonRatio()

                final int atomCount = formula.atomCount();
                final int carbon = formula.numberOfCarbons();
                final int hydrogen = formula.numberOfHydrogens();
                int tms = 0;
                int dms = 0;
                int pfb = 0;
                final PeriodicTable periodicTable = formula.getTableSelection().getPeriodicTable();
                final Element tmsElement = periodicTable.getByName("Tms");
                if (tmsElement != null) tms = formula.numberOf(tmsElement);
                final Element dmsElement = periodicTable.getByName("Dms");
                if (dmsElement != null) dms = formula.numberOf(dmsElement);
                final Element pfbElement = periodicTable.getByName("Pfb");
                if (pfbElement != null) pfb = formula.numberOf(pfbElement);

                final int hetero = atomCount - carbon - hydrogen + 4 * pfb; //count pfb once in atomCount and 4 extra times (5 Flour atoms)
                final int carbonRate = carbon + 3 * tms + 2 * dms + 7 * pfb;
                final double ratio = (double)hetero / (carbonRate == 0 ? heteroNonC : (double)carbonRate);
                return Math.log(MathUtils.pdf(ratio, heteroAverage, heteroDev));
            }
        };

        final GCMSFragmentationPatternAnalysis analysis = new GCMSFragmentationPatternAnalysis();
        analysis.setInitial();
        //no pre-processors
        //....
        //validator
        analysis.getInputValidators().add(new GCMSMissingValueValidator());
        //decomp scorer vertices
        analysis.getDecompositionScorers().add(new MassDeviationVertexScorer(false)); //todo compare to gcmstool. same?
        analysis.getDecompositionScorers().add(h2cDecompositionScorer);
        //scorer root
        analysis.getRootScorers().add(new MassDeviationVertexScorer(true));
        analysis.getRootScorers().add(new ChemicalPriorScorer(h2cScorer, 0d, 0d)); //todo nicht extra in GCMSTool gescored ? , change minimal mass?
        //lossScorer
        analysis.getLossScorers().add(new ChemicalPriorEdgeScorer(h2cScorer, 0d));
        analysis.getLossScorers().add(new TmsToDmsLossScorer());
        analysis.getLossScorers().add(new FractionOfParentLossScorer());
        analysis.getLossScorers().add(EICommonLossEdgeScorer.getDefaultGCMSCommonLossScorer());
        //fragmentScorer
        //analysis.getFragmentPeakScorers().add(new PeakIsNoiseScorer(4));
        analysis.getFragmentPeakScorers().add(gcmsOriginalPeakIsNoiseScorer);
        //analysis.getFragmentPeakScorers().add(new TreeSizeScorer(-1d));
        //post-processors
        //analysis.getPostProcessors().add(new MostRelevantPeaksFilter(5));
        analysis.getPostProcessors().add(new NoiseThresholdFilter(1d));
        //analysis.getPostProcessors().add(new MostRelevantPeaksFilter(5));

        //set boolean variables
        analysis.setRemoveIsotopePeaks(true);
        analysis.setUseChlorine(false);
        analysis.setUseHalogens(false);

        return analysis;
    }


    public double getHeteroAverage() {
        return heteroAverage;
    }

    public void setHeteroAverage(double heteroAverage) {
        this.heteroAverage = heteroAverage;
    }

    public double getHeteroDev() {
        return heteroDev;
    }

    public void setHeteroDev(double heteroDev) {
        this.heteroDev = heteroDev;
    }


    private static <S, T extends S> T getByClassName(Class<T> klass, List<S> list) {
        for (S elem : list) if (elem.getClass().equals(klass)) return (T)elem;
        return null;
    }


}
