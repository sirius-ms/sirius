package de.unijena.bioinf.sirius.core;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 19.09.16.
 */

import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilderFactory;
import de.unijena.bioinf.canopus.Canopus;
import de.unijena.bioinf.fingerid.webapi.WebAPI;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.utils.errorReport.ErrorReporter;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class ApplicationCore {
    public static final Logger DEFAULT_LOGGER;

    public static final String CITATION;
    public static final String CITATION_BIBTEX;

    public static final Path WORKSPACE;
    public static final WebAPI WEB_API;
    public static final Canopus CANOPUS;


    //creating
    static {
        try {
            System.setProperty("de.unijena.bioinf.ms.propertyLocations", "sirius_frontend.build.properties"
            );

            final String version = PropertyManager.PROPERTIES.getString("de.unijena.bioinf.sirius.version");
            final String build = PropertyManager.PROPERTIES.getString("de.unijena.bioinf.sirius.build");

            //#################### start init workspace ################################
            final String home = System.getProperty("user.home");
            final String defaultFolderName = PropertyManager.PROPERTIES.getString("de.unijena.bioinf.sirius.ws.default.name", ".sirius");
            final Path DEFAULT_WORKSPACE = Paths.get(home).resolve(defaultFolderName);
            final Map<String, String> env = System.getenv();
            final String ws = env.get("SIRIUS_WORKSPACE");
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

            // create ws files
            Path loggingPropFile = WORKSPACE.resolve("logging.properties");
            Path siriusPropsFile = WORKSPACE.resolve("sirius.properties");
            Path customProfileFile = WORKSPACE.resolve("custom.config");
            Path versionFile = WORKSPACE.resolve("version");
            try {
                if (Files.exists(versionFile)) {
                    List<String> lines = Files.readAllLines(versionFile);
                    if (lines == null || lines.isEmpty() || !lines.get(0).equals(version)) {
                        deleteFromWorkspace(loggingPropFile, siriusPropsFile, versionFile);
                        Files.write(versionFile, version.getBytes(), StandardOpenOption.CREATE);
                    }
                } else {
                    deleteFromWorkspace(loggingPropFile, siriusPropsFile, versionFile);
                    Files.write(versionFile, version.getBytes(), StandardOpenOption.CREATE);
                }

            } catch (IOException e) {
                System.err.println("Error while reading/writing workspace version file!");
                e.printStackTrace();
                deleteFromWorkspace(loggingPropFile, siriusPropsFile, versionFile);
                try {
                    Files.write(versionFile, version.getBytes(), StandardOpenOption.CREATE);
                } catch (IOException e1) {
                    System.err.println("Error while writing workspace version file!");
                    e1.printStackTrace();
                }
            }

            //#################### end init workspace ################################


            //init logging stuff
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
                //load user props
                Properties logProps = new Properties();
                try (InputStream input = Files.newInputStream(loggingPropFile, StandardOpenOption.READ)) {
                    logProps.load(input);
                } catch (IOException | NullPointerException e) {
                    System.err.println("Could not set logging properties, using default java logging properties and directories");
                    e.printStackTrace();
                }

                //add ErrorReporter LogManager if it exists
                try {
                    String errorReportHandlerClassName = "de.unijena.bioinf.ms.gui.errorReport.ErrorReportHandler";
                    ClassLoader.getSystemClassLoader().loadClass(errorReportHandlerClassName);
                    String handlers = logProps.getProperty("handlers");

                    if (handlers != null && !handlers.isEmpty())
                        handlers += "," + errorReportHandlerClassName;
                    else
                        handlers = errorReportHandlerClassName;

                    logProps.put("handlers", handlers);
                    logProps.put("de.unijena.bioinf.sirius.core.errorReport.ErrorReportHandler.level", "CONFIG");
                    logProps.put("de.unijena.bioinf.sirius.core.errorReport.ErrorReportHandler.formatter", "java.util.logging.SimpleFormatter");
                } catch (ClassNotFoundException ignore) {
                    //System.err.println("Skipping error report logger in CLI");
                    //this is just to skip the error report logger if it is no available (e.g. CLI)
                }


                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    logProps.store(out, "Auto generated in memory prop file");
                    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
                    LogManager.getLogManager().readConfiguration(in);
                } catch (IOException e) {
                    System.err.println("Could not read logging configuration.");
                    e.printStackTrace();
                }
            }

            //create custom properties if it not exists -> everything is commented out
            if (Files.notExists(customProfileFile)) {
                try (InputStream stream = ApplicationCore.class.getResourceAsStream("/custom.config")) {
                    List<String> lines =
                            new BufferedReader(new InputStreamReader(stream,
                                    StandardCharsets.UTF_8)).lines().map(line -> line.startsWith("#") ? line : "#" + line).collect(Collectors.toList());
                    Files.write(customProfileFile, lines);
                } catch (IOException e) {
                    System.err.println("Could NOT create sirius properties file");
                    e.printStackTrace();
                }
            }

            //overite default profiles from chemistry-base with custom profile
            try {
                PropertyManager.addPropertiesFromStream(Files.newInputStream(customProfileFile), "custom_configs", PropertyManager.MS_CONFIGS_BASE);
            } catch (IOException | ConfigurationException e) {
                System.err.println("Could not load custom Configs");
                e.printStackTrace();
            }

            DEFAULT_LOGGER = LoggerFactory.getLogger(ApplicationCore.class);
            DEFAULT_LOGGER.debug("Logging service initialized!");
            DEFAULT_LOGGER.debug("java.library.path = " + System.getProperty("java.library.path"));
            DEFAULT_LOGGER.debug("LD_LIBRARY_PATH = " + System.getenv("LD_LIBRARY_PATH"));
            DEFAULT_LOGGER.debug("java.class.path = " + System.getProperty("java.class.path"));
            DEFAULT_LOGGER.info("Sirius Workspace Successfull initialized at: " + WORKSPACE.toAbsolutePath().toString());


            PropertyManager.PROPERTIES.setProperty("de.unijena.bioinf.sirius.versionString", (version != null && build != null) ? "SIRIUS " + version + " (build " + build + ")" : "SIRIUS <Version Unknown>");
            DEFAULT_LOGGER.info("You run " + VERSION_STRING());

            String prop = PropertyManager.PROPERTIES.getString("de.unijena.bioinf.sirius.cite");
            CITATION = prop != null ? prop : "";
            prop = PropertyManager.PROPERTIES.getString("de.unijena.bioinf.sirius.cite-bib");
            CITATION_BIBTEX = prop != null ? prop : "";

            DEFAULT_LOGGER.debug("build properties initialized!");

            //init application properties
            //todo use apache property configuration to keep layout and comments
            Properties defaultProps = new Properties();
            try (InputStream stream = ApplicationCore.class.getResourceAsStream("/sirius.properties")) {
                defaultProps.load(stream);
                defaultProps.put("de.unijena.bioinf.sirius.fingerID.cache", WORKSPACE.resolve("csi_fingerid_cache").toString());
            } catch (IOException e) {
                DEFAULT_LOGGER.error("Could NOT create sirius properties file", e);
            }


            SiriusProperties.initSiriusPropertyFile(siriusPropsFile, defaultProps);
            PropertyManager.PROPERTIES.setProperty("de.unijena.bioinf.sirius.workspace", WORKSPACE.toAbsolutePath().toString());
            DEFAULT_LOGGER.debug("application properties initialized!");


            DEFAULT_LOGGER.info(TreeBuilderFactory.ILP_VERSIONS_STRING);
            DEFAULT_LOGGER.info("Treebuilder priorities are: " + Arrays.toString(TreeBuilderFactory.getBuilderPriorities()));


            HardwareAbstractionLayer hardware = new SystemInfo().getHardware();
            int cores = hardware.getProcessor().getPhysicalProcessorCount();
            PropertyManager.PROPERTIES.setProperty("de.unijena.bioinf.sirius.cpu.cores", String.valueOf(cores));
            PropertyManager.PROPERTIES.setProperty("de.unijena.bioinf.sirius.cpu.threads", String.valueOf(hardware.getProcessor().getLogicalProcessorCount()));
            DEFAULT_LOGGER.info("CPU check done. " + PropertyManager.getNumberOfCores() + " cores that handle " + PropertyManager.getNumberOfThreads() + " threads were found.");

            //bug reporting
            ErrorReporter.INIT_PROPS(PropertyManager.asProperties());
            DEFAULT_LOGGER.info("Bug reporter initialized.");

            WEB_API = new WebAPI();
            DEFAULT_LOGGER.info("Web API initialized.");

            //todo get canopus from server
            Canopus c = null;
            try {
                c = Canopus.loadFromFile(new File("/home/fleisch/work/sirius_testing/canopus/canopus_fp.data"));
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                CANOPUS = c;
            }
        } catch (Throwable e) {
            System.err.println("Application Core STATIC Block Error!");
            e.printStackTrace(System.err);
            throw e;
        }
    }

    private static void deleteFromWorkspace(final Path... files) {
        for (Path file : files) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                System.err.println("Could NOT delete " + file.toAbsolutePath().toString());
                e.printStackTrace();
            }
        }
    }

    public static String VERSION_STRING(){
        return PropertyManager.PROPERTIES.getString("de.unijena.bioinf.sirius.versionString");
    }

    public static void cite() {
        System.out.println(System.lineSeparator() + System.lineSeparator() + "Please cite the following publications when using our tool:" + System.lineSeparator());
        System.out.println(ApplicationCore.CITATION);
    }
}

