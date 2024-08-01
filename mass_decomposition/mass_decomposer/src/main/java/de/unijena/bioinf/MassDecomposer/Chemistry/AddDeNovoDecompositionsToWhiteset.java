package de.unijena.bioinf.MassDecomposer.Chemistry;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.IonMode;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;

import java.util.HashSet;
import java.util.Set;

/**
 * this is a common routine for IsotopePatternAnalysis and FragmentationPatternAnalysis to de novo generate molecular formulas and add them to a whiteset.
 */
public class AddDeNovoDecompositionsToWhiteset {

    /**
     * sets "requiresDeNovo" flag of new whiteset to false! Check for any interference. E.g. BottomUpSearch checks this flag.
     * @param ws
     * @param monoMz
     * @param massDev
     * @param possibleAdducts
     * @param constraints
     * @param decomposer
     * @return
     */
    public static Whiteset createNewWhitesetWithDenovoAdded(Whiteset ws, double monoMz, Deviation massDev, PossibleAdducts possibleAdducts, FormulaConstraints constraints, DecomposerCache decomposer) {
        if (ws == null) ws = Whiteset.empty();
        Set<MolecularFormula> formulas = new HashSet<>();
        for (IonMode ionMode : possibleAdducts.getIonModes()) {
            for (PrecursorIonType ionType : possibleAdducts.getAdducts(ionMode)) {
                //the decomposition set for individual adducts may have big overlap. But I believe, there is no better way.
                formulas.addAll(decomposer.getDecomposer(constraints.getChemicalAlphabet(), ionType).decomposeToMeasuredFormulas(monoMz, ionType, massDev.absoluteFor(monoMz), constraints));
            }
        }
        return ws.setRequiresDeNovo(false).addMeasured(formulas, AddDeNovoDecompositionsToWhiteset.class); //no need to perform denovo again. Make sure this does not interfere with other SIRIUS plugins such as BottomUpSearch
    }
}
