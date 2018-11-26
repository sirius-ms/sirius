package de.unijena.bioinf.sirius.ionGuessing;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import org.jetbrains.annotations.NotNull;

public class IonGuesser {
    /**
     * try to guess ionization from MS1. multiple  suggestions possible. In doubt [M]+ is ignored (cannot distinguish from isotope pattern)!
     *
     * @param experiment
     * @param candidateIonizations array of possible ionizations (lots of different adducts very likely make no sense!)
     * @return
     */
    public GuessIonizationFromMs1Result guessIonization(@NotNull Ms2Experiment experiment, PrecursorIonType[] candidateIonizations) {
        boolean guessedFromMergedMs1 = false;
        Spectrum<Peak> spec = experiment.getMergedMs1Spectrum();
        SimpleMutableSpectrum mutableMerged = null;
        if (spec != null) {
            guessedFromMergedMs1 = true;
            mutableMerged = new MutableMs2Spectrum(spec);
            Spectrums.filterIsotpePeaks(mutableMerged, new Deviation(100), 1, 2, 5, new ChemicalAlphabet());
        }
        //todo hack: if the merged spectrum only contains a single monoisotopic peak: use most intense MS1 (problem if only M+H+ and M+ in merged MS1?)
        if ((mutableMerged == null || mutableMerged.size() == 1) && experiment.getMs1Spectra().size() > 0) {
            guessedFromMergedMs1 = false;
            spec = Spectrums.selectSpectrumWithMostIntensePrecursor(experiment.getMs1Spectra(), experiment.getIonMass(), getMs1Analyzer().getDefaultProfile().getAllowedMassDeviation());
            if (spec == null) spec = experiment.getMs1Spectra().get(0);
        }

        if (spec == null)
            return new GuessIonizationFromMs1Result(candidateIonizations, candidateIonizations, IonGuessingSource.NoSource);

        SimpleMutableSpectrum mutableSpectrum = new SimpleMutableSpectrum(spec);
        Spectrums.normalizeToMax(mutableSpectrum, 100d);
        Spectrums.applyBaseline(mutableSpectrum, 1d);
//        //changed
        Spectrums.filterIsotpePeaks(mutableSpectrum, getMs1Analyzer().getDefaultProfile(experiment).getStandardMassDifferenceDeviation(), 0.3, 1, 5, new ChemicalAlphabet());

        PrecursorIonType[] ionType = Spectrums.guessIonization(mutableSpectrum, experiment.getIonMass(), profile.fragmentationPatternAnalysis.getDefaultProfile().getAllowedMassDeviation(), candidateIonizations);
        return new GuessIonizationFromMs1Result(ionType, candidateIonizations, guessedFromMergedMs1 ? IonGuessingSource.MergedMs1Spectrum : IonGuessingSource.NormalMs1);
    }
}
