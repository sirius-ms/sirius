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

import de.unijena.bioinf.ChemistryBase.ms.AdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.MutableMs2SpectrumWithAdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrumWithAdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.utils.SpectrumWithAdditionalFields;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.SpectralParser;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.unijena.bioinf.babelms.msp.MSP.*;

/**
 * Parser for Massbank MSP format
 */

public class MSPSpectralParser extends SpectralParser {
    private final static String START = NUM_PEAKS + ":";

    @Override
    public CloseableIterator<? extends SpectrumWithAdditionalFields<Peak>> parseSpectra(BufferedReader reader) throws IOException {
        return new MSPSpectraIterator(reader);
    }


    @Nullable
    SpectrumWithAdditionalFields<Peak> parseSpectrum(BufferedReader reader) throws IOException {
        return parseSpectrum(reader, null);
    }

    SpectrumWithAdditionalFields<Peak> parseSpectrum(BufferedReader reader, @Nullable AdditionalFields prevMetaInfo) throws IOException {

        AdditionalFields metaInfo = new AdditionalFields(false);
        String line;
        int peaks = -1;
        List<String> comments = new ArrayList<>();
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
                        final String key = line.substring(0, split).strip();
                        final String value = line.substring(split + 1).strip();
                        if (Arrays.stream(COMMENTS).anyMatch(key::equalsIgnoreCase)) {
                            comments.add(value);
                        } else {
                            metaInfo.put(key, value);
                        }
                    } else {
                        LoggerFactory.getLogger("Meta data key '" + line.substring(0, split) + "' does not have any value. Skipping...");
                    }
                }
            }
        }

        if (!comments.isEmpty())
            metaInfo.put(COMMENTS[0], String.join(COMMENT_SEPARATOR, comments));

        for (String c : comments) {
            parseMetadataComment(c).forEach(metaInfo::putIfAbsent);
        }

        //multi MS/MS .mat format extension -> MS-Dial export
        if (prevMetaInfo != null && metaInfo.size() == 1 && metaInfo.containsKey(SPEC_TYPE[1])) {
            prevMetaInfo.forEach(metaInfo::putIfAbsent);
            metaInfo.put("MAT_ADDITIONAL_SPEC", "TRUE");
        }

        SpectrumWithAdditionalFields<Peak> spectrum;
        {
            double[] masses = new double[peaks];
            double[] intensities = new double[peaks];

            for (int i = 0; i < masses.length; ) {
                line = reader.readLine();
                if (line == null)
                    return null;
                if (!line.isBlank()) {
                    String[] sl = line.strip().split("\\s+");
                    masses[i] = Utils.parseDoubleWithUnknownDezSep(sl[0]);
                    intensities[i] = Utils.parseDoubleWithUnknownDezSep(sl[1]);
                    i++;
                }
            }

            spectrum = new SimpleSpectrumWithAdditionalFields(masses, intensities);

            if (spectrum.isEmpty())
                LoggerFactory.getLogger(getClass()).error("0 Peaks found in current Block, Returning empty spectrum with meta data");
        }

        String msLevel = MSP.getWithSynonyms(metaInfo, SPEC_TYPE).orElse(null);
        if ((msLevel == null && (getWithSynonyms(metaInfo, PRECURSOR_MZ).isPresent() || (metaInfo.getField(SYN_PRECURSOR_MZ).filter(s -> !s.isBlank()).isPresent())) ||
                (msLevel != null && !("MS".equalsIgnoreCase(msLevel) || "MS1".equalsIgnoreCase(msLevel))))) { // we have MSn
            spectrum = new MutableMs2SpectrumWithAdditionalFields(
                    spectrum,
                    MSP.parsePrecursorMZ(metaInfo).orElseThrow(() -> new IOException("Could neither parse '" + Arrays.toString(PRECURSOR_MZ) + "' nor '" + EXACT_MASS + "'!")),
                    MSP.parseCollisionEnergy(metaInfo).orElse(null),
                    msLevel == null ? 2 : Character.getNumericValue(msLevel.charAt(msLevel.indexOf("MS") + 2))
            );

            ((MutableMs2Spectrum) spectrum).setIonization(MSP.parsePrecursorIonType(metaInfo)
                    .orElseThrow(() -> new IOException("Could neither parse '" +  Arrays.toString(PRECURSOR_ION_TYPE) + "' nor '" +  Arrays.toString(CHARGE) + "!"))
                    .getIonization());
        }

        spectrum.setAdditionalFields(metaInfo);

        return spectrum;

    }

    private class MSPSpectraIterator implements CloseableIterator<SpectrumWithAdditionalFields<Peak>> {
        private SpectrumWithAdditionalFields<Peak> next;
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

    /**
     * Extracts keys and values from a comment in form "k1=v1" "k2=v2", common in MoNA records.
     * For comments not in this form returns empty map.
     */
    private Map<String, String> parseMetadataComment(String comment) {
        Map<String, String> result = new HashMap<>();
        Pattern p = Pattern.compile("\"(?<key>[^\"=]*)=(?<value>[^\"]*)\"");
        Matcher m = p.matcher(comment);
        while (m.find()) {
            result.put(m.group("key"), m.group("value"));
        }
        return result;
    }
}
