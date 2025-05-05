package de.unijena.bioinf.ms.biotransformer;

import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.Delegate;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

@Accessors(fluent = false, chain = true)
public class BioTransformerJJob extends BasicMasterJJob<List<BioTransformerResult>> {
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
        List<JJob<List<BioTransformation>>> jobs = substrates.stream().map(ia -> makeJob(() -> transform(ia))).toList();
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
                })
                .filter(Objects::nonNull)
                .map(BioTransformerResult::new).toList(); //results here
    }

    protected List<BioTransformation> transform(IAtomContainer ia) throws Exception {
        return switch (getMetabolicTransformation()) {
            case PHASE_1_CYP450 -> BioTransformerWrapper.cyp450BTransformer(ia, getIterations(), getCyp450Mode(), isUseDB(), isUseSub());
            case EC_BASED -> BioTransformerWrapper.ecBasedBTransformer(ia, getIterations(), isUseDB(), isUseSub());
            case PHASE_2 -> BioTransformerWrapper.phaseIIBTransformer(ia, getIterations(), getP2Mode().P2ModeOrdinal(), isUseDB(), isUseSub());
            case HUMAN_GUT -> BioTransformerWrapper.hGutBTransformer(ia, getIterations(), isUseDB(), isUseSub());
            case ENV_MICROBIAL -> BioTransformerWrapper.envMicrobialTransformer(ia, getIterations(), isUseDB(), isUseSub());
            case ALL_HUMAN -> BioTransformerWrapper.allHumanTransformer(ia, getIterations(), getP2Mode().P2ModeOrdinal(), getCyp450Mode(), isUseDB(), isUseSub());
            case ABIOTIC -> BioTransformerWrapper.abioticTransformer(ia, getIterations());
            case HUMAN_CUSTOM_MULTI -> BioTransformerWrapper.multiBioTransformer(ia, settings.getSequenceSteps(), getCyp450Mode(), isUseDB(), isUseSub());
        };
    }

    private static JJob<List<BioTransformation>> makeJob(Callable<List<BioTransformation>> callable) {
        return new BasicJJob<>() {
            @Override
            protected List<BioTransformation> compute() throws Exception {
                return callable.call();
            }
        };
    }
}
