package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

/**
 *  Utility class to edit a CombinatorialGraph object.
 */
public class CombinatorialGraphManipulator {

    private static BitSet toBitSet(int number){
        BitSet bitset = new BitSet();
        int currentNumber = number;
        int idx = 0;

        while(currentNumber > 0){
            int newNumber = currentNumber / 2;
            int rest = currentNumber - 2*newNumber;
            if(rest == 1) bitset.set(idx);
            currentNumber = newNumber;
            idx++;
        }
        return bitset;
    }

    public static void addTerminalNodes(CombinatorialGraph graph, CombinatorialFragmenterScoring scoring, FTree fTree){
        MolecularGraph molecule = graph.root.fragment.parent;

        HashMap<String, ArrayList<CombinatorialNode>> mf2Node = new HashMap<>();
        ArrayList<CombinatorialNode> lst = new ArrayList<>();
        lst.add(graph.root);
        mf2Node.put(graph.root.fragment.getFormula().toString(), lst);

        for(CombinatorialNode node : graph.nodes){
            String mf = node.fragment.getFormula().toString();
            if(mf2Node.get(mf) == null){
                lst = new ArrayList<>();
                lst.add(node);
                mf2Node.put(mf, lst);
            }else{
                mf2Node.get(mf).add(node);
            }
        }

        int count = 0;
        for(Fragment ftFrag : fTree){
            String mf = ftFrag.getFormula().toString();
            lst = mf2Node.get(mf);
            if(lst != null){
                BitSet bitSet = toBitSet(count);
                bitSet.set(molecule.natoms);
                CombinatorialFragment terminal = new CombinatorialFragment(molecule,bitSet,new BitSet());

                for(CombinatorialNode node : lst){
                    graph.addReturnAlways(node,terminal,null,null,scoring,null);
                }
                count++;
            }
        }
    }

    public static double[][] calculateNodeDistances(CombinatorialGraph graph){
        return null;
    }

}
