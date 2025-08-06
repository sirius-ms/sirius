/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.security;

import com.brightgiant.secureapi.ExplorerHandshake;
import com.brightgiant.secureapi.Handshakes;
import com.brightgiant.secureapi.SiriusGuiHandshake;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import de.unijena.bioinf.ms.middleware.security.filters.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.text.ParseException;
import java.util.Collection;
import java.util.Map;

import static de.unijena.bioinf.ms.middleware.security.Authorities.*;

@EnableMethodSecurity
@Configuration
public class SecurityConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                //assets for React views
                registry.addResourceHandler("/assets/**")
                        .addResourceLocations("classpath:/templates/sirius_java_integrated/assets/");

                registry.addResourceHandler("/sirius_java_integrated/**")
                        .addResourceLocations("classpath:/templates/sirius_java_integrated/");
            }
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(JwtDecoder jwtDecoder,
                                            SiriusGuiHandshake siriusGuiHandshake, ExplorerHandshake explorerHandshake,
                                            HttpSecurity http) throws Exception
    {
        // disable CSRF
        http.csrf(AbstractHttpConfigurer::disable)
                 // This is the line that disables anonymous authentication
                .anonymous(AbstractHttpConfigurer::disable)
                // exclude endpoints from security filters and authorities
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/sse", "/actuator/**", "/api/account/**", "/api/info", "/api/connection-status",
                                "/", "/api", "/api/", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        // configure authorities to check for general api access
                        .anyRequest().hasAnyAuthority(
                                ALLOWED_FEATURE__API.getAuthority(),
                                BYPASS__EXPLORER.getAuthority(),
                                BYPASS__GUI.getAuthority()))
                //configure jwt converter
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder)
                                .jwtAuthenticationConverter(new SiriusJwtAuthenticationConverter())))
                // configure security filters, they usually just add authorities for authority-based permission checks
                .addFilterBefore(new ExceptionHandlerFilter(), BearerTokenAuthenticationFilter.class)
                .addFilterAfter(new AddFeatureAuthoritiesFilter(), BearerTokenAuthenticationFilter.class)
                .addFilterAfter(new AddBypassAuthoritiesFilter(
                        BypassRule.of(siriusGuiHandshake, BYPASS__GUI),
                        BypassRule.of(explorerHandshake, BYPASS__EXPLORER)), BearerTokenAuthenticationFilter.class);
        return http.build();
    }


    /**
     * This decoder is used for the 'local' profile. It parses the token
     * but DOES NOT validate the signature because it has already been validated by this application.
     * This is mainly to transform the existing token into the spring jwt object.
     */
    @Bean
    @Profile("local")
    public JwtDecoder jwtDecoderLocal() {
        return token -> {
            try {
                // Parse the token string into a JWT object from the nimbus library
                JWT parsedJwt = JWTParser.parse(token);

                Map<String, Object> claims = parsedJwt.getJWTClaimsSet().getClaims();
                Map<String, Object> headers = parsedJwt.getHeader().toJSONObject();

                // Create a Spring Security Jwt object from the parsed token
                return new Jwt(token,
                        parsedJwt.getJWTClaimsSet().getIssueTime().toInstant(),
                        parsedJwt.getJWTClaimsSet().getExpirationTime().toInstant(),
                        headers, claims);
            } catch (ParseException e) {
                throw new JwtException("Failed to parse local JWT. This is likely a bug. Contact support!", e);
            }
        };
    }

    @Bean
    @Profile("web")
    public JwtDecoder jwtDecoderWeb() {
        // todo implement to use as real web api.
        throw new UnsupportedOperationException("Not supported yet!");
    }

    @Bean
    public SiriusGuiHandshake siriusGuiHandshake() {
        return Handshakes.newSiriusHandshakeInstance();
    }

    @Bean
    public ExplorerHandshake explorerHandshake() {
        return Handshakes.getExplorerHandshakeInstance();
    }

    public static class SiriusJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {
        private final JwtGrantedAuthoritiesConverter source;

        private SiriusJwtAuthenticationConverter() {
            this.source = new JwtGrantedAuthoritiesConverter();
            source.setAuthoritiesClaimName(Authorities.AUTHORITIES_CLAIM_NAME);
            source.setAuthorityPrefix("");
        }

        @Override
        public JwtAuthenticationToken convert(@NotNull Jwt jwt) {
            Collection<GrantedAuthority> c = source.convert(jwt);
            c.addAll(Authorities.getAdditionalFromAccessToken(jwt));
            return new JwtAuthenticationToken(jwt, c);
        }
    }
}
