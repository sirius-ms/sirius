package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// TODO: Add normalization as field
@Called("Common Fragments:")
public class CommonFragmentsScore implements DecompositionScorer<Object>, MolecularFormulaScorer {

	private final HashMap<MolecularFormula, Double> commonFragments;
    private HashMap<MolecularFormula, Double> commonFragmentsH;
    private HashMap<MolecularFormula, Double> commonFragmentsWithoutH;
    private boolean hTolerance;
    private double normalization;

    public static final double COMMON_FRAGMENTS_NORMALIZATION = 0.3105875550595019d;

    public static final Object[] COMMON_FRAGMENTS = new Object[]{
        "C6H6", 3.5774489869229, "C5H9N", 3.49383484688177, "C7H6", 3.280814303199,
            "C9H10O2", 3.14213091566505, "C5H11N", 3.11702499453398, "C9H10O", 3.10851430486607,
            "C4H9N", 3.10851430486607, "C8H8O2", 3.10851430486607, "C9H9N", 3.03768825229746,
            "C5H6", 3.02847159719253, "C9H8O", 3.00977946418038, "C3H7N", 3.00030072022584,
            "C6H4", 2.98106935829795, "C11H12O", 2.98106935829795, "C6H8", 2.98106935829795,
            "C8H8O", 2.96146088690957, "C5H8", 2.9515105560564, "C11H12", 2.9210513485717,
            "C11H14", 2.90021726166885, "C10H10O2", 2.90021726166885, "C6H11N", 2.88963515233832,
            "C10H10O", 2.83497673980045, "C10H12O2", 2.83497673980045, "C8H7NO", 2.8122484887229,
            "C4H7N", 2.80068766632182, "C6H13NO", 2.74078952474075, "C11H12O2", 2.74078952474075,
            "C9H11N", 2.71578822253533, "C12H14", 2.71578822253533, "C12H10", 2.70304919675791,
            "C8H9N", 2.690145791922, "C8H13N", 2.67707371035464, "C5H7N", 2.66382848360462,
            "C4H6", 2.66382848360462, "C16H18", 2.66382848360462, "C8H8", 2.66382848360462
    };


    /*
    This is a list of fragments which are relative common (count >= 5), which have a bad chemical prior (hetero-to-carbon > 0.6),
    and which are contained in the KEGG database
     */
    private static final Object[] COMPENSATE_STRANGE_CHEMICAL_PRIOR = new Object[]{
            "C5H6N4",1.5231111245008198,
            "C5H5N5",3.754546488631306,
            "C3H4ClN5",10.68601829423076,
            "C4H4N2S",0.877725764113497,
            "C5H4N4",1.5231111245008198,
            "C4H5N3O",0.877725764113497
    };

    public static CommonFragmentsScore map(Object... values) {
        final HashMap<MolecularFormula, Double> map = new HashMap<MolecularFormula, Double>();
        for (int i=0; i < values.length; i += 2) {
            final String formula = (String)values[i];
            final double score = ((Number)values[i+1]).doubleValue();
            map.put(MolecularFormula.parse(formula), score);
        }
        return new CommonFragmentsScore(map);
    }

    public static CommonFragmentsScore getLearnedCommonFragmentScorerThatCompensateChemicalPrior() {
        return map(COMPENSATE_STRANGE_CHEMICAL_PRIOR);
    }

    public static CommonFragmentsScore getLearnedCommonFragmentScorer() {
        return getLearnedCommonFragmentScorer(1);
    }

    public Map<MolecularFormula, Double> getCommonFragments() {
        return Collections.unmodifiableMap(commonFragments);
    }

    public CommonFragmentsScore addLosses(Map<MolecularFormula, Double> losses) {
        commonFragmentsH = null;
        commonFragmentsWithoutH = null;
        final MolecularFormula[] fs = commonFragments.keySet().toArray(new MolecularFormula[0]);
        for (Map.Entry<MolecularFormula, Double> entry : losses.entrySet()) {
            for (MolecularFormula f : fs) {
                final MolecularFormula fx = f.add(entry.getKey());
                if (!commonFragments.containsKey(fx)) commonFragments.put(fx, commonFragments.get(f));
            }
        }
        return this;
    }

    public void addCommonFragment(MolecularFormula formula, double score) {
        commonFragments.put(formula, score);
        makeDirty();
    }

    private void makeDirty() {
        commonFragmentsH=null;
        commonFragmentsWithoutH=null;
    }

    private static Map<MolecularFormula, Double> mergeMaps(Map<MolecularFormula, Double> map1, Map<MolecularFormula, Double> map2, double multiplicator) {
        final HashMap<MolecularFormula, Double> merged = new HashMap<MolecularFormula, Double>(map1);
        for (Map.Entry<MolecularFormula, Double> mapEntry : map2.entrySet()) {
            for (Map.Entry<MolecularFormula, Double> entry : map1.entrySet()) {
                merged.put(mapEntry.getKey().add(entry.getKey()), (mapEntry.getValue()+entry.getValue())*multiplicator);
            }
        }
        return merged;
    }

