package de.unijena.bioinf.sirius.preprocessing.ions;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.IonMode;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.ms.annotations.Provides;
import de.unijena.bioinf.ms.annotations.Requires;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Search for m/z delta in MS1
 */
@Requires(MergedMs1Spectrum.class)
@Provides(PossibleIonModes.class)
public class DetectIonsFromMs1 implements IonModeDetection {

    @Nullable
    @Override
    public PossibleIonModes detect(ProcessedInput processedInput, Set<IonMode> candidates) {
        MergedMs1Spectrum ms1 = processedInput.getAnnotationOrThrow(MergedMs1Spectrum.class);
        final MS1MassDeviation dev = processedInput.getAnnotationOrDefault(MS1MassDeviation.class);
        SimpleMutableSpectrum mutableSpectrum;
        Ms2Experiment exp = processedInput.getExperimentInformation();
        if (ms1.isCorrelated() && ms1.mergedSpectrum.size()>1) {
            mutableSpectrum = new SimpleMutableSpectrum(ms1.mergedSpectrum);
            Spectrums.filterIsotpePeaks(mutableSpectrum, new Deviation(100), 1, 2, 5, new ChemicalAlphabet());
        } else {
            if (exp.getMergedMs1Spectrum().size()>0){
                SimpleSpectrum pks = Spectrums.selectSpectrumWithMostIntensePrecursor(exp.getMs1Spectra(), exp.getIonMass(), dev.allowedMassDeviation);
                mutableSpectrum = new SimpleMutableSpectrum(pks==null ? exp.getMs1Spectra().get(0) : pks);
            } else return null;
        }

        Spectrums.normalizeToMax(mutableSpectrum, 100d);
        Spectrums.applyBaseline(mutableSpectrum, 1d);
//        //changed
        Spectrums.filterIsotpePeaks(mutableSpectrum, dev.massDifferenceDeviation, 0.3, 1, 5, new ChemicalAlphabet());

        PrecursorIonType[] ionType = Spectrums.guessIonization(mutableSpectrum, exp.getIonMass(), dev.allowedMassDeviation, candidates.stream().map(PrecursorIonType::getPrecursorIonType).toArray(PrecursorIonType[]::new));
        return PossibleIonModes.uniformlyDistributed(Arrays.stream(ionType).map(u->(IonMode)u.getIonization()).collect(Collectors.toList()));

    }
}
