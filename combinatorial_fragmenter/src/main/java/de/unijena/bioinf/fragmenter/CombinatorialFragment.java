package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.TableSelection;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.silent.AtomContainer;
import org.openscience.cdk.smarts.SmartsFragmentExtractor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class CombinatorialFragment {

    protected final MolecularGraph parent;
    protected final float peakIntensity;
    protected final BitSet bitset;
    protected final BitSet disconnectedRings;
    protected MolecularFormula formula; // --> in general, with hydrogens!!
    protected final boolean innerNode;

    public CombinatorialFragment(MolecularGraph parent, BitSet bitset, BitSet disconnectedRings){
        this(parent, bitset, null, disconnectedRings);
    }

    public CombinatorialFragment(MolecularGraph parent, BitSet bitset, MolecularFormula formula, BitSet disconnectedRings, boolean isInnerNode, float peakIntensity) {
        this.parent = parent;
        this.bitset = bitset;
        this.formula = formula;
        this.disconnectedRings = disconnectedRings;
        this.innerNode = isInnerNode;
        this.peakIntensity = peakIntensity;
    }

    public CombinatorialFragment(MolecularGraph parent, BitSet bitset, MolecularFormula formula, BitSet disconnectedRings) {
        this.parent = parent;
        this.bitset = bitset;
        this.formula = formula;
        this.disconnectedRings = disconnectedRings;
        this.innerNode = true;
        this.peakIntensity = 0f;
    }

    public IAtom[] getAtoms() {
        if(!this.innerNode) return new IAtom[0];
        IAtom[] atoms = new IAtom[bitset.cardinality()];
        int k=0;
        for (int b = bitset.nextSetBit(0); b>=0; b = bitset.nextSetBit(b+1)) {
            atoms[k++] = parent.molecule.getAtom(b);
        }
        return atoms;
    }

    public IAtomContainer toMolecule() {
        if(!this.innerNode) return new AtomContainer();
        final int cardinality = bitset.cardinality();
        if (cardinality==parent.natoms) return parent.molecule;
        int[] indizes = new int[cardinality];
        int k=0;
        for (int b = bitset.nextSetBit(0); b>=0; b = bitset.nextSetBit(b+1)) {
            indizes[k++] = b;
        }
        try {
            return AtomContainerManipulator.extractSubstructure(parent.molecule, indizes);
            // Atom objects in the resulting IAtomContainer are copies of the Atom objects in parent.molecule
            // --> the number of implicit hydrogen atoms remains
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

    /**
     * Returns the number of all hydrogen atoms (explicit and implicit) in this fragment.<br>
     *
     * If this object is not a real fragment, this method returns 0.
     *
     * @return number of explicit and implicit hydrogen atoms in this fragment
     */
    public int numberOfHydrogens() {
        if(!this.innerNode) return this.formula.numberOfHydrogens();
        TableSelection sel = parent.getTableSelectionOfFormula();
        int[] atomLabels = parent.getAtomLabels();
        int count=0;
        for (int b = bitset.nextSetBit(0); b>=0; b = bitset.nextSetBit(b+1)) {
            if(atomLabels[b] == sel.hydrogenIndex()){
                count++;
            }else {
                count += parent.hydrogens[b];
            }
        }
        return count;
    }

    /**
     * Returns the number of hydrogen atoms by which the given molecular formula and the molecular formula of this
     * fragment differ.
     *
     * @param matcher another {@link MolecularFormula} object
     * @return difference of hydrogen atoms
     * @throws IllegalArgumentException if the given {@link MolecularFormula} differs not only in the number of hydrogen atoms
     */
    public int hydrogenRearrangements(MolecularFormula matcher) {
        MolecularFormula f = this.getFormula().withoutHydrogen();
        final MolecularFormula hydrogens = matcher.subtract(f);
        if (hydrogens.numberOfHydrogens()!=hydrogens.atomCount()) {
            throw new IllegalArgumentException("The given molecular formula differs not only " +
                    "in the number of hydrogen atoms");
        }
        return this.numberOfHydrogens() - hydrogens.numberOfHydrogens();
    }

    /**
     * In general, this method returns the molecular formula of this fragment including the hydrogen atoms.
     *
     * @return {@link MolecularFormula} object that represents the molecular formula of this fragment
     */
    public MolecularFormula getFormula() {
        if(formula==null) determineFormula();
        return formula;
    }

    /**
     * This method computes the molecular formula of this fragment including the hydrogen atom.
     */
    private void determineFormula() {
        final TableSelection sel = parent.getTableSelectionOfFormula();
        short[] buffer = sel.makeCompomer();
        int[] labels = parent.getAtomLabels();
        for (int node = bitset.nextSetBit(0); node >= 0; node = bitset.nextSetBit(node + 1)) {
            ++buffer[labels[node]];
        }
        buffer[sel.hydrogenIndex()] = (short) this.numberOfHydrogens();
        this.formula = MolecularFormula.fromCompomer(sel, buffer);
    }

    public TIntArrayList bonds() {
        if(!this.innerNode) return new TIntArrayList();
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
        return this.innerNode && bitset.get(b.getAtom(0).getIndex()) && bitset.get(b.getAtom(1).getIndex());
    }
    public boolean stillContains(IAtom a) {
        return this.innerNode && bitset.get(a.getIndex());
    }

    public String toSMILES() {
        try {
            return SmilesGenerator.generic().create(toMolecule());
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    public String toSMARTS(Set<IBond> bondsToCut) {
        final SmartsGen extr = new SmartsGen(parent.molecule);
        TIntHashSet atoms = new TIntHashSet();
        TIntHashSet peripherie = new TIntHashSet();
        for (IAtom a : getAtoms()) atoms.add(a.getIndex());
        for (IBond b : bondsToCut) {
            if (atoms.contains(b.getAtom(0).getIndex()) && !atoms.contains(b.getAtom(1).getIndex())) {
                peripherie.add(b.getAtom(1).getIndex());
            } else if (atoms.contains(b.getAtom(1).getIndex()) && !atoms.contains(b.getAtom(0).getIndex())) {
                peripherie.add(b.getAtom(0).getIndex());
            }
        }
        return extr.generate(atoms.toArray(), peripherie.toArray());
    }

    public float getPeakIntensity() {
        return peakIntensity;
    }

    public boolean isInnerNode(){
        return this.innerNode;
    }

    public IAtomContainer getLoss() {
        final HashSet<IAtom> atoms = new HashSet<>();
        final HashSet<IBond> bonds = new HashSet<>();
        for (IAtom a : parent.molecule.atoms()) atoms.add(a);
        for (IAtom a : getAtoms()) atoms.remove(a);
        try {
            return AtomContainerManipulator.extractSubstructure(parent.molecule, atoms.stream().mapToInt(IAtom::getIndex).toArray());
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, String> getAllSMARTS() {

        int k=0;
        for (IAtom a : parent.molecule.atoms()) {
            a.setProperty("PID", k++);
        }

        ArrayList<IAtomContainer> mols = new ArrayList<>();
        mols.addAll(Arrays.asList(extractSubstructures(Arrays.stream(this.getAtoms()).mapToInt(IAtom::getIndex).toArray())));
        final HashSet<IAtom> loss = new HashSet<>();
        for (IAtom a : parent.molecule.atoms()) loss.add(a);
        for (IAtom a : this.getAtoms()) loss.remove(a);
        mols.addAll(Arrays.asList(extractSubstructures(loss.stream().mapToInt(IAtom::getIndex).toArray())));
        final SmilesGenerator smi = SmilesGenerator.unique();
        final SmartsGen extr = new SmartsGen(parent.molecule);
        final HashMap<String, String> map = new HashMap<>();
        for (IAtomContainer m : mols) {
            try {
                final String smiles = smi.create(m);
                final TIntArrayList xs = new TIntArrayList();
                for (IAtom a : m.atoms()) xs.add(((Integer)a.getProperty("PID")).intValue());
                final String smarts = extr.generate(xs.toArray(), new int[0]);
                map.put(smiles, smarts);

            } catch (CDKException e) {
                LoggerFactory.getLogger(CombinatorialFragment.class).warn(e.getMessage());
            }

        }
        return map;
    }

    public String toSMARTSLoss(Set<IBond> bondsToCut) {
        final SmartsGen extr = new SmartsGen(parent.molecule);
        TIntHashSet atoms = new TIntHashSet();
        TIntHashSet peripherie = new TIntHashSet();
        for (IAtom a : parent.molecule.atoms()) atoms.add(a.getIndex());
        for (IAtom a : getAtoms()) atoms.remove(a.getIndex());
        for (IBond b : bondsToCut) {
            if (atoms.contains(b.getAtom(0).getIndex()) && !atoms.contains(b.getAtom(1).getIndex())) {
                peripherie.add(b.getAtom(1).getIndex());
            } else if (atoms.contains(b.getAtom(1).getIndex()) && !atoms.contains(b.getAtom(0).getIndex())) {
                peripherie.add(b.getAtom(0).getIndex());
            }
        }
        return extr.generate(atoms.toArray(), peripherie.toArray());
    }

    public String toUniqueSMILES() {
        try {
            return SmilesGenerator.unique().create(toMolecule());
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    public IAtomContainer[] getNeutralLosses() {
        HashSet<IAtom> atoms = new HashSet<>();
        for (IAtom a : parent.molecule.atoms()) {
            atoms.add(a);
        }
        for (IAtom a : getAtoms()) atoms.remove(a);
        IAtom[] todo = atoms.toArray(IAtom[]::new);
        final TIntArrayList indizes = new TIntArrayList();
        ArrayList<IAtomContainer> losses = new ArrayList<>();
        for (IAtom a : todo) {
            indizes.clearQuick();
            if (atoms.remove(a)) {
                indizes.add(a.getIndex());
                ArrayList<IAtom> stack = new ArrayList<>();
                stack.add(a);
                while (!stack.isEmpty()) {
                    final IAtom b = stack.remove(stack.size() - 1);
                    for (IBond bond : b.bonds()) {
                        for (IAtom neighbour : bond.atoms()) {
                            if (atoms.remove(neighbour)) {
                                stack.add(neighbour);
                                indizes.add(neighbour.getIndex());
                            }
                        }
                    }
                }
                try {
                    losses.add(AtomContainerManipulator.extractSubstructure(parent.molecule, indizes.toArray()));
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return losses.toArray(IAtomContainer[]::new);
    }

    private IAtomContainer[] extractSubstructures(int[] atomids) {
        Set<IAtom> atoms = Arrays.stream(atomids).mapToObj(parent.molecule::getAtom).collect(Collectors.toSet());
        IAtom[] todo = atoms.toArray(IAtom[]::new);
        final TIntArrayList indizes = new TIntArrayList();
        ArrayList<IAtomContainer> losses = new ArrayList<>();
        for (IAtom a : todo) {
            indizes.clearQuick();
            if (atoms.remove(a)) {
                indizes.add(a.getIndex());
                ArrayList<IAtom> stack = new ArrayList<>();
                stack.add(a);
                while (!stack.isEmpty()) {
                    final IAtom b = stack.remove(stack.size() - 1);
                    for (IBond bond : b.bonds()) {
                        for (IAtom neighbour : bond.atoms()) {
                            if (atoms.remove(neighbour)) {
                                stack.add(neighbour);
                                indizes.add(neighbour.getIndex());
                            }
                        }
                    }
                }
                try {
                    losses.add(AtomContainerManipulator.extractSubstructure(parent.molecule, indizes.toArray()));
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return losses.toArray(IAtomContainer[]::new);
    }
}
