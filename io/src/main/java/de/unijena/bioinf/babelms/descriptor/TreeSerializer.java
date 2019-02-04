package de.unijena.bioinf.babelms.descriptor;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ms.annotations.DataAnnotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Serializes the tree into a JSON document.
 *
 * Whats new?
 * - all annotations are always written or read. We do not longer "check for certain keys in the json document".
 * - this implies that every annotation type needs a "neutral" or null element. For example, Recalibration::none
 *
 * I did not try to make this class somehow "extendable" because I do not think that it is a good idea to put everything
 * as annotation into FTree. We should rather annotate the Experiment Objekt instead of the Tree and only add tree annotations
 * for stuff we either want to inspect from the tree (e.g. display in the tree renderer) or use for kernel methods
 */
public class TreeSerializer {

    Descriptor[] treeDescriptors, fragmentDescriptors, lossDescriptors;

    public TreeSerializer() {
        registerAnnotations();
    }

    public <G,D,L> void annotateTree(FTree tree, DataDocument<G,D,L> document, D fragmentNode) {
        for (Descriptor d : treeDescriptors) {
            d.write.write(document, fragmentNode, tree, tree.getAnnotation(d.annotationClass));
        }
    }

    public <G,D,L> void annotateFragment(FTree tree, Fragment f, DataDocument<G,D,L> document, D fragmentNode) {
        for (Descriptor d : fragmentDescriptors) {
            d.write.write(document, fragmentNode, tree, (DataAnnotation) tree.getFragmentAnnotationOrNull(d.annotationClass).get(f));
        }
    }
    public <G,D,L> void annotateLoss(FTree tree, Loss l, DataDocument<G,D,L> document, D fragmentNode) {
        for (Descriptor d : fragmentDescriptors) {
            d.write.write(document, fragmentNode, tree, (DataAnnotation) tree.getLossAnnotationOrNull(d.annotationClass).get(l));
        }
    }

    public void registerAnnotations() {
        // annotations for the tree

        treeDescriptors = new Descriptor[]{
                string(PrecursorIonType.class, "precursorIonType", PrecursorIonType::fromString, PrecursorIonType::toString),
                string(DataSource.class, "source", DataSource::fromString, DataSource::toString),
                string(Beautified.class, "beautified", Beautified::fromString, Beautified::toString),
                string(IonTreeUtils.Type.class, "treeType", IonTreeUtils.Type::valueOf, IonTreeUtils.Type::toString),
                string(RecalibrationFunction.class, "recalibration", RecalibrationFunction::fromString, RecalibrationFunction::toString),
                inplace(TreeStatistics.class, this::readTreeStatistics, this::writeTreeStatistics),
                inplace(UnconsideredCandidatesUpperBound.class, this::readUnconsideredUpperbound, this::writeUnconsideredUpperbound),
                inplace(ZodiacScore.class, this::readZodiacScore, this::writeZodiacScore),

                inplace(InChI.class, this::readInChI, this::writeInChI),
                string(Smiles.class, "smiles", Smiles::new, Smiles::toString),

        };

        // annotations for the fragments
        fragmentDescriptors = new Descriptor[]{
                inplace(AnnotatedPeak.class, this::readAnnotatedPeak, this::writeAnnotatedPeak),
                inplace(Score.class, this::readScores, this::writeScores),
                inplace(Ms1IsotopePattern.class, this::readIsotopes, this::writeIsotopes)
        };

        // annotations for losses
        lossDescriptors = new Descriptor[]{
                inplace(Score.class, this::readScores, this::writeScores),
                bool(InsourceFragmentation.class, "in-source", InsourceFragmentation.yes(), InsourceFragmentation.no())
        };

    }

    /*
    READ AND WRITE FUNCTIONS
     */


