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

import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.Intensity;
import de.unijena.bioinf.ms.middleware.model.features.Run;
import de.unijena.bioinf.ms.middleware.model.tags.TagSubmission;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.NoSqlProjectSearchContextProvider;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.run.Chromatography;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinitions;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectMaximumIntensities;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A run tagged as blank holds the background, not the sample: what a feature was measured at in a blank run
 * does not count towards the intensity it reports. The maxima count every run whatever its type, so tagging a
 * run, or untagging it, rewrites the documents of that run's features and moves no maximum.
 */
public class BlankRunIntensityTest {

    private NitriteSirirusProject ps;
    private SearchService searchService;
    private NoSQLProjectImpl project;
    private long sampleRun, blankRun;

    @BeforeEach
    @SneakyThrows
    public void createProject() {
        Path location = Files.createTempDirectory("blank-run-intensity-")
                .resolve("test" + SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
        ps.initDefaultTagDefinitions();
        searchService = new SearchServiceImpl(new NoSqlProjectSearchContextProvider(true, null, true));

        sampleRun = insertRun("sample");
        blankRun = insertRun("blank");
        project = new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), searchService, (a, b) -> false);
    }

    @AfterEach
    @SneakyThrows
    public void closeProject() {
        searchService.close();
        ps.close();
    }

    @SneakyThrows
    private long insertRun(String name) {
        LCMSRun run = LCMSRun.builder().name(name).chromatography(Chromatography.LC).build();
        ps.getStorage().insert(run);
        return run.getRunId();
    }

    private static Feature measured(long runId, double apex) {
        return Feature.builder().runId(runId).apexIntensity(apex).areaUnderCurve(apex * 20).build();
    }

    /** Imports and indexes an aligned feature with the given run features. */
    @SneakyThrows
    private String importFeature(String name, Feature... runFeatures) {
        AlignedFeatures af = AlignedFeatures.builder().charge((byte) 1).name(name).features(List.of(runFeatures)).build();
        ps.importAlignedFeatures(List.of(af));
        project.addToSearchIndex(List.of(String.valueOf(af.getAlignedFeatureId())), null);
        return String.valueOf(af.getAlignedFeatureId());
    }

    private void tagRun(long runId, String runType) {
        project.addTagsToObjects(Run.class, List.of(TagSubmission.builder()
                .taggedObjectId(String.valueOf(runId))
                .tagName(TagDefinitions.SAMPLE_TYPE.getTagName())
                .value(runType)
                .build()));
    }

    private Intensity fromDatabase(String id) {
        return project.findAlignedFeaturesById(id, false, EnumSet.of(AlignedFeature.OptField.intensities)).getIntensity();
    }

    private List<String> namesMatching(String query) {
        return project.findAlignedFeatures(query, Pageable.unpaged(), false, EnumSet.noneOf(AlignedFeature.OptField.class))
                .getContent().stream().map(AlignedFeature::getName).sorted().toList();
    }

    @Test
    public void testABlankRunDoesNotCountTowardsTheReportedIntensity() {
        tagRun(blankRun, TagDefinitions.SAMPLE_TYPE_BLANK);
        // the background is stronger than the sample - exactly the case a blank is there to tell apart
        String id = importFeature("contaminated", measured(sampleRun, 1.0e5), measured(blankRun, 8.0e5));

        assertEquals(1.0e5, fromDatabase(id).getApex(), "the sample run's value, not the blank's");
        assertEquals(2.0e6, fromDatabase(id).getAuc());
        assertEquals(List.of(), namesMatching("intensity.apex:[500000 TO *]"), "the index says the same");
        assertEquals(List.of("contaminated"), namesMatching("intensity.apex:[100000 TO *]"));
    }

    @Test
    public void testAFeatureSeenOnlyInBlanksHasNoIntensity() {
        tagRun(blankRun, TagDefinitions.SAMPLE_TYPE_BLANK);
        String id = importFeature("background", measured(blankRun, 8.0e5));

        Intensity intensity = fromDatabase(id);
        assertNotNull(intensity, "asked for, so present - and empty");
        assertNull(intensity.getApex());
        assertNull(intensity.getApexPercent());
        assertEquals(List.of(), namesMatching("intensity.apex:[0 TO *]"), "it matches no intensity filter");
        assertEquals(List.of(), namesMatching("intensity.apexPercent:[0 TO *]"));
    }

    /** Tagging a run afterwards - through the API, as the GUI's run dialog does - rewrites that run's features. */
    @Test
    public void testTaggingARunAsBlankRewritesItsFeaturesAndMovesNoMaximum() {
        importFeature("contaminated", measured(sampleRun, 1.0e5), measured(blankRun, 8.0e5));
        importFeature("sampleOnly", measured(sampleRun, 2.0e5));
        assertEquals(List.of("contaminated"), namesMatching("intensity.apex:[500000 TO *]"), "the blank still counts");
        ProjectMaximumIntensities before = ps.findProjectMaximumIntensities().orElseThrow();

        tagRun(blankRun, TagDefinitions.SAMPLE_TYPE_BLANK);

        assertEquals(List.of(), namesMatching("intensity.apex:[500000 TO *]"), "'contaminated' must have been rewritten");
        assertEquals(List.of("contaminated", "sampleOnly"), namesMatching("intensity.apex:[100000 TO *]"));
        assertEquals(before, ps.findProjectMaximumIntensities().orElseThrow(), "the maxima count blank runs too");

        // and back: a run that is no blank any more counts again
        project.removeTagsFromObject(Run.class, String.valueOf(blankRun), List.of(TagDefinitions.SAMPLE_TYPE.getTagName()));
        assertEquals(List.of("contaminated"), namesMatching("intensity.apex:[500000 TO *]"));
    }

    /** The percentage of a sample feature is still held against its own run's maximum. */
    @Test
    public void testTheRelativeIntensityComesFromTheSampleRunsOnly() {
        tagRun(blankRun, TagDefinitions.SAMPLE_TYPE_BLANK);
        importFeature("strongest", measured(sampleRun, 1.0e6));
        String id = importFeature("contaminated", measured(sampleRun, 1.0e5), measured(blankRun, 8.0e5));

        assertEquals(10d, fromDatabase(id).getApexPercent(), 1e-9,
                "10 % of the sample run, not the 100 % it reaches in its blank run");
    }
}
