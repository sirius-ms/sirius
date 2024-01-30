package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.AdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import org.jetbrains.annotations.NotNull;

public interface SpectrumWithAdditionalFields<P extends Peak> extends Spectrum<P> {
    @NotNull
    AdditionalFields additionalFields();
    void setAdditionalFields(@NotNull AdditionalFields fieldStore);
}
