package de.unijena.bioinf.ChemistryBase.ms.inputValidators;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.QualityAnnotator;
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

    @Override
    public void annotate(Ms2Dataset dataset) {
        try {
            sirius = new Sirius(dataset.getProfile());
        } catch (IOException e) {
            e.printStackTrace();
            sirius = new Sirius();
        }
        for (Ms2Experiment ms2Experiment : dataset) {
            if (CompoundQuality.hasProperty(ms2Experiment, SpectrumProperty.NoMS1Peak)) continue;
            if (isNotMonoisotopicPeak(ms2Experiment, dataset.getMeasurementProfile())){
                CompoundQuality.setProperty(ms2Experiment, SpectrumProperty.NotMonoisotopicPeak);
                continue;
            }
        }
    }

    private boolean isNotMonoisotopicPeak(Ms2Experiment experiment, MeasurementProfile profile) {
        final double precursorMass = experiment.getIonMass();

        MutableSpectrum<Peak> merged = new SimpleMutableSpectrum(experiment.getMergedMs1Spectrum());
        Spectrums.sortSpectrumByMass(merged);

        //todo which devation to finde peak?
        int idx = Spectrums.mostIntensivePeakWithin(merged, precursorMass, findMs1PeakDeviation);
        double realPrecursorMass = merged.getMzAt(idx);
        double precursorIntensity = merged.getIntensityAt(idx);
        if (idx<0){
            throw new RuntimeException("could not find precursor peak");
        }

        if (!containsAnyAlternativeMonoisotopicPeak(realPrecursorMass, precursorIntensity, idx, merged)) return false;

        List<IsotopePattern> patterns = computeIsotopePatterns(realPrecursorMass, merged, profile, realPrecursorMass);
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
//            double mass = merged.getMzAt(currentIndex);
//            double diff = realMass-mass;
//            if (diff>2.5) break;
//            if (Math.abs(diff-Math.round(diff))>0.01) continue;
            double maxMs1Int = Spectrums.getMaximalIntensity(merged);
            Peak peak = merged.getPeakAt(currentIndex);
            double mass = peak.getMass();
            double diff = precursorMass - mass;
            if (diff > 2.5) break;
            if (skipPeak(peak, precursorMass, precursorIntensity)) continue;


            List<IsotopePattern> isotopePatterns = computeIsotopePatterns(mass, merged, profile, realPrecursorMass);
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
            if (diff > 2.5) break;
            if (skipPeak(peak, precursorMass, precursorInt)) continue;

            return true;
        }
        return false;
    }

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

    private List<IsotopePattern> computeIsotopePatterns(double mass, MutableSpectrum<Peak> spectrum, MeasurementProfile profile, double precursorMass) {
        int absCharge = 1;//todo change something?
        boolean mergeMasses = false;
        SimpleMutableSpectrum isotopeSpec = Spectrums.extractIsotopePattern(spectrum, profile, mass, absCharge, mergeMasses);

        isotopeSpec = trimToPossiblePattern(isotopeSpec);


        //
        if (!containsMass(precursorMass, isotopeSpec)) return new ArrayList<>();


        MutableMs2Experiment mutableIsoExperiment = new MutableMs2Experiment();
        mutableIsoExperiment.setPrecursorIonType(PrecursorIonType.unknown(absCharge));
        mutableIsoExperiment.setIonMass(mass);
        mutableIsoExperiment.setMergedMs1Spectrum(new SimpleSpectrum(isotopeSpec));
        FormulaConstraints constraints = sirius.predictElementsFromMs1(mutableIsoExperiment);
        setUpperBounds(constraints);
        IsotopePatternAnalysis isotopePatternAnalysis = sirius.getMs1Analyzer();
//            isotopePatternAnalysis.getDefaultProfile(); //todo which profile
        MutableMeasurementProfile mutableMeasurementProfile = new MutableMeasurementProfile(profile);
        mutableMeasurementProfile.setFormulaConstraints(constraints);
        List<IsotopePattern> isotopePatterns = isotopePatternAnalysis.deisotope(mutableIsoExperiment, mutableMeasurementProfile);
        return isotopePatterns;
    }

    private SimpleMutableSpectrum trimToPossiblePattern(SimpleMutableSpectrum simpleMutableSpectrum){
        double monoInt = simpleMutableSpectrum.getIntensityAt(0);
        SimpleMutableSpectrum s = new SimpleMutableSpectrum();
        double lastIntRatio = 1;
        for (Peak peak : simpleMutableSpectrum) {
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
