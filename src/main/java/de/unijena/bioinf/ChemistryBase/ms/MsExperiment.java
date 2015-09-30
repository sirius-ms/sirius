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

import de.unijena.bioinf.ChemistryBase.chem.Ionization;

import java.util.List;

public interface MsExperiment {

    /**
     * @return the measurement profile which contains information about the expected measurement quality
     */
    public MeasurementProfile getMeasurementProfile();

    /**
     * @return the ionization type of the ion or null if this type is unknown
     */
    public Ionization getIonization();


    /**
     * Notes:
     * - If the data is preprocessed, then there should be a *clean* and single isotope pattern of the
     * ion in each ms1 spectrum. Further peaks are not allowed!
     * @return a list of MS1 spectra with the isotope pattern of this compound
     */
    public List<? extends Spectrum<Peak>> getMs1Spectra();

    /**
     * In practice it seems more accurate to merge all MS1 spectra into a single one and only use this for further
     * analysis. Some tools provide a very accurate peak picking and merging, such that this is something which
     * should not be done in SIRIUS itself.
     * @return merge all ms1 spectra to single one
     */
    public <T extends Spectrum<Peak>> T getMergedMs1Spectrum();

}
