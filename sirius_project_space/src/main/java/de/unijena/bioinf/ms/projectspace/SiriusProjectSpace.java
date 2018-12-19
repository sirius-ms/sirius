package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/*
 * This is the Sirius Project space. It operates only on uncompressed
 * directory based workspaces. For zip based workspaces: They should be
 * decompressed in some temp dir and copied/compressed back when all tasks are done.
 *
 * */
public class SiriusProjectSpace implements ProjectSpace {

    public static @NotNull SiriusProjectSpace loadProjectSpace(@Nullable FilenameFormatter filenameFormatter, @NotNull final String projectSpaceRoot) throws IOException {
        return loadProjectSpace(filenameFormatter, Paths.get(projectSpaceRoot));
    }

    public static @NotNull SiriusProjectSpace loadProjectSpace(@Nullable FilenameFormatter filenameFormatter, @NotNull final File projectSpaceRoot) throws IOException {
        return loadProjectSpace(filenameFormatter, projectSpaceRoot.toPath());
    }

    public static @NotNull SiriusProjectSpace loadProjectSpace(@Nullable FilenameFormatter filenameFormatter, @NotNull final Path projectSpaceRoot) throws IOException {
        if (Files.notExists(projectSpaceRoot))
            throw new IOException("Path does not exists");
        if (Files.isWritable(projectSpaceRoot))
            throw new IOException("No writing permission for Path");
        final SiriusProjectSpace ps = new SiriusProjectSpace(filenameFormatter, projectSpaceRoot);

        ps.loadProjectSpace();
        return ps;

    }


    public static @NotNull SiriusProjectSpace createNewProjectSpace(@Nullable FilenameFormatter filenameFormatter, @NotNull final String projectSpaceRoot) throws IOException {
        return createNewProjectSpace(filenameFormatter, Paths.get(projectSpaceRoot));
    }

    public static @NotNull SiriusProjectSpace createNewProjectSpace(@Nullable FilenameFormatter filenameFormatter, @NotNull final File projectSpaceRoot) throws IOException {
        return createNewProjectSpace(filenameFormatter, projectSpaceRoot.toPath());
    }


    public static @NotNull SiriusProjectSpace createNewProjectSpace(@Nullable FilenameFormatter filenameFormatter, @NotNull final Path projectSpaceRoot) throws IOException {
        if (Files.notExists(projectSpaceRoot)) {
            Files.createDirectories(projectSpaceRoot);
        } else {
            if (Files.isWritable(projectSpaceRoot))
                throw new IOException("No writing permission for Project-Space makePath: " + projectSpaceRoot.toAbsolutePath().toString());
            if (!Files.isDirectory(projectSpaceRoot))
                throw new IOException("Project-Space makePath is not a Directory: " + projectSpaceRoot.toAbsolutePath().toString());
            if (Files.list(projectSpaceRoot).findAny().isPresent())
                throw new IOException("Project-Space directory is not empty: " + projectSpaceRoot.toAbsolutePath().toString());
        }
        if (filenameFormatter == null)
            filenameFormatter = new StandardMSFilenameFormatter();

        return new SiriusProjectSpace(filenameFormatter, projectSpaceRoot);
    }


    protected final LinkedHashSet<ExperimentDirectory> experimentIDs = new LinkedHashSet<>();
    protected final List<SummaryWriter> summaryWriters = new ArrayList<>();

    protected DirectoryWriter writer;
    protected DirectoryReader reader;
    protected FilenameFormatter filenameFormatter;

    protected int currentMaxIndex = ExperimentDirectory.NO_INDEX;

    //loads existing project-space from reader and uses given writer
    protected SiriusProjectSpace(@NotNull final String rootPath, @NotNull final FilenameFormatter filenameFormatter, MetaDataSerializer... metaDataSerializers) throws IOException {
        this.filenameFormatter = filenameFormatter;
        this.reader = new DirectoryReader(new SiriusWorkspaceReader(new File(rootPath)), metaDataSerializers);
        this.writer = new DirectoryWriter(new SiriusFileWriter(new File(rootPath)), metaDataSerializers);
        summaryWriters.addAll(initBasicSummaries());
        loadProjectSpace();
    }

    protected void loadProjectSpace() {
        experimentIDs.clear();
        reader.forEach(expDir -> {
            experimentIDs.add(expDir);
            currentMaxIndex = Math.max(currentMaxIndex, expDir.getIndex());
        });

        //write .index to convert the project space automatically to ne new format
        experimentIDs.stream().filter(ExperimentDirectory::isRewrite).forEach(expDir -> {
            try {
                LOG.info("Writing " + expDir.getDirectoryName() + "to update your project-space to the current version");
                if (expDir.hasNoIndex())
                    expDir.setIndex(++currentMaxIndex);
                final ExperimentResult expResult = reader.parseExperiment(expDir);
                writeExperiment(expResult);
                expDir.setRewrite(false);
            } catch (IOException e) {
                LOG.error("Could not update you project-space to the new format");
            }
        });
    }

