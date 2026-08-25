package de.unijena.bioinf.ms.middleware.service.search.mappers;

import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TagMapperTest {

    @Test
    public void unknownTagDefinitionFailsWithADescriptiveError() {
        // A tag whose definition the search context does not know about: the mapper cannot pick a field
        // encoding for it. It used to dereference the missing ValueType and surface a bare NPE from deep
        // inside the Lucene write path, which said nothing about which tag was at fault.
        TagMapper mapper = new TagMapper(tagName -> null);
        Map<String, Tag> tags = Map.of("c1", Tag.builder().tagName("c1").value(true).build());

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> mapper.toIndexableFields("tags", tags),
                "an unregistered tag definition must fail with an explicit error, not a NullPointerException");
        assertTrue(e.getMessage().contains("c1"),
                "the error must name the offending tag, was: " + e.getMessage());
    }

    @Test
    public void knownTagDefinitionIsIndexed() {
        TagMapper mapper = new TagMapper(tagName -> ValueType.BOOLEAN);
        Map<String, Tag> tags = Map.of("c1", Tag.builder().tagName("c1").value(true).build());

        assertFalse(mapper.toIndexableFields("tags", tags).isEmpty(),
                "a tag with a registered value type must still produce indexable fields");
    }
}
