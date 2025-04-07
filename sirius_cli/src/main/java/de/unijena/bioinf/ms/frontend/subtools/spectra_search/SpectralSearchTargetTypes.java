package de.unijena.bioinf.ms.frontend.subtools.spectra_search;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import de.unijena.bioinf.spectraldb.SpectrumType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SpectralSearchTargetTypes implements Ms2ExperimentAnnotation {

    public final Set<SpectrumType> value;

    public SpectralSearchTargetTypes(EnumSet<SpectrumType> spectrumTypes) {
        this.value = spectrumTypes;
    }

    public SpectralSearchTargetTypes(Collection<SpectrumType> spectrumTypes) {
        this(EnumSet.copyOf(spectrumTypes));
    }

    @DefaultInstanceProvider
    public static SpectralSearchTargetTypes fromString(@DefaultProperty @Nullable String value) {
        if (value == null || value.isBlank())
            return new SpectralSearchTargetTypes(EnumSet.noneOf(SpectrumType.class));

        EnumSet<SpectrumType> types = Arrays.stream(value.split("\\s*,\\s*")).map(String::strip)
                .map(SpectrumType::valueOf).collect(Collectors.toCollection(() -> EnumSet.noneOf(SpectrumType.class)));
        return new SpectralSearchTargetTypes(types);
    }
}