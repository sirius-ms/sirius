/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.frontend.core;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilderFactory;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.auth.AuthServices;
import de.unijena.bioinf.ms.frontend.bibtex.BibtexManager;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.properties.SiriusConfigUtils;
import de.unijena.bioinf.sirius.SiriusCachedFactory;
import de.unijena.bioinf.sirius.SiriusFactory;
import de.unijena.bioinf.utils.errorReport.ErrorReporter;
import de.unijena.bioinf.webapi.ProxyManager;
import de.unijena.bioinf.webapi.WebAPI;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.log4j.Level;
import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXParser;
import org.jetbrains.annotations.NotNull;
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
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.SimpleFormatter;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class ApplicationCore {
    public static final Logger DEFAULT_LOGGER;

    public static final Path WORKSPACE;
    public static final Path TOKEN_FILE;

    public static final SiriusFactory SIRIUS_PROVIDER = new SiriusCachedFactory();
    public static final WebAPI WEB_API;
    @NotNull public static final BibtexManager BIBTEX;

    private static final boolean TIME = false;
    private static long t1;

    public static void measureTime(String message) {
        if (TIME) {
            long t2 = System.currentTimeMillis();
            System.err.println("===> " + message + " - " + (t2 - t1) / 1000d);
            t1 = t2;
        }
    }


    //creating
    static {
        if (TIME)
            t1 = System.currentTimeMillis();
        measureTime("Start AppCore");
        try {
//            System.setProperty("de.unijena.bioinf.jjobs.DEBUG", "true");

//            System.out.println("LD ==> " + System.getProperty("java.library.path"));
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
            org.apache.log4j.Logger.getLogger("net.sf.jnati").setLevel(Level.WARN);


            System.setProperty("de.unijena.bioinf.ms.propertyLocations", "sirius_frontend.build.properties");

            final String version = PropertyManager.getProperty("de.unijena.bioinf.siriusFrontend.version");
            final String[] versionParts = version.split("[.]");
            //#################### start init workspace ################################
            measureTime("Start init Workspace");
            final String home = System.getProperty("user.home");
            String defaultFolderName = PropertyManager.getProperty("de.unijena.bioinf.sirius.ws.default.name", null, ".sirius");
            if (versionParts != null && versionParts.length > 1)
                defaultFolderName = defaultFolderName + "-" + versionParts[0] + "." + versionParts[1];

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
            measureTime("DONE init Workspace, START init logging");

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
                measureTime("DONE init logging, START init Configs");

                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    logProps.store(out, "Auto generated in memory prop file");
                    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
                    LogManager.getLogManager().readConfiguration(in);

                    //add ErrorReporter LogManager if it exists
                    try {
                        final Class<?> handlerClss = ApplicationCore.class.getClassLoader().loadClass("de.unijena.bioinf.ms.gui.errorReport.ErrorReportHandler");
                        Handler handler = (Handler) handlerClss.getConstructor().newInstance();
                        handler.setLevel(java.util.logging.Level.CONFIG);
                        handler.setFormatter(new SimpleFormatter());
                        LogManager.getLogManager().getLogger("").addHandler(handler);
                    } catch (ClassNotFoundException ignore) {
                        //System.err.println("Skipping error report logger in CLI");
                        //this is just to skip the error report logger if it is no available (e.g. CLI)
                    }
                } catch (IOException e) {
                    System.err.println("Could not read logging configuration.");
                    e.printStackTrace();
                }
            }

            //create custom properties if it not exists -> everything is commented out
            if (Files.notExists(customProfileFile)) {
                final StringWriter buff = new StringWriter();
                PropertyManager.DEFAULTS.write(buff);
                String[] lines = buff.toString().split(System.lineSeparator());
                try (BufferedWriter w = Files.newBufferedWriter(customProfileFile,StandardCharsets.UTF_8)) {
                    for(String line : lines){
                        w.write(line.startsWith("#") ? line : "#" + line);
                        w.newLine();
                    }
                } catch (IOException e) {
                    System.err.println("Could NOT create sirius properties file");
                    e.printStackTrace();
                }
            }

            //overite default profiles from modules with custom profile
            try {
                PropertyManager.DEFAULTS.addNewConfig("CUSTOM_CONFIG", Files.newInputStream(customProfileFile));
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


            PropertyManager.setProperty("de.unijena.bioinf.sirius.versionString", (version != null) ? "SIRIUS " + version : "SIRIUS <Version Unknown>");
            DEFAULT_LOGGER.info("You run " + VERSION_STRING());

            BibTeXDatabase bibtex = null;
            try {
                BibTeXParser bibParser = new BibTeXParser();
                bibtex = bibParser.parse(new InputStreamReader(ApplicationCore.class.getResourceAsStream("/cite.bibtex")));
            } catch (Exception e) {
                DEFAULT_LOGGER.warn("Could NOT parse citation file. Citations may not be shown.", e);
            } finally {
                BIBTEX = new BibtexManager(bibtex);
            }


            DEFAULT_LOGGER.debug("build properties initialized!");

            //init application properties
            try (InputStream stream = ApplicationCore.class.getResourceAsStream("/sirius.properties")) {
                final PropertiesConfiguration defaultProps = SiriusConfigUtils.makeConfigFromStream(stream);
                defaultProps.setProperty("de.unijena.bioinf.sirius.fingerID.cache", WORKSPACE.resolve("csi_fingerid_cache").toString());
                SiriusProperties.initSiriusPropertyFile(siriusPropsFile.toFile(), defaultProps);
            } catch (IOException e) {
                DEFAULT_LOGGER.error("Could NOT create sirius properties file", e);
            }


            PropertyManager.setProperty("de.unijena.bioinf.sirius.workspace", WORKSPACE.toAbsolutePath().toString());
            DEFAULT_LOGGER.debug("application properties initialized!");


            DEFAULT_LOGGER.info(TreeBuilderFactory.ILP_VERSIONS_STRING);
            DEFAULT_LOGGER.info("Treebuilder priorities loaded from 'sirius.properties' are: " + Arrays.toString(TreeBuilderFactory.getBuilderPriorities()));

            measureTime("DONE init Configs, start Hardware Check");


            HardwareAbstractionLayer hardware = new SystemInfo().getHardware();
            int cores = hardware.getProcessor().getPhysicalProcessorCount();
            PropertyManager.setProperty("de.unijena.bioinf.sirius.cpu.cores", String.valueOf(cores));
            PropertyManager.setProperty("de.unijena.bioinf.sirius.cpu.threads", String.valueOf(hardware.getProcessor().getLogicalProcessorCount()));
            DEFAULT_LOGGER.info("CPU check done. " + PropertyManager.getNumberOfCores() + " cores that handle " + PropertyManager.getNumberOfThreads() + " threads were found.");
            measureTime("DONE  Hardware Check, START init bug reporting");


            //bug reporting
            ErrorReporter.INIT_PROPS(PropertyManager.asProperties());
            DEFAULT_LOGGER.info("Bug reporter initialized.");

            measureTime("DONE init bug reporting, START init WebAPI");

            TOKEN_FILE = WORKSPACE.resolve(PropertyManager.getProperty("de.unijena.bioinf.sirius.security.tokenFile",null,".rtoken"));
            AuthService service = AuthServices.createDefault(TOKEN_FILE, ProxyManager.getSirirusHttpAsyncClient());
            WEB_API = new WebAPI(service);
            DEFAULT_LOGGER.info("Web API initialized.");
            measureTime("DONE init  init WebAPI");

        } catch (Throwable e) {
            System.err.println("Application Core STATIC Block Error!");
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
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

    public static String VERSION_STRING() {
        return PropertyManager.getProperty("de.unijena.bioinf.sirius.versionString");
    }
}

