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
import de.unijena.bioinf.ms.middleware.model.events.DataImportEvent;
import de.unijena.bioinf.ms.middleware.model.events.ProjectChangeEvent;
import de.unijena.bioinf.ms.middleware.model.events.ProjectEventType;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        final ResolvedSchema projectChangeEventType;
        if (io.swagger.v3.core.jackson.ModelResolver.enumsAsRef) {
            projectChangeEventType = ModelConverters.getInstance()
                    .resolveAsResolvedSchema(new AnnotatedType(ProjectEventType.class));
            projectChangeEventType.schema.setName("ProjectEventType");
            projectChangeEventType.schema.setType("string");
            projectChangeEventType.schema.setEnum(List.of(ProjectEventType.values()));
            projectChangeEventType.schema.set$ref(null);
        }else projectChangeEventType = null;



        ResolvedSchema projectChangeEvent = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(ProjectChangeEvent.class));
        ResolvedSchema importEvent = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(DataImportEvent.class));
        ResolvedSchema backgroundComputationsStateEvent = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(BackgroundComputationsStateEvent.class));

        return openApi -> {
            openApi.schema(projectChangeEvent.schema.getName(), projectChangeEvent.schema)
                    .schema(importEvent.schema.getName(), importEvent.schema)
                    .schema(backgroundComputationsStateEvent.schema.getName(), backgroundComputationsStateEvent.schema);
            if (projectChangeEventType != null)
                openApi.schema(projectChangeEventType.schema.getName(), projectChangeEventType.schema);

            hideIndexOnlyOptFields(openApi);
        };
    }

    /**
     * Drops the opt fields that exist for the search index only from the published enums.
     * <p>
     * {@link AlignedFeature.OptField#statistics} makes an index document carry a feature's fold changes, which
     * is what the gui's blank filter queries. It was never retrievable: the field it fills is
     * {@code @Schema(hidden = true)}, so a client asking for it would pay for loading the largest part of a
     * feature - one entry per group pair, quantification and aggregation - and get back a property no generated
     * model has. Fold changes are read from the /statistics endpoints instead. Removing the value keeps it out
     * of the spec and therefore out of every generated client, while the java enum stays available to
     * {@link AlignedFeature#INDEXED_OPT_FIELDS} and to the projection that decides what a response carries.
     * <p>
     * Runs on the built document. This customizer also seeds the base {@link OpenAPI} bean, which has no
     * components and no paths yet - hence a no-op there rather than a guard at the call site.
     */
    private static void hideIndexOnlyOptFields(OpenAPI openApi) {
        hideEnumValue(openApi, AlignedFeature.OptField.class, AlignedFeature.OptField.statistics.name());
    }

    /**
     * Removes one constant from every rendering of an enum in the document.
     * <p>
     * Both renderings have to be handled: with {@code --enums-as-ref} the enum is a named component every
     * parameter refers to, without it the same values are inlined into each parameter that takes them. Matching
     * on the complete set of constants rather than on a schema name is what covers the inlined case, where
     * there is no name to match on.
     */
    private static void hideEnumValue(OpenAPI openApi, Class<? extends Enum<?>> enumClass, String valueToHide) {
        Set<String> allConstants = Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name).collect(Collectors.toSet());

        // Identity, not equality: Schema.equals compares content, so two parameters inlining the same enum are
        // equal while being distinct objects. A hash set would keep one and drop the rest, and mutating the one
        // it kept would leave the others in the document untouched.
        Collection<Schema> schemas = Collections.newSetFromMap(new IdentityHashMap<>());
        if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null)
            openApi.getComponents().getSchemas().values().forEach(s -> collectSchemas(s, schemas));
        if (openApi.getPaths() != null)
            openApi.getPaths().values().stream()
                    .flatMap(item -> item.readOperations().stream())
                    .forEach(op -> {
                        if (op.getParameters() != null)
                            op.getParameters().stream().map(Parameter::getSchema).forEach(s -> collectSchemas(s, schemas));
                        if (op.getRequestBody() != null && op.getRequestBody().getContent() != null)
                            op.getRequestBody().getContent().values()
                                    .forEach(mt -> collectSchemas(mt.getSchema(), schemas));
                    });

        for (Schema<?> schema : schemas) {
            List<?> values = schema.getEnum();
            // Only an enum that still offers every constant is this enum; a subset is somebody else's.
            if (values == null || !values.stream().map(String::valueOf).collect(Collectors.toSet()).containsAll(allConstants))
                continue;
            ((Schema<Object>) schema).setEnum(values.stream()
                    .filter(v -> !valueToHide.equals(String.valueOf(v)))
                    .collect(Collectors.toList()));
        }
    }

    /** Collects a schema and the nested ones that can carry an inlined enum: array items and properties. */
    private static void collectSchemas(Schema<?> schema, Collection<Schema> collected) {
        if (schema == null || !collected.add(schema))
            return;
        collectSchemas(schema.getItems(), collected);
        if (schema.getProperties() != null)
            schema.getProperties().values().forEach(s -> collectSchemas(s, collected));
    }
}
