package de.unijena.bioinf.ms.biotransformer;


import biotransformer.biosystems.BioSystem.BioSystemName;
import biotransformer.btransformers.Biotransformer;
import biotransformer.railsappspecific.*;
import biotransformer.transformation.Biotransformation;
import biotransformer.utils.BiotransformerSequenceStep;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.chemdb.InChISMILESUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class BioTransformerWrapper {

    public static BioTransformerResult abioticTransformer(IAtomContainer singleMolecule, int nrOfSteps) throws Exception {
        AbioticTransformer_rails abiotic_bt = new AbioticTransformer_rails(false, false);
        return toSiriusTransformations(singleMolecule, abiotic_bt.applyAbioticTransformationsChain(singleMolecule, true, true, nrOfSteps, 0.5d));
    }


    public static BioTransformerResult cyp450BTransformer(IAtomContainer singleMolecule, int nrOfSteps, Cyp450Mode cyp450Mode, boolean useDB, boolean useSub) throws Exception {
        Cyp450BTransformer_rails cyp450bt = new Cyp450BTransformer_rails(BioSystemName.HUMAN, useDB, useSub);
        return toSiriusTransformations(singleMolecule, cyp450bt.predictCyp450BiotransformationChainByMode(singleMolecule, true, true, nrOfSteps, 0.5d, cyp450Mode.getCyp450ModeOrdinal()));

    }

    public static BioTransformerResult ecBasedBTransformer(IAtomContainer singleMolecule, int nrOfSteps, boolean useDB, boolean useSub) throws Exception {
        ECBasedBTransformer_rails ecbt = new ECBasedBTransformer_rails(BioSystemName.HUMAN, useDB, useSub);
        return toSiriusTransformations(singleMolecule, ecbt.simulateECBasedMetabolismChain(singleMolecule, true, true, nrOfSteps, 0.5d));

    }

    public static BioTransformerResult hGutBTransformer(IAtomContainer singleMolecule, int nrOfSteps, boolean useDB, boolean useSub) throws Exception {
        HGutBTransformer_rails hgut = new HGutBTransformer_rails(useDB, useSub);
        return toSiriusTransformations(singleMolecule, hgut.simulateGutMicrobialMetabolism(singleMolecule, true, true, nrOfSteps, 0.5d));

    }

    public static BioTransformerResult phaseIIBTransformer(IAtomContainer singleMolecule, int nrOfSteps, int p2Mode, boolean useDB, boolean useSub) throws Exception {
        PhaseIIBTransformer_rails phase2b = new PhaseIIBTransformer_rails(BioSystemName.HUMAN, useDB, useSub);
        return toSiriusTransformations(singleMolecule, phase2b.applyPhase2TransformationsChainAndReturnBiotransformations(singleMolecule, true, true, true, nrOfSteps, 0.5d, p2Mode));

    }

    // SUPERBIO = ALL_HUMAN with iteration 4
    public static BioTransformerResult allHumanTransformer(IAtomContainer singleMolecule, int nrOfSteps, int p2Mode, Cyp450Mode cyp450Mode, boolean useDB, boolean useSub) throws Exception {
        SimulateHumanMetabolism_rails hsbt = new SimulateHumanMetabolism_rails(cyp450Mode.getCyp450ModeOrdinal(), p2Mode, useDB, "hmdb", false, 30, useSub);
        return toSiriusTransformations(singleMolecule, hsbt.simulateHumanMetabolism(singleMolecule, nrOfSteps));


    }

    public static BioTransformerResult envMicrobialTransformer(IAtomContainer singleMolecule, int nrOfSteps, boolean useDB, boolean useSub) throws Exception {
        EnvMicroBTransformer_rails ebt = new EnvMicroBTransformer_rails(useDB, useSub);
        return toSiriusTransformations(singleMolecule, ebt.applyEnvMicrobialTransformationsChain(singleMolecule, true, true, nrOfSteps, 0.5d));

    }

    public static BioTransformerResult multiBioTransformer(IAtomContainer singleMolecule, List<BioTransformerSequenceStep> transformerSeq, Cyp450Mode cyp450Mode, boolean useDB, boolean useSub) throws Exception {
        if (transformerSeq == null || transformerSeq.isEmpty())
            throw new IllegalArgumentException("sequenceSteps has to be set!");

        ArrayList<BiotransformerSequenceStep> bioTransSeq = transformerSeq.stream()
                .map(step ->
                        new BiotransformerSequenceStep(toBType(step.getMetabolicTransformation()), step.getIterations()))
                .collect(Collectors.toCollection(ArrayList::new));


        BiotransformerSequence_rails biotransformerSequence = new BiotransformerSequence_rails(bioTransSeq, useDB, useSub);
        return toSiriusTransformations(singleMolecule, biotransformerSequence.runSequence(singleMolecule, 0.5d, cyp450Mode.getCyp450ModeOrdinal()));
    }

    public static BioTransformerResult transform(IAtomContainer ia, BioTransformerSettings s) throws Exception {
        return switch (s.getMetabolicTransformation()) {
            case PHASE_1_CYP450 -> cyp450BTransformer(ia, s.getIterations(), s.getCyp450Mode(), s.isUseDB(), s.isUseSub());
            case EC_BASED -> ecBasedBTransformer(ia, s.getIterations(), s.isUseDB(), s.isUseSub());
            case PHASE_2 -> phaseIIBTransformer(ia, s.getIterations(), s.getP2Mode().P2ModeOrdinal(), s.isUseDB(), s.isUseSub());
            case HUMAN_GUT -> hGutBTransformer(ia, s.getIterations(), s.isUseDB(), s.isUseSub());
            case ENV_MICROBIAL -> envMicrobialTransformer(ia, s.getIterations(), s.isUseDB(), s.isUseSub());
            case ALL_HUMAN -> allHumanTransformer(ia, s.getIterations(), s.getP2Mode().P2ModeOrdinal(), s.getCyp450Mode(), s.isUseDB(), s.isUseSub());
            case ABIOTIC -> abioticTransformer(ia, s.getIterations());
            case HUMAN_CUSTOM_MULTI -> multiBioTransformer(ia, s.getSequenceSteps(), s.getCyp450Mode(), s.isUseDB(), s.isUseSub());
        };
    }

    private static BioTransformerResult toSiriusTransformations(@NotNull IAtomContainer originSubstrate, @Nullable Collection<Biotransformation> transformations) {
        if (transformations == null)
            return null;

        @Nullable String originInchiKey = getInchIFast(originSubstrate);

        final Map<String, Set<BioTransformation>> nonTerminalProductsByInchiKey = new HashMap<>(transformations.size());
        final List<BioTransformation> results = new ArrayList<>(transformations.size());

        for (Biotransformation bt : transformations) {
            if (bt.getProducts().isEmpty()) {
                log.warn("BioTransformation has no products! Skipping it.");
                continue;
            }

            if (bt.getSubstrates().isEmpty()) {
                log.warn("BioTransformation has no substrates! Skipping it.");
                continue;
            }

            if (bt.getSubstrates().getAtomContainerCount() > 1) {
                log.warn("Found more than one substrate in BioTransformation.");
            }

            if (bt.getProducts().getAtomContainerCount() > 1) {
                log.warn("Found more than one product in BioTransformation.");
            }

            final BioTransformation bT = toSiriusTransformation(bt);
            results.add(bT);

            // make product mapping
            for (IAtomContainer product : bT.getProducts()) {
                @Nullable String inchiKey = getInchIFast(product);
                if (inchiKey != null && !inchiKey.equals(originInchiKey) && notEndProduct(product))
                    nonTerminalProductsByInchiKey.computeIfAbsent(inchiKey, (k -> new LinkedHashSet<>())).add(bT);
            }
        }

        for (BioTransformation bT : results) {
            String substrateInchiKey = getInchIFast(bT.getSubstrate());
            if (nonTerminalProductsByInchiKey.containsKey(substrateInchiKey))
                bT.setPossibleSubstrateTransformations(new ArrayList<>(nonTerminalProductsByInchiKey.get(substrateInchiKey)));
        }

        return new BioTransformerResult(originSubstrate, results);
    }

    private static BioTransformation toSiriusTransformation(Biotransformation source) {
        BioTransformation.BioTransformationBuilder b = BioTransformation.builder()
                .enzymeNames(source.getEnzymeNames())
                .reactionType(source.getReactionType());

        if (source.getProducts() != null && !source.getProducts().isEmpty()) {
            Set<IAtomContainer> products = new HashSet<>();
            source.getProducts().atomContainers().forEach(products::add);
            b.products(products);
        }

        if (source.getSubstrates() != null && !source.getSubstrates().isEmpty())
            b.substrate(source.getSubstrates().atomContainers().iterator().next());

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

    private static boolean notEndProduct(IAtomContainer atomContainer) {
        return !isEndProduct(atomContainer);
    }

    private static boolean isEndProduct(IAtomContainer atomContainer) {
        return Boolean.TRUE.equals(atomContainer.getProperty("isEndProduct", Boolean.class));
    }


    @Nullable
    private static String getInchIFast(IAtomContainer atomContainer) {
        String inchikey = atomContainer.getProperty("InChIKey", String.class);
        if (Utils.isNullOrEmpty(inchikey))
            inchikey = atomContainer.getProperty("InChIKey".toUpperCase(), String.class);

        if (Utils.isNullOrEmpty(inchikey)) {
            try {
                InChI inchi = InChISMILESUtils.getInchi(atomContainer, true);
                if (inchi != null)
                    inchikey = inchi.key;
            } catch (CDKException e) {
                log.warn("Could not create InChIKey from BioTransformer result molecule. Skipping it. Cause: {}", e.getMessage());
            }
        }
        return inchikey;
    }
}
