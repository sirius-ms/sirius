package de.unijena.bioinf.MassDecomposer.Chemistry;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaFilter;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.MassDecomposer.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MassToFormulaDecomposer extends MassDecomposerFast<Element> {

    protected final ChemicalAlphabet alphabet;

    public MassToFormulaDecomposer(double precision, ChemicalAlphabet alphabet) {
        super(precision, new ChemicalAlphabetWrapper(alphabet));
        this.alphabet = alphabet;
    }


    public MassToFormulaDecomposer(double precision) {
        this(precision, new ChemicalAlphabet());
    }

    public List<MolecularFormula> decomposeToFormulas(double mass, Deviation deviation, Map<Element, Interval> boundaries, final FormulaFilter filter) {
        final Map<Element, Interval> boundaryMap;
        /*
        if (((validator instanceof ChemicalValidator) && ((ChemicalValidator)validator).getRdbeLowerbound() == 0) ||
                (validator instanceof ValenceValidator && ((ValenceValidator)validator).getMinValence() == 0 ))
            boundaryMap = new ValenceBoundary<Element>(alphabet).getMapFor(mass, boundaries); // TODO: implement parameterized valence boundary
        else
            boundaryMap = boundaries;
            */
        // Don't use valence boundary until it is fixed
        boundaryMap = boundaries;
        final boolean isAlsoDecVal = filter == null || filter instanceof DecompositionValidator;
        final List<int[]> decompositions = super.decompose(mass, deviation, boundaryMap, (isAlsoDecVal&&filter!=null) ? (DecompositionValidator<Element>)filter : null);
        final ArrayList<MolecularFormula> formulas = new ArrayList<MolecularFormula>(decompositions.size());
        for (int[] ary : decompositions) {
            final MolecularFormula formula = alphabet.decompositionToFormula(ary);
            if (!isAlsoDecVal && !filter.isValid(formula)) continue;
            formulas.add(formula);
        }
        return formulas;
    }

    @Override
    public ValencyAlphabet<Element> getAlphabet() {
        return new ChemicalAlphabetWrapper(alphabet);
    }
}
