package de.unijena.bioinf.fingerid.jjobs;

import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.fingerid.net.WebAPI;
import de.unijena.bioinf.fingeriddb.job.PredictorType;
import de.unijena.bioinf.jjobs.DependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;

import java.util.*;

public class FingerIDJJob extends DependentJJob<List<IdentificationResult>> {
    private List<IdentificationResult> input = null;
    private Ms2Experiment experiment = null;
    private int maxResults = Integer.MAX_VALUE;
    private boolean filter = true;

    //todo these things are always the same. maybe save in webapi or somewhere else
    private final PredictorType[] predicors;
    private final MaskedFingerprintVersion fingerprintVersion;

    public FingerIDJJob(MaskedFingerprintVersion fingerprintVersion) {
        this(fingerprintVersion, PredictorType.CSI_FINGERID);
    }

    public FingerIDJJob(MaskedFingerprintVersion fingerprintVersion, PredictorType... predicors) {
        super(JobType.CPU);
        this.fingerprintVersion = fingerprintVersion;
        this.predicors = predicors;
    }

    public void setInput(List<IdentificationResult> results, Ms2Experiment experiment) {
        this.input = results;
        this.experiment = experiment;
    }

    @Override
    protected List<IdentificationResult> compute() throws Exception {
        WebAPI webAPI = WebAPI.newInstance();

        List<IdentificationResult> input = new LinkedList<>();
        Ms2Experiment experiment = null;
        //collect input from input field
        if (this.input != null) {
            input.addAll(this.input);
            experiment = this.experiment;
        }

        //collect input from dependent jobs
        for (JJob j : requiredJobsDone) {
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
        //filter list if wanted
        if (filter) {
            // first filter result list by top scoring formulas
            final IdentificationResult top = ((LinkedList<IdentificationResult>) input).pollFirst();
            if (top == null || top.getResolvedTree() == null) return null;

            progressInfo("Filter Identification Results for CSI:FingerId usage");
            filteredResults.add(top);
            final double threshold = Math.max(top.getScore(), 0) - Math.max(5, top.getScore() * 0.25);

            for (IdentificationResult e : input) {
                if (e.getScore() < threshold) break;
                if (e.getResolvedTree() == null || e.getResolvedTree().numberOfVertices() <= 1) {
                    progressInfo("Cannot estimate structure for " + e.getMolecularFormula() + ". Fragmentation Tree is empty.");
                    continue;
                }
                filteredResults.add(e);
            }
        } else {
            filteredResults.addAll(filteredResults);
        }

        progressInfo("Search with CSI:FingerId");

        List<WebAPI.PredictionJJob> predictionJJobs = new ArrayList<>(filteredResults.size());

        for (IdentificationResult fingeridInput : filteredResults) {
            WebAPI.PredictionJJob predictionJob = webAPI.predictFingerprint(SiriusJobs.getGlobalJobManager(), experiment, fingeridInput, fingerprintVersion, predicors);
            predictionJJobs.addAll(predictionJJobs);

            //todo handle jobs, maybe this should be an innder class of cli, but it would be much cooler if we could use it in gui also
        }
        return null;

    }
}
