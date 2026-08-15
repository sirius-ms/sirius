package de.unijena.bioinf.ms.middleware.security;

import de.unijena.bioinf.ms.rest.model.license.AllowedFeatures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the license-feature -&gt; Spring authority wiring that the feature gates in {@link SecurityConfig}
 * rely on. Pure unit test (no Spring context): a granted feature must yield the matching
 * {@code allowedFeature:*} authority, an ungranted one must not, and the named constants the gates
 * reference must be initialized (a mistyped lookup would leave them {@code null} and break the gate).
 */
class AuthoritiesTest {

    // record order: cli, api, deNovo, importMSRuns, importPeakLists, importCef, transformationProducts
    private static AllowedFeatures onlyTransformationProducts() {
        return new AllowedFeatures(false, false, false, false, false, false, true);
    }

    private static AllowedFeatures onlyDeNovo() {
        return new AllowedFeatures(false, false, true, false, false, false, false);
    }

    private static AllowedFeatures allButTransformationProducts() {
        return new AllowedFeatures(true, true, true, true, true, true, false);
    }

    @Test
    void gateConstantsAreInitialized() {
        assertNotNull(Authorities.ALLOWED_FEATURE__TRANSFORMATION_PRODUCTS,
                "SecurityConfig FEATURE_GATES references this constant");
        assertNotNull(Authorities.ALLOWED_FEATURE__DENOVO,
                "SecurityConfig FEATURE_GATES references this constant");
    }

    @Test
    void transformationProductsFeatureYieldsItsAuthority() {
        assertTrue(Authorities.getFromAllowedFeatures(onlyTransformationProducts())
                        .contains(Authorities.ALLOWED_FEATURE__TRANSFORMATION_PRODUCTS),
                "transformationProducts=true must grant allowedFeature:transformationProducts");
    }

    @Test
    void transformationProductsAuthorityAbsentWhenFeatureOff() {
        assertFalse(Authorities.getFromAllowedFeatures(allButTransformationProducts())
                        .contains(Authorities.ALLOWED_FEATURE__TRANSFORMATION_PRODUCTS),
                "transformationProducts=false must not grant the authority (strict gate would then 403)");
    }

    @Test
    void deNovoFeatureYieldsItsAuthority() {
        assertTrue(Authorities.getFromAllowedFeatures(onlyDeNovo())
                        .contains(Authorities.ALLOWED_FEATURE__DENOVO),
                "deNovo=true must grant allowedFeature:deNovo (gates the structure sketcher + add-candidate)");
    }
}