    public static CommonFragmentsScore getLearnedCommonFragmentScorer(double scale) {
        final CommonFragmentsScore scorer = map(COMMON_FRAGMENTS);
        if (scale == 1) return scorer;
        for (Map.Entry<MolecularFormula, Double> entry : scorer.commonFragments.entrySet()) {
            entry.setValue(entry.getValue()*scale);
        }
        scorer.setNormalization(COMMON_FRAGMENTS_NORMALIZATION*scale);
        return scorer;
    }


	public CommonFragmentsScore(HashMap<MolecularFormula, Double> commonFragments) {
		this(commonFragments, COMMON_FRAGMENTS_NORMALIZATION, false);
	}

    public CommonFragmentsScore(HashMap<MolecularFormula, Double> commonFragments, double normalization, boolean hTolerance) {
        this.commonFragments = commonFragments;
        if (hTolerance) useHTolerance();
        this.normalization = normalization;
    }

    public CommonFragmentsScore() {
        this.commonFragments = new HashMap<MolecularFormula, Double>();
    }

    public double getNormalization() {
        return normalization;
    }

    public void setNormalization(double normalization) {
        this.normalization = normalization;
    }

    public CommonFragmentsScore useHTolerance() {
        if (hTolerance) return this;
        hTolerance = true;
        final MolecularFormula h = MolecularFormula.parse("H");
        final MolecularFormula[] forms = commonFragments.keySet().toArray(new MolecularFormula[0]);
        for (MolecularFormula f : forms) {
            final double score = commonFragments.get(f);
            final MolecularFormula fh = f.add(h);
            final MolecularFormula fn = f.subtract(h);
            final Double fhScore = commonFragments.get(fh);
            final Double fnScore = commonFragments.get(fn);
            commonFragments.put(fh, Math.max(score, fhScore == null ? 0 : fhScore));
            commonFragments.put(fn, Math.max(score, fnScore == null ? 0 : fnScore));
        }
        return this;
    }

    protected HashMap<MolecularFormula, Double> getCommonFragmentsWithoutH() {
        if (commonFragmentsWithoutH == null) {
            commonFragmentsWithoutH = new HashMap<MolecularFormula, Double>((int)(commonFragments.size()*1.4d));
            final MolecularFormula h = MolecularFormula.parse("H");
            for (Map.Entry<MolecularFormula, Double> entry : commonFragments.entrySet()) {
                commonFragmentsWithoutH.put(entry.getKey().subtract(h), entry.getValue());
            }
        }
        return commonFragmentsWithoutH;
    }

    protected HashMap<MolecularFormula, Double> getCommonFragmentsH() {
        if (commonFragmentsH == null) {
            commonFragmentsH = new HashMap<MolecularFormula, Double>((int)(commonFragments.size()*1.4d));
            final MolecularFormula h = MolecularFormula.parse("H");
            for (Map.Entry<MolecularFormula, Double> entry : commonFragments.entrySet()) {
                commonFragmentsH.put(entry.getKey().add(h), entry.getValue());
            }
        }
        return commonFragmentsH;
    }

    public Object prepare(ProcessedInput input) {
		return null;
	}

	@Override
	public double score(MolecularFormula formula, ProcessedPeak peak, ProcessedInput input, Object precomputed) {
        final Ionization ion = input.getExperimentInformation().getIonization();
        final Double intrinsic = commonFragments.get(formula);
        final double intr = intrinsic != null ? intrinsic.doubleValue() : 0;
        if (!hTolerance && ion instanceof Charge) {
            final Double score = (ion.getCharge() > 0 ? getCommonFragmentsH().get(formula) : getCommonFragmentsWithoutH().get(formula));
            return (score == null ? intr : Math.max(intr, score.doubleValue())) - normalization;
        } else {
            return intr - normalization;
        }
    }

    @Override
    public double score(MolecularFormula formula) {
        final Double val = commonFragments.get(formula);
        if (val == null) return -normalization;
        else return val.doubleValue()-normalization;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        commonFragments.clear();
        final Iterator<Map.Entry<String, G>> iter = document.iteratorOfDictionary(document.getDictionaryFromDictionary(dictionary, "fragments"));
        while (iter.hasNext()) {
            final Map.Entry<String,G> entry = iter.next();
            commonFragments.put(MolecularFormula.parse(entry.getKey()), document.getDouble(entry.getValue()));
        }
        commonFragmentsH = null;
        commonFragmentsWithoutH = null;
        if (document.getBooleanFromDictionary(dictionary, "hTolerance")) useHTolerance();
        normalization = document.getDoubleFromDictionary(dictionary, "normalization");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final D common = document.newDictionary();
        for (Map.Entry<MolecularFormula, Double> entry : commonFragments.entrySet()) {
            document.addToDictionary(common, entry.getKey().toString(), entry.getValue());
        }
        document.addDictionaryToDictionary(dictionary, "fragments", common);
        document.addToDictionary(dictionary, "hTolerance", hTolerance);
        document.addToDictionary(dictionary, "normalization", normalization);
    }

}
