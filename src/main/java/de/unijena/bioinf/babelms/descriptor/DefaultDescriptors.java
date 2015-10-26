package de.unijena.bioinf.babelms.descriptor;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.RecalibrationFunction;
import de.unijena.bioinf.ChemistryBase.ms.ft.Score;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

public class DefaultDescriptors {

    private static class IonizationDescriptor implements Descriptor<Ionization> {

        @Override
        public String[] getKeywords() {
            return new String[]{"ion"};
        }

        @Override
        public Class<Ionization> getAnnotationClass() {
            return Ionization.class;
        }

        @Override
        public <G, D, L> Ionization read(DataDocument<G, D, L> document, D dictionary) {
            return PeriodicTable.getInstance().ionByName(document.getStringFromDictionary(dictionary, "ion")).getIonization();
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, Ionization annotation) {
            document.addToDictionary(dictionary, "ion", annotation.toString());
        }
    }

    private static class PrecursorIonTypeDescriptor implements Descriptor<PrecursorIonType> {

        @Override
        public String[] getKeywords() {
            return new String[]{"precursorIonType"};
        }

        @Override
        public Class<PrecursorIonType> getAnnotationClass() {
            return PrecursorIonType.class;
        }

        @Override
        public <G, D, L> PrecursorIonType read(DataDocument<G, D, L> document, D dictionary) {
            return PeriodicTable.getInstance().ionByName(document.getStringFromDictionary(dictionary, "precursorIonType"));
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, PrecursorIonType annotation) {
            document.addToDictionary(dictionary, "precursorIonType", annotation.toString());
        }
    }

    private static class TreeScoringDescriptor implements Descriptor<TreeScoring> {

        @Override
        public String[] getKeywords() {
            return new String[]{"score"};
        }

        @Override
        public Class<TreeScoring> getAnnotationClass() {
            return TreeScoring.class;
        }

        @Override
        public <G, D, L> TreeScoring read(DataDocument<G, D, L> document, D dictionary) {
            final TreeScoring scoring = new TreeScoring();
            final D score = document.getDictionaryFromDictionary(dictionary, "score");
            scoring.setOverallScore(document.getDoubleFromDictionary(score, "total"));
            if (document.hasKeyInDictionary(score, "root")) {
                scoring.setRootScore(document.getDoubleFromDictionary(score, "root"));
            }

            scoring.setRecalibrationBonus(document.getDoubleFromDictionary(score, "recalibrationBonus"));

            for (String key : document.keySetOfDictionary(score)) {
                if (key.equals("total") || key.equals("root") || key.equals("recalibrationBonus") || key.equals("tree")) continue;
                scoring.addAdditionalScore(key, document.getDoubleFromDictionary(score, key));
            }

            return scoring;
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, TreeScoring annotation) {
            final D score = document.newDictionary();
            document.addToDictionary(score, "total", annotation.getOverallScore());
            document.addToDictionary(score, "recalibrationBonus", annotation.getRecalibrationBonus());
            document.addToDictionary(score, "root", annotation.getRootScore());
            double sum = 0d;
            for (Map.Entry<String, Double> special : annotation.getAdditionalScores().entrySet()) {
                document.addToDictionary(score, special.getKey(), special.getValue());
                sum += special.getValue();
            }
            document.addToDictionary(score, "tree", annotation.getOverallScore()-sum);
            document.addDictionaryToDictionary(dictionary, "score", score);
        }
    }

    private static class RecalibrationFunctionDescriptor implements Descriptor<RecalibrationFunction> {

        @Override
        public String[] getKeywords() {
            return new String[]{"recalibration"};
        }

        @Override
        public Class<RecalibrationFunction> getAnnotationClass() {
            return RecalibrationFunction.class;
        }

        @Override
        public <G, D, L> RecalibrationFunction read(DataDocument<G, D, L> document, D dictionary) {
            return RecalibrationFunction.fromString(document.getStringFromDictionary(dictionary, "recalibration"));
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, RecalibrationFunction annotation) {
            document.addToDictionary(dictionary, "recalibration", annotation.toString());
        }
    }

    private static class ScoreDescriptor implements Descriptor<Score> {

        private final WeakHashMap<String[], String[]> constantPool = new WeakHashMap<String[], String[]>();

        @Override
        public String[] getKeywords() {
            return new String[]{"score"};
        }

        @Override
        public Class<Score> getAnnotationClass() {
            return Score.class;
        }

        @Override
        public <G, D, L> Score read(DataDocument<G, D, L> document, D dictionary) {
            final ArrayList<String> nameList = new ArrayList<String>();
            final D scoredict = document.getDictionaryFromDictionary(dictionary,"score");
            for (String keyword : document.keySetOfDictionary(scoredict)) {
                nameList.add(keyword);
            }
            String[] names = nameList.toArray(new String[nameList.size()]);
            synchronized (constantPool) {
                if (constantPool.get(names)!=null)
                    names = constantPool.get(names);
                else
                    constantPool.put(names,names);
            }
            final Score score = new Score(names);
            for (int k=0; k < names.length; ++k) {
                score.set(k, document.getDoubleFromDictionary(scoredict, names[k]));
            }
            return score;
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, Score annotation) {
            final D scoredict = document.newDictionary();
            for (Map.Entry<String,Double> entry : annotation.entrySet()) {
                document.addToDictionary(scoredict, entry.getKey(), entry.getValue());
            }
            document.addDictionaryToDictionary(dictionary, "score", scoredict);
        }
    }

    private static class PeakDescriptor implements Descriptor<Peak> {

        @Override
        public String[] getKeywords() {
            return new String[]{"mz", "intensity"};
        }

        @Override
        public Class<Peak> getAnnotationClass() {
            return Peak.class;
        }

        @Override
        public <G, D, L> Peak read(DataDocument<G, D, L> document, D dictionary) {
            return new Peak(document.getDoubleFromDictionary(dictionary, "mz"), document.getDoubleFromDictionary(dictionary, "intensity"));
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, Peak annotation) {
            document.addToDictionary(dictionary, "mz", annotation.getMass());
            document.addToDictionary(dictionary, "intensity", annotation.getIntensity());
        }
    }

}
