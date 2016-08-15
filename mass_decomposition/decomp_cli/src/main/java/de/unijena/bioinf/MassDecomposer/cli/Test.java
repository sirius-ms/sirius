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
        String allowedElements = "CHNOPS";
        int allowedPPM = 0;
        double allowedMz = 0.003;

        final FormulaConstraints constraints = new FormulaConstraints("CHNOPS");
        MassToFormulaDecomposer decomposer = new MassToFormulaDecomposer(constraints.getChemicalAlphabet());

// you might want to remove RDBE >=0 condition which is enabled by default
        constraints.getFilters().clear(); // remove RDBE>=0
        for (double mass : new double[]{10000d}) {
            final long time = System.currentTimeMillis();
            List<MolecularFormula> formulas = decomposer.decomposeToFormulas(mass, new Deviation(allowedPPM, allowedMz), constraints);
            final long time2 = System.currentTimeMillis();
            System.out.println(time2-time);
            System.out.println(formulas.size());
        }
    }

}
