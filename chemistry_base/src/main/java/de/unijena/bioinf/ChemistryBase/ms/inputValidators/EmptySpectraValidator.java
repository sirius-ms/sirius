package de.unijena.bioinf.ChemistryBase.ms.inputValidators;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

import java.util.ArrayList;
import java.util.List;

public class EmptySpectraValidator implements Ms2ExperimentValidator {
    @Override
    public Ms2Experiment validate(Ms2Experiment input, Warning warning, boolean repair) throws InvalidException {
        if  (!anyEmpty(input.getMs1Spectra()) & !anyEmpty(input.getMs2Spectra())){
            return input;
        }
        List<SimpleSpectrum> ms1SpectraNonEmpty = new ArrayList<>();
        List<MutableMs2Spectrum> ms2SpectraNonEmpty = new ArrayList<>();
        MutableMs2Experiment mutableMs2Experiment = new MutableMs2Experiment(input);
        for (Spectrum<Peak> spectrum : input.getMs1Spectra()) {
            if (spectrum.size()>0) ms1SpectraNonEmpty.add(new SimpleSpectrum(spectrum));
        }
        for (Ms2Spectrum<Peak> spectrum : input.getMs2Spectra()) {
            if (spectrum.size()>0) ms2SpectraNonEmpty.add(new MutableMs2Spectrum(spectrum));
        }
        mutableMs2Experiment.setMs1Spectra(ms1SpectraNonEmpty);
        mutableMs2Experiment.setMs2Spectra(ms2SpectraNonEmpty);
        return mutableMs2Experiment;
    }

    private boolean anyEmpty(List<? extends Spectrum> spectrumList){
        for (Spectrum<Peak> spectrum : spectrumList) {
            if (spectrum.size()==0) return true;
        }
        return false;
    }
}
