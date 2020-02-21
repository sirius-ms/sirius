package de.unijena.bioinf.ms.gui.ms_viewer.data;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

public class SiriusMergedMs2Annotated extends SiriusSingleSpectrumAnnotated {

    public SiriusMergedMs2Annotated(FTree tree, Ms2Experiment experiment, double minMz, double maxMz) {
        super(tree, merge(experiment, tree), minMz, maxMz);
    }

    public SiriusMergedMs2Annotated(FTree tree, Ms2Experiment experiment) {
        super(tree, merge(experiment, tree));
    }

    private static Spectrum<? extends Peak> merge(Ms2Experiment experiment, FTree tree) {
//        ProcessedInput processedInput = tree.getAnnotationOrNull(ProcessedInput.class);
        System.out.println("Implement merged peaks stuff"); //todo reimplement
       /* if (false){
            List<ProcessedPeak> peakList = tree.getFragmentAnnotationOrNull(AnnotatedPeak.class).;
            SimpleMutableSpectrum spectrum = new SimpleMutableSpectrum();
            for (ProcessedPeak processedPeak : peakList) {
                spectrum.addPeak(processedPeak);
            }
            return spectrum;
        }*/
        return Spectrums.mergeSpectra(new Deviation(10, 0.1), true, false, experiment.getMs2Spectra());
    }
}
