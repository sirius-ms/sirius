package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.ms.biotransformer.BioTransformerSettings;
import de.unijena.bioinf.ms.biotransformer.Cyp450Mode;
import de.unijena.bioinf.ms.biotransformer.MetabolicTransformation;
import de.unijena.bioinf.ms.biotransformer.P2Mode;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import java.util.List;
import java.util.Stack;
// --transfromation HUMAN_CUSTOM_MULTI --seq-step CYP450 --seq-iterations 3  --seq-step PHASE_2 --seq-iterations 1

public class BioTransformerOptions {
    @CommandLine.Option(names = "--cyp450Mode", description = """
            Specify the CYP450 prediction Mode:
            RULE_BASED: CypReact + BioTransformer rules
            CY_PRODUCT: CyProduct only
            COMBINED: CypReact + BioTransformer rules + CyProducts."""
            , defaultValue = "COMBINED", order = 301)
    public Cyp450Mode cyp450Mode;


    @CommandLine.Option(names = "--p2Mode", description = """
            Specify the PhaseII prediction Mode:
            BT_RULE_BASED: BioTransformer rules
            P2_RULE_ONLY: PhaseII predictor only
            COMBINED_RULES: PhaseII predictor + BioTransformer""", defaultValue = "BT_RULE_BASED", order = 315)
    private P2Mode p2Mode;

    //todo should we make some hack in biotransformer to remove dependency on hmdb mysql online db. We disable ist for now per default
    @CommandLine.Option(names = "--useDB", description = "Retrieving from HMDB.", defaultValue = "false", order = 313)
    private boolean useDB;

    @CommandLine.Option(names = "--useSubstructure", description = "Please specify if you want use 2D structure (first 14 characters of InChIKey) when retrieving from database.", defaultValue = "true", hidden = true, order = 314)
    private boolean useSubstructure;

    @CommandLine.ArgGroup(multiplicity = "1", order = 309)
    public BioTransformer bioTransformer;


    public BioTransformerSettings toBioTransformerSetting() {
        BioTransformerSettings settings = new BioTransformerSettings()
                .setCyp450Mode(cyp450Mode)
                .setUseDB(useDB)
                .setUseSub(useSubstructure)
                .setP2Mode(p2Mode);

        if (bioTransformer.biotransformer != null) {
            settings.setMetabolicTransformation(bioTransformer.biotransformer.metabolicTransformation)
                    .setIterations(bioTransformer.biotransformer.iterations);
        } else if (bioTransformer.bioTransformerSequence != null) {
            settings.setMetabolicTransformation(MetabolicTransformation.HUMAN_CUSTOM_MULTI);
            bioTransformer.bioTransformerSequence.forEach(s ->
                    settings.addSequenceStep(s.metabolicTransformation, s.iterations));
        } else {
            throw new IllegalArgumentException("Either a single/predefined metabolic transformation or a custom transformation sequence must be given.");
        }

        return settings;
    }


    public static class BioTransformer {
        @CommandLine.ArgGroup(exclusive = false, order = 310)
        Single biotransformer;

        @CommandLine.ArgGroup(exclusive = false, multiplicity = "1..5", order = 320)
        List<Sequence> bioTransformerSequence;
    }

    public static class Single {
        @CommandLine.Option(names = {"--transformation"}, completionCandidates = MetabolicTransformationSingleCandidates.class, required = true, order = 311)
        private MetabolicTransformation metabolicTransformation;


        @CommandLine.Option(names = {"--iterations"}, description = """
                The number of steps for the prediction. This option can be set by the
                 user for the EC-based, CYP450, Phase II, and Environmental microbial
                 biotransformers. The default value is 1.""", defaultValue = "1", parameterConsumer = RangeValidator.class, order = 312)
        private int iterations; // Number of iterations

    }


    // todo Check if all of this parameters should be changeable.
    // todo write validation method that checks whether parameter match transformation type.


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
        //TODO validation method, überprüfe ob transformationen versch. Iterations Max haben -> keine egrenzung im code-> nur in Webclient
    }

    public static class MetabolicTransformationSingleCandidates implements Iterable<String> {
        @Override
        public java.util.@NotNull Iterator<String> iterator() {
            return MetabolicTransformation.valueSingleOnly().stream().map(MetabolicTransformation::name).iterator();
        }
    }

    public static class MetabolicTransformationSequenceCandidates implements Iterable<String> {
        @Override
        public java.util.@NotNull Iterator<String> iterator() {
            return MetabolicTransformation.valueSequenceOnly()
                    .stream()
                    .map(MetabolicTransformation::name).iterator();
        }
    }

    public static class RangeValidator implements CommandLine.IParameterConsumer {
        @Override
        public void consumeParameters(Stack<String> args, CommandLine.Model.ArgSpec argSpec, CommandLine.Model.CommandSpec commandSpec) {
            String value = args.pop(); // Hole den Parameterwert
            int intValue = Integer.parseInt(value);

            // Bereich prüfen
            if (intValue < 1 || intValue > 4) {
                throw new CommandLine.ParameterException(
                        commandSpec.commandLine(),
                        String.format("Invalid value '%s' for option '%s': Value must be between 1 and 3.", value, argSpec.getValue())
                );
            }

            // Falls gültig, setze den Wert in der CommandLine-Option
            argSpec.setValue(intValue);
        }

    }

}
