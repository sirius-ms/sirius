package de.unijena.bioinf.fragmenter;

import java.util.*;

/**
 * This fragmenter will create fragments step by step, beginning with fragments with higher score
 */
public class PriorizedFragmenter extends CombinatorialFragmenter {

    protected CombinatorialGraph graph;
    protected PriorityQueue<CombinatorialNode> nodes;

    protected CombinatorialNode currentFragment;

    // 0 = reject, 2 = acceptForFragmentation
    protected byte state;

    public PriorizedFragmenter(MolecularGraph molecularGraph, CombinatorialFragmenterScoring scoring) {
        super(molecularGraph, scoring);
        this.graph = new CombinatorialGraph(molecularGraph);
        this.nodes = new PriorityQueue<>((o1, o2) -> Float.compare(o2.totalScore,o1.totalScore));
        this.currentFragment = graph.root;
        this.state = 2;
    }

    public CombinatorialNode currentFragment() {
        return currentFragment;
    }

    public CombinatorialNode nextFragment() {
        // fragment node
        boolean[] updateFlag = new boolean[1];
        if (state>=2 && currentFragment.state==0) {
            currentFragment.state=1; // never fragment it again
            List<CombinatorialFragment> fragments = cutAllBonds(currentFragment.fragment, (parent, bonds, fragments1) -> {
                for (CombinatorialFragment f : fragments1) {
                    CombinatorialNode w = graph.addReturnAlways(currentFragment,f,bonds[0], bonds.length>1 ? bonds[1] : null, scoring,updateFlag);
                    if (updateFlag[0]) nodes.offer(w);
                }
            });
        }
        currentFragment = nodes.poll();
        state=0;
        return currentFragment;
    }

    /**
     * adds the current fragment into the fragmentation graph and put it back into the
     * queue to allow for further fragmentation
     */
    public void acceptFragmentForFragmentation() {
        state = 2;
    }

    public CombinatorialGraph createCombinatorialFragmentationGraph(CombinatorialFragmenter.Callback2 callback) {
        boolean[] updateFlag = new boolean[1];
        int nnodes=0, nedges=0;
        while (true) {
            // fragment node
            if (currentFragment.state==0) {
                currentFragment.state=1; // never fragment it again
                if (callback.cut(currentFragment, nnodes, nedges)) {
                    List<CombinatorialFragment> fragments = cutAllBonds(currentFragment.fragment, (parent, bonds, fragments1) -> {
                        for (CombinatorialFragment f : fragments1) {
                            CombinatorialNode w = graph.addReturnAlways(currentFragment, f, bonds[0], bonds.length > 1 ? bonds[1] : null, scoring, updateFlag);
                            if (updateFlag[0]) nodes.offer(w);
                        }
                    });
                }
            }
            currentFragment = nodes.poll();
            if (currentFragment==null) return graph;
        }
    }

    /*
    public CombinatorialGraph createCombinatorialFragmentationGraph(CombinatorialFragmenter.Callback2 callback) {
        ArrayDeque<CombinatorialNode> nodes = new ArrayDeque<>();
        nodes.addLast(graph.root);
        final int[] counting = new int[]{1,0};
        while (!nodes.isEmpty()) {
            CombinatorialNode n = nodes.pollFirst();
            List<CombinatorialFragment> fragments = cutAllBonds(n.fragment, (parent, bonds, fragments1) -> {
                for (CombinatorialFragment f : fragments1) {
                    ++counting[1];
                    CombinatorialNode w = graph.addReturnNovel(n,f,bonds[0], bonds.length>1 ? bonds[1] : null,scoring);
                    if (w!=null) {
                        ++counting[0];
                        if (callback.cut(w,counting[0],counting[1])) nodes.addLast(w);
                    }
                }
            });
        }
        return graph;
    }

 */
}
