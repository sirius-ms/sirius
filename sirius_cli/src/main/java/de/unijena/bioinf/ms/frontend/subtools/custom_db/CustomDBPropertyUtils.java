package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.chemdb.custom.CustomDatabases;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.properties.PropertyManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class CustomDBPropertyUtils {

    public static final String PROP_KEY = "de.unijena.bioinf.chemdb.custom.source";
    public static final String DB_DELIMITER = ",";
    public static final String NAME_DELIMITER = "|";

    /**
     * @return a map from location to DB name in the order they are listed in the property
     */
    public static LinkedHashMap<String, String> getCustomDBs() {
        LinkedHashMap<String, String> dbs = new LinkedHashMap<>();

        String prop = PropertyManager.getProperty(PROP_KEY, null, "");
        if (!prop.isBlank()) {
            String[] dbStrings = prop.split(DB_DELIMITER);
            for (String dbString : dbStrings) {
                String name, location;
                if (dbString.contains(NAME_DELIMITER)) {
                    String[] parts = dbString.split(Pattern.quote(NAME_DELIMITER));
                    location = parts[0];
                    name = parts[1];
                } else {
                    location = dbString;
                    name = getDBName(location);
                }
                dbs.put(location, name);
            }
        }
        return dbs;
    }

    public static String getDBName(String location) {
        return FilenameUtils.getBaseName(location);
    }

    public static void addDB(String location, String name) {
        if (location.contains(DB_DELIMITER) || location.contains(NAME_DELIMITER)) {
            throw new IllegalArgumentException("Database location should not contain special characters " + DB_DELIMITER + NAME_DELIMITER);
        }
        LinkedHashMap<String, String> dbs = getCustomDBs();
        if (dbs.containsKey(location)) {
            if (!dbs.get(location).equals(name)) {
                throw new RuntimeException("Cannot add custom DB " + name + " at " + location + " because it already exists with different name " + dbs.get(location));
            } else {
                log.info("Database {} at {} already exists, skipping.", name, location);
                return;
            }
        }
        if (dbs.containsValue(name)) {
            String previousLocation = getLocationByName(dbs, name).orElseThrow();
            throw new RuntimeException("Custom DB with name " + name + " already exists at " + previousLocation);
        }
        dbs.put(location, name);
        saveDBs(dbs);
    }

    public static void removeDBbyLocation(String location) {
        LinkedHashMap<String, String> dbs = getCustomDBs();
        if (dbs.remove(location) != null) {
            saveDBs(dbs);
        } else {
            log.info("Database at {} is not found, ignoring.", location);
        }
    }

    public static void removeDBbyName(String name) {
        LinkedHashMap<String, String> dbs = getCustomDBs();
        Optional<String> location = getLocationByName(dbs, name);
        if (location.isPresent()) {
            dbs.remove(location.get());
            saveDBs(dbs);
        } else {
            log.info("Database {} is not found, ignoring.", name);
        }
    }

    private static void saveDBs(LinkedHashMap<String, String> dbs) {
        String dbString = dbs.entrySet().stream()
                .filter(e -> Utils.notNullOrBlank(e.getKey()))
                .map(e -> e.getKey() + (getDBName(e.getKey()).equals(e.getValue()) ? "" : NAME_DELIMITER + e.getValue()))
                .collect(Collectors.joining(DB_DELIMITER));
        SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty(PROP_KEY, dbString);
    }

    public static Optional<String> getLocationByName(LinkedHashMap<String, String> dbs, String name) {
        return dbs.entrySet().stream()
                .filter(e -> e.getValue().equals(name))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    /**
     * Read the list of all custom DBs from the properties, and try to open them.
     */
    public static void loadAllCustomDBs(CdkFingerprintVersion version) {
        Set<String> customDbLocations = CustomDBPropertyUtils.getCustomDBs().keySet();
        for (String location : customDbLocations) {
            try {
                CustomDatabases.open(location, true, version, true);
            } catch (Exception e) {
                log.error("Error opening database {}, skipping.", location, e);
            }
        }
    }
}
