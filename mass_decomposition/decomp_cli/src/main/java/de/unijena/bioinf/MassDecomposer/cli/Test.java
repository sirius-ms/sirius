package de.unijena.bioinf.MassDecomposer.cli;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;

import java.util.HashMap;
import java.util.List;

/**
 * Created by kaidu on 27.07.16.
 */
public class Test {

    public static void main(String[] args) {
        final PeriodicTable P = PeriodicTable.getInstance();
        String allowedElements = "CHNOPSF";
        int allowedPPM = 0;
        double allowedMz = 0.005;
        FormulaConstraints constraints = new FormulaConstraints(allowedElements);
        constraints.setUpperbound(P.getByName("C"), 100);
        constraints.setUpperbound(P.getByName("H"), 100);
        constraints.setUpperbound(P.getByName("N"), 50);
        constraints.setUpperbound(P.getByName("F"), 50);
        constraints.setUpperbound(P.getByName("O"), 50);
        constraints.setUpperbound(P.getByName("P"), 10);
        constraints.setUpperbound(P.getByName("S"), 10);


        MassToFormulaDecomposer decomposer = new MassToFormulaDecomposer(constraints.getChemicalAlphabet());

// you might want to remove RDBE >=0 condition which is enabled by default
        constraints.getFilters().clear(); // remove RDBE>=0
        for (double mass : new double[]{2*1154.739624}) {
            final long time = System.currentTimeMillis();
            List<MolecularFormula> formulas = decomposer.decomposeToFormulas(mass, new Deviation(allowedPPM, allowedMz), constraints);
            final long time2 = System.currentTimeMillis();
            System.out.println(time2-time);
            System.out.println(formulas.size());
        }
    }

}
