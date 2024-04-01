package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.util.Optional;

public class IonType implements KnownAdductType {

    protected PrecursorIonType ionType;

    public IonType(PrecursorIonType ionType) {
        this.ionType = ionType;
    }

    @Override
    public Optional<PrecursorIonType> toPrecursorIonType() {
        return Optional.of(ionType);
    }
}
