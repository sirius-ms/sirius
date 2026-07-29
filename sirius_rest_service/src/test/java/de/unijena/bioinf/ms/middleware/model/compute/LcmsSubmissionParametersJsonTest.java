package de.unijena.bioinf.ms.middleware.model.compute;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the JSON contract of the {@code parameters} part of the ms-data-files import endpoints,
 * which is what the generated SDKs send.
 * <p>
 * The web layer is served by Jackson 3 (Spring Boot 4), which does not see the Jackson 2
 * {@code @JsonSerialize}/{@code @JsonDeserialize} annotations on the mass deviation fields.
 * Mass deviations therefore have to be bindable by plain Jackson introspection.
 */
class LcmsSubmissionParametersJsonTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void deserializesMassDeviationsFromObject() {
        LcmsSubmissionParameters params = MAPPER.readValue("""
                {"alignLCMSRuns":false,"noiseIntensity":0.05,
                 "traceMaxMassDeviation":{"ppm":10.0,"absolute":0.005},
                 "alignMaxMassDeviation":{"ppm":5.0,"absolute":0.001}}
                """, LcmsSubmissionParameters.class);

        assertFalse(params.isAlignLCMSRuns());
        assertEquals(0.05, params.getNoiseIntensity());
        assertEquals(10.0, params.getTraceMaxMassDeviation().getPpm());
        assertEquals(0.005, params.getTraceMaxMassDeviation().getAbsolute());
        assertEquals(5.0, params.getAlignMaxMassDeviation().getPpm());
        assertEquals(0.001, params.getAlignMaxMassDeviation().getAbsolute());
    }

    @Test
    void deserializesNullMassDeviations() {
        LcmsSubmissionParameters params = MAPPER.readValue(
                "{\"alignLCMSRuns\":false,\"noiseIntensity\":0.05,"
                        + "\"traceMaxMassDeviation\":null,\"alignMaxMassDeviation\":null}",
                LcmsSubmissionParameters.class);

        assertFalse(params.isAlignLCMSRuns());
        assertNull(params.getTraceMaxMassDeviation());
        assertNull(params.getAlignMaxMassDeviation());
    }

    @Test
    void deserializesEmptyParameters() {
        LcmsSubmissionParameters params = MAPPER.readValue("{}", LcmsSubmissionParameters.class);

        assertTrue(params.isAlignLCMSRuns());
        assertEquals(-1, params.getNoiseIntensity());
        assertNull(params.getTraceMaxMassDeviation());
    }

    @Test
    void deserializesMassDeviationFromString() {
        LcmsSubmissionParameters params = MAPPER.readValue(
                "{\"traceMaxMassDeviation\":\"10.0 ppm (0.005 m/z)\"}", LcmsSubmissionParameters.class);

        assertEquals(10.0, params.getTraceMaxMassDeviation().getPpm());
        assertEquals(0.005, params.getTraceMaxMassDeviation().getAbsolute());
    }

    @Test
    void serializesMassDeviationAsObject() {
        LcmsSubmissionParameters params = new LcmsSubmissionParameters();
        params.setTraceMaxMassDeviation(new Deviation(10.0, 0.005));

        JsonNode deviation = MAPPER.readTree(MAPPER.writeValueAsString(params)).get("traceMaxMassDeviation");

        assertTrue(deviation.isObject(), "mass deviations are documented as objects in the OpenAPI schema");
        assertEquals(10.0, deviation.get("ppm").asDouble());
        assertEquals(0.005, deviation.get("absolute").asDouble());
    }
}
