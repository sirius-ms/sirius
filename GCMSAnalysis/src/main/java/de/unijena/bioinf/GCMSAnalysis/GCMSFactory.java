package de.unijena.bioinf.GCMSAnalysis;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.math.MathUtils;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.GCMSFragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.MostRelevantPeaksFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoiseThresholdFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.GCMSMissingValueValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;


import java.util.Arrays;
import java.util.List;

public class GCMSFactory {

    private boolean useHalogens = false;
    private boolean useChlorine = false;
    private boolean usePFB = false;
    private boolean useTMS = false;
    private boolean useDerivates = false;
    //scoring
    private double heteroAverage = 0.5886335;
    private double heteroDev = 0.5550574;
    private double heteroNonC = 0.8;

    //Elements
    private Element dmsElement;
    private Element tmsElement;
    private Element pfbElement;

    private void setInitials(){
        if (useHalogens || useChlorine){
            heteroAverage = 0.5269789;
            heteroDev = 0.521254;
        }

        //introduce new "elements"
        PeriodicTable periodicTable = PeriodicTable.getInstance();
        periodicTable.addElement("Pfb", "Pfb", 181.007665, 1); //C7H2F5
        periodicTable.addElement("Tms", "Tms", 73.047352, 1); // C3H9Si
        periodicTable.addElement("Dms", "Dms", 58.023877, 2); //C2H6Si

        pfbElement = periodicTable.getByName("Pfb");
        tmsElement = periodicTable.getByName("Tms");
        dmsElement = periodicTable.getByName("Dms");

        //to extend TableSelection
        MolecularFormula.parse("").getTableSelection().extendElements(pfbElement, tmsElement, dmsElement);
    }

    public GCMSFragmentationPatternAnalysis getGCMSAnalysis(){
        setInitials();

        final PeakScorer gcmsOriginalPeakIsNoiseScorer = new PeakScorer() {
            @Called("gcms noise")

            @Override
            public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores) {
                // score peak intensity
                for (int i = 0; i < scores.length; i++) {
                    if (peaks.get(i).getRelativeIntensity()!=0) scores[i] += Math.log(100 * peaks.get(i).getRelativeIntensity() * 0.1); // 0-Model: pareto distribution of noise peaks
                    //todo but what has this to do with pareto?

                }
            }
        };

        final DecompositionScorer h2cDecompositionScorer = new DecompositionScorer() {
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

                if (tmsElement != null) tms = formula.numberOf(tmsElement);
                if (dmsElement != null) dms = formula.numberOf(dmsElement);
                if (pfbElement != null) pfb = formula.numberOf(pfbElement);

//                final int hetero = atomCount - carbon - hydrogen;// + 4 * pfb; //count pfb once in atomCount and 4 extra times (5 Flour atoms)
//                final int carbonRate = carbon;// + 3 * tms + 2 * dms + 7 * pfb;
                //final double ratio = (double)hetero / (carbonRate == 0 ? heteroNonC : (double)carbonRate);
                //todo in GCMSTool Peaks.scoreDecompositions without heteroNonC and the special elements stuff.
                final int hetero = atomCount- carbon - tms - dms - pfb;
                final int carbonRate = carbon;
                //todo different to heteroToCarbon rate, no Pfb.Tms... but instead H
                final double ratio = (double)carbonRate / ((double)hetero);

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

                if (tmsElement != null) tms = formula.numberOf(tmsElement);
                if (dmsElement != null) dms = formula.numberOf(dmsElement);
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
        //todo never scored in GCMSTool, change minimal mass?
//        analysis.getRootScorers().add(new MassDeviationVertexScorer(true));
//        analysis.getRootScorers().add(new ChemicalPriorScorer(h2cScorer, 0d, 11d));
        //lossScorer
        analysis.getLossScorers().add(new ChemicalPriorEdgeScorer(h2cScorer, 0d));
        analysis.getLossScorers().add(new TmsToDmsLossScorer());
        analysis.getLossScorers().add(new FractionOfParentLossScorer());
        analysis.getLossScorers().add(EICommonLossEdgeScorer.getGCMSCommonLossScorer(useChlorine, useHalogens, usePFB, useTMS));
        //fragmentScorer
        //analysis.getFragmentPeakScorers().add(new PeakIsNoiseScorer(4));
        analysis.getFragmentPeakScorers().add(gcmsOriginalPeakIsNoiseScorer);
        //analysis.getFragmentPeakScorers().add(new TreeSizeScorer(-1d));
        //post-processors
        //analysis.getPostProcessors().add(new MostRelevantPeaksFilter(5));
        //analysis.getPostProcessors().add(new NoiseThresholdFilter(0.01d));
        //analysis.getPostProcessors().add(new MostRelevantPeaksFilter(5));

        //set boolean variables
        analysis.setRemoveIsotopePeaks(true);
        analysis.setUseChlorine(useChlorine);
        analysis.setUseHalogens(useHalogens);
        analysis.setUsePFB(usePFB);
        analysis.setUseTMS(useTMS);
        analysis.setUseDerivates(useDerivates);

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

    public boolean isUseChlorine() {
        return useChlorine;
    }

    public void setUseChlorine(boolean useChlorine) {
        this.useChlorine = useChlorine;
    }

    public boolean isUseHalogens() {
        return useHalogens;
    }

    public void setUseHalogens(boolean useHalogens) {
        this.useHalogens = useHalogens;
    }

    public boolean isUsePFB() {
        return usePFB;
    }

    public void setUsePFB(boolean usePFB) {
        this.usePFB = usePFB;
    }

    public boolean isUseTMS() {
        return useTMS;
    }

    public boolean isUseDerivates() {
        return useDerivates;
    }

    public void setUseDerivates(boolean useDerivates) {
        this.useDerivates = useDerivates;
    }

    public void setUseTMS(boolean useTMS) {
        this.useTMS = useTMS;


    }

    private static <S, T extends S> T getByClassName(Class<T> klass, List<S> list) {
        for (S elem : list) if (elem.getClass().equals(klass)) return (T)elem;
        return null;
    }


}
