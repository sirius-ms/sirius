package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.Tanimoto;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.*;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.fingerid.webapi.PredictionJJob;
import de.unijena.bioinf.fingerid.webapi.WebAPI;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.*;
import de.unijena.bioinf.ms.gui.fingerid.FingerIdResultBean;
import de.unijena.bioinf.ms.gui.fingerid.FingerIdTask;
import de.unijena.bioinf.ms.gui.sirius.ComputingStatus;
import de.unijena.bioinf.ms.gui.sirius.ExperimentResultBean;
import de.unijena.bioinf.ms.gui.sirius.IdentificationResultBean;
import de.unijena.bioinf.ms.gui.sirius.SiriusResultElementConverter;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.logging.TextAreaJJobContainer;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

@Deprecated
public class CSIFingerIDComputation {

    protected final CSIPredictor positiveMode, negativeMode;


    public CSIFingerIDComputation() {
        this.positiveMode = new CSIPredictor(PredictorType.CSI_FINGERID_POSITIVE);
        this.negativeMode = new CSIPredictor(PredictorType.CSI_FINGERID_NEGATIVE);
        //listen to connection check
        MainFrame.CONNECTION_MONITOR.addConectionStateListener(evt -> {
            ConnectionMonitor.ConnectionState value = (ConnectionMonitor.ConnectionState) evt.getNewValue();
            setEnabled(value.equals(ConnectionMonitor.ConnectionState.YES));
        });

        initialize();
    }

    private void initialize() {
        SwingJJobContainer<Boolean> container = new TextAreaJJobContainer<>(new InitializeCSIFingerID(), "Initialize CSI:FingerID", "CSI:FingerID");
        Jobs.MANAGER.submitSwingJob(container);
    }

    //compute for a single experiment
    public void compute(ExperimentResultBean c, SearchableDatabase db) {
        computeAll(Collections.singletonList(c), db);
    }

    //csi fingerid compute all button in main panel
    public void computeAll(List<ExperimentResultBean> compounds, SearchableDatabase db) {
        final ArrayList<FingerIdTask> tasks = new ArrayList<>();
        for (ExperimentResultBean c : compounds) {
            final List<IdentificationResultBean> candidates = getTopSiriusCandidates(c);
            if (candidates.isEmpty()) {
                LoggerFactory.getLogger(getClass()).warn("No molecular formula candidates available for compound: " + c.getGUIName() + " with " + c.getIonization());
            } else {
                for (IdentificationResultBean e : candidates) {
                    tasks.add(new FingerIdTask(db, c, e));
                }
            }

        }
        computeAll(tasks);
    }

    public void computeAll(Collection<FingerIdTask> compounds) {
        for (FingerIdTask task : compounds) {
            SwingJJobContainer<Boolean> container = new TextAreaJJobContainer<>(new FingerIDGUITask(task.experiment, task.result, task.db), task.result.getMolecularFormula().toString(), "Structure Prediction");
            Jobs.MANAGER.submitSwingJob(container);
        }
    }

    protected static List<IdentificationResultBean> getTopSiriusCandidates(ExperimentResultBean container) {
        final ArrayList<IdentificationResultBean> elements = new ArrayList<>();
        if (container == null || !container.isComputed()) return elements;
        final List<IdentificationResultBean> results = container.getResults();
        if (results == null || results.isEmpty()) return elements;
        final IdentificationResultBean top = results.get(0);
        if (top.getResult().getResolvedTree().numberOfEdges() > 0)
            elements.add(top);
        final double threshold = calculateThreshold(top.getScore());
        for (int k = 1; k < results.size(); ++k) {
            IdentificationResultBean e = results.get(k);
            if (e.getScore() < threshold) break;
            if (e.getResult().getResolvedTree().numberOfEdges() > 0)
                elements.add(e);
        }
        return elements;
    }

    public static double calculateThreshold(double topScore) {
        return Math.max(topScore, 0) - Math.max(5, topScore * 0.25);
    }

    public void refreshDatabaseCacheDir() throws IOException {
        positiveMode.refreshCacheDir();
        negativeMode.refreshCacheDir();
    }

    protected class InitializeCSIFingerID extends BasicJJob<Boolean> {


        public InitializeCSIFingerID() {
            super(JobType.REMOTE);
        }

        @Override
        protected Boolean compute() throws Exception {
            //wait if no connection is there
            while (MainFrame.CONNECTION_MONITOR.checkConnection().isNotConnected()) {
                Thread.sleep(5000);
                checkForInterruption();
            }
            positiveMode.initialize();
            negativeMode.initialize();
            //download training structures
            TrainingStructuresPerPredictor.getInstance().addAvailablePredictorTypes(PredictorType.CSI_FINGERID_POSITIVE, PredictorType.CSI_FINGERID_NEGATIVE);
            return true;
        }

    }

