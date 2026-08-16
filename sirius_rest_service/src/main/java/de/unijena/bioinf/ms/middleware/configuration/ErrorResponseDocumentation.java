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

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.ProblemDetail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Documents the error responses of the API.
 * <p>
 * The service answers failures with RFC 9457 problem details ({@code spring.mvc.problemdetails.enabled}), but
 * springdoc only derives the success response from a controller method signature - it cannot know which errors an
 * endpoint raises. Without this, clients get no typed error model at all.
 * <p>
 * Wording that is specific to an endpoint lives on the endpoint, as {@link ApiError} / {@link NoApiError}; this
 * class only supplies the generic rules and does the assembling. That split matters: nothing here is keyed by
 * method name, operation id or path, so renaming an endpoint or moving it to another controller carries its
 * documentation along with no chance of it silently going stale.
 * <p>
 * The errors are added as an {@link OperationCustomizer}, that is, after springdoc has built the operation from
 * the handler signature, rather than through swagger's {@code @ApiResponse}. That is deliberate: declaring any
 * {@code @ApiResponse} on a handler method - even one carrying nothing but a description - makes springdoc drop
 * the response set it derived from the return type, which then has to be restated by hand. For generic return
 * types such as {@code Page<DatabaseStructure>} it cannot be restated faithfully at all: naming the raw type
 * erases the element type, and referencing the generated schema by name stops springdoc emitting it, leaving a
 * dangling reference. Adding the errors afterwards leaves every derived success response untouched.
 */
@Configuration
public class ErrorResponseDocumentation {

    private static final String PROBLEM_JSON = org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA = "ProblemDetail";

    /**
     * Errors that any endpoint may return, and therefore are documented on all of them.
     */
    private static final Map<Integer, String> COMMON = Map.of(
            500, "Unexpected server-side error. The problem detail carries the reason.");

    /**
     * Errors that follow from the shape of the request rather than from the individual endpoint. Coarse on
     * purpose; an endpoint that does not fit opts out with {@link NoApiError}.
     */
    private static final int PATH_PARAM_404 = 404;
    private static final String PATH_PARAM_404_TEXT =
            "The referenced object does not exist in this SIRIUS instance or project.";
    private static final int INPUT_400 = 400;
    private static final String INPUT_400_TEXT =
            "The request body or a parameter is malformed or violates a constraint.";

    /**
     * Registers the ProblemDetail schema once, so the error responses can point at it, and catches the
     * operations that {@link #errorResponses()} never sees.
     * <p>
     * Not every operation in the document comes from a controller handler method: the actuator endpoints are
     * contributed by springdoc itself and have no {@code HandlerMethod}, so an {@link OperationCustomizer} is
     * never invoked for them. They still deserve the generic errors, so they are filled in here, on the finished
     * document. Anything an endpoint declared for itself is already in place by now and is left alone.
     */
    @Bean
    public OpenApiCustomizer problemDetailSchema() {
        return openApi -> {
            if (openApi.getComponents().getSchemas() == null
                    || !openApi.getComponents().getSchemas().containsKey(PROBLEM_SCHEMA)) {
                ModelConverters.getInstance()
                        .readAll(ProblemDetail.class)
                        .forEach(openApi.getComponents()::addSchemas);
            }

            if (openApi.getPaths() == null)
                return;
            openApi.getPaths().values().forEach(item -> item.readOperations().forEach(op ->
                    COMMON.forEach((status, text) -> add(op, status, text, Set.of(), returnsBody(op)))));
        };
    }

    @Bean
    public OperationCustomizer errorResponses() {
        return (operation, handlerMethod) -> {
            Map<Integer, String> specific = Arrays.stream(
                            AnnotatedElementUtils.getMergedRepeatableAnnotations(
                                    handlerMethod.getMethod(), ApiError.class).toArray(ApiError[]::new))
                    .collect(Collectors.toMap(ApiError::status, ApiError::value, (a, b) -> a, HashMap::new));

            NoApiError optOut = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), NoApiError.class);
            Set<Integer> notReturned = optOut == null
                    ? Set.of()
                    : Arrays.stream(optOut.value()).boxed().collect(Collectors.toCollection(HashSet::new));

            boolean typed = returnsBody(operation);

            COMMON.forEach((status, text) -> add(operation, status, specific.getOrDefault(status, text), notReturned, typed));

            if (hasPathParameter(operation))
                add(operation, PATH_PARAM_404, specific.getOrDefault(PATH_PARAM_404, PATH_PARAM_404_TEXT), notReturned, typed);
            if (takesInput(operation))
                add(operation, INPUT_400, specific.getOrDefault(INPUT_400, INPUT_400_TEXT), notReturned, typed);

            // statuses that only apply where the endpoint declares them
            specific.forEach((status, text) -> add(operation, status, text, notReturned, typed));

            return operation;
        };
    }

    private static boolean hasPathParameter(Operation op) {
        return op.getParameters() != null
                && op.getParameters().stream().anyMatch(p -> "path".equals(p.getIn()));
    }

    private static boolean takesInput(Operation op) {
        return op.getRequestBody() != null || (op.getParameters() != null && !op.getParameters().isEmpty());
    }

    /**
     * True if any success response of this operation carries a body.
     * <p>
     * Operations that return nothing on success must not get a typed error body: the client generator builds the
     * Accept header from ALL declared response content types, so attaching {@code application/problem+json} to an
     * otherwise body-less operation makes the generated client send {@code Accept: application/problem+json} for
     * a request whose successful answer has no body at all, and the server correctly refuses it with 406.
     */
    private static boolean returnsBody(Operation op) {
        if (op.getResponses() == null)
            return false;
        return op.getResponses().entrySet().stream()
                .filter(e -> e.getKey().startsWith("2"))
                .anyMatch(e -> e.getValue().getContent() != null && !e.getValue().getContent().isEmpty());
    }

    /**
     * Adds an error response, never overwriting one that is already there. The ProblemDetail body is only
     * declared where it cannot affect content negotiation, see {@link #returnsBody(Operation)}; body-less
     * operations still document that the error can occur, just without a schema.
     */
    private static void add(Operation op, int status, String description, Set<Integer> notReturned, boolean typed) {
        if (notReturned.contains(status))
            return;
        String code = String.valueOf(status);
        if (op.getResponses() == null)
            op.setResponses(new ApiResponses());
        if (op.getResponses().containsKey(code))
            return;
        ApiResponse response = new ApiResponse().description(description);
        if (typed)
            response.content(new Content().addMediaType(PROBLEM_JSON,
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_SCHEMA))));
        op.getResponses().addApiResponse(code, response);
    }
}
