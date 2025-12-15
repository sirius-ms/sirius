package de.unijena.bioinf.ms.persistence.model.properties;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

@Getter
@Setter
@Builder
@Jacksonized
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE)
public class ProjectDetectedAdducts {
    public static final ProjectDetectedAdducts EMPTY = ProjectDetectedAdducts.builder().detectedAdducts(Set.of())
            .build();

    @NotNull
    @Builder.Default
    private Set<String> detectedAdducts = new HashSet<>();

    public Set<PrecursorIonType> getDetectedAdductsAsIonTypes() {
        return getDetectedAdducts().stream().map(PrecursorIonType::fromString).collect(Collectors.toSet());
    }

    public void addDetectedAdduct(PrecursorIonType adduct) {
        detectedAdducts.add(adduct.toString());
    }

    public void addDetectedAdducts(@Nullable Collection<PrecursorIonType> adducts) {
        if (Utils.isNullOrEmpty(adducts))
            return;
        adducts.stream().map(PrecursorIonType::toString).forEach(detectedAdducts::add);
    }
}
