package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.chem.FormulaFilter;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.List;

public class FormulaFilterList implements FormulaFilter {

    private final List<FormulaFilter> filters;

    public static FormulaFilter create(List<FormulaFilter> filters) {
        if (filters.size()==1) return filters.get(0);
        else return new FormulaFilterList(filters);
    }

    public FormulaFilterList(List<FormulaFilter> filters) {
        this.filters = filters;
    }

    @Override
    public boolean isValid(MolecularFormula formula) {
        for (FormulaFilter filter : filters)
            if (!filter.isValid(formula)) return false;
        return true;
    }
}
