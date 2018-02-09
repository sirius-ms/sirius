package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.Tanimoto;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.db.CustomDatabase;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.fingerid.jjobs.FormulaJob;
import de.unijena.bioinf.fingerid.net.WebAPI;
import de.unijena.bioinf.fingeriddb.job.PredictorType;
import de.unijena.bioinf.jjobs.*;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElementConverter;
import de.unijena.bioinf.sirius.logging.TextAreaJJobContainer;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.*;

public class CSIFingerIDComputation {

    protected final CSIPredictor positiveMode, negativeMode;


    public CSIFingerIDComputation() {
        this.positiveMode = new CSIPredictor(PredictorType.CSI_FINGERID_POSITIVE);
        this.negativeMode = new CSIPredictor(PredictorType.CSI_FINGERID_NEGATIVE);
        initialize();
    }

    private void initialize() {
        SwingJJobContainer<Boolean> container = new TextAreaJJobContainer<>(new InitializeCSIFingerID(), "Initialize CSI:FingerID", "CSI:FingerID");
        Jobs.MANAGER.submitSwingJob(container);
    }

    //compute for a single experiment
    public void compute(ExperimentContainer c, SearchableDatabase db) {
        final ArrayList<FingerIdTask> tasks = new ArrayList<>();
        for (SiriusResultElement e : getTopSiriusCandidates(c)) {
            if (e.getCharge() > 0) {
                tasks.add(new FingerIdTask(db, c, e));
            }
        }
        computeAll(tasks);
    }

    //csi fingerid compute all button in main panel
    public void computeAll(List<ExperimentContainer> compounds, SearchableDatabase db) {
        final ArrayList<FingerIdTask> tasks = new ArrayList<>();
        for (ExperimentContainer c : compounds) {
            for (SiriusResultElement e : getTopSiriusCandidates(c)) {
                if (e.getCharge() > 0) {
                    tasks.add(new FingerIdTask(db, c, e));
                }
            }
        }
        computeAll(tasks);
    }

    protected static List<SiriusResultElement> getTopSiriusCandidates(ExperimentContainer container) {
        final ArrayList<SiriusResultElement> elements = new ArrayList<>();
        if (container == null || !container.isComputed() || container.getResults() == null) return elements;
        final SiriusResultElement top = container.getResults().get(0);
        if (top.getResult().getResolvedTree().numberOfEdges() > 0)
            elements.add(top);
        final double threshold = calculateThreshold(top.getScore());
        for (int k = 1; k < container.getResults().size(); ++k) {
            SiriusResultElement e = container.getResults().get(k);
            if (e.getScore() < threshold) break;
            if (e.getResult().getResolvedTree().numberOfEdges() > 0)
                elements.add(e);
        }
        return elements;
    }

    public static double calculateThreshold(double topScore) {
        return Math.max(topScore, 0) - Math.max(5, topScore * 0.25);
    }

    public void computeAll(Collection<FingerIdTask> compounds) {
        for (FingerIdTask task : compounds) {
            SwingJJobContainer<Boolean> container = new TextAreaJJobContainer<>(new FingerIDGUITask(task.experiment, task.result, task.db), task.result.getMolecularFormula().toString(), "Structure Prediction");
            Jobs.MANAGER.submitSwingJob(container);
        }
    }

    /**
     * TODO: shouldn be part of this class
     */
    public List<SearchableDatabase> getAvailableDatabases() {
        final List<SearchableDatabase> db = new ArrayList<>();
        db.add(positiveMode.pubchem);
        db.add(positiveMode.bio);
        db.addAll(CustomDatabase.customDatabases(true));
        return db;
    }

    public SearchableDatabase getBioDb() {
        return positiveMode.bio;
    }

    public SearchableDatabase getPubchemDb() {
        return positiveMode.pubchem;
    }

    public void refreshCacheDir() throws IOException {
        positiveMode.refreshCacheDir();
        negativeMode.refreshCacheDir();

    }


    protected class InitializeCSIFingerID extends BasicJJob<Boolean> {


        public InitializeCSIFingerID() {
            super(JobType.REMOTE);
        }

        @Override
        protected Boolean compute() throws Exception {
            positiveMode.initialize();
            negativeMode.initialize();
            return true;
        }

    }

    protected class FingerIDGUITask extends BasicMasterJJob<Boolean> {
        public final ExperimentContainer container;
        public final SiriusResultElement originalResultElement;
        public final List<SiriusResultElement> addedResultElements;
        public final SearchableDatabase db;

        public FingerIDGUITask(ExperimentContainer container, SiriusResultElement originalResultElement, SearchableDatabase db) {
            super(JobType.WEBSERVICE);
            this.container = container;
            this.originalResultElement = originalResultElement;
            this.addedResultElements = new ArrayList<>();
            this.db = db;
        }

