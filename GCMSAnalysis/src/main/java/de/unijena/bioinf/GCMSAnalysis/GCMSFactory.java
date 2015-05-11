/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.GCMSAnalysis;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.MathUtils;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.GCMSFragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import org.apache.commons.math3.special.Erf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

            @Override
            public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores) {
                // score peak intensity
                for (int i = 0; i < scores.length; i++) {
                    if (peaks.get(i).getRelativeIntensity()!=0) scores[i] += Math.log(100 * peaks.get(i).getRelativeIntensity() * 0.1); // 0-Model: pareto distribution of noise peaks
                    //todo but what has this to do with pareto?

                }
            }

            @Override
            public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };

        final DecompositionScorer h2cDecompositionScorer = new DecompositionScorer<Object>() {
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

            @Override
            public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        final MolecularFormulaScorer h2cScorer = new MolecularFormulaScorer() {
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

        /**
         * MassdeviationScorer with massPenalty as in GCMSTool
         */
        final DecompositionScorer<Object> oldMassDeviationVertexScorer = new DecompositionScorer<Object>() {
            private final double sqrt2 = Math.sqrt(2);
            private final double massPenalty = 3.0;
            @Override
            public Object prepare(ProcessedInput input) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public double score(MolecularFormula formula, ProcessedPeak peak, ProcessedInput input, Object precomputed) {
                if (peak.getOriginalPeaks().isEmpty()) return 0d; // don't score synthetic peaks
                //difference to MassDeviationVertexScorer: score ions not unmodified mass
                final Ionization ionization = input.getExperimentInformation().getIonization();
                final double theoreticalMass = ionization.addToMass(formula.getMass());
                final double realMass = peak.getMass();
                final MeasurementProfile profile = input.getExperimentInformation().getMeasurementProfile();
                final Deviation dev = profile.getStandardMs2MassDeviation();
                final double sd = dev.absoluteFor(realMass);
                return Math.log(Erf.erfc(Math.abs(realMass - theoreticalMass) * massPenalty / (sd * sqrt2)));
            }

            @Override
            public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };

        final GCMSFragmentationPatternAnalysis analysis = new GCMSFragmentationPatternAnalysis();
        analysis.setInitial();
        //no pre-processors
        //....
        //validator
        //analysis.getInputValidators().add(new GCMSMissingValueValidator());
        //decomp scorer vertices
        analysis.getDecompositionScorers().add(oldMassDeviationVertexScorer);
        analysis.getDecompositionScorers().add(h2cDecompositionScorer);
        //scorer root
        //todo never scored in GCMSTool, change minimal mass?
//        analysis.getRootScorers().add(new MassDeviationVertexScorer);
//        analysis.getRootScorers().add(new ChemicalPriorScorer(h2cScorer, 0d, 11d));
        //lossScorer
        analysis.getLossScorers().add(new ChemicalPriorEdgeScorer(h2cScorer, 0d, 0d));
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


    public GCMSFragmentationPatternAnalysis getSparseGCMSAnalysis(){
        setInitials();

        final GCMSFragmentationPatternAnalysis analysis = new GCMSFragmentationPatternAnalysis();
        analysis.setInitial();   //todo weglassen?


        LossScorer mayNotBeSingle = new LossScorer() {
            @Override
            public Object prepare(ProcessedInput inputh) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public double score(Loss loss, ProcessedInput input, Object precomputed) {
                final List<Element> elements = loss.getFormula().elements();
                if (elements.size() == 1) {
                    //C and N may not be a single loss
                    final String mayNotBeSingle = "CN";
                    if (mayNotBeSingle.contains(elements.get(0).getSymbol())){
                        return Math.log(0.0001);
                    }
                    if (elements.get(0).getSymbol().equals("H") && loss.getFormula().numberOfHydrogens()>2){
                        return Math.log(0.0001);
                    }
                }
                return 0;
            }

            @Override
            public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };

        // loss scorers
        final List<LossScorer> lossScorers = new ArrayList<LossScorer>();
        //lossScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet());
        lossScorers.add(new DBELossScorer());
        lossScorers.add(new PureCarbonNitrogenLossScorer());
        lossScorers.add(new ChemicalPriorEdgeScorer()); //todo nicht in default FPA
        lossScorers.add(mayNotBeSingle);


        Map<MolecularFormula, Double> commonLosses = EICommonLossEdgeScorer.getSingleLossesGCMSCommonLossScorer().getCommonLosses();
        Map<MolecularFormula, Double> commonLossesConservative = new HashMap<MolecularFormula, Double>();
        for (Map.Entry<MolecularFormula, Double> entry : commonLosses.entrySet()) {
            if (entry.getValue().equals(Math.log(5))){
                commonLossesConservative.put(entry.getKey(), Math.log(2));
            } else if (entry.getValue().equals(Math.log(10))){
                commonLossesConservative.put(entry.getKey(), Math.log(4));
            } else if (entry.getValue().equals(Math.log(50))){
                commonLossesConservative.put(entry.getKey(), Math.log(8));
            } else if (entry.getValue().equals(Math.log(100))){
                commonLossesConservative.put(entry.getKey(), Math.log(16));
            }
        }
        CommonLossEdgeScorer commonLossEdgeScorer = new CommonLossEdgeScorer(commonLossesConservative, null);


        lossScorers.add(commonLossEdgeScorer);
        analysis.setLossScorers(lossScorers);

        // root scorers
        analysis.getRootScorers().add(new ChemicalPriorScorer());

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
