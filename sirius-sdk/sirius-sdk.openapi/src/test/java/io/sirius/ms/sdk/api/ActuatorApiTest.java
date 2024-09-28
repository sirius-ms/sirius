package io.sirius.ms.sdk.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;

import static org.junit.jupiter.api.Assertions.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ActuatorApiTest {

    private ActuatorApi instance;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        TestSetup.getInstance().loginIfNeeded();
        instance = new ActuatorApi(TestSetup.getInstance().getSiriusClient().getApiClient());
        objectMapper = new ObjectMapper();
    }

    @Test
    public void instanceTest() {
        assertNotNull(instance);
    }

    @Test
    public void healthTest() throws JsonProcessingException {
        // Calling the health API and checking response status
        JsonNode response = objectMapper.readTree(instance.healthWithResponseSpec().bodyToMono(String.class).block());
        assertNotNull(response);
        assertEquals("UP", response.get("status").asText());
        assertTrue(response.isObject());
    }

   /* @Test
    public void shutdownTest() {
        // Shutdown test is manually disabled to prevent actual shutdown during testing
//        JsonNode response = objectMapper.readTree(instance.healthWithResponseSpec().bodyToMono(String.class).block());
//        assertEquals("UP", response.get("status").asText());
//
//        response = objectMapper.readTree(instance.shutdown());
//        assertNotNull(response);
//        assertTrue(response.isObject());
//        assertNotEquals("UP", response.get("status").asText());
    }*/
}
