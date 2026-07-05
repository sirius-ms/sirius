package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.service.search.mappers.TagMapper;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Phase-2 unit tests for the mapper-specific fixes.
 * <ul>
 *   <li>H6 — TagMapper integer tags round-trip (numeric read-back, not stringValue()).</li>
 * </ul>
 */
public class MapperFixesTest {

    @Test
    public void integerTagRoundTrip_H6() {
        TagMapper mapper = new TagMapper(name -> ValueType.INTEGER);
        Map<String, Tag> tags = Map.of("count", Tag.builder().tagName("count").value(42).build());

        Document doc = new Document();
        mapper.toIndexableFields("tags", tags).forEach(doc::add);

        Map<String, Tag> restored = mapper.toPojo("tags", doc);
        assertEquals(Integer.valueOf(42), restored.get("count").getValue(),
                "integer tag must round-trip via numericValue(), not stringValue() (H6)");
    }
}
