/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.ms_viewer.data;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

public class SiriusSingleSpectrumModel implements MSViewerDataModel {

    protected final Spectrum<? extends Peak> spectrum;
    protected double scale;
    protected double minMz, maxMz;

    public SiriusSingleSpectrumModel(Spectrum<? extends Peak> spectrum) {
        this(spectrum, SiriusSingleSpectrumAnnotated.getVisibleRange(spectrum));
    }

    public SiriusSingleSpectrumModel(Spectrum<? extends Peak> spectrum, Range<Double> range) {
        this(spectrum, range.lowerEndpoint(), range.upperEndpoint());
    }

    public SiriusSingleSpectrumModel(Spectrum<? extends Peak> spectrum, double minMz, double maxMz) {
        this.spectrum = spectrum;
        this.scale = Spectrums.getMaximalIntensity(spectrum);
        this.minMz = minMz;
        this.maxMz = maxMz;
    }

    @Override
    public double minMz() {
        return minMz;
    }

    @Override
    public double maxMz() {
        return maxMz;
    }

    @Override
    public int getSize() {
        return spectrum.size();
    }

    @Override
    public double getMass(int index) {
        return spectrum.getMzAt(index);
    }

    @Override
    public double getRelativeIntensity(int index) {
        return spectrum.getIntensityAt(index) / scale;
    }

    @Override
    public double getAbsoluteIntensity(int index) {
        return spectrum.getIntensityAt(index);
    }

    @Override
    public String getMolecularFormula(int index) {
        return null;
    }

    @Override
    public PeakInformation getInformations(int index) {
        return null;
    }

    @Override
    public boolean isMarked(int index) {
        return false;
    }

    @Override
    public boolean isImportantPeak(int index) {
        return false;
    }

    @Override
    public boolean isUnimportantPeak(int index) {
        return false;
    }

    @Override
    public boolean isPlusZeroPeak(int index) {
        return false;
    }

    @Override
    public boolean isIsotope(int index) {
        return false;
    }

    @Override
    public int[] getIsotopePeaks(int index) {
        return new int[0];
    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public int getIndexWithMass(double mass) {
        return Spectrums.mostIntensivePeakWithin(spectrum, mass, new Deviation(1, 0.1));
    }

    @Override
    public int findIndexOfPeak(double mass, double tolerance) {
        return Spectrums.mostIntensivePeakWithin(spectrum, mass, new Deviation(1, tolerance));
    }

    @Override
    public String getIonization(int index) {
        return null;
    }
}
