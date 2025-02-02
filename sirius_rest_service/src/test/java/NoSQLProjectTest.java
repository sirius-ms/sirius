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
import de.unijena.bioinf.ms.backgroundruns.BackgroundRuns;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.features.*;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.middleware.model.statistics.FoldChange;
import de.unijena.bioinf.ms.middleware.model.statistics.StatisticsTable;
import de.unijena.bioinf.ms.middleware.model.statistics.StatisticsType;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.model.tags.TagDefinition;
import de.unijena.bioinf.ms.middleware.model.tags.TagDefinitionImport;
import de.unijena.bioinf.ms.middleware.model.tags.TagGroup;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.middleware.service.search.FakeLuceneSearchService;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.run.*;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
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
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NoSQLProjectTest {

    @Test
    public void testCompounds() throws IOException {

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, new FakeLuceneSearchService(), (a, b) -> false);

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
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, new FakeLuceneSearchService(), (a, b) -> false);

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
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, new FakeLuceneSearchService(), (a, b) -> false);

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
    public void testTagDefinitions() throws IOException {

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, new FakeLuceneSearchService(), (a, b) -> false);

            Map<String, TagDefinitionImport> catIn = Map.of(
                    "c0", TagDefinitionImport.builder().tagName("c0").valueType(ValueType.NONE).tagType("foo").build(),
                    "c1", TagDefinitionImport.builder().tagName("c1").valueType(ValueType.BOOLEAN).build(),
                    "c2", TagDefinitionImport.builder().tagName("c2").valueType(ValueType.INTEGER).possibleValues(List.of(0, 1)).build(),
                    "c3", TagDefinitionImport.builder().tagName("c3").valueType(ValueType.REAL).minValue(0d).maxValue(100d).build(),
                    "c4", TagDefinitionImport.builder().tagName("c4").valueType(ValueType.TEXT).possibleValues(List.of("BLANK")).build()
            );

            Assert.assertThrows(IllegalArgumentException.class, () -> project.createTags(
                    List.of(TagDefinitionImport.builder().tagName("c1").valueType(ValueType.BOOLEAN).possibleValues(List.of(0, 1)).build()), true)
            );

            Map<String, TagDefinition> cats0 = project.createTags(new ArrayList<>(catIn.values()), true).stream().collect(Collectors.toMap(TagDefinition::getTagName, Function.identity()));
            Map<String, TagDefinition> cats1 = project.findTags().stream().collect(Collectors.toMap(TagDefinition::getTagName, Function.identity()));
            Map<String, TagDefinition> cats2 = catIn.keySet().stream().map(project::findTagByName).collect(Collectors.toMap(TagDefinition::getTagName, Function.identity()));

            List<TagDefinition> cats3 = project.findTagsByType("foo");
            Assert.assertEquals(1, cats3.size());
            Assert.assertEquals("c0", cats3.getFirst().getTagName());

            Assert.assertThrows(ResponseStatusException.class, () -> project.findTagByName("foo"));

            Assert.assertEquals(catIn.size(), cats0.size());
            Assert.assertEquals(catIn.size(), cats1.size());
            Assert.assertEquals(catIn.size(), cats2.size());

            for (String name : catIn.keySet()) {
                TagDefinitionImport c = catIn.get(name);
                for (Map<String, TagDefinition> comparison : List.of(cats0, cats1, cats2)) {
                    TagDefinition cc = comparison.get(name);
                    Assert.assertEquals(c.getTagName(), cc.getTagName());
                    Assert.assertEquals(c.getValueType(), cc.getValueType());
                    Assert.assertEquals(c.getPossibleValues(), cc.getPossibleValues());
                    if (c.getPossibleValues() != null && cc.getPossibleValues() != null) {
                        Assert.assertArrayEquals(c.getPossibleValues().toArray(), cc.getPossibleValues().toArray());
                    }
                }
            }

            Assert.assertTrue(project.createTags(
                    List.of(TagDefinitionImport.builder().tagName("cfoo0").valueType(ValueType.NONE).build()), true
            ).getFirst().getPossibleValues().isEmpty());
            Assert.assertTrue(project.createTags(
                    List.of(TagDefinitionImport.builder().tagName("cfoo1").valueType(ValueType.BOOLEAN).build()), false
            ).getFirst().getPossibleValues().isEmpty());

            Assert.assertThrows(ResponseStatusException.class, () -> project.deleteTags("foo"));
            Assert.assertThrows(ResponseStatusException.class, () -> project.deleteTags("cfoo1"));

            Assert.assertThrows(ResponseStatusException.class, () -> project.addPossibleValuesToTagDefinition("cfoo0", List.of(true, false)));

            List<String> before = project.findTags().stream().map(TagDefinition::getTagName).toList();
            project.deleteTags("cfoo0");
            List<String> after = project.findTags().stream().map(TagDefinition::getTagName).toList();

            Assert.assertEquals(before.size() - 1, after.size());
            Assert.assertFalse(after.contains("cfoo0"));

            Assert.assertThrows(ResponseStatusException.class, () -> project.addPossibleValuesToTagDefinition("cfoo0", List.of(true, false)));
            Assert.assertThrows(ResponseStatusException.class, () -> project.addPossibleValuesToTagDefinition("cfoo1", List.of(true, false)));

            project.addPossibleValuesToTagDefinition("c4", List.of("sample", "control"));
            Assert.assertArrayEquals(new String[]{"BLANK", "sample", "control"}, project.findTagByName("c4").getPossibleValues().toArray(String[]::new));
            project.addPossibleValuesToTagDefinition("c4", List.of("qq"));
            Assert.assertArrayEquals(new String[]{"BLANK", "sample", "control", "qq"}, project.findTagByName("c4").getPossibleValues().toArray(String[]::new));

        }

    }

    @Test
    public void testGroups() throws IOException {

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, new FakeLuceneSearchService(), (a, b) -> false);

            project.createTags(List.of(
                    TagDefinitionImport.builder().tagName("sample").valueType(ValueType.TEXT).possibleValues(List.of("sample", "blank", "control")).build()
            ), true);

            List<LCMSRun> runs = List.of(
                    LCMSRun.builder().name("run1")
                            .chromatography(Chromatography.LC)
                            .fragmentation(Fragmentation.byValue("CID").orElseThrow())
                            .ionization(Ionization.byValue("ESI").orElseThrow())
                            .massAnalyzers(List.of(MassAnalyzer.byValue("FTICR").orElseThrow()))
                            .build(),
                    LCMSRun.builder().name("run2")
                            .chromatography(Chromatography.LC)
                            .fragmentation(Fragmentation.byValue("CID").orElseThrow())
                            .ionization(Ionization.byValue("ESI").orElseThrow())
                            .massAnalyzers(List.of(MassAnalyzer.byValue("FTICR").orElseThrow()))
                            .build(),
                    LCMSRun.builder().name("run3")
                            .chromatography(Chromatography.LC)
                            .fragmentation(Fragmentation.byValue("CID").orElseThrow())
                            .ionization(Ionization.byValue("ESI").orElseThrow())
                            .massAnalyzers(List.of(MassAnalyzer.byValue("FTICR").orElseThrow()))
                            .build()
            );

            ps.getStorage().insertAll(runs);
            project.addTagsToObject(Run.class, Long.toString(runs.get(0).getRunId()), List.of(Tag.builder().tagName("sample").value("sample").build()));
            project.addTagsToObject(Run.class, Long.toString(runs.get(1).getRunId()), List.of(Tag.builder().tagName("sample").value("blank").build()));
            project.addTagsToObject(Run.class, Long.toString(runs.get(2).getRunId()), List.of(Tag.builder().tagName("sample").value("control").build()));

            project.addTagGroup("group1", "tags.sample:sample", "type1");
            project.addTagGroup("group2", "tags.sample:blank", "type1");
            project.addTagGroup("group3", "tags.sample:control", "type2");

            Map<String, TagGroup> groups = project.findTagGroups().stream().collect(Collectors.toMap(TagGroup::getGroupName, Function.identity()));

            Assert.assertEquals(3, groups.size());
            Assert.assertTrue(groups.containsKey("group1"));
            Assert.assertTrue(groups.containsKey("group2"));
            Assert.assertTrue(groups.containsKey("group3"));
            Assert.assertEquals("type1", groups.get("group1").getGroupType());
            Assert.assertEquals("type1", groups.get("group2").getGroupType());
            Assert.assertEquals("type2", groups.get("group3").getGroupType());

            Page<Run> r1 = project.findObjectsByTagGroup(Run.class, "group1", Pageable.unpaged(), EnumSet.of(Run.OptField.none));
            Assert.assertEquals(1, r1.getContent().size());
            Assert.assertEquals("run1", r1.getContent().getFirst().getName());
            Page<Run> r2 = project.findObjectsByTagGroup(Run.class, "group2", Pageable.unpaged(), EnumSet.of(Run.OptField.none));
            Assert.assertEquals(1, r2.getContent().size());
            Assert.assertEquals("run2", r2.getContent().getFirst().getName());
            Page<Run> r3 = project.findObjectsByTagGroup(Run.class, "group3", Pageable.unpaged(), EnumSet.of(Run.OptField.none));
            Assert.assertEquals(1, r3.getContent().size());
            Assert.assertEquals("run3", r3.getContent().getFirst().getName());

            Assert.assertThrows(ResponseStatusException.class, () -> project.findTagGroup("foo"));
            TagGroup group = project.findTagGroup("group1");
            Assert.assertNotNull(group);
            Assert.assertEquals("group1", group.getGroupName());

            Assert.assertThrows(ResponseStatusException.class, () -> project.findTagGroupsByType("foo"));
            List<String> groupNames = project.findTagGroupsByType("type1").stream().map(TagGroup::getGroupName).toList();
            Assert.assertEquals(2, groupNames.size());
            Assert.assertTrue(groupNames.contains("group1"));
            Assert.assertTrue(groupNames.contains("group2"));

            Assert.assertThrows(ResponseStatusException.class, () -> project.findTagGroup("group4"));

            project.deleteTagGroup("group2");
            project.deleteTagGroup("group3");
            groupNames = project.findTagGroups().stream().map(TagGroup::getGroupName).toList();
            Assert.assertEquals(1, groupNames.size());
            Assert.assertEquals("group1", groupNames.getFirst());
        }
    }

    @Test
    public void testFoldChange() throws IOException, ExecutionException {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, new FakeLuceneSearchService(), (a, b) -> false);

            project.createTags(List.of(
                    TagDefinitionImport.builder().tagName("sample").valueType(ValueType.TEXT).possibleValues(List.of("sample", "blank", "control")).build()
            ), true);

            List<LCMSRun> runs = List.of(
                    LCMSRun.builder().name("run1")
                            .chromatography(Chromatography.LC)
                            .fragmentation(Fragmentation.byValue("CID").orElseThrow())
                            .ionization(Ionization.byValue("ESI").orElseThrow())
                            .massAnalyzers(List.of(MassAnalyzer.byValue("FTICR").orElseThrow()))
                            .build(),
                    LCMSRun.builder().name("run2")
                            .chromatography(Chromatography.LC)
                            .fragmentation(Fragmentation.byValue("CID").orElseThrow())
                            .ionization(Ionization.byValue("ESI").orElseThrow())
                            .massAnalyzers(List.of(MassAnalyzer.byValue("FTICR").orElseThrow()))
                            .build()
            );

            ps.getStorage().insertAll(runs);
            project.addTagsToObject(Run.class, Long.toString(runs.get(0).getRunId()), List.of(Tag.builder().tagName("sample").value("sample").build()));
            project.addTagsToObject(Run.class, Long.toString(runs.get(1).getRunId()), List.of(Tag.builder().tagName("sample").value("blank").build()));

            AlignedFeatures af = AlignedFeatures.builder().name("af").build();
            ps.getStorage().insert(af);

            Feature f1 = Feature.builder().alignedFeatureId(af.getAlignedFeatureId()).apexIntensity(2.0).runId(runs.get(0).getRunId()).build();
            Feature f2 = Feature.builder().alignedFeatureId(af.getAlignedFeatureId()).apexIntensity(1.0).runId(runs.get(1).getRunId()).build();
            ps.getStorage().insertAll(List.of(f1, f2));

            project.addTagGroup("sample", "tags.sample:sample", "type1");
            project.addTagGroup("blank", "tags.sample:blank", "type1");

            new BackgroundRuns(project, null).runFoldChange("sample", "blank", AggregationType.AVG, QuantMeasure.APEX_INTENSITY, AlignedFeature.class).awaitResult();

            List<FoldChange> fc = project.getFoldChanges(AlignedFeature.class, Long.toString(af.getAlignedFeatureId()));
            Assert.assertEquals(1, fc.size());
            Assert.assertEquals(2.0, fc.getFirst().getFoldChange(), Double.MIN_VALUE);
            Assert.assertEquals(FoldChange.class, fc.getFirst().getClass());
            Assert.assertEquals(Long.toString(af.getAlignedFeatureId()), fc.getFirst().getObjectId());
            fc = project.listFoldChanges(AlignedFeature.class, Pageable.unpaged()).getContent();
            Assert.assertEquals(1, fc.size());
            Assert.assertEquals(2.0, fc.getFirst().getFoldChange(), Double.MIN_VALUE);
            Assert.assertEquals(FoldChange.class, fc.getFirst().getClass());
            Assert.assertEquals(Long.toString(af.getAlignedFeatureId()), fc.getFirst().getObjectId());


            AlignedFeatures af2 = AlignedFeatures.builder().name("af2").build();
            ps.getStorage().insert(af2);

            Feature f21 = Feature.builder().alignedFeatureId(af2.getAlignedFeatureId()).apexIntensity(3.0).runId(runs.get(0).getRunId()).build();
            Feature f22 = Feature.builder().alignedFeatureId(af2.getAlignedFeatureId()).apexIntensity(1.0).runId(runs.get(1).getRunId()).build();
            ps.getStorage().insertAll(List.of(f21, f22));

            new BackgroundRuns(project, null).runFoldChange("sample", "blank", AggregationType.AVG, QuantMeasure.APEX_INTENSITY, AlignedFeature.class).awaitResult();
            new BackgroundRuns(project, null).runFoldChange("sample", "blank", AggregationType.MAX, QuantMeasure.APEX_INTENSITY, AlignedFeature.class).awaitResult();

            StatisticsTable table1 = project.getFoldChangeTable(AlignedFeature.class, AggregationType.AVG, QuantMeasure.APEX_INTENSITY);
            StatisticsTable table2 = project.getFoldChangeTable(AlignedFeature.class, AggregationType.MAX, QuantMeasure.APEX_INTENSITY);
            StatisticsTable table3 = project.getFoldChangeTable(AlignedFeature.class, AggregationType.MIN, QuantMeasure.APEX_INTENSITY);

            Assert.assertEquals(AggregationType.AVG, table1.getAggregationType());
            Assert.assertEquals(AggregationType.MAX, table2.getAggregationType());
            Assert.assertEquals(AggregationType.MIN, table3.getAggregationType());

            Assert.assertEquals(QuantMeasure.APEX_INTENSITY, table1.getQuantificationMeasure());
            Assert.assertEquals(QuantMeasure.APEX_INTENSITY, table2.getQuantificationMeasure());
            Assert.assertEquals(QuantMeasure.APEX_INTENSITY, table3.getQuantificationMeasure());

            Assert.assertEquals(QuantRowType.FEATURES, table1.getRowType());
            Assert.assertEquals(QuantRowType.FEATURES, table2.getRowType());
            Assert.assertEquals(QuantRowType.FEATURES, table3.getRowType());

            Assert.assertEquals(StatisticsType.FOLD_CHANGE, table1.getStatisticsType());
            Assert.assertEquals(StatisticsType.FOLD_CHANGE, table2.getStatisticsType());
            Assert.assertEquals(StatisticsType.FOLD_CHANGE, table3.getStatisticsType());

            Assert.assertEquals(1, table1.getColumnNames().length);
            Assert.assertEquals("sample / blank", table1.getColumnNames()[0]);
            Assert.assertEquals(1, table1.getColumnLeftGroups().length);
            Assert.assertEquals("sample", table1.getColumnLeftGroups()[0]);
            Assert.assertEquals(1, table1.getColumnRightGroups().length);
            Assert.assertEquals("blank", table1.getColumnRightGroups()[0]);

            Assert.assertEquals(1, table2.getColumnNames().length);
            Assert.assertEquals("sample / blank", table2.getColumnNames()[0]);
            Assert.assertEquals(1, table2.getColumnLeftGroups().length);
            Assert.assertEquals("sample", table2.getColumnLeftGroups()[0]);
            Assert.assertEquals(1, table2.getColumnRightGroups().length);
            Assert.assertEquals("blank", table2.getColumnRightGroups()[0]);

            Assert.assertEquals(0, table3.getColumnNames().length);
            Assert.assertEquals(0, table3.getColumnLeftGroups().length);
            Assert.assertEquals(0, table3.getColumnRightGroups().length);

            Assert.assertEquals(2, table1.getRowIds().length);
            Assert.assertTrue(Arrays.binarySearch(table1.getRowIds(), af.getAlignedFeatureId()) >= 0);
            Assert.assertTrue(Arrays.binarySearch(table1.getRowIds(), af2.getAlignedFeatureId()) >= 0);

            double[] vals = Arrays.stream(table1.getValues()).mapToDouble(col -> {
                Assert.assertEquals(1, col.length);
                return col[0];
            }).toArray();
            Assert.assertEquals(2, vals.length);
            Assert.assertTrue(Arrays.binarySearch(vals, 2.0) >= 0);
            Assert.assertTrue(Arrays.binarySearch(vals, 3.0) >= 0);

            vals = Arrays.stream(table2.getValues()).mapToDouble(col -> {
                Assert.assertEquals(1, col.length);
                return col[0];
            }).toArray();
            Assert.assertEquals(2, vals.length);
            Assert.assertTrue(Arrays.binarySearch(vals, 2.0) >= 0);
            Assert.assertTrue(Arrays.binarySearch(vals, 3.0) >= 0);

            Assert.assertEquals(0, table3.getValues().length);

            project.deleteFoldChange(AlignedFeature.class, "sample", "blank", AggregationType.AVG, QuantMeasure.APEX_INTENSITY);
            project.deleteFoldChange(AlignedFeature.class, "sample", "blank", AggregationType.MAX, QuantMeasure.APEX_INTENSITY);
            fc = project.listFoldChanges(AlignedFeature.class, Pageable.unpaged()).getContent();
            Assert.assertEquals(0, fc.size());
        }
    }

    @Test
    public void testTags() throws IOException {

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, new FakeLuceneSearchService(), (a, b) -> false);

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

            project.createTags(List.of(TagDefinitionImport.builder().tagName("c1").valueType(ValueType.BOOLEAN).build()), true);

            project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.builder().tagName("c1").value(true).build()));
            Map<String, ? extends Tag> tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(1, tags.size());
