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

public class SavitzkyGolayFilter implements Filter {

    de.unijena.bioinf.lcms.SavitzkyGolayFilter f;

    public SavitzkyGolayFilter() {
        this(8);
    }

    public SavitzkyGolayFilter(int preferredWindowSize) {
        if (preferredWindowSize>=32) f= de.unijena.bioinf.lcms.SavitzkyGolayFilter.Window32Polynomial2;
        else if (preferredWindowSize>=16)  f= de.unijena.bioinf.lcms.SavitzkyGolayFilter.Window16Polynomial2;
        else if (preferredWindowSize>=8) f= de.unijena.bioinf.lcms.SavitzkyGolayFilter.Window8Polynomial2;
        else if (preferredWindowSize>=4) f= de.unijena.bioinf.lcms.SavitzkyGolayFilter.Window4Polynomial2;
        else f = null;
    }

    @Override
    public double[] apply(double[] src) {
        if (f == null) {
            return src;
        } else {
            return f.applyExtended(src);
        }
    }

}
