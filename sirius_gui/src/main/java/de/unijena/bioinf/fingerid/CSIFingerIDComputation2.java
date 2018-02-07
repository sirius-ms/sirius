package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.fingerid.blast.FingerblastScoringMethod;
import de.unijena.bioinf.fingerid.db.CustomDatabase;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.fingerid.db.SearchableDbOnDisc;
import de.unijena.bioinf.fingerid.jjobs.FormulaJob;
import de.unijena.bioinf.fingerid.net.CachedRESTDB;
import de.unijena.bioinf.fingerid.net.VersionsInfo;
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
import gnu.trove.list.array.TIntArrayList;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class CSIFingerIDComputation2 {

    protected CachedRESTDB database;
    protected MaskedFingerprintVersion fpVersion;
    protected Fingerblast blaster;
    protected boolean initialized;

    protected List<CustomDatabase> customDatabases;
    protected SearchableDatabase bio, pubchem;

    public CSIFingerIDComputation2() {
        initialize();

    }

    private void initialize() {
        SwingJJobContainer<Boolean> container = new TextAreaJJobContainer<>(new InitializeCSIFingerID(), "Initialize CSI:FingerID");
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
            SwingJJobContainer container = new SwingJJobContainer<Boolean>(new FingerIDGUITask(task.experiment, task.result, task.db), "Predict structure for " + task.result.getMolecularFormula());
            Jobs.MANAGER.submitSwingJob(container);
        }
    }


    protected class InitializeCSIFingerID extends BasicJJob<Boolean> {

        public InitializeCSIFingerID() {
            super(JobType.REMOTE);
        }

        @Override
        protected Boolean compute() throws Exception {
            try (final WebAPI webAPI = WebAPI.newInstance()) {
                VersionsInfo versionsInfo = webAPI.getVersionInfo();
                final TIntArrayList list = new TIntArrayList(4096);
                PredictionPerformance[] performances = webAPI.getStatistics(list);

                final CdkFingerprintVersion version = (CdkFingerprintVersion) WebAPI.getFingerprintVersion();

                final MaskedFingerprintVersion.Builder v = MaskedFingerprintVersion.buildMaskFor(version);
                v.disableAll();

                int[] fingerprintIndizes = list.toArray();

                for (int index : fingerprintIndizes) {
                    v.enable(index);
                }

                MaskedFingerprintVersion fingerprintVersion = v.toMask();

                FingerblastScoringMethod method = webAPI.getCovarianceScoring(fingerprintVersion, 1d / performances[0].withPseudoCount(0.25).numberOfSamples());

                final List<CustomDatabase> cds = CustomDatabase.customDatabases(true);
                final File directory = getDefaultDirectory();
                synchronized (CSIFingerIDComputation2.this) {
                    database = new CachedRESTDB(versionsInfo, fingerprintVersion, getDefaultDirectory());
                    fpVersion = fingerprintVersion;
                    blaster = new Fingerblast(method, null);
                    initialized = true;
                    bio = new SearchableDbOnDisc("biological database", new File(directory, "bio"), false, true, false);
                    pubchem = new SearchableDbOnDisc("PubChem", new File(directory, "not-bio"), true, true, false);
                    customDatabases = cds;
                }
            }
            return true;
        }
    }

    public List<SearchableDatabase> getAvailableDatabases() {
        final List<SearchableDatabase> db = new ArrayList<>();
        db.add(pubchem);
        db.add(bio);
        db.addAll(CustomDatabase.customDatabases(true));
        return db;
    }

    public synchronized boolean isInitialized() {
        return initialized;
    }

    public File getDefaultDirectory() {
        final String val = PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.fingerID.cache");
        return Paths.get(val).toFile();
    }

    protected class FingerIDGUITask extends BasicMasterJJob<Boolean> {
        public final ExperimentContainer container;
        public final SiriusResultElement originalResultElement;
        public final List<SiriusResultElement> addedResultElements;
        public final SearchableDatabase db;

        public FingerIDGUITask(ExperimentContainer container, SiriusResultElement originalResultElement, SearchableDatabase db) {
            super(JobType.SCHEDULER);
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
            final PrecursorIonType origIonType = originalResultElement.getResult().getPrecursorIonType();
            // step 1: download molecular formulas and predict fingerprints
            final PossibleAdducts pa = container.getMs2Experiment().getAnnotation(PossibleAdducts.class, new PossibleAdducts());
            pa.addAdduct(origIonType);

            // generate additional result elements
            for (PrecursorIonType ion : pa.getAdducts(origIonType.getIonization())) {
                if (!ion.equals(origIonType) && originalResultElement.getResult().getMolecularFormula().isSubtractable(ion.getAdduct())) {
                    try {
                        addedResultElements.add(new SiriusResultElement(IdentificationResult.withPrecursorIonType(originalResultElement.getResult(), ion)));
                    } catch (RuntimeException e) {
                        LoggerFactory.getLogger(CSIFingerIDComputation2.class).error("Cannot neutralize " + originalResultElement.getResult().getMolecularFormula() + " with precursor ion type " + ion + ", although adduct " + ion.getAdduct() + " is subtractable? " + originalResultElement.getResult().getMolecularFormula().isSubtractable(ion.getAdduct()) + ", tree root is " + originalResultElement.getResult().getBeautifulTree());
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
                // search in database
                final FormulaJob fj = new FormulaJob(elem.getMolecularFormula(), database.getSearchEngine(db), elem.getResult().getPrecursorIonType());
                formulaJobs.add(fj);
                final PredictFingerprintJob pj = new PredictFingerprintJob(container, elem, fj, elem != originalResultElement);
                predictionJobs.add(pj);
                submitSubJob(fj);
                submitSubJob(pj);
                final FingerblastJob bj = new FingerblastJob(fj, pj, db);
                searchJobs.add(bj);
                submitSubJob(bj);
            }
            for (int i = 0; i < searchJobs.size(); ++i) {
                FingerIdResult result = searchJobs.get(i).takeResult();
                if (result == null) {
                    if (i == 0) {
                        LoggerFactory.getLogger(CSIFingerIDComputation2.class).warn("Got null value from fingerblast. CSIFingerIDComputation2:119");
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

        protected final ExperimentContainer container;
        protected final SiriusResultElement re;
        protected final FormulaJob formulaJob;
        protected final boolean requireCandidates;

        public PredictFingerprintJob(ExperimentContainer container, SiriusResultElement originalResultElement, FormulaJob formulaJob, boolean requireCandidates) {
            super(JobType.REMOTE);
            this.container = container;
            this.re = originalResultElement;
            this.formulaJob = formulaJob;
            this.requireCandidates = requireCandidates;
            addRequiredJob(formulaJob);
        }

        @Override
        protected ProbabilityFingerprint compute() throws Exception {
            if (requireCandidates && formulaJob.takeResult().isEmpty())
                return null;
            try (final WebAPI webAPI = WebAPI.newInstance()) {
                final WebAPI.PredictionJJob job = webAPI.makePredictionJob(container.getMs2Experiment(), re.getResult(), re.getResult().getResolvedTree(), fpVersion, PredictorType.CSI_FINGERID);
                submitSubJob(job);
                return job.takeResult();
            }

        }
    }

    protected class FingerblastJob extends BasicDependentJJob<FingerIdResult> {

        protected final FormulaJob formulaJob;
        protected final PredictFingerprintJob predictJob;
        protected final SearchableDatabase db;

        public FingerblastJob(FormulaJob formulaJob, PredictFingerprintJob predictJob, SearchableDatabase db) {
            super(JobType.CPU);
            this.formulaJob = formulaJob;
            this.predictJob = predictJob;
            this.db = db;
            addRequiredJob(formulaJob);
            addRequiredJob(predictJob);
        }

        @Override
        protected FingerIdResult compute() throws Exception {
            final List<FingerprintCandidate> candidates = formulaJob.takeResult();
            if (candidates == null) return null;
            final ProbabilityFingerprint fp = predictJob.takeResult();
            if (fp == null) {
                return null;
            }
            final List<Scored<FingerprintCandidate>> scored = blaster.score(candidates, fp);
            final FingerIdResult result = new FingerIdResult(scored, 0d, fp, predictJob.re.getResult().getResolvedTree());
            result.setAnnotation(SearchableDatabase.class, db);
            return result;
        }
    }

}
