package de.unijena.bioinf.FragmentationTreeConstruction.computation.merging;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

/**
 * A strategy to merge peaks from different spectra. It is guaranteed, that all peaks which are in the merge window are
 * from different spectra. Merging is done by picking all peaks in the same merge window and calculate a new mass
 * which represents them all. Their intensities are summed up by the merger.
 *
 * Important: Be careful with the parent peak! Use {{@link Ms2Experiment#getIonMass}} to get the parent mass.
 * Don't merge the best fitting parent peak away!
 */
public interface PeakMerger {

    public void mergePeaks(List<ProcessedPeak> peaks, Ms2Experiment experiment, Deviation mergeWindow,  Merger merger);

}
