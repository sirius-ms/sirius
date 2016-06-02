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
package de.unijena.bioinf.IsotopePatternAnalysis.util;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;

public class LinearIntensityDependency implements IntensityDependency {

    private double zeroIntensity, fullIntensity;

    public LinearIntensityDependency(){
        this.zeroIntensity = 1;
        this.fullIntensity = 1;
    }

    public LinearIntensityDependency(double fullIntensity, double zeroIntensity) {
        this.zeroIntensity = zeroIntensity;
        this.fullIntensity = fullIntensity;
    }



    public double getZeroIntensity() {
        return zeroIntensity;
    }

    public void setZeroIntensity(double zeroIntensity) {
        this.zeroIntensity = zeroIntensity;
    }

    public double getFullIntensity() {
        return fullIntensity;
    }

    public void setFullIntensity(double fullIntensity) {
        this.fullIntensity = fullIntensity;
    }

    @Override
    public double getValueAt(double intensity) {
        return fullIntensity*(intensity) + zeroIntensity*(1-intensity);
    }

    @Override
    public <G, D, L> LinearIntensityDependency readFromParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final double zeroIntensity = document.getDoubleFromDictionary(dictionary, "zeroIntensity");
        final double fullIntensity = document.getDoubleFromDictionary(dictionary, "fullIntensity");
        return new LinearIntensityDependency(fullIntensity, zeroIntensity);
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "zeroIntensity", zeroIntensity);
        document.addToDictionary(dictionary, "fullIntensity", fullIntensity);
    }
}
