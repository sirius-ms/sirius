package de.unijena.bioinf.FragmentationTreeConstruction.computation.msn;

import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

public interface MSNClosure {
	
	public double score(ProcessedPeak left, ProcessedPeak right);
	
}
