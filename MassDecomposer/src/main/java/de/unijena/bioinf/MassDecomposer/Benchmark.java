package de.unijena.bioinf.MassDecomposer;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.MassDecomposer.Chemistry.ChemicalAlphabet;

public class Benchmark {

    public static void main(String... args) {
        final MassDecomposer<Element> elementDecomposer = new MassDecomposer<Element>(1e-5, 20, 0.002d,
                new ChemicalAlphabet(PeriodicTable.getInstance().getAllByName("C", "H", "N", "O", "P", "S", "Cl")));
        final MassDecomposer<Element> fastDecomposer = new MassDecomposerFast<Element>(1e-5, 20, 0.002d,
                new ChemicalAlphabet(PeriodicTable.getInstance().getAllByName("C", "H", "N", "O", "P", "S", "Cl")));

        final double[] testSet = new double[]{
                MolecularFormula.parse("C66H75Cl2N9O24").getMass(),
                MolecularFormula.parse("C66H75Cl2N9O24").getMass(),
                MolecularFormula.parse("C63H91ClN13O14P").getMass(),
                MolecularFormula.parse("C63H98O29").getMass(),
                MolecularFormula.parse("C62H86N12O16").getMass(),
                /*,
                MolecularFormula.parse("C56H98N16O13").getMass(),
                MolecularFormula.parse("C62H111N11O12").getMass(),
                MolecularFormula.parse("C62H111N11O12").getMass(),
                MolecularFormula.parse("C55H75N17O13").getMass(),
                MolecularFormula.parse("C20H14I6N2O6").getMass(),
                MolecularFormula.parse("C20H14I6N2O6").getMass(),
                MolecularFormula.parse("C54H90N6O18").getMass(),
                MolecularFormula.parse("C54H90N6O18").getMass(),
                MolecularFormula.parse("C55H59N5O20").getMass(),
                MolecularFormula.parse("C55H59N5O20").getMass(),
                MolecularFormula.parse("C41H74N7O17P3S").getMass(),
                MolecularFormula.parse("C41H66N7O17P3S").getMass(),
                MolecularFormula.parse("C51H84O22").getMass(),
                MolecularFormula.parse("C51H84O22").getMass(),
                MolecularFormula.parse("C39H70N7O17P3S").getMass()
                */
        };
        double bestTime = Double.MAX_VALUE;
        System.out.println("#############\noriginal Decompose\n#############");
        for (int i=0; i < 10; ++i) {
            double now = System.nanoTime();
            for (double x : testSet) {
                elementDecomposer.decompose(x);
            }
            double after = System.nanoTime();
            if (after-now < bestTime) {
                bestTime = after-now;
                System.out.println((long)(bestTime*1e-6));
            }
        }
        bestTime = Double.MAX_VALUE;
        System.out.println("#############\nfast Decompose\n#############");
        for (int i=0; i < 10; ++i) {
            double now = System.nanoTime();
            for (double x : testSet) {
                fastDecomposer.decompose(x);
            }
            double after = System.nanoTime();
            if (after-now < bestTime) {
                bestTime = after-now;
                System.out.println((long)(bestTime*1e-6));
            }
        }

    }
}
