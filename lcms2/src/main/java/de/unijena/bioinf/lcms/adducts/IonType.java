package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.util.Objects;
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

    @Override
    public String toString() {
        return ionType.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IonType ionType1 = (IonType) o;
        return Objects.equals(ionType, ionType1.ionType);
    }

    @Override
    public int hashCode() {
        return ionType.hashCode();
    }
}
