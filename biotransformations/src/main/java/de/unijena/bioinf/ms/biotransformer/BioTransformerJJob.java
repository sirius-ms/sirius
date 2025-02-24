package de.unijena.bioinf.ms.biotransformer;

import biotransformer.transformation.Biotransformation;
import biotransformer.utils.BiotransformerSequenceStep;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
@Accessors(fluent = false, chain = true)
public class BioTransformerJJob extends BasicMasterJJob<List<BioTransformerResult>> {

    @Setter
    List<IAtomContainer> substrates;
    @Setter
    private MetabolicTransformation metabolicTransformation;
    @Setter
    private int iterations; // Number of iterations
    @Setter
    private Cyp450Mode cyp450Mode; // CYP450 Mode
    @Setter
    private int p2Mode; // Phase II Mode
    @Setter
    private boolean useDB; // Use the database flag
    @Setter
    private boolean useSub; // Use the substructure flag
    @Setter
    private ArrayList<BiotransformerSequenceStep> sequenceSteps;

    public BioTransformerJJob() {
        super(JobType.CPU);
    }

    @Override
    protected List<BioTransformerResult> compute() throws Exception {
        List<JJob<List<Biotransformation>>> jobs =
                switch (metabolicTransformation) {
                    case PHASE_1_CYP450 -> substrates.stream().map(ia -> makeJob(() ->
                                    BiotransformerWrapper.cyp450BTransformer(ia, iterations, cyp450Mode, useDB, useSub)))
                            .toList();
                    case EC_BASED -> substrates.stream().map(ia -> makeJob(() ->
                            BiotransformerWrapper.ecBasedBTransformer(ia, iterations, useDB, useSub))).toList();
                    case PHASE_2 -> substrates.stream().map(ia -> makeJob(() ->
                            BiotransformerWrapper.phaseIIBTransformer(ia,iterations,p2Mode,useDB,useSub))).toList();
                    case HUMAN_GUT -> substrates.stream().map(ia -> makeJob(() ->
                            BiotransformerWrapper.hGutBTransformer(ia,iterations,useDB,useSub))).toList();
                    case ENV_MICROBIAL -> substrates.stream().map(ia -> makeJob(() ->
                            BiotransformerWrapper.envMicrobialTransformer(ia,iterations,useDB,useSub))).toList();

                    case ALL_HUMAN -> substrates.stream().map(ia -> makeJob(() ->
                            BiotransformerWrapper.allHumanTransformer(ia,iterations,p2Mode,cyp450Mode,useDB,useSub))).toList();
                    case ABIOTIC -> substrates.stream().map(ia -> makeJob(() ->
                            BiotransformerWrapper.abioticTransformer(ia,iterations))).toList();
                    case HUMAN_CUSTOM_MULTI ->
                        substrates.stream().map(ia -> makeJob(() ->
                                        BiotransformerWrapper.multiBioTransformer(ia, sequenceSteps, cyp450Mode, useDB, useSub)))
                                .toList();


                };

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
