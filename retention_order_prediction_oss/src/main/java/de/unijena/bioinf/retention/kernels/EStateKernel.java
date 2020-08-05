/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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

package de.unijena.bioinf.retention.kernels;

import de.unijena.bioinf.retention.PredictableCompound;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.descriptors.molecular.KierHallSmartsDescriptor;
import org.openscience.cdk.qsar.result.IntegerArrayResult;

public class EStateKernel implements MoleculeKernel<int[]> {

    @Override
    public int[] prepare(PredictableCompound compound) {
        final KierHallSmartsDescriptor descr = new KierHallSmartsDescriptor();
        try {
            Aromaticity.cdkLegacy().apply(compound.getMolecule());
            final DescriptorValue calculate = descr.calculate(compound.getMolecule());
            final IntegerArrayResult value = (IntegerArrayResult) calculate.getValue();
            final int[] counts = new int[value.length()];
            for (int i=0; i < counts.length; ++i) {
                counts[i] = value.get(i);
            }
            return counts;
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double compute(PredictableCompound left, PredictableCompound right, int[] preparedLeft, int[] preparedRight) {
        int mn=0,mx=0;
        for (int i=0; i < preparedLeft.length; ++i) {
            mn += Math.min(preparedLeft[i],preparedRight[i]);
            mx += Math.max(preparedLeft[i],preparedRight[i]);
        }
        return ((double)mn)/mx;
    }
}
