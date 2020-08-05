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
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.qsar.descriptors.bond.BondPartialTChargeDescriptor;
import org.openscience.cdk.qsar.result.DoubleResult;

public class ElectroPathKernel {

    public static class Prepared {

        public Prepared(PredictableCompound c) {
            BondPartialTChargeDescriptor descr = new BondPartialTChargeDescriptor();
            for (IBond b  : c.getMolecule().bonds()) {
                final IAtom left = b.getAtom(0);
                final IAtom right = b.getAtom(1);
                String l = left.getSymbol(), r= right.getSymbol();
                String bond;
                switch (b.getOrder()) {
                    case SINGLE: bond="-";break;
                    case DOUBLE: bond="="; break;
                    case TRIPLE: bond="#"; break;
                    default: bond="?";
                }
                if (b.isAromatic()) bond = ":";
                double value = ((DoubleResult)descr.calculate(b,c.getMolecule()).getValue()).doubleValue();
                System.out.println(l+bond+r+"\t"+value);
            }
        }

    }

}
