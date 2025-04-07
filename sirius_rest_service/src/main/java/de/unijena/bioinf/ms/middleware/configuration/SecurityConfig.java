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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class SecurityConfig {
    @Value("${app.cors.origins:#{null}}")
    private String corsAllowedOrigins;

    @Value("${app.cors.methods:#{null}}")
    private String corsAllowedMethods;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                if (corsAllowedOrigins != null)
                    registry.addMapping("/api/**").allowedOrigins(corsAllowedOrigins).allowedMethods(corsAllowedMethods.split(","));
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                //todo generic solution to easily add more apps




                // React views
                registry.addResourceHandler("/KMD/**", "/formulaTreeView/**", "epi/**")
                        .addResourceLocations("classpath:/static/sirius_java_integrated/")
                        .resourceChain(true)
                        .addResolver(new PathResourceResolver() {
                            @Override
                            protected Resource getResource(String resourcePath, Resource location) throws IOException {
                                Resource resource = location.createRelative(resourcePath);
                                return resource.exists() && resource.isReadable() ? resource :
                                        location.createRelative("index.html");
                            }
                        });

                //assets for react views
                registry.addResourceHandler("/assets/**")
                        .addResourceLocations("classpath:/static/sirius_java_integrated/assets/");

                // just some basic example for testing.
                registry.addResourceHandler("/apps/hello-world/")
                        .addResourceLocations("classpath:/static/hello-world/")
                        .resourceChain(true)
                        .addResolver(new PathResourceResolver() {
                            @Override
                            protected Resource getResource(String resourcePath, Resource location) throws IOException {
                                Resource resource = location.createRelative(resourcePath);
                                return resource.exists() && resource.isReadable() ? resource :
                                        location.createRelative("index.html");
                            }
                        });
            }
        };
    }
}
