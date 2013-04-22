package de.unijena.bioinf.ChemistryBase.chem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * FormulaConstraints contains all constraints which reduce the size of all possible decompositions of a mass.
 * It contains of:
 * - allowed elements
 * - boundaries for elements
 * - constraints for formulas
 */
public class FormulaConstraints {

    private final ChemicalAlphabet chemicalAlphabet;
    private final int[] upperbounds;
    private final List<FormulaFilter> filters;

    public FormulaConstraints(FormulaConstraints c) {
        this(c.getChemicalAlphabet());
        System.arraycopy(c.upperbounds, 0, upperbounds, 0, c.upperbounds.length);
        filters.addAll(c.getFilters());
    }

    public FormulaConstraints(ChemicalAlphabet alphabet) {
        this.chemicalAlphabet = alphabet;
        this.upperbounds = new int[alphabet.size()];
        this.filters = new ArrayList<FormulaFilter>();
    }

    public ChemicalAlphabet getChemicalAlphabet() {
        return chemicalAlphabet;
    }

    public int[] getUpperbounds() {
        return upperbounds;
    }

    public void addFilter(FormulaFilter filter) {
        filters.add(filter);
    }

    public List<FormulaFilter> getFilters() {
        return filters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FormulaConstraints that = (FormulaConstraints) o;

        if (chemicalAlphabet != null ? !chemicalAlphabet.equals(that.chemicalAlphabet) : that.chemicalAlphabet != null)
            return false;
        if (filters != null ? !filters.equals(that.filters) : that.filters != null) return false;
        if (!Arrays.equals(upperbounds, that.upperbounds)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = chemicalAlphabet != null ? chemicalAlphabet.hashCode() : 0;
        result = 31 * result + (upperbounds != null ? Arrays.hashCode(upperbounds) : 0);
        result = 31 * result + (filters != null ? filters.hashCode() : 0);
        return result;
    }

    @Override
    public FormulaConstraints clone() {
        return new FormulaConstraints(this);
    }
}
