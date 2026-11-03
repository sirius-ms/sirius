/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2026 Bright Giant GmbH
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.lcms.QuantificationTable;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.trace.RawTraceRef;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The quantification table lists a feature's intensity per LC-MS run it was measured in. A feature without a
 * run - imported from a peak list or through the API - may still carry an intensity (its precursor peak
 * height), but it has no run to be listed under, so it must not end up in the table.
 */
public class NoSQLInstanceQuantificationTableTest {

    private NitriteSirirusProject ps;
    private NoSQLProjectSpaceManager manager;

    @BeforeEach
    @SneakyThrows
    public void createProject() {
        Path location = Files.createTempDirectory("quant-table-")
                .resolve("test" + SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
        manager = new NoSQLProjectSpaceManager(ps);
    }

    @AfterEach
    @SneakyThrows
    public void closeProject() {
        ps.close();
    }

    @SneakyThrows
    private NoSQLInstance instanceOf(Feature feature) {
        AlignedFeatures aligned = AlignedFeatures.singleton(feature);
        ps.importAlignedFeatures(List.of(aligned));
        return new NoSQLInstance(aligned, manager);
    }

    @Test
    public void testAFeatureWithoutARunIsNotListed() {
        // the precursor peak height a peak-list import stores
        NoSQLInstance instance = instanceOf(Feature.builder().averageMass(200d).apexIntensity(12_500d).build());

        QuantificationTable table = instance.getQuantificationTable().orElseThrow();
        assertEquals(0, table.length(), "there is no run to list its intensity under");
    }

    @Test
    @SneakyThrows
    public void testAFeatureOfARunIsListedUnderThatRun() {
        LCMSRun run = LCMSRun.builder().name("sample_1").build();
        ps.getStorage().insert(run);
        NoSQLInstance instance = instanceOf(Feature.builder()
                .averageMass(200d)
                .apexIntensity(12_500d)
                .runId(run.getRunId())
                .traceRef(new RawTraceRef(1, 0, 0, 5, 10, 0, 5, 10, 0))
                .build());

        QuantificationTable table = instance.getQuantificationTable().orElseThrow();
        assertEquals(1, table.length());
        assertEquals("sample_1", table.getName(0));
        assertEquals(12_500d, table.getAbundance(0));
    }
}
