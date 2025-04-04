package de.unijena.bioinf.ms.biotransformer;

import biotransformer.transformation.Biotransformation;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.Delegate;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.List;
import java.util.concurrent.Callable;

@Accessors(fluent = false, chain = true)
public class BioTransformerJJob extends BasicMasterJJob<List<BioTransformerResult>> {

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
        List<JJob<List<Biotransformation>>> jobs =
                switch (getMetabolicTransformation()) {
                    case PHASE_1_CYP450 -> substrates.stream().map(ia -> makeJob(() ->
                                    BiotransformerWrapper.cyp450BTransformer(ia, getIterations(), getCyp450Mode(), isUseDB(), isUseSub()))).toList();
                    case EC_BASED -> substrates.stream().map(ia -> makeJob(() ->
                            BiotransformerWrapper.ecBasedBTransformer(ia, getIterations(), isUseDB(), isUseSub()))).toList();
                    case PHASE_2 -> substrates.stream().map(ia -> makeJob(() ->
                            BiotransformerWrapper.phaseIIBTransformer(ia, getIterations(), getP2Mode(), isUseDB(), isUseSub()))).toList();
                    case HUMAN_GUT -> substrates.stream().map(ia -> makeJob(() ->
                            BiotransformerWrapper.hGutBTransformer(ia, getIterations(), isUseDB(), isUseSub()))).toList();
                    case ENV_MICROBIAL -> substrates.stream().map(ia -> makeJob(() ->
                            BiotransformerWrapper.envMicrobialTransformer(ia, getIterations(), isUseDB(), isUseSub()))).toList();
                    case ALL_HUMAN -> substrates.stream().map(ia -> makeJob(() ->
                            BiotransformerWrapper.allHumanTransformer(ia, getIterations(), getP2Mode(), getCyp450Mode(), isUseDB(), isUseSub()))).toList();
                    case ABIOTIC -> substrates.stream().map(ia -> makeJob(() ->
                            BiotransformerWrapper.abioticTransformer(ia, getIterations()))).toList();
                    case HUMAN_CUSTOM_MULTI -> substrates.stream().map(ia -> makeJob(() ->
                                    BiotransformerWrapper.multiBioTransformer(ia, getSequenceSteps(), getCyp450Mode(), isUseDB(), isUseSub()))).toList();
                };
        //TODO:
        submitSubJobsInBatches(jobs, jobManager.getCPUThreads()).forEach(JJob::takeResult);

        return jobs.stream().map(JJob::takeResult).map(BioTransformerResult::new).toList(); //results here
    }


    private static JJob<List<Biotransformation>> makeJob(Callable<List<Biotransformation>> callable) {
        return new BasicJJob<>() {
            @Override
            protected List<Biotransformation> compute() throws Exception {
                return callable.call();
            }
        };
    }
}
