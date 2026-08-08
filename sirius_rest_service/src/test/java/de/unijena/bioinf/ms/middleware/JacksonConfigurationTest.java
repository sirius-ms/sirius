package de.unijena.bioinf.ms.middleware;

import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ms.middleware.model.annotations.CompoundClasses;
import de.unijena.bioinf.ms.middleware.model.databases.BioTransformerParameters;
import de.unijena.bioinf.ms.middleware.model.features.FeatureImport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the {@code spring.jackson.*} settings of the shipped application.properties bind to
 * the Jackson 3 mapper Spring Boot 4 serves the REST layer with, and that they restore the Jackson 2
 * behaviour the published API contract was built on.
 * <p>
 * Property names are not stable across Jackson versions: {@code READ/WRITE_ENUMS_USING_TO_STRING}
 * moved from {@code SerializationFeature}/{@code DeserializationFeature} to {@code EnumFeature},
 * so a wrong key fails the whole application context at startup. This test fails instead.
 */
class JacksonConfigurationTest {

    private ApplicationContextRunner runnerWithShippedProperties() throws IOException {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withPropertyValues(shippedJacksonProperties());
    }

    /** the spring.jackson.* lines of the real application.properties */
    private static String[] shippedJacksonProperties() throws IOException {
        Path candidate = Path.of("src", "main", "resources", "application.properties");
        if (Files.notExists(candidate))
            candidate = Path.of("sirius_rest_service", "src", "main", "resources", "application.properties");
        final Path path = candidate;
        assertTrue(Files.exists(path), () -> "application.properties not found at " + path.toAbsolutePath());

        List<String> props = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("spring.jackson.")) props.add(trimmed);
        }
        assertFalse(props.isEmpty(), "no spring.jackson.* settings found - the API contract relies on them");
        return props.toArray(String[]::new);
    }

    @Test
    void shippedJacksonPropertiesBind() throws IOException {
        runnerWithShippedProperties().run(context -> assertNull(context.getStartupFailure(),
                () -> "application.properties does not bind: " + context.getStartupFailure()));
    }

    @Test
    void omittedMandatoryPrimitiveIsRejected() throws IOException {
        // charge is documented as required and has no default: a feature imported with a silently
        // defaulted charge of 0 would be wrong data, so the request has to fail.
        runnerWithShippedProperties().run(context -> {
            tools.jackson.databind.ObjectMapper mapper = context.getBean(tools.jackson.databind.ObjectMapper.class);
            assertThrows(tools.jackson.databind.DatabindException.class,
                    () -> mapper.readValue("{\"ionMass\":1.23}", FeatureImport.class),
                    "an omitted mandatory primitive must be rejected, not defaulted");
        });
    }

    @Test
    void omittedOptionalPrimitiveKeepsItsDocumentedDefault() throws IOException {
        // useDB is documented as optional with default true, so omitting it must be accepted and
        // must yield the documented value (field binding via the no-arg constructor).
        runnerWithShippedProperties().run(context -> {
            tools.jackson.databind.ObjectMapper mapper = context.getBean(tools.jackson.databind.ObjectMapper.class);
            BioTransformerParameters params = mapper.readValue("{}", BioTransformerParameters.class);
            assertTrue(params.isUseDB(), "omitting an optional primitive must keep its documented default");
        });
    }

    @Test
    void enumsStayNameBasedOnTheWire() throws IOException {
        runnerWithShippedProperties().run(context -> {
            tools.jackson.databind.ObjectMapper mapper = context.getBean(tools.jackson.databind.ObjectMapper.class);
            assertEquals("\"QTOF\"", mapper.writeValueAsString(MsInstrumentation.Instrument.QTOF),
                    "enums must be written by name(), not toString()");
            assertEquals(MsInstrumentation.Instrument.QTOF,
                    mapper.readValue("\"QTOF\"", MsInstrumentation.Instrument.class));
        });
    }

    @Test
    void propertyOrderFollowsDeclarationOrder() throws IOException {
        runnerWithShippedProperties().run(context -> {
            tools.jackson.databind.ObjectMapper mapper = context.getBean(tools.jackson.databind.ObjectMapper.class);
            String json = mapper.writeValueAsString(new CompoundClasses());
            assertTrue(json.indexOf("npcPathway") < json.indexOf("classyFireLineage"),
                    () -> "properties must keep declaration order, which is what the OpenAPI spec documents: " + json);
        });
    }
}
