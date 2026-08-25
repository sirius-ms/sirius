package de.unijena.bioinf.ms.middleware.configuration;

import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The index-only opt field must not reach a generated client.
 * <p>
 * {@code AlignedFeature.OptField.statistics} exists so an index document carries a feature's fold changes for
 * the blank filter to query; the field it fills is hidden from the schema, so asking for it over the api buys
 * an expensive load and an undocumented payload. It is stripped from the published spec while staying usable
 * in java - and it has to be stripped from both renderings, which is what these cover.
 */
class SwaggerConfigOptFieldTest {

    private static final String HIDDEN = AlignedFeature.OptField.statistics.name();

    private static List<String> allOptFields() {
        return Arrays.stream(AlignedFeature.OptField.values()).map(Enum::name).toList();
    }

    private static void customise(OpenAPI openApi) {
        new SwaggerConfig(null).openApiCustomiser().customise(openApi);
    }

    @Test
    @DisplayName("the named component enum loses it (--enums-as-ref)")
    void namedComponentEnumIsStripped() {
        Schema<String> optField = new StringSchema();
        optField.setEnum(allOptFields());
        OpenAPI openApi = new OpenAPI().components(new Components().addSchemas("AlignedFeatureOptField", optField));

        customise(openApi);

        List<?> values = openApi.getComponents().getSchemas().get("AlignedFeatureOptField").getEnum();
        assertFalse(values.contains(HIDDEN), "no client may ask for the index-only field");
        assertTrue(values.contains(AlignedFeature.OptField.tags.name()), "the retrievable ones must stay");
    }

    @Test
    @DisplayName("an enum inlined into a parameter loses it too (enums as string)")
    void inlinedParameterEnumIsStripped() {
        Schema<String> items = new StringSchema();
        items.setEnum(allOptFields());
        Parameter optFields = new Parameter().name("optFields").schema(new ArraySchema().items(items));
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/projects/{projectId}/aligned-features",
                new PathItem().get(new Operation().addParametersItem(optFields))));

        customise(openApi);

        assertFalse(items.getEnum().contains(HIDDEN), "the string flavour of the spec is published too");
    }

    @Test
    @DisplayName("every inlined copy is stripped, not just the first")
    void allInlinedCopiesAreStripped() {
        // Schema.equals is value-based, so two parameters that inline the same enum are equal but not the same
        // object. Collecting them into a hash set drops the second, and mutating the first does not reach it.
        Schema<String> onePage = new StringSchema();
        onePage.setEnum(allOptFields());
        Schema<String> aCompound = new StringSchema();
        aCompound.setEnum(allOptFields());

        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/api/projects/{projectId}/aligned-features/page", new PathItem().get(new Operation()
                        .addParametersItem(new Parameter().name("optFields").schema(new ArraySchema().items(onePage)))))
                .addPathItem("/api/projects/{projectId}/compounds/{compoundId}", new PathItem().get(new Operation()
                        .addParametersItem(new Parameter().name("optFieldsFeatures").schema(new ArraySchema().items(aCompound))))));

        customise(openApi);

        assertFalse(onePage.getEnum().contains(HIDDEN), "first copy");
        assertFalse(aCompound.getEnum().contains(HIDDEN), "second copy - equal to the first, but a different object");
    }

    @Test
    @DisplayName("an unrelated enum that happens to share values is left alone")
    void unrelatedEnumIsUntouched() {
        // Matching is on the complete set of constants, so a subset - anybody else's enum - must not be touched.
        Schema<String> other = new StringSchema();
        other.setEnum(List.of(AlignedFeature.OptField.none.name(), HIDDEN));
        OpenAPI openApi = new OpenAPI().components(new Components().addSchemas("SomethingElse", other));

        customise(openApi);

        assertTrue(openApi.getComponents().getSchemas().get("SomethingElse").getEnum().contains(HIDDEN),
                "only the opt-field enum is ours to edit");
    }
}
