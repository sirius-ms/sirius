package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.ArrayList;

public class ConsensusFeature implements Annotated<DataAnnotation> {

    protected final int featureId;
    protected final Feature[] features;
    protected final SimpleSpectrum[] coelutedPeaks;
    protected final SimpleSpectrum[] ms2;
    protected final long averageRetentionTime;
    protected final double averageMass, totalIntensity;
    protected final PrecursorIonType ionType;
    protected Annotated.Annotations<DataAnnotation> annotations = new Annotated.Annotations<>();

    public ConsensusFeature(int featureId, Feature[] features, SimpleSpectrum[] coelutedPeaks, SimpleSpectrum[] ms2, PrecursorIonType ionType,  long averageRetentionTime, double averageMass, double totalIntensity) {
        this.featureId = featureId;
        this.features = features;
        this.coelutedPeaks = coelutedPeaks;
        this.ms2 = ms2;
        this.averageRetentionTime = averageRetentionTime;
        this.averageMass = averageMass;
        this.totalIntensity = totalIntensity;
        this.ionType = ionType;
    }

    public int getFeatureId() {
        return featureId;
    }

    public Feature[] getFeatures() {
        return features;
    }

    public SimpleSpectrum[] getCoelutedPeaks() {
        return coelutedPeaks;
    }

    public SimpleSpectrum[] getMs2() {
        return ms2;
    }

    public long getAverageRetentionTime() {
        return averageRetentionTime;
    }

    public double getAverageMass() {
        return averageMass;
    }

    public double getTotalIntensity() {
        return totalIntensity;
    }

    public PrecursorIonType getIonType() {
        return ionType;
    }

    public Ms2Experiment toMs2Experiment() {

        final MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setName(String.valueOf(featureId));
        exp.setPrecursorIonType(ionType);
        exp.setMergedMs1Spectrum(Spectrums.mergeSpectra(coelutedPeaks));
        final ArrayList<MutableMs2Spectrum> ms2Spectra = new ArrayList<>();
        for (SimpleSpectrum s : ms2) {
            ms2Spectra.add(new MutableMs2Spectrum(s, averageMass, CollisionEnergy.none(), 2));
        }
        exp.setMs2Spectra(ms2Spectra);
        exp.setIonMass(averageMass);
        exp.setAnnotation(RetentionTime.class, new RetentionTime(averageRetentionTime/1000d));

        final TObjectDoubleHashMap<String> map = new TObjectDoubleHashMap<>();
        boolean good = false;
        for (Feature f : features) {
            map.put(f.origin.identifier, f.intensity);
            if (f.ms2Quality.betterThan(Quality.DECENT) && f.ms1Quality.betterThan(Quality.DECENT) && f.peakShapeQuality.betterThan(Quality.DECENT)) {
                good = true;
            }
        }

        exp.setAnnotation(Quantification.class, new Quantification(map));
        if (good)
            exp.setAnnotation(CompoundQuality.class, new CompoundQuality(CompoundQuality.CompoundQualityFlag.Good));

        return exp;
    }

    @Override
    public Annotations<DataAnnotation> annotations() {
        return annotations;
    }
}
