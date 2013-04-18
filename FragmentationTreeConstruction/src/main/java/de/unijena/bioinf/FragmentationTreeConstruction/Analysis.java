package de.unijena.bioinf.FragmentationTreeConstruction;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.MassDecomposer.Chemistry.ChemicalAlphabet;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.MassDecomposer.Interval;

import java.util.Arrays;
import java.util.HashMap;

public class Analysis {

    public static void main(String[] args) {
        final PeriodicTable P = PeriodicTable.getInstance();
        final ChemicalAlphabet alphabet = new ChemicalAlphabet(P.getAllByName("C", "H", "N", "O", "P",
                "F", "I"));
        final HashMap<Element, Interval> boundaries = new HashMap<Element, Interval>();
        /*
        boundaries.put(P.getByName("Cl"), new Interval(0, 6));
        boundaries.put(P.getByName("Br"), new Interval(0, 2));
         */
        boundaries.put(P.getByName("I"), new Interval(0, 5));
        boundaries.put(P.getByName("F"), new Interval(0, 10));
        //boundaries.put(P.getByName("S"), new Interval(0, 4));
        boundaries.put(P.getByName("P"), new Interval(0, 6));
        final MassToFormulaDecomposer decomposer = new MassToFormulaDecomposer(1e-5, 20, 0.002, alphabet);
        final MolecularFormula[] formulasX = decomposer.decomposeToFormulas(596.2985, boundaries).toArray(new MolecularFormula[0]);
        // isotopenanalyse

    }

    public static MolecularFormula[] filter(MolecularFormula[] formulas) {
        int n=formulas.length;
        int i=0;
        while (i < n) {
            if (numberOfNonCarbonHydrogenNitrogenOxygen(formulas[i]) > 10 || numberOfStrangeElements(formulas[i]) > 1) {
                formulas[i] = formulas[n-1];
                --n;
            } else ++i;
        }
        return Arrays.copyOf(formulas, n);
    }

    private static final Element OXYGEN = PeriodicTable.getInstance().getByName("O");
    private static final Element NITROGEN = PeriodicTable.getInstance().getByName("N");
    private static final Element[] CHNOPS = PeriodicTable.getInstance().getAllByName("C", "H", "N", "O", "P", "S");
    private static final Element[] STRANGE = PeriodicTable.getInstance().getAllByName("Br", "Cl", "F", "I");
    private static int numberOfNonCarbonHydrogenNitrogenOxygen(MolecularFormula f) {
        return f.atomCount() - (f.numberOfCarbons() + f.numberOfHydrogens() + f.numberOf(NITROGEN) + f.numberOf(OXYGEN));
    }
    private static int numberOfStrangeElements(MolecularFormula f) {
        int i=0;
        for (Element strange : STRANGE) if (f.numberOf(strange) > 0) ++i;
        return i;
    }

}
