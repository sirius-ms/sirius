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

package de.unijena.bioinf.ChemistryBase.utils;

import org.apache.commons.lang3.time.StopWatch;

import java.io.IOException;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;

public class Utils {

    public static final Comparator<String> ALPHANUMERIC_COMPARATOR = new AlphanumComparator();


    public static void withTime(String text, Consumer<StopWatch> exec) {
        withTimeR(text, (w) -> {
            exec.accept(w);
            return null;
        });
    }

    public static <R> R withTimeR(String text, Function<StopWatch, R> exec) {
        StopWatch w = new StopWatch();
        w.start();
        R r = exec.apply(w);
        w.stop();
        System.out.println(text + w);
        return r;
    }

    public static <R> R withTimeRIo(String text, IOFunctions.IOFunction<StopWatch, R> exec) throws IOException {
        StopWatch w = new StopWatch();
        w.start();
        R r = exec.apply(w);
        w.stop();
        System.out.println(text + w);
        return r;
    }

    public static double parseDoubleWithUnknownDezSep(String input) {
        input = input.replace(',', '.');
        int decimalSeperator = input.lastIndexOf('.');

        if (decimalSeperator > -1)
            input = input.substring(0, decimalSeperator).replace(".", "") + input.substring(decimalSeperator);


        return Double.parseDouble(input);
    }
}