        public String toString() {
            return "Predict " + originalResultElement.getResult().getMolecularFormula();
        }

        @Override
        protected Boolean compute() throws Exception {
            LOG().info("Predicting structure for: " + originalResultElement.getMolecularFormula().toString());
            final PrecursorIonType origIonType = originalResultElement.getResult().getPrecursorIonType();


            // step 1: download molecular formulas and predict fingerprints
            LOG().info("downloading molecular formulas and predicting fingerprints");
            final PossibleAdducts pa = container.getMs2Experiment().getAnnotation(PossibleAdducts.class, new PossibleAdducts());
            pa.addAdduct(origIonType);

            // generate additional result elements
            for (PrecursorIonType ion : pa.getAdducts(origIonType.getIonization())) {
                if (!ion.equals(origIonType) && originalResultElement.getResult().getMolecularFormula().isSubtractable(ion.getAdduct())) {
                    try {
                        addedResultElements.add(new SiriusResultElement(IdentificationResult.withPrecursorIonType(originalResultElement.getResult(), ion)));
                    } catch (RuntimeException e) {
                        LoggerFactory.getLogger(CSIFingerIDComputation.class).error("Cannot neutralize " + originalResultElement.getResult().getMolecularFormula() + " with precursor ion type " + ion + ", although adduct " + ion.getAdduct() + " is subtractable? " + originalResultElement.getResult().getMolecularFormula().isSubtractable(ion.getAdduct()) + ", tree root is " + originalResultElement.getResult().getBeautifulTree());
                    }
                }
            }

            final List<SiriusResultElement> inputs = new ArrayList<>();
            inputs.add(originalResultElement);
            inputs.addAll(addedResultElements);
            final ArrayList<FormulaJob> formulaJobs = new ArrayList<>();
            final ArrayList<PredictFingerprintJob> predictionJobs = new ArrayList<>();
            final ArrayList<FingerblastJob> searchJobs = new ArrayList<>();
            for (SiriusResultElement elem : inputs) {
                final CSIPredictor csi = elem.getResult().getPrecursorIonType().getCharge() > 0 ? positiveMode : negativeMode;
                // search in database
                final FormulaJob fj = new FormulaJob(elem.getMolecularFormula(), csi.database.getSearchEngine(db), elem.getResult().getPrecursorIonType());
                formulaJobs.add(fj);
                final PredictFingerprintJob pj = new PredictFingerprintJob(csi, container, elem, fj, elem != originalResultElement);
                predictionJobs.add(pj);
                submitSubJob(fj);
                submitSubJob(pj);
                final FingerblastJob bj = new FingerblastJob(csi, fj, pj, db);
                searchJobs.add(bj);
                submitSubJob(bj);
            }
            for (int i = 0; i < searchJobs.size(); ++i) {
                FingerIdResult result = searchJobs.get(i).awaitResult();
                if (result == null) {
                    if (i == 0) {
                        LoggerFactory.getLogger(CSIFingerIDComputation.class).warn("Got null value from fingerblast. CSIFingerIDComputation:119");
                    }
                } else {

                    final Compound[] compounds = new Compound[result.getCandidates().size()];
                    final double[] scores = new double[result.getCandidates().size()];
                    final double[] tanimotos = new double[result.getCandidates().size()];
                    int k = 0;
                    for (Scored<FingerprintCandidate> fc : result.getCandidates()) {
                        scores[k] = fc.getScore();
                        tanimotos[k] = Tanimoto.probabilisticTanimoto(result.predictedFingerprint, fc.getCandidate().getFingerprint()).expectationValue();
                        compounds[k] = new Compound(fc.getCandidate());
                        ++k;
                    }

                    FingerIdData data = new FingerIdData(result.getAnnotationOrThrow(SearchableDatabase.class), compounds, scores, tanimotos, result.predictedFingerprint);

                    inputs.get(i).setFingerIdData(data);
                    inputs.get(i).setFingerIdComputeState(ComputingStatus.COMPUTED);

                }

            }
            LOG().info("Collecting results");
            // replace result list
            final HashMap<Ion, SiriusResultElement> elems = new HashMap<>();
            for (SiriusResultElement e : container.getResults()) {
                elems.put(new Ion(e.getResult().getMolecularFormula(), e.getResult().getPrecursorIonType()), e);
            }
            for (SiriusResultElement e : inputs) {
                elems.put(new Ion(e.getResult().getMolecularFormula(), e.getResult().getPrecursorIonType()), e);
            }
            final ArrayList<SiriusResultElement> sorted = new ArrayList<>(elems.values());
            sorted.sort((a, b) -> {
                if (a.getRank() < b.getRank()) return -1;
                else if (a.getRank() > b.getRank()) return 1;
                else {
                    if (a.getFingerIdData() != null && b.getFingerIdData() != null) {
                        return Double.compare(b.getFingerIdData().getTopScore(), a.getFingerIdData().getTopScore());
                    } else if (a.getFingerIdData() != null) {
                        return -1;
                    } else return 1;
                }
            });
            sorted.forEach(x -> x.buildTreeVisualization(SiriusResultElementConverter::convertTree));
            container.setResults(sorted);
            double topHitScore = Double.NEGATIVE_INFINITY;
            SiriusResultElement topHit = null;
            for (SiriusResultElement elem : container.getResults()) {
                double score = elem.getFingerIdData() != null ? elem.getFingerIdData().getTopScore() : Double.NEGATIVE_INFINITY;
                if (score > topHitScore) {
                    topHit = elem;
                    topHitScore = score;
                }
            }
            container.setBestHit(topHit);
            return true;

        }
    }

