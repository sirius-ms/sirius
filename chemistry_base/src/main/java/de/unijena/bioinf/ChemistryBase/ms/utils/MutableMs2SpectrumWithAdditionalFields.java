package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.*;
import org.jetbrains.annotations.NotNull;

public class MutableMs2SpectrumWithAdditionalFields extends MutableMs2Spectrum implements SpectrumWithAdditionalFields<Peak> {

    private AdditionalFields additionalFields = new AdditionalFields();

    @Override
    @NotNull
    public AdditionalFields additionalFields() {
        return additionalFields;
    }

    @Override
    public void setAdditionalFields(@NotNull AdditionalFields fieldStore) {
        this.additionalFields = fieldStore;
    }

    public MutableMs2SpectrumWithAdditionalFields() {
    }

    public MutableMs2SpectrumWithAdditionalFields(Header header, SimpleMutableSpectrum peaks) {
        super(header, peaks);
    }

    public <T extends Peak, S extends Spectrum<T>> MutableMs2SpectrumWithAdditionalFields(S spec, double parentmass, CollisionEnergy energy, int mslevel) {
        super(spec, parentmass, energy, mslevel);
    }

    public MutableMs2SpectrumWithAdditionalFields(Spectrum<? extends Peak> spec) {
        super(spec);
    }
}
