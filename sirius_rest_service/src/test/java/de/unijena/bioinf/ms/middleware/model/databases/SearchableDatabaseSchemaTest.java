package de.unijena.bioinf.ms.middleware.model.databases;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The display name length limit applies to user created databases, so it belongs to the request
 * parameters only. Databases that ship with SIRIUS have longer display names, hence the response
 * model must not carry the limit: strictly validating clients (e.g. the pydantic based PySirius)
 * refuse to deserialize the database listing otherwise.
 */
class SearchableDatabaseSchemaTest {

    /** longest display name of a database that ships with SIRIUS */
    private static final String BUILT_IN_DISPLAY_NAME = "PubChem: bio and metabolites";

    @Test
    void requestSchemaDocumentsDisplayNameLimit() {
        Schema<?> displayName = displayNameSchemaOf(SearchableDatabaseParameters.class);

        assertEquals(Integer.valueOf(1), displayName.getMinLength());
        assertEquals(Integer.valueOf(15), displayName.getMaxLength());
    }

    @Test
    void responseSchemaDoesNotLimitDisplayName() {
        Schema<?> displayName = displayNameSchemaOf(SearchableDatabase.class);

        assertNull(displayName.getMinLength());
        assertNull(displayName.getMaxLength());
    }

    @Test
    void displayNameLimitIsEnforcedOnRequests() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertEquals(1, validator.validate(SearchableDatabaseParameters.builder()
                    .displayName(BUILT_IN_DISPLAY_NAME).build()).size());
            assertTrue(validator.validate(SearchableDatabaseParameters.builder()
                    .displayName("short enough").build()).isEmpty());
        }
    }

    @Test
    void builtInDisplayNamesStayValidOnResponses() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(SearchableDatabase.builder()
                    .databaseId("pubchem_bio")
                    .displayName(BUILT_IN_DISPLAY_NAME)
                    .build()).isEmpty());
        }
    }

    private static Schema<?> displayNameSchemaOf(Class<?> type) {
        Schema<?> schema = ModelConverters.getInstance().readAll(type).get(type.getSimpleName());
        assertNotNull(schema, () -> "no schema resolved for " + type.getSimpleName());

        Schema<?> displayName = (Schema<?>) schema.getProperties().get("displayName");
        assertNotNull(displayName, () -> "no displayName property in schema of " + type.getSimpleName());
        return displayName;
    }
}
