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
