/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.agents;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.JarFile;

/**
 * This Agent loads all libs and jar needed for the commercial ILP solvers into
 * the classpath and library path if available.
 * <p>
 * It is executed by the jvm before the main class of the app is executed
 * <p>
 * To use this agent you have to specify this following jvm parameter
 * -javaagent:{PATH_TO_JAR}/agents-{VERSION}.jar
 */

public class ILPAgent {
    private static Instrumentation inst = null;

    // The JRE will call method before launching the main()
    public static void premain(String agentArgs, Instrumentation inst) {
        ILPAgent.inst = inst;
        loadCplexLibs();
        loadGurobiLibs();
    }

    public static void addClassPath(File f) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        try {
            if (!(cl instanceof URLClassLoader)) {
                // If Java 9 or higher use Instrumentation
                inst.appendToSystemClassLoaderSearch(new JarFile(f));
            } else {
                // If Java 8 or below fallback to old method
                Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                m.setAccessible(true);
                m.invoke(cl, f.toURI().toURL());
            }

        } catch (Throwable e) {
            LoggerFactory.getLogger(ILPAgent.class).warn("Error adding jar to classpath at runtime: " + f.toString(), e);
        }
    }

    /*public static void addLibraryPath(String pathToAdd) throws Exception {
        System.out.println("add LD: " + pathToAdd);

        if (pathToAdd == null || pathToAdd.isBlank())
            return;
        String nu = System.getProperty("java.library.path") + ":" + pathToAdd;
        System.setProperty("java.library.path", nu);

        //This code only works up to JDK-11. All later versions will fail here
        //JDK-11 (LTS) end of life September 2023
        //Hopefully jpackage of JDK-17 (LTS) supports env variables so that this code becomes obsolete
        System.out.println("---> : " + pathToAdd);
        *//*Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
        fieldSysPath.setAccessible(true);
        fieldSysPath.set(null, null);*//*
        MethodHandles.Lookup cl = MethodHandles.privateLookupIn(ClassLoader.class, MethodHandles.lookup());
        VarHandle sys_paths = cl.findStaticVarHandle(ClassLoader.class, "sys_paths", String[].class);
        sys_paths.set(null);

        System.out.println("end LD: " + pathToAdd);

    }*/

    /**
     * Adds the specified path to the java library path
     *
     * @param pathToAdd the path to add
     * @throws Exception
     */
    public static void addLibraryPath(String pathToAdd) throws Exception{
        final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
        usrPathsField.setAccessible(true);

        //get array of paths
        final String[] paths = (String[])usrPathsField.get(null);

        //check if the path to add is already present
        for(String path : paths) {
            if(path.equals(pathToAdd)) {
                return;
            }
        }

        //add the new path
        final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
        newPaths[newPaths.length-1] = pathToAdd;
        usrPathsField.set(null, newPaths);
    }


    private static void loadGurobiLibs() {
        try {
            String env = System.getenv("GUROBI_HOME");

            if (env != null && !env.isBlank()) {
                Path p = Path.of(env).resolve("lib");
                if (Files.isDirectory(p)) {
                    addClassPath(p.resolve("gurobi.jar").toFile());
                    addLibraryPath(p.toAbsolutePath().toString());
                }else {
                    throw new Exception("Could not determine a valid Gurobi native libs directory!");
                }
            } else {
                LoggerFactory.getLogger(ILPAgent.class).debug("No GUROBI_HOME env variable found. Gurobi will NOT be available.");
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(ILPAgent.class).warn("Error when importing GUROBI_HOME. GUROBI might not be available", e);
        }
    }

    private static void loadCplexLibs() {
        try {
            String env = System.getenv("CPLEX_HOME");

            if (env != null && !env.isBlank()) {
                Path p = Path.of(env);
                if (p != null && Files.isDirectory(p)) {
                    final String osName = System.getProperty("os.name");
                    addClassPath(p.resolve("lib/cplex.jar").toFile());
                    Path libs = null;
                    if (osName.startsWith("Windows")) {
                        libs = isDirOrNull(p.resolve("bin/x64_win64"));
                    } else if (osName.startsWith("Linux")) {
                        libs = isDirOrNull(p.resolve("bin/x86-64_linux"));
                    } else if (osName.startsWith("Mac")) {
                        libs = isDirOrNull(p.resolve("bin/x86-64_osx"));
                    } else {
                        LoggerFactory.getLogger(ILPAgent.class).warn("Could not determine OS Guessing ILP libs.");
                        libs = isDirOrNull(p.resolve("bin/x64_win64"));
                        if (libs == null)
                            libs = isDirOrNull(p.resolve("bin/x86-64_linux"));
                        if (libs == null)
                            libs = isDirOrNull(p.resolve("bin/x86-64_osx"));
                    }

                    if (libs == null)
                        throw new Exception("Could not determine a valid CPLEX native lib directory!");

                    addLibraryPath(libs.toAbsolutePath().toString());
                }
            } else {
                LoggerFactory.getLogger(ILPAgent.class).debug("No CPLEX_HOME env variable found. CPLEX will NOT be available.");
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(ILPAgent.class).warn("Error when importing CPLEX_HOME. CPLEX might not be available", e);
        }
    }

    private static Path isDirOrNull(Path toCheck) {
        return Files.isDirectory(toCheck) ? toCheck : null;
    }
}
