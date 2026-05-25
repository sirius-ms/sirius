package de.unijena.bioinf.MassDecomposer.Chemistry;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * I hate this workaround, but its probably better to just hack it in than to refactor everything. PFAS might
 * sometimes have not a single hydrogen and, in this case, cannot be annotated via [M+H]+ but only via [M]+.
 */
public class AddIntrinsicPFASToDecomps {
    public static final Element F = PeriodicTable.getInstance().getByName("F");

    public static void addIntrinsicalPFAS(List<Decomposition> decompositions, Set<Ionization> ionModeSet, List<MassToFormulaDecomposer> decomposers, double mz, double absDev, List<FormulaConstraints> constraintList) {
        HashSet<Decomposition> newDecomps = null;
        int[] charges = ionModeSet.stream().mapToInt(Ionization::getCharge).distinct().toArray();
        for (int k=0; k < constraintList.size(); ++k) {
            FormulaConstraints fc = constraintList.get(k);
            if (fc.getUpperbound(F)>0) {
                fc = fc.clone();
                fc.setLowerbound(F, 1); // ENFORCE fluorine
                fc.setUpperbound(PeriodicTable.getInstance().getByName("H"), 0); // FORBID hydrogen
                for (int charge : charges) {
                    Charge ionization = new Charge(charge);
                    if (newDecomps==null) newDecomps = new HashSet<>();
                    for (MolecularFormula f : decomposers.get(k).decomposeToFormulas(mz, ionization, absDev, fc)) {
                        newDecomps.add(new Decomposition(f, ionization, 0d));
                    };
                }
            }
        }
        if (newDecomps!=null) {
            decompositions.addAll(newDecomps);
        }
    }
}