//            Assert.assertEquals(TagDefinitionImport.ValueType.BOOLEAN, tags.get("c1").getValueType());
            Assert.assertEquals(true, tags.get("c1").getValue());

            project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.builder().tagName("c1").value(false).build()));
            tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(1, tags.size());
//            Assert.assertEquals(TagDefinitionImport.ValueType.BOOLEAN, tags.get("c1").getValueType());
            Assert.assertEquals(false, tags.get("c1").getValue());

            Assert.assertThrows(ResponseStatusException.class, () -> project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.builder().tagName("c2").value(false).build())));
            Assert.assertThrows(ResponseStatusException.class, () -> project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.builder().tagName("c1").value(2.0).build())));

            project.createTags(List.of(
                    TagDefinitionImport.builder().tagName("c2").valueType(ValueType.INTEGER).build(),
                    TagDefinitionImport.builder().tagName("c3").valueType(ValueType.REAL).build(),
                    TagDefinitionImport.builder().tagName("c4").valueType(ValueType.TEXT).build()
            ), true);

            project.addTagsToObject(Run.class, run.getRunId(), List.of(
                    Tag.builder().tagName("c1").value(false).build(),
                    Tag.builder().tagName("c2").value(42).build(),
                    Tag.builder().tagName("c3").value(42.0).build(),
                    Tag.builder().tagName("c4").value("42").build()
            ));

            tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(4, tags.size());
