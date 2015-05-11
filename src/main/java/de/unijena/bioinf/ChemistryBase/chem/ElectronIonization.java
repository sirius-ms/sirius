/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.ChemistryBase.chem;

/**
 * EI Ionization mode. An electron is dislodged from the neutral molecule.
 * The getMass() is negative to make addToMass() and subtractFromMass() work as indicated.
 */
public class ElectronIonization extends Ionization {


    public ElectronIonization(){
    }

    @Override
    public double getMass() {
        return -Charge.ELECTRON_MASS;
    }

    @Override
    public int getCharge() {
        return 1;
    }

    @Override
    public String getName() {
        return "M+.";
    }
}
