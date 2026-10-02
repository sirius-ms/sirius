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

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.FeatureImport;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.NoSqlProjectSearchContextProvider;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A feature imported through the API is stored like one imported from a peak list: the MS1 intensity it
 * reports is the height of its precursor peak, and it sits on a feature of its own that belongs to no run.
 * The aligned feature's own apex intensity stays what the LC-MS pipeline puts there, which is a merged trace
 * and not an intensity in counts.
 */
public class FeatureImportIntensityTest {

    /** The intensity of the peak at the precursor mass in the MS1 below. */
    private static final double PRECURSOR_INTENSITY = 3d;

    private NitriteSirirusProject ps;
    private SearchService searchService;
    private NoSQLProjectImpl project;

    @BeforeEach
    @SneakyThrows
    public void createProject() {
        Path location = Files.createTempDirectory("feature-import-intensity-")
                .resolve("test" + SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
        searchService = new SearchServiceImpl(new NoSqlProjectSearchContextProvider(true, null, true));
        project = new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), searchService, (a, b) -> false);
    }

    @AfterEach
    @SneakyThrows
    public void closeProject() {
        searchService.close();
        ps.close();
    }

    private AlignedFeature importFeature() {
        BasicSpectrum ms1 = new BasicSpectrum(new double[]{1, 2, 42}, new double[]{1, 2, PRECURSOR_INTENSITY});
        BasicSpectrum ms2 = new BasicSpectrum(new double[]{1, 2, 42}, new double[]{1, 2, 3});
        ms2.setCollisionEnergy(CollisionEnergy.fromString("20eV"));
        ms2.setMsLevel(2);
        ms2.setPrecursorMz(42d);

        return project.addAlignedFeatures(List.of(FeatureImport.builder()
                .name("imported")
                .externalFeatureId("testFID")
                .ionMass(42d)
                .charge((byte) 1)
                .rtStartSeconds(6d)
                .rtApexSeconds(10d)
                .rtEndSeconds(12d)
                .mergedMs1(ms1)
                .ms2Spectra(List.of(ms2))
                .build()), null, EnumSet.of(AlignedFeature.OptField.intensities), "test").getFirst();
    }

    @Test
    @SneakyThrows
    public void testImportedFeatureCarriesItsIntensityOnAFeatureOfItsOwn() {
        importFeature();

        List<Feature> features = ps.getStorage().findAllStr(Feature.class).toList();
        assertEquals(1, features.size(), "an imported feature is one feature, belonging to no run");
        assertEquals(PRECURSOR_INTENSITY, features.getFirst().getApexIntensity(),
                "the height of the precursor peak in the MS1");
        assertNull(features.getFirst().getRunId(), "there is no run it was measured in");
    }

    @Test
    @SneakyThrows
    public void testTheAlignedFeatureKeepsNoIntensityOfItsOwn() {
        AlignedFeature imported = importFeature();

        AlignedFeatures stored = ps.getStorage()
                .getByPrimaryKey(Long.parseLong(imported.getAlignedFeatureId()), AlignedFeatures.class).orElseThrow();
        assertNull(stored.getApexIntensity(), "that field is the apex of a merged trace, which this has none of");
    }

    @Test
    public void testTheReportedIntensityIsThePrecursorHeight() {
        assertEquals(PRECURSOR_INTENSITY, importFeature().getIntensity().getApex());
    }

    /**
     * The intensities are an opt field: whoever does not ask gets none, whether the feature comes from the
     * database or from a search index document, which holds them either way.
     */
    @Test
    public void testTheIntensityIsOnlyThereWhenAskedFor() {
        String id = importFeature().getAlignedFeatureId();
        EnumSet<AlignedFeature.OptField> none = EnumSet.noneOf(AlignedFeature.OptField.class);
        EnumSet<AlignedFeature.OptField> asked = EnumSet.of(AlignedFeature.OptField.intensities);

        assertNull(project.findAlignedFeaturesById(id, false, none).getIntensity(), "by id, from the database");
        assertNull(project.findAlignedFeatures(null, Pageable.unpaged(), false, none).getContent().getFirst().getIntensity(),
                "from the search index");
        assertNull(project.findAlignedFeatures(Pageable.unpaged(), false, none).getContent().getFirst().getIntensity(),
                "a page from the database");

        assertEquals(PRECURSOR_INTENSITY, project.findAlignedFeaturesById(id, false, asked).getIntensity().getApex());
        assertEquals(PRECURSOR_INTENSITY, project.findAlignedFeatures(null, Pageable.unpaged(), false, asked)
                .getContent().getFirst().getIntensity().getApex());
        assertEquals(PRECURSOR_INTENSITY, project.findAlignedFeatures(Pageable.unpaged(), false, asked)
                .getContent().getFirst().getIntensity().getApex());
        assertEquals(100d, project.findAlignedFeaturesById(id, false, asked).getIntensity().getApexPercent(), 1e-9,
                "the only feature is the strongest the project has held");
    }
}
