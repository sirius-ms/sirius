package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.ExperimentResult;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is the SiriusProjectSpace. It operates only on uncompressed
 * directory based projects. For zip based projects: They should be
 * decompressed in some temp dir and copied/compressed back after the
 * project-space was closed.
 *
 * @author Markus Fleischauer
 * */
public class SiriusProjectSpace implements ProjectSpace {
    protected static final Logger LOG = LoggerFactory.getLogger(SiriusProjectSpace.class);

    public static @NotNull SiriusProjectSpace create(@Nullable FilenameFormatter filenameFormatter, @NotNull final Path projectSpaceRoot, MetaDataSerializer... metaDataSerializers) throws IOException {
        return create(filenameFormatter, projectSpaceRoot.toFile(), metaDataSerializers);
    }

    public static @NotNull SiriusProjectSpace create(@Nullable FilenameFormatter filenameFormatter, @NotNull final String projectSpaceRoot, MetaDataSerializer... metaDataSerializers) throws IOException {
        return create(filenameFormatter, new File(projectSpaceRoot), metaDataSerializers);
    }

    public static @NotNull SiriusProjectSpace create(@Nullable FilenameFormatter filenameFormatter, @NotNull final File projectSpaceRoot, MetaDataSerializer... metaDataSerializers) throws IOException {
        SiriusProjectSpace space = new SiriusProjectSpace(projectSpaceRoot, filenameFormatter, metaDataSerializers);
        space.loadProjectSpace();
        return space;
    }

    public static SiriusProjectSpace create(@NotNull final File rootOutPath, @NotNull final Collection<File> rootInputPaths, @Nullable final FilenameFormatter filenameFormatter, MetaDataSerializer... metaDataSerializers) throws IOException {
        final SiriusProjectSpace merged = new SiriusProjectSpace(rootOutPath, filenameFormatter, metaDataSerializers);
        final TIntSet ids = new TIntHashSet();
        merged.loadProjectSpace(merged.reader, ids, false);
        rootInputPaths.remove(rootOutPath);
        for (File file : rootInputPaths) {
            // check for zip stream
            final DirectoryReader.ReadingEnvironment env = file.isDirectory() ? new SiriusFileReader(file) : new SiriusZipFileReader(file);
            merged.loadProjectSpace(new DirectoryReader(env), ids, true);
        }
        return merged;

        //todo skip existing as in previous merger? could it not be that the user wants to
        // compare result of runs with different parameters?

        //protected HashSet<String> ignoreSet = new HashSet<>();
        //ignoreSet.add(result.getExperimentSource() + "_" + result.getExperimentName());
    }


    private final LinkedHashMap<String, ExperimentDirectory> experimentIDs = new LinkedHashMap<>();
    protected final List<SummaryWriter> summaryWriters = new ArrayList<>();

    protected DirectoryWriter writer;
    protected DirectoryReader reader;
    protected FilenameFormatter filenameFormatter;

    protected int currentMaxIndex = ExperimentDirectory.NO_INDEX;

    //loads existing project-space from reader and uses given writer
    protected SiriusProjectSpace(@NotNull File rootPath, @Nullable FilenameFormatter filenameFormatter, MetaDataSerializer... metaDataSerializers) throws IOException {
        rootPath.mkdirs();

        if (!rootPath.canWrite())
            throw new IOException("No writing permission for Project-Space makePath: " + rootPath.getAbsolutePath());
        if (!rootPath.isDirectory())
            throw new IOException("Project-Space path is not a Directory but has to be: " + rootPath.getAbsolutePath());

        this.reader = new DirectoryReader(new SiriusFileReader(rootPath), metaDataSerializers);
        this.writer = new DirectoryWriter(new SiriusFileWriter(rootPath), metaDataSerializers);

        this.filenameFormatter = filenameFormatter != null ? filenameFormatter : readFormatter(rootPath);

        summaryWriters.addAll(initBasicSummaries());
    }

    @NotNull
    private FilenameFormatter readFormatter(File rootPath) {
        FilenameFormatter filenameFormatter = null;
        Path formatFile = rootPath.toPath().resolve(SiriusLocations.SIRIUS_FORMATTER_FILE.fileName());
        if (Files.exists(formatFile)) {
            try {
                String l = Files.readAllLines(formatFile).stream().findFirst().orElse(null);
                if (l != null && !l.isEmpty())
                    try {
                        filenameFormatter = new StandardMSFilenameFormatter(l);
                    } catch (ParseException e) {
                        LOG.warn("Could not parse FileFormatter string: " + l);
                    }
            } catch (IOException ignored) {
                LOG.warn("Could not read FileFormatter file: " + formatFile.toAbsolutePath().toString());
            }
        }
        return filenameFormatter != null ? filenameFormatter : new StandardMSFilenameFormatter();
    }

    protected void loadProjectSpace() {
        experimentIDs.clear();
        loadProjectSpace(reader, new TIntHashSet(), false);
    }