//            Assert.assertEquals(TagDefinitionImport.ValueType.BOOLEAN, tags.get("c1").getValueType());
//            Assert.assertEquals(TagDefinitionImport.ValueType.INTEGER, tags.get("c2").getValueType());
//            Assert.assertEquals(TagDefinitionImport.ValueType.DOUBLE, tags.get("c3").getValueType());
//            Assert.assertEquals(TagDefinitionImport.ValueType.STRING, tags.get("c4").getValueType());
            Assert.assertEquals(false, tags.get("c1").getValue());
            Assert.assertEquals(Integer.valueOf(42), tags.get("c2").getValue());
            Assert.assertEquals(Double.valueOf(42.0), tags.get("c3").getValue());
            Assert.assertEquals("42", tags.get("c4").getValue());

            project.removeTagsFromObject(run.getClass(), run.getRunId(), List.of("c3", "c4"));
            tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(2, tags.size());
//            Assert.assertEquals(TagDefinitionImport.ValueType.BOOLEAN, tags.get("c1").getValueType());
//            Assert.assertEquals(TagDefinitionImport.ValueType.INTEGER, tags.get("c2").getValueType());
            Assert.assertEquals(false, tags.get("c1").getValue());
            Assert.assertEquals(Integer.valueOf(42), tags.get("c2").getValue());

            //todo Implement search
            Page<Run> page = project.findObjectsByTagFilter(Run.class, "tags.c2:[12 TO 43]", Pageable.unpaged(), EnumSet.of(Run.OptField.tags));
            Assert.assertEquals(1, page.getTotalElements());
            Assert.assertEquals(Long.toString(runs.getFirst().getRunId()), page.getContent().getFirst().getRunId());
            Assert.assertEquals(2, tags.size());
