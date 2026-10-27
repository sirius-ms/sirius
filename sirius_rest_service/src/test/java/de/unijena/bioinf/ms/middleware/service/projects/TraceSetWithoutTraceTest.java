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

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ms.middleware.model.features.TraceSet;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.NoSqlProjectSearchContextProvider;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.DataSource;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A feature imported without an LC-MS run has no trace to draw, and the trace views have to say so rather than
 * fail. The intensity cannot be what decides that - such a feature reports one - so the trace is.
 */
public class TraceSetWithoutTraceTest {

    private Path location;
    private NitriteSirirusProject ps;
    private SearchService searchService;

    @BeforeEach
    @SneakyThrows
    public void createProject() {
        location = Files.createTempDirectory("trace-set-without-trace-")
                .resolve("test" + SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
        searchService = new SearchServiceImpl(new NoSqlProjectSearchContextProvider(true, null, true));
    }

    @AfterEach
    @SneakyThrows
    public void closeProject() {
        searchService.close();
        ps.close();
    }

    private NoSQLProjectImpl project() {
        return new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), searchService, (a, b) -> false);
    }

    /**
     * What a peak-list import writes: an aligned feature with a source file and no trace, and one child feature
     * that belongs to no run but carries the intensity of the precursor peak.
     */
    @SneakyThrows
    private long addPeakListFeature(String name, Long compoundId) {
        AlignedFeatures af = AlignedFeatures.builder().charge((byte) 1).name(name).compoundId(compoundId)
                .dataSource(DataSource.fromPath("/tmp/" + name + ".mgf")).build();
        af.setApexIntensity(1_000_000d); // the aligned feature mirrors its only feature
        ps.getStorage().insert(af);
        ps.getStorage().insert(Feature.builder().alignedFeatureId(af.getAlignedFeatureId())
                .apexIntensity(1_000_000d).build());
        return af.getAlignedFeatureId();
    }

    @SneakyThrows
    private long addCompound(String name) {
        Compound compound = Compound.builder().name(name).build();
        ps.getStorage().insert(compound);
        return compound.getCompoundId();
    }

    @Test
    public void testFeatureWithoutATraceHasNoTraceSet() {
        long featureId = addPeakListFeature("peaklist-feature", null);
        Optional<TraceSet> traces = project().getTraceSetForAlignedFeature(String.valueOf(featureId), false);
        assertTrue(traces.isEmpty(), "a feature without a trace has no traces to show");
    }

    @Test
    public void testCompoundWithoutTracesHasNoTraceSet() {
        long compoundId = addCompound("peaklist-compound");
        addPeakListFeature("peaklist-feature", compoundId);
        Optional<TraceSet> traces = project().getTraceSetForCompound(String.valueOf(compoundId), Optional.empty());
        assertTrue(traces.isEmpty(), "a compound whose features have no traces has no traces to show");
    }
}
