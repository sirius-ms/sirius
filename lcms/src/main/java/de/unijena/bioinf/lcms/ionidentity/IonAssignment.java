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

package de.unijena.bioinf.lcms.ionidentity;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

class IonAssignment {

    final PrecursorIonType[] ionTypes;
    final double[] probabilities;

    public IonAssignment(PrecursorIonType[] ionTypes, double[] probabilities) {
        this.ionTypes = ionTypes;
        this.probabilities = probabilities;
    }

    public double probability(PrecursorIonType ionType) {
        for (int k=0; k < ionTypes.length; ++k) {
            if (ionType.equals(ionTypes[k])) return probabilities[k];
        }
        return 0d;
    }

    public static IonAssignment uniform(Set<PrecursorIonType> possibleIonTypes) {
        final PrecursorIonType[] types = possibleIonTypes.toArray(PrecursorIonType[]::new);
        final double[] probs = new double[types.length];
        Arrays.fill(probs, 1.0f/probs.length);
        return new IonAssignment(types,probs);
    }

    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("{");
        for (int k=0; k < probabilities.length; ++k) {
            buf.append(String.format(Locale.US, "%s (%.1f %%)",ionTypes[k].toString(), probabilities[k]*100 ));
            if (k < buf.length()-1) buf.append(",\t");
        }
        buf.append("}");
        return buf.toString();
    }
}