    protected <G,D,L> TreeStatistics readTreeStatistics(DataDocument<G,D,L> doc, D d, FTree tree) {
        return new TreeStatistics(doc.getDoubleFromDictionary(d,"explainedIntensity"), doc.getDoubleFromDictionary(d, "explainedIntensityOfExplainablePeaks"), doc.getDoubleFromDictionary(d, "ratioOfExplainedPeaks"));
    }
    protected <G,D,L> void writeTreeStatistics(DataDocument<G,D,L> doc, D d, FTree tree, TreeStatistics v) {
        doc.addToDictionary(d, "explainedIntensity", v.getExplainedIntensity());
        doc.addToDictionary(d, "explainedIntensityOfExplainablePeaks", v.getExplainedIntensityOfExplainablePeaks());
        doc.addToDictionary(d, "ratioOfExplainedPeaks", v.getRatioOfExplainedPeaks());
    }
    protected <G,D,L> UnconsideredCandidatesUpperBound readUnconsideredUpperbound(DataDocument<G,D,L> doc, D d, FTree tree) {
        if (doc.hasKeyInDictionary(d, "numberOfUnconsideredCandidates")) {
            return new UnconsideredCandidatesUpperBound((int)doc.getIntFromDictionary(d, "numberOfUnconsideredCandidates"),
                    doc.getDoubleFromDictionary(d, "lowestConsideredCandidateScore"));
        } else {
            return UnconsideredCandidatesUpperBound.noRankingInvolved();
        }
    }
    protected <G,D,L> void writeUnconsideredUpperbound(DataDocument<G,D,L> doc, D d, FTree tree, UnconsideredCandidatesUpperBound v) {
        if (!v.isNoRankingInvolved()) {
            doc.addToDictionary(d, "numberOfUnconsideredCandidates", v.getNumberOfUnconsideredCandidates());
            doc.addToDictionary(d, "lowestConsideredCandidateScore", v.getLowestConsideredCandidateScore());
        }
    }

    protected <G,D,L> ZodiacScore readZodiacScore(DataDocument<G,D,L> doc, D d, FTree tree) {
        return new ZodiacScore(doc.getDoubleFromDictionary(d, "zodiacProbability"));
    }
    protected <G,D,L> void writeZodiacScore(DataDocument<G,D,L> doc, D d, FTree tree, ZodiacScore v) {
        doc.addToDictionary(d, "zodiacProbability", v.getProbability());
    }

    protected <G,D,L> InChI readInChI(DataDocument<G,D,L> doc, D d, FTree tree) {
        return new InChI(doc.getStringFromDictionary(d, "inchikey"), doc.getStringFromDictionary(d,"inchi"));
    }
    protected <G,D,L> void writeInChI(DataDocument<G,D,L> doc, D d, FTree tree, InChI v) {
        if (v.in3D!=null) doc.addToDictionary(d, "inchi", v.in3D);
        if (v.key!=null) doc.addToDictionary(d, "inchikey", v.key);
    }

