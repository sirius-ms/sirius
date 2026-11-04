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

package de.unijena.bioinf.ms.middleware;

import de.unijena.bioinf.ms.frontend.subtools.middleware.MiddlewareAppOptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import picocli.CommandLine;

import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The REST service speaks plain HTTP and is meant for the GUI and API clients on the same machine, so it must
 * not listen on the network unless asked to.
 */
class ServerAddressTest {

    private static Properties shippedProperties() throws IOException {
        Path candidate = Path.of("src", "main", "resources", "application.properties");
        if (Files.notExists(candidate))
            candidate = Path.of("sirius_rest_service", "src", "main", "resources", "application.properties");
        final Path path = candidate;
        assertTrue(Files.exists(path), () -> "application.properties not found at " + path.toAbsolutePath());

        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(path)) {
            props.load(r);
        }
        return props;
    }

    @Test
    void shippedPropertiesBindToLoopbackOnly() throws IOException {
        InetAddress address = new Binder(new MapConfigurationPropertySource(shippedProperties()))
                .bind("server.address", InetAddress.class)
                .orElse(null);

        assertNotNull(address, "no server.address set: Tomcat would listen on all network interfaces");
        assertTrue(address.isLoopbackAddress(), () -> "server.address " + address + " is reachable from the network");
    }

    @Test
    void addressOptionOverridesTheDefault() {
        // the options write System properties as a side effect, which would leak into other tests of this JVM
        Properties saved = (Properties) System.getProperties().clone();
        try {
            System.clearProperty("server.address");
            new CommandLine(new MiddlewareAppOptions<>(null)).parseArgs("--address", "0.0.0.0");
            assertEquals("0.0.0.0", System.getProperty("server.address"));
        } finally {
            System.setProperties(saved);
        }
    }

    @Test
    void withoutAddressOptionTheDefaultIsKept() {
        Properties saved = (Properties) System.getProperties().clone();
        try {
            System.clearProperty("server.address");
            new CommandLine(new MiddlewareAppOptions<>(null)).parseArgs("--port", "8888");
            assertNull(System.getProperty("server.address"), "a System property would override application.properties");
        } finally {
            System.setProperties(saved);
        }
    }
}
