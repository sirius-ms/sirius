package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class IonType {

    private static Set<PrecursorIonType> FREQUENT_ION_TYPES = Set.of(

    );
    private static Set<PrecursorIonType> POSSIBLE_ION_TYPES = Set.of(
            PrecursorIonType.getPrecursorIonType("[M+NH4]+"),PrecursorIonType.getPrecursorIonType("[M+Na]+"),PrecursorIonType.getPrecursorIonType("[M+Cl]-"),
            PrecursorIonType.getPrecursorIonType("[M+Br]-"),
            PrecursorIonType.getPrecursorIonType("[M-H2O+H]+")
    );

    public IonType withIsotope(int iso) {
        return new IonType(ionType.withIsotopes(iso));
    }


    /**
     * I have to think more about this, but for the moment, we just use some rule of thumb.
     */
    public enum Frequency {
        // always trust FREQUENT adducts
        FREQUENT, // plain ionizations and NH4+
        // only trust POSSIBLE adducts if you see a direct edge
        POSSIBLE, // water loss and similar stuff, multimeres
        // only trust UNLIKELY adducts if you see at least two edges
        UNLIKELY; // other weird adducts

        // if not trust, add plain ionization as fallback
    }

    protected PrecursorIonType ionType;

    public IonType(PrecursorIonType ionType) {
        this.ionType = ionType;
    }

    public Frequency getAdductFrequency() {
        if (ionType.getMultimereCount()!=1) {
            if (ionType.getModification().isEmpty()) return Frequency.POSSIBLE;
            else return Frequency.UNLIKELY;
        } else {
            if (ionType.isPlainProtonationOrDeprotonation()) return Frequency.FREQUENT;
            if (FREQUENT_ION_TYPES.contains(ionType)) return Frequency.FREQUENT;
            if (POSSIBLE_ION_TYPES.contains(ionType)) return Frequency.POSSIBLE;
            return Frequency.UNLIKELY;
        }
    }

    public PrecursorIonType getIonType() {
        return ionType;
    }

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
        return ionType1.ionType.equals(ionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ionType);
    }
}
