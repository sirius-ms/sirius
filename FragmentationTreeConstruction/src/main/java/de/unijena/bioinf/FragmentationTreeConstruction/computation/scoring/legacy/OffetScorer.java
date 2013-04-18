package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.legacy;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.legacy.AbstractDecompositionScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.graph.format.ScoreName;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

@ScoreName("offset")
public class OffetScorer extends AbstractDecompositionScorer {
	
	private final double scoreOffset;
	
	public OffetScorer(double scoreOffset) {
		this.scoreOffset = scoreOffset;
	}
	
	@Override
	public double score(MolecularFormula formula, ProcessedPeak peak,
			ProcessedInput input) {
		return scoreOffset;
	}



}
