package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.FeatureImport;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.model.tags.TagDefinition;
import de.unijena.bioinf.ms.middleware.model.tags.TagDefinitionImport;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A4 — tag values must be returned in the same (formatted) representation on every endpoint.
 * <p>
 * The bulk aligned-feature listing ({@code findAlignedFeatures} with {@code OptField.tags}) builds tags via a
 * bulk tag query and emitted the raw stored value (e.g. epoch millis for a DATE tag), while the per-object tag
 * endpoint and the Run/Compound paths emit the formatted value (e.g. "yyyy-MM-dd"). Two fixes make the bulk
 * feature path correct: (1) the bulk tag query filtered on a non-existent {@code alignedFeatureId} field
 * instead of {@code taggedObjectId}, so it silently returned nothing and fell back to the per-feature path;
 * once it actually loads tags, (2) it must format them like every other endpoint. This test pins both: DATE
 * and TIME tags come back formatted on the bulk listing and match the per-object tag endpoint.
 */
public class TagValueFormattingTest {

    @SneakyThrows
    private static SearchService makeSearchService() {
        return new SearchServiceImpl(project -> {
            Map<String, ValueType> tagDefinitions = new HashMap<>();
            for (Object item : project.findTags()) {
                TagDefinition td = (TagDefinition) item;
                tagDefinitions.put(td.getTagName(), td.getValueType());
            }
            return new PerPojoSearchContext(null, tagDefinitions);
        });
    }

    private NitriteSirirusProject ps;
    private NoSQLProjectImpl project;

    @BeforeEach
    public void createTestProject() throws IOException {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
        project = new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), makeSearchService(), (a, b) -> false);
    }

    @AfterEach
    public void closeProject() throws Exception {
        if (ps != null)
            ps.close();
    }

    private static CompoundImport compound(String name, String externalFeatureId) {
        BasicSpectrum ms1 = new BasicSpectrum(new double[]{1, 2, 42}, new double[]{1, 2, 3});
        BasicSpectrum ms2 = new BasicSpectrum(new double[]{1, 2, 42}, new double[]{1, 2, 3});
        ms2.setCollisionEnergy(CollisionEnergy.fromString("20eV"));
        ms2.setMsLevel(2);
        ms2.setPrecursorMz(42d);
        ms2.setScanNumber(5);
        return CompoundImport.builder().name(name).features(
                List.of(FeatureImport.builder()
                        .externalFeatureId(externalFeatureId)
                        .name(name)
                        .ionMass(42d)
                        .charge((byte) 1)
                        .detectedAdducts(java.util.Set.of("M+H+"))
                        .rtStartSeconds(6d).rtApexSeconds(10d).rtEndSeconds(12d)
                        .mergedMs1(ms1)
                        .ms1Spectra(List.of(ms1))
                        .ms2Spectra(List.of(ms2, ms2))
                        .build())
        ).build();
    }

    @Test
    public void dateAndTimeTagValuesAreFormattedOnBulkFeatureListing() {
        project.createTags(List.of(
                TagDefinitionImport.builder().tagName("date").valueType(ValueType.DATE).build(),
                TagDefinitionImport.builder().tagName("time").valueType(ValueType.TIME).build()
        ), true);

        List<Compound> compounds = project.addCompounds(
                List.of(compound("alpha", "FID-A")),
                null, EnumSet.noneOf(Compound.OptField.class), EnumSet.noneOf(AlignedFeature.OptField.class), "src");
        String featureId = compounds.getFirst().getFeatures().getFirst().getAlignedFeatureId();

        // clients submit tag values in their formatted representation
        project.addTagsToObject(AlignedFeature.class, featureId, List.of(
                Tag.builder().tagName("date").value("2024-12-31").build(),
                Tag.builder().tagName("time").value("12:00:00").build()
        ));

        // path under test: the bulk aligned-feature listing with OptField.tags
        AlignedFeature feature = project.findAlignedFeatures(Pageable.unpaged(), false, EnumSet.of(AlignedFeature.OptField.tags))
                .getContent().stream()
                .filter(f -> featureId.equals(f.getAlignedFeatureId()))
                .findFirst().orElseThrow();
        Map<String, Tag> bulkTags = feature.getTags();
        Object dateValue = bulkTags.get("date").getValue();
        Object timeValue = bulkTags.get("time").getValue();
        assertTrue(dateValue instanceof String, "DATE tag must be returned formatted (String), not as raw epoch millis (A4)");
        assertTrue(timeValue instanceof String, "TIME tag must be returned formatted (String), not as a raw int (A4)");
        assertEquals("2024-12-31", dateValue, "DATE tag must be formatted as yyyy-MM-dd on the bulk feature listing (A4)");
        assertEquals("12:00:00", timeValue, "TIME tag must be formatted as HH:mm:ss on the bulk feature listing (A4)");

        // and it must agree with the dedicated per-object tag endpoint
        Map<String, Tag> directTags = project.findTagsByObject(AlignedFeature.class, featureId).stream()
                .collect(Collectors.toMap(Tag::getTagName, t -> t));
        assertEquals(directTags.get("date").getValue(), dateValue,
                "DATE tag value must match between the bulk listing and the per-object tag endpoint (A4)");
        assertEquals(directTags.get("time").getValue(), timeValue,
                "TIME tag value must match between the bulk listing and the per-object tag endpoint (A4)");
    }
}
