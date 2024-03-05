package matching.algorithm;

import matching.datastructures.AtomContainerE;
import matching.datastructures.AtomE;
import matching.datastructures.Pair;
import matching.datastructures.SideChainList;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.graph.ShortestPaths;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.Mappings;
import org.openscience.cdk.isomorphism.VentoFoggia;
import org.openscience.cdk.silent.MolecularFormula;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import matching.utils.HungarianAlgorithm;
import matching.utils.MoleculeManipulator;

import java.util.ArrayList;
import java.util.Map;

/**
 * <p>
 * An object of this class allows the comparision between two molecules of type {@link AtomContainerE}.<br>
 * </p>
 * <p>
 * These molecules will be compared under the assumption that they differ only in their arrangement of
 * their connected side chains. Which substructures are belonging to the core structure or the side chains, is determined
 * by a given list of side chains.<br>
 * </p>
 * <p>
 * These matcher calculates a distance between both molecules. This distance between
 * {@code molecule1} and {@code molecule2} is the minimal sum of weighted operations
 * to transform molecule2 into molecule1.<br>
 * An operation describes a placement of a specific side chain to an atom in molecule2 which is equivalent to an
 * atom contained in molecule1 which is connected to this side chain. This operation is weighted with the shortest distance
 * from (the bridge node of) this side chain to (the bridge node of) the side chain which is connected to the aimed atom.
 * </p>
 */
public class MinimalWeightedPlacements implements Matcher {

    /**
     * The molecule represented as {@link IAtomContainer} which will be compared to {@link #molecule2}.
     */
    private IAtomContainer molecule1;
    /**
     * The molecule represented as {@link IAtomContainer} which will be compared to {@link #molecule1}.
     */
    private IAtomContainer molecule2;
    /**
     * The list which contains the side chains whose occurrences in both molecules will be
     * removed and replaced by an '*'-atom.
     */
    private SideChainList sideChainList;
    /**
     * The distance between {@link #molecule1} and {@link #molecule2}.
     */
    private double distance;

    /**
     * Constructs a matcher object of class {@link MinimalWeightedPlacements}.<br>
     * Only if the given {@link AtomContainerE} objects and the given {@link SideChainList} object are
     * not empty, an object of this class is constructed.
     *
     * @param molecule1 the {@link IAtomContainer} which will be compared to {@code molecule2}
     * @param molecule2 the {@link IAtomContainer} which will be compared to {@code molecule1}
     * @param sideChainList the {@link SideChainList} which contains the side chains
     *                      whose occurrences in {@code molecule1} and {@code molecule2}
     *                      will be searched and substituted by '*'
     */
    public MinimalWeightedPlacements(IAtomContainer molecule1, IAtomContainer molecule2, SideChainList sideChainList) throws CDKException{
        if(molecule1.isEmpty() || molecule2.isEmpty()){
            throw new IllegalArgumentException("The given molecules don´t meet the requirements. One of the molecules" +
                    "or both are empty.");
        }else{
            if(sideChainList.size() == 0){
                throw new IllegalArgumentException("The given list of side chains is empty.");
            }else{
                // The given molecules molecule1 and molecule2 have to pre-processed first:
                // All aromatic bonds have to be detected and implicit hydrogens will be converted to explicit hydrogens.
                final CDKHydrogenAdder hydrogenAdder = CDKHydrogenAdder.getInstance(DefaultChemObjectBuilder.getInstance());
                CycleFinder cycles = Cycles.or(Cycles.all(), Cycles.all(6));
                Aromaticity aromaticity = new Aromaticity(ElectronDonation.daylight(), cycles);
                this.molecule1 = this.setUpMolecule(molecule1 instanceof AtomContainerE ? ((AtomContainerE) molecule1).getContainer() : molecule1, hydrogenAdder,aromaticity);
                this.molecule2 = this.setUpMolecule(molecule2 instanceof AtomContainerE ? ((AtomContainerE) molecule2).getContainer() : molecule2, hydrogenAdder,aromaticity);

                this.sideChainList = sideChainList;
                this.distance = -1;
            }
        }
    }

    private IAtomContainer setUpMolecule(IAtomContainer molecule, CDKHydrogenAdder hydrogenAdder, Aromaticity aromaticity) throws CDKException {
        aromaticity.apply(molecule);
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
        hydrogenAdder.addImplicitHydrogens(molecule);
        AtomContainerManipulator.convertImplicitToExplicitHydrogens(molecule);
        return molecule;
    }


