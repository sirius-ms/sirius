package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.AdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import org.jetbrains.annotations.NotNull;

public class SimpleSpectrumWithAdditionalFields extends SimpleSpectrum implements SpectrumWithAdditionalFields<Peak> {

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

    public SimpleSpectrumWithAdditionalFields(SimpleSpectrum spec) {
        super(spec);
    }

    protected SimpleSpectrumWithAdditionalFields(double[] masses, double[] intensities, boolean NoCopyAndOrder) {
        super(masses, intensities, NoCopyAndOrder);
    }

    public SimpleSpectrumWithAdditionalFields(double[] masses, double[] intensities) {
        super(masses, intensities);
    }

    public <T extends Peak, S extends Spectrum<T>> SimpleSpectrumWithAdditionalFields(S ts) {
        super(ts);
    }
}
