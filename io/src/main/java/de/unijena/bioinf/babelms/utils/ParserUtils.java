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

package de.unijena.bioinf.babelms.utils;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.exceptions.MultimereException;
import de.unijena.bioinf.ChemistryBase.exceptions.MultipleChargeException;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserUtils {

    /**
     * The level may be surrounded by other text, e.g. 'MS2 (CID)', 'ESI-MS2', 'MS2HCD', or separated: 'MS^2', 'MS-2'.
     * After a blank only a single digit is a level ('MS 2') - in 'GC-MS 70 eV' the number is something else.
     */
    private static final Pattern MS_LEVEL = Pattern.compile("MS[\\^_-]?(\\d+)|MS\\s(\\d)(?![\\d.,])");

    /**
     * @param spectrumType e.g. MS, MS1, MS2, ms2, MS/MS, MSMS, MS3 or just the level
     * @return the MS level, or -1 if the spectrum type is unknown
     */
    public static int parseMsLevel(String spectrumType) {
        final String type = spectrumType.strip().toUpperCase();
        try {
            if (type.equals("MS"))
                return 1;
            final Matcher m = MS_LEVEL.matcher(type); // an explicit level first, e.g. 'MS3 (MS/MS/MS)'
            if (m.find())
                return Integer.parseInt(m.group(1) != null ? m.group(1) : m.group(2));
            if (type.contains("MS/MS") || type.contains("MSMS") || type.contains("MS-MS"))
                return 2;
            if (type.chars().allMatch(Character::isDigit) && !type.isEmpty())
                return Integer.parseInt(type);
        } catch (NumberFormatException e) {
            // level too large for an int - unknown as well
        }
        return -1;
    }

    /**
     * The MS level of a spectrum - from its spectrum type, or without a usable one: MS2 if the record has a precursor
     * field and a precursor m/z can be determined at all (also from the exact mass), else MS1. SIRIUS only needs to
     * know whether it is a fragmentation spectrum.
     *
     * @param precursorField           the precursor m/z as given, null if there is none
     * @param precursorMzDeterminable  whether a precursor m/z can be determined from the record
     */
    public static int inferMsLevel(@Nullable String spectrumType, @Nullable String precursorField, BooleanSupplier precursorMzDeterminable,
                                   Logger log, Supplier<String> recordName) {
        final boolean typeGiven = spectrumType != null && !spectrumType.isBlank();
        final int msLevel = typeGiven ? parseMsLevel(spectrumType) : -1;
        if (msLevel >= 1)
            return msLevel;
        final boolean hasPrecursor = precursorField != null && precursorMzDeterminable.getAsBoolean();
        if (precursorField != null && !hasPrecursor)
            log.warn("Precursor m/z '{}' of record '{}' cannot be used, treated as MS1.", precursorField, recordName.get());
        final int inferred = hasPrecursor ? 2 : 1;
        if (typeGiven)
            log.warn("Unknown spectrum type '{}' in record '{}', treated as MS{}.", spectrumType, recordName.get(), inferred);
        return inferred;
    }

    /**
     * A value of the record that SIRIUS needs cannot be used - a data problem, not a parser failure.
     */
    public static class InvalidValueException extends IllegalArgumentException {
        public InvalidValueException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * @return true if the ion type was recognised but is not supported: multiply charged or a multimere
     */
    public static boolean isUnsupportedIonType(Throwable e) {
        return causeChainContains(e, MultipleChargeException.class, MultimereException.class);
    }

    /**
     * @return true for data problems of the record - an unsupported ion type or an unusable value
     */
    public static boolean isDataProblem(Throwable e) {
        return isUnsupportedIonType(e) || causeChainContains(e, InvalidValueException.class);
    }

    private static boolean causeChainContains(Throwable e, Class<?>... types) {
        return ExceptionUtils.getThrowableList(e).stream().anyMatch(c -> Arrays.stream(types).anyMatch(type -> type.isInstance(c)));
    }

    /**
     * Reports a record that is not imported: a data problem in one warning line, an unexpected exception (a defect of
     * the parser) with its stack trace.
     */
    public static void logIgnoredRecord(Logger log, @Nullable String name, Throwable e, boolean dataProblem) {
        final String record = "Record '" + Optional.ofNullable(name).orElse("N/A") + "' ignored because of: " + e.getMessage();
        if (dataProblem)
            log.warn(record);
        else
            log.error(record, e);
    }

    /**
     * @return the mass (also with a decimal comma), empty if it is not a finite, positive number - a mass of 0 means
     * none is known
     */
    public static Optional<Double> parsePositiveMass(@Nullable String value) {
        if (value == null || value.isBlank())
            return Optional.empty();
        final double mass = parseDecimal(value.strip());
        return mass > 0 && Double.isFinite(mass) ? Optional.of(mass) : Optional.empty();
    }

    /**
     * A number with a decimal point or a decimal comma. More than one separator is no single number - e.g. a list
     * ('165.1121,166.1150') or thousands separators ('1,234,567') - and would be misread, so it is rejected.
     *
     * @return the number, NaN if the value cannot be parsed (callers reject it as not finite)
     */
    public static double parseDecimal(String value) {
        int separators = 0;
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c == '.' || c == ',')
                separators++;
        }
        if (separators > 1)
            return Double.NaN;
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    public static void checkMolecularFormula(MutableMs2Experiment exp){
        if (exp.getMolecularFormula() == null) {
            try {
                if (exp.hasAnnotation(InChI.class)) {
                    exp.setMolecularFormula(exp.getAnnotationOrThrow(InChI.class).extractFormulaOrThrow());
                } else if (exp.hasAnnotation(Smiles.class)) {
                    exp.setMolecularFormula(SmilesUCdk.formulaFromSmiles(
                            exp.getAnnotationOrThrow(Smiles.class).smiles));
                }
            } catch (Exception e) {
                LoggerFactory.getLogger(ParserUtils.class).error("Error when extracting molecular formula from smiles or inchi.", e);
            }
        }

        if (exp.getMolecularFormula() != null)
            exp.setAnnotation(Whiteset.class, Whiteset.ofNeutralizedFormulas(List.of(exp.getMolecularFormula()), ParserUtils.class));
    }
}
