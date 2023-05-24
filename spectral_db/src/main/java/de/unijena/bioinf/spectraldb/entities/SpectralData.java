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

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.Arrays;

public class SpectralData extends SimpleMutableSpectrum {

    private long id = -1L;
    private long metaId = -1L;

    public <T extends Peak, S extends Spectrum<T>> SpectralData(S immutable) {
        super();
        this.masses = new TDoubleArrayList(immutable.size());
        this.intensities = new TDoubleArrayList(immutable.size());
        for (Peak p: immutable) {
            this.masses.add(p.getMass());
            this.intensities.add(p.getIntensity());
        }
    }

    public SpectralData(long id, long metaId, double[] masses, double[] intensities) {
        super();
        this.id = id;
        this.metaId = metaId;
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

    public double[] getMasses() {
        return this.masses.toArray();
    }

    public double[] getIntensities() {
        return this.intensities.toArray();
    }

}
