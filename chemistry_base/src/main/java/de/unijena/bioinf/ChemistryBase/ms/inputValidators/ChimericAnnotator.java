package de.unijena.bioinf.ChemistryBase.ms.inputValidators;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.FormulaSettings;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

import java.util.Collections;
import java.util.List;

public class ChimericAnnotator implements QualityAnnotator {
    private DatasetStatistics statistics;
    private List<SpectrumProperty> prerequisites = Collections.singletonList(SpectrumProperty.NoMS1Peak);
    private Deviation findMs1PeakDeviation;

    double max2ndMostIntenseRatio;
    double maxSummedIntensitiesRatio;

    public ChimericAnnotator(Deviation findMs1PeakDeviation, double max2ndMostIntenseRatio, double maxSummedIntensitiesRatio) {
        //todo really remove isotope peaks!??!?!??!

        this.findMs1PeakDeviation = findMs1PeakDeviation;

        this.max2ndMostIntenseRatio = max2ndMostIntenseRatio;
        this.maxSummedIntensitiesRatio = maxSummedIntensitiesRatio;
    }

    @Override
    public SpectrumProperty getPropertyToAnnotate() {
        return SpectrumProperty.Chimeric;
    }

    @Override
    public List<SpectrumProperty> getPrerequisites() {
        return prerequisites;
    }

    @Override
    public void prepare(DatasetStatistics statistics) {

    }

    /**
     * annotates {@link Ms2Experiment}s with SpectrumProperty.Chimeric if most intense MS1 looks chimeric
     * //TODO currently removes all isotopes from MS1, but they in fact still produce chimerics. Test with and without
     */
    @Override
    public void annotate(Ms2Dataset dataset) {
//        Deviation maxDeviation = dataset.getMeasurementProfile().getAllowedMassDeviation();
        //changed
        Deviation maxDeviation = dataset.getMeasurementProfile().getStandardMassDifferenceDeviation().multiply(2d);
        IsolationWindow isolationWindow = dataset.getIsolationWindow();

//        System.out.println("chimeric alphabet");
//        System.out.println(dataset.getMeasurementProfile().getFormulaConstraints().getChemicalAlphabet().toString());

        for (Ms2Experiment experiment : dataset.getExperiments()) {
            annotate(experiment, maxDeviation, isolationWindow, dataset.getMeasurementProfile().getFormulaConstraints().getChemicalAlphabet());
        }
    }

    protected void  annotate(Ms2Experiment experiment, Deviation maxDeviation, IsolationWindow isolationWindow, ChemicalAlphabet defaultAlphabet){
        Spectrum<Peak> ms1 = getMostIntenseSpectrumContainingPrecursorPeak(experiment);
        if (ms1==null) return;

        int ms1PrecursorIdx = Spectrums.mostIntensivePeakWithin(ms1, experiment.getIonMass(), findMs1PeakDeviation);
        if (ms1PrecursorIdx<0){
//                if (!hasProperty(experiment, SpectrumProperty.NoMS1Peak)){
//                    setSpectrumProperty(experiment, SpectrumProperty.NoMS1Peak);
//                }
            return;
        }
        Peak precursorPeak = ms1.getPeakAt(ms1PrecursorIdx);
        double precursorMz = precursorPeak.getMass();
        double filteredPrecursorIntensity = isolationWindow.getIntensity(precursorPeak.getIntensity(), precursorMz, precursorMz);

        double center = isolationWindow.getMassShift()+precursorPeak.getMass();
        double left = center-isolationWindow.getMaxWindowSize()/2;
        double right = center+isolationWindow.getMaxWindowSize()/2;

        SimpleMutableSpectrum ms1IsotopesRemoved = new SimpleMutableSpectrum(ms1);
        //todo which deviation to use? rather remove too much other peaks?
        ChemicalAlphabet alphabet;
        if (experiment.hasAnnotation(FormulaSettings.class)){
            alphabet = experiment.getAnnotation(FormulaSettings.class).getConstraints().getChemicalAlphabet();
        }
        else {
            alphabet = defaultAlphabet;
        }
        //todo rather remove too much?! chances that it's in fact an isotope are high
        //changed
//        Spectrums.filterIsotpePeaks(ms1IsotopesRemoved, maxDeviation, 0.5, 1.2, 5, alphabet); //todo or add up isotope intensities
        Spectrums.filterIsotpePeaks(ms1IsotopesRemoved, maxDeviation, 2, 2, 5, alphabet); //todo or add up isotope intensities

        Spectrum<Peak> massSorted = Spectrums.getMassOrderedSpectrum(ms1IsotopesRemoved);
        int precursorIdx = Spectrums.binarySearch(massSorted, precursorPeak.getMass());

        if (precursorIdx<0) {
            if (CompoundQuality.hasProperty(experiment, SpectrumProperty.NotMonoisotopicPeak)){
                //todo go on anyways?
//                    continue;
                ms1IsotopesRemoved.addPeak(precursorPeak);
                massSorted = Spectrums.getMassOrderedSpectrum(ms1IsotopesRemoved);
                precursorIdx = Spectrums.binarySearch(massSorted, precursorPeak.getMass());
            } else {
                ms1IsotopesRemoved.addPeak(precursorPeak);
                massSorted = Spectrums.getMassOrderedSpectrum(ms1IsotopesRemoved);
                precursorIdx = Spectrums.binarySearch(massSorted, precursorPeak.getMass());
            }
        }

        int idx = precursorIdx;
        double summedIntensity = 0d;
        double maxIntensity = 0d;
        while ((++idx<massSorted.size()) && massSorted.getMzAt(idx)<=right) {
            Peak p = massSorted.getPeakAt(idx);
            double intensity = isolationWindow.getIntensity(p.getIntensity(), precursorMz, p.getMass());
            maxIntensity = Math.max(maxIntensity, intensity);
            summedIntensity += intensity;
        }
        idx = precursorIdx;
        while ((--idx>=0) && massSorted.getMzAt(idx)>=left) {
            Peak p = massSorted.getPeakAt(idx);
            double intensity = isolationWindow.getIntensity(p.getIntensity(), precursorMz, p.getMass());
            maxIntensity = Math.max(maxIntensity, intensity);
            summedIntensity += intensity;
        }


        //todo best would be to look how much is fragmented in MS2. If nothing, it's not a problem
        if (maxIntensity>=max2ndMostIntenseRatio*filteredPrecursorIntensity || summedIntensity>=maxSummedIntensitiesRatio*filteredPrecursorIntensity){
            CompoundQuality.setProperty(experiment, SpectrumProperty.Chimeric);
        }
    }

    protected Spectrum<Peak> getMostIntenseSpectrumContainingPrecursorPeak(Ms2Experiment experiment){
        double precursorMass = experiment.getIonMass();
        int mostIntensiveIdx = -1;
        double maxIntensity = -1d;
        int pos = -1;
        for (Spectrum<Peak> spectrum : experiment.getMs1Spectra()) {
            ++pos;
            int idx = Spectrums.mostIntensivePeakWithin(spectrum, precursorMass, findMs1PeakDeviation);
            if (idx<0) continue;
            double intensity = spectrum.getIntensityAt(idx);
            if (intensity>maxIntensity){
                maxIntensity = intensity;
                mostIntensiveIdx = pos;
            }
        }
        if (mostIntensiveIdx<0) return null;
        return experiment.getMs1Spectra().get(mostIntensiveIdx);
    }
}
