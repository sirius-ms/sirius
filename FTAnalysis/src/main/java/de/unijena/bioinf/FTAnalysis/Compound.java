/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FTAnalysis;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

class Compound {

    private static PredictedLoss[] EMPTY_ARRAY = new PredictedLoss[0];

    static Iterable<PredictedLoss> foreachLoss(final List<Compound> compounds) {
        return new Iterable<PredictedLoss>() {
            @Override
            public Iterator<PredictedLoss> iterator() {
                return Iterables.concat(Iterables.transform(compounds, new Function<Compound, Iterable<PredictedLoss>>() {
                    @Override
                    public Iterable<PredictedLoss> apply(Compound arg) {
                        return Arrays.asList(arg.losses);
                    }
                })).iterator();
            }
        };
    }

    MolecularFormula formula;
    File file;
    PredictedLoss[] losses;

    public Compound(MolecularFormula formula, File file) {
        this.formula = formula;
        this.file = file;
        this.losses = EMPTY_ARRAY;
    }
}
