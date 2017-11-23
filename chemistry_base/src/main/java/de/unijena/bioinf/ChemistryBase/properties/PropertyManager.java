package de.unijena.bioinf.ChemistryBase.properties;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 31.08.17.
 */

import com.google.common.reflect.ClassPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class PropertyManager {
    public static final Logger LOGGER = LoggerFactory.getLogger(PropertyManager.class);
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
        Properties global = new Properties();
        try {
            List<URL> resources = new LinkedList<>();
            for (ClassPath.ResourceInfo resourceInfo : ClassPath.from(PropertyManager.class.getClassLoader()).getResources()) {
                if (resourceInfo.getResourceName().endsWith(".build.properties"))
                    resources.add(resourceInfo.url());
            }


            for (URL resource : resources) {
                System.out.println("Try loading properties from: " + resource.getPath());
                try (InputStream input = resource.openStream()) {
                    Properties props = new Properties();
                    props.load(input);
                    global.putAll(props);
                } catch (IOException e) {
                    System.err.println("Could not load properties from " + resource.toString());
                    e.printStackTrace();
                }
            }
            return global;

        } catch (IOException e) {
            System.err.println("Error while searching for properties files to load!");
            e.printStackTrace();
        }
        return global;
    }

}
