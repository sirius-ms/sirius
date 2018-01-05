package de.unijena.bioinf.fingerid.jjobs;

import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.canopus.Canopus;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.fingerid.net.WebAPI;
import de.unijena.bioinf.fingeriddb.job.PredictorType;
import de.unijena.bioinf.jjobs.DependentMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;

import java.util.*;

public class FingerIDJJob extends DependentMasterJJob<Map<IdentificationResult, ProbabilityFingerprint>> {
    private List<IdentificationResult> input = null;
    private Ms2Experiment experiment = null;
    private int maxResults = Integer.MAX_VALUE;
    private boolean filterIdentifications = true;

    //prediction options
    private final PredictorType[] predicors;
    private final MaskedFingerprintVersion fingerprintVersion;

    //fingerblast options
    private final Fingerblast fingerblast;
    private long dbFlag = 0L;
    private BioFilter bioFilter = BioFilter.ALL;

    //canopus options
    private Canopus canopus = null;


    public FingerIDJJob(Fingerblast fingerblast, MaskedFingerprintVersion fingerprintVersion) {
        this(fingerblast, fingerprintVersion, PredictorType.CSI_FINGERID);
    }

    public FingerIDJJob(Fingerblast fingerblast, MaskedFingerprintVersion fingerprintVersion, PredictorType... predicors) {
        super(JobType.CPU);
        this.fingerblast = fingerblast;
        this.fingerprintVersion = fingerprintVersion;
        this.predicors = predicors;
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

    @Override
    protected Map<IdentificationResult, ProbabilityFingerprint> compute() throws Exception {
        WebAPI webAPI = WebAPI.newInstance();

        List<IdentificationResult> input = new ArrayList<>();
        Ms2Experiment experiment = null;
        //collect input from input field
        if (this.input != null) {
            input.addAll(this.input);
            experiment = this.experiment;
        }

        //collect input from dependent jobs
        for (JJob j : requiredJobs) {
            if (j instanceof Sirius.SiriusIdentificationJob) {
                Sirius.SiriusIdentificationJob job = (Sirius.SiriusIdentificationJob) j;
                //todo i am not 100% sure if this is needed
                if (experiment == null) {
                    experiment = job.getExperiment();
                } else if (experiment != job.getExperiment()) {
                    throw new IllegalArgumentException("SiriusIdentificationJobs to collect are from different MS2Experments");
                }
                input.addAll(job.takeResult());
            }
        }

        if (input.isEmpty()) return null;

        //sort input with ascending score
        Collections.sort(input, new Comparator<IdentificationResult>() {
            @Override
            public int compare(IdentificationResult o1, IdentificationResult o2) {
                return Double.compare(o1.getScore(), o2.getScore()); //todo do we need ascending or descending
            }
        });

        //shrinking list to max number of values
        if (input.size() > maxResults)
            input.subList(maxResults, input.size()).clear();

        final ArrayList<IdentificationResult> filteredResults = new ArrayList<>();
        //filterIdentifications list if wanted
        if (filterIdentifications && input.size() > 0) {
            // first filterIdentifications identificationResult list by top scoring formulas
            final IdentificationResult top = input.get(0);
            if (top == null || top.getResolvedTree() == null) return null;
            progressInfo("Filter Identification Results for CSI:FingerId usage");
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
                    progressInfo("Ignore " + ir.getMolecularFormula() + " because the tree contains less than 3 vertices");
                    iter.remove();
                }
            }
        }

        if (filteredResults.isEmpty()) {
            progressInfo("No suitable fragmentation tree left.");
            return Collections.emptyMap();
        }

        progressInfo("Search with CSI:FingerId");


        //submit jobs
        List<WebAPI.PredictionJJob> predictionJobs = new ArrayList<>();
        List<FingerprintDependentJJob> annotationJobs = new ArrayList<>();
        for (IdentificationResult fingeridInput : filteredResults) {
            //prediction jobs
            WebAPI.PredictionJJob predictionJob = webAPI.makePredictionJob(experiment, fingeridInput, fingeridInput.getResolvedTree(), fingerprintVersion, predicors);
            submitSubJob(predictionJob);
            predictionJobs.add(predictionJob);

            //fingerblast jobs
            FingerblastJJob blastJob = new FingerblastJJob(fingerblast, bioFilter, dbFlag);
            blastJob.addRequiredJob(predictionJob);
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
        for (WebAPI.PredictionJJob predictionJob : predictionJobs) {
            ProbabilityFingerprint r = predictionJob.takeResult();
            if (r != null)
                fps.put(predictionJob.result, r);
        }

        //annotate results -> annotation are not thread save and should not be.
        for (FingerprintDependentJJob job : annotationJobs) {
            job.takeAndAnnotateResult();
        }


        return fps;
    }

    @Override
    protected JobManager jobManager() {
        return SiriusJobs.getGlobalJobManager();
    }
}
