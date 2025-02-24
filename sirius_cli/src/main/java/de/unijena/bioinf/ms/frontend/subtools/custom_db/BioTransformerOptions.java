package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.ms.biotransformer.Cyp450Mode;
import de.unijena.bioinf.ms.biotransformer.MetabolicTransformation;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import java.util.List;

// --transfromation HUMAN_CUSTOM_MULTI --seq-step CYP450 --seq-iterations 3  --seq-step PHASE_2 --seq-iterations 1

public class BioTransformerOptions {
    @CommandLine.ArgGroup(multiplicity = "1", exclusive = true, order = 301)
    public BioTransformer bioTransformer;

    @CommandLine.Option(names = "--cyp450Mode", description = "", defaultValue = "RULE_BASED", order = 309) //todo copy descripton from biotansformer cli
    public Cyp450Mode cyp450Mode; // CYP450 Mode



    //todo headline to differentiate single vs sequence.
    public static class BioTransformer {
        @CommandLine.ArgGroup(exclusive = false, heading = "", order = 310)
        Single biotransformer;

        @CommandLine.ArgGroup(exclusive = false, multiplicity = "1..4", heading = "", order = 320)
        List<Sequence> bioTransformerSequence;
    }

    public static class Single {
        @CommandLine.Option(names = {"--transformation"}, required = true, order = 311)
        MetabolicTransformation metabolicTransformation;
        @CommandLine.Option(names = {"--iterations"}, description = "", defaultValue = "1", order = 312) //todo copy descripton from biotansformer cli
        private int iterations; // Number of iterations
        @CommandLine.Option(names = "--useDB", description = "", defaultValue = "true", order = 313) //todo copy descripton from biotansformer cli
        private boolean useDB; // Use the database flag
        @CommandLine.Option(names = "--useSubstructure", description = "", defaultValue = "false", order = 314) //todo copy descripton from biotansformer cli
        private boolean useSubstructure; // Use the substructure flag

        // todo Check if all of this parameters should be changeable.
        // todo write validation method that checks whether parameter match tranformation type.
    }

    public static class Sequence {
        @CommandLine.Option(names = "--seq-step", completionCandidates = MetabolicTransformationSequenceCandidates.class, required = true, order = 321)
        public void setMetabolicTransformation(@NotNull MetabolicTransformation metabolicTransformation) {
            if (!MetabolicTransformation.valueSequenceOnly().contains(metabolicTransformation))
                throw new CommandLine.PicocliException("Metabolic transformation: '" + metabolicTransformation + "' is not allowed in transformation sequence.");
            this.metabolicTransformation = metabolicTransformation;
        }
        public MetabolicTransformation metabolicTransformation;

        @CommandLine.Option(names = "--seq-iterations", defaultValue = "1", order = 322)
        public int iterations;
    }

    public static class MetabolicTransformationSequenceCandidates implements Iterable<String> {
        @Override
        public java.util.@NotNull Iterator<String> iterator() {
            return MetabolicTransformation.valueSequenceOnly()
                    .stream()
                    .map(MetabolicTransformation::name).iterator();
        }
    }
}
