package reactionTool.sirius;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.babelms.inputresource.StringInputResource;
import de.unijena.bioinf.chemdb.InChISMILESUtils;
import de.unijena.bioinf.chemdb.custom.*;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintCandidateWrapper;
import de.unijena.bioinf.fingerid.fingerprints.cache.IFingerprinterCache;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBPropertyUtils;
import de.unijena.bioinf.webapi.WebAPI;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.isomorphism.Transform;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.smirks.Smirks;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import reactionTool.sirius.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReactionToolHandler {

    private final IChemObjectBuilder builder;
    private final SmilesParser smilesParser;
    private final SmilesGenerator smilesGenerator;
    private final ObjectMapper objectMapper;
    private final WebAPI<?> api;
    private final IFingerprinterCache ifpCache;

    public ReactionToolHandler(WebAPI<?> api, IFingerprinterCache ifpCache) {
        this.builder = SilentChemObjectBuilder.getInstance();
        this.smilesParser = new SmilesParser(this.builder);
        this.smilesGenerator = new SmilesGenerator(SmiFlavor.Isomeric);
        this.objectMapper = new ObjectMapper();
        this.api = api;
        this.ifpCache = ifpCache;
    }

    public List<String> process(String jsonPath, List<String> initialSmiles) throws IOException {
            ReactionSequence sequence = objectMapper.readValue(new File(jsonPath), ReactionSequence.class);
        return process(sequence, initialSmiles);
    }

    public List<String> process(ReactionSequence sequence, List<String> initialSmiles) {
        Set<String> currentPool = new HashSet<>(initialSmiles);
        Set<String> allProducts = new HashSet<>(initialSmiles);

        for (Step step : sequence.getSteps()) {
            currentPool = executeStep(step, currentPool, allProducts);
        }

        return deduplicateSmiles(allProducts);
    }

    private Set<String> executeStep(Step step, Set<String> currentPool, Set<String> allProducts) {
        Set<String> result;
        if (step instanceof ParallelStep parallelStep) {
            result = executeParallelStep(parallelStep, currentPool);
        } else if (step instanceof LoopStep loopStep) {
            result = executeLoopStep(loopStep, currentPool, allProducts);
        } else {
            result = currentPool;
        }
        allProducts.addAll(result);
        return result;
    }

    private Set<String> executeParallelStep(ParallelStep step, Set<String> currentPool) {
        Set<String> nextPool = new HashSet<>();
        Set<String> transformedInThisStep = new HashSet<>();

        for (Reaction reaction : step.getReactions()) {
            System.out.println("Applying reaction: " + reaction.getName());
            for (String smiles : currentPool) {
                try {
                    IAtomContainer mol = smilesParser.parseSmiles(smiles);
                    AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
                    CDKHydrogenAdder.getInstance(mol.getBuilder()).addImplicitHydrogens(mol);

                    // Use Mode.All to get one product per matching site
                    Iterable<IAtomContainer> products = Smirks.apply(mol, reaction.getSmarts(), Transform.Mode.All);
                    
                    boolean matched = false;
                    for (IAtomContainer product : products) {
                        matched = true;

                        // Partition product into unconnected molecules
                        IAtomContainerSet components = ConnectivityChecker.partitionIntoMolecules(product);
                        for (IAtomContainer component : components.atomContainers()) {
                            for (IAtom atom : component.atoms()) {
                                atom.setImplicitHydrogenCount(-1);
                            }
                            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(component);
                            CDKHydrogenAdder.getInstance(component.getBuilder()).addImplicitHydrogens(component);

                            String productSmiles = smilesGenerator.create(component);
                            nextPool.add(productSmiles);
                        }
                    }
                    
                    if (matched) {
                        transformedInThisStep.add(smiles);
                    }
                } catch (Exception e) {
                }
            }
        }

        // Add molecules that didn't match ANY reaction in this parallel step
        for (String smiles : currentPool) {
            if (!transformedInThisStep.contains(smiles)) {
                nextPool.add(smiles);
            }
        }

        return nextPool;
    }

    private Set<String> executeLoopStep(LoopStep step, Set<String> currentPool, Set<String> allProducts) {
        Set<String> resultPool = new HashSet<>(currentPool);
        for (int i = 0; i < step.getIterations(); i++) {
            for (Step innerStep : step.getSteps()) {
                resultPool = executeStep(innerStep, resultPool, allProducts);
            }
        }
        return resultPool;
    }

    public List<String> extractSmiles(String databaseName) throws IOException {
        CustomDatabase db = CustomDatabases.getCustomDatabaseByName(databaseName)
                .orElseThrow(() -> new IllegalArgumentException("Custom database not found: " + databaseName));

        try (Stream<FingerprintCandidateWrapper> contents = CustomDatabases.getContents(db)) {
            return contents
                    .map(wrapper -> wrapper.getCandidate(null, null).getSmiles())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    private List<String> deduplicateSmiles(Collection<String> smiles) {
        Set<String> seenInchiKeys = new HashSet<>();
        List<String> deduplicatedSmiles = new ArrayList<>();

        for (String smile : smiles) {
            try {
                IAtomContainer mol = smilesParser.parseSmiles(smile);
                InChI inchi = InChISMILESUtils.getInchi(mol, false);
                if (inchi != null && inchi.key != null) {
                    if (seenInchiKeys.add(inchi.key)) {
                        deduplicatedSmiles.add(smile);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing smiles: " + smiles);
            }
        }
        return deduplicatedSmiles;
    }

    /**
     * Creates a new custom database with given name and imports the given smiles into it
     */
    public void createProductDatabase(List<String> smiles, String productDatabaseName) throws IOException {
        Path location = CustomDataSources.getCustomDatabaseDirectory().resolve(productDatabaseName + CustomDatabases.CUSTOM_DB_SUFFIX);
        CdkFingerprintVersion version = api.getCDKChemDBFingerprintVersion();
        CustomDatabaseSettings config = CustomDatabaseSettings.builder()
                .name(productDatabaseName)
                .usedFingerprints(List.of(version.getUsedFingerprints()))
                .schemaVersion(CustomDatabase.CUSTOM_DATABASE_SCHEMA)
                .statistics(new CustomDatabaseSettings.Statistics())
                .build();

        CustomDatabase productDb = CustomDatabases.create(location.toAbsolutePath().toString(), config, version, false);

        try {
            String tsvContent = String.join("\n", smiles);
            SiriusJobs.getGlobalJobManager().submitJob(CustomDatabaseImporter.makeImportToDatabaseJob(
                    null,
                    List.of(new StringInputResource(tsvContent, productDatabaseName, ".tsv")),
                    null,
                    (NoSQLCustomDatabase<?, ?>) productDb,
                    api,
                    ifpCache,
                    1000,
                    null
            )).takeResult();
        } finally {
            CustomDBPropertyUtils.addDB(location.toAbsolutePath().toString(), productDatabaseName);
        }
    }
}
