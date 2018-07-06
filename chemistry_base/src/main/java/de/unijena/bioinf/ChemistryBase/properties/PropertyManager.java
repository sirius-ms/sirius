package de.unijena.bioinf.ChemistryBase.properties;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 31.08.17.
 */

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class PropertyManager {
    public static final Properties PROPERTIES;

    static {
        PROPERTIES = loadDefaultProperties();
    }

    public static void addPropertiesFromStream(InputStream stream) throws IOException {
        Properties props = new Properties();
        props.load(stream);
        PropertyManager.PROPERTIES.putAll(props);
    }

    public static void addPropertiesFromFile(Path files) {
        try {
            if (Files.exists(files)) {
                addPropertiesFromStream(Files.newInputStream(files, StandardOpenOption.READ));
            }
        } catch (IOException e) {
            System.err.println("WARNING: could not load Properties from: " + files.toString());
            e.printStackTrace();
        }
    }

    private static Properties loadDefaultProperties() {
        String p = System.getProperties().getProperty("de.unijena.bioinf.ms.sirius.props");
        LinkedHashSet<String> resources = new LinkedHashSet<>();
        resources.add("sirius.build.properties");

        if (p != null && !p.isEmpty())
            resources.addAll(Arrays.asList(p.split(",")));

        Properties global = new Properties();
        for (String resource : resources) {
            try (InputStream input = PropertyManager.class.getResourceAsStream("/" + resource)) {
                Properties props = new Properties();
                props.load(input);
                global.putAll(props);
            } catch (IOException e) {
                System.err.println("Could not load properties from " + resource.toString());
                e.printStackTrace();
            }
        }
        return global;

    }

    public static int getNumberOfCores() {
        return Integer.valueOf(PROPERTIES.getProperty("de.unijena.bioinf.sirius.cpu.cores", "1"));
    }

    public static int getNumberOfThreads() {
        return Integer.valueOf(PROPERTIES.getProperty("de.unijena.bioinf.sirius.cpu.threads", "2"));
    }

    public static void main(String[] args) {
        PropertyManager.PROPERTIES.get("foo");
        System.out.println(PropertyManager.PROPERTIES);
    }

}
