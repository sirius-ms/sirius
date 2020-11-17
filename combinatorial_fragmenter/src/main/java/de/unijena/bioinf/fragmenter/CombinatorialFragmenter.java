package de.unijena.bioinf.fragmenter;

import gnu.trove.list.array.TIntArrayList;
import org.openscience.cdk.interfaces.IBond;

import java.util.*;

/**
 * enumerates all structures by disconnecting bonds
 */
public class CombinatorialFragmenter {

    protected final MolecularGraph molecularGraph;

    public CombinatorialFragmenter(MolecularGraph molecularGraph) {
        this.molecularGraph = molecularGraph;
    }

    public interface Callback {
        public void cut(CombinatorialFragment parent, IBond[] bonds, CombinatorialFragment[] fragments);
    }

    public List<CombinatorialFragment> cutAllBonds(CombinatorialFragment fragment, Callback callback) {
        int[] bonds = fragment.bonds().toArray();
        List<CombinatorialFragment> list = new ArrayList<>();
        for (int i=0; i < bonds.length; ++i) {
            final int bond = bonds[i];
            if (fragment.allRingsDisconnected(bond)) {
                final CombinatorialFragment[] combinatorialFragments = cutBond(fragment, bond);
                list.add(combinatorialFragments[0]);
                list.add(combinatorialFragments[1]);
                if (callback!=null) {
                    callback.cut(fragment, new IBond[]{fragment.parent.bonds[bond]}, combinatorialFragments);
                }
            } else {
                int ringId = fragment.getSSSRIfCuttable(bond);
                if (ringId>=0) {
                    for (IBond b : fragment.parent.bondsOfRings[ringId]) {
                        final int bidx = b.getIndex();
                        if (bidx != bond && fragment.stillContains(b) && fragment.getSSSRIfCuttable(bidx)==ringId) {
                            final CombinatorialFragment[] combinatorialFragments =cutRing(fragment, ringId, bond, bidx);
                            list.add(combinatorialFragments[0]);
                            list.add(combinatorialFragments[1]);
                            if (callback!=null) {
                                callback.cut(fragment, new IBond[]{fragment.parent.bonds[bond], b}, combinatorialFragments);
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    public interface Callback2 {
        /**
         * @param node last node which was created
         * @return true, if node should be considered for further fragmentation
         */
        public boolean cut(CombinatorialNode node);
    }

    public CombinatorialGraph createCombinatorialFragmentationGraph(Callback2 callback) {
        CombinatorialGraph graph = new CombinatorialGraph(this.molecularGraph);
        ArrayDeque<CombinatorialNode> nodes = new ArrayDeque<>();
        nodes.addLast(graph.root);
        while (!nodes.isEmpty()) {
            CombinatorialNode n = nodes.pollFirst();
            List<CombinatorialFragment> fragments = cutAllBonds(n.fragment, (parent, bonds, fragments1) -> {
                for (CombinatorialFragment f : fragments1) {
                    CombinatorialNode w = graph.addReturnNovel(n,f,bonds[0], bonds.length>1 ? bonds[1] : null);
                    if (w!=null && callback.cut(w)) nodes.addLast(w);
                }
            });
        }
        return graph;
    }

    public CombinatorialFragment[] cutBond(CombinatorialFragment F, int bondIndex){
        final IBond bond = F.parent.bonds[bondIndex];
        final int u = bond.getAtom(0).getIndex(), v = bond.getAtom(1).getIndex();
        final BitSet bitset = new BitSet(F.parent.natoms);
        bitset.set(u);
        //long bitset = BitsetOps.maskFor(u);
        //final long forbidden = ~F.bitset | BitsetOps.maskFor(v);
        BitSet forbidden = (BitSet) F.bitset.clone();
        forbidden.flip(0,F.parent.natoms);
        forbidden.set(v);
        final int[][] adj = F.parent.getAdjacencyList();
        TIntArrayList stack = new TIntArrayList();
        stack.add(u);
        while (!stack.isEmpty()) {
            int w = stack.removeAt(stack.size()-1);
            int[] nbs = adj[w];
            for (int l=0; l < nbs.length; ++l) {
                if (!forbidden.get(nbs[l]) && !bitset.get(nbs[l])) {
                    bitset.set(nbs[l]);
                    stack.add(nbs[l]);
                }
            }
        }
        final CombinatorialFragment a = new CombinatorialFragment(F.parent, bitset, F.disconnectedRings);
        BitSet nbitset = ((BitSet)F.bitset.clone());
        nbitset.andNot(bitset);
        final CombinatorialFragment b = new CombinatorialFragment(F.parent, nbitset, F.disconnectedRings);
        return new CombinatorialFragment[]{a,b};
    }

    public CombinatorialFragment[] cutRing(CombinatorialFragment F, int ringId, int bond1, int bond2) {
        final int u = F.parent.bonds[bond1].getAtom(0).getIndex();
        BitSet bitset = new BitSet(F.parent.natoms);
        bitset.set(u);
        //long bitset = BitsetOps.maskFor(u);
        final int[][] adj = F.parent.getAdjacencyList();
        final int[][] bondj = F.parent.bondList;
        TIntArrayList stack = new TIntArrayList();
        stack.add(u);
        while (!stack.isEmpty()) {
            int w = stack.removeAt(stack.size()-1);
            int[] nbs = adj[w];
            for (int l=0; l < nbs.length; ++l) {
                if (bondj[w][l]!=bond1 && bondj[w][l]!=bond2 &&  F.bitset.get(nbs[l]) && !bitset.get(nbs[l])) {
                    bitset.set(nbs[l]);
                    stack.add(nbs[l]);
                }
            }
        }
        final BitSet disc = (BitSet)F.disconnectedRings.clone();
        disc.set(ringId);
        final CombinatorialFragment a = new CombinatorialFragment(F.parent, bitset, disc);
        BitSet nbitset = ((BitSet)F.bitset.clone());
        nbitset.andNot(bitset);
        final CombinatorialFragment b = new CombinatorialFragment(F.parent, nbitset, disc);
        return new CombinatorialFragment[]{a,b};
    }
}
