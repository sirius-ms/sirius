package matching.algorithm;

import matching.utils.HungarianAlgorithm;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.isomorphism.*;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.BondManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p>This class represents the implementation of the MCES distance which is limited
 * by allowing only a certain number of bond modifications in each molecule.<br>
 * An object of this class checks if two molecules can be converted into one another
 * by modifying at most k bonds per molecule. Currently, k is set to 1!
 * If this is the case and the total cost does not exceed the specified threshold,
 * {@link MCESDist2#compare()} will return the <b>minimal cost</b> for modifying both molecules;
 * if not, {@link Double#POSITIVE_INFINITY} will be returned.</p>
 *
 * There are two different types of bond modifications:
 * <ul>
 *     <li><b>bond deletions</b> those cost are equal to the bond oder, and </li>
 *     <li><b>electron pair modifications</b>: an electron pair of non-aromatic bonds will be deleted which costs 1 or
 *     aromatic bonds will be modified to non-aromatic single/double bonds which costs 0.5.</li>
 * </ul>
 *
 * Which one of these two modifications will be used is determined by the selected {@link MatchingType matching type}.
 * There are three different matching types:
 * <ul>
 *     <li>{@link MatchingType#BOND_DEL_MATCH_ORDER BOND_DEL_MATCH_ORDER}:<br>
 *     At most k bonds of each molecule will be deleted and the resulting structures have to be isomorphic.
 *     The isomorphism test utilizes {@link BondMatcher#forStrictOrder()}.
 *     The resulting cost is the sum of all costs for the deletion of at most k bonds in each molecule.</li>
 *     <li>{@link MatchingType#BOND_DEL_MATCH_ANY BOND_DEL_MATCH_ANY}:<br>
 *     At most k bonds of each molecule will be deleted and all possible isomorphisms between both modified structures
 *     will be computed. Note that these isomorphisms only take into account the atom types and the connectivity -
 *     i.e. bond orders are ignored. Subsequently, for each isomorphism corresponding costs for mapping different
 *     bond types onto each other will be computed (e.g. |2-1| = 1 for mapping a double bond onto a single bond).
 *     The resulting cost consists of the minimal mapping costs plus the deletion costs.</li>
 *     <li>{@link MatchingType#ELECTRON_PAIR_MOD ELECTRON_PAIR_MOD}:<br>
 *     At most k electron pairs of at most k bonds of each molecule will be modified as mentioned above and
 *     the resulting structures have to be isomorphic. Again, the isomorphism test utilizes
 *     {@link BondMatcher#forStrictOrder()}, and the resulting costs are defined as the sum of all costs for modifying
 *     at most k electron pairs of at most k bonds in each molecule.</li>
 * </ul>
 *
 */
public class MCESDist2 extends EDIC{

    public enum MatchingType{
        BOND_DEL_MATCH_ORDER, // bonds will be deleted and resulting structures checked for isomorphisms considering bond order
        BOND_DEL_MATCH_ANY, // bonds will be deleted and resulting structures checked for isomorphism considering NO bond order
        ELECTRON_PAIR_MOD // electron pair modifications (SB -> del, AB -> {SB, DB}, DB -> SB,...)
        // and resulting structures checked for isomorphism considering bond order
    }

    private final static double THRESHOLD = 2d;
    private final MatchingType matchingType;
    private double lowerBound;

    public MCESDist2(IAtomContainer molecule1, IAtomContainer molecule2, MatchingType matchingType) throws CDKException {
        super(molecule1, molecule2);
        this.matchingType = matchingType;
        this.lowerBound = -1d;
    }

    public MCESDist2(IAtomContainer molecule1, IAtomContainer molecule2) throws CDKException{
        this(molecule1, molecule2, MatchingType.BOND_DEL_MATCH_ANY);
    }

    private IAtomContainer copyAndRemoveExplicitImplicitHydrogenAtoms(IAtomContainer molecule){
        IAtomContainer newMolecule = AtomContainerManipulator.removeHydrogens(molecule); // remove explicit hydrogens
        for(final IAtom atom : newMolecule.atoms()) atom.setImplicitHydrogenCount(0); // set flags for implicit hydrogens to 0
        return newMolecule;
    }

    private IAtomContainer preprocessMolecule(IAtomContainer molecule){
        IAtomContainer processedMolecule = this.copyAndRemoveExplicitImplicitHydrogenAtoms(molecule);

        // save method for removing single atoms
        ArrayList<IAtom> atomsToRemove = new ArrayList<>(molecule.getAtomCount());
        for(final IAtom atom : processedMolecule.atoms()){
            if(processedMolecule.getConnectedBondsCount(atom) == 0){ // remove single atoms
                atomsToRemove.add(atom);
            } else if (atom.getFormalCharge() != 0){ // remove the formal charge because the isomorphism algorithm cannot map e.g. N to [N+]
                atom.setFormalCharge(0);
            }
        }
        atomsToRemove.forEach(processedMolecule::removeAtom);
        return processedMolecule;
    }

    @Override
    public double compare() {
        final IAtomContainer mol1 = this.preprocessMolecule(this.getFirstMolecule());
        final IAtomContainer mol2 = this.preprocessMolecule(this.getSecondMolecule());

        if(this.weightedDegreeBasedFilter(mol1, mol2) <= THRESHOLD){
            if(this.neighborhoodBasedFilter(mol1,mol2) <= THRESHOLD){
                try {
                    // 1.: Create list of modified copies of mol1 and mol2:
                    final HashMap<IAtomContainerSet, Double> modMol1Copies = this.getModifiedMoleculeCopies(mol1);
                    final HashMap<IAtomContainerSet, Double> modMol2Copies = this.getModifiedMoleculeCopies(mol2);

                    // 2.: Iterate over all combinations of modifications and
                    // check if both modified structures are isomorphic
                    double minDistance = Double.POSITIVE_INFINITY;
                    for (final IAtomContainerSet modMol1Copy : modMol1Copies.keySet()) {
                        final double modCost1 = modMol1Copies.get(modMol1Copy);
                        for (final IAtomContainerSet modMol2Copy : modMol2Copies.keySet()) {
                            final double modCost2 = modMol2Copies.get(modMol2Copy);

                            // For both molecules an isomorphism check will be done, if the number of connected components is equal
                            // otherwise: both modified molecules are not isomorphic
                            final int numComponents1 = modMol1Copy.getAtomContainerCount();
                            final int numComponents2 = modMol2Copy.getAtomContainerCount();
                            if (numComponents1 == numComponents2) {
                                if (numComponents1 > 1) {
                                    // bipartite matching for the components:
                                    // costMatrix[i][j] := cost for mapping component i in modMol1Copy onto component j in modMol2Copy
                                    final double[][] costMatrix = new double[numComponents1][numComponents2];
                                    for (int i = 0; i < numComponents1; i++) {
                                        for (int j = 0; j < numComponents2; j++) {
                                            final double mappingCost = this.getComponentMappingCost(modMol1Copy.getAtomContainer(i), modMol2Copy.getAtomContainer(j));
                                            costMatrix[i][j] = (mappingCost == Double.POSITIVE_INFINITY) ? Double.MAX_VALUE : mappingCost; // the HungarianAlgorithm cannot handle Infinity
                                        }
                                    }

                                    // assignment[i] = j means that component i in modMol1Copy is assigned to component j in modMol2Copy
                                    final int[] assignment = new HungarianAlgorithm(costMatrix).execute();
                                    double cost = modCost1 + modCost2;
                                    for (int i = 0; i < assignment.length; i++) {
                                        if (costMatrix[i][assignment[i]] == Double.MAX_VALUE) {
                                            cost = Double.POSITIVE_INFINITY;
                                            break;
                                        } else {
                                            cost += costMatrix[i][assignment[i]];
                                        }
                                    }
                                    minDistance = Math.min(minDistance, cost);
                                } else if (numComponents1 == 1) {
                                    final IAtomContainer component1 = modMol1Copy.getAtomContainer(0); // maybe here will be an IndexOutOfBoundsException
                                    final IAtomContainer component2 = modMol2Copy.getAtomContainer(0);
                                    minDistance = Math.min(minDistance, modCost1 + modCost2 +
                                            this.getComponentMappingCost(component1, component2));
                                } else {
                                    minDistance = Math.min(minDistance, modCost1 + modCost2);
                                }

                                if (minDistance == this.lowerBound) {
                                    this.score = minDistance;
                                    return this.score;
                                }
                            }
                        }
                    }

                    this.score = minDistance <= THRESHOLD ? minDistance : Double.POSITIVE_INFINITY;
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            }else{
                this.score = Double.POSITIVE_INFINITY;
            }
        }else{
            this.score = Double.POSITIVE_INFINITY;
        }

        return this.score;
    }


    private double getComponentMappingCost(IAtomContainer component1, IAtomContainer component2){
        if(component1.getAtomCount() != component2.getAtomCount()) return Double.POSITIVE_INFINITY;
        if(!this.haveSameMolecularFormula(component1, component2)) return Double.POSITIVE_INFINITY;

        final Pattern pattern = this.matchingType.equals(MatchingType.BOND_DEL_MATCH_ANY) ?
                VentoFoggia.findIdentical(component1, AtomMatcher.forElement(), BondMatcher.forAny()) :
                VentoFoggia.findIdentical(component1, AtomMatcher.forElement(), BondMatcher.forStrictOrder());

        if(this.matchingType.equals(MatchingType.BOND_DEL_MATCH_ANY)){
            double minCost = Double.POSITIVE_INFINITY;
            final Mappings mappings = pattern.matchAll(component2);
            for(final int[] mapping : mappings) minCost = Math.min(minCost, this.sumBondScoreDifferences(component1, component2, mapping));
            return minCost;
        }else{
            // pattern.matches doesn't just compute if there is one mapping --> apparently, it calls matchAll too
            return pattern.matchAll(component2).first().length > 0 ? 0d : Double.POSITIVE_INFINITY;
        }
    }

    private boolean haveSameMolecularFormula(IAtomContainer molecule1, IAtomContainer molecul2){
        final IMolecularFormula mf1 = MolecularFormulaManipulator.getMolecularFormula(molecule1);
        final IMolecularFormula mf2 = MolecularFormulaManipulator.getMolecularFormula(molecul2);
        return MolecularFormulaManipulator.compare(mf1, mf2);
    }

    //todo: if we want to make the number of modified bonds variable, only this method "getModifiedMoleculeCopies" has to be changed (probably)
    private HashMap<IAtomContainerSet, Double> getModifiedMoleculeCopies(IAtomContainer molecule) throws CloneNotSupportedException {
        HashMap<IAtomContainerSet, Double> modMolCopies = new HashMap<>();
        modMolCopies.put(ConnectivityChecker.partitionIntoMolecules(molecule), 0d);
        for(int i = 0; i < molecule.getBondCount(); i++){
            if(this.matchingType.equals(MatchingType.ELECTRON_PAIR_MOD)){
                final IBond bond = molecule.getBond(i);
                if(bond.isAromatic()){
                    // Create two copies where:
                    // 1. this bond turns to a single bond
                    final IAtomContainer molCopy1 = molecule.clone();
                    molCopy1.getBond(i).setOrder(IBond.Order.SINGLE);
                    molCopy1.getBond(i).setIsAromatic(false);
                    modMolCopies.put(ConnectivityChecker.partitionIntoMolecules(molCopy1), 0.5);

                    // 2. this bond turns to a double bond
                    final IAtomContainer molCopy2 = molecule.clone();
                    molCopy2.getBond(i).setOrder(IBond.Order.DOUBLE);
                    molCopy2.getBond(i).setIsAromatic(false);
                    modMolCopies.put(ConnectivityChecker.partitionIntoMolecules(molCopy2), 0.5);

                } else if (bond.getOrder().numeric() > 1) {
                    final IAtomContainer molCopy = molecule.clone();
                    BondManipulator.decreaseBondOrder(molCopy.getBond(i));
                    modMolCopies.put(ConnectivityChecker.partitionIntoMolecules(molCopy), 1d);
                }else{
                    // it's a single bond:
                    final IAtomContainer molCopy = molecule.clone();
                    this.removeBond(molCopy, i);
                    modMolCopies.put(ConnectivityChecker.partitionIntoMolecules(molCopy), 1d);
                }
            }else{
                final IAtomContainer molCopy = molecule.clone();
                final IBond removedBond = this.removeBond(molCopy, i);
                modMolCopies.put(ConnectivityChecker.partitionIntoMolecules(molCopy), this.bond2Weight(removedBond));
            }
        }
        return modMolCopies;
    }

    private IBond removeBond(IAtomContainer molecule, int bondIdx){
        final IBond removedBond = molecule.removeBond(bondIdx);
        for(final IAtom atom : removedBond.atoms()){
            if(molecule.getConnectedBondsCount(atom) == 0){
                molecule.removeAtom(atom);
            }
        }
        return removedBond;
    }

    private double sumBondScoreDifferences(IAtomContainer mol1, IAtomContainer mol2, int[] mapping){
        double absDifferences = 0d;
        for(int i = 0; i < mapping.length; i++){
            for(int j = i+1 ; j < mapping.length; j++){
                final IBond mol1Bond = mol1.getBond(mol1.getAtom(i), mol1.getAtom(j));
                final IBond mol2Bond = mol2.getBond(mol2.getAtom(mapping[i]), mol2.getAtom(mapping[j]));
                if(mol1Bond != null && mol2Bond != null)
                    absDifferences += Math.abs(this.bond2Weight(mol1Bond) - this.bond2Weight(mol2Bond));
            }
        }
        return absDifferences;
    }

    private double bond2Weight(IBond bond){
        if(bond.isAromatic()) return 1.5;
        return switch(bond.getOrder()){
            case UNSET, SINGLE -> 1d;
            case DOUBLE -> 2d;
            case TRIPLE -> 3d;
            case QUADRUPLE -> 4d;
            case QUINTUPLE -> 5d;
            case SEXTUPLE -> 6d;
        };
    }

    public double getLowerBound(){
        if(this.lowerBound == -1d){
            IAtomContainer mol1 = AtomContainerManipulator.removeHydrogens(this.getFirstMolecule());
            IAtomContainer mol2 = AtomContainerManipulator.removeHydrogens(this.getSecondMolecule());
            return this.computeLowerBound(mol1, mol2);
        }
        return this.lowerBound;
    }

    private double computeLowerBound(IAtomContainer molecule1, IAtomContainer molecule2){
        if(this.lowerBound == -1d){
            this.lowerBound = Math.max(this.weightedDegreeBasedFilter(molecule1, molecule2), this.neighborhoodBasedFilter(molecule1, molecule2));
        }
        return this.lowerBound;
    }

    private HashMap<String, ArrayList<IAtom>> atomsByElement(IAtomContainer molecule){
        return this.atomsByElement(molecule.atoms());
    }

    private HashMap<String, ArrayList<IAtom>> atomsByElement(Iterable<IAtom> atoms){
        final HashMap<String, ArrayList<IAtom>> element2AtomList = new HashMap<>();
        atoms.forEach(atom -> element2AtomList.computeIfAbsent(atom.getSymbol(), s -> new ArrayList<>()).add(atom));
        return element2AtomList;
    }

    private HashMap<String, ArrayList<IBond>> incidentBondsByElement(IAtom atom, Iterable<IBond> incidentBonds){
        final HashMap<String, ArrayList<IBond>> element2BondList = new HashMap<>();
        incidentBonds.forEach(bond -> element2BondList.computeIfAbsent(bond.getOther(atom).getSymbol(), s -> new ArrayList<>()).add(bond));
        return element2BondList;
    }

    private ArrayList<IAtom> sortByDegree(IAtomContainer molecule, ArrayList<IAtom> atoms){
        atoms.sort((atom1, atom2) -> {
            final int degreeAtom1 = molecule.getConnectedBondsCount(atom1);
            final int degreeAtom2 = molecule.getConnectedBondsCount(atom2);
            return (int) Math.signum(degreeAtom2 - degreeAtom1);
        });
        return atoms;
    }

    public double degreeBasedFilter(IAtomContainer molecule1, IAtomContainer molecule2){
        final HashMap<String, ArrayList<IAtom>> element2AtomsMol1 = this.atomsByElement(molecule1);
        final HashMap<String, ArrayList<IAtom>> element2AtomsMol2 = this.atomsByElement(molecule2);

        double degreeDiffs = 0d;
        for(final String atomType : element2AtomsMol1.keySet()){
            final ArrayList<IAtom> sortedAtoms1 = this.sortByDegree(molecule1, element2AtomsMol1.get(atomType));
            final ArrayList<IAtom> sortedAtoms2 = element2AtomsMol2.containsKey(atomType) ?
                    this.sortByDegree(molecule2, element2AtomsMol2.get(atomType)) : new ArrayList<>();

            final int minSize = Math.min(sortedAtoms1.size(), sortedAtoms2.size());
            for(int i = 0; i < minSize; i++){
                final int deg1 = molecule1.getConnectedBondsCount(sortedAtoms1.get(i));
                final int deg2 = molecule2.getConnectedBondsCount(sortedAtoms2.get(i));
                degreeDiffs += Math.abs(deg1 - deg2);
            }

            if(sortedAtoms1.size() < sortedAtoms2.size()){
                for(int i = minSize; i < sortedAtoms2.size(); i++) degreeDiffs += molecule2.getConnectedBondsCount(sortedAtoms2.get(i));
            }else{
                for(int i = minSize; i < sortedAtoms1.size(); i++) degreeDiffs += molecule1.getConnectedBondsCount(sortedAtoms1.get(i));
            }
        }

        for(final String atomType : element2AtomsMol2.keySet()){
            if(!element2AtomsMol1.containsKey(atomType)){
                for(final IAtom atom : element2AtomsMol2.get(atomType)) degreeDiffs += molecule2.getConnectedBondsCount(atom);
            }
        }

        return degreeDiffs / 2;
    }

    private ArrayList<IAtom> sortByWeightedDegree(IAtomContainer molecule, ArrayList<IAtom> atoms){
        atoms.sort((atom1, atom2) -> {
            final double sumAtom1 = this.sumIncidentBondWeights(molecule, atom1);
            final double sumAtom2 = this.sumIncidentBondWeights(molecule, atom2);
            return (int) Math.signum(sumAtom2 - sumAtom1);
        });
        return atoms;
    }

    private List<Double> getSortedWeightedDegrees(IAtomContainer molecule, ArrayList<IAtom> atoms){
        return atoms.stream().map(atom -> this.sumIncidentBondWeights(molecule, atom))
                .sorted((s1,s2) -> (int) Math.signum(s2 - s1)).toList();
    }

    private double sumIncidentBondWeights(IAtomContainer molecule, IAtom atom){
        List<IBond> incidentBonds = molecule.getConnectedBondsList(atom);
        double sum = 0d;
        for(IBond bond : incidentBonds) sum += this.bond2Weight(bond);
        return sum;
    }

    public double weightedDegreeBasedFilter(IAtomContainer molecule1, IAtomContainer molecule2){
        final HashMap<String, ArrayList<IAtom>> element2AtomsMol1 = this.atomsByElement(molecule1);
        final HashMap<String, ArrayList<IAtom>> element2AtomsMol2 = this.atomsByElement(molecule2);

        double wDegreeDiffs = 0d;
        for(final String atomType : element2AtomsMol1.keySet()){
            final List<Double> sortedWeightedDegrees1 = this.getSortedWeightedDegrees(molecule1, element2AtomsMol1.get(atomType));
            final List<Double> sortedWeightedDegrees2 = element2AtomsMol2.containsKey(atomType) ?
                    this.getSortedWeightedDegrees(molecule2, element2AtomsMol2.get(atomType)) : new ArrayList<>();

            final int minSize = Math.min(sortedWeightedDegrees1.size(), sortedWeightedDegrees2.size());
            for(int i = 0; i < minSize; i++){
                final double deg1 = sortedWeightedDegrees1.get(i);
                final double deg2 = sortedWeightedDegrees2.get(i);
                wDegreeDiffs += Math.abs(deg1 - deg2);
            }

            if(sortedWeightedDegrees1.size() < sortedWeightedDegrees2.size()){
                for(int i = minSize; i < sortedWeightedDegrees2.size(); i++) wDegreeDiffs += sortedWeightedDegrees2.get(i);
            }else{
                for(int i = minSize; i < sortedWeightedDegrees1.size(); i++) wDegreeDiffs += sortedWeightedDegrees1.get(i);
            }
        }

        for(final String atomType : element2AtomsMol2.keySet()){
            if(!element2AtomsMol1.containsKey(atomType)){
                for(final IAtom atom : element2AtomsMol2.get(atomType)){
                    wDegreeDiffs += this.sumIncidentBondWeights(molecule2, atom);
                }
            }
        }

        return wDegreeDiffs / 2;
    }

    public double neighborhoodBasedFilter(IAtomContainer molecule1, IAtomContainer molecule2){
        final HashMap<String, ArrayList<IAtom>> element2AtomsMol1 = this.atomsByElement(molecule1);
        final HashMap<String, ArrayList<IAtom>> element2AtomsMol2 = this.atomsByElement(molecule2);

        double score = 0d;
        for(final String atomType : element2AtomsMol1.keySet()){
            if(!element2AtomsMol2.containsKey(atomType)){
                for(final IAtom atom : element2AtomsMol1.get(atomType)) score += this.sumIncidentBondWeights(molecule1, atom);
            }else {
                final ArrayList<IAtom> atomsMol1 = element2AtomsMol1.get(atomType);
                final ArrayList<IAtom> atomsMol2 = element2AtomsMol2.get(atomType);

                final int maxSize = Math.max(atomsMol1.size(), atomsMol2.size());
                final double[][] costMatrix = new double[maxSize][maxSize]; // costMatrix[i][j] := minimal cost for mapping atomsMol1.get(i) onto atomsMol2.get(j)
                for(int i = 0; i < atomsMol1.size(); i++){
                    for(int j = 0; j < atomsMol2.size(); j++){
                        costMatrix[i][j] = this.minEdgeMappingCost(molecule1, molecule2, atomsMol1.get(i), atomsMol2.get(j));
                    }
                }

                // Implicit addition of pseudo atoms which have no incident bonds:
                if(atomsMol1.size() < atomsMol2.size()){
                    for(int i = atomsMol1.size(); i < maxSize; i++){
                        for(int j = 0; j < maxSize; j++){
                            costMatrix[i][j] = this.sumIncidentBondWeights(molecule2, atomsMol2.get(j));
                        }
                    }
                }else if(atomsMol1.size() > atomsMol2.size()){
                    for(int i = 0; i < maxSize; i++){
                        for(int j = atomsMol2.size(); j < maxSize; j++){
                            costMatrix[i][j] = this.sumIncidentBondWeights(molecule1, atomsMol1.get(i));
                        }
                    }
                }

                // Minimum Bipartite Matching:
                final int[] bestAtomMapping = new HungarianAlgorithm(costMatrix).execute(); // atomsMol1.get(i) is mapped to atomsMol2.get(bestAtomMapping[i])
                for (int i = 0; i < maxSize; i++) score += costMatrix[i][bestAtomMapping[i]];
            }
        }

        for(final String atomType : element2AtomsMol2.keySet()){
            if(!element2AtomsMol1.containsKey(atomType)){
                for(final IAtom atom : element2AtomsMol2.get(atomType)) score += this.sumIncidentBondWeights(molecule2, atom);
            }
        }

        return score / 2;
    }

    private void sortByOrder(List<IBond> bonds){
        bonds.sort((b1, b2) -> {
            final double w1 = this.bond2Weight(b1);
            final double w2 = this.bond2Weight(b2);
            return (int) Math.signum(w2 - w1);
        });
    }

    private double minEdgeMappingCost(IAtomContainer molecule1, IAtomContainer molecule2, IAtom mol1Atom, IAtom mol2Atom){
        // 1. Partition the incident edges:
        final HashMap<String, ArrayList<IBond>> element2IncBondsMol1 = this.incidentBondsByElement(mol1Atom, molecule1.getConnectedBondsList(mol1Atom));
        final HashMap<String, ArrayList<IBond>> element2IncBondsMol2 = this.incidentBondsByElement(mol2Atom, molecule2.getConnectedBondsList(mol2Atom));

        // 2. Compute the minimum mapping cost:
        // 2.1. Iterate over all atom types in element2IncBondsMol1 and
        // compute for each atom type the best mapping with minimum cost:
        double mappingCost = 0d;
        for(final String atomType : element2IncBondsMol1.keySet()){
            final ArrayList<IBond> incBonds1 = element2IncBondsMol1.get(atomType);
            final ArrayList<IBond> incBonds2 = element2IncBondsMol2.computeIfAbsent(atomType, s -> new ArrayList<>());
            this.sortByOrder(incBonds1);
            this.sortByOrder(incBonds2);

            final int minSize = Math.min(incBonds1.size(), incBonds2.size());
            for(int i = 0; i < minSize; i++){
                final IBond bond1 = incBonds1.get(i);
                final IBond bond2 = incBonds2.get(i);
                mappingCost += Math.abs(this.bond2Weight(bond1) - this.bond2Weight(bond2));
            }
            if(incBonds1.size() < incBonds2.size()){
                for(int i = minSize; i < incBonds2.size(); i++) mappingCost += this.bond2Weight(incBonds2.get(i));
            } else if(incBonds2.size() < incBonds1.size()) {
                for(int i = minSize; i < incBonds1.size(); i++) mappingCost += this.bond2Weight(incBonds1.get(i));
            }
        }

        for(final String atomType : element2IncBondsMol2.keySet()){
            if(!element2IncBondsMol1.containsKey(atomType)){
                final ArrayList<IBond> incBonds = element2IncBondsMol2.get(atomType);
                for(final IBond incBond : incBonds) mappingCost += this.bond2Weight(incBond);
            }
        }

        return mappingCost;
    }

    public static void main(String[] args){
        try{
            SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            String smiles1 = "CC(CN1C2=CC=CC=C2SC3=CC=CC=C31)N(C)C";
            String smiles2 = "CC(CN1C2=CC=CC=C2SC3=CC=CC=C31)[N+](C)(C)[O-].C.C([H])([H])([H])[H]";

            MCESDist2 dist = new MCESDist2(smiParser.parseSmiles(smiles1), smiParser.parseSmiles(smiles2));
            System.out.println(dist.compare());
        }catch (CDKException e){
            e.printStackTrace();
        }
    }
}
