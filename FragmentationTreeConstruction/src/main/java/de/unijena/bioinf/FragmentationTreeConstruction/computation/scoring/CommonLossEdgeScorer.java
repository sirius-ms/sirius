package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ImmutableParameterized;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.*;

public class CommonLossEdgeScorer implements LossScorer{

    private final HashMap<MolecularFormula, Double> commonLosses;
    private HashMap<MolecularFormula, Double> recombinatedList;
    private double normalization;
    private Recombinator recombinator;

    public CommonLossEdgeScorer() {
        this(Collections.<MolecularFormula, Double>emptyMap(), null);
    }

    public CommonLossEdgeScorer(Map<MolecularFormula, Double> commonLosses, Recombinator recombinator, double normalization) {
        this.commonLosses = new HashMap<MolecularFormula, Double>(commonLosses);
        this.recombinatedList = null;
        this.normalization = normalization;
        this.recombinator = recombinator;
    }


    public CommonLossEdgeScorer(Map<MolecularFormula, Double> commonLosses, Recombinator recombinator) {
        this(commonLosses, recombinator, 0d);
    }


    private final static String[] implausibleLosses = new String[]{"C2O", "C4O", "C3H2", "C5H2", "C7H2", "N", "C"};

    public final static String[] ales_list = new String[]{
            "H2", "H2O", "CH4", "C2H4", "C2H2",
            "C4H8", "C5H8", "C6H6", "CH2O",
            "CO", "CH2O2", "CO2", "C2H4O2",
            "C2H2O", "C3H6O2", "C3H4O4",
            "C3H2O3", "C5H8O4", "C6H10O5",
            "C6H8O6", "NH3", "CH5N",
            "CH3N", "C3H9N", "CHNO", "CH4N2O",
            "H3PO3", "H3PO4", "HPO3", "C2H5O4P",
            "H2S", "S", "SO2", "SO3", "H2SO4"
    };

    /**
     * If you have no clue about the correct score of your common losses, you can assume that they are all equally distributed.
     * In this case, you have to ignore the loss size scoring, e.g. if H2 and C6H6 have the same frequency, they are not
     * allowed to get different scores. We do this by adding the negative loss size score as common loss score. Therefore,
     * when adding the loss size score later, both scores are sumed up to 0.
     * Nevertheless: Maybe it is not wrong to say: C6H6 is "nicer" than H2, as it contains more information. Therefore,
     * you can add only e.g. 70% of the negative loss size score to the common loss score.
     * @param lossSizeScorer
     * @param compensation multiplicator with loss size score
     * @return
     */
    public static CommonLossEdgeScorer getLossSizeCompensationForExpertList(LossSizeScorer lossSizeScorer, double compensation) {
        final CommonLossEdgeScorer scorer = new CommonLossEdgeScorer();
        for (String f : ales_list) {
            final MolecularFormula m = MolecularFormula.parse(f);
            scorer.addCommonLoss(m, -(lossSizeScorer.score(m)+lossSizeScorer.getNormalization())*compensation);
        }
        return scorer;
    }

    /**
     *
     * @param penalty
     * @return
     */
    public CommonLossEdgeScorer addImplausibleLosses(double penalty) {
        for (String f : implausibleLosses) {
            addCommonLoss(MolecularFormula.parse(f), penalty);
        }
        return this;
    }

    public Map<MolecularFormula, Double> getCommonLosses() {
        return Collections.unmodifiableMap(commonLosses);
    }


    @Override
    public Object prepare(ProcessedInput input) {
        if (recombinatedList==null) recombinatedList = recombinator==null ? new HashMap<MolecularFormula, Double>()
                : recombinator.recombinate(Collections.unmodifiableMap(commonLosses), normalization);
        return null;
    }

    public void addCommonLoss(MolecularFormula loss, double score) {
        commonLosses.put(loss, score);
        recombinatedList = null;
    }

    public void clearLosses() {
        commonLosses.clear();
        recombinatedList = null;
    }

    public Recombinator getRecombinator() {
        return recombinator;
    }

    public double getNormalization() {
        return normalization;
    }

    public void setNormalization(double normalization) {
        this.normalization = normalization;
    }

    public void merge(HashMap<MolecularFormula, Double> map) {
        this.commonLosses.putAll(map);
        recombinatedList = null;
    }

    public void merge(CommonLossEdgeScorer lossScorer) {
        merge(lossScorer.commonLosses);
    }

    public void setRecombinator(Recombinator recombinator) {
        this.recombinator = recombinator;
        recombinatedList = null;
    }

