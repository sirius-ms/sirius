package de.unijena.bioinf.ms.properties;

import org.reflections8.Reflections;
import org.reflections8.scanners.ResourcesScanner;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ReflectionConfigCrawler {
    private static Reflections INSTANCE = null;


    private synchronized static Reflections makeReflection(){
        Reflections.log.ifPresent(l -> {
            Logger logger = LogManager.getLogManager().getLogger(l.getName());
            if (logger != null)
                logger.setLevel(Level.SEVERE);
        });

        return new Reflections(PropertyManager.DEFAULT_CONFIG_SOURCE, new ResourcesScanner());
    }

    public synchronized static void clear(){
        INSTANCE = null;
    }

    public synchronized static Set<String> getAllConfigs() {
        if (INSTANCE == null)
            INSTANCE = makeReflection();

        return INSTANCE.getResources(Pattern.compile(".*\\.config"));
    }

    public synchronized static Set<String> getAllClassMaps() {
        if (INSTANCE == null)
            INSTANCE = makeReflection();
        return INSTANCE.getResources(Pattern.compile(".*\\.map"));
    }
}
