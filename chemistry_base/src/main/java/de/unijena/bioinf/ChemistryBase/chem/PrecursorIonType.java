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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import de.unijena.bioinf.ms.annotations.TreeAnnotation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Objects;
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
 * <p>
 * We distinguish three different kind of formula types:
 * - NEUTRAL formulas are molecular formulas without any charge, protonation, adduct, in-source, modification. These
 *   are molecular formulas of the original molecule as they are stored in the database. An exception are intrinsical
 *   charged compounds, as those are represented in their neutralized form, too.
 * - MEASURED formulas are the neutral form of the molecule as it is measured in the MS. This means: it does not contain
 *   the charge, but it contains all other modifications (like in-source, adducts) but no H+, Na+ and so on.
 * - PRECURSOR formulas are the charged variant as it is measured in the MS. It contains a molecular formula that is, when adding the electron masses, identical in mass to the m/z of the peak.
 *
 * Internally, SIRIUS will always work with MEASURED formulas. However, whenever we communicate with a database or with
 * user input, we assume that this input might be the NEUTRAL formula. Again, a special exception are intrinsical-charged
 * compounds: they might be represented in their PRECURSOR formula when fetching them from database or getting them
 * from user input. In this case, the method that directly receives this input has to transform the formula into its
 * NEUTRAL form. This can be done in the Database class or in the Ms2Validator class.
 * </p>
 *
 *
 *
 */
@JsonSerialize(using = ToStringSerializer.class)
@JsonDeserialize(using = SimpleSerializers.PrecursorIonTypeDeserializer.class)
public class PrecursorIonType implements TreeAnnotation, Comparable<PrecursorIonType> {


    public boolean isApplicableToNeutralFormula(MolecularFormula neutralFormula) {
        if (inSourceFragmentation.isEmpty()) return true;
        else return neutralFormula.isSubtractable(inSourceFragmentation);
    }
    public boolean isApplicableToMeasuredFormula(MolecularFormula measuredFormula) {
        if (adduct.isEmpty()) return true;
        else return measuredFormula.isSubtractable(adduct);
    }

    public boolean hasMultipleIons() {
        return multipleIons;
    }

    public boolean isMultipleCharged(){
        return Math.abs(getCharge()) > 1;
    }

    protected static enum SPECIAL_TYPES {
        REGULAR, UNKNOWN, INTRINSICAL_CHARGED
    }

    private final Ionization ionization;
    private final MolecularFormula inSourceFragmentation;
    private final MolecularFormula adduct;
    private final String name;
    private final boolean multipleIons;

    private final byte multimere;


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

    PrecursorIonType(Ionization ion, MolecularFormula insource, MolecularFormula adduct, int multimere, final SPECIAL_TYPES special) {
        this.ionization = ion;
        this.inSourceFragmentation = insource == null ? MolecularFormula.emptyFormula() : insource;
        this.adduct = adduct == null ? MolecularFormula.emptyFormula() : adduct;
        this.special = special;
        this.multimere = (byte)multimere;
        this.multipleIons = checkForMultipleIons();
        this.name = formatToString();
    }

    private boolean checkForMultipleIons() {
        return (adduct.numberOf("Na")>0 || adduct.numberOf("K")>0 && adduct.numberOf("Cl")>0);
    }

