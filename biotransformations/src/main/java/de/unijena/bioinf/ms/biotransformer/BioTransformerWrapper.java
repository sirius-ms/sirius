package de.unijena.bioinf.ms.biotransformer;


import biotransformer.biosystems.BioSystem.BioSystemName;
import biotransformer.btransformers.Biotransformer;
import biotransformer.railsappspecific.*;
import biotransformer.transformation.Biotransformation;
import biotransformer.utils.BiotransformerSequenceStep;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.*;
import java.util.stream.Collectors;

public class BioTransformerWrapper {

    public static List<BioTransformation> abioticTransformer(IAtomContainer singleMolecule, int nrOfSteps) throws Exception {
        AbioticTransformer_rails abiotic_bt = new AbioticTransformer_rails(false, false);
        return toSiriusTransformations(abiotic_bt.applyAbioticTransformationsChain(singleMolecule, true, true, nrOfSteps, 0.5d));
    }


    public static List<BioTransformation> cyp450BTransformer(IAtomContainer singleMolecule, int nrOfSteps, Cyp450Mode cyp450Mode, boolean useDB, boolean useSub) throws Exception {
        Cyp450BTransformer_rails cyp450bt = new Cyp450BTransformer_rails(BioSystemName.HUMAN, useDB, useSub);
        return toSiriusTransformations(cyp450bt.predictCyp450BiotransformationChainByMode(singleMolecule, true, true, nrOfSteps, 0.5d, cyp450Mode.getCyp450ModeOrdinal()));

    }

    public static List<BioTransformation> ecBasedBTransformer(IAtomContainer singleMolecule, int nrOfSteps, boolean useDB, boolean useSub) throws Exception {
        ECBasedBTransformer_rails ecbt = new ECBasedBTransformer_rails(BioSystemName.HUMAN, useDB, useSub);
        return toSiriusTransformations(ecbt.simulateECBasedMetabolismChain(singleMolecule, true, true, nrOfSteps, 0.5d));

    }

    public static List<BioTransformation> hGutBTransformer(IAtomContainer singleMolecule, int nrOfSteps, boolean useDB, boolean useSub) throws Exception {
        HGutBTransformer_rails hgut = new HGutBTransformer_rails(useDB, useSub);
        return toSiriusTransformations(hgut.simulateGutMicrobialMetabolism(singleMolecule, true, true, nrOfSteps, 0.5d));

    }

    public static List<BioTransformation> phaseIIBTransformer(IAtomContainer singleMolecule, int nrOfSteps, int p2Mode, boolean useDB, boolean useSub) throws Exception {
        PhaseIIBTransformer_rails phase2b = new PhaseIIBTransformer_rails(BioSystemName.HUMAN, useDB, useSub);
        return toSiriusTransformations(phase2b.applyPhase2TransformationsChainAndReturnBiotransformations(singleMolecule, true, true, true, nrOfSteps, 0.5d, p2Mode));

    }

    // SUPERBIO =  ALL_HUMAN with iteration 4
    public static List<BioTransformation> allHumanTransformer(IAtomContainer singleMolecule, int nrOfSteps, int p2Mode, Cyp450Mode cyp450Mode, boolean useDB, boolean useSub) throws Exception {
        SimulateHumanMetabolism_rails hsbt = new SimulateHumanMetabolism_rails(cyp450Mode.getCyp450ModeOrdinal(), p2Mode, useDB, "hmdb", false, 30, useSub);
        return toSiriusTransformations(hsbt.simulateHumanMetabolism(singleMolecule, nrOfSteps));


    }

    public static List<BioTransformation> envMicrobialTransformer(IAtomContainer singleMolecule, int nrOfSteps, boolean useDB, boolean useSub) throws Exception {
        EnvMicroBTransformer_rails ebt = new EnvMicroBTransformer_rails(useDB, useSub);
        return toSiriusTransformations(ebt.applyEnvMicrobialTransformationsChain(singleMolecule, true, true, nrOfSteps, 0.5d));

    }

    public static List<BioTransformation> multiBioTransformer(IAtomContainer singleMolecule, List<BioTransformerSequenceStep> transformerSeq, Cyp450Mode cyp450Mode, boolean useDB, boolean useSub) throws Exception {
        if (transformerSeq == null || transformerSeq.isEmpty())
            throw new IllegalArgumentException("sequenceSteps has to be set!");

        ArrayList<BiotransformerSequenceStep> bioTransSeq = transformerSeq.stream()
                .map(step ->
                        new BiotransformerSequenceStep(toBType(step.getMetabolicTransformation()), step.getIterations()))
                .collect(Collectors.toCollection(ArrayList::new));


        BiotransformerSequence_rails biotransformerSequence = new BiotransformerSequence_rails(bioTransSeq, useDB, useSub);
        return toSiriusTransformations(biotransformerSequence.runSequence(singleMolecule, 0.5d, cyp450Mode.getCyp450ModeOrdinal()));

    }

    private static List<BioTransformation> toSiriusTransformations(@Nullable Collection<Biotransformation> sources){
        if (sources == null)
            return null;

        return sources.stream().map(BioTransformerWrapper::toSiriusTransformation).toList();
    }
    private static BioTransformation toSiriusTransformation(Biotransformation source){
        BioTransformation.BioTransformationBuilder b = BioTransformation.builder()
                .enzymeNames(source.getEnzymeNames())
                .reactionType(source.getReactionType());

        if (source.getProducts() != null){
            Set<IAtomContainer> products = new LinkedHashSet<>();
            source.getProducts().atomContainers().forEach(products::add);
            b.products(products);
        }

        if (source.getSubstrates() != null){
            Set<IAtomContainer> substrates = new LinkedHashSet<>();
            source.getSubstrates().atomContainers().forEach(substrates::add);
            b.substrates(substrates);
        }

        if (source.getBioSystemName() != null)
            b.bioSystemName(source.getBioSystemName().name());
        if (source.getScore() != null)
            b.score(source.getScore());
        return b.build();
    }
    private static Biotransformer.bType toBType(MetabolicTransformation metabolicTransformation) {
        return switch (metabolicTransformation) {
            case PHASE_1_CYP450 -> Biotransformer.bType.CYP450;
            case EC_BASED -> Biotransformer.bType.ECBASED;
            case ENV_MICROBIAL -> Biotransformer.bType.ENV;
            case HUMAN_GUT -> Biotransformer.bType.HGUT;
            case PHASE_2 -> Biotransformer.bType.PHASEII;
            default ->
                    throw new IllegalArgumentException("Unsupported metabolic transformation: " + metabolicTransformation);
        };
    }
}
