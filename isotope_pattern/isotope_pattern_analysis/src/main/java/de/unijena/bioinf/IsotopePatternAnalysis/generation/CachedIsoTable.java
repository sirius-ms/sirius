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
package de.unijena.bioinf.IsotopePatternAnalysis.generation;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;

import java.util.Arrays;
import java.util.HashMap;

class CachedIsoTable {

    private final HashMap<Element, IsotopologueTable[]> cache;
    private final IsotopicDistribution distribution;

    CachedIsoTable(IsotopicDistribution distribution) {
        this.cache = new HashMap<Element, IsotopologueTable[]>();
        this.distribution = distribution;
    }

    public Isotopologues getIsotopologuesFor(Element element, int numberOfAtoms) {
        IsotopologueTable[] tables = cache.get(element);
        if (tables == null) {
            tables = new IsotopologueTable[numberOfAtoms * 2];
            cache.put(element, tables);
        } else if (tables.length <= numberOfAtoms) {
            tables = Arrays.copyOf(tables, numberOfAtoms * 2);
        }
        if (tables[numberOfAtoms] == null) {
            tables[numberOfAtoms] = new IsotopologueTable(element, numberOfAtoms, distribution);
        }
        return tables[numberOfAtoms];
    }
}
