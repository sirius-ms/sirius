package de.unijena.bioinf.ChemistryBase.ms.inputValidators;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.sirius.Sirius;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotMonoisotopicAnnotatorUsingIPA implements QualityAnnotator {
    private Deviation findMs1PeakDeviation;
    private List<SpectrumProperty> prerequisites = Collections.singletonList(SpectrumProperty.NoMS1Peak);
    private Sirius sirius;
    private double betterThanMonoisotopicThreshold = 0.0;

    public NotMonoisotopicAnnotatorUsingIPA(Deviation findMs1PeakDeviation) {
        this.findMs1PeakDeviation = findMs1PeakDeviation;
    }

    @Override
    public SpectrumProperty getPropertyToAnnotate() {
        return SpectrumProperty.NotMonoisotopicPeak;
    }

    @Override
    public List<SpectrumProperty> getPrerequisites() {
        return prerequisites;
    }

    @Override
    public void prepare(DatasetStatistics statistics) {

    }

    /**
     *
     * //TODO test if this still works well revoming skipPeak and trimToPossiblePattern and setUpperBounds
     * //TODO therefore use different threshold. E.g. 50% better score.
     * @param dataset
     */
    @Override
    public void annotate(Ms2Dataset dataset) {
        if (sirius==null){
            try {
                sirius = new Sirius(dataset.getProfile());
            } catch (IOException e) {
                e.printStackTrace();
                sirius = new Sirius();
            }
        }
        for (Ms2Experiment ms2Experiment : dataset) {
            if (CompoundQuality.hasProperty(ms2Experiment, SpectrumProperty.NoMS1Peak)) continue;
            if (isNotMonoisotopicPeak(ms2Experiment, dataset.getMeasurementProfile())){
                CompoundQuality.setProperty(ms2Experiment, SpectrumProperty.NotMonoisotopicPeak);
                continue;
            }
        }
    }

    /**
     * uses the most intense of all MS1 precursor peaks. USES ABSOLUTE VALUES, not relative. Fails if different instrument types are merged.
     * @param experiment
     * @param profile
     * @return
     */
    private boolean isNotMonoisotopicPeak(Ms2Experiment experiment, MeasurementProfile profile) {
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
        if (mostIntensiveIdx<0) throw new RuntimeException("no MS1 precursor peak found.");

        return isNotMonoisotopicPeak(precursorMass, experiment.getMs1Spectra().get(mostIntensiveIdx), profile, experiment.getPrecursorIonType().getCharge());

    }

    private boolean isNotMonoisotopicPeak(double precursorMass, Spectrum<Peak> ms1, MeasurementProfile profile, int charge) {
        MutableSpectrum<Peak> massSortedMs1 = new SimpleMutableSpectrum(ms1);
        Spectrums.sortSpectrumByMass(massSortedMs1);

        int idx = Spectrums.mostIntensivePeakWithin(massSortedMs1, precursorMass, findMs1PeakDeviation);
        double realPrecursorMass = massSortedMs1.getMzAt(idx);
        double precursorIntensity = massSortedMs1.getIntensityAt(idx);
        if (idx<0){
            throw new RuntimeException("could not find precursor peak");
        }

        if (!containsAnyAlternativeMonoisotopicPeak(realPrecursorMass, precursorIntensity, idx, massSortedMs1)) return false;

        List<IsotopePattern> patterns = computeIsotopePatterns(realPrecursorMass, massSortedMs1, profile, realPrecursorMass, charge);
//        if (patterns.size()>0){
//            System.out.println("mono best "+patterns.get(0).getCandidate()+" with "+patterns.get(0).getScore()+" at "+patterns.get(0).getMonoisotopicMass());
//
//        }
        //todo never a negative score for the "mono" pattern?
        double bestMonoScore = Math.max((patterns.size()>0 ? patterns.get(0).getScore() : 0d), 0d);
        double bestScoreWithNotMonoPeak = Double.NEGATIVE_INFINITY;
        int currentIndex = idx;
        while (currentIndex>0){
            --currentIndex;
            double maxMs1Int = Spectrums.getMaximalIntensity(massSortedMs1);
            Peak peak = massSortedMs1.getPeakAt(currentIndex);
            double mass = peak.getMass();
            double diff = precursorMass - mass;
            if (diff > 2.5) break;
            if (skipPeak(peak, precursorMass, precursorIntensity)) continue;


            List<IsotopePattern> isotopePatterns = computeIsotopePatterns(mass, massSortedMs1, profile, realPrecursorMass, charge);
            for (IsotopePattern isotopePattern : isotopePatterns) {
                if (containsMass(realPrecursorMass, isotopePattern)){
                    if (isotopePattern.getScore()>bestMonoScore+betterThanMonoisotopicThreshold){
//                        System.out.println(isotopePattern.getCandidate()+" at "+isotopePattern.getMonoisotopicMass()+" with "+isotopePattern.getScore());
                    }
                    bestScoreWithNotMonoPeak = Math.max(isotopePattern.getScore(), bestScoreWithNotMonoPeak);
                }
            }
        }

        return  (bestScoreWithNotMonoPeak>bestMonoScore+betterThanMonoisotopicThreshold);
    }


    private boolean containsAnyAlternativeMonoisotopicPeak(double precursorMass, double precursorInt, int idx, MutableSpectrum<Peak> spectrum) {
        int currentIndex = idx;
        while (currentIndex>0) {
            --currentIndex;
            Peak peak = spectrum.getPeakAt(currentIndex);
            double diff = precursorMass - peak.getMass();
            if (diff > 2.5) break; // only assume precursor peak being +1 or +2
            if (skipPeak(peak, precursorMass, precursorInt)) continue;

            return true;
        }
        return false;
    }

    /*
    skip unlikely peaks
    //TODO necessary?!?! TEST!!!!
     */
    private boolean skipPeak(Peak peak, double precursorMass,  double precursorInt) {
        double mass = peak.getMass();
        double intensity = peak.getIntensity();
        if (intensity/precursorInt<0.33){
            return true;
        }
        double diff = precursorMass - mass;
        if (Math.abs(diff - Math.round(diff)) > 0.01) return true;
        return false;
    }

    /**
     * compute {@link IsotopePattern}s. Only use if possible pattern contains precursor peak(mass)
     * @param mass
     * @param spectrum
     * @param profile
     * @param precursorMass
     * @param charge
     * @return
     */
    private List<IsotopePattern> computeIsotopePatterns(double mass, MutableSpectrum<Peak> spectrum, MeasurementProfile profile, double precursorMass, int charge) {
        int absCharge = Math.abs(charge);
        boolean mergeMasses = false;
        Spectrum<Peak> isotopeSpec = Spectrums.extractIsotopePattern(spectrum, profile, mass, absCharge, mergeMasses);

        isotopeSpec = trimToPossiblePattern(isotopeSpec);


        //
        if (!containsMass(precursorMass, isotopeSpec)) return new ArrayList<>();


        MutableMs2Experiment mutableIsoExperiment = new MutableMs2Experiment();
        mutableIsoExperiment.setPrecursorIonType(PrecursorIonType.unknown(charge));
        mutableIsoExperiment.setIonMass(mass);
        mutableIsoExperiment.setMergedMs1Spectrum(new SimpleSpectrum(isotopeSpec));
        FormulaConstraints constraints = sirius.predictElementsFromMs1(mutableIsoExperiment);
        setUpperBounds(constraints);
        IsotopePatternAnalysis isotopePatternAnalysis = sirius.getMs1Analyzer();
        MutableMeasurementProfile mutableMeasurementProfile = new MutableMeasurementProfile(profile);
        mutableMeasurementProfile.setFormulaConstraints(constraints);
        List<IsotopePattern> isotopePatterns = isotopePatternAnalysis.deisotope(mutableIsoExperiment, mutableMeasurementProfile);
        return isotopePatterns;
    }

    /*
    trim pattern so it does not contain a large peak after a very small one
    //TODO necessary?!?! TEST!!!!
     */
    private SimpleMutableSpectrum trimToPossiblePattern(Spectrum<Peak> isotopeSpec){
        double monoInt = isotopeSpec.getIntensityAt(0);
        SimpleMutableSpectrum s = new SimpleMutableSpectrum();
        double lastIntRatio = 1;
        for (Peak peak : isotopeSpec) {
            double currentRatio = peak.getIntensity()/monoInt;
            if (currentRatio>0.5 && lastIntRatio<0.1){
                break;
            }
            s.addPeak(peak);
            lastIntRatio = currentRatio;
        }
        return s;
    }



    private Element F = PeriodicTable.getInstance().getByName("F");
    private Element I = PeriodicTable.getInstance().getByName("I");
    private Element B = PeriodicTable.getInstance().getByName("B");
    private void setUpperBounds(FormulaConstraints constraints){
        if (constraints.getUpperbound(F)>2) constraints.setUpperbound(F,2);
        if (constraints.getUpperbound(I)>2) constraints.setUpperbound(I,2);
        if (constraints.getUpperbound(B)>0) constraints.setUpperbound(B,0);
    }

    private boolean containsMass(double mass, IsotopePattern pattern){
        return containsMass(mass, pattern.getPattern());
    }

    private boolean containsMass(double mass, Spectrum<Peak> spectrum){
        for (Peak peak : spectrum) {
            if (peak.getMass()==mass) return true;
        }
        return false;
    }
}
