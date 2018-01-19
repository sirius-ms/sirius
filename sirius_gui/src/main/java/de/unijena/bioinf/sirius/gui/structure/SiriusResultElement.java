package de.unijena.bioinf.sirius.gui.structure;

import com.google.common.base.Function;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.Compound;
import de.unijena.bioinf.fingerid.FingerIdData;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * this is the view for SiriusResultElement.class
 */
public class SiriusResultElement extends AbstractEDTBean implements Comparable<SiriusResultElement>, PropertyChangeListener {
    //the results data structure
    private IdentificationResult resultElement;

    private boolean bestHit = false;
    private TreeNode tree; //just a gui tree.

    protected volatile FingerIdData fingerIdData;
    private volatile ComputingStatus fingerIdComputeState = ComputingStatus.UNCOMPUTED;

    public SiriusResultElement(IdentificationResult result) {
        this.tree = null;
        this.resultElement = result;
        this.fingerIdData = null;
    }

    public IdentificationResult getResult() {
        return resultElement;
    }

    public void buildTreeVisualization(Function<FTree, TreeNode> builder) {
        this.tree = builder.apply(resultElement.getResolvedTree());
    }

    public FingerIdData getFingerIdData() {
        return fingerIdData;
    }

    public void setFingerIdData(FingerIdData fingerIdData) {
        this.fingerIdData = fingerIdData;
        List<Scored<FingerprintCandidate>> candidates = getCandidates(fingerIdData);
        resultElement.setAnnotation(FingerIdResult.class, new FingerIdResult(candidates, 0d, fingerIdData.getPlatts(), null)); //TODO: implement
    }

    private List<Scored<FingerprintCandidate>> getCandidates(FingerIdData fingerIdData) {
        //todo again duplicate data!
        final Compound[] compounds = fingerIdData.getCompounds();
        final double[] scores = fingerIdData.getScores();
        List<Scored<FingerprintCandidate>> candidates = new ArrayList<>(compounds.length);
        for (int i = 0; i < compounds.length; i++) {
            final FingerprintCandidate candidate = compounds[i].asFingerprintCandidate();
            candidates.add(new Scored<>(candidate, scores[i]));
        }
        return candidates;
    }

    public int getRank() {
        return resultElement.getRank();
    }

    public double getScore() {
        return resultElement.getScore();
    }

    public MolecularFormula getMolecularFormula() {
        return resultElement.getMolecularFormula();
    }

    private final static Pattern pat = Pattern.compile("^\\s*\\[\\s*M\\s*|\\s*\\]\\s*\\d*\\s*[\\+\\-]\\s*$");

    public String getFormulaAndIonText() {
        final PrecursorIonType ionType = resultElement.getRawTree().getAnnotationOrThrow(PrecursorIonType.class);
        final MolecularFormula mf = resultElement.getMolecularFormula();
        String niceName = ionType.toString();
        niceName = pat.matcher(niceName).replaceAll("");
        if (ionType.isIonizationUnknown()) {
            return mf.toString();
        } else {
            return mf.toString() + " " + niceName;
        }
    }

    public int getCharge() {
        return resultElement.getResolvedTree().getAnnotationOrThrow(PrecursorIonType.class).getCharge();
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

    public void setFingerIdComputeState(final ComputingStatus fingerIdComputeState) {
        final ComputingStatus old = this.fingerIdComputeState;
        this.fingerIdComputeState = fingerIdComputeState;
        firePropertyChange("finger_compute_state", old, this.fingerIdComputeState);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        //todo handle fingeridjobs in the future
    }
}