//            Assert.assertEquals(TagDefinitionImport.ValueType.BOOLEAN, tags.get("c1").getValueType());
//            Assert.assertEquals(TagDefinitionImport.ValueType.INTEGER, tags.get("c2").getValueType());
            Assert.assertEquals(false, tags.get("c1").getValue());
            Assert.assertEquals(Integer.valueOf(42), tags.get("c2").getValue());

            project.deleteTags("c2");
            project.deleteTags("c3");
            tags = project.findRunById(run.getRunId(), EnumSet.of(Run.OptField.tags)).getTags();
            Assert.assertEquals(1, tags.size());
//            Assert.assertEquals(TagDefinitionImport.ValueType.BOOLEAN, tags.get("c1").getValueType());
            Assert.assertEquals(false, tags.get("c1").getValue());

            page = project.findObjectsByTagFilter(Run.class, "tags.c1:false", Pageable.unpaged(), EnumSet.of(Run.OptField.tags));
            Assert.assertEquals(1, page.getTotalElements());
            Assert.assertEquals(run.getRunId(), page.getContent().getFirst().getRunId());
            tags = page.get().findFirst().orElseThrow().getTags();
            Assert.assertEquals(1, tags.size());
//            Assert.assertEquals(TagDefinitionImport.ValueType.BOOLEAN, tags.get("c1").getValueType());
            Assert.assertEquals(false, tags.get("c1").getValue());

            project.createTags(List.of(
                    TagDefinitionImport.builder().tagName("date").valueType(ValueType.DATE).build(),
                    TagDefinitionImport.builder().tagName("time").valueType(ValueType.TIME).build()
            ), true);

            final Run run2 = project.findRunById(Long.toString(runs.get(1).getRunId()));
            project.addTagsToObject(Run.class, run2.getRunId(), List.of(
                    Tag.builder().tagName("date").value("2024-12-31").build(),
                    Tag.builder().tagName("time").value("12:00:00").build()
            ));

            page = project.findObjectsByTagFilter(Run.class, "tags.date:[2024-12-01 TO 2025-12-31] OR tags.time:12\\:00\\:00", Pageable.unpaged(), EnumSet.of(Run.OptField.tags));
            Assert.assertEquals(1, page.getTotalElements());
            Assert.assertEquals(run2.getRunId(), page.getContent().getFirst().getRunId());
            tags = page.get().findFirst().orElseThrow().getTags();
            Assert.assertEquals(2, tags.size());
