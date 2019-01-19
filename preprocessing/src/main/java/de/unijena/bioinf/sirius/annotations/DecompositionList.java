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
package de.unijena.bioinf.sirius.annotations;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.ms.annotations.DataAnnotation;

import java.util.*;

public class DecompositionList implements DataAnnotation {

    private final List<Decomposition> decompositions;


    public static DecompositionList fromFormulas(Iterable<MolecularFormula> formulas, Ionization ion) {
        final ArrayList<Decomposition> decompositions = new ArrayList<>(formulas instanceof Collection ? ((Collection) formulas).size() : 10);
        for (MolecularFormula f : formulas) decompositions.add(new Decomposition(f,ion,0d));
        return new DecompositionList(decompositions);
    }

    public void replace(Decomposition... decs) {
        this.decompositions.clear();
        this.decompositions.addAll(Arrays.asList(decs));
    }

    public Decomposition find(MolecularFormula formula) {
        for (Decomposition d : decompositions)
            if (d.getCandidate().equals(formula)) return d;
        return null;
    }


    public DecompositionList(List<Decomposition> decompositions) {
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

    public void disjoin(DecompositionList other, double mzOwn, double mzOther) {
        final HashMap<Ionization, HashSet<MolecularFormula>> ownMap = new HashMap<>(), otherMap = new HashMap<>();

        for (Decomposition d : decompositions) {
            Ionization i = d.getIon();
            if (!ownMap.containsKey(i)) {
                ownMap.put(i, new HashSet<MolecularFormula>());
                otherMap.put(i, new HashSet<MolecularFormula>());
            }
            ownMap.get(i).add(d.getCandidate());
        }
        for (Decomposition d : other.decompositions) {
            Ionization i = d.getIon();
            if (otherMap.containsKey(i))
                otherMap.get(i).add(d.getCandidate());
        }
        final HashSet<MolecularFormula> deleteLeft = new HashSet<>(), deleteRight = new HashSet<>();
        for (Ionization ion : ownMap.keySet()) {
            final double l = ion.subtractFromMass(mzOwn), r = ion.subtractFromMass(mzOther);
            final HashSet<MolecularFormula> left = ownMap.get(ion), right = otherMap.get(ion);
            for (MolecularFormula f : left) {
                if (right.contains(f)) {
                    if (Math.abs(l-f.getMass()) < Math.abs(r-f.getMass())) {
                        deleteRight.add(f);
                    } else deleteLeft.add(f);
                }
            }
            if (deleteLeft.size()>0) {
                Iterator<Decomposition> i = decompositions.iterator();
                while (i.hasNext()) {
                    final Decomposition d = i.next();
                    if (d.getIon().equals(ion) && deleteLeft.contains(d.getCandidate()))
                        i.remove();
                }
            }
            if (deleteRight.size()>0) {
                Iterator<Decomposition> i = other.decompositions.iterator();
                while (i.hasNext()) {
                    final Decomposition d = i.next();
                    if (d.getIon().equals(ion) && deleteRight.contains(d.getCandidate()))
                        i.remove();
                }
            }
        }

    }

    public HashMap<Ionization, List<MolecularFormula>> getFormulasPerIonMode() {
        final HashMap<Ionization, List<MolecularFormula>> map = new HashMap<>();
        for (Decomposition d : decompositions) {
            if (!map.containsKey(d.getIon()))
                map.put(d.getIon(), new ArrayList<MolecularFormula>());
            map.get(d.getIon()).add(d.getCandidate());
        }
        return map;
    }

    @Override
    public String toString() {
        return decompositions.toString();
    }

    public List<Decomposition> getDecompositions() {
        return decompositions;
    }
}
