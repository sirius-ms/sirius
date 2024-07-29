/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms.massbank;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * MassBankRecordFormat as described in
 * @see <a href="https://github.com/MassBank/MassBank-web/blob/main/Documentation/MassBankRecordFormat.md">http://google.com</a>
 */
@Getter
@Slf4j
public enum MassbankFormat {
    ACCESSION(),
    RECORD_TITLE(),
    DATE(),
    AUTHORS(),
    LICENSE(),
    COPYRIGHT(true, false),
    PUBLICATION(true, false),
    PROJECT(true, false),
    COMMENT(true, true),
    CH_NAME("CH$NAME:", true, true),
    CH_COMPOUND_CLASS("CH$COMPOUND_CLASS:"),
    CH_FORMULA("CH$FORMULA:"),
    CH_EXACT_MASS("CH$EXACT_MASS:"),
    CH_SMILES("CH$SMILES:"),
    CH_IUPAC("CH$IUPAC:"),
    CH_IUPAC_KEY("CH$LINK: INCHIKEY"),
    CH_LINK("CH$LINK:", true, true),
    SP_SCIENTIFIC_NAME("SP$SCIENTIFIC_NAME:", true, false),
    SP_LINEAGE("SP$LINEAGE:", true, false),
    SP_LINK("SP$LINK:", true, true),
    SP_SAMPLE("SP$SAMPLE:", true, true),
    AC_INSTRUMENT("AC$INSTRUMENT:"),
    AC_INSTRUMENT_TYPE("AC$INSTRUMENT_TYPE:"),
    AC_MASS_SPECTROMETRY_MS_TYPE("AC$MASS_SPECTROMETRY: MS_TYPE"),
    AC_MASS_SPECTROMETRY_ION_MODE("AC$MASS_SPECTROMETRY: ION_MODE"),
    MS_FOCUSED_ION_PRECURSOR_MZ("MS$FOCUSED_ION: PRECURSOR_M/Z", true, false),
    MS_FOCUSED_ION_ION_TYPE("MS$FOCUSED_ION: ION_TYPE", true, false),
    MS_FOCUSED_ION_PRECURSOR_TYPE("MS$FOCUSED_ION: PRECURSOR_TYPE", true, false),
    AC_MASS_SPECTROMETRY_COLLISION_ENERGY("AC$MASS_SPECTROMETRY: COLLISION_ENERGY", true, false),
    AC_CHROMATOGRAPHY_RETENTION_TIME("AC$CHROMATOGRAPHY: RETENTION_TIME", true, false),
    //    ("AC$MASS_SPECTROMETRY:subtag"),
    //    ("AC$CHROMATOGRAPHY:subtag"),
    //    ("AC$GENERAL:subtag"),
    //    ("MS$FOCUSED_ION:PRECURSOR_M/Z"),
    //    ("MS$DATA_PROCESSING:subtag"),
    PK_SPLASH("PK$SPLASH:"),
    PK_ANNOTATION("PK$ANNOTATION:", true, false, true),
    PK_NUM_PEAK("PK$NUM_PEAK:"),
    PK_PEAK("PK$PEAK:", false, false, true);


    private final String key;
    private final boolean optional;
    private final boolean iterative;
    private final boolean multiline;
    public static final List<String> KEYS_BY_LENGTH = Arrays.stream(values()).map(MassbankFormat::k).sorted(Comparator.comparing(String::length).reversed()).collect(Collectors.toList());

    MassbankFormat() {
        this(false, false);
    }

    MassbankFormat(boolean optional, boolean iterative) {
        this(null, optional, iterative);
    }

    MassbankFormat(String key) {
        this(key, false, false);
    }

    MassbankFormat(String key, boolean optional, boolean iterative) {
        this(key, optional, iterative, false);
    }

    MassbankFormat(String key, boolean optional, boolean iterative, boolean multiline) {
        this.key = key == null ? name() + ':' : key;
        this.optional = optional;
        this.iterative = iterative;
        this.multiline = multiline;
    }

    public String k() {
        return getKey();
    }

    public boolean isMandatory() {
        return !isOptional();
    }

    public boolean isUnique() {
        return !isIterative();
    }

    public boolean isSingleLine() {
        return !isMultiline();
    }

    public static void withKeyValue(@Nullable String v, @NotNull BiConsumer<String, String> doWith) {
        if (v == null || v.isBlank())
            return;

        for (String key : KEYS_BY_LENGTH) {
            if (v.startsWith(key)) {
                doWith.accept(v.substring(0, key.length()).strip(), v.substring(key.length()).strip());
                return;
            }
        }
        log.debug("Non supported Key in " + v + ", Ignoring!");
    }

    public static Optional<RetentionTime> parseRetentionTime(Map<String, String> metaInfo) {
        String value = metaInfo.getOrDefault(AC_CHROMATOGRAPHY_RETENTION_TIME.k(), "");
        RetentionTime.ParsedParameters retentionTimeParams = RetentionTime.parseRetentionTimeParameters(value);
        if (retentionTimeParams.unit() == null) {
            String title = metaInfo.getOrDefault(RECORD_TITLE.k(), "");
            if (title.contains("RT:")) {
                RetentionTime.ParsedParameters titleRetentionParams = RetentionTime.parseRetentionTimeParameters(title.split("RT:")[1]);
                if (titleRetentionParams.unit() != null || retentionTimeParams.from() == null) {
                    retentionTimeParams = titleRetentionParams;
                }
            }
        }

        return Optional.ofNullable(RetentionTime.fromParameters(retentionTimeParams));
    }

    public static Optional<PrecursorIonType> parsePrecursorIonType(Map<String, String> metaInfo) {
        String value = metaInfo.get(MS_FOCUSED_ION_PRECURSOR_TYPE.k());
        if (value != null)
            return Optional.of(PrecursorIonType.fromString(value));

        value = metaInfo.get(MS_FOCUSED_ION_ION_TYPE.k());
        if (value != null)
            return Optional.of(PrecursorIonType.fromString(value));

        value = metaInfo.get(AC_MASS_SPECTROMETRY_ION_MODE.k());
        if (value != null) {
            return Optional.of(value.toLowerCase().charAt(0) == 'n' ? PrecursorIonType.unknownNegative() : PrecursorIonType.unknownPositive());
        }
        return Optional.empty();
    }

    public static Optional<Double> parsePrecursorMZ(Map<String, String> metaInfo) {
        String value = metaInfo.get(MS_FOCUSED_ION_PRECURSOR_MZ.k());
        try {
            if (value != null) {
                String[] arr = value.split("/");
                return Optional.of(Double.parseDouble(arr[arr.length - 1]));
            }
        } catch (NumberFormatException ignored) {}

        try {
            value = metaInfo.get(CH_EXACT_MASS.k());
            if (value != null)
                return Optional.of(Double.parseDouble(value));
        } catch (NumberFormatException ignored) {}

        return Optional.empty();
    }

    public static Optional<String> parseName(Map<String, String> metaInfo) {
        String value = metaInfo.get(RECORD_TITLE.k());
        if (value != null)
            return Optional.of(value);

        value = metaInfo.get(CH_NAME.k());
        if (value != null)
            return Optional.of(value);

        value = metaInfo.get(ACCESSION.k());
        if (value != null) {
            return Optional.of(value);
        }
        return Optional.empty();
    }
}
