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
import de.unijena.bioinf.ms.middleware.controller.mixins.TagController;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.FeatureImport;
import de.unijena.bioinf.ms.middleware.model.features.MsData;
import de.unijena.bioinf.ms.middleware.model.features.Run;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.model.tags.TagCategory;
import de.unijena.bioinf.ms.middleware.model.tags.TagCategoryGroup;
import de.unijena.bioinf.ms.middleware.model.tags.TagCategoryImport;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.persistence.model.core.run.*;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.storage.db.nosql.Filter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.NumberDateFormat;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.Query;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
                            .rtApexSeconds(10d)
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
                            .rtApexSeconds(9d)
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

            Map<String, TagCategoryImport> catIn = Map.of(
                    "c0", TagCategoryImport.builder().name("c0").valueTypeAndPossibleValues(TagCategory.ValueType.NONE, null).categoryType("foo").build(),
                    "c1", TagCategoryImport.builder().name("c1").valueTypeAndPossibleValues(TagCategory.ValueType.BOOLEAN, List.of(true, false)).build(),
                    "c2", TagCategoryImport.builder().name("c2").valueTypeAndPossibleValues(TagCategory.ValueType.INTEGER, List.of(0, 1)).build(),
                    "c3", TagCategoryImport.builder().name("c3").valueTypeAndPossibleValues(TagCategory.ValueType.DOUBLE, null).build(),
                    "c4", TagCategoryImport.builder().name("c4").valueTypeAndPossibleValues(TagCategory.ValueType.STRING, null).build()
            );

            Assert.assertThrows(IllegalArgumentException.class, () -> project.addCategories(
                    List.of(TagCategoryImport.builder().name("c1").valueTypeAndPossibleValues(TagCategory.ValueType.BOOLEAN, List.of(0, 1)).build()), true)
            );

            Map<String, TagCategory> cats0 = project.addCategories(new ArrayList<>(catIn.values()), true).stream().collect(Collectors.toMap(TagCategory::getName, Function.identity()));
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
                TagCategoryImport c = catIn.get(name);
                for (Map<String, TagCategory> comparison : List.of(cats0, cats1, cats2)) {
                    TagCategory cc = comparison.get(name);
                    Assert.assertEquals(c.getName(), cc.getName());
                    Assert.assertEquals(c.getValueType(), cc.getValueType());
                    Assert.assertEquals(c.getPossibleValues(), cc.getPossibleValues());
                    if (c.getPossibleValues() != null && cc.getPossibleValues() != null) {
                        Assert.assertArrayEquals(c.getPossibleValues().toArray(), cc.getPossibleValues().toArray());
                    }
                }
            }

            Assert.assertNull(project.addCategories(
                    List.of(TagCategoryImport.builder().name("cfoo0").valueTypeAndPossibleValues(TagCategory.ValueType.NONE, null).build()), true
            ).getFirst().getPossibleValues());
            Assert.assertNull(project.addCategories(
                    List.of(TagCategoryImport.builder().name("cfoo1").valueTypeAndPossibleValues(TagCategory.ValueType.BOOLEAN, null).build()), false
            ).getFirst().getPossibleValues());

            Assert.assertThrows(ResponseStatusException.class, () -> project.deleteCategory("foo"));
            Assert.assertThrows(ResponseStatusException.class, () -> project.deleteCategory("cfoo1"));

            Assert.assertThrows(ResponseStatusException.class, () -> project.addPossibleValuesToCategory("cfoo0", List.of(true, false)));

            List<String> before = project.findCategories().stream().map(TagCategory::getName).toList();
            project.deleteCategory("cfoo0");
            List<String> after = project.findCategories().stream().map(TagCategory::getName).toList();

            Assert.assertEquals(before.size() - 1, after.size());
            Assert.assertFalse(after.contains("cfoo0"));

            Assert.assertThrows(ResponseStatusException.class, () -> project.addPossibleValuesToCategory("cfoo0", List.of(true, false)));
            Assert.assertThrows(ResponseStatusException.class, () -> project.addPossibleValuesToCategory("cfoo1", List.of(true, false)));

            project.addPossibleValuesToCategory("c4", List.of("sample", "blank"));
            Assert.assertArrayEquals(new String[]{"sample", "blank"}, project.findCategoryByName("c4").getPossibleValues().toArray(String[]::new));
            project.addPossibleValuesToCategory("c4", List.of("qq"));
            Assert.assertArrayEquals(new String[]{"sample", "blank", "qq"}, project.findCategoryByName("c4").getPossibleValues().toArray(String[]::new));

        }

    }

    @Test
    public void testGroups() throws IOException {

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, (a, b) -> false);

            List<TagCategoryImport> catIn = List.of(
                    TagCategoryImport.builder().name("c0").valueTypeAndPossibleValues(TagCategory.ValueType.NONE, null).categoryType("foo").build(),
                    TagCategoryImport.builder().name("c1").valueTypeAndPossibleValues(TagCategory.ValueType.BOOLEAN, List.of(true, false)).build(),
                    TagCategoryImport.builder().name("c2").valueTypeAndPossibleValues(TagCategory.ValueType.INTEGER, List.of(0, 1)).build(),
                    TagCategoryImport.builder().name("c3").valueTypeAndPossibleValues(TagCategory.ValueType.DOUBLE, null).build(),
                    TagCategoryImport.builder().name("c4").valueTypeAndPossibleValues(TagCategory.ValueType.STRING, null).build()
            );

            project.addCategories(catIn, true);

            project.addTagGroup("group1", "name:c0 AND categoryType:foo", "type1");
            project.addTagGroup("group2", "valueType:INTEGER", "type1");
            project.addTagGroup("group3", "name:c1 OR name:c3", "type2");

            Map<String, TagCategoryGroup> groups = project.findTagGroups().stream().collect(Collectors.toMap(TagCategoryGroup::getName, Function.identity()));

            Assert.assertEquals(3, groups.size());
            Assert.assertTrue(groups.containsKey("group1"));
            Assert.assertTrue(groups.containsKey("group2"));
            Assert.assertTrue(groups.containsKey("group3"));
            Assert.assertEquals("type1", groups.get("group1").getGroupType());
            Assert.assertEquals("type1", groups.get("group2").getGroupType());
            Assert.assertEquals("type2", groups.get("group3").getGroupType());
            Assert.assertEquals(1, groups.get("group1").getCategories().size());
            Assert.assertEquals(1, groups.get("group2").getCategories().size());
            Assert.assertEquals(2, groups.get("group3").getCategories().size());
            Assert.assertEquals("c0", groups.get("group1").getCategories().getFirst());
            Assert.assertEquals("c2", groups.get("group2").getCategories().getFirst());
            Assert.assertTrue(groups.get("group3").getCategories().contains("c1"));
            Assert.assertTrue(groups.get("group3").getCategories().contains("c3"));

            Assert.assertThrows(ResponseStatusException.class, () -> project.findTagGroup("foo"));
            TagCategoryGroup group = project.findTagGroup("group1");
            Assert.assertNotNull(group);
            Assert.assertEquals("group1", group.getName());

            Assert.assertThrows(ResponseStatusException.class, () -> project.findTagGroupsByType("foo"));
            List<String> groupNames = project.findTagGroupsByType("type1").stream().map(TagCategoryGroup::getName).toList();
            Assert.assertEquals(2, groupNames.size());
            Assert.assertTrue(groupNames.contains("group1"));
            Assert.assertTrue(groupNames.contains("group2"));

            project.deleteCategory("c3");
            group = project.findTagGroup("group3");
            Assert.assertEquals(1, group.getCategories().size());
            Assert.assertEquals("c1", group.getCategories().getFirst());
            project.deleteCategory("c1");
            Assert.assertThrows(ResponseStatusException.class, () -> project.findTagGroup("group3"));

            project.deleteTagGroup("group2");
            groupNames = project.findTagGroups().stream().map(TagCategoryGroup::getName).toList();
            Assert.assertEquals(1, groupNames.size());
            Assert.assertEquals("group1", groupNames.getFirst());
        }
    }

    // TODO test fold change

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
            final Run run = project.findRunById(Long.toString(runs.getFirst().getRunId()));

            project.addCategories(List.of(TagCategoryImport.builder().name("c1").valueTypeAndPossibleValues(TagCategory.ValueType.BOOLEAN, null).build()), true);

            project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.builder().valueType(TagCategoryImport.ValueType.BOOLEAN).category("c1").bool(true).build()));
            Map<String, ? extends Tag> tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(1, tags.size());
            Assert.assertEquals(TagCategoryImport.ValueType.BOOLEAN, tags.get("c1").getValueType());
            Assert.assertEquals(true, tags.get("c1").getBool());

            project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.builder().valueType(TagCategoryImport.ValueType.BOOLEAN).category("c1").bool(false).build()));
            tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(1, tags.size());
            Assert.assertEquals(TagCategoryImport.ValueType.BOOLEAN, tags.get("c1").getValueType());
            Assert.assertEquals(false, tags.get("c1").getBool());

            Assert.assertThrows(ResponseStatusException.class, () -> project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.builder().valueType(TagCategoryImport.ValueType.BOOLEAN).category("c2").bool(false).build())));
            Assert.assertThrows(ResponseStatusException.class, () -> project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.builder().valueType(TagCategoryImport.ValueType.DOUBLE).category("c1").real(2.0).build())));

            project.addCategories(List.of(
                    TagCategoryImport.builder().name("c2").valueTypeAndPossibleValues(TagCategory.ValueType.INTEGER, null).build(),
                    TagCategoryImport.builder().name("c3").valueTypeAndPossibleValues(TagCategory.ValueType.DOUBLE, null).build(),
                    TagCategoryImport.builder().name("c4").valueTypeAndPossibleValues(TagCategory.ValueType.STRING, null).build()
            ), true);

            project.addTagsToObject(Run.class, run.getRunId(), List.of(
                    Tag.builder().valueType(TagCategoryImport.ValueType.BOOLEAN).category("c1").bool(false).build(),
                    Tag.builder().valueType(TagCategoryImport.ValueType.INTEGER).category("c2").integer(42).build(),
                    Tag.builder().valueType(TagCategoryImport.ValueType.DOUBLE).category("c3").real(42.0).build(),
                    Tag.builder().valueType(TagCategoryImport.ValueType.STRING).category("c4").text("42").build()
            ));

            tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(4, tags.size());
            Assert.assertEquals(TagCategoryImport.ValueType.BOOLEAN, tags.get("c1").getValueType());
            Assert.assertEquals(TagCategoryImport.ValueType.INTEGER, tags.get("c2").getValueType());
            Assert.assertEquals(TagCategoryImport.ValueType.DOUBLE, tags.get("c3").getValueType());
            Assert.assertEquals(TagCategoryImport.ValueType.STRING, tags.get("c4").getValueType());
            Assert.assertEquals(false, tags.get("c1").getBool());
            Assert.assertEquals(Integer.valueOf(42), tags.get("c2").getInteger());
            Assert.assertEquals(Double.valueOf(42.0), tags.get("c3").getReal());
            Assert.assertEquals("42", tags.get("c4").getText());

            project.deleteTagsFromObject(run.getRunId(), List.of("c3", "c4"));
            tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(2, tags.size());
            Assert.assertEquals(TagCategoryImport.ValueType.BOOLEAN, tags.get("c1").getValueType());
            Assert.assertEquals(TagCategoryImport.ValueType.INTEGER, tags.get("c2").getValueType());
            Assert.assertEquals(false, tags.get("c1").getBool());
            Assert.assertEquals(Integer.valueOf(42), tags.get("c2").getInteger());

            Page<Run> page = project.findObjectsByTag(Run.class, "category:c2 AND integer:{12 TO 43}", Pageable.unpaged(), EnumSet.of(Run.OptField.tags));
            Assert.assertEquals(1, page.getTotalElements());
            Assert.assertEquals(Long.toString(runs.getFirst().getRunId()), page.getContent().getFirst().getRunId());
            Assert.assertEquals(2, tags.size());
            Assert.assertEquals(TagCategoryImport.ValueType.BOOLEAN, tags.get("c1").getValueType());
            Assert.assertEquals(TagCategoryImport.ValueType.INTEGER, tags.get("c2").getValueType());
            Assert.assertEquals(false, tags.get("c1").getBool());
            Assert.assertEquals(Integer.valueOf(42), tags.get("c2").getInteger());

            project.deleteCategory("c2");
            project.deleteCategory("c3");
            tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(1, tags.size());
            Assert.assertEquals(TagCategoryImport.ValueType.BOOLEAN, tags.get("c1").getValueType());
            Assert.assertEquals(false, tags.get("c1").getBool());

            page = project.findObjectsByTag(Run.class, "category:c1", Pageable.unpaged(), EnumSet.of(Run.OptField.tags));
            Assert.assertEquals(1, page.getTotalElements());
            Assert.assertEquals(run.getRunId(), page.getContent().getFirst().getRunId());
            tags = page.get().findFirst().orElseThrow().getTags();
            Assert.assertEquals(1, tags.size());
            Assert.assertEquals(TagCategoryImport.ValueType.BOOLEAN, tags.get("c1").getValueType());
            Assert.assertEquals(false, tags.get("c1").getBool());

            project.addCategories(List.of(
                    TagCategoryImport.builder().name("date").valueTypeAndPossibleValues(TagCategory.ValueType.DATE, null).build(),
                    TagCategoryImport.builder().name("time").valueTypeAndPossibleValues(TagCategory.ValueType.TIME, null).build()
                    ), true);

            final Run run2 = project.findRunById(Long.toString(runs.get(1).getRunId()));
            project.addTagsToObject(Run.class, run2.getRunId(), List.of(
                    Tag.builder().valueType(TagCategoryImport.ValueType.DATE).category("date").date("2024-12-31").build(),
                    Tag.builder().valueType(TagCategoryImport.ValueType.TIME).category("time").time("12:00:00").build()
            ));

            page = project.findObjectsByTag(Run.class, "date:[2024-12-01 TO 2025-12-31] OR time:12\\:00\\:00", Pageable.unpaged(), EnumSet.of(Run.OptField.tags));
            Assert.assertEquals(1, page.getTotalElements());
            Assert.assertEquals(run2.getRunId(), page.getContent().getFirst().getRunId());
            tags = page.get().findFirst().orElseThrow().getTags();
            Assert.assertEquals(2, tags.size());
            Assert.assertEquals(TagCategoryImport.ValueType.DATE, tags.get("date").getValueType());
            Assert.assertEquals("2024-12-31", tags.get("date").getDate());
            Assert.assertEquals(TagCategoryImport.ValueType.TIME, tags.get("time").getValueType());
            Assert.assertEquals("12:00:00", tags.get("time").getTime());

            Assert.assertThrows(ResponseStatusException.class, () -> project.findObjectsByTag(Run.class, "", Pageable.unpaged(), EnumSet.of(Run.OptField.tags)));
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
                    TagCategoryImport.builder().categoryType("sampleCat").name("sample type").valueTypeAndPossibleValues(TagCategory.ValueType.STRING, List.of("control", "blank", "sample")).build()
            ), true);

            StopWatch watch = new StopWatch();
            watch.start();

            for (Run run : control) {
                project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.builder().valueType(TagCategoryImport.ValueType.STRING).category("sample type").text("control").build()));
            }
            for (Run run : blank) {
                project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.builder().valueType(TagCategoryImport.ValueType.STRING).category("sample type").text("blank").build()));
            }
            for (Run run : sample) {
                project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.builder().valueType(TagCategoryImport.ValueType.STRING).category("sample type").text("sample").build()));
            }

            watch.stop();
            System.out.println("CREATE TAGS: " + watch);

            watch = new StopWatch();
            watch.start();

            project.findObjectsByTag(Run.class, "category:\"sample type\" AND text:sample", Pageable.unpaged(), EnumSet.of(Run.OptField.tags));

            watch.stop();
            System.out.println("FIND OBJ BY TAGS: " + watch);
        }

    }

    @Test
    public void testFilterTranslation() throws IOException, NoSuchMethodException, QueryNodeException, InvocationTargetException, IllegalAccessException, ParseException {
        StandardQueryParser parser = new StandardQueryParser(new StandardAnalyzer());
        parser.setPointsConfigMap(Map.of(
                "integer", new PointsConfig(new DecimalFormat(), Long.class),
                "real", new PointsConfig(new DecimalFormat(), Double.class),
                "date", new PointsConfig(new NumberDateFormat(new SimpleDateFormat("yyyy-MM-dd")), Long.class),
                "time", new PointsConfig(new NumberDateFormat(new SimpleDateFormat("HH:mm:ss")), Long.class)
        ));

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, (a, b) -> false);

            Method convert = project.getClass().getDeclaredMethod("convertQuery", Query.class);
            convert.setAccessible(true);
            Query query = parser.parse("(test || bla) && \"new york\" AND /[mb]oat/ AND integer:[1 TO *] OR real<=3 date:2024-01-01 date:[2023-10-01 TO 2023-12-24] date<2022-01-01 time:12\\:00\\:00 time:[12\\:00\\:00 TO 14\\:00\\:00} time<10\\:00\\:00", "text");
            Filter filter = (Filter) convert.invoke(project, query);
            Assert.assertEquals(
                    "(((text==test OR text==bla) AND text==new york AND text~=/[mb]oat/ AND int32:[1, " + Integer.MAX_VALUE + "]) OR real:[-Infinity, 3.0] OR " +
                            "int64:[" + TagController.DATE_FORMAT.parse("2024-01-01").getTime() + ", " + TagController.DATE_FORMAT.parse("2024-01-01").getTime() + "] OR " +
                            "int64:[" + TagController.DATE_FORMAT.parse("2023-10-01").getTime() + ", " + TagController.DATE_FORMAT.parse("2023-12-24").getTime() + "] OR " +
                            "int64:[" + Long.MIN_VALUE + ", " + (TagController.DATE_FORMAT.parse("2022-01-01").getTime() - 1) + "] OR " +
                            "int64:[" + TagController.TIME_FORMAT.parse("12:00:00").getTime() + ", " + TagController.TIME_FORMAT.parse("12:00:00").getTime() + "] OR " +
                            "int64:[" + TagController.TIME_FORMAT.parse("12:00:00").getTime() + ", " + (TagController.TIME_FORMAT.parse("14:00:00").getTime() - 1) + "] OR " +
                            "int64:[" + Long.MIN_VALUE + ", " + (TagController.TIME_FORMAT.parse("10:00:00").getTime() - 1) + "])",
                    filter.toString()
            );
        }
    }

}
