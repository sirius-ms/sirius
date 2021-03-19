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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by kaidu on 20.06.2015.
 */
public class SpectrumBinaryWriter {

    public static void writeSpectra(OutputStream out, SimpleSpectrum[] spectra) throws IOException {
        final DataOutputStream outputStream = new DataOutputStream(out);
        writeSpectra(outputStream, spectra);
    }

    public static void writeSpectra(DataOutputStream out, SimpleSpectrum[] spectra) throws IOException {
        out.writeInt(spectra.length);
        for (int i=0; i < spectra.length; ++i) {
            final SimpleSpectrum spec = spectra[i];
            out.writeInt(spec.size());
            for (int j=0; j < spec.size(); ++j) {
                out.writeDouble(spec.getMzAt(j));
            }
            for (int j=0; j < spec.size(); ++j) {
                out.writeDouble(spec.getIntensityAt(j));
            }
        }
    }

}
