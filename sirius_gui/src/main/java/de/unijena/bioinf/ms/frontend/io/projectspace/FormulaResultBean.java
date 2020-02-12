package de.unijena.bioinf.ms.frontend.io.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import de.unijena.bioinf.projectspace.ContainerListener;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;
import de.unijena.bioinf.sirius.scores.SiriusScore;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * this is the view for SiriusResultElement.class
 */
public class FormulaResultBean implements SiriusPCS, Comparable<FormulaResultBean>, DataAnnotation {
    private final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this, true);
    //todo som unregister listener stategy

    @Override
    public HiddenChangeSupport pcs() {
        return pcs;
    }

    //the results data structure
    private final FormulaResultId fid;
    private final InstanceBean parent;

    private int rank;

    private ContainerListener.Defined scoreListener, fingerBlastListener, fingerprintListener, canopusListener;

    protected FormulaResultBean(int rank) {
        this.rank = rank;
        fid = null;
        parent = null;
    }

    public FormulaResultBean(FormulaResultId fid, InstanceBean parent, int rank) {
        this.fid = fid;
        this.parent = parent;
        this.rank = rank;
        configureListeners();
    }

    //todo  compute states need to be observable
    private void configureListeners() {
        //this is used to detect a new tree as well as a new zodiac score
        scoreListener = parent.projectSpace().defineFormulaResultListener().onUpdate().onlyFor(FormulaScoring.class).
                thenDo((event -> {
                    FormulaScoring fScores = (FormulaScoring) event.getAffectedComponent(FormulaScoring.class).orElse(null);
                    pcs.firePropertyChange("formulaScore", null, fScores);


                    if (event.hasChanged(FTree.class)) {
                        final FTree fTree = (FTree) event.getAffectedComponent(FTree.class).orElse(null);
                        //todo create gui tree here?
                        pcs.firePropertyChange("tree", null, fTree);
                    }
                })).register();

        fingerprintListener = parent.projectSpace().defineFormulaResultListener().onUpdate().onlyFor(FingerprintResult.class).
                thenDo((event -> {
                    FingerprintResult fpRes = (FingerprintResult) event.getAffectedComponent(FingerprintResult.class).orElse(null);
                    pcs.firePropertyChange("fingerprint", null, fpRes);
                })).register();

        fingerBlastListener = parent.projectSpace().defineFormulaResultListener().onUpdate().onlyFor(FingerblastResult.class).
                thenDo((event -> {
                    FingerblastResult fbRes = (FingerblastResult) event.getAffectedComponent(FingerblastResult.class).orElse(null);
                    pcs.firePropertyChange("fingerblast", null, fbRes);
                })).register();

        canopusListener = parent.projectSpace().defineFormulaResultListener().onUpdate().onlyFor(CanopusResult.class).
                thenDo((event -> {
                    CanopusResult cRes = (CanopusResult) event.getAffectedComponent(CanopusResult.class).orElse(null);
                    pcs.firePropertyChange("canopus", null, cRes);
                })).register();
    }



    public FormulaResultId getID() {
        return fid;
    }

    public FormulaResult getResult(Class<? extends DataAnnotation>... components) {
        return parent.loadFormulaResult(getID(), components);
    }

    public FormulaScore getRankingScore() {
        return getResult(FormulaScoring.class).getAnnotationOrThrow(FormulaScoring.class).getAnnotationOrThrow(parent.getID().getRankingScoreType().orElse(SiriusScore.class));
    }

    public double getRankingScoreValue() {
        return getRankingScore().score();
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
    public Optional<FingerblastResult> getFingerblastResult(){
        return getResult(FingerblastResult.class).getAnnotation(FingerblastResult.class);
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
    }

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

    public double getMedianRelMassDev() {
        return getMetrics().map(FTreeMetricsHelper::getMedianMassDeviation).map(Deviation::getPpm).orElse(Double.NaN);
    }
    public double getMedianAbsMassDev() {
        return getMetrics().map(FTreeMetricsHelper::getMedianMassDeviation).map(Deviation::getAbsolute).orElse(Double.NaN);
    }

    @Override
    public int compareTo(FormulaResultBean o) {
        return Integer.compare(getRank(), o.getRank());
    }


}
