/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.spectraldb.entities;

import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.Arrays;

public class Ms2SpectralData extends SimpleMutableSpectrum implements OrderedSpectrum<Peak> {

    private long id = -1L;
    private long metaId = -1L;
    private final double precursorMz;

    public Ms2SpectralData(MutableMs2Spectrum spectrum) {
        super();
        this.precursorMz = spectrum.getPrecursorMz();
        final MutableSpectrum<? extends Peak> t = new SimpleMutableSpectrum(spectrum);
        Spectrums.sortSpectrumByMass(t);
        this.masses = new TDoubleArrayList(Spectrums.copyMasses(t));
        this.intensities = new TDoubleArrayList(Spectrums.copyIntensities(t));
    }

    public Ms2SpectralData(long id, long metaId,  double precursorMz, double[] masses, double[] intensities) {
        super();
        this.id = id;
        this.metaId = metaId;
        this.precursorMz = precursorMz;
        this.masses = new TDoubleArrayList(Arrays.copyOf(masses, masses.length));
        this.intensities = new TDoubleArrayList(Arrays.copyOf(intensities, intensities.length));
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getMetaId() {
        return metaId;
    }

    public void setMetaId(long metaId) {
        this.metaId = metaId;
    }

    public double getPrecursorMz() {
        return precursorMz;
    }

    public double[] getMasses() {
        return this.masses.toArray();
    }

    public double[] getIntensities() {
        return this.intensities.toArray();
    }
}
