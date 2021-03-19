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

package de.unijena.bioinf.canopus;

import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;

import java.util.*;

class CompoundClass {
    protected final short index;
    protected final ClassyfireProperty ontology;
    protected final List<LabeledCompound> compounds;

    CompoundClass(short index, ClassyfireProperty ontology) {
        this.index = index;
        this.ontology = ontology;
        this.compounds = new ArrayList<>(500);
    }

    public List<LabeledCompound> drawExamples(int number, Random r) {
        if (compounds.size() > number*20) {
            final HashMap<String, LabeledCompound> compoundHashMap = new HashMap<>();
            while (compoundHashMap.size() < number) {
                LabeledCompound c = compounds.get(r.nextInt(compounds.size()));
                compoundHashMap.put(c.inchiKey, c);
            }
            return new ArrayList<>(compoundHashMap.values());
        } else {
            final ArrayList<LabeledCompound> examples = new ArrayList<>(compounds);
            Collections.shuffle(examples);
            return examples.subList(0, number);
        }
    }
}
