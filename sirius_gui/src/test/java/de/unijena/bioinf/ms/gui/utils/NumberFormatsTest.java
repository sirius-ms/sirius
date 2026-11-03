/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NumberFormatsTest {

    private final Locale original = Locale.getDefault();

    @AfterEach
    public void restoreLocale() {
        Locale.setDefault(original);
    }

    /**
     * The regression this class exists for: under a comma-decimal locale the plain constructor renders a mass
     * as "271,060" while the summary files render "271.060".
     */
    @Test
    public void decimalSeparatorIsADotUnderACommaDecimalLocale() {
        Locale.setDefault(Locale.GERMANY);

        assertEquals("271,060", new DecimalFormat("#0.000").format(271.0601), "precondition: the plain constructor is locale dependent");
        assertEquals("271.060", NumberFormats.decimalFormat("#0.000").format(271.0601));
    }

    @Test
    public void formatsAreIdenticalAcrossLocales() {
        Locale.setDefault(Locale.US);
        String us = NumberFormats.decimalFormat("#0.00000").format(0.5);

        Locale.setDefault(Locale.GERMANY);
        String de = NumberFormats.decimalFormat("#0.00000").format(0.5);

        Locale.setDefault(Locale.forLanguageTag("ar-EG")); //eastern arabic digits by default
        String eg = NumberFormats.decimalFormat("#0.00000").format(0.5);

        assertEquals("0.50000", us);
        assertEquals(us, de);
        assertEquals(us, eg);
    }

    /**
     * Patterns without a decimal separator are locale safe by accident today; they must stay stable anyway,
     * since the grouping separator would appear as soon as a pattern gains a comma.
     */
    @Test
    public void integerPatternIsUnaffected() {
        Locale.setDefault(Locale.GERMANY);
        assertEquals("1234", NumberFormats.decimalFormat("##0").format(1234.5));
    }

    /**
     * The fingerprint probability tooltip used a "%" pattern, which renders a *leading* percent sign, while the
     * call site appended one too - "42 %" came out as "%42 %".
     */
    @Test
    public void percentageRendersDigitsOnly() {
        Locale.setDefault(Locale.GERMANY);

        assertEquals("42", NumberFormats.percentage().format(0.4234));
        assertEquals("0", NumberFormats.percentage().format(0.0));
        assertEquals("100", NumberFormats.percentage().format(1.0));
        assertEquals("87", NumberFormats.percentage().format(0.87));
    }

    @Test
    public void percentageRoundsLikeTheOldFormatDid() {
        assertEquals("0", NumberFormats.percentage().format(0.005));
        assertEquals("100", NumberFormats.percentage().format(0.995));
    }

    @Test
    public void groupingSeparatorIsADot() {
        Locale.setDefault(Locale.GERMANY);
        assertEquals("1,234.5", NumberFormats.decimalFormat("#,##0.#").format(1234.5));
    }
}
