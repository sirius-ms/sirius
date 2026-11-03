package de.unijena.bioinf.ms.biotransformer;

import org.junit.jupiter.api.Test;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class MySqlSchemeDriverTest {

    @Test
    void rewritesTheUrlsBioTransformerHasCompiledIn() {
        // the two constants inside BioTransformer's RetriveFromDB
        assertEquals("jdbc:mariadb://162.243.63.130:3306/hmdb?permitMysqlScheme",
                MySqlSchemeDriver.toMariaDbUrl("jdbc:mysql://162.243.63.130:3306/hmdb"));
        assertEquals("jdbc:mariadb://192.241.161.227/pathbank?permitMysqlScheme",
                MySqlSchemeDriver.toMariaDbUrl("jdbc:mysql://192.241.161.227/pathbank"));
    }

    @Test
    void keepsExistingParameters() {
        assertEquals("jdbc:mariadb://h/db?useSsl=true&permitMysqlScheme",
                MySqlSchemeDriver.toMariaDbUrl("jdbc:mysql://h/db?useSsl=true"));
        assertEquals("jdbc:mariadb://h/db?permitMysqlScheme",
                MySqlSchemeDriver.toMariaDbUrl("jdbc:mysql://h/db?permitMysqlScheme"));
    }

    @Test
    void leavesForeignUrlsAlone() {
        assertNull(MySqlSchemeDriver.toMariaDbUrl(null));
        assertEquals("jdbc:postgresql://h/db", MySqlSchemeDriver.toMariaDbUrl("jdbc:postgresql://h/db"));
        assertFalse(new MySqlSchemeDriver().acceptsURL("jdbc:postgresql://h/db"));
        assertTrue(new MySqlSchemeDriver().acceptsURL("jdbc:mysql://h/db"));
    }

    @Test
    void isRegisteredForTheMysqlScheme() throws Exception {
        Driver found = null;
        for (Driver d : Collections.list(DriverManager.getDrivers()))
            if (d instanceof MySqlSchemeDriver) found = d;
        assertNotNull(found, "MySqlSchemeDriver should be registered via META-INF/services");
        assertSame(found, DriverManager.getDriver("jdbc:mysql://162.243.63.130:3306/hmdb"),
                "DriverManager must pick our driver for BioTransformer's URLs");
    }
}
