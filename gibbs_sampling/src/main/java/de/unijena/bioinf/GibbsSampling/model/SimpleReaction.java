package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.GibbsSampling.model.Reaction;

public class SimpleReaction implements Reaction {
    private final MolecularFormula group;
    private final int stepSize;

    public SimpleReaction(MolecularFormula group) {
        this(group, 1);
    }

    public SimpleReaction(MolecularFormula group, int stepSize) {
        if(group == null) {
            throw new RuntimeException("group null");
        } else {
            this.group = group;
            this.stepSize = stepSize;
        }
    }

    public boolean hasReaction(MolecularFormula mf1, MolecularFormula mf2) {
        return mf1.add(this.group).equals(mf2);
    }

    public boolean hasReactionAnyDirection(MolecularFormula mf1, MolecularFormula mf2) {
        return mf1.add(this.group).equals(mf2)?true:mf2.add(this.group).equals(mf1);
    }

    public MolecularFormula netChange() {
        return this.group.clone();
    }

    public Reaction negate() {
        return new SimpleReaction(this.group.negate(), this.stepSize);
    }

    public int stepSize() {
        return this.stepSize;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("SimpleReaction{");
        sb.append(this.group);
        sb.append('}');
        return sb.toString();
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof SimpleReaction)) {
            return false;
        } else {
            SimpleReaction that = (SimpleReaction)o;
            return this.group.equals(that.group);
        }
    }

    public int hashCode() {
        return this.group.hashCode();
    }
}
