package de.unijena.bioinf.ms.gui.sirius;

import com.google.common.base.Function;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.jjobs.JobStateEvent;
import de.unijena.bioinf.ms.gui.fingerid.FingerIdResultBean;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.core.AbstractEDTBean;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.regex.Pattern;

/**
 * this is the view for SiriusResultElement.class
 */
public class SiriusResultElement extends AbstractEDTBean implements Comparable<SiriusResultElement>, PropertyChangeListener {
    //the results data structure
    private IdentificationResult identificationResult;

    private boolean bestHit = false;
    private TreeNode tree; //just a gui tree.

    protected volatile FingerIdResultBean fingerIdData;

    private volatile ComputingStatus fingerIdComputeState = ComputingStatus.UNCOMPUTED;

    public SiriusResultElement(IdentificationResult result) {
        this.tree = null;
        this.identificationResult = result;
        this.fingerIdData = null;
    }

    public IdentificationResult getResult() {
        return identificationResult;
    }

    public void buildTreeVisualization(Function<FTree, TreeNode> builder) {
        this.tree = builder.apply(identificationResult.getResolvedTree());
    }

    public FingerIdResultBean getFingerIdData() {
        return fingerIdData;
    }

    public void setFingerIdData(FingerIdResult resultToWrap) {
        this.identificationResult.setAnnotation(FingerIdResult.class, resultToWrap);
        makeFingerIdData();
    }

    public void makeFingerIdData() {
        if (!identificationResult.hasAnnotation(FingerIdResult.class))
            throw new IllegalArgumentException("No FingeridResult present to use as soource for FingerIdData");
        this.fingerIdData = new FingerIdResultBean(identificationResult.getAnnotation(FingerIdResult.class));

    }

    public int getRank() {
        return identificationResult.getRank();
    }

    public double getScore() {
        return identificationResult.getScore();
    }

    public PrecursorIonType getPrecursorIonType() {
        return identificationResult.getPrecursorIonType();
    }

    public MolecularFormula getMolecularFormula() {
        return identificationResult.getMolecularFormula();
    }

    private final static Pattern pat = Pattern.compile("^\\s*\\[\\s*M\\s*|\\s*\\]\\s*\\d*\\s*[\\+\\-]\\s*$");

    public String getFormulaAndIonText() {
        final PrecursorIonType ionType = identificationResult.getPrecursorIonType();
        final MolecularFormula mf = identificationResult.getMolecularFormula();
        String niceName = ionType.toString();
        niceName = pat.matcher(niceName).replaceAll("");
        if (ionType.isIonizationUnknown()) {
            return mf.toString();
        } else {
            return mf.toString() + " " + niceName;
        }
    }

    public int getCharge() {
        return identificationResult.getResolvedTree().getAnnotationOrThrow(PrecursorIonType.class).getCharge();
    }

    public TreeNode getTreeVisualization() {
        return tree;
    }

    public boolean isBestHit() {
        return bestHit;
    }

    public void setBestHit(final boolean bestHit) {
        final boolean old = this.bestHit;
        this.bestHit = bestHit;
        firePropertyChange("best_hit", old, bestHit);
    }

    @Override
    public int compareTo(SiriusResultElement o) {
        return Integer.compare(getRank(), o.getRank());
    }

    public ComputingStatus getFingerIdComputeState() {
        return fingerIdComputeState;
    }

    public synchronized void setFingerIdComputeState(final ComputingStatus fingerIdComputeState) {
        final ComputingStatus old = this.fingerIdComputeState;
        this.fingerIdComputeState = fingerIdComputeState;
        firePropertyChange("finger_compute_state", old, this.fingerIdComputeState);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt instanceof JobStateEvent) {
            JobStateEvent e = (JobStateEvent) evt;
            setFingerIdComputeState(Jobs.getComputingState(e.getNewValue()));
        }
    }

    public double getExplainedPeaksRatio() {
        return identificationResult.getExplainedPeaksRatio();
    }

    public double getNumOfExplainedPeaks() {
        return identificationResult.getNumOfExplainedPeaks();
    }

    public double getExplainedIntensityRatio() {
        return identificationResult.getExplainedIntensityRatio();
    }

    public double getNumberOfExplainablePeaks() {
        return identificationResult.getNumberOfExplainablePeaks();
    }
}
