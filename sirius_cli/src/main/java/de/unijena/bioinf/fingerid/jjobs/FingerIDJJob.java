package de.unijena.bioinf.fingerid.jjobs;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.ChemistryBase.ms.ft.IonTreeUtils;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.canopus.Canopus;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.chemdb.CompoundCandidateChargeState;
import de.unijena.bioinf.chemdb.SearchStructureByFormula;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.fingerid.TrainingStructuresPerPredictor;
import de.unijena.bioinf.fingerid.TrainingStructuresSet;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.fingerid.db.CachedRESTDB;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.fingerid.net.PredictionJJob;
import de.unijena.bioinf.fingerid.net.WebAPI;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.predictor_types.UserDefineablePredictorType;
import de.unijena.bioinf.jjobs.BasicDependentMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.projectspace.ExperimentResult;
import de.unijena.bioinf.sirius.projectspace.ExperimentResultJJob;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FingerIDJJob extends BasicDependentMasterJJob<Map<IdentificationResult, ProbabilityFingerprint>> {
    private List<IdentificationResult> input = null;
    private Ms2Experiment experiment = null;
    private int maxResults = Integer.MAX_VALUE;
    private boolean filterIdentifications = true;

    //prediction options
    private final Collection<UserDefineablePredictorType> predictors;
    private final MaskedFingerprintVersion fingerprintVersion;

    //fingerblast options
    private final Fingerblast fingerblast;
    private long dbFlag = 0L;
    private BioFilter bioFilter = BioFilter.ALL;

    CachedRESTDB database;
    SearchableDatabase queryDb;


    //canopus options
    private Canopus canopus = null;

    protected List<IdentificationResult> addedIdentificationResults = new ArrayList<>();

    public FingerIDJJob(Fingerblast fingerblast, MaskedFingerprintVersion fingerprintVersion, CachedRESTDB database, SearchableDatabase queryDb, Collection<UserDefineablePredictorType> predictors) {
        super(JobType.CPU);
        this.fingerblast = fingerblast;
        this.fingerprintVersion = fingerprintVersion;
        this.predictors = predictors;
        this.database = database;
        this.queryDb = queryDb;
    }

    public void setInput(List<IdentificationResult> results, Ms2Experiment experiment) {
        this.input = results;
        this.experiment = experiment;
    }

    public void setDbFlag(long dbFlag) {
        this.dbFlag = dbFlag;
    }

    public void setCanopus(Canopus canopus) {
        this.canopus = canopus;
    }

    public void setBioFilter(BioFilter bioFilter) {
        this.bioFilter = bioFilter;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public void setFilterIdentifications(boolean filterIdentifications) {
        this.filterIdentifications = filterIdentifications;
    }

    public List<IdentificationResult> getAddedIdentificationResults() {
        return addedIdentificationResults;
    }

    @Override
    protected Map<IdentificationResult, ProbabilityFingerprint> compute() throws Exception {
//        try {

            final SearchStructureByFormula searchStructureByFormula = database.getSearchEngine(queryDb);

            List<IdentificationResult> input = new ArrayList<>();
            Ms2Experiment experiment = null;
            //collect input from input field
            if (this.input != null) {
                input.addAll(this.input);
                experiment = this.experiment;
            }

            //collect input from dependent jobs
            for (JJob j : getRequiredJobs()) {
                //todo just for compatibility with old version. remove afterwards!!!
                if (j instanceof Sirius.SiriusIdentificationJob) {
                    Sirius.SiriusIdentificationJob job = (Sirius.SiriusIdentificationJob) j;
                    if (experiment == null) {
                        experiment = job.getExperiment();
                    } else if (experiment != job.getExperiment()) {
                        throw new IllegalArgumentException("SiriusIdentificationJobs to collect are from different MS2Experiments");
                    }
                    input.addAll(job.awaitResult());
                }
                if (j instanceof ExperimentResultJJob) {
                    ExperimentResultJJob job = (ExperimentResultJJob) j;
                    ExperimentResult experimentResult = job.awaitResult();
                    if (experiment == null) {
                        experiment = experimentResult.getExperiment();
                    } else if (experiment != experimentResult.getExperiment()) {
                        throw new IllegalArgumentException("SiriusIdentificationJobs to collect are from different MS2Experiments");
                    }
                    List<IdentificationResult> results = experimentResult.getResults();
                    if (results!=null) input.addAll(results);
                }
            }

            if (input.isEmpty()) return null;

            //sort input with ascending score
            Collections.sort(input);

            //shrinking list to max number of values
            if (input.size() > maxResults)
                input.subList(maxResults, input.size()).clear();

            final ArrayList<IdentificationResult> filteredResults = new ArrayList<>();
            //filterIdentifications list if wanted
            if (filterIdentifications && input.size() > 0) {
                // first filterIdentifications identificationResult list by top scoring formulas
                final IdentificationResult top = input.get(0);
                if (top == null || top.getResolvedTree() == null) return null;
                progressInfo("Filter Identification Results for CSI:FingerID usage");
                filteredResults.add(top);
                final double threshold = Math.max(top.getScore(), 0) - Math.max(5, top.getScore() * 0.25);
                for (int k = 1, n = input.size(); k < n; ++k) {
                    IdentificationResult e = input.get(k);
                    if (e.getScore() < threshold) break;
                    if (e.getResolvedTree() == null || e.getResolvedTree().numberOfVertices() <= 1) {
                        progressInfo("Cannot estimate structure for " + e.getMolecularFormula() + ". Fragmentation Tree is empty.");
                        continue;
                    }
                    filteredResults.add(e);
                }
            } else {
                filteredResults.addAll(input);
            }

            Iterator<IdentificationResult> iter = filteredResults.iterator();
            {
                while (iter.hasNext()) {
                    final IdentificationResult ir = iter.next();
                    if (ir.getBeautifulTree().numberOfVertices() < 3) {
                        LoggerFactory.getLogger(FingerIDJJob.class).warn("Ignore fragmentation tree for " + ir.getMolecularFormula() + " because it contains less than 3 vertices.");
                        //progressInfo("Ignore " + ir.getMolecularFormula() + " because the tree contains less than 3 vertices");
                        iter.remove();
                    }
                }
            }

            if (filteredResults.isEmpty()) {
                progressInfo("No suitable fragmentation tree left.");
                return Collections.emptyMap();
            }

            progressInfo("Search with CSI:FingerID");

            // EXPAND LIST

            final List<IdentificationResult> ionTypes = new ArrayList<>();
            for (IdentificationResult ir : filteredResults) {
                final Ms2Experiment validatedExperiment;
                {
                    ProcessedInput pi = ir.getRawTree().getAnnotationOrNull(ProcessedInput.class);
                    if (pi != null) validatedExperiment = pi.getExperimentInformation();
                    else {
                        LOG().info("FingerID job has no access to processed input data");
                        validatedExperiment = experiment;
                    }
                }
                final PossibleAdducts adductTypes = validatedExperiment.getAnnotation(PossibleAdducts.class, new PossibleAdducts(PeriodicTable.getInstance().adductsByIonisation(experiment.getPrecursorIonType())));
                for (PrecursorIonType ionType : adductTypes) {
                    if (!ionType.equals(ir.getBeautifulTree().getAnnotationOrThrow(PrecursorIonType.class)) && new IonTreeUtils().isResolvable(ir.getBeautifulTree(), ionType)) {
                        IdentificationResult newIr = IdentificationResult.withPrecursorIonType(ir, ionType);
                        if (newIr.getResolvedTree().numberOfVertices() >= 3)
                            ionTypes.add(newIr);
                    }
                }
            }
            filteredResults.addAll(ionTypes);
            Collections.sort(filteredResults);
            addedIdentificationResults.addAll(ionTypes);

            //submit jobs
            List<PredictionJJob> predictionJobs = new ArrayList<>();
            List<FingerprintDependentJJob> annotationJobs = new ArrayList<>();

            for (IdentificationResult fingeridInput : filteredResults) {
                PredictionJJob predictionJob = WebAPI.INSTANCE.makePredictionJob(experiment, fingeridInput, fingeridInput.getResolvedTree(), fingerprintVersion, predictors);
                submitSubJob(predictionJob);
                predictionJobs.add(predictionJob);

                // formula jobs: retrieve fingerprint candidates for specific MF
                FormulaJob formulaJob = new FormulaJob(fingeridInput.getMolecularFormula(), searchStructureByFormula, fingeridInput.getPrecursorIonType());

                //fingerblast jobs
                final PrecursorIonType ionType = fingeridInput.getResolvedTree().getAnnotationOrThrow(PrecursorIonType.class);
                final PredictorType predictorType = getUnambiguousUserDefineablePredictorTypeOrThrow(predictors).toPredictorType(fingeridInput.getPrecursorIonType());
                final TrainingStructuresSet trainingStructuresSet = TrainingStructuresPerPredictor.getInstance().getTrainingStructuresSet(predictorType);
                FingerblastJJob blastJob = new FingerblastJJob(fingerblast, bioFilter, dbFlag, CompoundCandidateChargeState.getFromPrecursorIonType(ionType), trainingStructuresSet);
                blastJob.addRequiredJob(formulaJob);
                blastJob.addRequiredJob(predictionJob);
                submitSubJob(formulaJob);
                submitSubJob(blastJob);
                annotationJobs.add(blastJob);

                // submit canopus jobs if needed
                if (canopus != null) {
                    CanopusJJob canopusJob = new CanopusJJob(canopus);
                    canopusJob.addRequiredJob(predictionJob);
                    submitSubJob(canopusJob);
                    annotationJobs.add(canopusJob);
                }
            }

            //collect results
            Map<IdentificationResult, ProbabilityFingerprint> fps = new HashMap<>(input.size());
            for (PredictionJJob predictionJob : predictionJobs) {
                ProbabilityFingerprint r = predictionJob.awaitResult();
                if (r != null)
                    fps.put(predictionJob.result, r);
            }

            //annotate results -> annotation are not thread save and should not be.
            for (FingerprintDependentJJob job : annotationJobs) {
                job.takeAndAnnotateResult();
            }

            // delete added IR without database hit
            final List<IdentificationResult> toDelete = new ArrayList<>();
            for (IdentificationResult ar : addedIdentificationResults) {
                FingerIdResult fr = ar.getAnnotationOrNull(FingerIdResult.class);
                if (fr == null || fr.getCandidates().isEmpty()) {
                    toDelete.add(ar);
                }
            }
            addedIdentificationResults.removeAll(toDelete);
            filteredResults.removeAll(toDelete);


            return fps;
//        }
    }

    private UserDefineablePredictorType getUnambiguousUserDefineablePredictorTypeOrThrow(Collection<UserDefineablePredictorType> predictors) {
        //currently we only support results of a single predictor
        if (predictors.size()==0){
            throw new IllegalArgumentException("No UserDefineablePredictorType given.");
        } else if (predictors.size()>1){
            throw new NoSuchMethodError("Multiple predictors per result are not supported.");
        }
        return predictors.iterator().next();
    }
}
