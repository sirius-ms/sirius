package de.unijena.bioinf.ms.middleware.security;

import com.brightgiant.secureapi.ExplorerHandshake;
import com.brightgiant.secureapi.SiriusGuiHandshake;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static de.unijena.bioinf.ms.middleware.security.Authorities.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that the license {@link SecurityConfig.FeatureGate}s actually enforce access at the HTTP layer:
 * a gated endpoint (react view or REST API) is reachable only with its exact {@code allowedFeature:*}
 * authority, and NOT via the gui/explorer bypass (strict gating).
 * <p>
 * Runs the REAL {@link SecurityConfig} filter chain over stub endpoints mapped at the gated paths, inside a
 * self-contained context ({@link TestApp}) so it never boots the real {@code SiriusMiddlewareApplication}
 * (whose CLI wiring is hostile to slicing). The test therefore exercises the actual path matching +
 * authorization rules, independent of the controllers' business logic.
 */
@SpringBootTest(classes = EndpointSecurityTest.TestApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class EndpointSecurityTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(SecurityConfig.class)
    static class TestApp {
        // No JwtDecoder bean exists without the local/web profile; provide a stub (never invoked - the tests
        // inject the Authentication directly and send no bearer token).
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> { throw new UnsupportedOperationException("not used in this test"); };
        }

        @Bean
        StubController stubController() {
            return new StubController();
        }
    }

    // Replace the real handshakes so no external handshake instance is created; with no handshake header
    // their bypass predicate returns false, i.e. no bypass authority is granted (see BypassRule).
    @MockitoBean
    SiriusGuiHandshake siriusGuiHandshake;
    @MockitoBean
    ExplorerHandshake explorerHandshake;

    @Autowired
    MockMvc mvc;

    /** Stub endpoints mapped at the gated + control paths; security decides 200 vs 403, not the body. */
    @RestController
    static class StubController {
        @GetMapping("/api/reactions")
        String reactionApi() { return "ok"; }

        @GetMapping("/reactionTool")
        String reactionView() { return "ok"; }

        @PutMapping("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/denovo-structures/add-candidate")
        String addCandidate() { return "ok"; }

        @GetMapping("/structEdit")
        String structEditView() { return "ok"; }

        @GetMapping("/api/info")
        String permitAllControl() { return "ok"; }               // permitAll

        @GetMapping("/database")
        String nonGatedView() { return "ok"; }                    // covered by anyRequest (bypass/api)

        @GetMapping("/api/projects/{projectId}/aligned-features/page")
        String ungatedApi() { return "ok"; }                      // covered by anyRequest (bypass/api)
    }

    private static Authentication with(GrantedAuthority... authorities) {
        return new UsernamePasswordAuthenticationToken("tester", "n/a", List.of(authorities));
    }

    // ---------- transformationProducts gate: /api/reactions + /reactionTool ----------

    @Test
    void reactionApi_allowed_withFeature() throws Exception {
        mvc.perform(get("/api/reactions").with(authentication(with(ALLOWED_FEATURE__TRANSFORMATION_PRODUCTS))))
                .andExpect(status().isOk());
    }

    @Test
    void reactionApi_forbidden_withoutFeature() throws Exception {
        mvc.perform(get("/api/reactions").with(authentication(with(ALLOWED_FEATURE__API))))
                .andExpect(status().isForbidden());
    }

    @Test
    void reactionApi_bypassDoesNotUnlock() throws Exception {
        mvc.perform(get("/api/reactions").with(authentication(with(BYPASS__GUI, BYPASS__EXPLORER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void reactionView_allowed_withFeature() throws Exception {
        mvc.perform(get("/reactionTool").with(authentication(with(ALLOWED_FEATURE__TRANSFORMATION_PRODUCTS))))
                .andExpect(status().isOk());
    }

    @Test
    void reactionView_forbidden_withBypassOnly() throws Exception {
        mvc.perform(get("/reactionTool").with(authentication(with(BYPASS__GUI))))
                .andExpect(status().isForbidden());
    }

    // ---------- deNovo gate: add-candidate + /structEdit ----------

    @Test
    void addCandidate_allowed_withDeNovo() throws Exception {
        mvc.perform(put("/api/projects/pid/aligned-features/fid/denovo-structures/add-candidate")
                        .with(authentication(with(ALLOWED_FEATURE__DENOVO))))
                .andExpect(status().isOk());
    }

    @Test
    void addCandidate_forbidden_withoutDeNovo() throws Exception {
        mvc.perform(put("/api/projects/pid/aligned-features/fid/denovo-structures/add-candidate")
                        .with(authentication(with(BYPASS__GUI, ALLOWED_FEATURE__API))))
                .andExpect(status().isForbidden());
    }

    @Test
    void structEditView_allowed_withDeNovo() throws Exception {
        mvc.perform(get("/structEdit").with(authentication(with(ALLOWED_FEATURE__DENOVO))))
                .andExpect(status().isOk());
    }

    @Test
    void structEditView_forbidden_withBypassOnly() throws Exception {
        mvc.perform(get("/structEdit").with(authentication(with(BYPASS__GUI))))
                .andExpect(status().isForbidden());
    }

    // ---------- controls: the gates must not over-block the rest ----------

    @Test
    void permitAll_ok_withoutAuthentication() throws Exception {
        mvc.perform(get("/api/info")).andExpect(status().isOk());
    }

    @Test
    void nonGatedView_ok_withBypass() throws Exception {
        mvc.perform(get("/database").with(authentication(with(BYPASS__GUI))))
                .andExpect(status().isOk());
    }

    @Test
    void ungatedApi_ok_withApiFeature() throws Exception {
        mvc.perform(get("/api/projects/pid/aligned-features/page").with(authentication(with(ALLOWED_FEATURE__API))))
                .andExpect(status().isOk());
    }
}
