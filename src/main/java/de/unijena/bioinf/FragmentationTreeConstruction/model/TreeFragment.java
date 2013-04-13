package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;

import java.util.Collections;
import java.util.List;

/**
 * @author Kai Dührkop
 */
public class TreeFragment extends Fragment {

    private Loss parentLoss;

    TreeFragment(int index, TreeFragment parent, ScoredMolecularFormula ownDecomposition, Loss loss, ProcessedPeak peak) {
        super(index, ownDecomposition, peak);
        setColor(loss == null ? 0 : loss.getTail().getColor());
        this.parentLoss = (parent == null) ? null : new Loss(parent, this, loss.getLoss(), loss.getWeight());
    }

    public Loss getParentEdge() {
        return parentLoss;
    }

    void setParentEdge(Loss parentLoss) {
        this.parentLoss = parentLoss;
    }

    @Override
    public List<Loss> getOutgoingEdges() {
        return outgoingEdges;
    }

    @Override
    public List<Loss> getIncomingEdges() {
        return parentLoss == null ? Collections.<Loss>emptyList() : Collections.singletonList(parentLoss);
    }

    @Override
    public List<Fragment> getParents() {
        return parentLoss == null ? Collections.<Fragment>emptyList() : Collections.singletonList(parentLoss.getHead());
    }

    public boolean isRoot() {
        return parentLoss == null;
    }
}
