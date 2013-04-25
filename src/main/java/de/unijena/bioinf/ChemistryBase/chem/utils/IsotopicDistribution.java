package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Isotopes;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class IsotopicDistribution {

	private final HashMap<String, Isotopes> isotopeMap;
    private ArrayList<Isotopes> isotopes;
    private PeriodicTable table;

    public IsotopicDistribution(PeriodicTable table) {
        this.table = table;
        this.isotopeMap = new HashMap<String, Isotopes>();
        this.isotopes = new ArrayList<Isotopes>();
    }

    public Isotopes getIsotopesFor(String symbol) {
        return isotopeMap.get(symbol);
    }

    public Isotopes getIsotopesFor(Element element) {
        if (this.isotopes.size() > element.getId()) {
            Isotopes iso = this.isotopes.get(element.getId());
            if (iso != null) return iso;
        }
        final Isotopes isotopes = isotopeMap.get(element.getSymbol());
        if (isotopes != null) {
            while (this.isotopes.size() <= element.getId()) this.isotopes.add(null);
            this.isotopes.set(element.getId(), isotopes);
            return isotopes;
        }
        return null;
    }

    public void merge(IsotopicDistribution otherDist){
        for (Map.Entry<String, Isotopes> entry : otherDist.isotopeMap.entrySet()) {
            addIsotope(entry.getKey(), entry.getValue());
        }
    }

    public void addIsotope(String elementSymbol, Isotopes isotopes) {
        final Element element = table.getByName(elementSymbol);
        isotopeMap.put(elementSymbol, isotopes);
        if (element != null) {
            while (this.isotopes.size() <= element.getId()) this.isotopes.add(null);
            this.isotopes.set(element.getId(), isotopes);
        }
    }

    public void addIsotope(String elementSymbol, double[] masses, double[] abundances) {
        addIsotope(elementSymbol, new Isotopes(masses, abundances));
    }
	
}