    /**
     * <p>
     * Calculates and returns the distance between {@link #molecule1} and {@link #molecule2}.<br>
     * This distance is equivalent to the minimal sum of weighted side chain placements to transform
     * {@link #molecule2} into {@link #molecule1} - and vice versa. Each placement is weighted with
     * the minimum number of bonds between the source and the target atom.
     * </p>
     * <p>
     * These two molecules will be compared under the assumption that they differ only in the arrangement of their
     * side chains contained in the given {@link #sideChainList}.
     * That means, that the arrangement of these side chains in one molecule is a permutation of
     * the arrangement of these side chains in the other molecule and that the given side chain list
     * decides which substructures are belonging to the core structure and to the side chains.<br>
     * For this assumption to hold, the following must apply:
     * </p>
     * <ul>
     *     <li> {@link #molecule1} and {@link #molecule2} have the same molecular formula</li>
     *     <li> {@link #molecule1} and {@link #molecule2} have the same core structure</li>
     *     <li> for each side chain contained in {@link #sideChainList} the number
     *          of occurrences in both molecules is equal</li>
     * </ul>
     * <p>
     * During this procedure, this assumption will be checked. If {@link #molecule1} and {@link #molecule2}
     * don´t meet this requirements, {@link Double#MAX_VALUE} will be returned.
     * </p>
     *
     * @return the distance between {@link #molecule1} and {@link #molecule2}. This distance is equal to
     * {@link Double#MAX_VALUE} if the given assumption is not true.
     */
    public double compare() {
        AtomContainerE molecule1, molecule2;
        try{
            molecule1 = new AtomContainerE(this.molecule1.clone());
            molecule2 = new AtomContainerE(this.molecule2.clone());
        }catch (CloneNotSupportedException e){
            System.out.println("An error occurred while cloning both molecules.");
            this.distance = -1;
            return this.distance;
        }


        if (!this.sameMolecularFormula()) {
            this.distance = Double.MAX_VALUE;
            return this.distance;
        } else {
            // 1.REMOVAL OF THE SIDE CHAINS:

            ArrayList<Pair<AtomE, Integer>> nodesMol1 = new ArrayList<Pair<AtomE, Integer>>();
            ArrayList<Pair<AtomE, Integer>> nodesMol2 = new ArrayList<Pair<AtomE, Integer>>();

            /* Remove for each side chain contained in sideChainList every occurrences in molecule1 and molecule2,
             * and store the resulting '*'-labeled atoms together with their index of the side chain which occurred
             * at this atom. During this procedure, check if molecule1 and molecule2 contain the same side chains.
             */
            for (int i = 0; i < this.sideChainList.size(); i++) {
                Pair<AtomContainerE, ArrayList<AtomE>> r1 = MoleculeManipulator.removeSideChain(molecule1, this.sideChainList.get(i));
                molecule1 = r1.getObject1();
                Pair<AtomContainerE, ArrayList<AtomE>> r2 = MoleculeManipulator.removeSideChain(molecule2, this.sideChainList.get(i));
                molecule2 = r2.getObject1();

                // check, if the number of occurrences of sideChainList.get(i) is equal in both molecules:
                // if this is not the case -> the assumption is not fulfilled
                if (r1.getObject2().size() != r2.getObject2().size()) {
                    this.distance = Double.MAX_VALUE;
                    return this.distance;
                }

                // number of occurrences of sideChainList.get(i) is equals in both molecules:
                // -> r1.getObject2().size = r2.getObject2().size
                for (int n = 0; n < r1.getObject2().size(); n++) {
                    nodesMol1.add(new Pair<AtomE, Integer>(r1.getObject2().get(n), i));
                    nodesMol2.add(new Pair<AtomE, Integer>(r2.getObject2().get(n), i));
                }
            }

            /* At this moment it is known:
             *  - molecule1 and molecule2 have the same molecular formula
             *  - for each side chain contained in this.sideChainList every occurrences in molecule1 and
             *    molecule2 are removed and replaced by an atom with symbol '*'
             *  - molecule1 and molecule2 contained the same side chains
             * But we don´t know if both molecules have the same core structure.
             * Thus, it has to be checked if molecule1 and molecule2 are isomorphic.
             */

            //2. ISOMORPHISM:
            VentoFoggia m1 = (VentoFoggia) VentoFoggia.findIdentical(molecule1);
            Mappings isomorphisms = m1.matchAll(molecule2);

            // check, if both molecules are isomorphic:
            if (!isomorphisms.atLeast(1)) {
                // molecule1 and molecule differ in their core structure -> assumption is not fulfilled:
                this.distance = Double.MAX_VALUE;
                return this.distance;
            }

            // the assumption is fulfilled -> this.molecule1 and this.molecule2 differ only in the
            // arrangement of their side chains contained in this.sideChainList

            if (nodesMol1.size() > 0) {
                //3. WEIGHTED PERFECT BIPARTITE MATCHING:
                double minCost = Double.MAX_VALUE;
                double[][] costmatrix = new double[nodesMol1.size()][nodesMol2.size()];

                for (Map<IAtom, IAtom> isom : isomorphisms.toAtomMap()) {
                    // construct a cost matrix or adjacency matrix of an bipartite weighted graph:
                    for (int i = 0; i < nodesMol1.size(); i++) {
                        for (int j = 0; j < nodesMol2.size(); j++) {
                            if (nodesMol1.get(i).getObject2() == nodesMol2.get(j).getObject2()) {
                                ShortestPaths shortestPaths = new ShortestPaths(molecule2, nodesMol2.get(j).getObject1());
                                costmatrix[i][j] = shortestPaths.distanceTo(isom.get(nodesMol1.get(i).getObject1()));
                            } else {
                                costmatrix[i][j] = Double.MAX_VALUE;
                            }
                        }
                    }

                    // find the optimal assignment/matching:
                    HungarianAlgorithm hungarianAlg = new HungarianAlgorithm(costmatrix);
                    int[] optAssignment = hungarianAlg.execute();

                    /* calculate the cost of this assignment:
                     * optAssignment[i]: ith side chain in molecule1 is assigned to
                     * the optAssignment[i]-th side chain in molecule2:
                     */
                    double cost = 0;
                    for (int i = 0; i < optAssignment.length; i++) {
                        cost = cost + costmatrix[i][optAssignment[i]];
                    }

                    if (cost < minCost) {
                        minCost = cost;
                    }
                }
                // minCost is equals to the minimum cost of an perfect bipartite matching
                // between nodesMol1 and nodesMol2 for all isomorphisms between molecule1 and molecule2:
                this.distance = minCost;
                return this.distance;
            } else {
                /* nodesMol1 and nodesMol2 are empty because there are no occurrences of
                 * the side chains contained in this.sideChainList. That means that
                 * nothing was removed and both molecules don´t contain any '*' atom.
                 * Nevertheless, molecule1 and molecule2 are ismorphic and equal.
                 * This case is considered because an object of HungarianAlgorithm
                 * would throw a IllegalArgumentException.
                 */
                this.distance = 0;
                return this.distance;
            }
        }
    }


