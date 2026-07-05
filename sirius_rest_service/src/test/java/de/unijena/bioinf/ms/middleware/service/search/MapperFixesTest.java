package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.model.annotations.CompoundClasses;
import de.unijena.bioinf.ms.middleware.model.annotations.LipidAnnotation;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.service.search.mappers.CompoundClassesMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.GenericPojoMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.LipidAnnotationMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.TagMapper;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.projectspace.IndexField;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase-2 unit tests for the mapper-specific fixes.
 * <ul>
 *   <li>H6 — TagMapper integer tags round-trip (numeric read-back, not stringValue()).</li>
 *   <li>H8 — LipidAnnotationMapper indexes the correct getters; CompoundClassesMapper tolerates null NPC;
 *       GenericPojoMapper.getIdValue works for renamed and inherited id fields.</li>
 * </ul>
 */
public class MapperFixesTest {

    // ---- H6: TagMapper integer round-trip ----

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

    // ---- H8a: LipidAnnotationMapper reads the correct getters ----

    @Test
    public void lipidAnnotationIndexesCorrectFields_H8() {
        LipidAnnotation lipid = LipidAnnotation.builder()
                .lipidSpecies("PC 34:1")
                .lipidMapsId("LMGP01010005")
                .lipidClassName("PC")
                .build();

        Map<String, String> byName = new HashMap<>();
        for (IndexableField f : new LipidAnnotationMapper().toIndexableFields("lipid", lipid))
            byName.putIfAbsent(f.name(), f.stringValue());

        assertEquals("LMGP01010005", byName.get("lipid.lipidMapsId"),
                "lipidMapsId field must come from getLipidMapsId(), not getLipidSpecies() (H8)");
        assertEquals("PC", byName.get("lipid.lipidClassName"),
                "lipidClassName field must come from getLipidClassName(), not getLipidSpecies() (H8)");
    }

    // ---- H8b: CompoundClassesMapper tolerates missing NPC classification ----

    @Test
    public void compoundClassesWithoutNpcDoesNotThrow_H8() {
        CompoundClasses cc = new CompoundClasses(); // no NPC, no ClassyFire
        assertDoesNotThrow(() -> new CompoundClassesMapper().toIndexableFields("classes", cc),
                "indexing a compound without NPC classification must not NPE (H8)");
    }

    // ---- H8c: getIdValue for renamed and inherited id fields ----

    @NoArgsConstructor
    @AllArgsConstructor
    public static class RenamedIdPojo {
        @IndexField(name = "docId", documentId = true)
        public String identifier;
    }

    @Test
    public void getIdValueWorksForRenamedField_H8() {
        GenericPojoMapper<RenamedIdPojo> mapper = new GenericPojoMapper<>(RenamedIdPojo.class);
        assertEquals("ID-1", mapper.getIdValue(new RenamedIdPojo("ID-1")),
                "getIdValue must resolve a field whose @IndexField name differs from the Java field name (H8)");
    }

    @NoArgsConstructor
    public static class ParentWithId {
        @IndexField(name = "id", documentId = true)
        public String id;
    }

    @NoArgsConstructor
    public static class ChildPojo extends ParentWithId {
        @IndexField(name = "extra")
        public String extra;
    }

    @Test
    public void getIdValueWorksForInheritedField_H8() {
        GenericPojoMapper<ChildPojo> mapper = new GenericPojoMapper<>(ChildPojo.class);
        ChildPojo child = new ChildPojo();
        child.id = "ID-2";
        assertEquals("ID-2", mapper.getIdValue(child),
                "getIdValue must resolve an inherited id field (H8)");
    }
}
