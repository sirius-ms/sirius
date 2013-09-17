package de.unijena.bioinf.FragmentationTree.analyze;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelectionOption {
    int num;
    List<MolecularFormula> formulas;
    public SelectionOption(String s) {
        if (s.isEmpty()) return;
        if (s.matches("\\d+")) {
            num = Integer.parseInt(s);
        } else if (s.contains(",")) {
            final String[] xs = s.split(",");
            formulas = new ArrayList<MolecularFormula>();
            for (String x : xs) formulas.add(MolecularFormula.parse(x));
        } else {
            formulas = new ArrayList<MolecularFormula>(Arrays.asList(MolecularFormula.parse(s)));
        }
    }

    public boolean isRestricted() {
        return num == 0 && formulas == null;
    }
    public List<MolecularFormula> specificFormula() {
        return formulas;
    }
    public int maxNumberOfTrees() {
        return num;
    }

}
