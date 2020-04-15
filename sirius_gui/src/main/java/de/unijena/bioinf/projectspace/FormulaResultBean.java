package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.FBCandidateFingerprints;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * this is the view for SiriusResultElement.class
 */
public class FormulaResultBean implements SiriusPCS, Comparable<FormulaResultBean>, DataAnnotation {
    private final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this, true);

    @Override
    public HiddenChangeSupport pcs() {
        return pcs;
    }

    //the results data structure
    private final FormulaResultId fid;
    private final InstanceBean parent;

    private int rank;

    private List<ContainerListener.Defined> listeners = null;

    protected FormulaResultBean(int rank) {
        this.rank = rank;
        fid = null;
        parent = null;
    }

    public FormulaResultBean(FormulaResultId fid, InstanceBean parent, int rank) {
        this.fid = fid;
        this.parent = parent;
        this.rank = rank;
    }

    private List<ContainerListener.Defined> configureListeners() {
        List<ContainerListener.Defined> listeners = new ArrayList<>(5);
        //this is used to detect a new tree as well as a new zodiac score
        listeners.add(parent.projectSpace().defineFormulaResultListener().onUpdate().onlyFor(FormulaScoring.class).
                thenDo((event -> {
                    if (!event.id.equals(fid))
                        return;
                    FormulaScoring fScores = (FormulaScoring) event.getAffectedComponent(FormulaScoring.class).orElse(null);
                    pcs.firePropertyChange("formulaResult.formulaScore", null, fScores);
                })));

        listeners.add(parent.projectSpace().defineFormulaResultListener().onUpdate().onlyFor(FTree.class).
                thenDo((event -> {
                    if (!event.id.equals(fid))
                        return;
                    FTree ftree = (FTree) event.getAffectedComponent(FTree.class).orElse(null);
                    pcs.firePropertyChange("formulaResult.ftree", null, ftree);
                })));

        listeners.add(parent.projectSpace().defineFormulaResultListener().onUpdate().onlyFor(FBCandidates.class).
                thenDo((event -> {
                    if (!event.id.equals(fid))
                        return;
                    FingerprintResult fpRes = (FingerprintResult) event.getAffectedComponent(FingerprintResult.class).orElse(null);
                    pcs.firePropertyChange("formulaResult.fingerprint", null, fpRes);
                })));

        listeners.add(parent.projectSpace().defineFormulaResultListener().onUpdate().onlyFor(FBCandidates.class).
                thenDo((event -> {
                    if (!event.id.equals(fid))
                        return;
                    FBCandidates fbRes = (FBCandidates) event.getAffectedComponent(FBCandidates.class).orElse(null);
                    pcs.firePropertyChange("formulaResult.fingerid", null, fbRes);
                })));

        listeners.add(parent.projectSpace().defineFormulaResultListener().onUpdate().onlyFor(CanopusResult.class).
                thenDo((event -> {
                    if (!event.id.equals(fid))
                        return;
                    CanopusResult cRes = (CanopusResult) event.getAffectedComponent(CanopusResult.class).orElse(null);
                    pcs.firePropertyChange("formulaResult.canopus", null, cRes);
                })));
        return listeners;
    }

    public void registerProjectSpaceListeners() {
        if (listeners == null)
            listeners = configureListeners();
        listeners.forEach(ContainerListener.Defined::register);

    }

    public void unregisterProjectSpaceListeners() {
        if (listeners == null)
            return;
        listeners.forEach(ContainerListener.Defined::unregister);
    }


    public FormulaResultId getID() {
        return fid;
    }

    @SafeVarargs
    public final FormulaResult getResult(Class<? extends DataAnnotation>... components) {
        parent.addToCache();
        return parent.loadFormulaResult(getID(), components);
    }

    public <T extends FormulaScore> double getScoreValue(Class<T> scoreType) {
        return getScore(scoreType).orElse(FormulaScore.NA(scoreType)).score();
    }

    public <T extends FormulaScore> Optional<T> getScore(final Class<T> scoreType) {
        return getResult(FormulaScoring.class).getAnnotation(FormulaScoring.class).flatMap(it -> it.getAnnotation(scoreType));
    }

    public Optional<FTree> getFragTree(){
        return getResult(FTree.class).getAnnotation(FTree.class);
    }


    public Optional<FingerprintResult> getFingerprintResult(){
        return getResult(FingerprintResult.class).getAnnotation(FingerprintResult.class);
    }
    public Optional<FBCandidates> getFingerIDCandidates(){
        return getResult(FBCandidates.class).getAnnotation(FBCandidates.class);
    }

    public Optional<FBCandidateFingerprints> getFingerIDCandidatesFPs(){
        return getResult(FBCandidateFingerprints.class).getAnnotation(FBCandidateFingerprints.class);
    }


    public Optional<CanopusResult> getCanopusResult(){
        return getResult(CanopusResult.class).getAnnotation(CanopusResult.class);
    }

    //ranking stuff
    public int getRank() {
        return rank;
    }

    public boolean isBestHit() {
        return getRank() == 1;
    } //todo us also fingerid score for marking best hit

    //id based info
    public PrecursorIonType getPrecursorIonType() {
        return getID().getIonType();
    }

    public int getCharge() {
        return getPrecursorIonType().getCharge();
    }

    public MolecularFormula getMolecularFormula() {
        return getID().getMolecularFormula();
    }

    private final static Pattern pat = Pattern.compile("^\\s*\\[\\s*M\\s*|\\s*\\]\\s*\\d*\\s*[\\+\\-]\\s*$");

    public String getFormulaAndIonText() {
        final PrecursorIonType ionType = getPrecursorIonType();
        final MolecularFormula mf = getMolecularFormula();
        String niceName = ionType.toString();
        niceName = pat.matcher(niceName).replaceAll("");
        if (ionType.isIonizationUnknown()) {
            return mf.toString();
        } else {
            return mf.toString() + " " + niceName;
        }
    }

    private Optional<FTreeMetricsHelper> getMetrics() {
        return getResult(FTree.class).getAnnotation(FTree.class).map(FTreeMetricsHelper::new);
    }

    public double getExplainedPeaksRatio() {
        return getMetrics().map(FTreeMetricsHelper::getExplainedPeaksRatio).orElse(Double.NaN);
    }

    public double getNumOfExplainedPeaks() {
        return getMetrics().map(FTreeMetricsHelper::getNumOfExplainedPeaks).map(Integer::doubleValue).orElse(Double.NaN);
    }

    public double getExplainedIntensityRatio() {
        return getMetrics().map(FTreeMetricsHelper::getExplainedIntensityRatio).orElse(Double.NaN);
    }

    public double getNumberOfExplainablePeaks() {
        return getMetrics().map(FTreeMetricsHelper::getNumberOfExplainablePeaks).map(Integer::doubleValue).orElse(Double.NaN);
    }

    public double getMedianMassDevPPM() {
        return getMetrics().map(FTreeMetricsHelper::getMedianMassDeviation).map(Deviation::getPpm).orElse(Double.NaN);
    }
    public double getMedianMassDev() {
        return getMetrics().map(FTreeMetricsHelper::getMedianMassDeviation).map(Deviation::getAbsolute).orElse(Double.NaN);
    }

    @Override
    public int compareTo(FormulaResultBean o) {
        return Integer.compare(getRank(), o.getRank());
    }


}
