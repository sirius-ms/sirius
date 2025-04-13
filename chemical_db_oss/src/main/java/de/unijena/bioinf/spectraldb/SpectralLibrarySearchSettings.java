package de.unijena.bioinf.spectraldb;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bionf.spectral_alignment.SpectralMatchingType;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SpectralLibrarySearchSettings {
    /**
     * Minimal spectral similarity of a spectral match to be considered a hit.
     */
    protected float minSimilarity;
    /**
     * Minimal number of matching peaks of a spectral match to be considered a hit.
     */
    protected int minNumOfPeaks;
    /**
     * Maximum number of hits to store
     */
    protected int maxNumOfHits =  100;

    /**
     * Matching algorith to be used for comparing spectra.
     */
    protected SpectralMatchingType matchingType;
    /**
     * Maximal allowed mass deviation of a library spectrum's compound compared to the query spectrum's precursor to be considered as candidate.
     */
    protected Deviation precursorDeviation;

    /**
     * Type of spectrum
     */
    protected Set<SpectrumType> targetTypes;


    public boolean exceeded(SpectralSimilarity similarity) {
        return similarity.similarity>= minSimilarity && similarity.sharedPeaks >= minNumOfPeaks;
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
                this.minSimilarity,
                this.minNumOfPeaks,
                this.maxNumOfHits,
                this.matchingType,
                this.precursorDeviation != null ? new Deviation(this.precursorDeviation.getPpm()) : null,
                new HashSet<>(this.targetTypes)
        );

        clone.setMaxNumOfHits(this.maxNumOfHits);

        return clone;
    }

}
