package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2ExperimentImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2SpectrumImpl;

import java.util.ArrayList;
import java.util.List;

public class NormalizeToSumPreprocessor implements Preprocessor {


    @Override
    public Ms2Experiment process(Ms2Experiment experiment) {
        List<? extends Ms2Spectrum> specs = experiment.getMs2Spectra();
        final ArrayList<Ms2Spectrum> spectra = new ArrayList<Ms2Spectrum>(specs.size());
        for (Ms2Spectrum spec : specs) {
            final SimpleMutableSpectrum ms = new SimpleMutableSpectrum(spec);
            Spectrums.normalize(ms, Normalization.Sum(100d));
            final Ms2SpectrumImpl ms2 = new Ms2SpectrumImpl(ms, spec.getCollisionEnergy(), spec.getPrecursorMz(), spec.getTotalIonCount());
            spectra.add(ms2);
        }
        final Ms2ExperimentImpl exp = new Ms2ExperimentImpl(experiment);
        exp.setMs2Spectra(spectra);
        return exp;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        // nothing
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        // nothing
    }
}
