package de.unijena.bioinf.spectraldb;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bionf.spectral_alignment.SpectralMatchingType;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumSet;
import java.util.Set;

@Setter
@Getter
public class SpectralLibrarySearchSettings {

    private float minCosine;
    private int minimumNumberOfPeaks;
    private SpectralMatchingType matchingType;
    private Deviation precursorDeviation;
    private Set<SpectrumType> targetTypes;

    private int maxNumberOfAnalogHits =  100;
    private int maxNumberOfExactHits =  100;

    public static SpectralLibrarySearchSettings conservativeDefaultForCosine() {
        return new SpectralLibrarySearchSettings(SpectralMatchingType.INTENSITY, SpectrumType.SPECTRUM, 0.33f, 3, new Deviation(10));
    }

    public static SpectralLibrarySearchSettings conservativeDefaultForModifiedCosine() {
        return new SpectralLibrarySearchSettings(SpectralMatchingType.MODIFIED_COSINE, SpectrumType.MERGED_SPECTRUM, 0.5f, 6, new Deviation(10));
    }

    public SpectralLibrarySearchSettings(SpectralMatchingType matchingType, SpectrumType targetType, float minCosine, int minimumNumberOfPeaks, Deviation precursorDeviation) {
        this(matchingType, EnumSet.of(targetType), minCosine, minimumNumberOfPeaks, precursorDeviation);
    }

    public SpectralLibrarySearchSettings(SpectralMatchingType matchingType, Set<SpectrumType> targetTypes, float minCosine, int minimumNumberOfPeaks, Deviation precursorDeviation) {
        this.minCosine = minCosine;
        this.minimumNumberOfPeaks = minimumNumberOfPeaks;
        this.precursorDeviation = precursorDeviation;
        this.matchingType = matchingType;
        this.targetTypes = targetTypes;
    }

    public boolean exceeded(SpectralSimilarity similarity) {
        return similarity.similarity>=minCosine && similarity.sharedPeaks >= minimumNumberOfPeaks;
    }

    public boolean containsTargetType(SpectrumType targetType) {
        return targetTypes != null && targetTypes.contains(targetType);
    }

    public void addTargetType(SpectrumType targetType) {
        if (targetTypes == null)
            targetTypes = EnumSet.of(targetType);
        else
            targetTypes.add(targetType);
    }

    @Override
    public SpectralLibrarySearchSettings clone() {
        SpectralLibrarySearchSettings clone = new SpectralLibrarySearchSettings(
                this.matchingType,
                this.targetTypes,
                this.minCosine,
                this.minimumNumberOfPeaks,
                this.precursorDeviation != null ? new Deviation(this.precursorDeviation.getPpm()) : null
        );

        clone.setMaxNumberOfAnalogHits(this.maxNumberOfAnalogHits);
        clone.setMaxNumberOfExactHits(this.maxNumberOfExactHits);

        return clone;
    }

}
