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

    protected CombinatorialFragment inverse;

    public CombinatorialFragment(MolecularGraph parent, BitSet bitset, BitSet disconnectedRings){
        this(parent, bitset, null, disconnectedRings);
    }

    public CombinatorialFragment getInverse() {
        return inverse;
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

    public BitSet getBitSet(){
        return this.bitset;
    }

    public MolecularGraph getIntactMolecule(){
        return this.parent;
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

    public IAtomContainer toMolecule() { //todo: add the hydrogen atom at the cutted site
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
                count++; //explicit hydrogen atoms
            }else {
                count += parent.hydrogens[b]; // implicit hydrogen atoms
                count += this.numberOfCutIncidentBonds(b); // hydrogen atoms which are bound to this atom after cutting the incident bond
            }
        }
        return count;
    }

    /**
     * Returns the number of bonds which are incident to the atom with index {@code atomIdx} and disconnect in this fragment.
     *
     * @param atomIdx index of the atom in the {@link CombinatorialFragment#parent molecule}
     * @return number of adjacent atoms in {@link CombinatorialFragment#parent molecule} which were removed
     */
    private int numberOfCutIncidentBonds(int atomIdx){
        int[][] adjList = this.parent.adjacencyList;
        int[] adjacentAtoms = adjList[atomIdx];
        int count = 0;
        for(int k : adjacentAtoms){
            if(!this.bitset.get(k)) count++;
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

    public ArrayList<Integer> bonds() {
        if(!this.innerNode) return new ArrayList<>();
        final int[][] adj = parent.getAdjacencyList();
        final int[][] bondj = parent.bondList;
        final ArrayList<Integer> bonds = new ArrayList<>();
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
        extr.setLevel(SmartsGen.Level.EXACT);
        for (int a : peripherie.toArray()) extr.setLevel(a, SmartsGen.Level.GENERIC);
        atoms.addAll(peripherie);
        return extr.generate(atoms.toArray());
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

    public HashMap<String, String> getAllSMARTS(boolean keepAromaticRingsIntact, boolean addCutEdges) {
        return getAllSMARTS(keepAromaticRingsIntact,addCutEdges, 0);
    }

    public HashMap<String, String> getAllSMARTS(boolean keepAromaticRingsIntact, boolean addCutEdges, int keepRingsSmallerIntactThanX) {

        int k=0;
        for (IAtom a : parent.molecule.atoms()) {
            a.setProperty("PID", k++);
        }

        ArrayList<IAtomContainer> mols = new ArrayList<>();
        mols.addAll(Arrays.asList(extractSubstructures(Arrays.stream(this.getAtoms()).mapToInt(IAtom::getIndex).toArray(),keepAromaticRingsIntact, addCutEdges,keepRingsSmallerIntactThanX, false)));
        final HashSet<IAtom> loss = new HashSet<>();
        for (IAtom a : parent.molecule.atoms()) loss.add(a);
        for (IAtom a : this.getAtoms()) loss.remove(a);
        mols.addAll(Arrays.asList(extractSubstructures(loss.stream().mapToInt(IAtom::getIndex).toArray(),keepAromaticRingsIntact, addCutEdges,keepRingsSmallerIntactThanX, true)));
        final SmilesGenerator smi = SmilesGenerator.unique();
        final SmartsGen extr = new SmartsGen(parent.molecule);
        final HashMap<String, String> map = new HashMap<>();
        for (IAtomContainer m : mols) {
            extr.setLevel(SmartsGen.Level.EXACT);
            try {
                final String smiles = smi.create(m);
                final TIntArrayList xs = new TIntArrayList();
                for (IAtom a : m.atoms()) {
                    final int pid = a.getProperty("PID", Integer.class);
                    xs.add(pid);
                    if (a.getProperty(SmartsGen.ATOM_LABELING_LEVEL)!=null) {
                        extr.setLevel(pid, a.getProperty(SmartsGen.ATOM_LABELING_LEVEL));
                    } else if (!a.getSymbol().equals("C")) {
                        extr.setLevel(pid, SmartsGen.Level.WITH_AROMATICITY);
                    }
                }
                final String smarts = extr.generateShortest(xs.toArray());
                if (smarts != null) map.put(smiles, smarts);

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
        extr.setLevel(SmartsGen.Level.EXACT);
        for (int a : peripherie.toArray()) extr.setLevel(a, SmartsGen.Level.GENERIC);
        atoms.addAll(peripherie);
        return extr.generate(atoms.toArray());
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

    public boolean isDirectFragmentOf(CombinatorialFragment parentFragment){
        // naive approach: cut all bonds in parentFragment and check if this fragment is contained in the resulting list
        final CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(this.parent);
        final List<CombinatorialFragment> fragments = fragmenter.cutAllBonds(parentFragment, null);
        for(final CombinatorialFragment fragment : fragments){
            if(fragment.bitset.equals(this.bitset)) return true;
        }
        return false;
    }

    private IAtomContainer[] extractSubstructures(int[] atomids, boolean keepAromaticRingsIntact, boolean addCutEdges, int keepRingsSmallerIntactThanX, boolean isLoss) {
        Set<IAtom> atoms = Arrays.stream(atomids).mapToObj(parent.molecule::getAtom).collect(Collectors.toSet());
        final BitSet visited = new BitSet(parent.molecule.getAtomCount());
        final Set<IAtom> orig = new HashSet<>(atoms);
        IAtom[] todo = atoms.toArray(IAtom[]::new);
        final TIntHashSet indizes = new TIntHashSet();
        final TIntHashSet cutIndizes = new TIntHashSet();
        ArrayList<IAtomContainer> losses = new ArrayList<>();
        Set<IAtom> ringSet = new HashSet<>();
        // keep rings intact
        if (keepRingsSmallerIntactThanX > 0) {
            for (int i = 0; i < parent.bonds.length; ++i) {
                if (parent.ringMemberships[i].length == 0) continue;
                IBond b = parent.bonds[i];
                if (atoms.contains(b.getAtom(0)) && atoms.contains(b.getAtom(1))) {
                    for (int j = 0; j < parent.ringMemberships[i].length; ++j) {
                        ArrayList<IBond> bondsOfRing = parent.bondsOfRings[parent.ringMemberships[i][j]];
                        if (bondsOfRing.size() <= keepRingsSmallerIntactThanX) {
                            // add all atoms of the ring to the atoms set (but not the orig set)
                            for (IBond ringbond : bondsOfRing) {
                                atoms.add(ringbond.getAtom(0));
                                atoms.add(ringbond.getAtom(1));
                                ringSet.add(ringbond.getAtom(0));
                                ringSet.add(ringbond.getAtom(1));

                            }
                        }
                    }
                }
            }
        }
        ringSet.removeAll(orig);
        ///////////////////

        for (IAtom a : todo) {
            indizes.clear();
            cutIndizes.clear();
            visited.clear();
            visited.set(a.getIndex());
            if (atoms.remove(a)) {
                indizes.add(a.getIndex());
                ArrayList<IAtom> stack = new ArrayList<>();
                stack.add(a);
                while (!stack.isEmpty()) {
                    final IAtom b = stack.remove(stack.size() - 1);
                    for (IBond bond : b.bonds()) {
                        for (IAtom neighbour : bond.atoms()) {
                            if (!visited.get(neighbour.getIndex()) && (atoms.remove(neighbour) || (keepAromaticRingsIntact && b.isAromatic() && a.isAromatic() && neighbour.isAromatic()))) {
                                stack.add(neighbour);
                                indizes.add(neighbour.getIndex());
                                visited.set(neighbour.getIndex());
                            } else if (addCutEdges && (orig.contains(a) && !orig.contains(neighbour)) || ringSet.contains(neighbour)) {
                                cutIndizes.add(neighbour.getIndex());
                            }
                        }
                    }
                }
                //////////////////////////////////////////////////
                try {
                    final IAtomContainer m = extractSubstructureAndMark(parent.molecule, indizes.toArray(), cutIndizes);
                    losses.add(m);
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return losses.toArray(IAtomContainer[]::new);
    }
    public static IAtomContainer extractSubstructureAndMark(IAtomContainer atomContainer, int[] atomIndices, TIntHashSet atomIndizesToMark)
            throws CloneNotSupportedException {
        IAtomContainer substructure = (IAtomContainer) atomContainer.clone();
        int numberOfAtoms = substructure.getAtomCount();
        IAtom[] atoms = new IAtom[numberOfAtoms];
        for (int atomIndex = 0; atomIndex < numberOfAtoms; atomIndex++) {
            atoms[atomIndex] = substructure.getAtom(atomIndex);
        }

        Arrays.sort(atomIndices);
        for (int index = 0; index < numberOfAtoms; index++) {
            if (Arrays.binarySearch(atomIndices, index) < 0) {
                if (atomIndizesToMark.contains(index)) {
                    IAtom atom = atoms[index];
                    atom.setProperty(SmartsGen.ATOM_LABELING_LEVEL, SmartsGen.Level.ELEMENT);
                } else {
                    IAtom atom = atoms[index];
                    substructure.removeAtom(atom);
                }
            }
        }

        return substructure;
    }
}
