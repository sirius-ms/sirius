package de.unijena.bioinf.spectraldb;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bionf.spectral_alignment.SpectralMatchingType;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;

public class SpectralLibrarySearchSettings {

    private float minCosine;
    private int minimumNumberOfPeaks;
    private SpectralMatchingType matchingType;
    private Deviation precursorDeviation;
    private SpectrumType targetType;

    public static SpectralLibrarySearchSettings conservativeDefaultForCosine() {
        return new SpectralLibrarySearchSettings(SpectralMatchingType.INTENSITY, SpectrumType.SPECTRUM, 0.33f, 3, new Deviation(10));
    }
    public static SpectralLibrarySearchSettings conservativeDefaultForModifiedCosine() {
        return new SpectralLibrarySearchSettings(SpectralMatchingType.MODIFIED_COSINE, SpectrumType.MERGED_SPECTRUM, 0.5f, 6, new Deviation(10));
    }

    public SpectralLibrarySearchSettings(SpectralMatchingType matchingType, SpectrumType targetType, float minCosine, int minimumNumberOfPeaks, Deviation precursorDeviation) {
        this.minCosine = minCosine;
        this.minimumNumberOfPeaks = minimumNumberOfPeaks;
        this.precursorDeviation = precursorDeviation;
        this.matchingType = matchingType;
        this.targetType = targetType;
    }

    public boolean exceeded(SpectralSimilarity similarity) {
        return similarity.similarity>=minCosine && similarity.sharedPeaks >= minimumNumberOfPeaks;
    }

    public SpectrumType getTargetType() {
        return targetType;
    }

    public void setTargetType(SpectrumType targetType) {
        this.targetType = targetType;
    }

    public float getMinCosine() {
        return minCosine;
    }

    public void setMinCosine(float minCosine) {
        this.minCosine = minCosine;
    }

    public int getMinimumNumberOfPeaks() {
        return minimumNumberOfPeaks;
    }

    public void setMinimumNumberOfPeaks(int minimumNumberOfPeaks) {
        this.minimumNumberOfPeaks = minimumNumberOfPeaks;
    }

    public SpectralMatchingType getMatchingType() {
        return matchingType;
    }

    public void setMatchingType(SpectralMatchingType matchingType) {
        this.matchingType = matchingType;
    }

    public Deviation getPrecursorDeviation() {
        return precursorDeviation;
    }

    public void setPrecursorDeviation(Deviation precursorDeviation) {
        this.precursorDeviation = precursorDeviation;
    }
}
