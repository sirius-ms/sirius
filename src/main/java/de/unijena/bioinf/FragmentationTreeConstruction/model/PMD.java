package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

/**
 * @author Kai Dührkop
 */
public class PMD implements Comparable<PMD> {

    private final double weight;
    private final MolecularFormula formula;

    public PMD(MolecularFormula formula, double weight) {
        this.weight = weight;
        this.formula = formula;
    }

    public double getWeight() {
        return weight;
    }

    public MolecularFormula getFormula() {
        return formula;
    }

    @Override
    public int compareTo(PMD o) {
        return Double.compare(weight, o.weight);
    }
}
