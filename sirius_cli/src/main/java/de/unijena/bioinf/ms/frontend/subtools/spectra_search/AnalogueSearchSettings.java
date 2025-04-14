package de.unijena.bioinf.ms.frontend.subtools.spectra_search;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import de.unijena.bioinf.spectraldb.SpectralLibrarySearchSettings;
import de.unijena.bioinf.spectraldb.SpectrumType;
import de.unijena.bionf.spectral_alignment.SpectralMatchingType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
public class AnalogueSearchSettings extends SpectralLibrarySearchSettings implements Ms2ExperimentAnnotation {
    @Getter
    protected boolean enabled;

    public AnalogueSearchSettings(boolean enabled, float minSimilarity, int minNumOfPeaks, int maxNumOfHits, SpectralMatchingType matchingType, Deviation precursorDeviation, Set<SpectrumType> targetTypes) {
        super(minSimilarity, minNumOfPeaks, maxNumOfHits, matchingType, precursorDeviation, targetTypes);
        this.enabled = enabled;
    }

    @DefaultInstanceProvider
    public static AnalogueSearchSettings newInstance(
            @DefaultProperty(propertyKey = "enabled") boolean enabled,
            @DefaultProperty(propertyKey = "minSimilarity") float minSimilarity,
            @DefaultProperty(propertyKey = "minNumOfPeaks") int minNumOfPeaks,
            @DefaultProperty(propertyKey = "maxNumOfHits") int maxNumOfHits,
            @DefaultProperty(propertyKey = "matchingType") SpectralMatchingType matchingType,
            @DefaultProperty(propertyKey = "precursorDeviation") Deviation precursorDeviation,
            @DefaultProperty(propertyKey = "targetTypes") Set<SpectrumType> targetTypes
    ) {
        return new AnalogueSearchSettings(enabled, minSimilarity, minNumOfPeaks, maxNumOfHits, matchingType, precursorDeviation, targetTypes);
    }
}