    public String substituteName(MolecularFormula neutralFormula) {
        if (isIonizationUnknown()) {
            return multimereStr() + neutralFormula + " " + (getCharge() > 0 ? "+" : "-") + getCharge();
        }
        final StringBuilder buf = new StringBuilder(128);
        buf.append(multimereStr());
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

    private String multimereStr() {
        return multimere == 1 ? "" : String.valueOf(multimere);
    }

    public boolean equals(PrecursorIonType other) {
        if (other == null) return false;
        return this.special == other.special && this.ionization.equals(other.ionization) && this.adduct.equals(other.adduct) && this.inSourceFragmentation.equals(other.inSourceFragmentation) && this.multimere==other.multimere;
    }

    public boolean equals(Object other) {
        if (other == null) return false;
        if (other instanceof PrecursorIonType) {
            return equals((PrecursorIonType) other);
        } else return false;
    }

    public PrecursorIonType withoutAdduct() {
        return new PrecursorIonType(getIonization(), inSourceFragmentation, MolecularFormula.emptyFormula(), multimere, special);
    }

    public PrecursorIonType withMultimere(int count) {
        return new PrecursorIonType(getIonization(), inSourceFragmentation, MolecularFormula.emptyFormula(), count, special);
    }

    public PrecursorIonType withoutInsource() {
        return new PrecursorIonType(getIonization(), MolecularFormula.emptyFormula(), adduct, multimere, special);
    }

    public PrecursorIonType substituteAdduct(MolecularFormula newAdduct) {
        return new PrecursorIonType(getIonization(), inSourceFragmentation, newAdduct, multimere, special);
    }

    public PrecursorIonType substituteInsource(MolecularFormula newInsource) {
        return new PrecursorIonType(getIonization(), newInsource, adduct, multimere, special);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ionization, inSourceFragmentation, adduct, special, multimere);
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
            return "[" + multimereStr() + "M]" + (getCharge() > 0 ? "+" : "-");
        }
        final StringBuilder buf = new StringBuilder(128);
        buf.append("[").append(multimereStr()).append("M");
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

    /**
     *
     * @param neutral molecular formula without any modification in neutralized form (even when intrinsical-charged)
     * @return molecular formula with adducts, in-source fragmentations and so on, but WITHOUT charge (no H+, Na+, ...)
     */
    public MolecularFormula neutralMoleculeToMeasuredNeutralMolecule(MolecularFormula neutral) {
        return neutral.multiply(multimere).subtract(inSourceFragmentation).add(adduct);
    }

    /**
     * @param measured molecular formula with adducts, in-source fragmentations and so on, but WITHOUT charge (no H+, Na+, ...)
     * @return molecular formula without any modification in neutralized form (even when intrinsical-charged)
     */
    public MolecularFormula measuredNeutralMoleculeToNeutralMolecule(MolecularFormula measured) {
        return measured.add(inSourceFragmentation).subtract(adduct).divide(multimere);
    }

    /**
     * @param precursor molecular formula with adducts, in-source fragmentations AND charge (atoms of the ionization)
     * @return molecular formula without any modification in neutralized form (even when intrinsical-charged)
     */
    public MolecularFormula precursorIonToNeutralMolecule(MolecularFormula precursor) {
        return precursor.subtract(adduct).add(inSourceFragmentation).subtract(ionization.getAtoms()).divide(multimere);
    }

    /**
     * @param formula molecular formula without any modification in neutralized form (even when intrinsical-charged)
     * @return molecular formula with adducts, in-source fragmentations AND charge (atoms of the ionization)
     */
    public MolecularFormula neutralMoleculeToPrecursorIon(MolecularFormula formula) {
        return formula.multiply(multimere).add(adduct).subtract(inSourceFragmentation).add(ionization.getAtoms());
    }

    /**
     *
     * @param neutral molecular formula without any modification in neutralized form (even when intrinsical-charged)
     * @return molecular formula with adducts, in-source fragmentations and so on, but WITHOUT charge (no H+, Na+, ...)
     */
    public double neutralMassToMeasuredNeutralMass(double neutral) {
        return neutral*multimere - inSourceFragmentation.getMass() + adduct.getMass();
    }

    /**
     * @param measured molecular formula with adducts, in-source fragmentations and so on, but WITHOUT charge (no H+, Na+, ...)
     * @return molecular formula without any modification in neutralized form (even when intrinsical-charged)
     */
    public double measuredNeutralMassToNeutralMass(double measured) {
        return (measured + inSourceFragmentation.getMass() - adduct.getMass())/multimere;
    }


