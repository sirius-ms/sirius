
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

/**
 * Ionization mode in which the adduct is unknown and only the charge is known.
 * This is the default mode as long as the user don't give an ionization mode as input.
 */
public class Charge extends Ionization {
    /*
    it seems that Na - Na+ ~= H - H+ ~=
     */
    // source: Mohr, Peter J. and Taylor, Barry N. and Newell, David B., Rev. Mod. Phys. 2012.
	public final static double ELECTRON_MASS =   0.00054857990946d;
	private final int charge;
	
	public Charge(int charge) {
		this.charge = charge;
	}

    /**
     * [M + H+] ~= [M + H] - [H - H+] {@literal =>} "Ion mass" = -charge * (H-H+) = difference between molecule+H and molecule+ion
     *
     * @return
     */
	@Override
	public double getMass() {
		return -charge * ELECTRON_MASS;
	}

	@Override
	public int getCharge() {
		return charge;
	}

	@Override
	public String getName() {
		return charge > 0 ? POSITIVE_CHARGE : (charge < 0 ? NEGATIVE_CHARGE : UNKNOWN_CHARGE);
	}

	public static final String POSITIVE_CHARGE = "[M + ?]+";
	public static final String NEGATIVE_CHARGE ="[M + ?]-";
	public static final String UNKNOWN_CHARGE = "[M + ?]";

	

}