//            Assert.assertEquals(ValueType.DATE.getTagValueClass(), tags.get("date").getValue().getClass());
            Assert.assertEquals("2024-12-31", tags.get("date").getValue());
//            Assert.assertEquals(ValueType.TIME.getTagValueClass(), tags.get("time").getValue().getClass());
            Assert.assertEquals("12:00:00", tags.get("time").getValue());

            Assert.assertThrows(ResponseStatusException.class, () -> project.findObjectsByTagFilter(Run.class, "", Pageable.unpaged(), EnumSet.of(Run.OptField.tags)));
        }

    }

    @Test
    public void testMany() throws IOException {

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, new FakeLuceneSearchService(), (a, b) -> false);

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

            project.createTags(List.of(
                    TagDefinitionImport.builder().tagType("sampleCat").tagName("sample-type").valueType(ValueType.TEXT).possibleValues(List.of("control", "blank", "sample")).build()
            ), true);

            StopWatch watch = new StopWatch();
            watch.start();

            for (Run run : control) {
                project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.builder().tagName("sample-type").value("control").build()));
            }
            for (Run run : blank) {
                project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.builder().tagName("sample-type").value("blank").build()));
            }
            for (Run run : sample) {
                project.addTagsToObject(Run.class, run.getRunId(), List.of(Tag.builder().tagName("sample-type").value("sample").build()));
            }

            watch.stop();
            System.out.println("CREATE TAGS: " + watch);

            watch = new StopWatch();
            watch.start();

            project.findObjectsByTagFilter(Run.class, "tags.sample-type:sample", Pageable.unpaged(), EnumSet.of(Run.OptField.tags));

            watch.stop();
            System.out.println("FIND OBJ BY TAGS: " + watch);
        }

    }

