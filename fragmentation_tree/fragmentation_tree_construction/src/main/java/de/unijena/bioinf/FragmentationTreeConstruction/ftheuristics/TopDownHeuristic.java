package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;

import java.util.BitSet;

public class TopDownHeuristic extends AbstractHeuristic {

    private final BitSet usedColors;

    public TopDownHeuristic(FGraph graph) {
        super(graph);
        this.usedColors = new BitSet(ncolors);
    }

    @Override
    public FTree solve() {
        compute();
        return buildSolution(true);
    }

    private void compute() {
        Fragment root = graph.getRoot().getChildren(0);
        usedColors.set(root.getColor());
        selectedEdges.add(root.getIncomingEdge());
        Fragment u = root;
        Loss l;
        while ((l=findBestLoss(root))!=null) {
            Fragment v = l.getTarget();
            Loss l2;
            while ((l2=findBestLoss(v))!=null) {
                v = l2.getTarget();
            }
        }
    }

    private Loss findBestLoss(Fragment u) {
        Loss bestLoss = null;
        for (int i=0, n=u.getOutDegree(); i <n; ++i) {
            Loss l = u.getOutgoingEdge(i);
            if (!usedColors.get(l.getTarget().getColor()) && (bestLoss==null || bestLoss.getWeight() < l.getWeight())) {
                bestLoss = l;
            }
        }
        if (bestLoss == null) return null;
        else {
            selectedEdges.add(bestLoss);
            usedColors.set(bestLoss.getTarget().getColor());
            return bestLoss;
        }
    }
}
