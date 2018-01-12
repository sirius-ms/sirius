package de.unijena.bioinf.babelms.descriptor;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

class DefaultDescriptors {


    static void addAll(DescriptorRegistry registry) {
        registry.put(FTree.class, Ionization.class, new IonizationDescriptor());
        registry.put(FTree.class, InChI.class, new InChIDescriptor());
        registry.put(FTree.class, PrecursorIonType.class, new PrecursorIonTypeDescriptor("precursorIonType"));
        registry.put(FTree.class, RecalibrationFunction.class, new RecalibrationFunctionDescriptor());
        registry.put(FTree.class, Smiles.class, new SmilesDescriptor());
        registry.put(FTree.class, TreeScoring.class, new TreeScoringDescriptor());
        registry.put(Fragment.class, Ms2IsotopePattern.class, new IsotopePatternDescriptor());
        registry.put(Fragment.class, Peak.class, new PeakDescriptor());
        registry.put(Fragment.class, AnnotatedPeak.class, new AnnotatedPeakDescriptor());
        registry.put(Fragment.class, Score.class, new ScoreDescriptor());
        registry.put(Fragment.class, Ionization.class, new IonizationDescriptor());

        registry.put(Loss.class, Score.class, new ScoreDescriptor());
        registry.put(Loss.class, InsourceFragmentation.class, new InsourceDescriptor());

        registry.put(FTree.class, IonTreeUtils.Type.class, new IonTypeDescriptor());

        registry.put(FTree.class, UnregardedCandidatesUpperBound.class, new UnregardedCandidatesUpperBoundDescriptor());
    }

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
                return PeriodicTable.getInstance().ionByName(document.getStringFromDictionary(dictionary, "ion")).getIonization();
            } else {
                return PeriodicTable.getInstance().ionByName(document.getStringFromDictionary(dictionary, "precursorIonType")).getIonization();
            }
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, Ionization annotation) {
            document.addToDictionary(dictionary, "ion", annotation.toString());
        }
    }

    private static class InsourceDescriptor implements Descriptor<InsourceFragmentation> {

        @Override
        public String[] getKeywords() {
            return new String[]{"insourceFragmentation"};
        }

        @Override
        public Class<InsourceFragmentation> getAnnotationClass() {
            return InsourceFragmentation.class;
        }

        @Override
        public <G, D, L> InsourceFragmentation read(DataDocument<G, D, L> document, D dictionary) {
            if (document.hasKeyInDictionary(dictionary, "insourceFragmentation")) return new InsourceFragmentation(true);
            else return null;
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, InsourceFragmentation annotation) {
            if (annotation!=null && annotation.isInsource()) {
                document.addToDictionary(dictionary, "insourceFragmentation", true);
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
            if (inchi!=null || inchikey!=null) return new InChI(inchikey, inchi);
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
            return PeriodicTable.getInstance().ionByName(document.getStringFromDictionary(dictionary, keywordName));
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, PrecursorIonType annotation) {
            document.addToDictionary(dictionary, keywordName, annotation.toString());
        }
    }

    private static class IsotopePatternDescriptor implements Descriptor<Ms2IsotopePattern> {

        @Override
        public String[] getKeywords() {
            return new String[]{"isotopes"};
        }

        @Override
        public Class<Ms2IsotopePattern> getAnnotationClass() {
            return Ms2IsotopePattern.class;
        }

        @Override
        public <G, D, L> Ms2IsotopePattern read(DataDocument<G, D, L> document, D dictionary) {
            final List<Peak> peaks = new ArrayList<>();
            final D isotopes = document.getDictionaryFromDictionary(dictionary, "isotopes");
            final L mzs = document.getListFromDictionary(isotopes, "mz"), ints = document.getListFromDictionary(isotopes, "relInt");
            if (mzs==null || ints==null) return null;
            for (int k=0, n=Math.min(document.sizeOfList(mzs), document.sizeOfList(ints)); k < n; ++k) {
                peaks.add(new Peak(document.getDoubleFromList(mzs, k), document.getDoubleFromList(ints, k)));
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
            document.addDictionaryToDictionary(dictionary, "isotopes", isotopes);
            document.addToDictionary(isotopes, "score", annotation.getScore());
            document.addListToDictionary(isotopes, "mz", mzs);
            document.addListToDictionary(isotopes, "relInt", ints);
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
            return new Peak(document.getDoubleFromDictionary(dictionary, "mz"), document.hasKeyInDictionary(dictionary,"relativeIntensity") ? document.getDoubleFromDictionary(dictionary, "relativeIntensity") : document.getDoubleFromDictionary(dictionary, "intensity"));
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
            final MolecularFormula formula = MolecularFormula.parse(document.getStringFromDictionary(dictionary, "molecularFormula"));
            final double mass = document.getDoubleFromDictionary(dictionary, "mz");
            final double relativeIntensity = document.hasKeyInDictionary(dictionary, "relativeIntensity") ?
                    document.getDoubleFromDictionary(dictionary, "relativeIntensity") : 0d;
            final double recalibratedMass = (document.hasKeyInDictionary(dictionary, "recalibratedMass")) ?
                    document.getDoubleFromDictionary(dictionary, "recalibratedMass") : 0d;
            final Ionization ion = PeriodicTable.getInstance().ionByName(document.getStringFromDictionary(dictionary, "ion")).getIonization();

            final ArrayList<Peak> originalPeaks = new ArrayList<Peak>();
            if (document.hasKeyInDictionary(dictionary, "peaks")) {
                final L peakList = document.getListFromDictionary(dictionary, "peaks");
                for (int i=0, n=document.sizeOfList(peakList); i < n; ++i) {
                    final D peakData = document.getDictionaryFromList(peakList, i);

                    final double intensity;
                    final double mz;
                    if (document.hasKeyInDictionary(peakData, "intensity")) {
                        intensity = document.getDoubleFromDictionary(peakData, "intensity");
                    } else intensity = document.getDoubleFromDictionary(peakData, "int");

                    originalPeaks.add(new Peak(document.getDoubleFromDictionary(peakData, "mz"), intensity));
                }
            }

            final ArrayList<CollisionEnergy> energies = new ArrayList<CollisionEnergy>();
            if (document.hasKeyInDictionary(dictionary, "collisionEnergies")) {
                if (document.hasKeyInDictionary(dictionary, "collisionEnergies")) {
                    final L energyList = document.getListFromDictionary(dictionary, "collisionEnergies");
                    for (int i=0, n=document.sizeOfList(energyList); i < n; ++i) {
                        final CollisionEnergy energy = CollisionEnergy.fromString(document.getStringFromList(energyList, i));
                        energies.add(energy);
                    }
                }
            }

            return new AnnotatedPeak(formula, mass, recalibratedMass, relativeIntensity, ion, originalPeaks.toArray(new Peak[originalPeaks.size()]), energies.toArray(new CollisionEnergy[energies.size()]));
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, AnnotatedPeak annotation) {
            if (!document.hasKeyInDictionary(dictionary, "molecularFormula"))
                document.addToDictionary(dictionary, "molecularFormula", annotation.getMolecularFormula().toString());
            document.addToDictionary(dictionary, "mz", annotation.getMass());
            document.addToDictionary(dictionary, "relativeIntensity", annotation.getRelativeIntensity());
            document.addToDictionary(dictionary, "recalibratedMass", annotation.getRecalibratedMass());
            document.addToDictionary(dictionary, "massDeviation", Deviation.fromMeasurementAndReference(annotation.getMass(), annotation.getIonization().addToMass(annotation.getMolecularFormula().getMass())).toString());
            document.addToDictionary(dictionary, "recalibratedMassDeviation", Deviation.fromMeasurementAndReference(annotation.getRecalibratedMass(), annotation.getIonization().addToMass(annotation.getMolecularFormula().getMass())).toString());
            document.addToDictionary(dictionary, "ion", annotation.getIonization().toString());

            final Peak[] peaks = annotation.getOriginalPeaks();
            final L peaklist = document.newList();
            for (Peak p : peaks) {
                final D dic = document.newDictionary();
                document.addToDictionary(dic, "mz", p.getMass());
                document.addToDictionary(dic, "intensity", p.getIntensity());
                document.addDictionaryToList(peaklist, dic);
            }
            document.addListToDictionary(dictionary, "peaks", peaklist);

            final CollisionEnergy[] energies = annotation.getCollisionEnergies();
            final L energyList = document.newList();
            for (CollisionEnergy e : energies) {
                document.addToList(energyList, e.toString());
            }
            document.addListToDictionary(dictionary, "collisionEnergies", energyList);
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

    private static class UnregardedCandidatesUpperBoundDescriptor implements Descriptor<UnregardedCandidatesUpperBound> {

        @Override
        public String[] getKeywords() {
            return new String[]{"numberOfUnregardedCandidates", "lowestConsideredCandidateScore"};
        }

        @Override
        public Class<UnregardedCandidatesUpperBound> getAnnotationClass() {
            return UnregardedCandidatesUpperBound.class;
        }

        @Override
        public <G, D, L> UnregardedCandidatesUpperBound read(DataDocument<G, D, L> document, D dictionary) {
            final int numberOfUnregardedCandidates = (int)document.getIntFromDictionary(dictionary, "numberOfUnregardedCandidates");
            final double lowestConsideredCandidateScore = document.getDoubleFromDictionary(dictionary, "lowestConsideredCandidateScore");
            return new UnregardedCandidatesUpperBound(numberOfUnregardedCandidates, lowestConsideredCandidateScore);
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, UnregardedCandidatesUpperBound annotation) {
            document.addToDictionary(dictionary, "numberOfUnregardedCandidates", annotation.getNumberOfUnregardedCandidates());
            document.addToDictionary(dictionary, "lowestConsideredCandidateScore", annotation.getLowestConsideredCandidateScore());
        }
    }
}
