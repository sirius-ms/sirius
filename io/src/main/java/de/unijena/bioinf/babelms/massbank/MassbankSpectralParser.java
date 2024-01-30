/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.babelms.massbank;

import de.unijena.bioinf.ChemistryBase.ms.AdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.MutableMs2SpectrumWithAdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrumWithAdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.utils.SpectrumWithAdditionalFields;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.SpectralParser;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static de.unijena.bioinf.babelms.massbank.MassbankFormat.*;

/**
 * Parser for Massbank MSP format
 */
public class MassbankSpectralParser extends SpectralParser {
    public static final String MULTILINE_INDENT = "  ";

    @Override
    public CloseableIterator<? extends SpectrumWithAdditionalFields<Peak>> parseSpectra(BufferedReader reader) throws IOException {
        return new Iterator(reader);
    }


    @Nullable
    SpectrumWithAdditionalFields<Peak> parseSpectrum(BufferedReader reader) throws IOException {
        List<String> peakAnnotationsCSV = new ArrayList<>();
        AdditionalFields metaInfo = new AdditionalFields(false);
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.isBlank())
                continue;

            if (line.startsWith(PK_ANNOTATION.k())) {
                while ((line = reader.readLine()).startsWith(MULTILINE_INDENT))
                    peakAnnotationsCSV.add(line.strip());
            }

            if (line.startsWith(PK_PEAK.k())) {
                int peaks = metaInfo.getField(PK_NUM_PEAK.k()).map(Integer::parseInt).orElse(-1);
                SpectrumWithAdditionalFields<Peak> spectrum;
                {
                    double[] masses = new double[peaks];
                    double[] intensities = new double[peaks];

                    for (int i = 0; i < masses.length; ) {
                        line = reader.readLine();
                        if (line == null) {
                            LoggerFactory.getLogger(getClass()).warn("Unexpected end of peak list. Ended at '" + i + "/" + peaks + "'.");
                            break;
                        }
                        if (!line.isBlank()) {
                            String[] sl = line.strip().split("\\s+");
                            masses[i] = Double.parseDouble(sl[0]);
                            intensities[i] = Double.parseDouble(sl[1]);
                            i++;
                        }
                    }

                    spectrum = new SimpleSpectrumWithAdditionalFields(masses, intensities);

                    if (spectrum.isEmpty())
                        LoggerFactory.getLogger(getClass()).error("0 Peaks found in current Block, Returning empty spectrum with meta data");
                }

                String msLevel = metaInfo.get(AC_MASS_SPECTROMETRY_MS_TYPE.k());
                if ((msLevel == null && (metaInfo.containsKey(MS_FOCUSED_ION_PRECURSOR_MZ.k()))) ||
                        (msLevel != null && (!"MS".equalsIgnoreCase(msLevel)) && !"MS1".equalsIgnoreCase(msLevel))) { // we have MSn
                    spectrum = new MutableMs2SpectrumWithAdditionalFields(
                            spectrum,
                            parsePrecursorMZ(metaInfo).orElseThrow(() -> new IOException("Could not parse '" + MS_FOCUSED_ION_PRECURSOR_MZ.k() + "':'" + metaInfo.get(MS_FOCUSED_ION_PRECURSOR_MZ.k()) + "' OR '" + CH_EXACT_MASS.k() + "':'" + metaInfo.get(CH_EXACT_MASS.k()) + "'.")),
                            metaInfo.getField(AC_MASS_SPECTROMETRY_COLLISION_ENERGY.k()).map(CollisionEnergy::fromStringOrNull).orElse(null),
                            msLevel == null || msLevel.isBlank() ? 2 : Character.getNumericValue(msLevel.charAt(msLevel.indexOf("MS") + 2))
                    );

                    ((MutableMs2Spectrum) spectrum).setIonization(parsePrecursorIonType(metaInfo)
                            .orElseThrow(() -> new IOException("Could neither parse '" + MS_FOCUSED_ION_ION_TYPE.k() + "' nor '" + AC_MASS_SPECTROMETRY_ION_MODE.k() + "!"))
                            .getIonization());
                }

                spectrum.setAdditionalFields(metaInfo);
                return spectrum;
            }


            withKeyValue(line, metaInfo::put);
        }

        LoggerFactory.getLogger("Unexpected end of Stream. No Peaks found! No spectrum resturned");
        return null;

    }


    private class Iterator implements CloseableIterator<SpectrumWithAdditionalFields<Peak>> {
        private SpectrumWithAdditionalFields<Peak> next;
        private final BufferedReader reader;

        public Iterator(BufferedReader reader) throws IOException {
            this.reader = reader;
            next = parseSpectrum(this.reader);
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public SpectrumWithAdditionalFields<Peak> next() {
            SpectrumWithAdditionalFields<Peak> current = next;
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
