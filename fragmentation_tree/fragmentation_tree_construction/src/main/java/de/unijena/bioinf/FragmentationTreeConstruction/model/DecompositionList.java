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
package de.unijena.bioinf.FragmentationTreeConstruction.model;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.*;

public class DecompositionList {

    private final List<Scored<MolecularFormula>> decompositions;

    public static DecompositionList fromFormulas(Iterable<MolecularFormula> formulas) {
        final ArrayList<Scored<MolecularFormula>> decompositions = new ArrayList<Scored<MolecularFormula>>(formulas instanceof Collection ? ((Collection) formulas).size() : 10);
        for (MolecularFormula f : formulas) decompositions.add(new Scored<MolecularFormula>(f,0d));
        return new DecompositionList(decompositions);

    }

    public DecompositionList(List<Scored<MolecularFormula>> decompositions) {
        this.decompositions = decompositions;
    }

    public Collection<MolecularFormula> getFormulas() {
        return new AbstractCollection<MolecularFormula>() {
            @Override
            public Iterator<MolecularFormula> iterator() {
                return Iterators.transform(decompositions.iterator(), new Function<Scored<MolecularFormula>, MolecularFormula>() {
                    @Override
                    public MolecularFormula apply(Scored<MolecularFormula> input) {
                        return input.getCandidate();
                    }
                });
            }

            @Override
            public int size() {
                return decompositions.size();
            }
        };
    }

    public List<Scored<MolecularFormula>> getDecompositions() {
        return decompositions;
    }
}
