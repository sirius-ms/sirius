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
import de.unijena.bioinf.babelms.utils.ParserUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static de.unijena.bioinf.babelms.massbank.MassbankFormat.*;

/**
 * Parser for Massbank MSP format
 */
@Slf4j
public class MassbankSpectralParser extends SpectralParser {
    public static final String MULTILINE_INDENT = "  ";

    /** Peaks allocated before any peak line is read - a record with more grows as its peak lines arrive. */
    private static final int INITIAL_PEAK_CAPACITY = 4096;

    @Override
    public CloseableIterator<? extends SpectrumWithAdditionalFields<Peak>> parseSpectra(BufferedReader reader) throws IOException {
        return new Iterator(reader);
    }


    /**
     * A record that cannot be read as declared. The reader is left after the record's '//', so parsing can go on with
     * the next record.
     */
    static class MalformedRecordException extends RuntimeException {
        @Nullable
        final String accession;

        MalformedRecordException(String message, @Nullable Throwable cause, @Nullable String accession) {
            super(cause == null ? message : message + ": " + cause.getMessage(), cause);
            this.accession = accession;
        }
    }

    /**
     * @throws MalformedRecordException if a record ended by '//' cannot be read - the reader is then after its '//'
     * @throws RuntimeException         if the input ends inside a record (e.g. no MassBank file at all)
     */
    @Nullable
    SpectrumWithAdditionalFields<Peak> parseSpectrum(BufferedReader reader) throws IOException {
        List<String> peakAnnotationsCSV = new ArrayList<>();
        AdditionalFields metaInfo = new AdditionalFields(false);
        SpectrumWithAdditionalFields<Peak> spectrum = null;
        String line;
        boolean seenContent = false;
        boolean peaksRead = false;
        boolean recordEnded = false; // the '//' of this record has been read

        try {
            // an empty record ('//' before any content) does not end the loop, see below
            while ((line = reader.readLine()) == null || !isRecordEnd(line) || !seenContent) {

                if (line == null) {
                    if (seenContent) {
                        throw new EndOfStreamException();
                    } else {
                        break;
                    }
                }

                if (line.isBlank())
                    continue;
                if (!seenContent && isRecordEnd(line))
                    continue; // an empty record ('//' right after '//'), e.g. from joining files
                seenContent = true;

                // PK$PEAK is the last field of a record - anything after its peaks means the peak count is wrong
                if (peaksRead)
                    throw new MalformedRecordException("Line after the peak list - '" + PK_NUM_PEAK.k() + "' is too low: '" + line.strip() + "'", null, null);

                if (line.startsWith(PK_ANNOTATION.k())) {
                    while ((line = reader.readLine()) != null && line.startsWith(MULTILINE_INDENT) && !isRecordEnd(line)) {
                        peakAnnotationsCSV.add(line.strip());
                    }
                }

                if (line == null)
                    throw new EndOfStreamException();
                if (isRecordEnd(line)) // right after PK$ANNOTATION
                    break;
                if (line.startsWith(PK_PEAK.k())) {
                    int peaks;
                    try {
                        peaks = metaInfo.getField(PK_NUM_PEAK.k()).map(Integer::parseInt).orElse(-1);
                    } catch (NumberFormatException e) {
                        throw new MalformedRecordException("'" + PK_NUM_PEAK.k() + "' is not a number", null, null);
                    }
                    if (peaks < 0)
                        throw new MalformedRecordException("'" + PK_NUM_PEAK.k() + "' is missing or not valid", null, null);
                    {
                        // the declared count only proves itself by the peak lines that follow - do not allocate for it up front
                        final int initialCapacity = Math.min(peaks, INITIAL_PEAK_CAPACITY);
                        final DoubleArrayList mzValues = new DoubleArrayList(initialCapacity);
                        final DoubleArrayList intensities = new DoubleArrayList(initialCapacity);

                        for (int i = 0; i < peaks; ) {
                            line = reader.readLine();
                            if (line == null)
                                throw new EndOfStreamException();
                            if (isRecordEnd(line)) {
                                recordEnded = true;
                                throw new MalformedRecordException("'" + PK_NUM_PEAK.k() + " " + peaks + "' is too high, the peak list ends after " + i + " peaks", null, null);
                            }
                            if (!line.isBlank()) {
                                String[] sl = line.strip().split("\\s+");
                                if (sl.length < 2)
                                    throw new MalformedRecordException("Malformed peak line '" + line.strip() + "'", null, null);
                                final double mz = ParserUtils.parseDecimal(sl[0]);
                                final double intensity = ParserUtils.parseDecimal(sl[1]);
                                if (!Double.isFinite(mz) || !Double.isFinite(intensity))
                                    throw new MalformedRecordException("Malformed peak line '" + line.strip() + "'", null, null);
                                mzValues.add(mz);
                                intensities.add(intensity);
                                i++;
                            }
                        }

                        spectrum = new SimpleSpectrumWithAdditionalFields(mzValues.toDoubleArray(), intensities.toDoubleArray());
                        peaksRead = true;

                        if (spectrum.isEmpty())
                            log.error("0 Peaks found in current Block, Returning empty spectrum with meta data");
                    }

                    // SIRIUS only needs to know whether this is a fragmentation spectrum - without a usable MS_TYPE, a
                    // precursor field makes it MS2 if a precursor m/z can be determined at all, also from the exact mass
                    final String precursorField = getGivenPrecursorMzField(metaInfo).orElse(null);
                    final Supplier<String> accession = () -> metaInfo.getField(ACCESSION.k()).orElse("N/A");
                    final int msLevel = ParserUtils.inferMsLevel(metaInfo.get(AC_MASS_SPECTROMETRY_MS_TYPE.k()), precursorField,
                            () -> parsePrecursorMZ(metaInfo).isPresent(), log, accession);
                    if (msLevel > 1) { // we have MSn
                        final double precursorMz = parsePrecursorMZ(metaInfo).orElseThrow(() -> new MalformedRecordException("Could not parse '" + MS_FOCUSED_ION_PRECURSOR_MZ.k() + "':'" + metaInfo.get(MS_FOCUSED_ION_PRECURSOR_MZ.k()) + "' OR '" + CH_EXACT_MASS.k() + "':'" + metaInfo.get(CH_EXACT_MASS.k()) + "'.", null, null));
                        spectrum = new MutableMs2SpectrumWithAdditionalFields(
                                spectrum,
                                precursorMz,
                                metaInfo.getField(AC_MASS_SPECTROMETRY_COLLISION_ENERGY.k()).map(CollisionEnergy::fromStringOrNull).orElse(null),
                                msLevel
                        );

                        ((MutableMs2Spectrum) spectrum).setIonization(parsePrecursorIonType(metaInfo)
                                .orElseThrow(() -> new MalformedRecordException("Could neither parse '" + MS_FOCUSED_ION_ION_TYPE.k() + "' nor '" + AC_MASS_SPECTROMETRY_ION_MODE.k() + "'", null, null))
                                .getIonization());
                    }
                    continue; // the peak lines are no key-value lines
                }

                withKeyValue(line, metaInfo::putIfAbsent);
            }
            if (seenContent && spectrum == null) { // ended by '//', but without PK$PEAK
                recordEnded = true;
                throw new MalformedRecordException("Record without peak list", null, null);
            }
        } catch (EndOfStreamException e) {
            throw new RuntimeException("Unexpected end of stream in a MassBank file");
        } catch (RuntimeException e) {
            // skip the rest of the record, the next one starts after its '//'
            if (!recordEnded) {
                while ((line = reader.readLine()) != null && !isRecordEnd(line)) {
                }
                if (line == null) // no '//' to continue after
                    throw new RuntimeException("Unexpected end of stream in a MassBank file", e);
            }
            final String accession = metaInfo.get(ACCESSION.k());
            if (e instanceof MalformedRecordException m)
                throw new MalformedRecordException(m.getMessage(), m.getCause(), accession);
            throw new MalformedRecordException("Record cannot be read", e, accession);
        }
        if (spectrum != null) {
            spectrum.setAdditionalFields(metaInfo);
        }
        return spectrum;
    }

    private static boolean isRecordEnd(String line) {
        return "//".equals(line.strip());
    }

    /** The input ends inside a record - not recoverable, as there is no '//' to continue after. */
    private static class EndOfStreamException extends RuntimeException {
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
