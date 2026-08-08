package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Numbers and dates are stored as numeric values in the index, so reading them as text would silently yield nothing.
 * These fields therefore have to be converted the same way they are when a whole object is reconstructed.
 */
class DocumentIndexedFieldsTest {

    private static final long A_DATE = 1_700_000_000_000L;

    private static LuceneDocumentIndexedFields fields() {
        Document document = new Document();
        document.add(new StringField("alignedFeatureId", "42", Field.Store.YES));
        document.add(new StringField("name", "caffeine", Field.Store.YES));
        document.add(new StoredField("ionMass", 195.0876d));
        document.add(new StoredField("intensity", 1234.5f));
        document.add(new StoredField("charge", 1));
        document.add(new StoredField("runId", 7L));
        document.add(new StringField("blank", "true", Field.Store.YES));
        document.add(new StoredField("measured", A_DATE));
        document.add(new StringField("measure", QuantMeasure.AREA_UNDER_CURVE.name(), Field.Store.YES));
        document.add(new StringField("adduct", "[M+H]+", Field.Store.YES));
        document.add(new StringField("adduct", "[M+Na]+", Field.Store.YES));
        return new LuceneDocumentIndexedFields(document);
    }

    @Test
    @DisplayName("text is read as it was stored")
    void textFields() {
        assertEquals("caffeine", fields().getString("name"));
        assertEquals("42", fields().getString("alignedFeatureId"));
    }

    @Test
    @DisplayName("numbers are read from their numeric value, not from text")
    void numericFields() {
        LuceneDocumentIndexedFields fields = fields();

        assertEquals(195.0876d, fields.getDouble("ionMass"));
        assertEquals(1234.5f, fields.getFloat("intensity"));
        assertEquals(1, fields.getInt("charge"));
        assertEquals(7L, fields.getLong("runId"));
    }

    @Test
    @DisplayName("booleans, dates and enums are converted as well")
    void otherSimpleTypes() {
        LuceneDocumentIndexedFields fields = fields();

        assertEquals(Boolean.TRUE, fields.getBoolean("blank"));
        assertEquals(new Date(A_DATE), fields.getDate("measured"));
        assertEquals(QuantMeasure.AREA_UNDER_CURVE, fields.getEnum("measure", QuantMeasure.class));
    }

    @Test
    @DisplayName("primitive types can be requested as well")
    void primitiveTypeLiterals() {
        assertEquals(7L, fields().get("runId", long.class));
        assertEquals(1, fields().get("charge", int.class));
    }

    @Test
    @DisplayName("a field that is absent or was not requested is null")
    void missingFieldsAreNull() {
        assertNull(fields().getString("notRequested"));
        assertNull(fields().getLong("notRequested"));
    }

    @Test
    @DisplayName("all values of a repeated field can be read")
    void repeatedFields() {
        List<String> adducts = fields().getAll("adduct", String.class);

        assertEquals(List.of("[M+H]+", "[M+Na]+"), adducts);
        assertEquals("[M+H]+", fields().getString("adduct"), "a single read returns the first value");
    }

    @Test
    @DisplayName("a number stored as text, e.g. an id, can be read as number")
    void numbersStoredAsTextAreParsed() {
        //ids are text properties of the indexed objects, so they end up as text in the index
        assertEquals(42L, fields().getLong("alignedFeatureId"));
        assertEquals(42, fields().getInt("alignedFeatureId"));
    }

    @Test
    @DisplayName("text that is no number is reported instead of failing obscurely")
    void textThatIsNoNumberIsReported() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> fields().getLong("name"));

        assertTrue(e.getMessage().contains("name") && e.getMessage().contains("caffeine"),
                "the message must name field and value: " + e.getMessage());
    }

    @Test
    @DisplayName("a date can only be read from a date field")
    void datesCannotBeReadFromText() {
        assertThrows(IllegalArgumentException.class, () -> fields().getDate("name"));
    }

    @Test
    @DisplayName("types that the index cannot provide are rejected instead of returning something useless")
    void unsupportedTypesAreRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> fields().get("name", StringBuilder.class));

        assertTrue(e.getMessage().contains("name"), "the message must name the field: " + e.getMessage());
    }
}
