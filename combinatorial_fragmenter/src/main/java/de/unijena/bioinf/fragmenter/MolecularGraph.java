package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.TableSelection;
import gnu.trove.impl.Constants;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Optional;

public class MolecularGraph {

    public static final boolean CALCULATE_MORGAN_INDEX = true;

    protected final IAtomContainer molecule;
    protected final int natoms;
    protected int[][] adjacencyList, bondList;
    private final int[] atomLabels;
    protected MolecularFormula formula;
    protected int[][] sssr;
    protected ArrayList<IBond>[] bondsOfRings; // per ring: ids of the bonds
    protected int[][] ringMemberships; // per bond: ids of the rings
    protected IBond[] bonds;
    protected int[] hydrogens;

    protected int[] atomIdentities;

    public MolecularGraph(IAtomContainer c) {
        this(MolecularFormula.parseOrThrow(MolecularFormulaManipulator.getString(MolecularFormulaManipulator.getMolecularFormula(c))), c);
    }

    public boolean inRing(IBond b) {
        return ringMemberships[b.getIndex()].length>0;
    }

    public MolecularGraph(MolecularFormula formula, IAtomContainer mol) {
        this.molecule = mol;

        this.sssr = Cycles.mcb(mol).paths();
        try {
            AtomContainerManipulator.percieveAtomTypesAndConfigureUnsetProperties(molecule);
            Aromaticity.cdkLegacy().apply(molecule);
        } catch (CDKException e) {
            e.printStackTrace();
        }
        this.formula = formula;
        this.natoms = molecule.getAtomCount();
        this.ringMemberships = new int[molecule.getBondCount()][];
        final PeriodicTable T = PeriodicTable.getInstance();
        TableSelection sel = formula.getTableSelection();
        this.adjacencyList = new int[natoms][];
        this.bondList = new int[natoms][];
        this.atomLabels = new int[natoms];
        this.hydrogens = new int[natoms];
        for (IAtom a : mol.atoms()) {
            hydrogens[a.getIndex()] = a.getImplicitHydrogenCount();
        }
        this.bonds = new IBond[mol.getBondCount()];
        for (IBond b : mol.bonds()) this.bonds[b.getIndex()] = b;
        for (int k=0; k < adjacencyList.length; ++k) {
            final IAtom atom = molecule.getAtom(k);
            adjacencyList[k] = new int[atom.getBondCount()];
            bondList[k] = new int[atom.getBondCount()];
            int l=0;
            for (IBond b : atom.bonds()) {
                bondList[k][l] = b.getIndex();
                adjacencyList[k][l++] = b.getOther(atom).getIndex();
            }
            atomLabels[k] = sel.indexOf(T.get(atom.getAtomicNumber()));
        }
        this.bondsOfRings = (ArrayList<IBond>[]) new ArrayList[sssr.length];
        calculateRingMembership();
        if (CALCULATE_MORGAN_INDEX) calculateMorganIndex();
    }

    public Optional<int[]> getAtomIdentities() {
        return Optional.ofNullable(atomIdentities);
    }

