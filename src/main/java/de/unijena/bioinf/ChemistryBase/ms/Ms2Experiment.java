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
package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.List;

/**
 * A Ms2Experiment is a MS/MS measurement of a *single* compound. If there are multiple compounds measured in your
 * spectrum, clean up and separate them into multiple Ms2Experiment instances, too!
 */
public interface Ms2Experiment extends MsExperiment{

    /**
     * @return a list of MS2 spectra belonging to this compound
     */
    public List<? extends Ms2Spectrum<? extends Peak>> getMs2Spectra();

    /**
     * @return the mass-to-charge ratio of the ion to analyze
     */
    public double getIonMass();

    /**
     * for LCMS runs only
     * @return retention time of current spectrum
     */
    public double getRetentionTime();

    /***
     * The further methods provide information which is OPTIONAL. The algorithm should be able to handle cases in
     * which this methods return NULL.
     */

    /**
     * The neutral mass is the mass of the molecule. In contrast the ion mass is the mass of the molecule + ion adduct.
     * Notice that the molecule may be also charged. That doesn't matter. Neutral says nothing about the charge, but
     * about the absence of an ionization.
     * @return the *exact* (idealized) mass of the molecule or 0 if the mass is unknown
     */
    public double getMoleculeNeutralMass();

    /**
     * @return molecular formula of the neutral molecule
     */
    public MolecularFormula getMolecularFormula();

}
