package de.unijena.bioinf.ChemistryBase.ms.inputValidators;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.BasicSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

/**
 * Created by ge28quv on 05/07/17.
 */
public class MissingMergedSpectrumValidator implements Ms2ExperimentValidator {

    @Override
    public Ms2Experiment validate(Ms2Experiment input, Warning warning, boolean repair) throws InvalidException {
        MutableMs2Experiment mutableMs2Experiment = new MutableMs2Experiment(input);
        if (mutableMs2Experiment.getMs1Spectra().isEmpty()){
            if (repair){
                mutableMs2Experiment.getMs1Spectra().add(new SimpleSpectrum(new double[0], new double[0]));
            } else {
                throw new InvalidException("no MS1 given for "+mutableMs2Experiment.getName());
            }
        }
        if (mutableMs2Experiment.getMergedMs1Spectrum() == null) {
            if (repair) {
                if (mutableMs2Experiment.getMs1Spectra().size() != 1){
                    warning.warn("no merged MS1 given for "+mutableMs2Experiment.getName());
                }
                mutableMs2Experiment.setMergedMs1Spectrum(mutableMs2Experiment.getMs1Spectra().get(0));
            } else {
                throw new InvalidException("no merged MS1 given for "+mutableMs2Experiment.getName());
            }
        }
        return mutableMs2Experiment;
    }
}
