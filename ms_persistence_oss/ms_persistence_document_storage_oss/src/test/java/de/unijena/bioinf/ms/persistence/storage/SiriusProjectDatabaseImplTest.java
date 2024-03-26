package de.unijena.bioinf.ms.persistence.storage;
import de.unijena.bioinf.ChemistryBase.fp.StandardFingerprintData;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class SiriusProjectDatabaseImplTest {


    @Test
    public void testInitEmpty() throws IOException {
        Path path = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try {
            try (NitriteSirirusProject ps = new NitriteSirirusProject(path)) {
                assertTrue(ps.findFingerprintData(FingerIdData.class, 1).isEmpty());
                assertTrue(ps.findFingerprintData(FingerIdData.class, -1).isEmpty());
                assertTrue(ps.findFingerprintData(CanopusCfData.class, 1).isEmpty());
                assertTrue(ps.findFingerprintData(CanopusCfData.class, -1).isEmpty());
                assertTrue(ps.findFingerprintData(CanopusNpcData.class, 1).isEmpty());
                assertTrue(ps.findFingerprintData(CanopusNpcData.class, -1).isEmpty());
            }
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    public void testInitWithFingerprintData() throws IOException {
        //prepare ->  copy database to not accidentally break in on error and to be sure that we can access a valid path
        Path path = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try {
            try (InputStream s = getClass().getResourceAsStream("/sirius-project-all-fp-data.sirius")) {
                Files.write(path, Objects.requireNonNull(s).readAllBytes());
            }
            try (NitriteSirirusProject ps = new NitriteSirirusProject(path)) {
                assertTrue(ps.findFingerprintData(FingerIdData.class, 1).isPresent());
                assertTrue(ps.findFingerprintData(FingerIdData.class, -1).isPresent());
                assertTrue(ps.findFingerprintData(CanopusCfData.class, 1).isPresent());
                assertTrue(ps.findFingerprintData(CanopusCfData.class, -1).isPresent());
                assertTrue(ps.findFingerprintData(CanopusNpcData.class, 1).isPresent());
                assertTrue(ps.findFingerprintData(CanopusNpcData.class, -1).isPresent());
            }
        } finally {
            Files.deleteIfExists(path);
        }

    }


    @ParameterizedTest
    @ValueSource(ints = {1, -1})
    public void testWriteFingerIdData(int charge) throws IOException {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject db = new NitriteSirirusProject(location)) {
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
        } finally {
            Files.deleteIfExists(location);
        }
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
    public void testWriteStandardFingerprintData(int charge, String file, IOFunctions.IOFunction<BufferedReader, StandardFingerprintData<?>> reader) throws IOException {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject db = new NitriteSirirusProject(location)) {
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
        } finally {
            Files.deleteIfExists(location);
        }
    }

    @ParameterizedTest
    @MethodSource("standardFingerprintData")
    public void testWriteStandardFingerprintDataExists(int charge, String file, IOFunctions.IOFunction<BufferedReader, StandardFingerprintData<?>> reader) throws IOException {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject db = new NitriteSirirusProject(location)) {
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
        } finally {
            Files.deleteIfExists(location);
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