    /**
     * Returns the calculated distance between {@link #molecule1} and {@link #molecule2}.<br>
     * If the distance hasn't been calculated yet, {@link #compare()} will be called.
     *
     * @return the distance between {@link #molecule1} and {@link #molecule2}. This distance is equal to
     * {@link Double#MAX_VALUE} if the given assumption is not true.
     */
    public double getScore() {
        if (this.distance < 0) {
            return this.compare();
        } else {
            return this.distance;
        }
    }


    /**
     * Returns if {@link #molecule1} and {@link #molecule2} have the same molecular formula.
     *
     * @return true if {@link #molecule1} and {@link #molecule2} have the same molecular formula, otherwise false
     */
    private boolean sameMolecularFormula() {
        //Erzeuge als erstes eine Summenformel für molecule1:
        MolecularFormula molForm1 = new MolecularFormula();

        for (IAtom atom : this.molecule1.atoms()) {
            molForm1.addIsotope(atom);
        }

        //Erzeuge eine Summenformel für molecule2:
        MolecularFormula molForm2 = new MolecularFormula();

        for (IAtom atom : this.molecule2.atoms()) {
            molForm2.addIsotope(atom);
        }

        return MolecularFormulaManipulator.compare(molForm1, molForm2);
    }

    /**
     * Returns the {@link AtomContainerE} {@link #molecule1}.
     *
     * @return the {@link AtomContainerE} {@link #molecule1}
     */
    public IAtomContainer getFirstMolecule(){
        return this.molecule1;
    }

    /**
     * Returns the {@link AtomContainerE} {@link #molecule2}.
     *
     * @return the {@link AtomContainerE} {@link #molecule2}
     */
    public IAtomContainer getSecondMolecule(){
        return this.molecule2;
    }
}