    protected static class Ion {
        protected final MolecularFormula formula;
        protected final PrecursorIonType ionType;

        public Ion(MolecularFormula formula, PrecursorIonType ionType) {
            this.formula = formula;
            this.ionType = ionType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Ion ion = (Ion) o;

            if (!formula.equals(ion.formula)) return false;
            return ionType.equals(ion.ionType);
        }

        @Override
        public int hashCode() {
            int result = formula.hashCode();
            result = 31 * result + ionType.hashCode();
            return result;
        }
    }

    protected class PredictFingerprintJob extends BasicDependentMasterJJob<ProbabilityFingerprint> {

        protected final CSIPredictor predictor;
        protected final ExperimentContainer container;
        protected final SiriusResultElement re;
        protected final FormulaJob formulaJob;
        protected final boolean requireCandidates;

        public PredictFingerprintJob(CSIPredictor predictor, ExperimentContainer container, SiriusResultElement originalResultElement, FormulaJob formulaJob, boolean requireCandidates) {
            super(JobType.REMOTE);
            this.predictor = predictor;
            this.container = container;
            this.re = originalResultElement;
            this.formulaJob = formulaJob;
            this.requireCandidates = requireCandidates;
            addRequiredJob(formulaJob);
        }

        @Override
        protected ProbabilityFingerprint compute() throws Exception {
            if (requireCandidates && formulaJob.awaitResult().isEmpty())
                return null;
            try (final WebAPI webAPI = WebAPI.newInstance()) {
                final WebAPI.PredictionJJob job = webAPI.makePredictionJob(container.getMs2Experiment(), re.getResult(), re.getResult().getResolvedTree(), predictor.fpVersion, EnumSet.of(predictor.predictorType));
                submitSubJob(job);
                return job.awaitResult();
            }

        }
    }

    protected class FingerblastJob extends BasicDependentJJob<FingerIdResult> {

        protected final FormulaJob formulaJob;
        protected final PredictFingerprintJob predictJob;
        protected final SearchableDatabase db;
        protected final CSIPredictor predictor;

        public FingerblastJob(CSIPredictor csi, FormulaJob formulaJob, PredictFingerprintJob predictJob, SearchableDatabase db) {
            super(JobType.CPU);
            this.predictor = csi;
            this.formulaJob = formulaJob;
            this.predictJob = predictJob;
            this.db = db;
            addRequiredJob(formulaJob);
            addRequiredJob(predictJob);
        }

        @Override
        protected FingerIdResult compute() throws Exception {
            final List<FingerprintCandidate> candidates = formulaJob.awaitResult();
            if (candidates == null) return null;
            final ProbabilityFingerprint fp = predictJob.awaitResult();
            if (fp == null) {
                return null;
            }
            final List<Scored<FingerprintCandidate>> scored = predictor.blaster.score(candidates, fp);
            final FingerIdResult result = new FingerIdResult(scored, 0d, fp, predictJob.re.getResult().getResolvedTree());
            result.setAnnotation(SearchableDatabase.class, db);
            return result;
        }
    }


    /////////////////////////////////// API /////////////////////////////////
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        boolean old = this.enabled;
        this.enabled = enabled;
        pcs.firePropertyChange("enabled", old, this.enabled);
    }

    public CSIPredictor getPredictor(PrecursorIonType type) {
        if (type.getCharge() >= 0 ) return positiveMode;
        else return negativeMode;
    }

    public CSIPredictor getPredictor(PredictorType type) {
        if (type==PredictorType.CSI_FINGERID_POSITIVE) return positiveMode;
        else if (type == PredictorType.CSI_FINGERID_NEGATIVE) return negativeMode;
        else throw new NoSuchElementException("Unknown predictor type " + type);
    }

}
