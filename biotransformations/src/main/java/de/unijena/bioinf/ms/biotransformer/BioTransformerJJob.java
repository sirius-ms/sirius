package de.unijena.bioinf.ms.biotransformer;

import biotransformer.transformation.Biotransformation;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.List;
import java.util.concurrent.Callable;

public class BioTransformerJJob extends BasicMasterJJob<List<BioTransformerResult>> {
    List<IAtomContainer> substrates;

    private MetabolicTransformation metabolicTransformation;

    private int iterations; // Number of iterations
    private int cyp450Mode; // CYP450 Mode
    private int p2Mode; // Phase II Mode
    private boolean useDB; // Use the database flag
    private boolean useSub; // Use the substructure flag

    public BioTransformerJJob() {
        super(JobType.CPU);
    }

    @Override
    protected List<BioTransformerResult> compute() throws Exception {
        List<JJob<List<Biotransformation>>> jobs =
                switch (metabolicTransformation) {
                    case PHASE_1_CYP450 -> substrates.stream().map(ai -> makeJob(() ->
                                    BiotransformerWrapper.Cyp450BTransformer(ai, iterations, cyp450Mode, useDB, useSub)))
                            .toList();
                    case EC_BASED -> substrates.stream().map(ai -> makeJob(() ->
                            BiotransformerWrapper.ECBasedBTransformer(ai, iterations, useDB, useSub))).toList();
                    case PHASE_2 -> {
                    }
                    case HUMAN_GUT -> {
                    }
                    case ENV_MICROBIAL -> {
                    }
                    case ALL_HUMAN -> {
                    }
                    case SUPER_BIO -> {
                    }
                    case ABIOTIC -> {
                    }
                    case HUMAN_CUSTOM_MULTI -> throw new IllegalArgumentException("This is the single transformation Job, use ....");
                };

        submitSubJobsInBatches(jobs,8).forEach(JJob::takeResult);

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
