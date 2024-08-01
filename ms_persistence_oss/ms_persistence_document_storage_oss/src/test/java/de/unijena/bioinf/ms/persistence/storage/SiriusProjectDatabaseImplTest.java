package de.unijena.bioinf.ms.persistence.storage;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.StandardFingerprintData;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.utils.ExFunctions;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.CompoundJsonMapper;
import de.unijena.bioinf.confidence_score.ExpansiveSearchConfidenceMode;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdduct;
import de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts;
import de.unijena.bioinf.ms.persistence.model.sirius.*;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.ms.properties.ConfigType;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import de.unijena.bioinf.storage.blob.Compressible;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import org.dizitart.no2.exceptions.UniqueConstraintException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class SiriusProjectDatabaseImplTest {
    private static void withDb(ExFunctions.Consumer<NitriteSirirusProject> projectConsumer) {
        try {
            Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
            try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
                projectConsumer.accept(ps);
            } finally {
                Files.deleteIfExists(location);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void withDb(String dbResource, ExFunctions.Consumer<NitriteSirirusProject> projectConsumer) {
        //prepare ->  copy database to not accidentally break in on error and to be sure that we can access a valid path
        Path path = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try {
            try (InputStream s = SiriusProjectDatabaseImplTest.class.getResourceAsStream(dbResource)) {
                Files.write(path, Objects.requireNonNull(s).readAllBytes());
            }

            try (NitriteSirirusProject ps = new NitriteSirirusProject(path)) {
                projectConsumer.accept(ps);
            } finally {
                Files.deleteIfExists(path);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testInitEmpty() {
        withDb(ps -> {
            assertTrue(ps.findFingerprintData(FingerIdData.class, 1).isEmpty());
            assertTrue(ps.findFingerprintData(FingerIdData.class, -1).isEmpty());
            assertTrue(ps.findFingerprintData(CanopusCfData.class, 1).isEmpty());
            assertTrue(ps.findFingerprintData(CanopusCfData.class, -1).isEmpty());
            assertTrue(ps.findFingerprintData(CanopusNpcData.class, 1).isEmpty());
            assertTrue(ps.findFingerprintData(CanopusNpcData.class, -1).isEmpty());
        });
    }

    @Test
    public void testInitWithFingerprintData() {
        withDb("/sirius-project-all-fp-data.sirius", ps -> {
            assertTrue(ps.findFingerprintData(FingerIdData.class, 1).isPresent());
            assertTrue(ps.findFingerprintData(FingerIdData.class, -1).isPresent());
            assertTrue(ps.findFingerprintData(CanopusCfData.class, 1).isPresent());
            assertTrue(ps.findFingerprintData(CanopusCfData.class, -1).isPresent());
            assertTrue(ps.findFingerprintData(CanopusNpcData.class, 1).isPresent());
            assertTrue(ps.findFingerprintData(CanopusNpcData.class, -1).isPresent());
        });
    }


    @ParameterizedTest
    @ValueSource(ints = {1, -1})
    public void testWriteFingerIdData(int charge) {
        withDb(db -> {
            final FingerIdData input;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/csi_fingerid.tsv"))))) {
                input = FingerIdData.read(r);
            }
            assertNotNull(input, "Creating input failed!");
            db.insertFingerprintData(input, charge);
            Optional<FingerIdData> out = db.findFingerprintData(FingerIdData.class, charge);
            assertTrue(out.isPresent());
            assertInstanceOf(FingerIdData.class, out.get());
            assertTrue(input.getFingerprintVersion().compatible(out.get().getFingerprintVersion()));
            assertArrayEquals(input.getPerformances(), out.get().getPerformances());
        });
    }

    private static Stream<Arguments> standardFingerprintData() {
        return Stream.of(
                Arguments.of(1, "/canopus.tsv", (IOFunctions.IOFunction<BufferedReader, StandardFingerprintData<?>>) CanopusCfData::read),
                Arguments.of(-1, "/canopus.tsv", (IOFunctions.IOFunction<BufferedReader, StandardFingerprintData<?>>) CanopusCfData::read),
                Arguments.of(1, "/canopus_npc.tsv", (IOFunctions.IOFunction<BufferedReader, StandardFingerprintData<?>>) CanopusNpcData::read),
                Arguments.of(-1, "/canopus_npc.tsv", (IOFunctions.IOFunction<BufferedReader, StandardFingerprintData<?>>) CanopusNpcData::read)
        );
    }

    @ParameterizedTest
    @MethodSource("standardFingerprintData")
    public void testWriteStandardFingerprintData(int charge, String file, IOFunctions.IOFunction<BufferedReader, StandardFingerprintData<?>> reader) {
        withDb(db -> {
            final StandardFingerprintData<?> input;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(file))))) {
                input = reader.apply(r);
            }
            assertNotNull(input, "Creating input failed!");

            db.insertFingerprintData(input, charge);
            Optional<StandardFingerprintData<?>> out = (Optional<StandardFingerprintData<?>>) db.findFingerprintData(input.getClass(), charge);
            assertTrue(out.isPresent());
            assertInstanceOf(input.getClass(), out.get());
            assertTrue(input.getFingerprintVersion().compatible(out.get().getFingerprintVersion()));
        });
    }

    @ParameterizedTest
    @MethodSource("standardFingerprintData")
    public void testWriteStandardFingerprintDataExists(int charge, String file, IOFunctions.IOFunction<BufferedReader, StandardFingerprintData<?>> reader) {
        withDb(db -> {
            final StandardFingerprintData<?> input;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(file))))) {
                input = reader.apply(r);
            }
            assertNotNull(input, "Creating input failed!");

            db.insertFingerprintData(input, charge);
            RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
                db.insertFingerprintData(input, charge);
            });

            assertInstanceOf(UniqueConstraintException.class, thrown.getCause());
        });
    }


    @Test
    public void crudFTreeTest() {
        withDb(db -> {
            //prepare
            String[] ftreeFiles = new String[]{"/trees/C20H17NO6_[M+H]+.json", "/trees/C18H19NO6_[M+Na]+.json"};
            FTree[] ftrees = new FTree[2];
            for (int i = 0; i < ftreeFiles.length; i++) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(ftreeFiles[0]))))) {
                    ftrees[i] = new FTJsonReader().parse(r);
                }
            }

            FTreeResult source = FTreeResult.builder().fTree(ftrees[0]).alignedFeatureId(1).formulaId(2).build();

            {   // insert and get
                db.getStorage().insert(source);
                Optional<FTreeResult> ftree = db.getStorage().getByPrimaryKey(source.getFormulaId(), FTreeResult.class);
                assertTrue(ftree.isPresent());
                assertEquals(ftrees[0].getTreeWeight(), ftree.map(FTreeResult::getFTree).map(FTree::getTreeWeight).get());
                assertEquals(ftrees[0].numberOfEdges(), ftree.map(FTreeResult::getFTree).map(FTree::numberOfEdges).get());
                assertEquals(ftrees[0].numberOfVertices(), ftree.map(FTreeResult::getFTree).map(FTree::numberOfVertices).get());
                assertEquals(2, ftree.map(FTreeResult::getFormulaId).get());
                assertEquals(1, ftree.map(FTreeResult::getAlignedFeatureId).get());
            }

            {   //modify and update
                source.setFTree(ftrees[1]);
                source.setFormulaId(22);
                db.getStorage().upsert(source);
                Optional<FTreeResult> ftree = db.getStorage().getByPrimaryKey(source.getFormulaId(), FTreeResult.class);
                assertTrue(ftree.isPresent());
                assertEquals(ftrees[1].getTreeWeight(), ftree.map(FTreeResult::getFTree).map(FTree::getTreeWeight).orElse(null));
                assertEquals(ftrees[1].numberOfEdges(), ftree.map(FTreeResult::getFTree).map(FTree::numberOfEdges).orElse(null));
                assertEquals(ftrees[1].numberOfVertices(), ftree.map(FTreeResult::getFTree).map(FTree::numberOfVertices).orElse(null));
                assertEquals(22, ftree.map(FTreeResult::getFormulaId).get());
                assertEquals(1, ftree.map(FTreeResult::getAlignedFeatureId).get());
            }

            {   //modify and update
                db.getStorage().remove(source);
                Optional<FTreeResult> ftree = db.getStorage().getByPrimaryKey(source.getFormulaId(), FTreeResult.class);
                assertTrue(ftree.isEmpty());
            }
        });
    }

    @Test
    public void crudFingerprintCandidateTest() {
        withDb("/sirius-project-all-fp-data.sirius", db -> {
            //prepare
            List<FingerprintCandidate> inputCompounds = Stream.of("/structures/C6H4ClN3.json.gz", "/structures/C47H75NO17.json.gz").flatMap(s -> {
                try (InputStream i = Objects.requireNonNull(getClass().getResourceAsStream(s))) {
                    List<FingerprintCandidate> compounds = new ArrayList<>();
                    try (final CloseableIterator<FingerprintCandidate> fciter = new CompoundJsonMapper().readFingerprints(CdkFingerprintVersion.getDefault(),
                            Compressible.decompressRawStream(i, Compressible.Compression.GZIP).orElse(null))) {
                        while (fciter.hasNext())
                            compounds.add(fciter.next());
                        return compounds.stream();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).toList();

            //test
            {   // insert and get
                db.getStorage().insertAll(inputCompounds);
                assertEquals(db.getStorage().countAll(FingerprintCandidate.class), inputCompounds.size());
                for (FingerprintCandidate source : inputCompounds) {
                    Optional<FingerprintCandidate> compound = db.getStorage().getByPrimaryKey(source.getInchiKey2D(), FingerprintCandidate.class, "fingerprint");
                    assertTrue(compound.isPresent());
                    assertArrayEquals(source.getFingerprint().toIndizesArray(), compound.map(FingerprintCandidate::getFingerprint).map(Fingerprint::toIndizesArray).orElse(null));

                    assertEquals(source.getLinks(), compound.map(FingerprintCandidate::getLinks).orElse(null));
                    assertEquals(source.getName(), compound.map(CompoundCandidate::getName).orElse(null));
                }
            }

            //insert duplicate fil
            {
                RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () ->
                        db.getStorage().insert(inputCompounds.iterator().next()));
                assertInstanceOf(UniqueConstraintException.class, thrown.getCause());
            }


            {   //modify and update success
                FingerprintCandidate source = inputCompounds.iterator().next();
                source.setName("TEST NAME");
                DBLink testLink = new DBLink("TEST_DB", "0815");
                source.mergeDBLinks(List.of(testLink));
                db.getStorage().upsert(source);
                Optional<FingerprintCandidate> compound = db.getStorage().getByPrimaryKey(source.getInchiKey2D(), FingerprintCandidate.class, "fingerprint");
                assertTrue(compound.isPresent());
                //test for changes
                assertEquals(source.getName(), compound.map(FingerprintCandidate::getName).orElse(null));
                assertTrue(compound.map(FingerprintCandidate::getLinks).orElseThrow().contains(testLink));
                //test if other still correct
                assertArrayEquals(source.getFingerprint().toIndizesArray(), compound.map(FingerprintCandidate::getFingerprint).map(Fingerprint::toIndizesArray).orElse(null));
                assertEquals(source.getLinks(), compound.map(FingerprintCandidate::getLinks).orElse(null));
                assertEquals(source.getName(), compound.map(CompoundCandidate::getName).orElse(null));
            }

            {   //modify and update
                db.getStorage().removeAll(inputCompounds);
                for (FingerprintCandidate source : inputCompounds) {
                    Optional<FingerprintCandidate> compound = db.getStorage().getByPrimaryKey(source.getInchiKey2D(), FingerprintCandidate.class);
                    assertTrue(compound.isEmpty());
                }
            }
        });
    }

    @Test
    public void upsertAdductOnFeatureTest() {
        withDb("/sirius-project-features.sirius", db -> {
            //prepare
            AlignedFeatures feature = db.getStorage().findAllStr(AlignedFeatures.class).findFirst().orElseThrow();
            assertNull(feature.getDetectedAdducts());

            DetectedAdducts adducts = new DetectedAdducts().add(
                    DetectedAdduct.builder().adduct(PrecursorIonType.fromString("[M+H]+")).score(.6)
                            .source(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.MS1_PREPROCESSOR).build(),
                    DetectedAdduct.builder().adduct(PrecursorIonType.fromString("[M-H20+H]+")).score(.3)
                            .source(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.MS1_PREPROCESSOR).build(),
                    DetectedAdduct.builder().adduct(PrecursorIonType.fromString("[M+Na]+")).score(.1)
                            .source(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.MS1_PREPROCESSOR).build(),
                    DetectedAdduct.builder().adduct(PrecursorIonType.fromString("[M+H]+")).score(.9)
                            .source(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.LCMS_ALIGN).build()
            );

            {//add adducts to feature
                feature.setDetectedAdducts(adducts);
                assertEquals(1, db.getStorage().upsert(feature), "Updated Feature with Adducts");
                assertEquals(feature.getDetectedAdducts(), db.getStorage().getByPrimaryKey(feature.getAlignedFeatureId(), feature.getClass()).map(AlignedFeatures::getDetectedAdducts).orElse(null));
            }

            {//modify adducts on feature
                feature.getDetectedAdducts().remove(PrecursorIonType.fromString("[M-H20+H]+"));
                assertEquals(1, db.getStorage().upsert(feature), "Remove adduct from Feature");
                assertEquals(feature.getDetectedAdducts(), db.getStorage().getByPrimaryKey(feature.getAlignedFeatureId(), feature.getClass()).map(AlignedFeatures::getDetectedAdducts).orElse(null));

                feature.getDetectedAdducts().removeBySource(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.LCMS_ALIGN);
                assertEquals(1, db.getStorage().upsert(feature), "Remove adduct by source from Feature");
                assertEquals(feature.getDetectedAdducts(), db.getStorage().getByPrimaryKey(feature.getAlignedFeatureId(), feature.getClass()).map(AlignedFeatures::getDetectedAdducts).orElse(null));
            }
        });
    }

    @Test
    public void crudParametersTest() {
        //prepare
        ParameterConfig projectConfig = PropertyManager.DEFAULTS.newIndependentInstance(ConfigType.PROJECT.name());
        ParameterConfig computeConfig = PropertyManager.DEFAULTS.newIndependentInstance(ConfigType.BATCH_COMPUTE.name());

        Parameters project1 = Parameters.of(projectConfig, ConfigType.PROJECT, true);
        project1.setAlignedFeatureId(1);
        Parameters project2 = Parameters.of(projectConfig, ConfigType.PROJECT, true);
        project2.setAlignedFeatureId(1);
        Parameters input = Parameters.of(computeConfig, ConfigType.BATCH_COMPUTE, true);
        project2.setAlignedFeatureId(1);

        withDb(db -> {
            //insert
            assertEquals(1, db.getStorage().insert(project1), "Insert project config");
            assertTrue(db.getStorage().getByPrimaryKey(project1.getParametersId(), project1.getClass()).isPresent(), "check if inserted config exists");

            Set<String> expected = new HashSet<>();
            project1.newParameterConfig().getConfigKeys().forEachRemaining(expected::add);

            Set<String> actual = new HashSet<>();
            db.getStorage().getByPrimaryKey(project1.getParametersId(), project1.getClass())
                    .map(Parameters::newParameterConfig).map(ParameterConfig::getConfigKeys)
                    .ifPresent(it -> it.forEachRemaining(actual::add));
            assertEquals(expected, actual, "Check if content has been preserved");

            //fail duplicate entry
            RuntimeException thrown = assertThrowsExactly(RuntimeException.class, () -> db.getStorage().insert(project2));
            assertInstanceOf(UniqueConstraintException.class, thrown.getCause());

            //insert second type same feature
            assertEquals(1, db.getStorage().insert(input), "Insert input config");
            assertTrue(db.getStorage().getByPrimaryKey(input.getParametersId(), input.getClass()).isPresent(), "check if inserted config exists");
            assertEquals(2, db.getStorage().countAll(Parameters.class));

            //delete entry
            assertEquals(1, db.getStorage().remove(project1), "Delete project config");
            assertTrue(db.getStorage().getByPrimaryKey(project1.getParametersId(), project1.getClass()).isEmpty(), "check if deleted project not exists");
            assertEquals(1, db.getStorage().countAll(Parameters.class));
        });
    }

    @Test
    public void crudSpectraMatchTest() {
        //prepare
        SpectraMatch match1 = SpectraMatch.builder()
                .alignedFeatureId(10)
                .searchResult(SpectralSearchResult.SearchResult.builder()
                        .candidateInChiKey("MYWUZJCMWCOHBA")
                        .dbName("SpecLib-9000")
                        .dbId("42")
                        .similarity(SpectralSimilarity.builder().similarity(.9).sharedPeaks(23).build())
                        .smiles("C[C@@H](CC1=CC=CC=C1)NC")
                        .molecularFormula(MolecularFormula.parseOrThrow("C10H15N"))
                        .adduct(PrecursorIonType.fromString("[M+H]+"))
                        .exactMass(149.23)
                        .querySpectrumIndex(0)
                        .rank(1)
                        .build())
                .build();


        withDb(db -> {
            //insert
            assertEquals(1, db.getStorage().insert(match1), "Failed to Insert match. Affected entries wrong");
            assertTrue(db.getStorage().getByPrimaryKey(match1.getSpecMatchId(), match1.getClass()).isPresent(), "Inserted match does not exist!");
            assertEquals(match1.getSimilarity(), db.getStorage().getByPrimaryKey(match1.getSpecMatchId(), match1.getClass()).get().getSimilarity());
            assertEquals(match1.getAdduct(), db.getStorage().getByPrimaryKey(match1.getSpecMatchId(), match1.getClass()).get().getAdduct());
            assertEquals(match1.getMolecularFormula(), db.getStorage().getByPrimaryKey(match1.getSpecMatchId(), match1.getClass()).get().getMolecularFormula());
            {
                //fail duplicate entry
                RuntimeException thrown = assertThrowsExactly(RuntimeException.class, () -> db.getStorage().insert(match1));
                assertInstanceOf(UniqueConstraintException.class, thrown.getCause());
            }

            //delete entry
            assertEquals(1, db.getStorage().remove(match1), "Delete match failed.");
            assertTrue(db.getStorage().getByPrimaryKey(match1.getSpecMatchId(), match1.getClass()).isEmpty(), "Match still exists after delete");
            assertEquals(0, db.getStorage().countAll(SpectraMatch.class));
        });
    }

    @Test
    public void crudCsiMatchTest() {
        //prepare
        CsiStructureMatch match1 = CsiStructureMatch.builder()
                .formulaId(1)
                .alignedFeatureId(10)
                .candidateInChiKey("BQJCRHHNABKAKU")
                .csiScore(-20.008d)
                .tanimotoSimilarity(0.97)
                .mcesDistToTopHit(1.5d)
                .build();

        withDb(db -> {
            //insert
            assertEquals(1, db.getStorage().insert(match1), "Failed to Insert match. Affected entries wrong");
            assertTrue(db.getStorage().getByPrimaryKey(match1.getId(), match1.getClass()).isPresent(), "Inserted match does not exist!");
            assertEquals(0.97d, db.getStorage().getByPrimaryKey(match1.getId(), match1.getClass()).map(CsiStructureMatch::getTanimotoSimilarity).orElse(null));
            assertEquals(1.5d, db.getStorage().getByPrimaryKey(match1.getId(), match1.getClass()).map(CsiStructureMatch::getMcesDistToTopHit).orElse(null));
            assertEquals(-20.008d, db.getStorage().getByPrimaryKey(match1.getId(), match1.getClass()).map(CsiStructureMatch::getCsiScore).orElse(null));

            {
                //fail duplicate entry
                RuntimeException thrown = assertThrowsExactly(RuntimeException.class, () -> db.getStorage().insert(match1));
                assertInstanceOf(UniqueConstraintException.class, thrown.getCause());
            }
            {
                //fail with null key
                assertThrowsExactly(IOException.class, () -> db.getStorage().insert(DenovoStructureMatch.builder()));
            }
            //delete entry
            assertEquals(1, db.getStorage().remove(match1), "Delete match failed.");
            assertTrue(db.getStorage().getByPrimaryKey(match1.getId(), match1.getClass()).isEmpty(), "Match still exists after delete");
            assertEquals(0, db.getStorage().countAll(CsiStructureMatch.class));
        });
    }

    @Test
    public void crudDeNovoMatchTest() {
        //prepare
        DenovoStructureMatch match1 = DenovoStructureMatch.builder()
                .formulaId(1)
                .alignedFeatureId(10)
                .candidateInChiKey("BQJCRHHNABKAKU")
                .csiScore(-20.008d)
                .tanimotoSimilarity(0.97)
                .modelScore(9000d)
                .build();

        withDb(db -> {
            //insert
            assertEquals(1, db.getStorage().insert(match1), "Failed to Insert match. Affected entries wrong");
            assertTrue(db.getStorage().getByPrimaryKey(match1.getId(), match1.getClass()).isPresent(), "Inserted match does not exist!");
            assertEquals(0.97d, db.getStorage().getByPrimaryKey(match1.getId(), match1.getClass()).map(DenovoStructureMatch::getTanimotoSimilarity).orElse(null));
            assertEquals(9000d, db.getStorage().getByPrimaryKey(match1.getId(), match1.getClass()).map(DenovoStructureMatch::getModelScore).orElse(null));
            assertEquals(-20.008d, db.getStorage().getByPrimaryKey(match1.getId(), match1.getClass()).map(DenovoStructureMatch::getCsiScore).orElse(null));

            {
                //fail duplicate entry
                RuntimeException thrown = assertThrowsExactly(RuntimeException.class, () -> db.getStorage().insert(match1));
                assertInstanceOf(UniqueConstraintException.class, thrown.getCause());
            }
            {
                //fail with null key
                assertThrowsExactly(IOException.class, () -> db.getStorage().insert(DenovoStructureMatch.builder()));
            }
            //delete entry
            assertEquals(1, db.getStorage().remove(match1), "Delete match failed.");
            assertTrue(db.getStorage().getByPrimaryKey(match1.getId(), match1.getClass()).isEmpty(), "Match still exists after delete");
            assertEquals(0, db.getStorage().countAll(DenovoStructureMatch.class));
        });
    }

    @Test
    public void crudCsiStructureSearchResultTest() {
        CsiStructureSearchResult result = CsiStructureSearchResult.builder()
                .confidenceApprox(.9)
                .confidenceExact(.81)
                .alignedFeatureId(10)
                .expansiveSearchConfidenceMode(ExpansiveSearchConfidenceMode.Mode.APPROXIMATE)
                .specifiedDatabases(List.of("HMDB", "COCONUT"))
                .expandedDatabases(List.of("PUBCHEM"))
                .matches(List.of(CsiStructureMatch.builder().build()))
                .build();

        withDb(db -> {
            //insert
            assertEquals(1, db.getStorage().insert(result), "Failed to Insert result. Affected entries wrong");
            assertTrue(db.getStorage().getByPrimaryKey(result.getAlignedFeatureId(), result.getClass()).isPresent(), "Inserted result does not exist!");
            assertEquals(.9, db.getStorage().getByPrimaryKey(result.getAlignedFeatureId(), result.getClass()).map(CsiStructureSearchResult::getConfidenceApprox).orElse(null));
            assertEquals(.81, db.getStorage().getByPrimaryKey(result.getAlignedFeatureId(), result.getClass()).map(CsiStructureSearchResult::getConfidenceExact).orElse(null));
            assertEquals(10, db.getStorage().getByPrimaryKey(result.getAlignedFeatureId(), result.getClass()).map(CsiStructureSearchResult::getAlignedFeatureId).orElse(null));
            assertNull(db.getStorage().getByPrimaryKey(result.getAlignedFeatureId(), result.getClass()).get().getMatches(), "Match should not have been serialized");

            {
                //fail duplicate entry
                RuntimeException thrown = assertThrowsExactly(RuntimeException.class, () -> db.getStorage().insert(result));
                assertInstanceOf(UniqueConstraintException.class, thrown.getCause());
            }
            {
                //fail with null key
                assertThrowsExactly(IOException.class, () -> db.getStorage().insert(CsiStructureSearchResult.builder()));
            }
            //delete entry
            assertEquals(1, db.getStorage().remove(result), "Delete match failed.");
            assertTrue(db.getStorage().getByPrimaryKey(result.getAlignedFeatureId(), result.getClass()).isEmpty(), "Match still exists after delete");
            assertEquals(0, db.getStorage().countAll(CsiStructureSearchResult.class));
        });
    }

    @Test
    public void csiStructureSearchResultMatchJoinTest() {
        //prepare
        CsiStructureMatch match1 = CsiStructureMatch.builder().formulaId(1).alignedFeatureId(10)
                .candidateInChiKey("BQJCRHHNABKAKU").csiScore(-20.008d).tanimotoSimilarity(0.97).mcesDistToTopHit(0d)
                .build();
        CsiStructureMatch match2 = CsiStructureMatch.builder().formulaId(1).alignedFeatureId(10)
                .candidateInChiKey("ZPUCINDJVBIVPJ").csiScore(-297.5d).tanimotoSimilarity(0.68).mcesDistToTopHit(Double.NaN)
                .build();
        CsiStructureMatch noMatch1 = CsiStructureMatch.builder().formulaId(1).alignedFeatureId(99)
                .candidateInChiKey("MYWUZJCMWCOHBA").csiScore(-297.5d).tanimotoSimilarity(0.68).mcesDistToTopHit(Double.NaN)
                .build();

        CsiStructureSearchResult result = CsiStructureSearchResult.builder().confidenceApprox(.9).confidenceExact(.81)
                .alignedFeatureId(10).expansiveSearchConfidenceMode(ExpansiveSearchConfidenceMode.Mode.APPROXIMATE)
                .specifiedDatabases(List.of("HMDB", "COCONUT")).expandedDatabases(List.of("PUBCHEM"))
                .matches(List.of(match1, match2))
                .build();

        withDb(db -> {
            //insert result without matches
            assertEquals(1, db.getStorage().insert(result), "Failed to Insert result. Affected entries wrong");
            assertTrue(db.getStorage().getByPrimaryKey(result.getAlignedFeatureId(), result.getClass()).isPresent(), "Inserted result does not exist!");
            assertNull(db.getStorage().getByPrimaryKey(result.getAlignedFeatureId(), result.getClass()).get().getMatches(), "Match should not have been serialized");

            //insert matches
            assertEquals(3, db.getStorage().insertAll(List.of(match1, match2, noMatch1)), "Failed to Insert matches. (Preparation)");

            // finde result with matches.
            {
                Optional<CsiStructureSearchResult> r = db.findCsiStructureSearchResult(result.getAlignedFeatureId(), true);
                assertTrue(r.isPresent(), "Inserted result does not exist!");
                assertNotNull(r.get().getMatches(), "Match should has not been extracted");
                assertEquals(result.getMatches().size(), r.get().getMatches().size(), "Unexpected number of joined matches!");
            }
            //delete match
            {
                assertEquals(1, db.getStorage().remove(match1), "Delete match failed.");
                assertEquals(2, db.getStorage().countAll(CsiStructureMatch.class));
                Optional<CsiStructureSearchResult> r = db.findCsiStructureSearchResult(result.getAlignedFeatureId(), true);
                assertNotNull(r.get().getMatches(), "Match should has not been extracted");
                assertEquals(1, r.get().getMatches().size(), "Unexpected number of joined matches!");
            }
        });
    }

    private static Stream<Arguments> peakListData() {
        return Stream.of(
                Arguments.of("/peaklists/Bicuculline_Unknown.ms", 1, true, false),
                Arguments.of("/peaklists/ForTox_TestMix_AMSMS_MFE-MS2Extr.cef", 1, true, true),
                Arguments.of("/peaklists/laudanosine.mgf", 4, true, false),
                Arguments.of("/peaklists/ForTox_TestMix_TMSMS_multi_msms.cef", 3, true, true)
        );
    }

    @ParameterizedTest
    @MethodSource("peakListData")
    public void importFeaturesFromMs2ExperimentTest(String inputFile, int expectedMsMs, boolean ms1, boolean rt) {
        withDb(db -> {
            try (InputStream in = Objects.requireNonNull(SiriusProjectDatabaseImplTest.class.getResourceAsStream(inputFile))) {
                CloseableIterator<Ms2Experiment> it = new MsExperimentParser().getParser(inputFile).parseIterator(in, URI.create(inputFile));
                while (it.hasNext()) {
                    Ms2Experiment exp = it.next();

                    AlignedFeatures feature = db.importMs2ExperimentAsAlignedFeature(exp);
                    {
                        assertNotNull(feature);
                        assertEquals(expectedMsMs, feature.getMSData().get().getMsnSpectra().size());
                        assertEquals(ms1, feature.getMSData().get().getMergedMs1Spectrum() != null);
                        assertEquals(ms1, feature.getMSData().get().getIsotopePattern() != null);
                        assertTrue(Math.abs(exp.getIonMass() - feature.getAverageMass()) < .0001);
                        assertEquals(exp.getPrecursorIonType().getCharge(), feature.getCharge());
                        assertEquals(exp.getName(), feature.getName());
                        assertEquals(exp.getFeatureId(), feature.getExternalFeatureId());
                        assertEquals(exp.getMolecularFormula(), feature.getMolecularFormula());
                        if (rt)
                            assertEquals(0, exp.getAnnotation(RetentionTime.class).get().compareTo(feature.getRetentionTime()));
                        else
                            assertNull(feature.getRetentionTime());
                    }

                    {
                        assertTrue(db.getStorage().getByPrimaryKey(feature.getAlignedFeatureId(), AlignedFeatures.class).isPresent());
                        AlignedFeatures extrFeature = db.getStorage().getByPrimaryKey(feature.getAlignedFeatureId(), AlignedFeatures.class).get();
                        assertTrue(extrFeature.getMSData().isEmpty()); // data is stored in separate collection
                        // fetch ms data
                        db.fetchMsData(extrFeature);
                        assertTrue(extrFeature.getMSData().isPresent());
                        assertEquals(expectedMsMs, extrFeature.getMSData().get().getMsnSpectra().size());
                        assertEquals(ms1, extrFeature.getMSData().get().getMergedMs1Spectrum() != null);
                        assertEquals(ms1, extrFeature.getMSData().get().getIsotopePattern() != null);

                        assertTrue(Math.abs(exp.getIonMass() - extrFeature.getAverageMass()) < .0001);
                        assertEquals(exp.getPrecursorIonType().getCharge(), extrFeature.getCharge());
                        assertEquals(exp.getName(), extrFeature.getName());
                        assertEquals(exp.getFeatureId(), extrFeature.getExternalFeatureId());
                        assertEquals(exp.getMolecularFormula(), extrFeature.getMolecularFormula());
                        if (rt)
                            assertEquals(0, exp.getAnnotation(RetentionTime.class).get().compareTo(extrFeature.getRetentionTime()));
                        else
                            assertNull(extrFeature.getRetentionTime());
                    }

                    {
                        assertTrue(db.findAlignedFeatureAsMsExperiment(feature.getAlignedFeatureId()).isPresent());
                        Ms2Experiment extrExp = db.findAlignedFeatureAsMsExperiment(feature.getAlignedFeatureId()).get();
                        assertEquals(expectedMsMs, extrExp.getMs2Spectra().size());
                        assertEquals(ms1, extrExp.getMs1Spectra() != null && !extrExp.getMs1Spectra().isEmpty());
                        assertTrue(Math.abs(exp.getIonMass() - extrExp.getIonMass()) < .0001);
                        assertEquals(exp.getPrecursorIonType().getCharge(), extrExp.getPrecursorIonType().getCharge());
                        assertEquals(exp.getName(), extrExp.getName());
                        assertEquals(exp.getFeatureId(), extrExp.getFeatureId());
                        assertEquals(exp.getMolecularFormula(), extrExp.getMolecularFormula());
                        assertEquals(rt, extrExp.hasAnnotation(RetentionTime.class));
                        if (rt)
                            assertEquals(0, exp.getAnnotation(RetentionTime.class).get().compareTo(extrExp.getAnnotation(RetentionTime.class).get()));
                    }
                }
            }
        });
    }

    @ParameterizedTest
    @MethodSource("peakListData")
    public void cascadeDeleteTest(String inputFile, int expectedMsMs, boolean ms1, boolean rt) {
        withDb(db -> {
            try (InputStream in = Objects.requireNonNull(SiriusProjectDatabaseImplTest.class.getResourceAsStream(inputFile))) {
                CloseableIterator<Ms2Experiment> it = new MsExperimentParser().getParser(inputFile).parseIterator(in, URI.create(inputFile));
                while (it.hasNext()) {
                    Ms2Experiment exp = it.next();

                    AlignedFeatures feature = db.importMs2ExperimentAsAlignedFeature(exp);

                    Compound compound = db.getStorage().getByPrimaryKey(feature.getCompoundId(), Compound.class).orElseThrow();

                    assertTrue(db.cascadeDeleteCompound(compound.getCompoundId()) > 0);
                    for (Class<?> clazz : db.getStorage().getAllRegisteredClasses()) {
                        assertEquals(0, db.getStorage().countAll(clazz));
                    }
                }
            }
        });
    }

//    @Test
//    ~12k features ~3G data for performance testing
    public void importFeaturesFromManyMs2ExperimentTest() {
        Path inputFile = Path.of("/home/fleisch/sirius-testing/demo/louis_OMZ_ETNP/OMZ_ETNP_SIRIUS_nobatch.mgf");
        try {
            Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
            try (NitriteSirirusProject db = new NitriteSirirusProject(location)) {
                try (InputStream in = Files.newInputStream(inputFile)) {
                    CloseableIterator<Ms2Experiment> it = new MsExperimentParser().getParser(inputFile).parseIterator(in, inputFile.toUri());
                    int i = 0;
                    while (it.hasNext()) {
                        Ms2Experiment exp = it.next();
                        AlignedFeatures feature = db.importMs2ExperimentAsAlignedFeature(exp);
                        assertNotNull(feature);
                        assertTrue(db.getStorage().getByPrimaryKey(feature.getAlignedFeatureId(), AlignedFeatures.class).isPresent());

                        System.out.println("Imported Feature: " + i++);
                    }
                    long count = db.getStorage().countAll(AlignedFeatures.class);
                    System.out.println("Imported: " + count);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {

        final FingerIdData csi;
        final CanopusCfData cf;
        final CanopusNpcData npc;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(Objects.requireNonNull(SiriusProjectDatabaseImplTest.class.getResourceAsStream("/csi_fingerid.tsv"))))) {
            csi = FingerIdData.read(r);
        }

        try (BufferedReader r = new BufferedReader(new InputStreamReader(Objects.requireNonNull(SiriusProjectDatabaseImplTest.class.getResourceAsStream("/canopus.tsv"))))) {
            cf = CanopusCfData.read(r);
        }

        try (BufferedReader r = new BufferedReader(new InputStreamReader(Objects.requireNonNull(SiriusProjectDatabaseImplTest.class.getResourceAsStream("/canopus_npc.tsv"))))) {
            npc = CanopusNpcData.read(r);
        }

        int count = 0;
        while (count++ < 1000) {
            Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
//          Path location = FileUtils.createTmpProjectSpaceLocation(null);
            try {
                Utils.withTime("Run '" + count + "' took: ", w -> {
                    try {
                        try (NitriteSirirusProject db = new NitriteSirirusProject(location)) {
                            {
                                db.insertFingerprintData(csi, 1);
                                Optional<FingerIdData> out = db.findFingerprintData(FingerIdData.class, 1);
                                assertTrue(out.isPresent());
                                assertInstanceOf(FingerIdData.class, out.get());
                                assertTrue(csi.getFingerprintVersion().compatible(out.get().getFingerprintVersion()));
                                assertArrayEquals(csi.getPerformances(), out.get().getPerformances());
                            }

                            {
                                db.insertFingerprintData(csi, -1);
                                Optional<FingerIdData> out = db.findFingerprintData(FingerIdData.class, -1);
                                assertTrue(out.isPresent());
                                assertInstanceOf(FingerIdData.class, out.get());
                                assertTrue(csi.getFingerprintVersion().compatible(out.get().getFingerprintVersion()));
                                assertArrayEquals(csi.getPerformances(), out.get().getPerformances());
                            }

                            {
                                db.insertFingerprintData(cf, 1);
                                Optional<CanopusCfData> out1 = db.findFingerprintData(CanopusCfData.class, 1);
                                assertTrue(out1.isPresent());
                                assertInstanceOf(CanopusCfData.class, out1.get());
                                assertTrue(cf.getFingerprintVersion().compatible(out1.get().getFingerprintVersion()));
                            }

                            {
                                db.insertFingerprintData(cf, -1);
                                Optional<CanopusCfData> out1 = db.findFingerprintData(CanopusCfData.class, -1);
                                assertTrue(out1.isPresent());
                                assertInstanceOf(CanopusCfData.class, out1.get());
                                assertTrue(cf.getFingerprintVersion().compatible(out1.get().getFingerprintVersion()));
                            }

                            {
                                db.insertFingerprintData(npc, 1);
                                Optional<CanopusNpcData> out1 = db.findFingerprintData(CanopusNpcData.class, 1);
                                assertTrue(out1.isPresent());
                                assertInstanceOf(CanopusNpcData.class, out1.get());
                                assertTrue(npc.getFingerprintVersion().compatible(out1.get().getFingerprintVersion()));
                            }
                            {
                                db.insertFingerprintData(npc, -1);
                                Optional<CanopusNpcData> out1 = db.findFingerprintData(CanopusNpcData.class, -1);
                                assertTrue(out1.isPresent());
                                assertInstanceOf(CanopusNpcData.class, out1.get());
                                assertTrue(npc.getFingerprintVersion().compatible(out1.get().getFingerprintVersion()));
                            }

                        }
                        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
                            assertTrue(ps.findFingerprintData(FingerIdData.class, 1).isPresent());
                            assertTrue(ps.findFingerprintData(FingerIdData.class, -1).isPresent());
                            assertTrue(ps.findFingerprintData(CanopusCfData.class, 1).isPresent());
                            assertTrue(ps.findFingerprintData(CanopusCfData.class, -1).isPresent());
                            assertTrue(ps.findFingerprintData(CanopusNpcData.class, 1).isPresent());
                            assertTrue(ps.findFingerprintData(CanopusNpcData.class, -1).isPresent());
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

            } finally {
                FileUtils.deleteRecursively(location);
            }
        }
    }
}
