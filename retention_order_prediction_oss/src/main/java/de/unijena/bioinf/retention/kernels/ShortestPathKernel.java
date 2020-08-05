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

import de.unijena.bioinf.fingerid.fingerprints.ShortestPathFingerprinter;
import de.unijena.bioinf.retention.PredictableCompound;
import de.unijena.bioinf.retention.kernels.MoleculeKernel;

import java.util.Set;

public class ShortestPathKernel implements MoleculeKernel<Set<String>> {


    @Override
    public Set<String> prepare(PredictableCompound compound) {
        return new ShortestPathFingerprinter.ShortestPathWalker(compound.getMolecule()).paths();
    }

    @Override
    public double compute(PredictableCompound left, PredictableCompound right, Set<String> preparedLeft, Set<String> preparedRight) {
        int intersection = 0;
        for (String s : preparedLeft) {
            if (preparedRight.contains(s))
                ++intersection;
        }
        int union = preparedLeft.size()+preparedRight.size()-intersection;
        return ((double)intersection)/union;
    }

}
