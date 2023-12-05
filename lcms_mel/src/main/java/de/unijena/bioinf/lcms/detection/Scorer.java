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

package de.unijena.bioinf.lcms.detection;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

public interface Scorer {

    /**
     *
     * @param values list of values
     * @param mean mean of values
     * @return score for each value
     */
    DoubleList score(DoubleList values, double mean);

    /**
     * Studentize scoring
     */
    class StudentScorer implements Scorer {

        @Override
        public DoubleList score(DoubleList values, double mean) {
            double std = Math.sqrt(values.doubleStream().map(x -> (x - mean) * (x - mean)).sum() / values.size());
            return new DoubleArrayList(values.doubleStream().map(x -> -Math.log(Math.abs((x - mean) / std))).iterator());
        }
    }

}
