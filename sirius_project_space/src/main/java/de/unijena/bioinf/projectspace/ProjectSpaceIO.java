/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.properties.PropertyManager;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ProjectSpaceIO {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectSpaceIO.class);

    protected final ProjectSpaceConfiguration configuration;

    public ProjectSpaceIO(ProjectSpaceConfiguration configuration) {
        this.configuration = configuration;
    }

    public SiriusProjectSpace openExistingProjectSpace(Path path) throws IOException {
        final SiriusProjectSpace space;

        if (isZipProjectSpace(path)) {
            space = makeZipProjectSpace(path);
        } else if (isExistingProjectspaceDirectory(path) || (Files.isDirectory(path) &&
                FileUtils.listAndClose(path, s -> s.filter(p -> !p.getFileName().toString().equals(PSLocations.FORMAT)).count()) == 0)) {
            doTSVConversion(path);
            space = new SiriusProjectSpace(configuration, new PathProjectSpaceIOProvider(path));
        } else throw new IOException("Location '" + path + "' is not a valid Project Location");

        space.open();
        return space;
    }

    private static void doTSVConversion(Path path) throws IOException {
        if (FileUtils.listAndClose(path, s -> s.anyMatch(p -> p.getFileName().toString().toLowerCase().endsWith(".tsv")))) {
            return;
        } else {
            List<Path> list = FileUtils.walkAndClose(s -> s.filter(p -> p.toString().toLowerCase().endsWith(".csv")).collect(Collectors.toList()), path);
            if (!list.isEmpty()) {
                LOG.info("Project-Space seems to use outdated '.csv' file extension. Try to convert to new `.tsv` format where necessary. This might take a while.");
                for (Path p : list)
                    Files.move(p, Paths.get(p.toString().replace(".csv", ".tsv")));
                LOG.info("Conversion to '.tsv' based naming scheme FINISHED.");

            }

        }
    }

    private boolean doZip4JTSVConversion(ZipFile zipLocation) throws IOException {
        if (zipLocation.getFileHeaders().stream().anyMatch(h -> !h.isDirectory() && h.getFileName().endsWith(".tsv")))
            return false;
        //search files
        List<FileHeader> hs = zipLocation.getFileHeaders().stream()
                .filter(h -> !h.isDirectory())
                .filter(h -> h.getFileName().endsWith(".csv"))
                .collect(Collectors.toList());
        //if needed unpack change and repack
        if (!hs.isEmpty()) {
            try {
                LOG.info("Project=Space seems to use outdated '.csv' file extension. Try to convert to new `.tsv` format where necessary. This might take a while.");
                Path tmpProject = Path.of(zipLocation.getFile().getAbsolutePath() + "-tmp-" + UUID.randomUUID());
                zipLocation.extractAll(tmpProject.toAbsolutePath().toString());
                for (FileHeader h : hs) {
                    Path p = tmpProject.resolve(h.getFileName());
                    Files.move(p, Paths.get(p.toString().replace(".csv", ".tsv")));
                }

                Path nuLocation = Path.of(tmpProject.toAbsolutePath() + ".sirius");
                ZipParameters para = new ZipParameters();
                para.setIncludeRootFolder(false);
                try (ZipFile nu = new ZipFile(nuLocation.toFile())) {
                    nu.addFolder(tmpProject.toFile(), para);
                }
                FileUtils.deleteRecursively(tmpProject);
                zipLocation.close();
                Files.move(nuLocation, zipLocation.getFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
                LOG.info("Conversion to '.tsv' based naming scheme FINISHED.");
                return true;
            } finally {
                zipLocation.close();
            }
        }
        return false;
    }

    public SiriusProjectSpace createNewProjectSpace(Path path) throws IOException {
        return createNewProjectSpace(path, true);
    }

    public SiriusProjectSpace createNewProjectSpace(Path path, boolean compressed) throws IOException {
        final SiriusProjectSpace space;
        if (isZipProjectSpace(path)) {
            if (path.getParent() != null && Files.notExists(path.getParent()))
                Files.createDirectories(path.getParent());

            space = makeZipProjectSpace(path);
            space.setProjectSpaceProperty(CompressionFormat.class, space.ioProvider.getCompressionFormat());
        } else {
            if (Files.exists(path)) {
                if (Files.isRegularFile(path) || FileUtils.listAndClose(path, Stream::count) > 0)
                    throw new IOException("Could not create new Project '" + path + "' because it directory already exists and is not empty");
            } else {
                Files.createDirectories(path);
            }
            space = new SiriusProjectSpace(configuration, new PathProjectSpaceIOProvider(path));
            if (configuration.hasProjectSpacePropertyRegistered(VersionInfo.class))
                space.setProjectSpaceProperty(VersionInfo.class, new VersionInfo(PropertyManager.getProperty("de.unijena.bioinf.siriusFrontend.version")));

            if (compressed)
                space.setProjectSpaceProperty(CompressionFormat.class, space.ioProvider.getCompressionFormat());
        }

        space.open();
        return space;
    }

    protected SiriusProjectSpace makeZipProjectSpace(Path path) throws IOException {
        ProjectIOProvider<?, ?, ?> provider = getDefaultZipProvider(path);
        if (provider instanceof ZipFSProjectSpaceIOProvider)
            ((ZipFSProjectSpaceIOProvider) provider).fsManager.writeFile(null, ProjectSpaceIO::doTSVConversion);
        else if (provider instanceof Zip4JProjectSpaceIOProvider) {
            if (doZip4JTSVConversion(((Zip4JProjectSpaceIOProvider) provider).zipLocation)) {
                provider = new Zip4JProjectSpaceIOProvider(path);
            }
        } else if (provider instanceof Zip4jvmProjectSpaceIOProvider) {//todo change hack
            doZip4JTSVConversion(new ZipFile(((Zip4jvmProjectSpaceIOProvider) provider).zipLocation.toFile()));
        }

        return new SiriusProjectSpace(configuration, provider);
    }


    public SiriusProjectSpace createTemporaryProjectSpace() throws IOException {
        return createTemporaryProjectSpace(true);
    }

    public SiriusProjectSpace createTemporaryProjectSpace(boolean compressed) throws IOException {
        final Path tempFile = createTmpProjectSpaceLocation();
        final SiriusProjectSpace space = createNewProjectSpace(tempFile, compressed);
        space.addProjectSpaceListener(new TemporaryProjectSpaceCleanUp(tempFile));
        space.open();
        return space;
    }

    public static Path createTmpProjectSpaceLocation() {
        return FileUtils.createTmpProjectSpaceLocation(null);
    }

    /**
     * Copies a Project-Space to a new location.
     *
     * @param sourceSpace         The project to be copied
     * @param targetLocation      target location
     * @param switchToNewLocation if true switch space location to copyLocation (saveAs vs. saveCopy)
     * @return true if space location has been changed successfully and false otherwise
     * @throws IOException if an I/O error happens
     */
    public static boolean copyProject(@NotNull final SiriusProjectSpace sourceSpace, @NotNull final Path targetLocation, final boolean switchToNewLocation) throws IOException {
        try {
            // source space is only read
            return sourceSpace.withAllWriteLockedDo(() -> {
                @NotNull final Path sourceSpaceLocation = sourceSpace.getLocation();

                final boolean isZipTarget = isZipProjectSpace(targetLocation);
                final boolean isZipSource = isZipProjectSpace(sourceSpaceLocation);

                StopWatch t = new StopWatch();
                t.start();

                sourceSpace.flush();
                CompressionFormat sourceFormat = sourceSpace.ioProvider.getCompressionFormat();
                CompressionFormat targetFormat;

                if (isZipSource && isZipTarget) {
                    targetFormat = ZipProvider.getDefaultCompressionFormat();

                    if (Objects.equals(sourceFormat, targetFormat)) {
                        Files.copy(sourceSpaceLocation, targetLocation); //might keep old non hierarchical zip version but is super fast
                        return !switchToNewLocation || sourceSpace.changeLocation(getDefaultZipProvider(targetLocation));
                    }
                }

                try (SiriusProjectSpace targetSpace = new ProjectSpaceIO(sourceSpace.configuration).createNewProjectSpace(targetLocation)) {
                    targetFormat = targetSpace.ioProvider.getCompressionFormat();

                    if (Objects.equals(sourceFormat, targetFormat) && (targetSpace.ioProvider instanceof PathProjectSpaceIOProvider && sourceSpace.ioProvider instanceof PathProjectSpaceIOProvider)) {
                        FileUtils.copyFolder(((PathProjectSpaceIOProvider) sourceSpace.ioProvider).getRoot(), ((PathProjectSpaceIOProvider) targetSpace.ioProvider).getRoot());
                    } else {
                        ProjectWriter w = targetSpace.ioProvider.newWriter(targetSpace::getProjectSpaceProperty);
                        ProjectReader r = sourceSpace.ioProvider.newReader(sourceSpace::getProjectSpaceProperty);
                        //todo in place iteration
                        List<String> files = r.listFilesRecursive(null).stream().filter(p -> !PSLocations.COMPRESSION.equals(p)).collect(Collectors.toList());

                        for (String file : files) {
                            w.binaryFile(file, out -> r.binaryFile(file, in -> in.transferTo(out)));
                        }
                    }

                }
                t.stop();
                System.out.println("Copied Project in: " + t.toString());

                if (switchToNewLocation) {
                    boolean rr = sourceSpace.changeLocation(isZipTarget
                            ? getDefaultZipProvider(targetLocation)
                            : new PathProjectSpaceIOProvider(targetLocation));
                    sourceSpace.setProjectSpaceProperty(CompressionFormat.class, targetFormat);
                    return rr;
                }

                return true;
            });
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Check for a compressed project-space by file ending
     */
    public static boolean isZipProjectSpace(Path file) {
        if (Files.exists(file) && !Files.isRegularFile(file)) return false;
        final String lowercaseName = file.getFileName().toString().toLowerCase();
        return lowercaseName.endsWith(".workspace") || lowercaseName.endsWith(".sirius") || lowercaseName.endsWith(".zip");
    }


    /**
     * Just a quick check to discriminate a project-space for an arbitrary folder
     */
    public static boolean isExistingProjectspaceDirectory(@NotNull Path f) {
        return isExistingProjectspaceDirectoryNum(f) >= 0;
    }

    public static int isExistingProjectspaceDirectoryNum(@NotNull Path f) {
        try {
            if (Files.notExists(f) || Files.isRegularFile(f) || Files.notExists(f.resolve(".format")))
                return -1;
            return FileUtils.listAndClose(f, s -> s.filter(Files::isDirectory).count()).intValue();
        } catch (Exception e) {
            // not critical: if file cannot be read, it is not a valid workspace
            LOG.error("Workspace check failed! This is not a valid Project-Space!", e);
            return -3;
        }
    }

    public static ProjectIOProvider<?, ?, ?> getDefaultZipProvider(@NotNull Path location) {
        return ZipProvider.newInstance(location, PropertyManager.getProperty("de.unijena.bioinf.sirius.project.zipProvider"));
    }

    public static Path defaultProjectDir(){
        String loc = PropertyManager.getProperty("sirius.projects.location");
        if (loc != null)
            return Path.of(loc);
        return Path.of(System.getProperty("user.home")).resolve("sirius-projects");
    }
}