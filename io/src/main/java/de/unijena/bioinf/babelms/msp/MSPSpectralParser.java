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

package de.unijena.bioinf.babelms.msp;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.SpectralParser;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;

import static de.unijena.bioinf.babelms.msp.MSP.*;

/**
 * Parser for Massbank MSP format
 */
public class MSPSpectralParser extends SpectralParser {
    private final static String START = NUM_PEAKS + ":";


    @Override
    public CloseableIterator<? extends AnnotatedSpectrum<Peak>> parseSpectra(BufferedReader reader) throws IOException {
        return new MSPSpectraIterator(reader);
    }


    @Nullable
    AnnotatedSpectrum<Peak> parseSpectrum(BufferedReader reader) throws IOException {

        AdditionalFields metaInfo = new AdditionalFields(false);
        String line;
        int peaks = -1;

        while (peaks == -1) {
            line = reader.readLine();

            if (line == null)
                return null; // Stream end -> no peaks to read -> no spectrum
            if (line.isBlank())
                continue;

            if (line.toLowerCase().startsWith(START.toLowerCase())) {
                peaks = Integer.parseInt(line.substring(line.indexOf(':') + 1).strip());
            } else {
                if (line.startsWith(SYNONYME_KEY)) {
                    metaInfo.put(line.substring(0, 11), line.substring(11));
                } else {
                    int split = line.indexOf(':');
                    if (split >= 0 && split < line.length() - 1) {
                        metaInfo.put(
                                line.substring(0, split).strip(),
                                line.substring(split + 1).strip());
                    } else {
                        LoggerFactory.getLogger("Meta data key '" + line.substring(0, split) + "' does not have any value. Skipping...");
                    }
                }
            }
        }

        AnnotatedSpectrum<Peak> spectrum;
        {
            double[] masses = new double[peaks];
            double[] intensities = new double[peaks];

            for (int i = 0; i < masses.length; ) {
                line = reader.readLine();
                if (line == null)
                    return null;
                if (!line.isBlank()) {
                    String[] sl = line.strip().split("\\s+");
                    masses[i] = Double.parseDouble(sl[0]);
                    intensities[i] = Double.parseDouble(sl[1]);
                    i++;
                }
            }

            spectrum = new SimpleSpectrum(masses, intensities);

            if (spectrum.isEmpty())
                LoggerFactory.getLogger(getClass()).error("0 Peaks found in current Block, Returning empty spectrum with meta data");
        }

        String msLevel = metaInfo.get(SPEC_TYPE);
        if (metaInfo.containsKey(PRECURSOR_MZ) || metaInfo.containsKey(SYN_PRECURSOR_MZ) || !("MS".equalsIgnoreCase(msLevel) || "MS1".equalsIgnoreCase(msLevel))) { // we have MSn
            spectrum = new MutableMs2Spectrum(
                    spectrum,
                    MSP.parsePrecursorMZ(metaInfo).orElseThrow(() -> new IOException("Could neither parse '" + PRECURSOR_MZ + "' nor '" + EXACT_MASS + "'!")),
                    MSP.parseCollisionEnergy(metaInfo).orElse(null),
                    msLevel == null ? 2 : Character.getNumericValue(msLevel.charAt(msLevel.indexOf("MS") + 2))
            );

            ((MutableMs2Spectrum) spectrum).setIonization(MSP.parsePrecursorIonType(metaInfo)
                    .orElseThrow(() -> new IOException("Could neither parse '" + PRECURSOR_ION_TYPE + "' nor '" + CHARGE + "!"))
                    .getIonization());
        }

        spectrum.setAnnotation(AdditionalFields.class, metaInfo);

        return spectrum;

    }

    private class MSPSpectraIterator implements CloseableIterator<AnnotatedSpectrum<Peak>> {
        private AnnotatedSpectrum<Peak> next;
        private final BufferedReader reader;

        public MSPSpectraIterator(BufferedReader reader) throws IOException {
            this.reader = reader;
            next = parseSpectrum(this.reader);
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public AnnotatedSpectrum<Peak> next() {
            AnnotatedSpectrum<Peak> current = next;
            try {
                next = parseSpectrum(reader);
            } catch (IOException e) {
                throw new RuntimeException("Error when parsing Spectrum!", e);
            }
            return current;
        }

        @Override
        public void close() throws IOException {
            if (reader != null)
                reader.close();
        }
    }

}
