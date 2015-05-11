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
package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

public class JenaMs2Spectrum extends JenaMsSpectrum implements Ms2Spectrum<Peak> {

    private final double precursorMz;
    private final CollisionEnergy collisionEnergy;


    public JenaMs2Spectrum(Spectrum<? extends Peak> spectrum, double precursorMz, double totalIonCount, CollisionEnergy collisionEnergy, double retentionTime) {
        super(spectrum, totalIonCount, retentionTime);
        this.precursorMz = precursorMz;
        this.collisionEnergy = collisionEnergy;
    }

    @Override
    public double getPrecursorMz() {
        return precursorMz;
    }

    @Override
    public CollisionEnergy getCollisionEnergy() {
        return collisionEnergy;
    }

    @Override
    public Ionization getIonization() {
        return PeriodicTable.getInstance().ionByName("[M+H]+");
    }

    @Override
    public int getMsLevel() {
        return 2;
    }
}
