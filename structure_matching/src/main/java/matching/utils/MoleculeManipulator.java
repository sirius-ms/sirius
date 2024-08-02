package matching.utils;

import matching.datastructures.AtomContainerE;
import matching.datastructures.AtomE;
import matching.datastructures.Pair;
import matching.datastructures.SideChain;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * The class MoleculeManipulator is an utility class that provides several methods to manipulate an
 * AtomContainer.<br>
 * </p>
 * <p>
 * It extends the class {@link AtomContainerManipulator} and
 * provides the method {@link #removeSideChain(AtomContainerE, SideChain)} which allows
 * to remove every occurrence of a specified side chain in a given AtomContainer. For example:
 * {@code Pair<AtomContainerE, ArrayList<AtomE>> pair = MoleculeManipulator.removeSideChain(molecule, sideChain);}
 * {@code molecule = pair.getObject1();}
 * </p>
 *
 */
public class MoleculeManipulator extends AtomContainerManipulator {

    /**
     * <p>
     * This method searches for all occurrences of the specified side chain in the given AtomContainer
     * and replaces them with an atom of symbol '*'.<br>
     * </p>
     * <p>
     * To find all occurrences of the given {@code sideChain}, Breadth-First Search (BFS) is used to verify for each
     * atom of {@code molecule} if this atom is the bridge node of a subgraph which isomorphic to the given side chain;
     * i.e. for each atom, it is validated whether it is an atom belonging to the searched side chain and linking this
     * side chain to rest of the given molecule.<br>
     * </p>
     * <p>Note that the input AtomContainerE may also be changed. Therefore, this method must be applied as follows:</p>
     * <pre>
     *     {@code Pair<AtomContainerE, ArrayList<AtomE>> result = MoleculeManipulator.removeSideChain(molecule, sideChain);}
     *     {@code molecule = result.getObject1();}
     * </pre>
     *
     * @param molecule  the {@link AtomContainerE} in which all occurrences of the given side chain are searched and removed
     * @param sideChain the {@link SideChain} which is searched and removed in the given AtomContainer
     *
     * @return  an object of {@link Pair} which contains the modified AtomContainerE,
     *          in which all occurrences of the given side chain were removed and replaced with an atom of symbol '*',
     *          and an object of {@link ArrayList}, which contains the found bridge nodes
     */
    public static Pair<AtomContainerE, ArrayList<AtomE>> removeSideChain(AtomContainerE molecule, SideChain sideChain){
        ArrayList<AtomE> bridgeNodes = new ArrayList<AtomE>();

        //BREATH-FIRST SEARCH

        //Initialisation:
        ArrayList<AtomE> queue = new ArrayList<AtomE>();

        for(IAtom atom : molecule.atoms()){
            AtomE atomE = (AtomE) atom;
            atomE.setColor(false);
        }

        AtomE startAtom = (AtomE) molecule.getAtom(0);
        startAtom.setColor(true);
        queue.add(startAtom);

        /*
         * Now, all atoms of molecule which can be reached from startAtom are to be processed.
         * For this, every atom has to be sequentially pulled out of queue,
         * which contains only discovered (color = true) but not yet processed atoms.
         * For the processing of an already discovered atom there are two steps:
         *  - First step:   it will be checked if this atom is a bridge node of the searched side chain.
         *                  If it is the case, this side chain will be replaced with an atom of symbol '*'.
         *  - Second step:  it will be iterated through the adjacency list of this atom to look for all
         *                  undiscovered (color = false) atoms. Because these atoms are found now, their color
         *                  will be set to 'true'. These atoms will be added to the queue.
         */
        while(!queue.isEmpty()){

            AtomE atom = queue.remove(0);

            //First step: Check if atom is a bridge node of the searched side chain
            Object[] results = isBridgeNodeOfSideChain(molecule, atom, sideChain, queue);
            boolean isBridgeNode = (Boolean) results[0];
            molecule = (AtomContainerE) results[1];
            queue = (ArrayList<AtomE>) results[2];

            if(isBridgeNode){
                atom.setSymbol("*");
                atom.setAtomicNumber(0);
                atom.setFormalCharge(0);
                bridgeNodes.add(atom);
            }

            //Second step: Iterate trough the adjacency list of atom and find all adjacent atoms with color = false
            List<IAtom> adjacentAtoms = molecule.getConnectedAtomsList(atom);

            for(IAtom adjAtom : adjacentAtoms){
                AtomE adjAtomE = (AtomE) adjAtom;

                if(!adjAtomE.getColor()){
                    adjAtomE.setColor(true);
                    queue.add(adjAtomE);
                }
            }
        }

        return new Pair<AtomContainerE, ArrayList<AtomE>>(molecule, bridgeNodes);
    }

    /**
     * <p>
     * Checks if the given atom {@code node} contained in {@code molecule} is the bridge node of a subgraph which is
     * isomorphic to the given side chain;
     * i.e. checks if {@code node} is equivalent to the bridge node of the given {@code sideChain}. <br>
     * </p>
     * <p>
     * This method calls {@link #isEquivalent(AtomContainerE, AtomE, SideChain, AtomE, ArrayList, int)},
     * if {@code node} has the same chemical symbol and formal charge as
     * {@link SideChain#bridgeNode sideChain.getBridgeNode()}.
     * If isEquivalent returns true, {@code node} is the bridge node of the searched side chain and
     * all atoms of this side chain will be removed - except {@code node}.
     * </p>
     *
     * @param molecule  the {@link AtomContainerE} which contains the atom {@code node}
     * @param node      the {@link AtomE} contained in {@code molecule} and checked if it is equivalent
     *                  to {@link SideChain#bridgeNode sideChain.getBridgeNode()}
     * @param sideChain the {@link SideChain} which is searched (and removed) in {@code molecule}
     * @param queue     the {@link ArrayList} which contains the atoms of {@code molecule}
     *                  which have been already discovered during breath-first search but have not been processed yet
     *
     * @return  an array of type {@link Object} with components:
     *          <ul>
     *              <li>first component: a boolean of value 'true' true if {@code node} is equivalent to
     *                  {@link SideChain#bridgeNode side sideChain.getBridgeNode()}, otherwise 'false'</li>
     *              <li>second component: the possibly changed {@code molecule} of class {@link AtomContainerE}</li>
     *              <li>third component: the possibly changed {@code queue} of type {@link ArrayList}</li>
     *          </ul>
     */
    private static Object[] isBridgeNodeOfSideChain(AtomContainerE molecule, AtomE node, SideChain sideChain, ArrayList<AtomE> queue){
        //Check if node and sideChain.getBridgeNode() have the same label:
        if(!sameChemicalSymbolAndCharge(node, sideChain.getBridgeNode())){
            return new Object[]{false, molecule, queue};
        }else{
            SideChain copySideChain = new SideChain(new AtomContainerE(sideChain.getSideChain()), sideChain.getBridgeNode());
            Object[] results = isEquivalent(molecule, node, copySideChain, copySideChain.getBridgeNode(), queue, 0);
            return new Object[]{results[0], results[1], results[2]};
        }
    }

    /**
     * <p>
     * This method checks whether the atom {@code nodeM} corresponds to
     * the atom {@code nodeSC} in terms of the context of the searched side chain.<br>
     * </p>
     * <p>
     * The atoms {@code nodeM} and {@code nodeSC} are equivalent if and only if
     * both atoms have the same chemical symbol, if they are connected to the same (unmarked) subgraphs
     * which have not been processed yet, and if both atoms contain the same marked nodes regarding to their
     * {@link AtomE#depth}.
     * </p>
     *
     * @param molecule  the {@link AtomContainerE} which contains the atom {@code nodeM}
     * @param nodeM     the {@link AtomE} contained in {@code molecule} and is compared with the atom {@code nodeSC} to
     *                  check whether it is equivalent to {@code nodeSC} or not
     * @param sideChain the {@link SideChain} which contains {@code nodeSC}
     * @param nodeSC    the {@link AtomE} contained in {@code sideChain} and is compared with the atom {@code nodeM} to
     *                  check whether it is equivalent to {@code nodeM} or nor
     * @param queue     the {@link ArrayList} which contains the atoms of {@code molecule}
     *                  which have been already discovered during breath-first search but have not been processed yet
     * @param depth     the depth of the recursive call
     *
     * @return  an array of type {@link Object} with components:
     *          <ul>
     *              <li>first component: a boolean of value 'true' if {@code nodeM} is equivalent to {@code nodeSC},
     *              otherwise 'false'</li>
     *              <li>second component: the {@link AtomContainerE} {@code molecule} whose unmarked subgraphs
     *              connected to {@code nodeM} were removed if both compared atoms are equivalent,
     *              otherwise the unchanged {@code molecule}</li>
     *              <li>third component: the possibly adjusted {@link ArrayList} {@code queue}</li>
     *              <li>fourth component: the {@link SideChain} {@code sideChain} whose unmarked subgraphs
     *              connected to {@code nodeSC} were removed if both compared atoms are equivalent,
     *              otherwise the unchanged {@code sideChain}</li>
     *          </ul>
     */
    private static Object[] isEquivalent(AtomContainerE molecule, AtomE nodeM,
                                 SideChain sideChain, AtomE nodeSC, ArrayList<AtomE> queue, int depth){
        // Initialisation:
        // Create a copy for molecule, sideChain and queue:
        AtomContainerE oldMolecule = new AtomContainerE(molecule);
        SideChain oldSideChain = new SideChain(new AtomContainerE(sideChain.getSideChain()), sideChain.getBridgeNode());
        ArrayList<AtomE> oldQueue = new ArrayList<AtomE>(queue);

        nodeM.setMarked(true);
        nodeSC.setMarked(true);

        nodeM.setDepth(depth);
        nodeSC.setDepth(depth);

        /* Iteration:
         * Now, it has to be checked if nodeM and nodeSC are equivalent. These both nodes (or atoms) are equivalent,
         * if and only if both nodes have the same label and all subgraphs, which are adjacent to nodeSC, are equivalent
         * to the subgraphs, which are adjacent to nodeM.
         * Because of this, it has to be verified in the following iteration if each node
         * contained in the adjacency list of nodeSC has its equivalent in the adjacency list of nodeM.
         * If this is the case, it is not permissible for further nodes to be found in the nodeM's adjacency list -
         * except nodes with depth = 0.
         */
        List<IAtom> adjNodesOfNodeSC = sideChain.getSideChain().getConnectedAtomsList(nodeSC);

        for(IAtom node1 : adjNodesOfNodeSC){
            AtomE adjNodeSC = (AtomE) node1;
            if(!adjNodeSC.getMarked() && sideChain.getSideChain().contains(adjNodeSC)){

                /* Create a list of all nodes contained in the adjacency list of nodeM
                 * which are possibly equivalent to adjNodeSC - list of all candidates.
                 * Iterate through the adjacency list of nodeM and search for all nodes that have the same label and are
                 * linked by a same labeled bond.
                 */
                ArrayList<AtomE> candidates = new ArrayList<AtomE>();
                List<IAtom> adjNodesOfNodeM = molecule.getConnectedAtomsList(nodeM);

                for(IAtom node2 : adjNodesOfNodeM){
                    AtomE adjNodeM = (AtomE) node2;
                    if(!adjNodeM.getMarked()){

                        IBond bondM = molecule.getBond(nodeM, adjNodeM);
                        IBond bondSC = sideChain.getSideChain().getBond(nodeSC, adjNodeSC);

                        if(sameChemicalSymbolAndCharge(adjNodeM, adjNodeSC) && sameBondType(bondM, bondSC)){
                            candidates.add(adjNodeM);
                        }
                    }
                }

                /*
                 * Now we iterate through "candidates" and search for a node which is equivalent to adjNodeSC.
                 * If an equivalent node is found, this node will be removed.
                 * If there is no equivalent node, then nodeSC and nodeM are not equivalent and 'false' will be returned.
                 */
                boolean bool = false;

                while(!candidates.isEmpty() && !bool){
                    AtomE candidate = candidates.remove(0);

                    Object[] results = isEquivalent(molecule, candidate, sideChain, adjNodeSC, queue, depth+1);
                    boolean isEquivalent = (Boolean) results[0];
                    molecule = (AtomContainerE) results[1];
                    queue = (ArrayList<AtomE>) results[2];
                    sideChain = (SideChain) results[3];

                    if(isEquivalent){
                        molecule.removeAtom(candidate);
                        queue.remove(candidate);
                        sideChain.getSideChain().removeAtom(adjNodeSC);
                        bool = true;
                    }
                }

                if(!bool){
                    nodeM.setMarked(false);
                    nodeSC.setMarked(false);
                    return new Object[]{false, oldMolecule, oldQueue, oldSideChain};
                }
            }
        }

        /* We know: For each unmarked node contained in the adjacency list of nodeSC,
         * there exist an equivalent unmarked node in the adjacency list of nodeM.
         * These nodes were removed out of molecule and sideChain.
         * Now, we have to check if there is no further unmarked node in the adjacency list of nodeM -
         * except case: depth = 0 - and if the marked nodes linked to nodeM are equivalent to
         * the marked nodes linked to nodeSC.
         */
        nodeM.setMarked(false);
        nodeSC.setMarked(false);

        List<IAtom> adjNodesOfNodeM = molecule.getConnectedAtomsList(nodeM);
        adjNodesOfNodeSC = sideChain.getSideChain().getConnectedAtomsList(nodeSC);

        if(depth == 0){
            if(adjNodesOfNodeM.size() == 1){
                return new Object[]{true, molecule, queue, sideChain};
            }else{
                return new Object[]{false, oldMolecule, oldQueue, oldSideChain};
            }
        }else{
            if(adjNodesOfNodeM.size() == adjNodesOfNodeSC.size()){

                //Compare adjNodesOfNodeM and adjNodesOfNodeSC:
                for(int i = 0; i < adjNodesOfNodeM.size(); i++){
                    AtomE adjMarkdNodeM = (AtomE) adjNodesOfNodeM.get(i);

                    if(!adjMarkdNodeM.getMarked()){
                        return new Object[]{false, oldMolecule, oldQueue, oldSideChain};
                    }

                    for(int j = 0; j < adjNodesOfNodeSC.size(); j++){
                        AtomE adjMrkdNodeSC = (AtomE) adjNodesOfNodeSC.get(j);

                        if(adjMarkdNodeM.getDepth() == adjMrkdNodeSC.getDepth()){
                            IBond bondM = molecule.getBond(nodeM, adjMarkdNodeM);
                            IBond bondSC = sideChain.getSideChain().getBond(nodeSC, adjMrkdNodeSC);

                            if(sameBondType(bondM, bondSC)) {
                                adjNodesOfNodeSC.remove(j);
                                break;
                            }else{
                                return new Object[]{false, oldMolecule, oldQueue, oldSideChain};
                            }
                        }
                    }
                }

                if(adjNodesOfNodeSC.isEmpty()){
                    return new Object[]{true, molecule, queue, sideChain};
                }else{
                    return new Object[]{false, oldMolecule, oldQueue, oldSideChain};
                }
            }else{
                return new Object[]{false, oldMolecule, oldQueue, oldSideChain};
            }
        }
    }

    /**
     * Returns 'true' if {@code atom1} and {@code atom2} have the same chemical symbol and the same formal charge.
     *
     * @param atom1 the {@link IAtom} whose chemical symbol and formal charge are compared to those of {@code atom2}
     * @param atom2 the {@link IAtom} whose chemical symbol and formal charge are compared to those of {@code atom1}
     * @return true if the given atoms have the same chemical symbol and formal charge
     */
    private static boolean sameChemicalSymbolAndCharge(IAtom atom1, IAtom atom2){
        return (atom1.getSymbol().equals(atom2.getSymbol()) && atom1.getFormalCharge().equals(atom2.getFormalCharge()));
    }

    /**
     * Returns 'true' if {@code bond1} and {@code bond2} have the same bond order or both bonds are aromatic.
     *
     * @param bond1 the {@link IBond} which is compared to {@code bond1}
     * @param bond2 he {@link IBond} whcih is compared to {@code bond2}
     * @return true if both bonds have the same bond order or if both bonds are aromatic
     */
    private static boolean sameBondType(IBond bond1, IBond bond2){
        return (bond1.getOrder() == bond2.getOrder()) || (bond1.isAromatic() && bond2.isAromatic());
    }

}
