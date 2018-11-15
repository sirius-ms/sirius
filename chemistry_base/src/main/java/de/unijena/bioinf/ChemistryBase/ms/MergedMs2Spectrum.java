package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;

public class MergedMs2Spectrum extends MutableMs2Spectrum implements Ms2ExperimentAnnotation {

    public MergedMs2Spectrum() {
    }

    public <T extends Peak, S extends Spectrum<T>> MergedMs2Spectrum(S spec, double parentmass, CollisionEnergy energy, int mslevel) {
        super(spec, parentmass, energy, mslevel);
    }

    public MergedMs2Spectrum(Spectrum<? extends Peak> spec) {
        super(spec);
    }
}
