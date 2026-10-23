/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2026 Bright Giant GmbH
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

import org.junit.jupiter.api.Test;
import org.webjars.WebJarVersionLocator;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The swagger-ui webjar is pinned to a newer version than springdoc bundles. springdoc serves the Swagger UI from
 * the webjar path of the version it is configured with, so a version that does not match the webjar on the classpath
 * makes /swagger-ui.html answer 404.
 */
class SwaggerUiVersionTest {

    @Test
    void configuredSwaggerUiVersionIsTheShippedWebjar() throws IOException {
        Path candidate = Path.of("src", "main", "resources", "application.properties");
        if (Files.notExists(candidate))
            candidate = Path.of("sirius_rest_service", "src", "main", "resources", "application.properties");
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(candidate)) {
            props.load(r);
        }

        String shipped = new WebJarVersionLocator().version("swagger-ui");
        assertNotNull(shipped, "no swagger-ui webjar on the classpath");
        assertEquals(shipped, props.getProperty("springdoc.swagger-ui.version"),
                "springdoc.swagger-ui.version in application.properties must name the swagger-ui webjar that is shipped");
    }
}
