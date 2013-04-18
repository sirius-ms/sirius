package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GraphFragment extends Fragment {

    final List<Loss> incommingEdges;

    GraphFragment(int index, ScoredMolecularFormula decomposition, ProcessedPeak peak) {
        super(index, decomposition, peak);
        this.incommingEdges = new ArrayList<Loss>();
    }

    void addIncommingEdge(Loss loss) {
        this.incommingEdges.add(loss);
    }

    void removeOutgoingEdge(Loss outgoing) {
        final boolean done = outgoingEdges.remove(outgoing);
        assert done;
    }

    void removeIncommingEdge(Loss incomming) {
        final boolean done = incommingEdges.remove(incomming);
        assert done;
    }

    boolean shouldBeTrimmed() {
        if (!isLeaf()) return false;
        for (Loss l : incommingEdges) {
            if (l.getWeight() > 0) return false;
        }
        return true;
    }

    @Override
    public List<Loss> getIncomingEdges() {
        return Collections.unmodifiableList(incommingEdges);
    }

    @Override
    public List<Fragment> getParents() {
        final Fragment[] parents = new Fragment[incommingEdges.size()];
        for (int i=0; i < incommingEdges.size(); ++i) parents[i] = incommingEdges.get(i).getHead();
        return Arrays.asList(parents);
    }
}
