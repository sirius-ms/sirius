package de.unijena.bioinf.ChemistryBase.properties;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 31.08.17.
 */

import com.google.common.reflect.ClassPath;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class PropertyLoader {

    static {
            loadProperties();
    }

    public static void load() {}

    protected static void loadProperties() {
        try {
            List<URL> resources = new LinkedList<>();
            for (ClassPath.ResourceInfo resourceInfo : ClassPath.from(PropertyLoader.class.getClassLoader()).getResources()) {
                if (resourceInfo.getResourceName().endsWith(".build.properties"))
                    resources.add(resourceInfo.url());
            }

            for (URL resource : resources) {
                try (InputStream input = resource.openStream()) {
                    Properties props = new Properties();
                    props.load(input);
                    System.getProperties().putAll(props);
                } catch (IOException e) {
                    System.err.println("Could not load properties from " + resource.toString());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Error while searching for properties files to load!");
            e.printStackTrace();
        }
    }

}
