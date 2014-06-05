package de.unijena.bioinf.FragmentationTreeConstruction.model.graph;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.utils.Utils;

public class FTEdge implements Loss {

    protected FTVertex from;
    protected FTVertex to;
    protected Object[] annotations;
    protected MolecularFormula formula;
    protected double weight;

    FTEdge(FTVertex from, FTVertex to) {
        this.from = from;
        this.to = to;
        this.formula = from.formula.subtract(to.formula);
        this.annotations = Utils.EMPTY_ARRAY;
        this.weight = 0d;
    }

    public FTVertex getParent() {
        return from;
    }

    public FTVertex getChild() {
        return to;
    }

    public double getWeight() {
        return weight;
    }

    @Override
    public MolecularFormula getMolecularFormula() {
        return formula;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

}
