package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

public class Loss {

    private final static Object[] EMPTY_ARRAY = new Object[0];
    protected final Fragment source;
    protected final Fragment target;
    protected MolecularFormula formula;
    protected double weight;
    protected int sourceEdgeOffset, targetEdgeOffset;
    protected Object[] annotations;

    public Loss(Fragment from, Fragment to, MolecularFormula loss, double weight) {
        this.source = from;
        this.target = to;
        this.formula = loss;
        this.weight = weight;
        this.annotations = EMPTY_ARRAY;
        this.sourceEdgeOffset = 0;
        this.targetEdgeOffset = 0;
    }

    public Loss(Fragment from, Fragment to) {
        this(from, to, from.formula.subtract(to.formula), 0d);
    }

    public boolean isDeleted() {
        return sourceEdgeOffset >= 0;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public Fragment getSource() {
        return source;
    }

    public Fragment getTarget() {
        return target;
    }

    public MolecularFormula getFormula() {
        return formula;
    }

    public void setFormula(MolecularFormula formula) {
        this.formula = formula;
    }

    final Object getAnnotation(int id) {
        if (id >= annotations.length) return null;
        return annotations[id];
    }

    final void setAnnotation(int id, int capa, Object o) {
        if (id >= annotations.length) annotations = new Object[Math.max(capa, id + 1)];
        annotations[id] = o;
    }

}
