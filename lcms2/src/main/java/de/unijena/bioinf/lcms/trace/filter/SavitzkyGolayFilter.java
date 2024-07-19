/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.lcms.trace.filter;

import de.unijena.bioinf.lcms.Extrema;

import java.util.Objects;

public class SavitzkyGolayFilter implements Filter {

    public enum SGF {
        W1P1, W2P2, W3P2, W4P2, W8P2, W16P2, W32P2, AUTO
    }

    private final de.unijena.bioinf.lcms.SavitzkyGolayFilter filter;

    public SavitzkyGolayFilter(SGF type) {
        filter = switch (type) {
            case W1P1 -> de.unijena.bioinf.lcms.SavitzkyGolayFilter.Window1Polynomial1;
            case W2P2 -> de.unijena.bioinf.lcms.SavitzkyGolayFilter.Window2Polynomial2;
            case W3P2 -> de.unijena.bioinf.lcms.SavitzkyGolayFilter.Window3Polynomial2;
            case W4P2 -> de.unijena.bioinf.lcms.SavitzkyGolayFilter.Window4Polynomial2;
            case W8P2 -> de.unijena.bioinf.lcms.SavitzkyGolayFilter.Window8Polynomial2;
            case W16P2 -> de.unijena.bioinf.lcms.SavitzkyGolayFilter.Window16Polynomial2;
            case W32P2 -> de.unijena.bioinf.lcms.SavitzkyGolayFilter.Window32Polynomial2;
            case AUTO -> null;
        };
    }

    @Override
    public double[] apply(double[] src) {
        de.unijena.bioinf.lcms.SavitzkyGolayFilter f = filter != null ? filter : Extrema.getProposedFilter3(src);
        if (f == null) {
            return src;
        } else {
            return f.applyExtended(src);
        }
    }

}
