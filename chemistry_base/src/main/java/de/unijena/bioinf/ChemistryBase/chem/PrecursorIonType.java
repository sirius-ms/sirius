/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ms.annotations.TreeAnnotation;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * The IonType is an arbitrary modification of the molecular formula of the precursor ion
 * <p>
 * it includes in-source fragmentations and adducts but separates them from the ion mode.
 * <p>
 * For example, [M+NH4]+ consists of the ionization H+ and the adduct NH3.
 * However, [M+Na]+ consists of the ionization Na+ but has no adduct
 * [M-H2O+H]+ consists of the ionization H+ and the in-source fragmentation H2O
 * <p>
 * Besides the chemistry background, IonType works as follows:
 * - every modification that has to be applied to the precursor ion AND all of its fragments go into ionization
 * - every modification that only apply to the precursor (e.g. in-source fragmentation) goes into modification
 * - every modification that apply to the precursor but might get lost in the fragments (e.g. adducts) goes into modification
 */
public class PrecursorIonType implements TreeAnnotation {

    protected static enum SPECIAL_TYPES {
        REGULAR, UNKNOWN, INTRINSICAL_CHARGED
    }

    private final Ionization ionization;
    private final MolecularFormula inSourceFragmentation;
    private final MolecularFormula adduct;
    private final MolecularFormula modification;
    private final String name;


    public boolean hasNeitherAdductNorInsource() {
        return inSourceFragmentation.isEmpty() && adduct.isEmpty();
    }

    private final SPECIAL_TYPES special; // flag used to annotate unknown ion types


    public static PrecursorIonType fromString(String name) {
        return getPrecursorIonType(name);
    }

    public static PrecursorIonType getPrecursorIonType(String name) {
        try {
            return PeriodicTable.getInstance().ionByName(name);
        } catch (UnknownElementException e) {
            throw new IllegalArgumentException("Illegal IonType: " + name, e);
        }
    }

    public static Optional<PrecursorIonType> parsePrecursorIonType(String name) {
        try {
            return Optional.of(PeriodicTable.getInstance().ionByName(name));
        } catch (UnknownElementException e) {
            LoggerFactory.getLogger(PrecursorIonType.class).error("Could not parse IonType from String", e);
            return Optional.empty();
        }
    }

    public static PrecursorIonType getPrecursorIonType(Ionization ion) {
        return PeriodicTable.getInstance().getPrecursorIonTypeFromIonization(ion);
    }

    public static PrecursorIonType unknown(int charge) {
        return PeriodicTable.getInstance().getUnknownPrecursorIonType(charge);
    }

    public static PrecursorIonType unknownPositive() {
        return PeriodicTable.getInstance().unknownPositivePrecursorIonType();
    }

    public static PrecursorIonType unknownNegative() {
        return PeriodicTable.getInstance().unknownNegativePrecursorIonType();
    }

    PrecursorIonType(Ionization ion, MolecularFormula insource, MolecularFormula adduct, final SPECIAL_TYPES special) {
        this.ionization = ion;
        this.inSourceFragmentation = insource == null ? MolecularFormula.emptyFormula() : insource;
        this.adduct = adduct == null ? MolecularFormula.emptyFormula() : adduct;
        this.modification = this.adduct.subtract(this.inSourceFragmentation);
        this.special = special;
        this.name = formatToString();
    }

    public String substituteName(MolecularFormula neutralFormula) {
        if (isIonizationUnknown()) {
            return neutralFormula + " " + (getCharge() > 0 ? "+" : "-") + getCharge();
        }
        final StringBuilder buf = new StringBuilder(128);
        buf.append(neutralFormula.toString());
        if (!inSourceFragmentation.isEmpty()) {
            buf.append(" - ");
            buf.append(inSourceFragmentation.toString());
        }
        if (!adduct.isEmpty()) {
            buf.append(" + ");
            buf.append(adduct.toString());
        }
        if (!ionization.getAtoms().isEmpty()) {
            if (ionization.getAtoms().isAllPositiveOrZero()) {
                buf.append(" + ");
                buf.append(ionization.getAtoms().toString());
            } else {
                buf.append(" - ");
                buf.append(ionization.getAtoms().negate().toString());
            }
        }
        if (ionization.getCharge() == 1) buf.append("+");
        else if (ionization.getCharge() == -1) buf.append("-");
        else {
            buf.append(String.valueOf(getCharge()));
            buf.append(getCharge() > 0 ? "+" : "-");
        }
        return buf.toString();
    }

    public boolean equals(PrecursorIonType other) {
        if (other == null) return false;
        return this.special == other.special && this.ionization.equals(other.ionization) && this.modification.equals(other.modification);
    }

    public boolean equals(Object other) {
        if (other == null) return false;
        if (other instanceof PrecursorIonType) {
            return equals((PrecursorIonType) other);
        } else return false;
    }

    public PrecursorIonType withoutAdduct() {
        return new PrecursorIonType(getIonization(), inSourceFragmentation, MolecularFormula.emptyFormula(), special);
    }

    public PrecursorIonType withoutInsource() {
        return new PrecursorIonType(getIonization(), MolecularFormula.emptyFormula(), adduct, special);
    }

    public PrecursorIonType substituteAdduct(MolecularFormula newAdduct) {
        return new PrecursorIonType(getIonization(), inSourceFragmentation, newAdduct, special);
    }

    public PrecursorIonType substituteInsource(MolecularFormula newInsource) {
        return new PrecursorIonType(getIonization(), newInsource, adduct, special);
    }

