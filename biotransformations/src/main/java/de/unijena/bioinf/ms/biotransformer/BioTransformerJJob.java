package de.unijena.bioinf.ms.biotransformer;

import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.Delegate;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Accessors(fluent = false, chain = true)
public class BioTransformerJJob extends BasicMasterJJob<List<BioTransformerResult>> {
    /**
     * The origin un-metabolized substrate (drug, toxin etc. of interest).
     */
    @Getter
    @Setter
    List<IAtomContainer> substrates;

    @Setter
    @Delegate
    BioTransformerSettings settings;

    public BioTransformerJJob(BioTransformerSettings settings) {
        this();
        this.settings = settings;
    }

    public BioTransformerJJob() {
        super(JobType.CPU);
    }

    @Override
    protected List<BioTransformerResult> compute() throws Exception {

        final int totalProgress = substrates.size() + 1;
        AtomicInteger progress = new AtomicInteger(0);
        JobProgressEventListener progressListener = evt -> {if (evt.isDone()) updateProgress(totalProgress, progress.incrementAndGet());};

        List<JJob<BioTransformerResult>> jobs = substrates.stream().map(ia -> {
            BasicJJob<BioTransformerResult> job = new BasicJJob<BioTransformerResult>() {
                @Override
                protected BioTransformerResult compute() throws Exception {
                    return BioTransformerWrapper.transform(ia, settings);
                }
            };
            job.addJobProgressListener(progressListener);
            return job;
        }).collect(Collectors.toList());

        submitSubJobsInBatches(jobs, jobManager.getCPUThreads()).forEach(JJob::getResult);

        return jobs.stream()
                .map(job -> {
                    try {
                        return job.awaitResult();
                    } catch (ExecutionException e) {
                        logError("Error when calling BioTransformer. Skipping the result. Some transformation products might be missing. Message: "+ e.getMessage());
                        logDebug("Error when calling BioTransformer. Skipping the result. Some transformation products might be missing.", e);
                        return null;
                    }
                }).filter(Objects::nonNull)
                .toList();
    }
}
