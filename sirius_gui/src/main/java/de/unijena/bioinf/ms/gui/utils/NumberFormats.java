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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Number formats for the GUI.
 * <p>
 * SIRIUS is not localized - everything it displays and writes is English - but {@code new DecimalFormat(pattern)}
 * picks up the platform default locale, so on a German system a mass would render as {@code 271,060} while the
 * same value written to a summary file renders as {@code 271.060}. Always build GUI formats through this class
 * rather than through the {@link DecimalFormat} constructor directly.
 */
public final class NumberFormats {

    private NumberFormats() {
    }

    private static final DecimalFormatSymbols US_SYMBOLS = DecimalFormatSymbols.getInstance(Locale.US);

    /**
     * @return a {@link DecimalFormat} for the given pattern that renders the same on every platform, independent
     * of the default locale.
     */
    public static DecimalFormat decimalFormat(String pattern) {
        return new DecimalFormat(pattern, US_SYMBOLS);
    }

    /**
     * Renders a probability in {@code [0,1]} as whole percent <b>digits only</b>, e.g. {@code 0.4234 -> "42"}.
     * The percent sign is left to the caller, so it can be placed and spaced as the surrounding text needs.
     */
    public static DecimalFormat percentage() {
        DecimalFormat format = decimalFormat("#0");
        format.setMultiplier(100);
        return format;
    }
}
