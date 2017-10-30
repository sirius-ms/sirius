package de.unijena.bioinf.sirius.core;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 19.09.16.
 */

import de.unijena.bioinf.ChemistryBase.properties.PropertyLoader;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree.TreeBuilderFactory;
import de.unijena.bioinf.jjobs.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class ApplicationCore {
    public static final Logger DEFAULT_LOGGER;

    private static final String USER_PROPERTIES_FILE_HAEDER = "This is the default Sirius properties file containing default values for all sirius properties that can be set";
    public static final Path USER_PROPERTIES_FILE;
    private static final Properties USER_PROPERTIES;

    public final static Path WORKSPACE;

    public final static String VERSION_STRING;
    public final static String CITATION;
    public final static String CITATION_BIBTEX;

    protected final static JobManager jobManager;


    //creating
    static {
        //search and load build property files
        PropertyLoader.load();
        //init workspace
        String home = System.getProperty("user.home");
        String path = System.getProperty("de.unijena.bioinf.sirius.ws");
        final Path DEFAULT_WORKSPACE = Paths.get(home).resolve(path);
        final Map<String, String> env = System.getenv();
        String ws = env.get("SIRIUS_WORKSPACE");

        if (ws != null) {
            Path wsDir = Paths.get(ws);
            if (Files.isDirectory(wsDir)) {
                WORKSPACE = wsDir;
            } else if (Files.notExists(wsDir)) {
                try {
                    Files.createDirectories(wsDir);
                } catch (IOException e) {
                    System.err.println("Could not create Workspace set in environment variable! Falling back to default Workspace - " + DEFAULT_WORKSPACE.toString());
                    e.printStackTrace();
                    wsDir = DEFAULT_WORKSPACE;
                } finally {
                    WORKSPACE = wsDir;
                }
            } else {
                System.err.println("WARNING: " + wsDir.toString() + " is not a directory! Falling back to default Workspace - " + DEFAULT_WORKSPACE.toString());
                WORKSPACE = DEFAULT_WORKSPACE;
            }
        } else {
            WORKSPACE = DEFAULT_WORKSPACE;
        }

        if (Files.notExists(WORKSPACE)) {
            try {
                Files.createDirectories(WORKSPACE);
            } catch (IOException e) {
                System.err.println("Could NOT create Workspace");
                e.printStackTrace();
                System.exit(1);
            }
        }

        //init logging stuff
        Path loggingPropFile = WORKSPACE.resolve("logging.properties");
        if (Files.notExists(loggingPropFile)) {
            try (InputStream input = ApplicationCore.class.getResourceAsStream("/logging.properties")) {
                // move default properties file
                Files.copy(input, loggingPropFile);

            } catch (IOException | NullPointerException e) {
                System.err.println("Could not set logging properties, using default java logging properties and directories");
                e.printStackTrace();
            }
        }

        if (Files.exists(loggingPropFile)) {
            System.setProperty("java.util.logging.config.file", loggingPropFile.toString());
        }

        DEFAULT_LOGGER = LoggerFactory.getLogger(ApplicationCore.class);
        DEFAULT_LOGGER.debug("Logging service initialized!");
        DEFAULT_LOGGER.debug("java.library.path = " + System.getProperty("java.library.path"));
        DEFAULT_LOGGER.debug("LD_LIBRARY_PATH = " + System.getenv("LD_LIBRARY_PATH"));
        DEFAULT_LOGGER.debug("java.class.path = " + System.getProperty("java.class.path"));
        DEFAULT_LOGGER.info("Sirius Workspace Successfull initialized at: " + WORKSPACE.toAbsolutePath().toString());

        final String version = System.getProperty("de.unijena.bioinf.sirius.version");
        final String build = System.getProperty("de.unijena.bioinf.sirius.build");

        VERSION_STRING = (version != null && build != null) ? "Sirius " + version + " (build " + build + ")" : "Sirius";
        DEFAULT_LOGGER.debug(VERSION_STRING);

        String prop = System.getProperty("de.unijena.bioinf.sirius.cite");
        CITATION = prop != null ? prop : "";
        prop = System.getProperty("de.unijena.bioinf.sirius.cite-bib");
        CITATION_BIBTEX = prop != null ? prop : "";

        DEFAULT_LOGGER.debug("build properties initialized!");

        //init application properties
        USER_PROPERTIES_FILE = WORKSPACE.resolve("sirius.properties");
        USER_PROPERTIES = new Properties();


        try (InputStream stream = ApplicationCore.class.getResourceAsStream("/sirius.properties")) {
            USER_PROPERTIES.load(stream);
            changeDefaultPropterty("de.unijena.bioinf.sirius.fingerID.cache", WORKSPACE.resolve("csi_fingerid_cache").toString());
        } catch (IOException e) {
            DEFAULT_LOGGER.error("Could NOT create sirius properties file", e);
        }


        if (Files.exists(USER_PROPERTIES_FILE)) {
            try (InputStream stream = Files.newInputStream(USER_PROPERTIES_FILE)) {
                Properties tmp = new Properties();
                tmp.load(stream);
                USER_PROPERTIES.putAll(tmp);
            } catch (IOException e) {
                DEFAULT_LOGGER.warn("Could NOT load Properties form user properties file, falling back to default properties", e);
            }
        }

        addDefaultProptery("de.unijena.bioinf.sirius.workspace", WORKSPACE.toAbsolutePath().toString());
        storeUserProperties();
        DEFAULT_LOGGER.debug("application properties initialized!");


        String p = System.getProperty("de.unijena.bioinf.sirius.treebuilder");
        if (p != null && !p.isEmpty()) {
            if (TreeBuilderFactory.setBuilderPriorities(p.replaceAll("\\s", "").split(",")))
                DEFAULT_LOGGER.debug("Treebuilder priorities are set to: " + Arrays.toString(TreeBuilderFactory.getBuilderPriorities()));
            else
                DEFAULT_LOGGER.debug("Could not parse Treebuilder priorities, falling back to default!" + Arrays.toString(TreeBuilderFactory.getBuilderPriorities()));
        }

        DEFAULT_LOGGER.debug(TreeBuilderFactory.ILP_VERSIONS_STRING);

        jobManager = new JobManager(1);

        HardwareAbstractionLayer hardware = new SystemInfo().getHardware();
        addDefaultProptery("de.unijena.bioinf.sirius.cpu.cores", String.valueOf(hardware.getProcessor().getPhysicalProcessorCount()));
        addDefaultProptery("de.unijena.bioinf.sirius.cpu.threads", String.valueOf(hardware.getProcessor().getLogicalProcessorCount()));
    }


    public static void addDefaultPropteries(File properties) throws IOException {
        addDefaultPropteries(properties.toPath());
    }

    public static void addDefaultPropteries(Path properties) throws IOException {
        Properties p = new Properties();
        p.load(Files.newInputStream(properties));
        addDefaultPropteries(p);
    }

    public static void addDefaultPropteries(Properties properties) {
        System.getProperties().putAll(properties);
        System.getProperties().putAll(USER_PROPERTIES);
    }

    public static void addDefaultProptery(String propertyName, String propertyValue) {
        System.setProperty(propertyName, propertyValue);
        System.getProperties().putAll(USER_PROPERTIES);
    }


    public static void changeDefaultPropterties(Properties properties) {
        System.getProperties().putAll(properties);
        USER_PROPERTIES.putAll(properties);
    }

    public static void changeDefaultPropterty(String propertyName, String propertyValue) {
        System.getProperties().setProperty(propertyName, propertyValue);
        USER_PROPERTIES.setProperty(propertyName, propertyValue);
    }

    public static void changeDefaultProptertiesPersistent(Properties properties) {
        changeDefaultPropterties(properties);
        storeUserProperties();
    }


    public static void changeDefaultProptertyPersistent(String propertyName, String propertyValue) {
        changeDefaultPropterty(propertyName, propertyValue);
        storeUserProperties();
    }

    public static Properties getUserCopyOfUserProperties() {
        return new Properties(USER_PROPERTIES);
    }

    private static void storeUserProperties() {
        try {
            Files.deleteIfExists(USER_PROPERTIES_FILE);
            try (OutputStream stream = Files.newOutputStream(USER_PROPERTIES_FILE, StandardOpenOption.CREATE_NEW)) {
                USER_PROPERTIES.store(stream, USER_PROPERTIES_FILE_HAEDER);
            } catch (IOException e) {
                DEFAULT_LOGGER.error("Could not save new Properties file! Changes not saved!", e);
            }
        } catch (IOException e) {
            DEFAULT_LOGGER.error("Could not remove old Properties file! Changes not saved!", e);
        }
    }


}

