package de.unijena.bioinf.fragmenter;

import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.BitSet;

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

    /**
     * Converts this {@link CombinatorialEdge} into an integer value by concatenating
     * the BitSet of its source and target fragments and
     * then converting the resulting BitSet into its decimal value.<br>
     * The given parameter {@code bitSetLength} denotes the maximal length of each BitSet found in the
     * corresponding {@link CombinatorialGraph}.
     * Although each source fragment should be a real fragment and that the first BitSet
     * has length {@link MolecularGraph#natoms}. But in the future there can be some changes,
     * so that the source fragment does not have to be a real fragment.
     * Thus, the length of the BitSet can be greater than the number of atoms in the molecule.
     *
     * @return an integer value representing this CombinatorialEdge
     */
    public int toIntegerValue(int bitSetLength){
        BitSet sourceBitSet = this.source.fragment.bitset;
        BitSet targetBitSet = this.target.fragment.bitset;

        int resultValue = 0;
        for(int i = sourceBitSet.nextSetBit(0); i >= 0; i = sourceBitSet.nextSetBit(i+1)){
            // 'i' is index of a bit that is set to true (or 1)
            resultValue = resultValue + (int) Math.pow(2,i);
        }

        for(int i = targetBitSet.nextSetBit(0); i>= 0; i = targetBitSet.nextSetBit(i+1)){
            // 'i' is index of a bit that is set to true in 'targetBitSet'.
            // because we are "merging" the source and target bitsets, the first index of targetBitSet is equal to 'bitSetLength'
            resultValue = resultValue + (int) Math.pow(2,i+bitSetLength);
        }

        return resultValue;
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
