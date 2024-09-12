/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.FeatureImport;
import de.unijena.bioinf.ms.middleware.model.features.MsData;
import de.unijena.bioinf.ms.middleware.model.features.Run;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.model.tags.TagCategory;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.persistence.model.core.run.*;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NoSQLProjectTest {

    @Test
    public void testCompounds() throws IOException {

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, (a, b) -> false);

            BasicSpectrum ms1 = new BasicSpectrum(new double[]{1, 2, 42}, new double[]{1, 2, 3}, 1d);
            BasicSpectrum ms2 = new BasicSpectrum(new double[]{1, 2, 42}, new double[]{1, 2, 3}, 1d);

            ms2.setCollisionEnergy(CollisionEnergy.fromString("20eV"));
            ms2.setMsLevel(2);
            ms2.setPrecursorMz(42d);
            ms2.setScanNumber(5);

            List<CompoundImport> imports = List.of(CompoundImport.builder().name("foo").features(
                    List.of(FeatureImport.builder()
//                            .name("foo")
                            .externalFeatureId("testFID")
                            .ionMass(42d)
                            .charge(1)
                            .detectedAdducts(Set.of("M+H+"))
                            .rtStartSeconds(6d)
                            .rtEndSeconds(12d)
                            .mergedMs1(ms1)
                            .ms1Spectra(List.of(ms1))
                            .ms2Spectra(List.of(ms2, ms2))
                            .build())
            ).build());

            List<Compound> compounds = project.addCompounds(imports, null, EnumSet.of(Compound.OptField.none), EnumSet.of(AlignedFeature.OptField.msData));
            List<Compound> compounds2 = project.findCompounds(Pageable.unpaged(), EnumSet.of(Compound.OptField.none), EnumSet.of(AlignedFeature.OptField.msData)).getContent();

            Assert.assertEquals(1, compounds.size());
            Assert.assertEquals(1, compounds2.size());

            Compound c1 = compounds.get(0);
            Compound c2 = compounds2.get(0);

            Assert.assertEquals(1, c1.getFeatures().size());
            Assert.assertEquals(1, c2.getFeatures().size());

            AlignedFeature f1 = c1.getFeatures().get(0);
            AlignedFeature f2 = c2.getFeatures().get(0);

            Assert.assertTrue(EqualsBuilder.reflectionEquals(c1, c2, "features"));
            Assert.assertTrue(EqualsBuilder.reflectionEquals(f1, f2, "msData"));

            MsData d1 = f1.getMsData();
            MsData d2 = f2.getMsData();

            Assert.assertNotNull(d1);
            Assert.assertNotNull(d2);

            Assert.assertTrue(EqualsBuilder.reflectionEquals(d1.getMergedMs1(), d2.getMergedMs1()));
            Assert.assertTrue(EqualsBuilder.reflectionEquals(d1.getMergedMs2(), d2.getMergedMs2()));
            Assert.assertEquals(2, d1.getMs2Spectra().size());
            Assert.assertEquals(2, d2.getMs2Spectra().size());

            Assert.assertTrue(EqualsBuilder.reflectionEquals(d1.getMs2Spectra().get(0), d2.getMs2Spectra().get(0)));
            Assert.assertTrue(EqualsBuilder.reflectionEquals(d1.getMs2Spectra().get(1), d2.getMs2Spectra().get(1)));

        }
    }

    @Test
    public void testFeatures() throws IOException {

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, (a, b) -> false);

            BasicSpectrum ms1 = new BasicSpectrum(new double[]{1, 2, 42}, new double[]{1, 2, 3}, 1d);
            BasicSpectrum ms2 = new BasicSpectrum(new double[]{1, 2, 42}, new double[]{1, 2, 3}, 1d);

            ms2.setCollisionEnergy(CollisionEnergy.fromString("20eV"));
            ms2.setMsLevel(2);
            ms2.setPrecursorMz(42d);
            ms2.setScanNumber(5);

            List<FeatureImport> imports = List.of(FeatureImport.builder()
                            .name("foo")
                            .externalFeatureId("testFID")
                            .ionMass(42d)
                            .charge(1)
                            .detectedAdducts(Set.of("M+H+"))
                            .rtStartSeconds(6d)
                            .rtEndSeconds(12d)
                            .mergedMs1(ms1)
                            .ms1Spectra(List.of(ms1))
                            .ms2Spectra(List.of(ms2, ms2))
                            .build());

            List<AlignedFeature> features = project.addAlignedFeatures(imports, null, EnumSet.of(AlignedFeature.OptField.msData));
            List<AlignedFeature> features2 = project.findAlignedFeatures(Pageable.unpaged(), EnumSet.of(AlignedFeature.OptField.msData)).getContent();


            Assert.assertEquals(1, features.size());
            Assert.assertEquals(1, features2.size());

            AlignedFeature f1 = features.get(0);
            AlignedFeature f2 = features2.get(0);

            Assert.assertTrue(EqualsBuilder.reflectionEquals(f1, f2, "msData"));

            MsData d1 = f1.getMsData();
            MsData d2 = f2.getMsData();

            Assert.assertNotNull(d1);
            Assert.assertNotNull(d2);

            Assert.assertTrue(EqualsBuilder.reflectionEquals(d1.getMergedMs1(), d2.getMergedMs1()));
            Assert.assertTrue(EqualsBuilder.reflectionEquals(d1.getMergedMs2(), d2.getMergedMs2()));
            Assert.assertEquals(2, d1.getMs2Spectra().size());
            Assert.assertEquals(2, d2.getMs2Spectra().size());

            Assert.assertTrue(EqualsBuilder.reflectionEquals(d1.getMs2Spectra().get(0), d2.getMs2Spectra().get(0)));
            Assert.assertTrue(EqualsBuilder.reflectionEquals(d1.getMs2Spectra().get(1), d2.getMs2Spectra().get(1)));

        }
    }

    @Test
    public void testRuns() throws IOException {

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, (a, b) -> false);

            LCMSRun runIn = LCMSRun.builder()
                    .name("run1")
                    .chromatography(Chromatography.LC)
                    .fragmentation(Fragmentation.byValue("CID").orElseThrow())
                    .ionization(Ionization.byValue("ESI").orElseThrow())
                    .massAnalyzers(List.of(MassAnalyzer.byValue("FTICR").orElseThrow()))
                    .build();

            ps.getStorage().insert(runIn);
            Run runOut = project.findRunById(Long.toString(runIn.getRunId()));

            Assert.assertEquals(1, project.findRuns(Pageable.unpaged()).getTotalElements());
            Assert.assertEquals(runIn.getRunId(), Long.parseLong(runOut.getRunId()));
            Assert.assertEquals(runIn.getName(), runOut.getName());
            Assert.assertEquals(runIn.getChromatography().getFullName(), runOut.getChromatography());
            Assert.assertEquals(runIn.getIonization().getFullName(), runOut.getIonization());
            Assert.assertEquals(runIn.getFragmentation().getFullName(), runOut.getFragmentation());
            Assert.assertEquals(1, runOut.getMassAnalyzers().size());
            Assert.assertEquals(runIn.getMassAnalyzers().getFirst().getFullName(), runOut.getMassAnalyzers().getFirst());
        }

    }

    @Test
    public void testCategories() throws IOException {

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, (a, b) -> false);

            Map<String, TagCategory.ValueType> catIn = Map.of(
                    "c0", TagCategory.ValueType.BOOLEAN,
                    "c1", TagCategory.ValueType.INTEGER,
                    "c2", TagCategory.ValueType.DOUBLE,
                    "c3", TagCategory.ValueType.STRING
            );

            Map<String, TagCategory.ValueType> cats0 = project.addCategories(Project.Taggable.RUN, catIn.keySet().stream().map(name -> TagCategory.builder().name(name).valueType(catIn.get(name)).build()).toList()).stream().collect(Collectors.toMap(TagCategory::getName, TagCategory::getValueType));
            Map<String, TagCategory.ValueType> cats1 = project.findCategories(Project.Taggable.RUN).stream().collect(Collectors.toMap(TagCategory::getName, TagCategory::getValueType));
            Map<String, TagCategory.ValueType> cats2 = catIn.keySet().stream().map(name -> project.findCategoryByName(Project.Taggable.RUN, name)).collect(Collectors.toMap(TagCategory::getName, TagCategory::getValueType));

            Assert.assertEquals(0, project.addCategories(Project.Taggable.RUN, List.of(TagCategory.builder().name("c0").valueType(catIn.get("foo")).build())).size());
            Assert.assertThrows(ResponseStatusException.class, () -> project.findCategoryByName(Project.Taggable.RUN, "foo"));

            Assert.assertEquals(catIn.size(), cats0.size());
            Assert.assertEquals(catIn.size(), cats1.size());
            Assert.assertEquals(catIn.size(), cats2.size());

            for (String name : catIn.keySet()) {
                Assert.assertEquals(catIn.get(name), cats0.get(name));
                Assert.assertEquals(catIn.get(name), cats1.get(name));
                Assert.assertEquals(catIn.get(name), cats2.get(name));
            }

        }

    }

    @Test
    public void testTags() throws IOException {

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, (a, b) -> false);

            LCMSRun runIn = LCMSRun.builder()
                    .name("run1")
                    .chromatography(Chromatography.LC)
                    .fragmentation(Fragmentation.byValue("CID").orElseThrow())
                    .ionization(Ionization.byValue("ESI").orElseThrow())
                    .massAnalyzers(List.of(MassAnalyzer.byValue("FTICR").orElseThrow()))
                    .build();

            ps.getStorage().insert(runIn);
            Run run = project.findRunById(Long.toString(runIn.getRunId()));

            project.addCategories(Project.Taggable.RUN, List.of(TagCategory.builder().name("c1").valueType(TagCategory.ValueType.BOOLEAN).build()));

            project.addTagsToObject(Project.Taggable.RUN, run.getRunId(), List.of(Tag.builder().categoryName("c1").value(true).build()));
            Map<String, Tag> tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(1, tags.size());
            Assert.assertEquals(true, tags.get("c1").getValue());

            project.addTagsToObject(Project.Taggable.RUN, run.getRunId(), List.of(Tag.builder().categoryName("c1").value(false).build()));
            tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(1, tags.size());
            Assert.assertEquals(false, tags.get("c1").getValue());

            Assert.assertThrows(ResponseStatusException.class, () -> project.addTagsToObject(Project.Taggable.RUN, run.getRunId(), List.of(Tag.builder().categoryName("c2").value(false).build())));
            Assert.assertThrows(ResponseStatusException.class, () -> project.addTagsToObject(Project.Taggable.RUN, run.getRunId(), List.of(Tag.builder().categoryName("c1").value(2.0).build())));

            project.addCategories(Project.Taggable.RUN, List.of(
                    TagCategory.builder().name("c2").valueType(TagCategory.ValueType.INTEGER).build(),
                    TagCategory.builder().name("c3").valueType(TagCategory.ValueType.DOUBLE).build(),
                    TagCategory.builder().name("c4").valueType(TagCategory.ValueType.STRING).build()
            ));

            project.addTagsToObject(Project.Taggable.RUN, run.getRunId(), List.of(
                    Tag.builder().categoryName("c2").value(42).build(),
                    Tag.builder().categoryName("c3").value(42.0).build(),
                    Tag.builder().categoryName("c4").value("42").build()
            ));

            project.addTagsToObject(Project.Taggable.RUN, run.getRunId(), List.of(Tag.builder().categoryName("c1").value(false).build()));
            tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(4, tags.size());
            Assert.assertEquals(false, tags.get("c1").getValue());
            Assert.assertEquals(42, tags.get("c2").getValue());
            Assert.assertEquals(42.0, tags.get("c3").getValue());
            Assert.assertEquals("42", tags.get("c4").getValue());

            project.deleteTagsFromObject(Project.Taggable.RUN, run.getRunId(), List.of("c3", "c4"));
            tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(2, tags.size());
            Assert.assertEquals(false, tags.get("c1").getValue());
            Assert.assertEquals(42, tags.get("c2").getValue());

            project.deleteCategories(Project.Taggable.RUN, List.of("c2", "c3"));
            tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(1, tags.size());
            Assert.assertEquals(false, tags.get("c1").getValue());

        }

    }

}
