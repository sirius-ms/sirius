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
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.SpectralParser;
import de.unijena.bioinf.babelms.utils.ParserUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
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

    @Override
    public CloseableIterator<? extends SpectrumWithAdditionalFields<Peak>> parseSpectra(BufferedReader reader) throws IOException {
        return new MSPSpectraIterator(reader);
    }


    @Nullable
    SpectrumWithAdditionalFields<Peak> parseSpectrum(BufferedReader reader) throws IOException {
        return parseSpectrum(reader, null);
    }

    /**
     * A record whose spectrum cannot be read as declared - the peak list is the measurement, so the record must not
     * be imported. The reader is left at the start of the next record, so parsing can go on from there.
     */
    static class MalformedRecordException extends RuntimeException {
        /**
         * The problem belongs to the spectrum read before: either its peak count was too low, or the broken spectrum
         * is an additional spectrum of the same record (MS-DIAL .mat).
         */
        final boolean ofPreviousSpectrum;
        /** Name of the broken record, if its header had one. */
        @Nullable
        final String recordName;

        MalformedRecordException(String message, AdditionalFields header, @Nullable List<String> comments, boolean ofPreviousSpectrum) {
            this(message, null, header, comments, ofPreviousSpectrum);
        }

        MalformedRecordException(String message, @Nullable Throwable cause, AdditionalFields header, @Nullable List<String> comments, boolean ofPreviousSpectrum) {
            super(cause == null ? message : message + ": " + cause.getMessage(), cause);
            this.ofPreviousSpectrum = ofPreviousSpectrum;
            this.recordName = recordName(header, comments);
        }
    }

    /**
     * Named like in every other message about the record (e.g. the MS-DIAL PEAKID from a comment) - never failing, it
     * is used while reporting a broken record.
     */
    @Nullable
    private static String recordName(AdditionalFields header, @Nullable List<String> comments) {
        try {
            return MSP.parseName(header, comments == null || comments.isEmpty() ? MSP.extractComments(header).orElse(null) : comments).orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * One line read ahead and put back, e.g. the first line of the next record after a broken one - kept here instead
     * of BufferedReader.mark/reset, which fails for lines longer than the read-ahead limit.
     */
    @Nullable
    private String pendingLine;
    @Nullable
    private BufferedReader pendingLineReader;

    private String readLine(BufferedReader reader) throws IOException {
        if (pendingLine != null && pendingLineReader == reader) {
            final String line = pendingLine;
            pendingLine = null;
            pendingLineReader = null;
            return line;
        }
        pendingLine = null;
        pendingLineReader = null;
        return reader.readLine();
    }

    private void unreadLine(BufferedReader reader, String line) {
        pendingLine = line;
        pendingLineReader = reader;
    }

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /** Peaks allocated before any peak line is read - a record with more grows as its peak lines arrive. */
    private static final int INITIAL_PEAK_CAPACITY = 4096;

    /**
     * @return true if the first non-blank character of the line can start a number - header keys never do.
     */
    private static boolean startsLikeNumber(String line) {
        for (int i = 0; i < line.length(); i++) {
            final char c = line.charAt(i);
            if (!Character.isWhitespace(c))
                return Character.isDigit(c) || c == '.' || c == '+' || c == '-';
        }
        return false;
    }

    /**
     * A peak line starts with two whitespace separated numbers (m/z and intensity), anything after them is ignored.
     *
     * @param peak receives m/z and intensity if the line is a peak line, may be null
     */
    static boolean parsePeakLine(String line, @Nullable double[] peak) {
        if (!startsLikeNumber(line))
            return false;
        final String[] tokens = WHITESPACE.split(line.strip(), 3);
        if (tokens.length < 2)
            return false;
        final double mz = ParserUtils.parseDecimal(tokens[0]);
        final double intensity = ParserUtils.parseDecimal(tokens[1]);
        if (!Double.isFinite(mz) || !Double.isFinite(intensity))
            return false; // not a number, NaN, Infinity
        if (peak != null) {
            peak[0] = mz;
            peak[1] = intensity;
        }
        return true;
    }

    static boolean isPeakLine(String line) {
        return parsePeakLine(line, null);
    }

    /** 'mz:intensity' - the m/z has a decimal separator, unlike a time (10:30) or a ratio (1:1). */
    private static final Pattern COLON_SEPARATED_PEAK = Pattern.compile("^\\s*[+-]?\\d*[.,]\\d+\\s*:\\s*[+-]?\\.?\\d");

    /**
     * Peak data, even if not in a supported peak format - e.g. 'mz:intensity' would otherwise be read as a key.
     */
    private static boolean isPeakData(String line) {
        return isPeakLine(line) || (startsLikeNumber(line) && COLON_SEPARATED_PEAK.matcher(line).find());
    }

    /**
     * Skips to the next record, which starts with the next 'key: value' line. That line stays unread.
     */
    private void skipToNextRecord(BufferedReader reader) throws IOException {
        while (true) {
            final String line = readLine(reader);
            if (line == null)
                return;
            if (!line.isBlank() && isKeyLine(line)) {
                unreadLine(reader, line);
                return;
            }
        }
    }

    /**
     * Skips the rest of a broken record: up to its 'Num Peaks' and past the peak list after it.
     */
    private void skipPastPeakList(BufferedReader reader) throws IOException {
        String line;
        while ((line = readLine(reader)) != null) {
            if (isNumPeaksLine(line)) {
                int peaks = Math.max(0, parseNumPeaks(line));
                while (peaks > 0 && (line = readLine(reader)) != null) {
                    if (line.isBlank())
                        continue;
                    if (isKeyLine(line)) {
                        unreadLine(reader, line);
                        return;
                    }
                    peaks--;
                }
                skipToNextRecord(reader);
                return;
            }
        }
    }

    /** 'Num Peaks: N' - recognised like any other key, also indented or with blanks before the ':'. */
    private static boolean isNumPeaksLine(String line) {
        final int split = line.indexOf(':');
        return split > 0 && line.substring(0, split).strip().equalsIgnoreCase(NUM_PEAKS);
    }

    /** @return the declared number of peaks, or -1 if it is not a valid number */
    private static int parseNumPeaks(String numPeaksLine) {
        try {
            return Math.max(-1, Integer.parseInt(numPeaksLine.substring(numPeaksLine.indexOf(':') + 1).strip()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** A 'key: value' line - keys never start like a number, so peak data (also 'mz:intensity') is never one. */
    private static boolean isKeyLine(String line) {
        return line.indexOf(':') >= 0 && !startsLikeNumber(line);
    }

    /**
     * @throws MalformedRecordException if the spectrum cannot be read as declared. The reader is then positioned at
     *                                  the start of the next record.
     */
    SpectrumWithAdditionalFields<Peak> parseSpectrum(BufferedReader reader, @Nullable AdditionalFields prevMetaInfo) throws IOException {

        AdditionalFields metaInfo = new AdditionalFields(false);
        String line;
        int peaks = -1;
        List<String> comments = new ArrayList<>();
        String lastKey = null;
        boolean headerLineRead = false; // also lines that are not stored, e.g. keys without value
        while (peaks == -1) {
            line = readLine(reader);

            if (line == null)
                return null; // Stream end -> no peaks to read -> no spectrum
            if (line.isBlank())
                continue;

            if (isPeakData(line)) {
                if (!headerLineRead) {
                    // no header read yet: these peaks continue the previous spectrum, whose peak count was too low
                    skipToNextRecord(reader);
                    throw new MalformedRecordException("Peak lines after the end of a peak list - '" + NUM_PEAKS + "' of the previous spectrum is too low", metaInfo, comments, true);
                }
                // 'Num Peaks' is missing, or a wrapped value starts with numbers - either way, drop the record as a
                // whole. If 'Num Peaks' is missing, the end found is the one of the next record, which is lost as well.
                skipPastPeakList(reader);
                throw new MalformedRecordException("Peak lines inside the header - '" + NUM_PEAKS + "' is missing, or a value is broken", metaInfo, comments, isAdditionalSpectrum(metaInfo, prevMetaInfo));
            }

            headerLineRead = true;
            if (isNumPeaksLine(line)) {
                peaks = parseNumPeaks(line);
                if (peaks < 0) {
                    skipToNextRecord(reader);
                    throw new MalformedRecordException("'" + line.strip() + "' is not a valid number of peaks", metaInfo, comments, isAdditionalSpectrum(metaInfo, prevMetaInfo));
                }
            } else {
                if (line.startsWith(SYNONYME_KEY)) {
                    final int keyLength = SYNONYME_KEY.length() + 2; // Synon: $:NN
                    if (line.length() >= keyLength) {
                        lastKey = line.substring(0, keyLength);
                        metaInfo.put(lastKey, line.substring(keyLength));
                    } else
                        LoggerFactory.getLogger(getClass()).warn("Synonym '" + line + "' has no synonym number. Skipping...");
                } else {
                    int split = line.indexOf(':');
                    if (split >= 0 && split < line.length() - 1) {
                        final String key = line.substring(0, split).strip();
                        final String value = line.substring(split + 1).strip();
                        lastKey = key;
                        if (Arrays.stream(COMMENTS).anyMatch(key::equalsIgnoreCase)) {
                            comments.add(value);
                        } else {
                            metaInfo.put(key, value);
                        }
                    } else if (split < 0) {
                        LoggerFactory.getLogger(getClass()).warn("Line '" + line + "' is not of the form 'key: value'. Skipping it."
                                + (lastKey != null ? " The value of '" + lastKey + "' before it may be broken." : ""));
                    } else {
                        lastKey = line.substring(0, split).strip();
                        LoggerFactory.getLogger(getClass()).debug("Meta data key '" + lastKey + "' does not have any value. Skipping...");
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
        final boolean additional = isAdditionalSpectrum(metaInfo, prevMetaInfo);
        if (additional) {
            prevMetaInfo.forEach(metaInfo::putIfAbsent);
            metaInfo.put("MAT_ADDITIONAL_SPEC", "TRUE");
        }

        SpectrumWithAdditionalFields<Peak> spectrum;
        {
            // the declared count only proves itself by the peak lines that follow - do not allocate for it up front
            final int initialCapacity = Math.min(peaks, INITIAL_PEAK_CAPACITY);
            final DoubleArrayList mzValues = new DoubleArrayList(initialCapacity);
            final DoubleArrayList intensities = new DoubleArrayList(initialCapacity);

            final double[] peak = new double[2];
            while (mzValues.size() < peaks) {
                line = readLine(reader);
                if (line == null)
                    throw new MalformedRecordException("File ends inside the peak list, " + mzValues.size() + " of " + peaks + " peaks read", metaInfo, comments, additional);
                if (line.isBlank())
                    continue;
                if (!parsePeakLine(line, peak)) {
                    final boolean nextHeader = isKeyLine(line);
                    unreadLine(reader, line);
                    skipToNextRecord(reader);
                    throw new MalformedRecordException(nextHeader
                            ? "'" + NUM_PEAKS + ": " + peaks + "' is too high, the peak list ends after " + mzValues.size() + " peaks"
                            : "Malformed peak line '" + line + "'", metaInfo, comments, additional);
                }
                mzValues.add(peak[0]);
                intensities.add(peak[1]);
            }

            spectrum = new SimpleSpectrumWithAdditionalFields(toArray(mzValues), toArray(intensities));

            if (spectrum.isEmpty())
                LoggerFactory.getLogger(getClass()).error("0 Peaks found in current Block, Returning empty spectrum with meta data");
        }

        try {
            // SIRIUS only needs to know whether this is a fragmentation spectrum - without a usable spectrum type, a
            // precursor m/z says so
            final int msLevel = ParserUtils.inferMsLevel(MSP.getWithSynonyms(metaInfo, SPEC_TYPE).orElse(null),
                    MSP.getGivenPrecursorMzField(metaInfo).orElse(null), () -> MSP.parsePrecursorMZ(metaInfo).isPresent(),
                    LoggerFactory.getLogger(getClass()), () -> Optional.ofNullable(recordName(metaInfo, comments)).orElse("N/A"));
            if (msLevel > 1) { // we have MSn
                final double precursorMz = MSP.parsePrecursorMZ(metaInfo).orElseThrow(() -> new MalformedRecordException("Could neither parse '" + Arrays.toString(PRECURSOR_MZ) + "', '" + SYN_PRECURSOR_MZ + "' nor '" + EXACT_MASS + "' with a known adduct", metaInfo, comments, additional));
                spectrum = new MutableMs2SpectrumWithAdditionalFields(
                        spectrum,
                        precursorMz,
                        MSP.parseCollisionEnergy(metaInfo).orElse(null),
                        msLevel
                );

                ((MutableMs2Spectrum) spectrum).setIonization(MSP.parsePrecursorIonType(metaInfo, false) // warned once per record, by the experiment parser
                        .orElseThrow(() -> new MalformedRecordException("Could neither parse '" + Arrays.toString(PRECURSOR_ION_TYPE) + "' nor '" + Arrays.toString(CHARGE) + "'", metaInfo, comments, additional))
                        .getIonization());
            }
        } catch (MalformedRecordException e) {
            throw e;
        } catch (RuntimeException e) {
            // the whole record has been read, the reader is already at the next one
            throw new MalformedRecordException("Spectrum cannot be created", e, metaInfo, comments, additional);
        }

        spectrum.setAdditionalFields(metaInfo);

        return spectrum;

    }

    /** The backing array if the list is exactly full, as usual - the spectrum copies it anyway. */
    private static double[] toArray(DoubleArrayList list) {
        return list.size() == list.elements().length ? list.elements() : list.toDoubleArray();
    }

    /**
     * MS-DIAL .mat records hold several spectra; the header of each one after the first is only its MSTYPE.
     */
    private static boolean isAdditionalSpectrum(AdditionalFields metaInfo, @Nullable AdditionalFields prevMetaInfo) {
        return prevMetaInfo != null && metaInfo.size() == 1 && metaInfo.containsKey(SPEC_TYPE[1]);
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