    protected List<SummaryWriter> initBasicSummaries() {
        List<SummaryWriter> sums = new ArrayList<>();

        sums.add((experiments, writer) -> {
            final String v = PropertyManager.getProperty("de.unijena.bioinf.sirius.versionString");
            if (v != null) {
                try {
                    writer.write(SiriusLocations.SIRIUS_VERSION_FILE.fileName(), w -> w.write(v));
                } catch (IOException e) {
                    LOG.error("Could not write Version info", e);
                }
            }
        });

        sums.add((experiments, writer) -> {
            final String v = PropertyManager.getProperty("de.unijena.bioinf.sirius.cite");
            if (v != null) {
                try {
                    writer.write(SiriusLocations.SIRIUS_CITATION_FILE.fileName(), w -> w.write(v));
                } catch (IOException e) {
                    LOG.error("Could not write Citation info", e);
                }
            }
        });

        sums.add((experiments, writer) -> {
            try {
                writer.write(SiriusLocations.SIRIUS_FORMATTER_FILE.fileName(), w -> w.write(filenameFormatter.getFormatExpression()));
            } catch (IOException e) {
                LOG.error("Could not write file formatter info", e);
            }
        });

        return sums;
    }


    private String makeFileName(ExperimentResult exp) {
        return filenameFormatter.formatName(exp,
                exp.getAnnotation(ExperimentDirectory.class).getIndex());
    }

    public String makeFormulaIdentifier(ExperimentResult ex, IdentificationResult result) {
        return makeFileName(ex) + ":" + result.getMolecularFormula() + ":" + SiriusLocations.simplify(result.getPrecursorIonType());
    }

    public String makeMassIdentifier(ExperimentResult ex, IdentificationResult result) {
        return makeFileName(ex) + ":" + ex.getExperiment().getIonMass() + ":" + SiriusLocations.simplify(result.getPrecursorIonType().withoutAdduct());
    }

    public String makePath(ExperimentResult ex, IdentificationResult result, SiriusLocations.Location l) {
        StringBuilder location = new StringBuilder();
        if (ex != null)
            location.append(makeFileName(ex)).append("/");

        if (l.directory != null && !l.directory.isEmpty())
            location.append(l.directory).append("/");

        location.append(l.fileName(result));

        return location.toString();
    }


    //API methods
    @Override
    public void deleteExperiment(ExperimentDirectory id) throws IOException {
        if (!experimentIDs.contains(id))
            throw new IllegalArgumentException("The project-space does not contain the given ID: " + id.getDirectoryName());

        //Files.delete(Paths.get(id.getDirectoryName())); todo implement corectly
        experimentIDs.remove(id);
    }

    @Override
    public ExperimentResult loadExperiment(ExperimentDirectory id) throws IOException {
        return reader.parseExperiment(id);
    }

    @Override
    public void writeExperiment(ExperimentResult result) throws IOException {
        //todo check renaming!!!!!!!!!!!!!!!
        if (!result.hasAnnotation(ExperimentDirectory.class)) {
            writer.writeExperiment();
        }
        writer.writeExperiment(result);
    }

    @Override
    public void writeSummaries() {
        for (SummaryWriter summaryWriter : summaryWriters) {
            summaryWriter.writeSummary(this, writer);
        }
    }

    @Override
    public void registerSummaryWriter(List<SummaryWriter> writerList) {
        writerList.addAll(writerList);
    }


    @NotNull
    public Iterator<ExperimentDirectory> idIterator() {
        return new IdIterator();
    }

    public Iterable<ExperimentDirectory> ids() {
        return this::idIterator;
    }

    @NotNull
    @Override
    public Iterator<ExperimentResult> iterator() {
        return new ExperimentIterator();
    }


    //internal classes
    public class IdIterator implements Iterator<ExperimentDirectory> {
        Iterator<ExperimentDirectory> baseIter = experimentIDs.iterator();
        ExperimentDirectory current;

        @Override
        public boolean hasNext() {
            return baseIter.hasNext();
        }

        @Override
        public ExperimentDirectory next() {
            if (!hasNext()) throw new NoSuchElementException();
            current = baseIter.next();
            return current;
        }

        @Override
        public void remove() {
            try {
                deleteExperiment(current);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public class ExperimentIterator implements Iterator<ExperimentResult> {
        Iterator<ExperimentDirectory> baseIter = experimentIDs.iterator();
        ExperimentDirectory current;

        @Override
        public boolean hasNext() {
            return baseIter.hasNext();
        }

        @Override
        public ExperimentResult next() {
            if (!hasNext()) throw new NoSuchElementException();
            try {
                current = baseIter.next();
                return loadExperiment(current);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void remove() {
            try {
                deleteExperiment(current);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
