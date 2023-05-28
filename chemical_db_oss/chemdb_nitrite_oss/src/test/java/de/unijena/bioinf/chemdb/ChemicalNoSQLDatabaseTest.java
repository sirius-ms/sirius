/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.chemdb.nitrite.ChemicalNitriteDatabase;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintWrapper;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDatabase;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralData;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralMetadata;
import de.unijena.bioinf.storage.blob.file.FileBlobStorage;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class ChemicalNoSQLDatabaseTest {

    static ChemicalNitriteDatabase chemDb;
    static MolecularFormula[] formulas;

    @BeforeClass
    public static void importData() throws IOException {

        ChemicalBlobDatabase<?> source = new ChemicalBlobDatabase<>(new FileBlobStorage(Path.of("src/test/resources/test-blob-db").toAbsolutePath()));
        List<FingerprintCandidate> candidates = new ArrayList<>();
        for (MolecularFormula formula : source.formulas)
                candidates.addAll(source.lookupStructuresAndFingerprintsByFormula(formula));

        formulas = source.formulas;
        Path tempDB = Files.createTempFile("chemDB-nitrite_", "_unitTest");
        chemDb = new ChemicalNitriteDatabase(tempDB);
        ChemicalNoSQLDBs.importCompoundsAndFingerprintsLazy(chemDb.getStorage(), candidates, null, "2099-12-24", null, 5, 100);
    }

    @Test
    public void rawTestTags() throws IOException {
        List<SpectralNoSQLDatabase.Tag> tags = chemDb.getStorage().findAllStr(SpectralNoSQLDatabase.Tag.class).toList();
        assertEquals(2, tags.size());
        assertTrue(tags.contains(SpectralNoSQLDatabase.Tag.of(ChemDbTags.TAG_DATE ,"2099-12-24")));
        assertTrue(tags.contains(SpectralNoSQLDatabase.Tag.of(ChemDbTags.TAG_FP_ID , String.valueOf(5))));
    }

    @Test
    public void rawTestCompounds() throws IOException {
        List<FingerprintCandidate> fcs = chemDb.getStorage().findAllStr(FingerprintCandidate.class).toList();
        assertEquals(21, fcs.size());
        fcs.forEach(fc -> assertNull(fc.getFingerprint()));
    }

    @Test
    public void rawTestFps() throws IOException {
        List<FingerprintWrapper> fps = chemDb.getStorage().findAllStr(FingerprintWrapper.class).toList();
        assertEquals(21, fps.size());
        fps.forEach(fc -> assertNotNull(fc.getFingerprint()));
    }

    @Test
    public void rawTestSpectraMeta() throws IOException {
        List<Ms2SpectralMetadata> specMeta = chemDb.getStorage().findAllStr(Ms2SpectralMetadata.class).toList();
        assertEquals(0, specMeta.size());
    }

    @Test
    public void rawTestSpectraData() throws IOException {
        Stream<Ms2SpectralData> spectData = chemDb.getStorage().findAllStr(Ms2SpectralData.class);
        assertEquals(0, spectData.count());
    }

    @Test
    public void lookUpStructureAndFingerprintByFormula() throws IOException {
        for (MolecularFormula formula : formulas) {
            List<FingerprintCandidate> candidates = chemDb.lookupStructuresAndFingerprintsByFormula(formula);
            assertFalse(candidates.isEmpty());
            candidates.forEach(fc ->
                    assertNotNull(fc.getFingerprint()));
        }
    }

}
