/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.middleware.SiriusContext;
import de.unijena.bioinf.ms.middleware.model.events.BackgroundComputationsStateEvent;
import de.unijena.bioinf.ms.middleware.model.events.ProjectChangeEvent;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    final SiriusContext context;

    public SwaggerConfig(SiriusContext context) {
        this.context = context;
    }

    @Bean
    public OpenAPI api(OpenApiCustomizer openApiCustomiser) {
        OpenAPI oapi = new OpenAPI()
                .components(new Components())
                .info(apiInfo());
        openApiCustomiser.customise(oapi);
        return oapi;
    }

    private Info apiInfo() {
        return new Info()

                .title("SIRIUS Nightsky API")
                .description(
                        "REST API that provides the full functionality of SIRIUS and its web services as background service. " +
                        "It is intended as entry-point for scripting languages and software integration SDKs." +
                        "This API is exposed by SIRIUS " + ApplicationCore.VERSION_OBJ().getMajorVersion()
                )
//                .termsOfServiceUrl("https://bio.informatik.uni-jena.de/software/sirius/")
//                .license("GNU General Public License v3.0")
//                .licenseUrl("https://github.com/sirius-ms/sirius_frontend/blob/release-4.4/LICENSE.txt")
                .version(context.getApiVersion());

    }

    @Bean
    public OpenApiCustomizer openApiCustomiser() {
        ResolvedSchema projectChangeEvent = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(ProjectChangeEvent.class));
        ResolvedSchema backgroundComputationsStateEvent = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(BackgroundComputationsStateEvent.class));
        return openApi -> openApi
                .schema(projectChangeEvent.schema.getName(), projectChangeEvent.schema)
                .schema(backgroundComputationsStateEvent.schema.getName(), backgroundComputationsStateEvent.schema);
    }
}
