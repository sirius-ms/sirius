package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Peak;

import java.util.List;

public interface FTFragment {

    /**
     * @return an immutable list of all children fragments
     */
    public List<? extends FTFragment> getChildren();

    public List<? extends FTLoss<? extends FTFragment>> getOutgoingEdges();

    /**
     * @return an immutable list of all parent fragments or a singleton list for trees
     */
    public List<? extends FTFragment> getParents();

    public List<? extends FTLoss<? extends FTFragment>> getIncomingEdges();

    /**
     * @return the parent node for the tree fragment. null for graphs
     */
    public FTFragment getParent();
    public <T extends FTLoss<? extends FTFragment>> T getIncomingEdge();

    public MolecularFormula getFormula();

    public Peak getPeak();

    public double getRelativePeakIntensity();

    /**
     * @return a list of unique collision energies the peak appears in
     */
    public List<CollisionEnergy> getCollisionEnergies();

}
