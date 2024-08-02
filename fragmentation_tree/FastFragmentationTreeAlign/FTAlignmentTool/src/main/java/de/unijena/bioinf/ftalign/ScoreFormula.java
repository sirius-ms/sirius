
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ftalign;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kai Dührkop
 */
public class ScoreFormula {

    public final float matchFixed;
    public final float matchSizeDependend;

    public final float missmatchFixed;
    public final float missmatchSizeDependend;

    private final static String SUBPATTERN = "([+-]?\\s*\\d+(?:\\.\\d*)?)";

    private final static Pattern PATTERN = Pattern.compile(SUBPATTERN + "?\\s*(?:x\\s*" +
            SUBPATTERN + ")?");

    //TODO: Quick'n dirty -> rewrite
    public ScoreFormula(String value) {
        try {
        final Matcher matcher = PATTERN.matcher(value.toLowerCase());
        float[] values = new float[]{0f,0f,0f,0f};
        int index = 0;
        while (matcher.find()) {
            final String fixed = matcher.group(1);
            final String sizeDependend = matcher.group(2);
            if (fixed != null && !fixed.isEmpty()) {
                values[index*2] = Float.parseFloat(fixed);
            }
            if (sizeDependend != null && !sizeDependend.isEmpty()) {
                values[index*2+1] = Float.parseFloat(sizeDependend);
            }
            if (++index > 2) break;
        }
        matchFixed = values[0];
        matchSizeDependend = values[1];
        missmatchFixed = values[2];
        missmatchSizeDependend = values[3];
        } catch (RuntimeException exc) {
            exc.printStackTrace();
            throw (exc);
        }
    }


}
