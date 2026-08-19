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

    /**
     * What the engine must not know about: the API model it is described in, and the package that describes it.
     * There is no exception - a mapper names the fields it writes, and what those values mean is said on the
     * other side of the boundary.
     */
    private static final List<String> FORBIDDEN = List.of(
            "de/unijena/bioinf/ms/middleware/model/search/SearchableField",
            "de/unijena/bioinf/ms/middleware/service/search/description/");

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
