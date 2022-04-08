/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms.descriptor;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.data.TypeError;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.sirius.annotations.SpectralRecalibration;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class DefaultDescriptors {


    static void addAll(DescriptorRegistry registry) {
        registry.put(FTree.class, InChI.class, new InChIDescriptor());
        registry.put(FTree.class, PrecursorIonType.class, new PrecursorIonTypeDescriptor("precursorIonType"));
        registry.put(FTree.class, SpectralRecalibration.class, new RecalibrationFunctionDescriptor());
        registry.put(FTree.class, Beautified.class, new BeautificationDescriptor());
        registry.put(FTree.class, Smiles.class, new SmilesDescriptor());
        //registry.put(FTree.class, TreeStatistics.class, new TreeScoringDescriptor());
        registry.put(Fragment.class, Ms2IsotopePattern.class, new Ms2IsotopePatternDescriptor());
        registry.put(Fragment.class, Ms1IsotopePattern.class, new Ms1IsotopePatternDescriptor());
        registry.put(Fragment.class, Peak.class, new PeakDescriptor());
        registry.put(Fragment.class, AnnotatedPeak.class, new AnnotatedPeakDescriptor());
        registry.put(Fragment.class, Score.class, new ScoreDescriptor());
        registry.put(Fragment.class, Ionization.class, new IonizationDescriptor());
        registry.put(Fragment.class, ImplicitAdduct.class, new ImplicitAdductDescriptor());
;       registry.put(FTree.class, LipidSpecies.class, new LipidDescriptor());

        registry.put(Loss.class, Score.class, new ScoreDescriptor());
        registry.put(Loss.class, LossType.class, new LossTypeDescriptor());
        registry.put(Loss.class, ImplicitAdduct.class, new ImplicitAdductDescriptor());

        registry.put(FTree.class, IonTreeUtils.Type.class, new IonTypeDescriptor());
        registry.put(FTree.class, IonTreeUtils.ExpandedAdduct.class, new ExpandedAdductDescritor());

        registry.put(FTree.class, UnconsideredCandidatesUpperBound.class, new UnregardedCandidatesUpperBoundDescriptor());
        registry.put(FTree.class, TreeStatistics.class, new TreeStatisticsDescriptor());
    }


    private static class LipidDescriptor implements Descriptor<LipidSpecies> {
        private final String KEY = "lipid-annotation";
        @Override
        public String[] getKeywords() {
            return new String[]{KEY};
        }

        @Override
        public Class<LipidSpecies> getAnnotationClass() {
            return LipidSpecies.class;
        }

        @Override
        public <G, D, L> LipidSpecies read(DataDocument<G, D, L> document, D dictionary) {
            if (document.hasKeyInDictionary(dictionary, KEY)) {
                return LipidSpecies.fromString(document.getStringFromDictionary(dictionary,KEY));
            } else return null;
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, LipidSpecies annotation) {
            if (annotation!=null) {
                document.addToDictionary(dictionary, KEY, annotation.toString());
            }
        }
    }

    private static class TreeStatisticsDescriptor implements Descriptor<TreeStatistics> {

        @Override
        public String[] getKeywords() {
            return new String[]{"statistics"};
        }

        @Override
        public Class<TreeStatistics> getAnnotationClass() {
            return TreeStatistics.class;
        }

        @Override
        public <G, D, L> TreeStatistics read(DataDocument<G, D, L> document, D dictionary) {
            if (document.hasKeyInDictionary(dictionary,"statistics")) {
                final D stats = document.getDictionaryFromDictionary(dictionary, "statistics");
                return new TreeStatistics(
                        document.getDoubleFromDictionary(stats,"explainedIntensity"),
                        document.getDoubleFromDictionary(stats,"explainedIntensityOfExplainablePeaks"),
                        document.getDoubleFromDictionary(stats,"ratioOfExplainedPeaks")
                );
            } else {
                return TreeStatistics.none();
            }
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, TreeStatistics annotation) {
            final D dic = document.newDictionary();
            document.addToDictionary(dic, "explainedIntensity", annotation.getExplainedIntensity());
            document.addToDictionary(dic, "explainedIntensityOfExplainablePeaks", annotation.getExplainedIntensityOfExplainablePeaks());
            document.addToDictionary(dic, "ratioOfExplainedPeaks", annotation.getRatioOfExplainedPeaks());
            document.addDictionaryToDictionary(dictionary,"statistics", dic);
        }
    }

    private static class BeautificationDescriptor implements Descriptor<Beautified> {

        @Override
        public String[] getKeywords() {
            return new String[]{"nodeBoost"};
        }

        @Override
        public Class<Beautified> getAnnotationClass() {
            return Beautified.class;
        }

        @Override
        public <G, D, L> Beautified read(DataDocument<G, D, L> document, D dictionary) {
            if (document.hasKeyInDictionary(dictionary,"nodeBoost")) {
                double nodeBoost = document.getDoubleFromDictionary(dictionary, "nodeBoost");
                double beautificationPenalty = 0d;
                if (document.hasKeyInDictionary(dictionary, "beautificationPenalty")) {
                    beautificationPenalty = document.getDoubleFromDictionary(dictionary, "beautificationPenalty");
                }
                return Beautified.beautified(nodeBoost, beautificationPenalty);
            } else {
                return Beautified.ugly();
            }
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, Beautified annotation) {
            document.addToDictionary(dictionary, "nodeBoost", annotation.getNodeBoost() );
        }
    }

    @Deprecated // we already have Fragment#getIonization so this is not necessary anymore...
    private static class IonizationDescriptor implements Descriptor<Ionization> {

        @Override
        public String[] getKeywords() {
            return new String[]{"ion","precursorIonType"};
        }

        @Override
        public Class<Ionization> getAnnotationClass() {
            return Ionization.class;
        }

        @Override
        public <G, D, L> Ionization read(DataDocument<G, D, L> document, D dictionary) {
            if (document.hasKeyInDictionary(dictionary,"ion")) {
                return PeriodicTable.getInstance().ionByNameOrNull(document.getStringFromDictionary(dictionary, "ion")).getIonization();
            } else {
                return PeriodicTable.getInstance().ionByNameOrNull(document.getStringFromDictionary(dictionary, "precursorIonType")).getIonization();
            }
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, Ionization annotation) {
            //document.addToDictionary(dictionary, "ion", annotation.toString());
        }
    }

    private static class LossTypeDescriptor implements Descriptor<LossType> {

        @Override
        public String[] getKeywords() {
            return new String[]{"insourceFragmentation", "adductLoss"};
        }

        @Override
        public Class<LossType> getAnnotationClass() {
            return LossType.class;
        }

        @Override
        public <G, D, L> LossType read(DataDocument<G, D, L> document, D dictionary) {
            if (document.hasKeyInDictionary(dictionary, "insourceFragmentation")) return LossType.insource();
            else if (document.hasKeyInDictionary(dictionary, "adductLoss")) {
                final D ad = document.getDictionaryFromDictionary(dictionary, "adductLoss");
                final String adduct = document.getStringFromDictionary(ad, "adduct");
                final String orig = document.getStringFromDictionary(ad, "modifiedMolecularFormula");
                return LossType.adductLoss(MolecularFormula.parseOrThrow(adduct), MolecularFormula.parseOrThrow(orig));
            } else return LossType.regular();
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, LossType annotation) {
            switch (annotation.getType()) {
                case REGULAR:
                    return;
                case IN_SOURCE:
                    document.addToDictionary(dictionary, "insourceFragmentation", "yes");
                    return;
                case ADDUCT_LOSS:
                    final LossType.AdductLossInformation ad = annotation.getAdductLossInformation();
                    final D dic = document.newDictionary();
                    document.addToDictionary(dic, "adduct", ad.getAdductFormula().toString());
                    document.addToDictionary(dic,"modifiedMolecularFormula", ad.getOriginalFormula().toString());
                    document.addDictionaryToDictionary(dictionary, "adductLoss", dic);
            }
        }
    }


    private static class ImplicitAdductDescriptor implements Descriptor<ImplicitAdduct> {

        @Override
        public String[] getKeywords() {
            return new String[]{"implicitAdduct","hasImplicitAdduct"};
        }

        @Override
        public Class<ImplicitAdduct> getAnnotationClass() {
            return ImplicitAdduct.class;
        }

        @Override
        public <G, D, L> ImplicitAdduct read(DataDocument<G, D, L> document, D dictionary) {
                // hacked :/
                if (document.hasKeyInDictionary(dictionary,"hasImplicitAdduct")) {
                    D dict = document.getDictionaryFromDictionary(dictionary, "hasImplicitAdduct");
                    MolecularFormula f = MolecularFormula.parseOrThrow(document.getStringFromDictionary(dict, "adductFormula"));
                    double score = 0d;
                    if (document.hasKeyInDictionary(dict, "score"))
                        score = document.getDoubleFromDictionary(dict, "score");
                    AnnotatedPeak peak = null;
                    if (document.hasKeyInDictionary(dict, "mz")) {
                        peak = new AnnotatedPeakDescriptor().read(document, dict);
                    }
                    return new ImplicitAdduct(f, Optional.ofNullable(peak), score);
                } else if (document.hasKeyInDictionary(dictionary,"implicitAdduct")) {
                    return new ImplicitAdduct(MolecularFormula.parseOrThrow(document.getStringFromDictionary(dictionary, "implicitAdduct")));
                } else return ImplicitAdduct.none();
            }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, ImplicitAdduct annotation) {
            if (annotation.hasImplicitAdduct()) {
                final D impl = document.newDictionary();
                document.addToDictionary(impl, "adductFormula", annotation.getAdductFormula().toString());
                if (annotation.getImplicitPeak().isPresent()) {
                    document.addToDictionary(impl, "score", annotation.getScore());
                    new AnnotatedPeakDescriptor().write(document, impl, annotation.getImplicitPeak().get());
                } else {
                    //legacy
                    //document.addToDictionary(dictionary, "implicitAdduct", annotation.getAdductFormula().toString());
                }
                document.addDictionaryToDictionary(dictionary, "hasImplicitAdduct", impl);
            }
        }
    }


    private static class InChIDescriptor implements Descriptor<InChI> {

        @Override
        public String[] getKeywords() {
            return new String[]{"inchi", "inchikey"};
        }

        @Override
        public Class<InChI> getAnnotationClass() {
            return InChI.class;
        }

        @Override
        public <G, D, L> InChI read(DataDocument<G, D, L> document, D dictionary) {
            String inchi=null, inchikey=null;
            if (document.hasKeyInDictionary(dictionary, "inchi")) {
                inchi = document.getStringFromDictionary(dictionary, "inchi");
            }
            if (document.hasKeyInDictionary(dictionary, "inchikey")) {
                inchikey = document.getStringFromDictionary(dictionary, "inchikey");
            }
            if (inchi!=null || inchikey!=null) return InChIs.newInChI(inchikey, inchi);
            else return null;
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, InChI annotation) {
            if (annotation.key!=null) document.addToDictionary(dictionary, "inchikey", annotation.key);
            if (annotation.in3D!=null) document.addToDictionary(dictionary, "inchi", annotation.in3D);
        }
    }

    private static class SmilesDescriptor implements Descriptor<Smiles> {

        @Override
        public String[] getKeywords() {
            return new String[]{"smarts"};
        }

        @Override
        public Class<Smiles> getAnnotationClass() {
            return Smiles.class;
        }

        @Override
        public <G, D, L> Smiles read(DataDocument<G, D, L> document, D dictionary) {
            if (document.hasKeyInDictionary(dictionary, "smarts")) {
                return new Smiles(document.getStringFromDictionary(dictionary, "smarts"));
            } else return null;
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, Smiles annotation) {
            if (annotation.smiles!=null){
                document.addToDictionary(dictionary, "smarts", annotation.smiles);
            }
        }
    }

    private static class PrecursorIonTypeDescriptor implements Descriptor<PrecursorIonType> {

        private final String keywordName;

        public PrecursorIonTypeDescriptor(String keywordName) {
            this.keywordName = keywordName;
        }

        @Override
        public String[] getKeywords() {
            return new String[]{keywordName};
        }

        @Override
        public Class<PrecursorIonType> getAnnotationClass() {
            return PrecursorIonType.class;
        }

        @Override
        public <G, D, L> PrecursorIonType read(DataDocument<G, D, L> document, D dictionary) {
            return PeriodicTable.getInstance().ionByNameOrNull(document.getStringFromDictionary(dictionary, keywordName));
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, PrecursorIonType annotation) {
            document.addToDictionary(dictionary, keywordName, annotation.toString());
        }
    }

    private static class Ms2IsotopePatternDescriptor implements Descriptor<Ms2IsotopePattern> {

        @Override
        public String[] getKeywords() {
            return new String[]{"fragmentIsotopes"};
        }

        @Override
        public Class<Ms2IsotopePattern> getAnnotationClass() {
            return Ms2IsotopePattern.class;
        }

        @Override
        public <G, D, L> Ms2IsotopePattern read(DataDocument<G, D, L> document, D dictionary) {
            final List<Peak> peaks = new ArrayList<>();
            final D isotopes = document.getDictionaryFromDictionary(dictionary, "fragmentIsotopes");
            final L mzs = document.getListFromDictionary(isotopes, "mz"), ints = document.getListFromDictionary(isotopes, "relInt");
            if (mzs==null || ints==null) return null;
            for (int k=0, n=Math.min(document.sizeOfList(mzs), document.sizeOfList(ints)); k < n; ++k) {
                peaks.add(new SimplePeak(document.getDoubleFromList(mzs, k), document.getDoubleFromList(ints, k)));
            }

            double score = document.hasKeyInDictionary(isotopes, "score") ? document.getDoubleFromDictionary(isotopes, "score") : 0d;
            return new Ms2IsotopePattern(peaks.toArray(new Peak[peaks.size()]), score);
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, Ms2IsotopePattern annotation) {
            final D isotopes = document.newDictionary();
            final L mzs = document.newList(), ints = document.newList();
            final Peak[] peaks = annotation.getPeaks();
            for (Peak p : peaks) {
                document.addToList(mzs, p.getMass());
                document.addToList(ints, p.getIntensity());
            }
            document.addDictionaryToDictionary(dictionary, "fragmentIsotopes", isotopes);
            document.addToDictionary(isotopes, "score", annotation.getScore());
            document.addListToDictionary(isotopes, "mz", mzs);
            document.addListToDictionary(isotopes, "relInt", ints);
        }
    }

    private static class Ms1IsotopePatternDescriptor implements Descriptor<Ms1IsotopePattern> {

        @Override
        public String[] getKeywords() {
            return new String[]{"isotopes"};
        }

        @Override
        public Class<Ms1IsotopePattern> getAnnotationClass() {
            return Ms1IsotopePattern.class;
        }

        @Override
        public <G, D, L> Ms1IsotopePattern read(DataDocument<G, D, L> document, D dictionary) {
            final List<Peak> peaks = new ArrayList<>();
            final D isotopes = document.getDictionaryFromDictionary(dictionary, "isotopes");
            final L mzs = document.getListFromDictionary(isotopes, "mz"), ints = document.getListFromDictionary(isotopes, "relInt");
            if (mzs==null || ints==null) return null;
            for (int k=0, n=Math.min(document.sizeOfList(mzs), document.sizeOfList(ints)); k < n; ++k) {
                peaks.add(new SimplePeak(document.getDoubleFromList(mzs, k), document.getDoubleFromList(ints, k)));
            }

            double score = document.hasKeyInDictionary(isotopes, "score") ? document.getDoubleFromDictionary(isotopes, "score") : 0d;
            return new Ms1IsotopePattern(peaks.toArray(new Peak[peaks.size()]), score);
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, Ms1IsotopePattern annotation) {
            final D isotopes = document.newDictionary();
            final L mzs = document.newList(), ints = document.newList();
            final Peak[] peaks = annotation.getPeaks();
            for (Peak p : peaks) {
                document.addToList(mzs, p.getMass());
                document.addToList(ints, p.getIntensity());
            }
            document.addDictionaryToDictionary(dictionary, "isotopes", isotopes);
            document.addToDictionary(isotopes, "score", annotation.getScore());
            document.addListToDictionary(isotopes, "mz", mzs);
            document.addListToDictionary(isotopes, "relInt", ints);
        }
    }

    /*
    private static class TreeScoringDescriptor implements Descriptor<TreeStatistics> {

        @Override
        public String[] getKeywords() {
            return new String[]{"score"};
        }

        @Override
        public Class<TreeStatistics> getAnnotationClass() {
            return TreeStatistics.class;
        }


        // TODO: quickn dirty
        @Override
        public <G, D, L> TreeStatistics read(DataDocument<G, D, L> document, D dictionary) {
            final TreeStatistics scoring = new TreeStatistics();
            final D score = document.getDictionaryFromDictionary(dictionary, "score");
            scoring.setOverallScore(document.getDoubleFromDictionary(score, "total"));
            if (document.hasKeyInDictionary(score, "root")) {
                scoring.setRootScore(document.getDoubleFromDictionary(score, "root"));
            }

            scoring.setRecalibrationBonus(document.getDoubleFromDictionary(score, "recalibrationBonus"));
            if (document.hasKeyInDictionary(score, "recalibrationPenalty")) {
                scoring.setRecalibrationPenalty(document.getDoubleFromDictionary(score, "recalibrationPenalty"));
            }
            if (document.hasKeyInDictionary(score, "beautificationPenalty")){
                scoring.setBeautificationPenalty(document.getDoubleFromDictionary(score, "beautificationPenalty"));
            } else scoring.setBeautificationPenalty(0);


            for (String key : document.keySetOfDictionary(score)) {
                if (key.equals("total") || key.equals("root") || key.equals("recalibrationBonus") || key.equals("beautificationPenalty") || key.equals("tree")) continue;
                final double addScore = document.getDoubleFromDictionary(score, key);
                scoring.addAdditionalScore(key, addScore);
                scoring.setOverallScore(scoring.getOverallScore()-addScore);
            }

            if (document.hasKeyInDictionary(dictionary, "ratioOfExplainedPeaks")) {
                scoring.setRatioOfExplainedPeaks(document.getDoubleFromDictionary(dictionary, "ratioOfExplainedPeaks"));
            }
            if (document.hasKeyInDictionary(dictionary, "explainedIntensity")) {
                scoring.setExplainedIntensity(document.getDoubleFromDictionary(dictionary, "explainedIntensity"));
            }
            if (document.hasKeyInDictionary(dictionary, "explainedIntensityOfExplainablePeaks")) {
                scoring.setExplainedIntensityOfExplainablePeaks(document.getDoubleFromDictionary(dictionary, "explainedIntensityOfExplainablePeaks"));
            }
            if (document.hasKeyInDictionary(score,"isotope"))
                scoring.setIsotopeMs1Score(document.getDoubleFromDictionary(score, "isotope"));


            return scoring;
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, TreeScoring annotation) {
            final D score = document.newDictionary();
            document.addToDictionary(score, "total", annotation.getOverallScore());
            document.addToDictionary(score, "recalibrationBonus", annotation.getRecalibrationBonus());
            document.addToDictionary(score, "recalibrationPenalty", annotation.getRecalibrationPenalty());
            document.addToDictionary(score, "beautificationPenalty", annotation.getBeautificationPenalty());
            document.addToDictionary(score, "root", annotation.getRootScore());
            double sum = 0d;
            for (Map.Entry<String, Double> special : annotation.getAdditionalScores().entrySet()) {
                document.addToDictionary(score, special.getKey(), special.getValue());
                sum += special.getValue();
            }
            document.addToDictionary(score, "tree", annotation.getOverallScore()-sum);
            document.addDictionaryToDictionary(dictionary, "score", score);

            document.addToDictionary(dictionary, "ratioOfExplainedPeaks", annotation.getRatioOfExplainedPeaks());
            document.addToDictionary(dictionary, "explainedIntensity", annotation.getExplainedIntensity());
            document.addToDictionary(dictionary, "explainedIntensityOfExplainablePeaks", annotation.getExplainedIntensityOfExplainablePeaks());
            document.addToDictionary(score, "isotope", annotation.getIsotopeMs1Score());
        }
    }
    */

    private static class RecalibrationFunctionDescriptor implements Descriptor<SpectralRecalibration> {

        @Override
        public String[] getKeywords() {
            return new String[]{"recalibration"};
        }

        @Override
        public Class<SpectralRecalibration> getAnnotationClass() {
            return SpectralRecalibration.class;
        }

        @Override
        public <G, D, L> SpectralRecalibration read(DataDocument<G, D, L> document, D dictionary) {
            if (!document.hasKeyInDictionary(dictionary, "recalibration")) return SpectralRecalibration.none();
            if (document.isString(document.getFromDictionary(dictionary, "recalibration"))) {
                //backward compatibility
                RecalibrationFunction merged = RecalibrationFunction.fromString(document.getStringFromDictionary(dictionary, "recalibration"));
                return new SpectralRecalibration(null, merged);
            }
            final D rec = document.getDictionaryFromDictionary(dictionary, "recalibration");
            final RecalibrationFunction merged;
            if (!document.hasKeyInDictionary(rec,"merged")) merged = RecalibrationFunction.identity();
            else merged = RecalibrationFunction.fromString(document.getStringFromDictionary(rec, "merged"));
            final RecalibrationFunction[] separates;
            if (document.hasKeyInDictionary(rec, "separate")) {
                final List<RecalibrationFunction> seps = new ArrayList<>();
                final L list = document.getListFromDictionary(rec, "separate");
                for (int k=0; k < document.sizeOfList(list); ++k) {
                    final String s = document.getStringFromList(list, k);
                    if (s.equals("none")) seps.add(null);
                    else seps.add(RecalibrationFunction.fromString(s));
                }
                separates = seps.toArray(new RecalibrationFunction[seps.size()]);
            } else separates = null;
            return new SpectralRecalibration(separates, merged);
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, SpectralRecalibration annotation) {

            final D rec = document.newDictionary();
            final RecalibrationFunction f = annotation.getMergedRecalibrationFunction();
            document.addToDictionary(rec, "merged", f.toString());

            final RecalibrationFunction[] singleSpectrumRecalibrationFunctions = annotation.getSingleSpectrumRecalibrationFunctions();
            if (singleSpectrumRecalibrationFunctions!=null) {
                final L list = document.newList();
                for (RecalibrationFunction g : singleSpectrumRecalibrationFunctions) {
                    if (g==null) document.addToList(list, "none");
                    else document.addToList(list, g.toString());
                }
                document.addListToDictionary(rec, "separate", list);
            }

            document.addDictionaryToDictionary(dictionary, "recalibration", rec);
        }
    }

    private static class ScoreDescriptor implements Descriptor<Score> {

        //private final WeakHashMap<String[], String[]> constantPool = new WeakHashMap<String[], String[]>();

        @Override
        public String[] getKeywords() {
            return new String[]{"score", "scores"};
        }

        @Override
        public Class<Score> getAnnotationClass() {
            return Score.class;
        }

        @Override
        public <G, D, L> Score read(DataDocument<G, D, L> document, D dictionary) {
            final ArrayList<String> nameList = new ArrayList<String>();
            final D scoredict = document.getDictionaryFromDictionary(dictionary,"scores");
            for (String keyword : document.keySetOfDictionary(scoredict)) {
                nameList.add(keyword);
            }
            String[] names = nameList.toArray(new String[nameList.size()]);
            /*
            synchronized (constantPool) {
                if (constantPool.get(names)!=null) {
                    names = constantPool.get(names);
                } else {
                    constantPool.put(names, names);
                }
            }
             */
            final Score.HeaderBuilder score = Score.defineScoring();
            for (int k=0; k < names.length; ++k) {
                score.define(names[k]);
            }
            final Score.ScoreAssigner assign = score.score();
            for (int k=0; k < names.length; ++k) {
                assign.set(names[k], document.getDoubleFromDictionary(scoredict, names[k]));
            }
            return assign.done();
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, Score annotation) {
            final D scoredict = document.newDictionary();
            for (Map.Entry<String,Double> entry : annotation.asMap().entrySet()) {
                document.addToDictionary(scoredict, entry.getKey(), entry.getValue());
            }
            document.addToDictionary(dictionary, "score", annotation.sum());
            document.addDictionaryToDictionary(dictionary, "scores", scoredict);
        }
    }

    private static class PeakDescriptor implements Descriptor<Peak> {

        @Override
        public String[] getKeywords() {
            return new String[]{"mz", "intensity","relativeIntensity"};
        }

        @Override
        public Class<Peak> getAnnotationClass() {
            return Peak.class;
        }

        @Override
        public <G, D, L> Peak read(DataDocument<G, D, L> document, D dictionary) {
            return new SimplePeak(document.getDoubleFromDictionary(dictionary, "mz"), document.hasKeyInDictionary(dictionary,"relativeIntensity") ? document.getDoubleFromDictionary(dictionary, "relativeIntensity") : document.getDoubleFromDictionary(dictionary, "intensity"));
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, Peak annotation) {
            //document.addToDictionary(dictionary, "mz", annotation.getMass());
            //document.addToDictionary(dictionary, "intensity", annotation.getIntensity());
        }
    }

    private static class AnnotatedPeakDescriptor implements Descriptor<AnnotatedPeak> {

        @Override
        public String[] getKeywords() {
            return new String[]{"peaks", "massdev", "collisionEnergies", "ion"};
        }

        @Override
        public Class<AnnotatedPeak> getAnnotationClass() {
            return AnnotatedPeak.class;
        }

        @Override
        public <G, D, L> AnnotatedPeak read(DataDocument<G, D, L> document, D dictionary) {
            if (!(document.hasKeyInDictionary(dictionary, "mz") && document.hasKeyInDictionary(dictionary, "relativeIntensity") && document.hasKeyInDictionary(dictionary, "molecularFormula") && document.hasKeyInDictionary(dictionary, "ion"))) {
                return null;
            }
            final MolecularFormula formula = MolecularFormula.parseOrNull(document.getStringFromDictionary(dictionary, "molecularFormula"));
            final double mass = document.getDoubleFromDictionary(dictionary, "mz");
            final double relativeIntensity = document.hasKeyInDictionary(dictionary, "relativeIntensity") ?
                    document.getDoubleFromDictionary(dictionary, "relativeIntensity") : 0d;
            final double recalibratedMass = (document.hasKeyInDictionary(dictionary, "recalibratedMass")) ?
                    document.getDoubleFromDictionary(dictionary, "recalibratedMass") : 0d;
            final Ionization ion = PeriodicTable.getInstance().ionByNameOrNull(document.getStringFromDictionary(dictionary, "ion")).getIonization();

            final ArrayList<CollisionEnergy> energies = new ArrayList<CollisionEnergy>();

            final ArrayList<Peak> originalPeaks = new ArrayList<Peak>();
            final TIntArrayList specIds = new TIntArrayList();
            if (document.hasKeyInDictionary(dictionary, "peaks")) {
                final L peakList = document.getListFromDictionary(dictionary, "peaks");
                for (int i=0, n=document.sizeOfList(peakList); i < n; ++i) {
                    final D peakData = document.getDictionaryFromList(peakList, i);

                    final double intensity;
                    final double mz;
                    if (document.hasKeyInDictionary(peakData, "intensity")) {
                        intensity = document.getDoubleFromDictionary(peakData, "intensity");
                    } else intensity = document.getDoubleFromDictionary(peakData, "int");

                    if (document.hasKeyInDictionary(peakData, "ce")) {
                        energies.add(CollisionEnergy.fromString(document.getStringFromDictionary(peakData, "ce")));
                    }
                    if (document.hasKeyInDictionary(peakData, "spectrum")) {
                        specIds.add((int)document.getIntFromDictionary(peakData, "spectrum"));
                    }

                    originalPeaks.add(new SimplePeak(document.getDoubleFromDictionary(peakData, "mz"), intensity));
                }
            }


            // LEGACY MODE
            if (document.hasKeyInDictionary(dictionary, "collisionEnergies")) {
                energies.clear();
                if (document.hasKeyInDictionary(dictionary, "collisionEnergies")) {
                    final L energyList = document.getListFromDictionary(dictionary, "collisionEnergies");
                    for (int i=0, n=document.sizeOfList(energyList); i < n; ++i) {
                        final CollisionEnergy energy = CollisionEnergy.fromString(document.getStringFromList(energyList, i));
                        energies.add(energy);
                    }
                }
            }

            return new AnnotatedPeak(formula, mass, recalibratedMass, relativeIntensity, ion, originalPeaks.toArray(new Peak[originalPeaks.size()]), energies.toArray(new CollisionEnergy[energies.size()]),specIds.toArray() );
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, AnnotatedPeak annotation) {
            // molecularFormula and ion are always part of the node
            //if (!document.hasKeyInDictionary(dictionary, "molecularFormula"))
            //    document.addToDictionary(dictionary, "molecularFormula", annotation.getMolecularFormula().toString());
            document.addToDictionary(dictionary, "mz", annotation.getMass());
            document.addToDictionary(dictionary, "relativeIntensity", annotation.getRelativeIntensity());
            document.addToDictionary(dictionary, "recalibratedMass", annotation.getRecalibratedMass());
            //document.addToDictionary(dictionary, "ion", annotation.getIonization().toString());

            final Peak[] peaks = annotation.getOriginalPeaks();
            final L peaklist = document.newList();
            for (int k=0; k < peaks.length; ++k) {
                final D dic = document.newDictionary();
                document.addToDictionary(dic, "mz", peaks[k].getMass());
                document.addToDictionary(dic, "intensity", peaks[k].getIntensity());
                if (annotation.getCollisionEnergies()[k]!=null) {
                    document.addToDictionary(dic, "ce", annotation.getCollisionEnergies()[k].toString());
                }
                document.addToDictionary(dic,"spectrum", annotation.getSpectrumIds()[k]);
                document.addDictionaryToList(peaklist, dic);
            }
            document.addListToDictionary(dictionary, "peaks", peaklist);
/*
            final CollisionEnergy[] energies = annotation.getCollisionEnergies();
            final L energyList = document.newList();
            for (CollisionEnergy e : energies) {
                document.addToList(energyList, e.toString());
            }
            document.addListToDictionary(dictionary, "collisionEnergies", energyList);
            */
        }

    }

    private static class IonTypeDescriptor implements Descriptor<IonTreeUtils.Type> {
        private final static String TOK = "treeType";
        @Override
        public String[] getKeywords() {
            return new String[]{TOK};
        }

        @Override
        public Class<IonTreeUtils.Type> getAnnotationClass() {
            return IonTreeUtils.Type.class;
        }

        @Override
        public <G, D, L> IonTreeUtils.Type read(DataDocument<G, D, L> document, D dictionary) {
            final String val = document.getStringFromDictionary(dictionary, TOK);
            if (val.equals("neutralized")) return IonTreeUtils.Type.RESOLVED;
            else if (val.equals("ionized")) return IonTreeUtils.Type.IONIZED;
            else if (val.equals("raw")) return IonTreeUtils.Type.RAW;
            else throw new IllegalArgumentException("Unknown tree type \"" + val + "\"");
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, IonTreeUtils.Type annotation) {
            String value;
            switch (annotation) {
                case IONIZED: value = "ionized"; break;
                case RESOLVED: value = "neutralized"; break;
                case RAW: value = "raw"; break;
                default: value = "raw";
            }
            document.addToDictionary(dictionary, TOK, value);

        }
    }

    private static class ExpandedAdductDescritor implements Descriptor<IonTreeUtils.ExpandedAdduct> {
        private final static String TOK = "expandedAdduct";
        @Override
        public String[] getKeywords() {
            return new String[]{TOK};
        }

        @Override
        public Class<IonTreeUtils.ExpandedAdduct> getAnnotationClass() {
            return IonTreeUtils.ExpandedAdduct.class;
        }

        @Override
        public <G, D, L> IonTreeUtils.ExpandedAdduct read(DataDocument<G, D, L> document, D dictionary) {
            final String val = document.getStringFromDictionary(dictionary, TOK);
            try {
                return IonTreeUtils.ExpandedAdduct.valueOf(val.toUpperCase());
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new IllegalArgumentException("Unknown expandedAdduct \"" + val + "\"", e);
            }
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, IonTreeUtils.ExpandedAdduct annotation) {
            document.addToDictionary(dictionary, TOK, annotation.name().toLowerCase());
        }
    }

    private static class UnregardedCandidatesUpperBoundDescriptor implements Descriptor<UnconsideredCandidatesUpperBound> {

        @Override
        public String[] getKeywords() {
            return new String[]{"numberOfUnconsideredCandidates", "lowestConsideredCandidateScore"};
        }

        @Override
        public Class<UnconsideredCandidatesUpperBound> getAnnotationClass() {
            return UnconsideredCandidatesUpperBound.class;
        }

        @Override
        public <G, D, L> UnconsideredCandidatesUpperBound read(DataDocument<G, D, L> document, D dictionary) {
            final int numberOfUnconsideredCandidates = (int)document.getIntFromDictionary(dictionary, "numberOfUnconsideredCandidates");
            final double lowestConsideredCandidateScore = document.getDoubleFromDictionary(dictionary, "lowestConsideredCandidateScore");
            return new UnconsideredCandidatesUpperBound(numberOfUnconsideredCandidates, lowestConsideredCandidateScore);
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, UnconsideredCandidatesUpperBound annotation) {
            document.addToDictionary(dictionary, "numberOfUnconsideredCandidates", annotation.getNumberOfUnconsideredCandidates());
            document.addToDictionary(dictionary, "lowestConsideredCandidateScore", annotation.getLowestConsideredCandidateScore());
        }
    }
}
