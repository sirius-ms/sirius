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

package de.unijena.bioinf.ms.middleware.service.search;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import de.unijena.bioinf.ms.middleware.service.search.mappers.GenericPojoMapper;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps the search engine from learning about the API again.
 * <p>
 * The engine indexes and queries; describing what it holds is a separate concern that reads it. Nothing in the
 * engine may name the REST model that description ends up in - the moment it does, javadoc providers and
 * vocabularies start being injected into the index again, which is exactly the tangle this separation undid.
 * <p>
 * Checked against the compiled classes rather than the source, so it also catches a reference that arrives
 * through a constant, a signature or an annotation.
 */
public class SearchEngineBoundaryTest {

    /** Packages that make up the search engine itself. */
    private static final List<String> ENGINE_PACKAGES = List.of(
            "de/unijena/bioinf/ms/middleware/service/search/mappers",
            "de/unijena/bioinf/ms/middleware/service/search/dynamic");

    /** What the engine must not know about. */
    private static final List<String> FORBIDDEN = List.of(
            "de/unijena/bioinf/ms/middleware/model/search/SearchableField",
            "de/unijena/bioinf/ms/middleware/service/search/description/SearchableFieldDescriber",
            "de/unijena/bioinf/ms/middleware/service/search/description/SearchableFieldService",
            "de/unijena/bioinf/ms/middleware/service/search/description/ApiDocFieldDescriptions");

    /**
     * A mapper may opt into declaring the vocabulary of the fields it writes, which is the one description
     * concept the engine is allowed to name. Everything else in the description package stays out.
     */
    private static final List<String> ALLOWED_TO_NAME_A_VOCABULARY = List.of("CompoundClassesMapper.class");

    @Test
    public void testTheSearchEngineDoesNotKnowHowItIsDescribed() throws IOException {
        List<String> offenders = new ArrayList<>();
        int checked = 0;

        for (String enginePackage : ENGINE_PACKAGES) {
            for (Path classFile : classFilesOf(enginePackage)) {
                checked++;
                String constants = new String(Files.readAllBytes(classFile), StandardCharsets.ISO_8859_1);
                for (String forbidden : FORBIDDEN)
                    if (constants.contains(forbidden))
                        offenders.add(classFile.getFileName() + " references " + forbidden);
            }
        }

        assertTrue(checked > 10, "expected to check the whole engine, only found " + checked + " classes");
        assertTrue(offenders.isEmpty(),
                "the search engine must not know about the API it is described in:\n  " + String.join("\n  ", offenders));
    }

    /**
     * The one allowed direction, stated positively so that removing it is a deliberate act rather than an
     * accident: a mapper says which values it writes, and nothing else about how it is presented.
     */
    @Test
    public void testAMapperMayStillDeclareItsVocabulary() throws IOException {
        List<String> declaring = new ArrayList<>();
        for (Path classFile : classFilesOf(ENGINE_PACKAGES.get(0))) {
            String constants = new String(Files.readAllBytes(classFile), StandardCharsets.ISO_8859_1);
            if (constants.contains("de/unijena/bioinf/ms/middleware/service/search/description/FieldVocabulary"))
                declaring.add(classFile.getFileName().toString());
        }

        assertTrue(ALLOWED_TO_NAME_A_VOCABULARY.containsAll(declaring),
                "unexpected mapper declaring a vocabulary: " + declaring);
    }

    /**
     * The compiled production classes of a package. Found through the code source of a class that is certainly
     * in the engine, because asking the class loader for the package would just as happily hand back the test
     * classes that share the package name.
     */
    private static List<Path> classFilesOf(String packagePath) throws IOException {
        Path packageDir = mainClassesRoot().resolve(packagePath);
        assertTrue(Files.isDirectory(packageDir), "cannot locate compiled classes of " + packagePath);
        try (Stream<Path> files = Files.list(packageDir)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".class")).toList();
        }
    }

    private static Path mainClassesRoot() {
        try {
            URL location = GenericPojoMapper.class.getProtectionDomain().getCodeSource().getLocation();
            assertNotNull(location, "cannot locate the compiled search engine");
            return Path.of(location.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
