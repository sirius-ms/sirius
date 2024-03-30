
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
import de.unijena.bioinf.ms.annotations.DataAnnotation;

/**
 * To measure a molecule in a mass spectrometer, the molecule have to be ionized. We distinguish two different
 * ionization forms:
 * - the molecule is ionized but the adduct is unknown. In this case we can only guess the charge from the isotopic
 *   pattern. The adduct is a subformula of the predicted molecular formula
 * - the molecule is ionized by a known adduct. In this case we know the charge and can subtract the mass of the adduct
 *   from the input spectra. Therefore, the adduct is not included in the resulting molecular formula
 *   
 *  The algorithms should compute the neutral mass spectra as early as possible. The neutral mass spectrum is a spectrum
 *  where each peak contains only the masses (not mass-to-charge ratio) of the measured molecules (without adducts).
 */
public abstract class Ionization implements Comparable<Ionization>, DataAnnotation {

    /**
     * @return mass (Dalton) of the ion
     */
    public abstract double getMass();
    
    /**
     * computes the mass-to-charge ratio for an ion with the given theoretical mass and this as ionization.
     * This is done by adding the mass of the ion to the given mass and divide the result by the
     * charge of the ion.
     * @return mass-to-charge ratio for a given neutral mass
     */
    public double addToMass(double omass) {
        return (omass + getMass()) / chargeNumber();
    }

    /**
     * computes the neutral mass for the given mass-to-charge ratio. This is done by multiplying the
     * mass-to-charge ratio by the charge and subtracting the mass of the ion. 
     */
    public double subtractFromMass(double omass) {
        return (omass*chargeNumber()) - getMass();
    }

    public abstract int getCharge();
    
    public int chargeNumber() {
    	return Math.abs(getCharge());
    }

    /**
     * if the adduct is known, return its molecular formula (may be negative). Protons and electrons should not be added (because they
     * are no atoms)! In general: add only atoms to the formula which have isotopic species, because this method is
     * used for isotopic pattern generation of a formula with the given ionization mode.
     */
    public MolecularFormula getAtoms() {
    	return MolecularFormula.emptyFormula();
    }
    
    public abstract String getName();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Ionization ion = (Ionization) o;

        if (getCharge() != ion.getCharge()) return false;
        return Double.compare(ion.getMass(), getMass()) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = getMass() != +0.0d ? Double.doubleToLongBits(getMass()) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + getCharge();
        return result;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int compareTo(Ionization o) {
        return Double.compare(getMass(), o.getMass());
    }

    public static Ionization fromString(String value) {
        PrecursorIonType precursorIonType;
        try {
            precursorIonType = PeriodicTable.getInstance().ionByName(value);
        } catch (UnknownElementException e) {
            throw new IllegalArgumentException("Unknown ion mode '" + value + "'!", e);
        }

        if (precursorIonType==null || !precursorIonType.hasNeitherAdductNorInsource())
            throw new IllegalArgumentException("Unknown ion mode '" + value + "'!");
        return precursorIonType.getIonization();
    }
}