    protected class FingerIDGUITask extends BasicMasterJJob<Boolean> {
        public final ExperimentResultBean container;
        public final IdentificationResultBean originalResultElement;
        public final List<IdentificationResultBean> addedResultElements;
        public final SearchableDatabase db;

        public FingerIDGUITask(ExperimentResultBean container, IdentificationResultBean originalResultElement, SearchableDatabase db) {
            super(JobType.WEBSERVICE);
            this.container = container;
            this.originalResultElement = originalResultElement;
            this.addedResultElements = new ArrayList<>();
            this.db = db;
            addPropertyChangeListener(JobStateEvent.JOB_STATE_EVENT, originalResultElement);
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
            final List<IdentificationResultBean> inputs = new ArrayList<>();
            inputs.add(originalResultElement);
            if (originalResultElement.getResult().getPrecursorIonType().isPlainProtonationOrDeprotonation()) {
                final HashMap<Ion, IdentificationResultBean> knownIonMap = new HashMap<>();
                for (IdentificationResultBean elem : container.getResults()) {
                    if (elem.getResult() != null)
                        knownIonMap.put(new Ion(elem.getResult().getMolecularFormula(), elem.getResult().getPrecursorIonType()), elem);
                }
                // generate additional result elements
                for (PrecursorIonType ion : pa.getAdducts(origIonType.getIonization())) {
                    if (!ion.equals(origIonType) && originalResultElement.getResult().getMolecularFormula().isSubtractable(ion.getAdduct())) {
                        try {
                            final Ion addIon = new Ion(ion.measuredNeutralMoleculeToNeutralMolecule(originalResultElement.getResult().getMolecularFormula()), ion);
                            IdentificationResultBean e = knownIonMap.get(addIon);
                            if (e == null) {
                                e = new IdentificationResultBean(IdentificationResult.withPrecursorIonType(originalResultElement.getResult(), ion));
                                addedResultElements.add(e);
                            } else {
                                inputs.add(e);
                            }

                        } catch (RuntimeException e) {
                            LoggerFactory.getLogger(CSIFingerIDComputation.class).error("Cannot neutralize " + originalResultElement.getResult().getMolecularFormula() + " with precursor ion type " + ion + ", although adduct " + ion.getAdduct() + " is subtractable? " + originalResultElement.getResult().getMolecularFormula().isSubtractable(ion.getAdduct()) + ", tree root is " + originalResultElement.getResult().getBeautifulTree());
                        }
                    }
                }
            }

            LOG().info("Submitting Formula, Prediction and Search jobs");
            inputs.addAll(addedResultElements);
            final ArrayList<FormulaJob> formulaJobs = new ArrayList<>();
            final ArrayList<PredictFingerprintJob> predictionJobs = new ArrayList<>();
            final ArrayList<FingerblastJob> searchJobs = new ArrayList<>();

            for (IdentificationResultBean elem : inputs) {
                final CSIPredictor csi = elem.getResult().getPrecursorIonType().getCharge() > 0 ? positiveMode : negativeMode;
                // search in database
                final FormulaJob fj = new FormulaJob(elem.getMolecularFormula(), csi.database.getSearchEngine(db), elem.getResult().getPrecursorIonType());
                formulaJobs.add(fj);
                final PredictFingerprintJob pj = new PredictFingerprintJob(csi, container, elem, fj, elem != originalResultElement);
                predictionJobs.add(pj);
                submitSubJob(fj);
                submitSubJob(pj);
                final PredictorType predictorType = elem.getResult().getPrecursorIonType().getCharge() > 0 ? PredictorType.CSI_FINGERID_POSITIVE : PredictorType.CSI_FINGERID_NEGATIVE;
                final TrainingStructuresSet trainingStructuresSet = TrainingStructuresPerPredictor.getInstance().getTrainingStructuresSet(predictorType);
                final FingerblastJob bj = new FingerblastJob(csi, fj, pj, db, trainingStructuresSet);
                searchJobs.add(bj);
                submitSubJob(bj);
            }
            for (int i = 0; i < searchJobs.size(); ++i) {
                LOG().info("Awaiting search Job " + searchJobs.get(i).LOG().getName());
                FingerIdResult result = searchJobs.get(i).awaitResult();
                if (result == null) {
                    LOG().info("Result for search Job " + searchJobs.get(i).LOG().getName() + " is NULL");
                    if (i == 0) {
                        LOG().warn("Got null value from fingerblast. CSIFingerIDComputation:119");
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

                    FingerIdResultBean data = new FingerIdResultBean(result.getAnnotationOrThrow(SearchableDatabase.class), compounds, scores, tanimotos, result.predictedFingerprint);

                    inputs.get(i).setFingerIdData(data);
                    inputs.get(i).setFingerIdComputeState(ComputingStatus.COMPUTED);

                }

            }
            LOG().info("Collecting results");
            synchronized (container) {
                // replace result list
                final HashMap<Ion, IdentificationResultBean> elems = new HashMap<>();
                for (IdentificationResultBean e : container.getResults()) {
                    elems.put(new Ion(e.getResult().getMolecularFormula(), e.getResult().getPrecursorIonType()), e);
                }
                for (IdentificationResultBean e : inputs) {
                    if (e.getFingerIdData() != null) {
                        elems.put(new Ion(e.getResult().getMolecularFormula(), e.getResult().getPrecursorIonType()), e);
                    }
                }
                final ArrayList<IdentificationResultBean> sorted = new ArrayList<>(elems.values());
                sorted.sort((a, b) -> {
                    int compare = Integer.compare(a.getRank(), b.getRank());
                    if (compare == 0) {
                        if (a.getFingerIdData() != null && b.getFingerIdData() != null) {
                            return Double.compare(b.getFingerIdData().getTopScore(), a.getFingerIdData().getTopScore());
                        } else if (a.getFingerIdData() != null) {
                            return -1;
                        } else if (b.getFingerIdData() != null)
                            return 1;
                        else return 0;
                    }
                    return compare;
                });
                sorted.forEach(x -> x.buildTreeVisualization(SiriusResultElementConverter::convertTree));
                container.setResults(sorted);
                double topHitScore = Double.NEGATIVE_INFINITY;
                IdentificationResultBean topHit = null;
                for (IdentificationResultBean elem : container.getResults()) {
                    double score = elem.getFingerIdData() != null ? elem.getFingerIdData().getTopScore() : Double.NEGATIVE_INFINITY;
                    if (score > topHitScore) {
                        topHit = elem;
                        topHitScore = score;
                    }
                }
                container.setBestHit(topHit);
            }
            return true;

        }

        @Override
        protected void cleanup() {
            super.cleanup();
            this.removePropertyChangeListener(originalResultElement);
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
        protected final ExperimentResultBean container;
        protected final IdentificationResultBean re;
        protected final FormulaJob formulaJob;
        protected final boolean requireCandidates;

        public PredictFingerprintJob(CSIPredictor predictor, ExperimentResultBean container, IdentificationResultBean originalResultElement, FormulaJob formulaJob, boolean requireCandidates) {
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

            final PredictionJJob job = WebAPI.INSTANCE.makePredictionJob(container.getMs2Experiment(), re.getResult(), re.getResult().getResolvedTree(), predictor.fpVersion, EnumSet.of(predictor.predictorType));
            submitSubJob(job);
            return job.awaitResult();


        }
    }

    protected class FingerblastJob extends BasicDependentJJob<FingerIdResult> {

        protected final FormulaJob formulaJob;
        protected final PredictFingerprintJob predictJob;
        protected final SearchableDatabase db;
        protected final CSIPredictor predictor;
        private final TrainingStructuresSet trainingStructuresSet;

        public FingerblastJob(CSIPredictor csi, FormulaJob formulaJob, PredictFingerprintJob predictJob, SearchableDatabase db, TrainingStructuresSet trainingStructuresSet) {
            super(JobType.CPU);
            this.predictor = csi;
            this.formulaJob = formulaJob;
            this.predictJob = predictJob;
            this.db = db;
            this.trainingStructuresSet = trainingStructuresSet;
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
            for (FingerprintCandidate candidate : candidates) {
                postprocessCandidate(candidate);
            }
            final List<Scored<FingerprintCandidate>> scored = predictor.blaster.score(candidates, fp);
            final FingerIdResult result = new FingerIdResult(scored, 0d, fp, predictJob.re.getResult().getResolvedTree());
            result.setAnnotation(SearchableDatabase.class, db);
            return result;
        }

        protected void postprocessCandidate(FingerprintCandidate candidate) {
            //annotate training compounds;
            if (trainingStructuresSet.isInTrainingData(candidate.getInchi())) {
                long flags = candidate.getBitset();
                candidate.setBitset(flags | DatasourceService.Sources.TRAIN.flag);
            }
        }
    }


    /////////////////////////////////// API /////////////////////////////////
    /*private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

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
    }*/

    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
//        boolean old = this.enabled;
        this.enabled = enabled;
//        pcs.firePropertyChange("enabled", old, this.enabled);
    }

    public CSIPredictor getPredictor(PrecursorIonType type) {
        if (type.getCharge() >= 0) return positiveMode;
        else return negativeMode;
    }

    public CSIPredictor getPredictor(PredictorType type) {
        if (type == PredictorType.CSI_FINGERID_POSITIVE) return positiveMode;
        else if (type == PredictorType.CSI_FINGERID_NEGATIVE) return negativeMode;
        else throw new NoSuchElementException("Unknown predictor type " + type);
    }

}
