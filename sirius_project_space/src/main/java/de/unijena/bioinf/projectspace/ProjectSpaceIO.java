package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            space = newZipProjectSpace(path, false);
        } else if (isExistingProjectspaceDirectory(path) || (Files.isDirectory(path) && FileUtils.listAndClose(path, Stream::count) == 0)) {
            doTSVConversion(path);
            space = new SiriusProjectSpace(configuration, path);
        } else throw new IOException("Location '" + path + "' is not a valid Project Location");

        space.open();
        return space;
    }

    private static Path doTSVConversionZip(Path path) throws IOException {
        final Path fs = asZipFS(path, false);
        doTSVConversion(fs);
        return fs;
    }

    private static void doTSVConversion(Path path) throws IOException {
        if (FileUtils.listAndClose(path, s -> s.anyMatch(p -> p.getFileName().toString().toLowerCase().endsWith(".tsv")))) {
            return;
        } else {
            List<Path> list = FileUtils.walkAndClose(s -> s.filter(p -> p.toString().endsWith(".csv")).collect(Collectors.toList()), path);
            if (!list.isEmpty()) {
                LOG.warn("Project=Space seems to use outdated '.csv' file extension. Try to convert to new `.tsv` format if necessary.");
                for (Path p : list)
                    Files.move(p, Paths.get(p.toString().replace("csv",".tsv")));
            }
        }
    }

    public SiriusProjectSpace createNewProjectSpace(Path path) throws IOException {
        final SiriusProjectSpace space;
        if (isZipProjectSpace(path)) {
            if (path.getParent() != null && Files.notExists(path.getParent()))
                Files.createDirectories(path.getParent());

            space = newZipProjectSpace(path, true);
        } else {
            if (Files.exists(path)) {
                if (Files.isRegularFile(path) || FileUtils.listAndClose(path, Stream::count) > 0)
                    throw new IOException("Could not create new Project '" + path + "' because it directory already exists and is not empty");
            } else {
                Files.createDirectories(path);
            }
            space = new SiriusProjectSpace(configuration, path);
        }

        space.open();
        return space;
    }

    protected SiriusProjectSpace newZipProjectSpace(Path path, boolean createNew) throws IOException {
        final Path fs = createNew ? asZipFS(path, createNew): doTSVConversionZip(path);
        return new SiriusProjectSpace(configuration, fs);
    }

    protected static Path asZipFS(Path zipFile, boolean createNew) throws IOException {
        final Map<String, String> option = new HashMap<>();
        if (createNew)
            option.put("create", "true");
        FileSystem zipFS = FileSystems.newFileSystem(URI.create("jar:file:" + zipFile.toUri().getPath()), option);
        return zipFS.getPath(zipFS.getSeparator());
    }

    public SiriusProjectSpace createTemporaryProjectSpace() throws IOException {
        final Path tempFile = createTmpProjectSpaceLocation();
        final SiriusProjectSpace space = new SiriusProjectSpace(configuration,tempFile);
        space.addProjectSpaceListener(new TemporaryProjectSpaceCleanUp(tempFile));
        space.open();
        return space;
    }

    public static Path createTmpProjectSpaceLocation() throws IOException {
        return Files.createTempDirectory(".sirius-tmp-project-");
    }

    /**
     * Copies a Project-Space to a new location.
     *
     * @param space               The project to be copied
     * @param copyLocation        target location
     * @param switchToNewLocation if true switch space location to copyLocation (saveAs vs. saveCopy)
     * @return true if space location has been changed successfully and false otherwise
     * @throws IOException if an I/O error happens
     */
    public static boolean copyProject(@NotNull final SiriusProjectSpace space, @NotNull final Path copyLocation, final boolean switchToNewLocation) throws IOException {
        //todo zip to zip copy by simple file copy
        return space.withAllLockedDo(() -> {
            @NotNull final Path nuSpaceLocation;
            final boolean isZipTarget = isZipProjectSpace(copyLocation);
            if (isZipTarget) { //create new mounted zip file for target location
                nuSpaceLocation = asZipFS(copyLocation, true);
            } else {
                nuSpaceLocation = copyLocation;
                Files.createDirectories(nuSpaceLocation);
            }

            FileUtils.copyFolder(space.getRootPath(), nuSpaceLocation); //just copy the data -> mounted ZipFS does the rest

            if (switchToNewLocation)
                return space.changeLocation(nuSpaceLocation);
            else if (isZipTarget){
                nuSpaceLocation.getFileSystem().close();
            }

            return false;
        });
    }

    /**
     * Check for a compressed project-space by file ending
     */
    public static boolean isZipProjectSpace(Path file) {
        if (Files.exists(file) && !Files.isRegularFile(file)) return false;
        final String lowercaseName = file.getFileName().toString().toLowerCase();
        return lowercaseName.endsWith(".workspace") || lowercaseName.endsWith(".sirius");
    }


    /**
     * Just a quick check to discriminate a project-space for an arbitrary folder
     */
    public static boolean isExistingProjectspaceDirectory(@NotNull Path f) {
        return isExistingProjectspaceDirectoryNum(f) > 0;
    }

    public static int isExistingProjectspaceDirectoryNum(@NotNull Path f) {
        try {
            if (!Files.exists(f) || Files.isRegularFile(f) || FileUtils.listAndClose(f, Stream::count) == 0)
                return -1;
            try (SiriusProjectSpace space = new SiriusProjectSpace(new ProjectSpaceConfiguration(), f)) {
                space.open();
                return space.size();
            } catch (IOException ignored) {
                return -2;
            }
        } catch (Exception e) {
            // not critical: if file cannot be read, it is not a valid workspace
            LOG.error("Workspace check failed! This is not a valid Project-Space!", e);
            return -3;
        }
    }


}
