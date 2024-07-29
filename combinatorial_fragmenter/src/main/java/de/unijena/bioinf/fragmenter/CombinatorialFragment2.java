package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.algorithm.BitsetOps;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.TableSelection;
import gnu.trove.list.array.TIntArrayList;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.util.BitSet;

public class CombinatorialFragment2 {

    protected final MolecularGraph parent;
    protected final long bitset;
    protected final BitSet disconnectedRings;
    protected MolecularFormula formula;

    public CombinatorialFragment2(MolecularGraph parent, long bitset, BitSet disconnectedRings) {
        this.parent = parent;
        this.bitset = bitset;
        this.formula = null;
        this.disconnectedRings = disconnectedRings;
    }

    public IAtom[] getAtoms() {
        IAtom[] atoms = new IAtom[BitsetOps.cardinality(bitset)];
        int k=0;
        for (int b = BitsetOps.nextSetBit(bitset,0); b>=0; b = BitsetOps.nextSetBit(bitset,b+1)) {
            atoms[k++] = parent.molecule.getAtom(b);
        }
        return atoms;
    }

    public IAtomContainer toMolecule() {
        final int cardinality = BitsetOps.cardinality(bitset);
        if (cardinality==parent.natoms) return parent.molecule;
        int[] indizes = new int[cardinality];
        int k=0;
        for (int b = BitsetOps.nextSetBit(bitset,0); b>=0; b = BitsetOps.nextSetBit(bitset,b+1)) {
            indizes[k++] = b;
        }
        try {
            return AtomContainerManipulator.extractSubstructure(parent.molecule, indizes);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean allRingsDisconnected(int bondIndex) {
        for (int ringId : parent.ringMemberships[bondIndex]) {
            if (!disconnectedRings.get(ringId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * if there is exactly one ring which is not disconnected yet, return its ID, otherwise return -1
     */
    int getSSSRIfCuttable(int bondIndex) {
        int found = -1;
        for (int ringId : parent.ringMemberships[bondIndex]) {
            if (!disconnectedRings.get(ringId)) {
                if (found>=0) return -1;
                else found = ringId;
            }
        }
        return found;
    }

    public MolecularFormula getFormula() {
        if(formula==null) determineFormula();
        return formula;
    }

    private void determineFormula() {
        final TableSelection sel = parent.getTableSelectionOfFormula();
        short[] buffer = sel.makeCompomer();
        int[] labels = parent.getAtomLabels();
        for (int node=BitsetOps.nextSetBit(bitset,0); node >= 0; node=BitsetOps.nextSetBit(bitset,node+1) ) {
            ++buffer[labels[node]];
        }
        this.formula = MolecularFormula.fromCompomer(sel, buffer);
    }

    public TIntArrayList bonds() {
        final int[][] adj = parent.getAdjacencyList();
        final int[][] bondj = parent.bondList;
        final TIntArrayList bonds = new TIntArrayList();
        for (int node=BitsetOps.nextSetBit(bitset,0); node >= 0; node=BitsetOps.nextSetBit(bitset,node+1) ) {
            int[] l = adj[node];
            for (int bond = 0; bond < l.length; ++bond) {
                if (l[bond] > node && BitsetOps.get(bitset, l[bond])) {
                    bonds.add(bondj[node][bond]);
                }
            }
        }
        return bonds;
    }

    public boolean stillContains(IBond b) {
        return BitsetOps.get(bitset,b.getAtom(0).getIndex()) && BitsetOps.get(bitset, b.getAtom(1).getIndex());
    }

    public String toSMILES() {
        try {
            return SmilesGenerator.generic().create(toMolecule());
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }
}
