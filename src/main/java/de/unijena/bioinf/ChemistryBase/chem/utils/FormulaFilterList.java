package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.chem.FormulaFilter;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Bundles a list of filters into a single filter object. A formula pass this filter iff it passes all of its filters
 */
public class FormulaFilterList implements FormulaFilter, Parameterized {

    private final List<FormulaFilter> filters;

    public static FormulaFilter create(List<FormulaFilter> filters) {
        if (filters.size()==1) return filters.get(0);
        else return new FormulaFilterList(filters);
    }

    public FormulaFilterList(List<FormulaFilter> filters) {
        this.filters = new ArrayList<FormulaFilter>(filters);
    }

    public List<FormulaFilter> getFilters() {
        return filters;
    }

    @Override
    public boolean isValid(MolecularFormula formula) {
        for (FormulaFilter filter : filters)
            if (!filter.isValid(formula)) return false;
        return true;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        filters.clear();
        final Iterator<G> iter = document.iteratorOfList(document.getListFromDictionary(dictionary, "filters"));
        while (iter.hasNext()) {
            filters.add((FormulaFilter)helper.unwrap(document, iter.next()));
        }
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final L list = document.newList();
        for (FormulaFilter f : filters) {
            document.addToList(list, helper.wrap(document, f));
        }
        document.addListToDictionary(dictionary, "filters", list);
    }

}
