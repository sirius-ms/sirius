package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.legacy;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.DecompositionScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.io.FragmentReader;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.Arrays;
import java.util.HashMap;

public class UseInputFragmentAsScaffoldScorer implements DecompositionScorer<Object> {

	private final HashMap<MolecularFormula, Double> commonFragments;
	private final double[] commonPeaks;
	private final double peakScore;

	public UseInputFragmentAsScaffoldScorer(FragmentReader.FragmentList list, double score, double peakScore) {
		this.commonFragments = new HashMap<MolecularFormula, Double>(list.formulas.size()*2);
		this.commonPeaks = new double[list.mzs.size()];
		for (MolecularFormula formula : list.formulas) {
			commonFragments.put(formula, score);
		}
		int k=0;
		for (Double mz : list.mzs) {
			commonPeaks[k++] = mz;
		}
		Arrays.sort(commonPeaks);
		this.peakScore = peakScore;
	}

	public Object prepare(ProcessedInput input) {
		return null;
	}

    public double score(MolecularFormula formula, ProcessedPeak peak, ProcessedInput input, Object precomputed) {
    	final Double score = commonFragments.get(formula);
    	if (score != null) return score.doubleValue();
    	int k = Arrays.binarySearch(commonPeaks, peak.getMz());
    	if (k >= 0) return peakScore;
    	final Deviation dev = new Deviation(5, 1e-5, 1e-5);
    	final int insertionPoint = -k - 1;
    	if (insertionPoint < commonPeaks.length && dev.inErrorWindow(peak.getMz(), commonPeaks[insertionPoint])) {
    		return peakScore;
    	} else if (insertionPoint - 1 >= 0 && dev.inErrorWindow(peak.getMz(), commonPeaks[insertionPoint-1])) {
    		return peakScore;
    	} else if (insertionPoint -2 >= 0 && dev.inErrorWindow(peak.getMz(), commonPeaks[insertionPoint-2])) {
    		return peakScore;
    	}
    	return 0d;
    }

}
