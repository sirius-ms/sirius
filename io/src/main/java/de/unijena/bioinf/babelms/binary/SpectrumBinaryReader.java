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

package de.unijena.bioinf.babelms.binary;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Created by kaidu on 20.06.2015.
 */
public class SpectrumBinaryReader {

    public static SimpleSpectrum[] readSpectra(DataInputStream in) throws IOException {
        final SimpleSpectrum[] spectra = new SimpleSpectrum[in.readInt()];
        for (int i=0; i < spectra.length; ++i) {
            final int N = in.readInt();
            final double[] mz = new double[N];
            final double[] ints = new double[N];
            for (int j=0; j < N; ++j) {
                mz[j] = in.readDouble();
            }
            for (int j=0; j < N; ++j) {
                ints[j] = in.readDouble();
            }
            spectra[i] = new SimpleSpectrum(mz, ints);
        }
        return spectra;
    }

}
