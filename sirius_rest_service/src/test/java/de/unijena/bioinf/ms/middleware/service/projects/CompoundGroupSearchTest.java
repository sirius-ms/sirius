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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Phase-6 (M6): compound tag-group filtering. Compound-level tagging/indexing does not exist yet, so the group
 * is resolved on the aligned-feature index and the matching features are aggregated to their compounds.
 */
public class CompoundGroupSearchTest {

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
    public void compoundGroupFilteringViaFeatureIndex_M6() {
        project.createTags(List.of(
                TagDefinitionImport.builder().tagName("sample").valueType(ValueType.TEXT)
                        .possibleValues(List.of("sample", "blank")).build()), true);

        List<Compound> compounds = project.addCompounds(
                List.of(compound("alpha", "FID-A"), compound("beta", "FID-B")),
                null, EnumSet.noneOf(Compound.OptField.class), EnumSet.noneOf(AlignedFeature.OptField.class), "src");

        // Index the aligned features (addCompounds does not index them itself).
        List<AlignedFeature> feats = project.findAlignedFeatures(
                Pageable.unpaged(), false, EnumSet.noneOf(AlignedFeature.OptField.class)).getContent();
        project.getSearchService().addDocuments(project.getProjectId(), feats);

        Compound alpha = compounds.stream().filter(c -> "alpha".equals(c.getName())).findFirst().orElseThrow();
        Compound beta = compounds.stream().filter(c -> "beta".equals(c.getName())).findFirst().orElseThrow();

        // Tag alpha's feature 'sample' and beta's feature 'blank'.
        project.addTagsToObject(AlignedFeature.class, alpha.getFeatures().getFirst().getAlignedFeatureId(),
                List.of(Tag.builder().tagName("sample").value("sample").build()));
        project.addTagsToObject(AlignedFeature.class, beta.getFeatures().getFirst().getAlignedFeatureId(),
                List.of(Tag.builder().tagName("sample").value("blank").build()));

        project.addTagGroup("g_sample", "tags.sample:sample", "type1");

        Page<Compound> result = project.findCompoundsByGroup("g_sample", Pageable.unpaged(), false,
                EnumSet.noneOf(Compound.OptField.class), EnumSet.noneOf(AlignedFeature.OptField.class));

        assertEquals(1, result.getTotalElements(),
                "group must match exactly the compound whose feature is tagged 'sample' (M6)");
        assertEquals(alpha.getCompoundId(), result.getContent().getFirst().getCompoundId());
    }
}
