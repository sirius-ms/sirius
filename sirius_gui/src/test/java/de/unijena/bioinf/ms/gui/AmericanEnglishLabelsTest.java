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

package de.unijena.bioinf.ms.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The GUI is written in American English. This guards the spellings that have already slipped in once
 * (see #761): "homologue"/"analogue" are British.
 * <p>
 * Only string literals are checked, and only those that reach a user. Java identifiers keep whatever
 * spelling they have, and two kinds of literal are deliberately exempt because renaming them would
 * change behaviour rather than wording: {@code ParameterConfig} keys such as
 * {@code AnalogueSearchSettings.enabled}, and persisted property / property-change names such as
 * {@code showHomologueSeriesPanel}.
 */
public class AmericanEnglishLabelsTest {

    /** British spellings and what to write instead. */
    private static final String[][] BRITISH = {
            {"homologue", "homolog (or \"homologous series\")"},
            {"analogue", "analog"},
    };

    /** Literals that are keys or property names, not wording. */
    private static final Pattern EXEMPT = Pattern.compile(
            "AnalogueSearchSettings|ZodiacAnalogueNodes|showHomologueSeriesPanel|de\\.unijena\\.");

    private static final Pattern STRING_LITERAL = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"");

    @Test
    public void guiLabelsUseAmericanEnglish() throws IOException {
        Path sources = sourceRoot();
        List<String> offenders = new ArrayList<>();
        int scanned = 0;

        try (Stream<Path> files = Files.walk(sources)) {
            List<Path> javaFiles = files.filter(p -> p.toString().endsWith(".java")).toList();
            for (Path file : javaFiles) {
                scanned++;
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    Matcher literals = STRING_LITERAL.matcher(lines.get(i));
                    while (literals.find()) {
                        String literal = literals.group();
                        if (EXEMPT.matcher(literal).find())
                            continue;
                        for (String[] british : BRITISH) {
                            if (literal.toLowerCase().contains(british[0]))
                                offenders.add(sources.relativize(file) + ":" + (i + 1)
                                        + "  " + literal.trim() + "   -> use " + british[1]);
                        }
                    }
                }
            }
        }

        //a wrong working directory would otherwise make this test pass without looking at anything
        assertTrue(scanned > 100, "expected to scan the GUI sources, but only found " + scanned + " java files under " + sources.toAbsolutePath());
        assertTrue(offenders.isEmpty(), "British spellings in user facing strings:\n  " + String.join("\n  ", offenders));
    }

    private static Path sourceRoot() {
        for (Path candidate : List.of(Path.of("src/main/java"), Path.of("sirius_gui/src/main/java"))) {
            if (Files.isDirectory(candidate))
                return candidate;
        }
        throw new IllegalStateException("cannot locate the sirius_gui sources from " + Path.of("").toAbsolutePath());
    }
}
