package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Arrays;
import java.util.BitSet;

public class GreedyHeuristic extends AbstractHeuristic {

    protected final Loss[] losses;

    public GreedyHeuristic(FGraph graph) {
        super(graph);
        this.losses = graph.losses().toArray(new Loss[graph.numberOfEdges()]);
        Arrays.sort(losses, (u,v)->Double.compare(v.getWeight(),u.getWeight()));
    }
    public FTree solve() {
        final TIntIntHashMap selectedVertices = new TIntIntHashMap(ncolors, 0.75f, -1, -1);
        final BitSet usedColors = new BitSet(ncolors);
        for (int i=0; i < losses.length; ++i) {
            final Fragment target = losses[i].getTarget();
            final int O = target.getColor();
            if (!usedColors.get(O)) {
                final int fout = selectedVertices.get(O);
                if (fout < 0 || fout == target.getVertexId()) {
                    final Fragment source = losses[i].getSource();
                    final int fid = selectedVertices.get(source.getColor());
                    if (fid < 0) {
                        // color is not used yet, so we are free to use this vertex
                        selectedEdges.add(losses[i]);
                        usedColors.set(O); // we are not allowed to use this color again
                        // whenever we want to use the incoming color, we have to use THIS vertex
                        selectedVertices.put(source.getColor(), source.getVertexId());
                        if (fout<0)selectedVertices.put(target.getColor(), target.getVertexId());
                    } else if (fid == source.getVertexId()) {
                        selectedEdges.add(losses[i]);
                        selectedVertices.put(target.getColor(), target.getVertexId());
                        usedColors.set(O); // we are not allowed to use this color again
                    }
                }
            }
        }
        return buildSolution(true);
    }
}
