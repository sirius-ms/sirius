package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.HashMap;
import java.util.Map;

@Called("Common Fragments:")
public class CommonFragmentsScore implements DecompositionScorer<Object> {

	private final HashMap<MolecularFormula, Double> commonFragments;
    private HashMap<MolecularFormula, Double> commonFragmentsH;
    private HashMap<MolecularFormula, Double> commonFragmentsWithoutH;
    private boolean hTolerance;

    private static final Object[] Fragments = new Object[]{
    "C6H6O", 0.15, "C5H5N", 0.15, "C8H6N", 0.15, "C6H9O", 0.15, "C5H7O", 0.155, "C6H5O", 0.155,
            "C4H10N", 0.155, "C5H10N", 0.155, "C8H13", 0.155, "C4H5O2", 0.16, "C6H7O", 0.16, "C3H2O", 0.16,
            "C6H7N", 0.165, "C3H6", 0.165, "C5H4N", 0.165, "C6H8N", 0.17, "C7H6N", 0.175, "C4H7N", 0.175, "C8H11", 0.175,
            "C8H7N", 0.18, "C7H8N", 0.18, "C4H6", 0.18, "C8H8N", 0.185, "C6H11", 0.185, "C4H6NO", 0.195,
            "C3H7O", 0.195, "C7H11", 0.205, "C5H5O", 0.215, "C4H5N", 0.215, "C6H6N", 0.215, "C4H4", 0.215,
            "C4H7O", 0.22, "CH2N", 0.225, "C3H7N", 0.225, "C4H5O", 0.23, "C8H9", 0.23, "C5H8N", 0.23, "C5H3", 0.235,
            "C2H2O", 0.235, "C6H4", 0.24, "C9H7", 0.245, "C4H2", 0.245, "C8H6", 0.245, "C7H7O", 0.255, "C3H4N", 0.265,
            "C4H9", 0.27, "C3H2", 0.27, "C5H9", 0.275, "C3H5N", 0.275, "C3H8N", 0.285, "C6H9", 0.285, "C7H5", 0.285,
            "C5H4", 0.29, "C5H6", 0.295, "C4H8N", 0.3, "C4H6N", 0.305, "C7H9", 0.31, "C2H5", 0.31, "C7H6", 0.315,
            "C5H6N", 0.32, "CH4N", 0.33, "C2H5N", 0.33, "C2H3N", 0.34, "C6H6", 0.345, "C3H4", 0.345, "C8H7", 0.345,
            "C3H5O", 0.385, "C2H5O", 0.4, "C3H7", 0.42, "C5H7", 0.425, "C4H7", 0.475, "C2H6N", 0.515, "C3H3O", 0.545,
            "C4H5", 0.56, "C3H6N", 0.565, "C4H3", 0.58, "C6H7", 0.605, "C7H7", 0.615, "C2H3O", 0.655, "C5H5", 0.685,
            "C2H4N", 0.69, "C6H5", 0.74, "C3H3", 0.75, "C3H5", 0.9
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
        final CommonFragmentsScore scorer = map(Fragments);
        if (scale == 1) return scorer;
        for (Map.Entry<MolecularFormula, Double> entry : scorer.commonFragments.entrySet()) {
            entry.setValue(entry.getValue()*scale);
        }
        return scorer;
    }


	public CommonFragmentsScore(HashMap<MolecularFormula, Double> commonFragments) {
		this(commonFragments, false);
	}

    public CommonFragmentsScore(HashMap<MolecularFormula, Double> commonFragments, boolean hTolerance) {
        this.commonFragments = commonFragments;
        if (hTolerance) useHTolerance();
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
            return score == null ? intr : Math.max(intr, score.doubleValue());
        } else {
            return intr;
        }
    }

}
