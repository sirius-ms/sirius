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
import de.unijena.bioinf.ms.middleware.model.tags.*;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.persistence.model.core.run.*;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

            Map<String, TagCategory> catIn = Map.of(
                    "c0", TagCategory.builder().name("c0").valueType(TagCategory.ValueType.NONE).categoryType("foo").build(),
                    "c1", TagCategory.builder().name("c1").valueRange(TagCategory.ValueRange.FIXED).valueType(TagCategory.ValueType.BOOLEAN).possibleValues(List.of(true, false)).build(),
                    "c2", TagCategory.builder().name("c2").valueRange(TagCategory.ValueRange.FIXED).valueType(TagCategory.ValueType.INTEGER).possibleValues(List.of(0, 1)).build(),
                    "c3", TagCategory.builder().name("c3").valueRange(TagCategory.ValueRange.VARIABLE).valueType(TagCategory.ValueType.DOUBLE).build(),
                    "c4", TagCategory.builder().name("c4").valueRange(TagCategory.ValueRange.VARIABLE).valueType(TagCategory.ValueType.STRING).build()
            );

            Assert.assertThrows(IllegalArgumentException.class, () -> project.addCategories(
                    List.of(TagCategory.builder().name("c1").valueRange(TagCategory.ValueRange.FIXED).valueType(TagCategory.ValueType.BOOLEAN).build())
            ));
            Assert.assertThrows(IllegalArgumentException.class, () -> project.addCategories(
                    List.of(TagCategory.builder().name("c1").valueRange(TagCategory.ValueRange.FIXED).valueType(TagCategory.ValueType.BOOLEAN).possibleValues(List.of(0, 1)).build())
            ));

            Map<String, TagCategory> cats0 = project.addCategories(new ArrayList<>(catIn.values())).stream().collect(Collectors.toMap(TagCategory::getName, Function.identity()));
            Map<String, TagCategory> cats1 = project.findCategories().stream().collect(Collectors.toMap(TagCategory::getName, Function.identity()));
            Map<String, TagCategory> cats2 = catIn.keySet().stream().map(project::findCategoryByName).collect(Collectors.toMap(TagCategory::getName, Function.identity()));

            List<TagCategory> cats3 = project.findCategoriesByType("foo");
            Assert.assertEquals(1, cats3.size());
            Assert.assertEquals("c0", cats3.getFirst().getName());

            Assert.assertThrows(ResponseStatusException.class, () -> project.findCategoryByName("foo"));

            Assert.assertEquals(catIn.size(), cats0.size());
            Assert.assertEquals(catIn.size(), cats1.size());
            Assert.assertEquals(catIn.size(), cats2.size());

            for (String name : catIn.keySet()) {
                TagCategory c = catIn.get(name);
                for (Map<String, TagCategory> comparison : List.of(cats0, cats1, cats2)) {
                    TagCategory cc = comparison.get(name);
                    Assert.assertEquals(c.getName(), cc.getName());
                    Assert.assertEquals(c.getValueRange(), cc.getValueRange());
                    Assert.assertEquals(c.getValueType(), cc.getValueType());
                    Assert.assertEquals(c.getPossibleValues(), cc.getPossibleValues());
                    if (c.getPossibleValues() != null && cc.getPossibleValues() != null) {
                        Assert.assertArrayEquals(c.getPossibleValues().toArray(), cc.getPossibleValues().toArray());
                    }
                }
            }

            Assert.assertNull(project.addCategories(
                    List.of(TagCategory.builder().name("cfoo0").valueType(TagCategory.ValueType.NONE).build())
            ).getFirst().getPossibleValues());
            Assert.assertNull(project.addCategories(
                    List.of(TagCategory.builder().name("cfoo1").valueType(TagCategory.ValueType.BOOLEAN).valueRange(TagCategory.ValueRange.VARIABLE).build())
            ).getFirst().getPossibleValues());

        }

    }

    @Test
    public void testTags() throws IOException {

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, (a, b) -> false);

            List<LCMSRun> runs = List.of(
                    LCMSRun.builder()
                        .name("run1")
                        .chromatography(Chromatography.LC)
                        .fragmentation(Fragmentation.byValue("CID").orElseThrow())
                        .ionization(Ionization.byValue("ESI").orElseThrow())
                        .massAnalyzers(List.of(MassAnalyzer.byValue("FTICR").orElseThrow()))
                        .build(),
                    LCMSRun.builder()
                            .name("run2")
                            .chromatography(Chromatography.LC)
                            .fragmentation(Fragmentation.byValue("CID").orElseThrow())
                            .ionization(Ionization.byValue("ESI").orElseThrow())
                            .massAnalyzers(List.of(MassAnalyzer.byValue("FTICR").orElseThrow()))
                            .build()
                    );

            ps.getStorage().insertAll(runs);
            Run run = project.findRunById(Long.toString(runs.getFirst().getRunId()));

            project.addCategories(List.of(TagCategory.builder().name("c1").valueType(TagCategory.ValueType.BOOLEAN).build()));

            project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.BoolTag.builder().categoryName("c1").value(true).build()));
            Map<String, Tag> tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(1, tags.size());
            Assert.assertTrue(tags.get("c1") instanceof Tag.BoolTag);
            Assert.assertEquals(true, ((Tag.BoolTag) tags.get("c1")).getValue());

            project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.BoolTag.builder().categoryName("c1").value(false).build()));
            tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(1, tags.size());
            Assert.assertTrue(tags.get("c1") instanceof Tag.BoolTag);
            Assert.assertEquals(false, ((Tag.BoolTag) tags.get("c1")).getValue());

            Assert.assertThrows(ResponseStatusException.class, () -> project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.BoolTag.builder().categoryName("c2").value(false).build())));
            Assert.assertThrows(ResponseStatusException.class, () -> project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.DoubleTag.builder().categoryName("c1").value(2.0).build())));

            project.addCategories(List.of(
                    TagCategory.builder().name("c2").valueType(TagCategory.ValueType.INTEGER).build(),
                    TagCategory.builder().name("c3").valueType(TagCategory.ValueType.DOUBLE).build(),
                    TagCategory.builder().name("c4").valueType(TagCategory.ValueType.STRING).build()
            ));

            project.addTagsToObject(Run.class, run.getRunId(), List.of(
                    Tag.BoolTag.builder().categoryName("c1").value(false).build(),
                    Tag.IntTag.builder().categoryName("c2").value(42).build(),
                    Tag.DoubleTag.builder().categoryName("c3").value(42.0).build(),
                    Tag.StringTag.builder().categoryName("c4").value("42").build()
            ));

            tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(4, tags.size());
            Assert.assertTrue(tags.get("c1") instanceof Tag.BoolTag);
            Assert.assertTrue(tags.get("c2") instanceof Tag.IntTag);
            Assert.assertTrue(tags.get("c3") instanceof Tag.DoubleTag);
            Assert.assertTrue(tags.get("c4") instanceof Tag.StringTag);
            Assert.assertEquals(false, ((Tag.BoolTag) tags.get("c1")).getValue());
            Assert.assertEquals(Integer.valueOf(42), ((Tag.IntTag) tags.get("c2")).getValue());
            Assert.assertEquals(Double.valueOf(42.0), ((Tag.DoubleTag) tags.get("c3")).getValue());
            Assert.assertEquals("42", ((Tag.StringTag) tags.get("c4")).getValue());

            project.deleteTagsFromObject(Run.class, run.getRunId(), List.of("c3", "c4"));
            tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(2, tags.size());
            Assert.assertTrue(tags.get("c1") instanceof Tag.BoolTag);
            Assert.assertTrue(tags.get("c2") instanceof Tag.IntTag);
            Assert.assertEquals(false, ((Tag.BoolTag) tags.get("c1")).getValue());
            Assert.assertEquals(Integer.valueOf(42), ((Tag.IntTag) tags.get("c2")).getValue());

            project.deleteCategories(List.of("c2", "c3"));
            tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(1, tags.size());
            Assert.assertTrue(tags.get("c1") instanceof Tag.BoolTag);
            Assert.assertEquals(false, ((Tag.BoolTag) tags.get("c1")).getValue());

            Page<Run> page = project.findObjectsByTag(Run.class, "c1", TagFilter.TaggedFilter.builder().build(), Pageable.unpaged(), EnumSet.of(Run.OptField.tags));
            Assert.assertEquals(1, page.getTotalElements());
            Assert.assertEquals(run.getRunId(), page.getContent().getFirst().getRunId());
            tags = page.get().findFirst().orElseThrow().getTags();
            Assert.assertEquals(1, tags.size());
            Assert.assertTrue(tags.get("c1") instanceof Tag.BoolTag);
            Assert.assertEquals(false, ((Tag.BoolTag) tags.get("c1")).getValue());

            page = project.findObjectsByTag(Run.class, "c1", TagFilter.TaggedFilter.builder().value(false).build(), Pageable.unpaged(), EnumSet.of(Run.OptField.tags));
            Assert.assertEquals(1, page.getTotalElements());
            Assert.assertEquals(Long.toString(runs.get(1).getRunId()), page.getContent().getFirst().getRunId());
        }

    }

    @Test
    public void testMany() throws IOException {

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, (a, b) -> false);

            List<LCMSRun> lcmsRuns = IntStream.range(0, 10000).mapToObj(i -> (LCMSRun) LCMSRun.builder()
                    .name("run" + i)
                    .chromatography(Chromatography.LC)
                    .fragmentation(Fragmentation.byValue("CID").orElseThrow())
                    .ionization(Ionization.byValue("ESI").orElseThrow())
                    .massAnalyzers(List.of(MassAnalyzer.byValue("FTICR").orElseThrow()))
                    .build()).toList();

            ps.getStorage().insertAll(lcmsRuns);

            List<Run> runs = project.findRuns(Pageable.unpaged()).getContent();
            List<Run> control = runs.subList(0, runs.size() / 3);
            List<Run> blank = runs.subList(runs.size() / 3, 2 * runs.size() / 3);
            List<Run> sample = runs.subList(2 * runs.size() / 3, runs.size());

            project.addCategories(List.of(
                    TagCategory.builder().categoryType("sampleCat").name("sample type").valueType(TagCategory.ValueType.STRING).valueRange(TagCategory.ValueRange.FIXED).possibleValues(List.of("control", "blank", "sample")).build()
            ));

            StopWatch watch = new StopWatch();
            watch.start();

            for (Run run : control) {
                project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.StringTag.builder().categoryName("sample type").value("control").build()));
            }
            for (Run run : blank) {
                project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.StringTag.builder().categoryName("sample type").value("blank").build()));
            }
            for (Run run : sample) {
                project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.StringTag.builder().categoryName("sample type").value("sample").build()));
            }

            watch.stop();
            System.out.println("CREATE TAGS: " + watch);

            watch = new StopWatch();
            watch.start();

            project.findObjectsByTag(Run.class, "sample type", TagFilter.StringTagFilter.builder().equals("sample").build(), Pageable.unpaged(), EnumSet.of(Run.OptField.tags));

            watch.stop();
            System.out.println("FIND OBJ BY TAGS: " + watch);
        }

    }

}
