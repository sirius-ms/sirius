package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.FeatureImport;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.middleware.model.tags.TagDefinition;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase-0 harness T0.3: guarantees the invariant that the Lucene search index stays consistent with the
 * NoSQL database across CRUD performed through {@link NoSQLProjectImpl}.
 * <p>
 * Regression guards for two fixed drift bugs:
 * <ul>
 *   <li>A2 — {@code addCompounds} (the direct-import chokepoint) now adds imported features to the search
 *       index; imported data is searchable without a manual rebuild step.</li>
 *   <li>B1 — per-entity deletes cascade the DB delete <em>and</em> remove the entity from the search index,
 *       so deleted features/compounds are no longer searchable.</li>
 * </ul>
 */
public class IndexConsistencyTest {

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
                        .rtStartSeconds(6d)
                        .rtApexSeconds(10d)
                        .rtEndSeconds(12d)
                        .mergedMs1(ms1)
                        .ms1Spectra(List.of(ms1))
                        .ms2Spectra(List.of(ms2, ms2))
                        .build())
        ).build();
    }

    private List<Compound> addTwoCompounds() {
        return project.addCompounds(
                List.of(compound("alpha", "FID-A"), compound("beta", "FID-B")),
                null,
                EnumSet.noneOf(Compound.OptField.class),
                EnumSet.noneOf(AlignedFeature.OptField.class),
                "src");
    }

    private List<String> indexedFeatureIds() {
        Page<String> ids = project.getSearchService()
                .searchIds(project.getProjectId(), null, Pageable.unpaged(), AlignedFeature.class);
        return ids.getContent();
    }

    /**
     * A2 — {@code addCompounds} must keep the search index in sync: features imported through it are
     * searchable immediately, with no manual/rebuild indexing step.
     */
    @Test
    public void testAddCompoundsIndexesFeatures() {
        List<Compound> compounds = addTwoCompounds();
        assertEquals(2, compounds.size());

        List<String> indexed = indexedFeatureIds();
        assertEquals(2, indexed.size(),
                "addCompounds must add both imported aligned features to the search index (A2)");
    }

    /**
     * B1 — deleting an aligned feature must also remove it from the search index.
     */
    @Test
    public void testDeleteFeatureRemovesFromIndex() {
        List<Compound> compounds = addTwoCompounds();
        String deletedFeatureId = compounds.stream()
                .flatMap(c -> c.getFeatures().stream())
                .filter(f -> "alpha".equals(f.getName()))
                .map(AlignedFeature::getAlignedFeatureId)
                .findFirst().orElseThrow();

        project.deleteAlignedFeaturesById(deletedFeatureId);

        // Sanity: the DB delete happened.
        Page<AlignedFeature> remainingInDb = project.findAlignedFeatures(
                Pageable.unpaged(), false, EnumSet.noneOf(AlignedFeature.OptField.class));
        assertEquals(1, remainingInDb.getTotalElements(), "feature must be gone from the DB");

        // The actual defect: the deleted feature must no longer be in the search index.
        List<String> indexed = indexedFeatureIds();
        assertFalse(indexed.contains(deletedFeatureId),
                "deleted aligned feature must be removed from the search index (B1)");
        assertEquals(1, indexed.size(),
                "exactly one feature should remain indexed after deleting one of two (B1)");
    }

    /**
     * B1 — deleting a compound must also remove its aligned features from the search index.
     */
    @Test
    public void testDeleteCompoundRemovesFeaturesFromIndex() {
        List<Compound> compounds = addTwoCompounds();
        Compound toDelete = compounds.stream().filter(c -> "alpha".equals(c.getName())).findFirst().orElseThrow();
        String deletedFeatureId = toDelete.getFeatures().getFirst().getAlignedFeatureId();

        project.deleteCompoundById(toDelete.getCompoundId());

        List<String> indexed = indexedFeatureIds();
        assertFalse(indexed.contains(deletedFeatureId),
                "aligned feature of a deleted compound must be removed from the search index (B1)");
    }
}