    /**
     * @param precursor molecular formula with adducts, in-source fragmentations AND charge (atoms of the ionization)
     * @return molecular formula without any modification in neutralized form (even when intrinsical-charged)
     */
    public double precursorMassToNeutralMass(double precursor) {
        return (ionization.subtractFromMass(precursor - adduct.getMass() + inSourceFragmentation.getMass()))/multimere;
    }


    /**
     * @param formula molecular formula without any modification in neutralized form (even when intrinsical-charged)
     * @return molecular formula with adducts, in-source fragmentations AND charge (atoms of the ionization)
     */
    public double neutralMassToPrecursorMass(double formula) {
        return ionization.addToMass(formula*multimere + adduct.getMass() - inSourceFragmentation.getMass());
    }


    /**
     * @param precursor molecular formula with adducts, in-source fragmentations AND charge (atoms of the ionization)
     * @return molecular formula without any modification in neutralized form (even when intrinsical-charged)
     */
    public double precursorMassToMeasuredNeutralMass(double precursor) {
        return ionization.subtractFromMass(precursor);
    }

    /**
     * @param formula molecular formula without any modification in neutralized form (even when intrinsical-charged)
     * @return molecular formula with adducts, in-source fragmentations AND charge (atoms of the ionization)
     */
    public double measuredNeutralMassToPrecursorMass(double formula) {
        return ionization.addToMass(formula);
    }



    /**
     * @return the mass difference between the ion mass and the neutral mass including in-source fragmentation, adduct, and electron masses
     */
    public double getModificationMass() {
        return ionization.getMass() + adduct.getMass() - inSourceFragmentation.getMass();
    }

    public double subtractIonAndAdduct(double mz) {
        return ionization.subtractFromMass(mz - adduct.getMass());
    }

    public double addIonAndAdduct(double mz) {
        return ionization.addToMass(mz + adduct.getMass());
    }



    /**
     * @return the sum of all modifications (adducts and in-source fragmentations)
     */
    public MolecularFormula getModification() {
        return adduct.subtract(inSourceFragmentation);
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

    public int getMultimereCount() {
        return multimere;
    }

    public boolean isMultimere() {
        return multimere>1;
    }

    public boolean isSupportedForFragmentationTreeComputation() {
        return Math.abs(getCharge())==1 && multimere==1;
    }

    public MolecularFormula getAdductAndIons() {
        return adduct.add(ionization.getAtoms());
    }

    public boolean isIntrinsicalCharged() {
        return special == SPECIAL_TYPES.INTRINSICAL_CHARGED;
    }

    public boolean isPlainProtonationOrDeprotonation() {
        return this.adduct.isEmpty() && this.inSourceFragmentation.isEmpty() && ((getCharge() > 0 && ionization.equals(PeriodicTable.getInstance().getProtonation())) || (getCharge() < 0 && ionization.equals(PeriodicTable.getInstance().getDeprotonation()))) && !isIntrinsicalCharged();
    }

    @Override
    public int compareTo(@NotNull PrecursorIonType o) {
        return ionTypeComparator.compare(this, o);
    }

    //if changed, update in documentation
    public static Comparator<PrecursorIonType> ionTypeComparator = Comparator.comparing(PrecursorIonType::isIonizationUnknown, Comparator.reverseOrder())
            .thenComparing(p -> -Math.signum(p.getCharge()))
            .thenComparing(PrecursorIonType::getMultimereCount)
            .thenComparing(p -> !p.getModification().isEmpty())
            .thenComparing(p -> !p.isPlainProtonationOrDeprotonation())
            .thenComparing(PrecursorIonType::isIntrinsicalCharged)
            .thenComparing(p -> !p.getAdduct().equals(MolecularFormula.parseOrNull("H3N")))
            .thenComparing(p -> !p.getAdduct().isEmpty())
            .thenComparing(p -> !p.getInSourceFragmentation().isEmpty())
            .thenComparing(p -> p.getAdduct().getMass())
            .thenComparing(p -> p.getInSourceFragmentation().getMass());

}
