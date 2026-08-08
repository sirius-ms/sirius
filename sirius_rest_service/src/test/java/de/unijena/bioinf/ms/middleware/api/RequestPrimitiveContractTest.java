package de.unijena.bioinf.ms.middleware.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the rule behind our use of primitives in API models: a primitive field means the value is
 * mandatory and has no meaningful default, so the API must reject a request that omits it instead of
 * silently substituting 0/false. A property that documents a default must conversely be accepted when
 * omitted, and must then carry its documented default.
 * <p>
 * Whether that holds depends on how Jackson binds the DTO, which depends on its Lombok annotations
 * (constructor binding rejects an omitted primitive, field binding keeps the field initialiser).
 * That is deliberately <em>not</em> asserted structurally here - "has a no-arg constructor" is not a
 * reliable proxy, because Jackson 3 still prefers an all-args constructor when both exist. Instead
 * each property is probed against a real Jackson 3 mapper configured like production.
 */
public class RequestPrimitiveContractTest {

    private static final String MODEL_PACKAGE = "de.unijena.bioinf.ms.middleware.model";

    /** Jackson 3 defaults, i.e. FAIL_ON_NULL_FOR_PRIMITIVES enabled, as the REST layer runs it */
    private static final tools.jackson.databind.json.JsonMapper MAPPER =
            tools.jackson.databind.json.JsonMapper.builder().build();

    @Test
    @DisplayName("primitive properties of request models behave as their documentation promises")
    void primitivesBehaveAsDocumented() throws IOException {
        JsonNode spec = loadSpec("openapi-spec-enums-as-string.json");
        JsonNode schemas = spec.path("components").path("schemas");
        Map<String, Class<?>> modelsBySimpleName = scanModels();

        List<String> violations = new ArrayList<>();
        List<String> undetermined = new ArrayList<>();
        int checked = 0;

        for (String schemaName : requestReachableSchemas(spec)) {
            Class<?> type = modelsBySimpleName.get(schemaName);
            JsonNode schema = schemas.path(schemaName);
            if (type == null || schema.isMissingNode()) continue; // e.g. renamed via @Schema(name=..)

            Set<String> required = new HashSet<>();
            schema.path("required").forEach(n -> required.add(n.asText()));
            List<Field> primitives = primitiveFields(type);

            for (Field probed : primitives) {
                JsonNode property = schema.path("properties").path(probed.getName());
                if (property.isMissingNode()) continue;
                checked++;

                // a property that documents a default is never truly mandatory, whatever "required" says
                boolean mandatory = required.contains(probed.getName()) && !property.has("default");
                Boolean rejected = omittingIsRejected(type, primitives, probed);

                if (rejected == null) {
                    undetermined.add(schemaName + "." + probed.getName());
                } else if (mandatory && !rejected) {
                    violations.add(("%s.%s is documented as mandatory, but omitting it is accepted and silently "
                            + "yields the Java default. Make %s bind through a constructor (drop the no-arg "
                            + "constructor) or document a default for it.")
                            .formatted(schemaName, probed.getName(), type.getSimpleName()));
                } else if (!mandatory && rejected) {
                    violations.add(("%s.%s is documented as optional (default=%s), but omitting it is rejected. "
                            + "Make %s field bound (only @Getter/@Setter plus the implicit no-arg constructor, no "
                            + "@Builder/@AllArgsConstructor) so the documented default survives.")
                            .formatted(schemaName, probed.getName(), property.path("default").asText("<none>"),
                                    type.getSimpleName()));
                }
            }
        }

        assertTrue(checked > 0, "no primitive request properties were checked - is the spec up to date?");
        assertTrue(violations.isEmpty(), () -> "primitive contract violations:\n  " + String.join("\n  ", violations)
                + (undetermined.isEmpty() ? "" : "\n(undetermined, could not be probed: " + undetermined + ")"));
    }

    /**
     * @return true if omitting {@code probed} is rejected, false if it is accepted, null if the probe hit an
     * unrelated problem (e.g. the constructor itself refusing the minimal payload)
     */
    private static Boolean omittingIsRejected(Class<?> type, List<Field> primitives, Field probed) {
        // supply every other primitive so that the probed one is the only omitted primitive
        String json = primitives.stream()
                .filter(f -> !f.getName().equals(probed.getName()))
                .map(f -> "\"" + f.getName() + "\":" + dummyValue(f))
                .collect(Collectors.joining(",", "{", "}"));
        try {
            MAPPER.readValue(json, type);
            return false;
        } catch (tools.jackson.databind.exc.MismatchedInputException e) {
            String message = String.valueOf(e.getMessage());
            if (message.contains("\"" + probed.getName() + "\"")) return true;
            return null; // failed for a different property
        } catch (Throwable t) {
            return null;
        }
    }

    private static String dummyValue(Field f) {
        Class<?> t = f.getType();
        if (t == boolean.class) return "false";
        if (t == char.class) return "\"x\"";
        return "0";
    }

    /** transitive closure of schemas reachable from any requestBody */
    private static Set<String> requestReachableSchemas(JsonNode spec) {
        Set<String> seeds = new LinkedHashSet<>();
        spec.path("paths").forEach(pathItem -> pathItem.forEach(operation -> {
            if (operation.isObject() && operation.has("requestBody")) collectRefs(operation.get("requestBody"), seeds);
        }));

        JsonNode schemas = spec.path("components").path("schemas");
        Set<String> seen = new LinkedHashSet<>();
        Deque<String> stack = new ArrayDeque<>(seeds);
        while (!stack.isEmpty()) {
            String name = stack.pop();
            if (!seen.add(name)) continue;
            JsonNode schema = schemas.path(name);
            if (schema.isMissingNode()) continue;
            Set<String> next = new LinkedHashSet<>();
            collectRefs(schema, next);
            next.removeAll(seen);
            stack.addAll(next);
        }
        return seen;
    }

    private static void collectRefs(JsonNode node, Set<String> out) {
        if (node == null) return;
        if (node.isObject()) {
            JsonNode ref = node.get("$ref");
            if (ref != null && ref.isTextual() && ref.asText().startsWith("#/components/schemas/"))
                out.add(ref.asText().substring("#/components/schemas/".length()));
            node.forEach(child -> collectRefs(child, out));
        } else if (node.isArray()) {
            node.forEach(child -> collectRefs(child, out));
        }
    }

    private static List<Field> primitiveFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> k = type; k != null && k != Object.class; k = k.getSuperclass())
            for (Field f : k.getDeclaredFields())
                if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic() && f.getType().isPrimitive())
                    fields.add(f);
        return fields;
    }

    private static Map<String, Class<?>> scanModels() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
        Map<String, Class<?>> byName = new HashMap<>();
        for (BeanDefinition bd : scanner.findCandidateComponents(MODEL_PACKAGE)) {
            try {
                Class<?> c = Class.forName(bd.getBeanClassName());
                byName.putIfAbsent(c.getSimpleName(), c);
            } catch (Throwable ignored) {
            }
        }
        return byName;
    }

    private static JsonNode loadSpec(String fileName) throws IOException {
        Path path = Path.of(fileName);
        if (Files.notExists(path)) path = Path.of("sirius_rest_service").resolve(fileName);
        final Path resolved = path;
        assertTrue(Files.exists(resolved), () -> "OpenAPI spec not found: " + fileName
                + ". Regenerate it with ':sirius_rest_service:generateOpenApiDocsEnumsAsString'.");
        return new ObjectMapper().readTree(Files.readString(resolved));
    }
}
