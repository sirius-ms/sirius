package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.QuantRowType;
import de.unijena.bioinf.ms.middleware.model.statistics.FoldChange;
import de.unijena.bioinf.ms.middleware.model.statistics.Statistics;
import de.unijena.bioinf.ms.middleware.service.search.mappers.FoldChangeMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.GenericPojoMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneMappingUtils;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.projectspace.IndexField;
import de.unijena.bioinf.projectspace.QueryRewriter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.SortField;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase-2/6 mapper edge cases (M11 sub-cases):
 * <ul>
 *   <li>unsupported "simple" field types (BigDecimal/short/char/...) are rejected with a clear error instead
 *       of being silently keyword-indexed and breaking on read-back;</li>
 *   <li>a POJO without a no-argument constructor gives a clear error on read-back;</li>
 *   <li>FoldChange group names containing '.' round-trip (the field-name delimiter is escaped).</li>
 * </ul>
 */
public class MapperEdgeCaseTest {

    // ---- unsupported field type is rejected ----

    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnsupportedTypePojo {
        @IndexField(name = "id", documentId = true)
        public String id;
        @IndexField(name = "ratio")
        public BigDecimal ratio;
    }

    @Test
    public void unsupportedFieldTypeIsRejected_M11() {
        GenericPojoMapper<UnsupportedTypePojo> mapper = new GenericPojoMapper<>(UnsupportedTypePojo.class);
        assertThrows(IllegalArgumentException.class, () -> mapper.detectAnalyzersAndPointConfigs(
                        new HashMap<String, PointsConfig>(), new HashMap<String, Analyzer>(), new ArrayList<CharSequence>(),
                        new HashMap<String, SortField.Type>(), new HashMap<String, QueryRewriter>()),
                "an unsupported numeric field type must be rejected with a clear error (M11)");
    }

    // ---- missing no-arg constructor ----

    public static class NoDefaultCtorPojo {
        @IndexField(name = "id", documentId = true)
        public String id;

        public NoDefaultCtorPojo(String id) {
            this.id = id;
        }
    }

    @Test
    public void missingNoArgConstructorGivesClearError_M11() {
        GenericPojoMapper<NoDefaultCtorPojo> mapper = new GenericPojoMapper<>(NoDefaultCtorPojo.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> mapper.toPojo(new Document()),
                "reconstructing a POJO without a no-arg constructor must give a clear error (M11)");
        assertTrue(ex.getMessage().toLowerCase().contains("no-arg"),
                "error should mention the missing no-arg constructor, was: " + ex.getMessage());
    }

    // ---- FoldChange group name containing '.' ----

    @Test
    public void foldChangeGroupNameWithDotRoundTrips_M11() {
        FoldChangeMapper mapper = new FoldChangeMapper.AlignedFeatureFoldChange();
        String objectIdField = LuceneMappingUtils.getDocumentIdFieldName(AlignedFeature.class).orElseThrow();

        FoldChange fc = FoldChange.builder()
                .objectId("1")
                .quantType(QuantRowType.FEATURES)
                .leftGroup("group.with.dots")
                .rightGroup("control")
                .aggregation(AggregationType.AVG)
                .quantification(QuantMeasure.APEX_INTENSITY)
                .foldChange(2.0)
                .leftAbundance(1.0)
                .rightAbundance(0.5)
                .build();

        Document doc = new Document();
        // Only stored fields are returned by a Lucene search (searcher.storedFields()), so add only those —
        // adding the non-stored DoublePoint/DocValues fields would not reflect the real read path.
        mapper.toIndexableFields("stats", List.<Statistics>of(fc))
                .forEach(f -> { if (f.fieldType().stored()) doc.add(f); });
        doc.add(new StringField(objectIdField, "1", Field.Store.YES));

        List<Statistics> restored = mapper.toPojo("stats", doc);
        assertNotNull(restored);
        assertEquals(1, restored.size());
        FoldChange r = (FoldChange) restored.getFirst();
        assertEquals("group.with.dots", r.getLeftGroup(), "left group with '.' must round-trip (M11)");
        assertEquals("control", r.getRightGroup());
        assertEquals(AggregationType.AVG, r.getAggregation());
        assertEquals(QuantMeasure.APEX_INTENSITY, r.getQuantification());
    }
}
