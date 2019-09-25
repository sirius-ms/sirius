package de.unijena.bioinf.ms.gui.sirius;

import com.google.common.base.Function;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.jjobs.JobStateEvent;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.core.AbstractEDTBean;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.fingerid.FingerIdResultBean;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.IdentificationResults;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;
import de.unijena.bioinf.sirius.SiriusScore;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.regex.Pattern;

/**
 * this is the view for SiriusResultElement.class
 */
public class IdentificationResultBean extends AbstractEDTBean implements Comparable<IdentificationResultBean>, PropertyChangeListener, ResultAnnotation {
    //the results data structure
    private IdentificationResult identificationResult;

    private TreeNode tree; //just a gui tree.

    //todo best hit property change

    // computing state
    private volatile ComputingStatus fingerIdComputeState = ComputingStatus.UNCOMPUTED;
    private volatile ComputingStatus canopusComputeState = ComputingStatus.UNCOMPUTED;


    public IdentificationResultBean(IdentificationResult result) {
        identificationResult = result;
        identificationResult.setAnnotation(IdentificationResultBean.class, this);
        tree = null;
    }

    public IdentificationResult getResult() {
        return identificationResult;
    }

    public void buildTreeVisualization(Function<FTree, TreeNode> builder) {
        this.tree = builder.apply(identificationResult.getResolvedTree());
    }

    public FingerIdResultBean getFingerIdData() {
        return identificationResult.getAnnotation(FingerIdResult.class).getAnnotation(FingerIdResultBean.class);
    }

    public FingerIdResultBean getCanopusData() {
        throw new UnsupportedOperationException();
    }

    public int getRank() {
        return identificationResult.getRank();
    }

    public double getSiriusScore() {
        return identificationResult.getScore(SiriusScore.class);
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
        return identificationResult.getRank() == 1;
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

    @Override
    public int compareTo(IdentificationResultBean o) {
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




    //todo run via propterty change
    private void makeFingerIdData() {
        if (!identificationResult.hasAnnotation(FingerIdResult.class))
            throw new IllegalArgumentException("No FingeridResult present to use as soource for FingerIdData");
        new FingerIdResultBean(identificationResult.getAnnotation(FingerIdResult.class));
    }

    //todo  compute states need to be observable
    private void configureListeners() {
        identificationResult.addAnnotationChangeListener(evt -> {
            if (evt.getPropertyName().equals(DataAnnotation.getIdentifier(FingerIdResult.class)))
                makeFingerIdData();

            firePropertyChange(evt);
        });

        identificationResult.addAnnotationChangeListener(evt -> {
            if (evt.getPropertyName().equals(DataAnnotation.getIdentifier(FingerIdResult.class)))
                makeFingerIdData();

            firePropertyChange(evt);
        });
    }
}