    private SiriusProjectSpace loadProjectSpace(@NotNull final DirectoryReader r, @NotNull final TIntSet ids, boolean forceRewrite) {
        r.forEach(expDir -> {
            //resolve index conflicts
            if (expDir.hasIndex() && !ids.add(expDir.getIndex())) {
                expDir.setIndex(ExperimentDirectory.NO_INDEX);
                expDir.setRewrite(true);
            } else {
                expDir.setRewrite(forceRewrite || expDir.isRewrite());
            }

            addID(expDir);
            currentMaxIndex = Math.max(currentMaxIndex, expDir.getIndex());
        });

        experimentIDs.values().stream().filter(ExperimentDirectory::hasNoIndex).forEach(expDir ->
                expDir.setIndex(++currentMaxIndex));

        //rewrite Experiments if necessary. Either to convert the project space to a new format
        // or move the Experiments to a new writing location or apply a new naming convention
        experimentIDs.values().stream().filter(ExperimentDirectory::isRewrite).collect(Collectors.toList()).forEach(expDir -> {
            try {
                LOG.info("Writing " + expDir.getDirectoryName() + "to update your project-space to the current version");
                final ExperimentResult expResult = r.parseExperiment(expDir);
                writeExperiment(expResult);
                expDir.setRewrite(false);
            } catch (IOException e) {
                LOG.error("Could not update you project-space to the new format");
            }
        });
        return this;
    }

    private ExperimentDirectory createID(@NotNull ExperimentResult result) {
        final int index = ++currentMaxIndex;
        final ExperimentDirectory dir = new ExperimentDirectory(filenameFormatter.formatName(result, index));
        dir.setIndex(index);
        return dir;
    }

    private ExperimentDirectory addID(ExperimentDirectory expDir) {
        return experimentIDs.put(expDir.getDirectoryName(), expDir);
    }

    private ExperimentDirectory removeID(ExperimentDirectory expDir) {
        return experimentIDs.remove(expDir.getDirectoryName());
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


    //API methods

    /**
     * Parses an experiment with the given ID.
     * Override this method to implement caching for the project-space
     *
     * @param id ExperimentDirectory that identifies the experiment to parse.
     *           The id needs to be part of the project-space
     * @return The parsed ExperimentResult annotated with the given id
     *
     * @throws IllegalArgumentException if the project-space does not contain the given id/path
     * @throws IOException            if the given path cannot be parsed from disk.
     *
     */
    @Override
    public ExperimentResult parseExperiment(ExperimentDirectory id) throws IOException {
        if (!experimentIDs.containsKey(id.getDirectoryName()))
            throw new IllegalArgumentException("The project-space does not contain the given ID: " + id.getDirectoryName());

        return reader.parseExperiment(id);
    }

    @Override
    public void deleteExperiment(ExperimentDirectory id) throws IOException {
        if (removeID(id) == null)
            throw new IllegalArgumentException("The project-space does not contain the given ID: " + id.getDirectoryName());
        writer.deleteExperiment(id);
    }

    @Override
    public void writeExperiment(final @NotNull ExperimentResult result) throws IOException {
        final ExperimentDirectory expDir = result.computeAnnotationIfAbsent(ExperimentDirectory.class, () -> createID(result));
        final String nuName = filenameFormatter.formatName(result, result.getAnnotation(ExperimentDirectory.class).getIndex());

        if (!nuName.equals(expDir.getDirectoryName())) { //rewrite with new name and delete old
            //check if new name conflicts with old name
            if (experimentIDs.containsKey(nuName))
                throw new IOException(
                        "Duplicate Experiment name. " +
                                "Either your naming scheme doe not create unique experiment names " +
                                "or som experiment naming results in a naming conflict");

            final ExperimentDirectory deleteKey = new ExperimentDirectory(expDir.getDirectoryName());
            expDir.setDirectoryName(nuName);
            addID(expDir);
            writer.writeExperiment(result);
            deleteExperiment(deleteKey);
        } else { //override old
            writer.writeExperiment(result);
        }
    }

    @Override
    public void writeSummaries(Iterable<ExperimentResult> resultsToSummarize) {
        for (SummaryWriter summaryWriter : summaryWriters) {
            summaryWriter.writeSummary(resultsToSummarize, writer);
        }
    }

    @Override
    public void registerSummaryWriter(List<SummaryWriter> writers) {
        summaryWriters.addAll(writers);
    }

    @Override
    public boolean removeSummaryWriter(List<SummaryWriter> writers) {
        return summaryWriters.removeAll(writers);
    }

    @Override
    public int getNumberOfWrittenExperiments() {
        return experimentIDs.size();
    }

    @Override
    public void close() throws IOException {
        reader.close();
        writer.close();
    }

    @NotNull
    @Override
    public Iterable<ExperimentResult> parseExperiments() {
        return ExperimentIterator::new;
    }

    @NotNull
    @Override
    public Iterator<ExperimentDirectory> iterator() {
        return new IdIterator();
    }

    //internal classes
    public class IdIterator implements Iterator<ExperimentDirectory> {
        private final Iterator<ExperimentDirectory> baseIter = experimentIDs.values().iterator();
        private ExperimentDirectory current;

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
        private final Iterator<ExperimentDirectory> baseIter = experimentIDs.values().iterator();
        private ExperimentDirectory current;

        @Override
        public boolean hasNext() {
            return baseIter.hasNext();
        }

        @Override
        public ExperimentResult next() {
            if (!hasNext()) throw new NoSuchElementException();
            try {
                current = baseIter.next();
                return parseExperiment(current);
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
