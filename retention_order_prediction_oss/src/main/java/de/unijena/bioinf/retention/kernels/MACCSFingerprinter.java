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
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import java.util.BitSet;

public class MACCSFingerprinter implements MoleculeKernel<BitSet> {
    @Override
    public BitSet prepare(PredictableCompound compound) {
        try {
            return new org.openscience.cdk.fingerprint.MACCSFingerprinter(SilentChemObjectBuilder.getInstance()).getBitFingerprint(compound.getMolecule()).asBitSet();
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double compute(PredictableCompound left, PredictableCompound right, BitSet preparedLeft, BitSet preparedRight) {
        final BitSet copy = (BitSet) preparedLeft.clone();
        copy.and(preparedRight);
        return ((double)copy.cardinality()) / ((double)preparedLeft.cardinality()+preparedRight.cardinality()-copy.cardinality());
    }
}