    private void calculateMorganIndex() {
        final CircularFingerprinterMod circularFingerprinter = new CircularFingerprinterMod(CircularFingerprinterMod.CLASS_ECFP2);
        circularFingerprinter.storeIdentitesPerIteration=true;
        try {
            circularFingerprinter.calculate(molecule);
            this.atomIdentities = circularFingerprinter.identitiesPerIteration.get(circularFingerprinter.identitiesPerIteration.size()-1);
            for (int i=0; i < atomIdentities.length; ++i) {
                molecule.getAtom(i).setProperty("ECFP", atomIdentities[i]);
            }
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    public CombinatorialFragment asFragment() {
        BitSet bitset = new BitSet();
        for (int i=0; i < natoms; ++i) bitset.set(i);
        return new CombinatorialFragment(this, bitset, formula, new BitSet(bondsOfRings.length));
    }

    public IAtomContainer getMolecule() {
        return molecule;
    }

    public MolecularFormula getFormula(){
        return this.formula;
    }

    public IBond[] getBonds() {
        return bonds;
    }

    public int[] getHydrogens(){
        return this.hydrogens;
    }

    private void calculateRingMembership() {
        final TIntArrayList[] memberships = new TIntArrayList[bonds.length];
        for (int i=0; i < memberships.length; ++i) memberships[i] = new TIntArrayList();
        for (int i=0; i  <bondsOfRings.length; ++i) bondsOfRings[i] = new ArrayList<>();
        for (int ring = 0; ring < sssr.length; ++ring) {
            int[] path = sssr[ring];
            for (int j = 1; j < path.length; ++j) {
                IBond bond = molecule.getBond(molecule.getAtom(path[j - 1]), molecule.getAtom(path[j]));
                memberships[bond.getIndex()].add(ring);
                bondsOfRings[ring].add(bond);
            }
        }
        for (int i=0; i < memberships.length; ++i) {
            ringMemberships[i] = memberships[i].toArray();
        }
    }

    public Element getElementOf(int atom) {
        return formula.getTableSelection().get(atomLabels[atom]);
    }

    int[] getAtomLabels() {
        return atomLabels;
    }

    public int[][] getAdjacencyList() {
        return adjacencyList;
    }

    public int[][] getBondList(){
        return this.bondList;
    }

    TableSelection getTableSelectionOfFormula() {
        return formula.getTableSelection();
    }

    public int[][] getRingMemberships(){
        return this.ringMemberships;
    }

    public int[][] getSSSR(){
        return this.sssr;
    }

    public ArrayList<IBond>[] getBondsOfRings(){
        return this.bondsOfRings;
    }

    private String[] getBondTypeName(IBond bond, boolean specificBondName){
        String[] bondNames = new String[2];
        if(specificBondName){
            bondNames[0] = DirectedBondTypeScoring.bondNameSpecific(bond, true);
            bondNames[1] = DirectedBondTypeScoring.bondNameSpecific(bond, false);
        }else{
            bondNames[0] = DirectedBondTypeScoring.bondNameGeneric(bond, true);
            bondNames[1] = DirectedBondTypeScoring.bondNameGeneric(bond, false);
        }
        return bondNames;
    }

    public TObjectIntHashMap<String> getNumberOfBondTypes(boolean specificBondName){
        TObjectIntHashMap<String> bondNames2Amount = new TObjectIntHashMap<>(this.molecule.getBondCount(), 0.75f, 0);

        for(IBond bond : this.molecule.bonds()){
            String[] bondNames = this.getBondTypeName(bond, specificBondName);

            if(bondNames2Amount.containsKey(bondNames[0])){
                bondNames2Amount.adjustValue(bondNames[0], 1);
            }else if(bondNames2Amount.containsKey(bondNames[1])){
                bondNames2Amount.adjustValue(bondNames[1], 1);
            }else{
                bondNames2Amount.put(bondNames[0], 1);
            }
        }

        return bondNames2Amount;
    }

    public HashMap<String, ArrayList<IBond>> bondNames2BondList(boolean specificBondName){
        HashMap<String, ArrayList<IBond>> bondNames2BondList = new HashMap<>(this.molecule.getBondCount(), 0.75f);

        for(IBond bond : this.bonds){
            String[] bondNames = this.getBondTypeName(bond, specificBondName);

            if(bondNames2BondList.containsKey(bondNames[0])){
                bondNames2BondList.get(bondNames[0]).add(bond);
            }else if(bondNames2BondList.containsKey(bondNames[1])){
                bondNames2BondList.get(bondNames[1]).add(bond);
            }else{
                ArrayList<IBond> bondList = new ArrayList<>();
                bondList.add(bond);
                bondNames2BondList.put(bondNames[0], bondList);
            }
        }

        return bondNames2BondList;
    }
}
