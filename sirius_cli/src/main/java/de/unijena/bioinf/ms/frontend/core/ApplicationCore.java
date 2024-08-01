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

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilderFactory;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.auth.AuthServices;
import de.unijena.bioinf.fingerid.fingerprints.cache.IFingerprinterCache;
import de.unijena.bioinf.fingerid.fingerprints.cache.NonBlockingIFingerprinterCache;
import de.unijena.bioinf.ms.frontend.bibtex.BibtexManager;
import de.unijena.bioinf.ms.persistence.storage.StorageUtils;
import de.unijena.bioinf.ms.properties.ConfigType;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.properties.SiriusConfigUtils;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.rest.ProxyManager;
import de.unijena.bioinf.sirius.SiriusFactory;
import de.unijena.bioinf.webapi.Tokens;
import de.unijena.bioinf.webapi.WebAPI;
import de.unijena.bioinf.webapi.rest.RestAPI;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXParser;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.LogManager;

import static de.unijena.bioinf.ms.frontend.SiriusCLIApplication.APP_TYPE_PROPERTY_KEY;
import static de.unijena.bioinf.ms.frontend.core.Workspace.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class ApplicationCore {

    public enum AppType {CLI, GUI, SERVICE};

    public static final AppType APP_TYPE = AppType.valueOf(
            System.getProperty(APP_TYPE_PROPERTY_KEY, "CLI"));

    public static final Logger DEFAULT_LOGGER;


    public static final Path TOKEN_FILE;

    private static IFingerprinterCache IFP_CACHE = null;
    public synchronized static IFingerprinterCache IFP_CACHE() {
        if (IFP_CACHE == null)
            IFP_CACHE = new NonBlockingIFingerprinterCache(3 * SiriusJobs.getCPUThreads());
        return IFP_CACHE;
    }

    public static final SiriusFactory SIRIUS_PROVIDER = StorageUtils.siriusProvider();
    public static final WebAPI<?> WEB_API;
    @NotNull
    public static final BibtexManager BIBTEX;

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
            //init static block (does not work via static import Oo)
            Path it = WORKSPACE;
            final String version = VERSION();
            // create ws files
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
                try (BufferedWriter w = Files.newBufferedWriter(customProfileFile, StandardCharsets.UTF_8)) {
                    for (String line : lines) {
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
                PropertyManager.DEFAULTS.addNewConfig(ConfigType.CUSTOM.name(), Files.newInputStream(customProfileFile));
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
            String arch = System.getProperty("os.arch").toLowerCase();
            String os = System.getProperty("os.name").toLowerCase();
            DEFAULT_LOGGER.info("You run " + VERSION_STRING() + " on " + os + "_" + arch);
            DEFAULT_LOGGER.info("You run SIRIUS in '" + APP_TYPE + "' mode." );


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


//            HardwareAbstractionLayer hardware = new SystemInfo().getHardware();
//            int cores = hardware.getProcessor().getPhysicalProcessorCount();
            int threads = Runtime.getRuntime().availableProcessors();
            PropertyManager.setProperty("de.unijena.bioinf.sirius.cpu.cores", String.valueOf(Math.max(1, threads / 2)));
            PropertyManager.setProperty("de.unijena.bioinf.sirius.cpu.threads", String.valueOf(threads));
            DEFAULT_LOGGER.info("CPU check done. " + PropertyManager.getNumberOfCores() + " cores that handle " + PropertyManager.getNumberOfThreads() + " threads were found.");
            measureTime("DONE  Hardware Check, START init bug reporting");


            TOKEN_FILE = WORKSPACE.resolve(PropertyManager.getProperty("de.unijena.bioinf.sirius.security.tokenFile", null, ".rtoken"));

            AuthService service = ProxyManager.applyClient(c -> AuthServices.createDefault(PropertyManager.getProperty("de.unijena.bioinf.sirius.security.audience"), TOKEN_FILE, c));
            Subscription sub = null; //web connection
            try {
                sub = NetUtils.tryAndWait(() -> service.getToken().map(Tokens::getActiveSubscription).orElse(null),
                        () -> NetUtils.checkThreadInterrupt(Thread.currentThread()), 30000) ;
            } catch (Exception e) {
                LoggerFactory.getLogger(ApplicationCore.class).debug("Error when refreshing token", e);
                if (APP_TYPE == AppType.CLI){
                    LoggerFactory.getLogger(ApplicationCore.class).error("Error when refreshing token: " + e.getMessage() + " Your refresh token might be corrupted or invalid. Please clear login and re-login!", e);
                    throw e; // fail CLI execution, since a run without login is likely to mak no sense.
                }else {
                    LoggerFactory.getLogger(ApplicationCore.class).warn("Error when refreshing token: " + e.getMessage() + " Cleaning login information. Please re-login!");
                    AuthServices.clearRefreshToken(service, TOKEN_FILE); // in case token is corrupted or the account has been deleted
                }
            }
            WEB_API = new RestAPI(service, sub);
            DEFAULT_LOGGER.info("Web API initialized.");
            measureTime("DONE init WebAPI");

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

    public static String VERSION() {
        return PropertyManager.getProperty("de.unijena.bioinf.siriusFrontend.version");
    }

    public static DefaultArtifactVersion VERSION_OBJ() {
        return new DefaultArtifactVersion(VERSION());
    }
}