//    @Test
//    public void testFilterTranslation() throws IOException, QueryNodeException, InvocationTargetException, IllegalAccessException, ParseException {
//        Filter filter = LuceneUtils.translateTagFilter("(test || bla) && \"new york\" AND /[mb]oat/ AND integer:[1 TO *] OR real<=3 date:2024-01-01 date:[2023-10-01 TO 2023-12-24] date<2022-01-01 time:12\\:00\\:00 time:[12\\:00\\:00 TO 14\\:00\\:00} time<10\\:00\\:00");
//        Assert.assertEquals(
//                "(((text==test OR text==bla) AND text==new york AND text~=/[mb]oat/ AND int32:[1, " + Integer.MAX_VALUE + "]) OR real:[-Infinity, 3.0] OR " +
//                        "int64:[" + TaggableController.DATE_FORMAT.parse("2024-01-01").getTime() + ", " + TaggableController.DATE_FORMAT.parse("2024-01-01").getTime() + "] OR " +
//                        "int64:[" + TaggableController.DATE_FORMAT.parse("2023-10-01").getTime() + ", " + TaggableController.DATE_FORMAT.parse("2023-12-24").getTime() + "] OR " +
//                        "int64:[" + Long.MIN_VALUE + ", " + (TaggableController.DATE_FORMAT.parse("2022-01-01").getTime() - 1) + "] OR " +
//                        "int64:[" + TaggableController.TIME_FORMAT.parse("12:00:00").getTime() + ", " + TaggableController.TIME_FORMAT.parse("12:00:00").getTime() + "] OR " +
//                        "int64:[" + TaggableController.TIME_FORMAT.parse("12:00:00").getTime() + ", " + (TaggableController.TIME_FORMAT.parse("14:00:00").getTime() - 1) + "] OR " +
//                        "int64:[" + Long.MIN_VALUE + ", " + (TaggableController.TIME_FORMAT.parse("10:00:00").getTime() - 1) + "])",
//                filter.toString()
//        );
//    }

}
