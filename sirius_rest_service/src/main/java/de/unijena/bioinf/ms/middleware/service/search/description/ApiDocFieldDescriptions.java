/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.search.description;

import com.github.therapi.runtimejavadoc.Comment;
import com.github.therapi.runtimejavadoc.CommentElement;
import com.github.therapi.runtimejavadoc.CommentText;
import com.github.therapi.runtimejavadoc.FieldJavadoc;
import com.github.therapi.runtimejavadoc.InlineLink;
import com.github.therapi.runtimejavadoc.InlineTag;
import com.github.therapi.runtimejavadoc.InlineValue;
import com.github.therapi.runtimejavadoc.Link;
import com.github.therapi.runtimejavadoc.RuntimeJavadoc;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provides human-readable searchable-field descriptions with the same content and precedence as the OpenAPI
 * documentation: an explicit {@link Schema} description wins, otherwise the field javadoc is used (baked into
 * the class files by the therapi-runtime-javadoc annotation processor, the same source springdoc reads).
 * <p>
 * Descriptions are normalized to plain text suitable for direct display (e.g. tooltips): source line
 * wrapping and markup are removed, only deliberate paragraph breaks ({@code <p>}, {@code <br>}, blank
 * javadoc lines) are kept as newlines.
 * <p>
 * This is REST-layer glue by design: the lucene machinery (mappers, index managers, search contexts) stays
 * framework-neutral and receives this provider via constructor injection instead of reading documentation
 * sources itself. Keep this class in the REST module when the search machinery is extracted into its own
 * module.
 */
public final class ApiDocFieldDescriptions {

    private ApiDocFieldDescriptions() {
    }

    /**
     * Reads the description of an indexed field as shown in the OpenAPI documentation, null if there is none.
     */
    public static final Function<Field, @Nullable String> PROVIDER = ApiDocFieldDescriptions::describe;

    @Nullable
    private static String describe(@NotNull Field field) {
        // an explicit schema annotation wins - same precedence as springdoc
        Schema schema = field.getAnnotation(Schema.class);
        if (schema != null && !schema.description().isEmpty())
            return emptyToNull(normalize(schema.description()));

        FieldJavadoc javadoc = RuntimeJavadoc.getJavadoc(field);
        if (javadoc.isEmpty())
            return null;
        return emptyToNull(normalize(toPlainText(javadoc.getComment())));
    }

    /**
     * Flattens a javadoc comment to its text: inline tags like {@code ...} and {@literal ...} are replaced
     * by their content, links by their label or referenced name.
     */
    private static String toPlainText(@NotNull Comment comment) {
        StringBuilder text = new StringBuilder();
        for (CommentElement element : comment) {
            if (element instanceof CommentText commentText) {
                text.append(commentText.getValue());
            } else if (element instanceof InlineTag inlineTag) { // {@code ...}, {@literal ...}
                if (inlineTag.getValue() != null)
                    text.append(inlineTag.getValue());
            } else if (element instanceof InlineLink inlineLink) { // {@link Foo#bar label}
                text.append(linkText(inlineLink.getLink()));
            } else if (element instanceof InlineValue inlineValue) { // {@value Foo#BAR}
                if (inlineValue.getValue().getReferencedMemberName() != null)
                    text.append(inlineValue.getValue().getReferencedMemberName());
            }
        }
        return text.toString();
    }

    private static String linkText(@NotNull Link link) {
        if (link.getLabel() != null && !link.getLabel().isBlank())
            return link.getLabel();
        if (link.getReferencedMemberName() != null && !link.getReferencedMemberName().isBlank())
            return link.getReferencedMemberName();
        String className = link.getReferencedClassName();
        if (className == null)
            return "";
        return className.substring(className.lastIndexOf('.') + 1);
    }

    /**
     * Normalizes documentation text to plain text with two rules for breaks: deliberate ones
     * ({@code <p>}, {@code <br>}, blank lines) become a single newline, while newlines from source
     * line wrapping collapse to a space (the same semantics javadoc rendering applies).
     */
    private static String normalize(@NotNull String raw) {
        // deliberate breaks: paragraph/line-break tags become paragraph separators
        String text = raw.replaceAll("(?i)<\\s*/?\\s*(p|br)\\s*/?\\s*>", "\n\n");
        // strip any remaining markup tags
        text = text.replaceAll("<[^>]+>", "");
        // resolve common HTML entities ("&amp;" last to avoid double-unescaping)
        text = text.replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")
                .replace("&nbsp;", " ").replace("&amp;", "&");
        // blank lines are deliberate paragraph breaks and become a single newline,
        // all other whitespace runs (incl. source line wrapping) collapse to a single space
        return Arrays.stream(text.split("\\n\\s*\\n"))
                .map(paragraph -> paragraph.replaceAll("\\s+", " ").trim())
                .filter(paragraph -> !paragraph.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    @Nullable
    private static String emptyToNull(@NotNull String text) {
        return text.isEmpty() ? null : text;
    }
}
