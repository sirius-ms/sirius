package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.util.Optional;

/**
 * A wrapper around PrecursorIonType that allows us to support other kind of adducts that might not be supported
 * yet via PrecursorIonType. In the long run, it would be better of course to allow these adducts directly in
 * PrecursorIonType.
 */
public interface KnownAdductType {
    public Optional<PrecursorIonType> toPrecursorIonType();
}
