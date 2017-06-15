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

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;

/**
 * The information in this profile should represent properties of the instrument, the measurement and the quality
 * of the data. It should not contains details or parameters of the scorings.
 * For example: meanNoiseIntensity may be an attribute in this interface. The derived parameter lambda for
 * noise probability is NO attribute of this interface!
 * In practice, instances of this class could be provided by a factory. e.g. ProfileFactory.create(MsInstrumentation.TOF, Quality.HIGH);
 * or modified by the user (e.g. myProfile.setExpectedFragmentMassDeviation(20) to consider a unexpected low quality of MS2 spectra).
 */
public interface MeasurementProfile {

    Deviation getAllowedMassDeviation();

    Deviation getStandardMassDifferenceDeviation();

    Deviation getStandardMs1MassDeviation();

    Deviation getStandardMs2MassDeviation();

    FormulaConstraints getFormulaConstraints();

    double getIntensityDeviation();

    double getMedianNoiseIntensity();

}
