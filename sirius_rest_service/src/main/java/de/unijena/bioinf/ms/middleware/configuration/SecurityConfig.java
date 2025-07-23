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

package de.unijena.bioinf.ms.middleware.configuration;

import com.brightgiant.secureapi.ExplorerHandshake;
import com.brightgiant.secureapi.Handshakes;
import com.brightgiant.secureapi.SiriusGuiHandshake;
import de.unijena.bioinf.ms.middleware.ErrorResponseHandler;
import de.unijena.bioinf.ms.middleware.security.ApiAllowedFilter;
import de.unijena.bioinf.webapi.WebAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

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
    SecurityFilterChain securityFilterChain(WebAPI<?> webAPI, SiriusGuiHandshake siriusGuiHandshake, ExplorerHandshake explorerHandshake, ErrorResponseHandler errorResponseHandler, HttpSecurity http) throws Exception {
        // disable CSRF
        http.csrf(AbstractHttpConfigurer::disable);

        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests((authorize) -> authorize.anyRequest().permitAll());

        http.addFilterAfter(new ApiAllowedFilter(webAPI, errorResponseHandler, siriusGuiHandshake.or(explorerHandshake),
                        List.of("/**"),
                        List.of("/sse", "/actuator/**", "/api/account/**",
                                "/api/info", "/api/connection-status",
                                "/", "/api", "/api/", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")),
                AuthorizationFilter.class);
        return http.build();
    }

    @Bean
    public SiriusGuiHandshake siriusGuiHandshake() {
        return Handshakes.newSiriusHandshakeInstance();
    }

    @Bean
    public ExplorerHandshake explorerHandshake() {
        return Handshakes.getExplorerHandshakeInstance();
    }
}
