package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.algorithm.BitsetOps;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.TableSelection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.util.BitSet;

public class CombinatorialFragment {

    protected final MolecularGraph parent;
    protected final BitSet bitset;
    protected final BitSet disconnectedRings;
    protected MolecularFormula formula;

    public CombinatorialFragment(MolecularGraph parent, BitSet bitset, BitSet disconnectedRings) {
        this.parent = parent;
        this.bitset = bitset;
        this.formula = null;
        this.disconnectedRings = disconnectedRings;
    }

    public IAtom[] getAtoms() {
        IAtom[] atoms = new IAtom[bitset.cardinality()];
        int k=0;
        for (int b = bitset.nextSetBit(0); b>=0; b = bitset.nextSetBit(b+1)) {
            atoms[k++] = parent.molecule.getAtom(b);
        }
        return atoms;
    }

    public IAtomContainer toMolecule() {
        final int cardinality = bitset.cardinality();
        if (cardinality==parent.natoms) return parent.molecule;
        int[] indizes = new int[cardinality];
        int k=0;
        for (int b = bitset.nextSetBit(0); b>=0; b = bitset.nextSetBit(b+1)) {
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

    public int numberOfHydrogens() {
        int count=0;
        for (int b = bitset.nextSetBit(0); b>=0; b = bitset.nextSetBit(b+1)) {
            count += parent.hydrogens[b];
        }
        return count;
    }

    public int hydrogenRearrangements(MolecularFormula matcher) {
        MolecularFormula f = getFormula();
        final MolecularFormula hydrogens = matcher.subtract(f);
        if (hydrogens.numberOfHydrogens()!=hydrogens.atomCount()) {
            throw new IllegalArgumentException("Molecular formulas do not match");
        }
        return numberOfHydrogens() - hydrogens.numberOfHydrogens();
    }

    public MolecularFormula getFormula() {
        if(formula==null) determineFormula();
        return formula;
    }

    private void determineFormula() {
        final TableSelection sel = parent.getTableSelectionOfFormula();
        short[] buffer = sel.makeCompomer();
        int[] labels = parent.getAtomLabels();
        for (int node=bitset.nextSetBit(0); node >= 0; node=bitset.nextSetBit(node+1) ) {
            ++buffer[labels[node]];
        }
        this.formula = MolecularFormula.fromCompomer(sel, buffer);
    }

    public TIntArrayList bonds() {
        final int[][] adj = parent.getAdjacencyList();
        final int[][] bondj = parent.bondList;
        final TIntArrayList bonds = new TIntArrayList();
        for (int node=bitset.nextSetBit(0); node >= 0; node=bitset.nextSetBit(node+1) ) {
            int[] l = adj[node];
            for (int bond = 0; bond < l.length; ++bond) {
                if (l[bond] > node && bitset.get(l[bond])) {
                    bonds.add(bondj[node][bond]);
                }
            }
        }
        return bonds;
    }

    public boolean stillContains(IBond b) {
        return bitset.get(b.getAtom(0).getIndex()) && bitset.get(b.getAtom(1).getIndex());
    }

    public String toSMILES() {
        try {
            return SmilesGenerator.generic().create(toMolecule());
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }
}