    protected <G,D,L> AnnotatedPeak readAnnotatedPeak(DataDocument<G,D,L> document, D dictionary, FTree tree) {
        final MolecularFormula formula = document.hasKeyInDictionary(dictionary, "molecularFormula") ? MolecularFormula.parse(document.getStringFromDictionary(dictionary, "molecularFormula")) : null;
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
    protected <G,D,L> void writeAnnotatedPeak(DataDocument<G,D,L> document, D dictionary, FTree tree, AnnotatedPeak annotation) {
        if (annotation.getMolecularFormula()!=null)
            document.addToDictionary(dictionary, "molecularFormula", annotation.getMolecularFormula().toString());
        document.addToDictionary(dictionary, "mz", annotation.getMass());
        document.addToDictionary(dictionary, "relativeIntensity", annotation.getRelativeIntensity());
        document.addToDictionary(dictionary, "recalibratedMass", annotation.getRecalibratedMass());
        document.addToDictionary(dictionary, "massDeviation", Deviation.fromMeasurementAndReference(annotation.getMass(), annotation.getIonization().addToMass(annotation.getMolecularFormula().getMass())).toString());
        if (annotation.getRecalibratedMass()>0)
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

    private final WeakHashMap<String[], String[]> constantPool = new WeakHashMap<String[], String[]>();

    public <G, D, L> Score readScores(DataDocument<G, D, L> document, D dictionary, FTree tree) {
        if (!document.hasKeyInDictionary(dictionary, "scores"))
            return Score.none();
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


    public <G, D, L> void writeScores(DataDocument<G, D, L> document, D dictionary, FTree tree, Score annotation) {
        if (annotation.isEmpty()) return;
        final D scoredict = document.newDictionary();
        for (Map.Entry<String,Double> entry : annotation.asMap().entrySet()) {
            document.addToDictionary(scoredict, entry.getKey(), entry.getValue());
        }
        document.addToDictionary(dictionary, "score", annotation.sum());
        document.addDictionaryToDictionary(dictionary, "scores", scoredict);
    }

    public <G, D, L> Ms1IsotopePattern readIsotopes(DataDocument<G, D, L> document, D dictionary, FTree tree) {
        if (!document.hasKeyInDictionary(dictionary, "isotopes" ))
            return Ms1IsotopePattern.none();
        final List<Peak> peaks = new ArrayList<>();
        final D isotopes = document.getDictionaryFromDictionary(dictionary, "isotopes");
        final L mzs = document.getListFromDictionary(isotopes, "mz"), ints = document.getListFromDictionary(isotopes, "relInt");
        if (mzs==null || ints==null) return null;
        for (int k=0, n=Math.min(document.sizeOfList(mzs), document.sizeOfList(ints)); k < n; ++k) {
            peaks.add(new Peak(document.getDoubleFromList(mzs, k), document.getDoubleFromList(ints, k)));
        }

        double score = document.hasKeyInDictionary(isotopes, "score") ? document.getDoubleFromDictionary(isotopes, "score") : 0d;
        return new Ms1IsotopePattern(peaks.toArray(new Peak[peaks.size()]), score);
    }


    public <G, D, L> void writeIsotopes(DataDocument<G, D, L> document, D dictionary,FTree tree, Ms1IsotopePattern annotation) {
        if (annotation.isEmpty()) return;
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


    public <T extends DataAnnotation> Descriptor<T> bool(Class<T> klass, String keyName, T trueOne, T falseOne) {
        return string(klass, keyName, s->s.equals("yes") ? trueOne : falseOne, u->u.equals(trueOne)?"yes":"no");
    }

    public <T extends DataAnnotation> Descriptor<T> string(Class<T> klass, String keyName, ReadFromStringFunction<T> read, WriteToStringFunction<T> write) {
        return new Descriptor<T>(klass, new ReadFromStringImpl<>(read, keyName), new WriteToStringImpl<>(write, keyName));
    }

    public <T extends DataAnnotation> Descriptor<T> dictionary(Class<T> klass, String keyName, ReadFunction<T> read, WriteFunction<T> write) {
        return new Descriptor<>(klass, new ReadToDictImpl<>(read, keyName), new WriteToDictImpl<>(write, keyName));
    }

    public <T extends DataAnnotation> Descriptor<T> inplace(Class<T> klass, ReadFunction<T> read, WriteFunction<T> write) {
        return new Descriptor<T>(klass, read, write);
    }

    protected static class Descriptor<T extends DataAnnotation> {
        private final Class<T> annotationClass;
        private final ReadFunction<T> read;
        private final WriteFunction<T> write;

        public Descriptor(Class<T> annotationClass, ReadFunction<T> read, WriteFunction<T> write) {
            this.annotationClass = annotationClass;
            this.read = read;
            this.write = write;
        }
    }



    protected static interface ReadFunction<T extends DataAnnotation> {
        public <G, D, L> T read(DataDocument<G,D,L> document, D container, FTree tree);
    }

    protected static interface WriteFunction<T extends DataAnnotation> {
        public <G, D, L> void write(DataDocument<G,D,L> document, D container, FTree tree, T value);
    }

    protected static interface ReadFromStringFunction<T extends DataAnnotation> {
        public T read(String value);
    }
    protected static interface WriteToStringFunction<T extends DataAnnotation> {
        public String write(T value);
    }

    protected static class ReadFromStringImpl<T extends DataAnnotation> implements ReadFunction<T>  {
        private final ReadFromStringFunction<T> wrapped;
        private final String key;

        public ReadFromStringImpl(ReadFromStringFunction<T> wrapped, String key) {
            this.wrapped = wrapped;
            this.key = key;
        }

        @Override
        public <G, D, L> T read(DataDocument<G, D, L> document, D container, FTree tree) {
            return wrapped.read(document.getStringFromDictionary(container, key));
        }
    }
    protected static class WriteToStringImpl<T extends DataAnnotation> implements WriteFunction<T>  {
        private final WriteToStringFunction<T> wrapped;
        private final String key;

        public WriteToStringImpl(WriteToStringFunction<T> wrapped, String key) {
            this.wrapped = wrapped;
            this.key = key;
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D container, FTree tree, T value) {
            document.addToDictionary(container, key, wrapped.write(value));
        }
    }

    protected static class WriteToDictImpl<T extends DataAnnotation> implements WriteFunction<T>  {
        private final WriteFunction<T> wrapper;
        private final String key;

        public WriteToDictImpl(WriteFunction<T> wrapper, String key) {
            this.wrapper = wrapper;
            this.key = key;
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D container, FTree tree, T value) {
            D d = document.newDictionary();
            wrapper.write(document, d, tree, value);
            document.addDictionaryToDictionary(container, key, d);
        }
    }
    protected static class ReadToDictImpl<T extends DataAnnotation> implements ReadFunction<T>  {
        private final ReadFunction<T> wrapper;
        private final String key;

        public ReadToDictImpl(ReadFunction<T> wrapper, String key) {
            this.wrapper = wrapper;
            this.key = key;
        }

        @Override
        public <G, D, L> T read(DataDocument<G, D, L> document, D container, FTree tree) {
            return wrapper.read(document, document.getDictionaryFromDictionary(container, key), tree);
        }
    }

}
