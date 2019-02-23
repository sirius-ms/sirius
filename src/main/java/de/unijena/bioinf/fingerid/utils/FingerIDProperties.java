package de.unijena.bioinf.fingerid.utils;


import de.unijena.bioinf.ms.properties.PropertyManager;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * All version numbers are directly taken from the build. So there is no more redundant data. Note that version a.b.c-SNAPSHOT lower than a.b.c
 */
public class FingerIDProperties {
    /*static {
        //todo fix
        //init sirius credentials to global property manager
        Path creds = Paths.get(System.getProperty("user.home"), "sirius.credentials");
        PropertyManager.addPropertiesFromFile(creds);
    }*/


    public static String fingeridVersion() {
        return PropertyManager.getStringProperty("de.unijena.bioinf.fingerid.version");
    }

    public static String fingeridWebHost() {
        return PropertyManager.getStringProperty("de.unijena.bioinf.fingerid.web.host");
    }

    public static String fingeridWebPort() {
        return PropertyManager.getStringProperty("de.unijena.bioinf.fingerid.web.port");
    }

    public static String databaseDate() {
        return PropertyManager.getStringProperty("de.unijena.bioinf.fingerid.db.date");
    }

    public static String siriusVersion() {
        return PropertyManager.getStringProperty("de.unijena.bioinf.sirius.version");
    }

    public static String sirius_guiVersion() {
        return siriusVersion();
    }
}