    @Override
    public int hashCode() {
        return 31 * ionization.hashCode() + modification.hashCode() + 17 * special.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    private String formatToString() {
        if (isIonizationUnknown()) {
            return ionization.toString();
        }
        if (isIntrinsicalCharged()) {
            return "[M]" + (getCharge() > 0 ? "+" : "-");
        }
        final StringBuilder buf = new StringBuilder(128);
        buf.append("[M");
        if (!inSourceFragmentation.isEmpty()) {
            buf.append(" - ");
            buf.append(inSourceFragmentation.toString());
        }
        if (!adduct.isEmpty()) {
            buf.append(" + ");
            buf.append(adduct.toString());
        }
        if (!ionization.getAtoms().isEmpty()) {
            if (ionization.getAtoms().isAllPositiveOrZero()) {
                buf.append(" + ");
                buf.append(ionization.getAtoms().toString());
            } else {
                buf.append(" - ");
                buf.append(ionization.getAtoms().negate().toString());
            }
        }
        buf.append("]");
        if (ionization.getCharge() == 1) buf.append("+");
        else if (ionization.getCharge() == -1) buf.append("-");
        else {
            buf.append(String.valueOf(getCharge()));
            buf.append(getCharge() > 0 ? "+" : "-");
        }
        return buf.toString();
    }

    public boolean isIonizationUnknown() {
        return special == SPECIAL_TYPES.UNKNOWN;
        //return ionization instanceof Charge && this == unknown(getCharge());
    }

    public boolean isUnknownPositive() {
        return isIonizationUnknown() && isPositive();

    }

    public boolean isUnknownNegative() {
        return isIonizationUnknown() && isNegative();

    }

    public boolean isUnknownNoCharge() {
        return isIonizationUnknown() && getCharge() == 0;

    }

    public boolean isPositive() {
        return getCharge() > 0;

    }

    public boolean isNegative() {
        return getCharge() < 0;

    }

    public int getCharge() {
        return ionization.getCharge();
    }

    //////// ????????????????
    public MolecularFormula neutralMoleculeToMeasuredNeutralMolecule(MolecularFormula neutral) {
        /*
        if (isIntrinsicalCharged())
            neutral = getCharge() > 0 ? neutral.subtract(MolecularFormula.getHydrogen()) : neutral.add(MolecularFormula.getHydrogen());

         */
        return neutral.subtract(inSourceFragmentation).add(adduct);
    }

    /**
     * The measured neutral molecule is the ion without the charge (proton, sodium or similar). But it contains all
     * adducts and other modificatons (like in source fragments)
     */
    public MolecularFormula measuredNeutralMoleculeToNeutralMolecule(MolecularFormula measured) {
        if (isIntrinsicalCharged())
            measured = getCharge() > 0 ? measured.add(MolecularFormula.getHydrogen()) : measured.subtract(MolecularFormula.getHydrogen());
        if (inSourceFragmentation == null) return measured;
        return measured.add(inSourceFragmentation).subtract(adduct);
    }


    /**
     * the precursor ion is the measured(!) ion. The neutral molecule is the "expected" neutral molecule.
     * Translation from precursor to expected molecule involves removing of the adduct and reverting all
     * in source fragmentations.
     */
    public MolecularFormula precursorIonToNeutralMolecule(MolecularFormula precursor) {
        return precursor.subtract(modification).subtract(ionization.getAtoms());
    }

    public MolecularFormula neutralMoleculeToPrecursorIon(MolecularFormula formula) {
        return formula.add(modification).add(ionization.getAtoms());
    }

    /**
     * @return the mass difference between the ion mass and the neutral mass including in-source fragmentation, adduct, and electron masses
     */
    public double getModificationMass() {
        return ionization.getMass() + modification.getMass();
    }

    public double subtractIonAndAdduct(double mz) {
        return ionization.subtractFromMass(mz - adduct.getMass());
    }

    /*
    TODO: in-source is not contained here. is this correct? CHECK!
     */
    public double addIonAndAdduct(double mz) {
        return ionization.addToMass(mz + adduct.getMass());
    }

    /**
     * is used by mass decomposer to translate a m/z value into a neutralized mass which can then be decomposed into
     * a molecular formula (of a neutral molecule)
     */
    public double precursorMassToNeutralMass(double mz) {
        if (isIntrinsicalCharged()) return mz - modification.getMass() + Charge.ELECTRON_MASS * ionization.getCharge();
        return ionization.subtractFromMass(mz - modification.getMass());
    }

    public double neutralMassToPrecursorMass(double mz) {
        if (isIntrinsicalCharged()) return mz + modification.getMass() - Charge.ELECTRON_MASS * ionization.getCharge();
        return ionization.addToMass(mz + modification.getMass());
    }


    /**
     * @return the sum of all modifications (adducts and in-source fragmentations)
     */
    public MolecularFormula getModification() {
        return modification;
    }

    public Ionization getIonization() {
        return ionization;
    }

    public MolecularFormula getInSourceFragmentation() {
        return inSourceFragmentation;
    }

    public MolecularFormula getAdduct() {
        return adduct;
    }

    public MolecularFormula getAdductAndIons() {
        return adduct.add(ionization.getAtoms());
    }

    public boolean isIntrinsicalCharged() {
        return special == SPECIAL_TYPES.INTRINSICAL_CHARGED;
    }

    public boolean isPlainProtonationOrDeprotonation() {
        return this.modification.isEmpty() && this.inSourceFragmentation.isEmpty() && ((getCharge() > 0 && ionization.equals(PeriodicTable.getInstance().getProtonation())) || (getCharge() < 0 && ionization.equals(PeriodicTable.getInstance().getDeprotonation()))) && !isIntrinsicalCharged();
    }
}
