package de.unijena.bioinf.fragmenter;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;

public class CombinatorialEdge {
    protected IBond cut1, cut2;
    protected byte direction;
    protected CombinatorialNode source, target;
    /**
     * This is the score or profit of this edge.
     */
    protected float score;

    public CombinatorialEdge(CombinatorialNode source, CombinatorialNode target){
        this(source, target, null, null, false, false);
    }

    public CombinatorialEdge(CombinatorialNode source, CombinatorialNode target, IBond cut1, boolean leftToRight) {
        this(source,target,cut1,null, leftToRight, false);
    }

    public CombinatorialEdge(CombinatorialNode source, CombinatorialNode target, IBond cut1, IBond cut2, boolean direction1, boolean direction2) {
        this.source = source;
        this.target = target;
        this.cut1 = cut1;
        this.cut2 = cut2;
        this.direction = (byte)((direction1 ? 2 : 0) | (direction2  ? 4 : 0));
        this.score = 0f;
    }

    public IAtom[] getAtomsOfFragment() {
        if(cut1 != null) {
            IAtom[] atoms = (cut2 == null ? new IAtom[1] : new IAtom[2]);
            if (getDirectionOfFirstCut()) atoms[0] = cut1.getAtom(0);
            else atoms[0] = cut1.getAtom(1);
            if (cut2 == null) return atoms;
            if (getDirectionOfSecondCut()) atoms[1] = cut2.getAtom(0);
            else atoms[1] = cut2.getAtom(1);
            return atoms;
        }else{
            return null;
        }
    }

    public IAtom[] getAtomsOfLoss() {
        if(cut1 != null) {
            IAtom[] atoms = (cut2 == null ? new IAtom[1] : new IAtom[2]);
            if (getDirectionOfFirstCut()) atoms[0] = cut1.getAtom(1);
            else atoms[0] = cut1.getAtom(0);
            if (cut2 == null) return atoms;
            if (getDirectionOfSecondCut()) atoms[1] = cut2.getAtom(1);
            else atoms[1] = cut2.getAtom(0);
            return atoms;
        }else{
            return null;
        }
    }

    public boolean getDirectionOfFirstCut() {
        return (this.direction & 2)!=0;
    }

    public boolean getDirectionOfSecondCut() {
        return (this.direction & 4)!=0;
    }

    public IBond getCut1() {
        return cut1;
    }

    public IBond getCut2() {
        return cut2;
    }

    public CombinatorialNode getSource() {
        return source;
    }

    public CombinatorialNode getTarget() {
        return target;
    }

    public float getScore(){
        return this.score;
    }
}
