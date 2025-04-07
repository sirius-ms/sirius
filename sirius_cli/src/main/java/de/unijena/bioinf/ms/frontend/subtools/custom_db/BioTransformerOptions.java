package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.ms.biotransformer.BioTransformerSettings;
import de.unijena.bioinf.ms.biotransformer.Cyp450Mode;
import de.unijena.bioinf.ms.biotransformer.MetabolicTransformation;
import de.unijena.bioinf.ms.biotransformer.P2Mode;
import biotransformer.utils.BiotransformerSequenceStep;
import biotransformer.btransformers.Biotransformer;
//import org.gradle.internal.impldep.com.beust.jcommander.IParameterValidator;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import java.util.*;
// --transfromation HUMAN_CUSTOM_MULTI --seq-step CYP450 --seq-iterations 3  --seq-step PHASE_2 --seq-iterations 1

public class BioTransformerOptions {
    @CommandLine.ArgGroup(multiplicity = "1", exclusive = true, order = 301)
    public BioTransformer bioTransformer;

    @CommandLine.Option(names = "--cyp450Mode", description = "Specify the CYP450 predictoin Mode here: 1) CypReact + BioTransformer\n" +
            " rules; 2) CyProduct only; 3) Combined: CypReact + BioTransformer rules +\n" +
            " CyProducts.\n" +
            " Default mode is 1.", defaultValue = "RULE_BASED", order = 309)
    public Cyp450Mode cyp450Mode; // CYP450 Mode

    public BioTransformerSettings toBioTransformerSetting() {
        return BioTransformerSettings.builder()
                .cyp450Mode(cyp450Mode)
                .metabolicTransformation(bioTransformer.biotransformer.metabolicTransformation)
                .iterations(bioTransformer.biotransformer.iterations)
                .useDB(bioTransformer.biotransformer.useDB)
                .useSub(bioTransformer.biotransformer.useSubstructure)
                .p2Mode(bioTransformer.biotransformer.p2Mode.P2ModeOrdinal())
                .sequenceSteps((ArrayList<BiotransformerSequenceStep>) Optional.ofNullable(bioTransformer.bioTransformerSequence)
                        .orElse(Collections.emptyList())
                        .stream().map(sequence-> new BiotransformerSequenceStep(toBType(sequence.metabolicTransformation),sequence.iterations))
                        .toList())
                .build();
    }


    public static class BioTransformer {
        @CommandLine.ArgGroup(exclusive = false, heading = "### Single Transformer Options ###\n", order = 310)
        Single biotransformer;

        @CommandLine.ArgGroup(exclusive = false, multiplicity = "1..5", heading = "### Sequential Transformation Options ###\n", order = 320)
        List<Sequence> bioTransformerSequence;
    }

    public static class Single {
        @CommandLine.Option(names = {"--transformation"},completionCandidates = MetabolicTransformationSingleCandidates.class,required = true, order = 311)
        private MetabolicTransformation metabolicTransformation;

        @CommandLine.Option(names = "--p2Mode", description = "Specify the PhaseII predictoin Mode here: 1) BioTransformer rules; 2)\n" +
                " PhaseII predictor only; 3) Combined: PhaseII predictor + BioTransformer\n" +
                " rules.\n" +
                " Default mode is 1.\n", defaultValue = "BT_RULE_BASED"
        ,order = 315)
        private P2Mode p2Mode;

        @CommandLine.Spec
        private CommandLine.Model.CommandSpec spec; // for validation

        private void validate() {
            // Prüfen, ob --p2Mode gesetzt ist, wenn transformation ≠ PHASE_2
            if (p2Mode != null && metabolicTransformation != MetabolicTransformation.PHASE_2) {
                throw new CommandLine.ParameterException(
                        spec.commandLine(),
                        "--p2Mode is only allowed, when --transformation has the value: PHASE_2"
                );
            }
        }


        @CommandLine.Option(names = {"--iterations"}, description = "The number of steps for the prediction. This option can be set by the\n" +
                " user for the EC-based, CYP450, Phase II, and Environmental microbial\n" +
                " biotransformers. The default value is 1.", defaultValue = "1",parameterConsumer = RangeValidator.class, order = 312)
        private int iterations; // Number of iterations
        @CommandLine.Option(names = "--useDB", description = "Please specify if you want to enable the retrieving from database\n" +
                " feature.", defaultValue = "true", order = 313)
        private boolean useDB; // Use the database flag
        @CommandLine.Option(names = "--useSubstructure", description = "Please specify if you want to enable the using first\n" +
                " 14 characters of InChIKey when retrieving from database feature.", defaultValue = "false", order = 314)
        private boolean useSubstructure; // Use the substructure flag



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
        public java.util.@NotNull Iterator<String> iterator(){
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
            if (intValue < 1 || intValue > 3) {
                throw new CommandLine.ParameterException(
                        commandSpec.commandLine(),
                        String.format("Invalid value '%s' for option '%s': Value must be between 1 and 3.", value, argSpec.getValue())
                );
            }

            // Falls gültig, setze den Wert in der CommandLine-Option
            argSpec.setValue(intValue);
        }

    }

    public Biotransformer.bType toBType(MetabolicTransformation metabolicTransformation) {
        switch (metabolicTransformation){
            case PHASE_1_CYP450 -> {return Biotransformer.bType.CYP450;}
            case EC_BASED -> {return Biotransformer.bType.ECBASED;}
            case ENV_MICROBIAL -> {return Biotransformer.bType.ENV;}
            case HUMAN_GUT -> {return Biotransformer.bType.HGUT;}
            case PHASE_2 -> { return Biotransformer.bType.PHASEII;}
        }

        return null;
    }

/*    public static class BioTransformerParas {
        boolean useDB;
        boolean useSubstructure;
        MetabolicTransformation metabolicTransformation;
        int p2Mode;
        int cyp450Mode;
        int iterations;
    }
        public BioTransformerParas toBioTransformerParas() {
            BioTransformerParas paras = new BioTransformerParas();
            paras.useDB = bioTransformer.biotransformer.useDB;
            paras.useSubstructure = bioTransformer.biotransformer.useSubstructure;
            paras.metabolicTransformation = bioTransformer.biotransformer.metabolicTransformation;
            paras.p2Mode = bioTransformer.biotransformer.p2Mode.ordinal();
            paras.cyp450Mode = cyp450Mode.ordinal();
            paras.iterations = bioTransformer.biotransformer.iterations;

            return new BioTransformerParas();
        }
*/


}
