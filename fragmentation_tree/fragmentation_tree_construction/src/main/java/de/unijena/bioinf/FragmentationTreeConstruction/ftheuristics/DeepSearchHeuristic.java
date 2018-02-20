package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;

import java.util.ArrayList;
import java.util.BitSet;

public class DeepSearchHeuristic extends AbstractHeuristic {

    private final BitSet usedColors;
    private final ArrayList<Fragment> stack;

    public DeepSearchHeuristic(FGraph graph) {
        super(graph);
        this.usedColors = new BitSet(ncolors);
        this.stack = new ArrayList<>(ncolors);
        stack.add(graph.getRoot());
    }

    @Override
    public FTree solve() {
        compute();
        return buildSolution(true);
    }

    private void compute() {
        while (!stack.isEmpty()) {
            final int i = stack.size()-1;
            final Fragment u = stack.get(i);
            double maxScore = Double.NEGATIVE_INFINITY;
            int maxIndex = -1;
            for (int index = 0; index < u.getOutDegree(); ++index) {
                final Fragment v = u.getChildren(index);
                if (!usedColors.get(v.getColor())) {
                    final double score = u.getOutgoingEdge(index).getWeight();
                    if (score > maxScore) {
                        maxIndex = index;
                        maxScore = score;
                    }
                }
            }
            if (maxIndex>=0) {
                selectedEdges.add(u.getOutgoingEdge(maxIndex));
                final Fragment v = u.getChildren(maxIndex);
                usedColors.set(v.getColor());
                stack.add(v);
            } else {
                stack.remove(i);
            }
        }
    }
}