    public double score(MolecularFormula formula) {
        Double score = recombinatedList.get(formula);
        if (score!=null) return score.doubleValue() - normalization;
        score = commonLosses.get(formula);
        if (score!=null) return score.doubleValue() - normalization;
        return -normalization;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        return score(loss.getFormula());
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final Iterator<Map.Entry<String,G>> iter = document.iteratorOfDictionary(document.getDictionaryFromDictionary(dictionary, "losses"));
        clearLosses();
        while (iter.hasNext()) {
            final Map.Entry<String, G> entry = iter.next();
            commonLosses.put(MolecularFormula.parse(entry.getKey()), document.getDouble(entry.getValue()));
        }
        this.normalization = document.getDoubleFromDictionary(dictionary, "normalization");
        if (document.hasKeyInDictionary(dictionary, "recombinator"))
            this.recombinator = (Recombinator)helper.unwrap(document, document.getFromDictionary(dictionary, "recombinator"));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final D common = document.newDictionary();
        for (Map.Entry<MolecularFormula, Double> entry : commonLosses.entrySet()) {
            document.addToDictionary(common, entry.getKey().toString(), entry.getValue());
        }
        document.addDictionaryToDictionary(dictionary, "losses", common);
        if (recombinator != null)
            document.addToDictionary(dictionary, "recombinator", helper.wrap(document, recombinator));
        document.addToDictionary(dictionary, "normalization", normalization);
    }

    /**
     * A recombinator extends the list of common losses by combination of losses
     */
    public interface Recombinator extends ImmutableParameterized<Recombinator> {
        public HashMap<MolecularFormula, Double> recombinate(Map<MolecularFormula, Double> source, double normalizationConstant);
    }

    public static class LegacyOldSiriusRecombinator implements Recombinator {


        @Override
        public HashMap<MolecularFormula, Double> recombinate(Map<MolecularFormula, Double> source, double normalizationConstant) {
            final ArrayList<MolecularFormula> losses = new ArrayList<MolecularFormula>(source.keySet());
            final HashMap<MolecularFormula, Double> recs = new HashMap<MolecularFormula, Double>();
            List<MolecularFormula> src = new ArrayList<MolecularFormula>(losses);
            final double gamma = Math.exp(source.get(Math.log(10)));
            for (int i=1; i <=3; ++i) {
                final double score = Math.log(gamma/i);
                final ArrayList<MolecularFormula> newSrc = new ArrayList<MolecularFormula>();
                for (MolecularFormula f : losses) {
                    for (MolecularFormula g : src) {
                        newSrc.add(f.add(g));
                    }
                }
                src = newSrc;
                for (MolecularFormula f : src) recs.put(f, score);
            }
            return recs;
        }

        @Override
        public <G, D, L> Recombinator readFromParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            return new LegacyOldSiriusRecombinator();
        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        }
    }

    /**
     * The LossSizeRecombinator recombinates by the following strategy:
     * 1. calculate the final score of both common loss (after adding loss size prior and normalizations)
     * 2. use the average of both scores as new common loss score
     * 3. subtract the loss size score from this score, such that the loss size of the resulting loss is not considered
     *    (as we expect not one loss with this size but two consecutive losses instead)
     * 4. add a small penalty for not observing the intermediate fragment
     */
    public static class LossSizeRecombinator implements Recombinator {

        private final double penalty;
        private final LossSizeScorer lossSizeScorer;


        LossSizeRecombinator() {
            this(null, 0d);
        }

        public LossSizeRecombinator(LossSizeScorer scorer, double penalty) {
            this.penalty = penalty;
            this.lossSizeScorer = scorer;
        }

        @Override
        public HashMap<MolecularFormula, Double> recombinate(Map<MolecularFormula, Double> source, double normalizationConstant) {
            final HashMap<MolecularFormula, Double> recombination = new HashMap<MolecularFormula, Double>(source.size()*source.size());
            final List<MolecularFormula> sourceList = new ArrayList<MolecularFormula>(source.keySet());
            for (int i=0; i < sourceList.size(); ++i) {
                final MolecularFormula a = sourceList.get(i);
                final double aScore = lossSizeScorer.score(a) + source.get(a) - normalizationConstant ;
                for (int j=i; j < sourceList.size(); ++j) {
                    final MolecularFormula b = sourceList.get(j);
                    final double bScore = lossSizeScorer.score(b) + source.get(b) - normalizationConstant;
                    final MolecularFormula combination = a.add(b);
                    final double abScore = lossSizeScorer.score(combination);
                    final double combinatedScore = (aScore + bScore) -abScore + normalizationConstant + penalty;
                    if (combinatedScore > 0d ) {
                        Double sc = source.get(combination);
                        if (sc == null || sc.doubleValue() < combinatedScore) {
                            recombination.put(combination, combinatedScore);
                        }
                    }
                }
            }
            return recombination;
        }

        @Override
        public <G, D, L> Recombinator readFromParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            return new LossSizeRecombinator((LossSizeScorer)helper.unwrap(document,document.getFromDictionary(dictionary,"lossSize")), document.getDoubleFromDictionary(dictionary,"penalty"));
        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            document.addToDictionary(dictionary, "penalty", penalty);
            document.addToDictionary(dictionary, "lossSize", helper.wrap(document,lossSizeScorer));
        }
    }

}
