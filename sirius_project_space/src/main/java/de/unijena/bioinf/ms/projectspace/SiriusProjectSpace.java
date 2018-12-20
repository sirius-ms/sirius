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
import java.nio.file.Path;
import java.util.*;

/*
 * This is the Sirius Project space. It operates only on uncompressed
 * directory based projects. For zip based projects: They should be
 * decompressed in some temp dir and copied/compressed back when all tasks are done (close method).
 *
 * */
public class SiriusProjectSpace implements ProjectSpace {
    protected static final Logger LOG = LoggerFactory.getLogger(SiriusProjectSpace.class);

    public static @NotNull SiriusProjectSpace createProjectSpace(@Nullable FilenameFormatter filenameFormatter, @NotNull final Path projectSpaceRoot) throws IOException {
        return createProjectSpace(filenameFormatter, projectSpaceRoot.toFile());
    }

    public static @NotNull SiriusProjectSpace createProjectSpace(@Nullable FilenameFormatter filenameFormatter, @NotNull final String projectSpaceRoot) throws IOException {
        return createProjectSpace(filenameFormatter, new File(projectSpaceRoot));
    }

    public static @NotNull SiriusProjectSpace createProjectSpace(@Nullable FilenameFormatter filenameFormatter, @NotNull final File projectSpaceRoot) throws IOException {
        if (filenameFormatter == null)
            filenameFormatter = new StandardMSFilenameFormatter();

        SiriusProjectSpace space = new SiriusProjectSpace(projectSpaceRoot, filenameFormatter);
        space.loadProjectSpace();
        return space;
    }

    public static SiriusProjectSpace createMergedProjectSpace(@NotNull final File rootOutPath, @NotNull final Iterable<File> rootInputPaths, @NotNull final FilenameFormatter filenameFormatter, MetaDataSerializer... metaDataSerializers) throws IOException {
        final SiriusProjectSpace merged = new SiriusProjectSpace(rootOutPath, filenameFormatter, metaDataSerializers);
        final TIntSet ids = new TIntHashSet();
        merged.loadProjectSpace(merged.reader, ids, false);
        for (File file : rootInputPaths) {
            // check for zip stream
            final DirectoryReader.ReadingEnvironment env = file.isDirectory() ? new SiriusFileReader(file) : new SiriusZipFileReader(file);
            merged.loadProjectSpace(new DirectoryReader(env), ids, true);
        }
        return merged;
        //todo skip existing as in previous merger?
        //protected HashSet<String> ignoreSet = new HashSet<>();
        //ignoreSet.add(result.getExperimentSource() + "_" + result.getExperimentName());
    }


    protected final LinkedHashSet<ExperimentDirectory> experimentIDs = new LinkedHashSet<>();
    protected final List<SummaryWriter> summaryWriters = new ArrayList<>();

    protected DirectoryWriter writer;
    protected DirectoryReader reader;
    protected FilenameFormatter filenameFormatter;

    protected int currentMaxIndex = ExperimentDirectory.NO_INDEX;

    //loads existing project-space from reader and uses given writer
    protected SiriusProjectSpace(@NotNull final File rootPath, @NotNull final FilenameFormatter filenameFormatter, MetaDataSerializer... metaDataSerializers) throws IOException {
        rootPath.mkdirs();

        if (!rootPath.canWrite())
            throw new IOException("No writing permission for Project-Space makePath: " + rootPath.getAbsolutePath());
        if (!rootPath.isDirectory())
            throw new IOException("Project-Space path is not a Directory but has to be: " + rootPath.getAbsolutePath());

        this.filenameFormatter = filenameFormatter;
        this.reader = new DirectoryReader(new SiriusZipFileReader(rootPath), metaDataSerializers);
        this.writer = new DirectoryWriter(new SiriusFileWriter(rootPath), metaDataSerializers);
        summaryWriters.addAll(initBasicSummaries());
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

            experimentIDs.add(expDir);
            currentMaxIndex = Math.max(currentMaxIndex, expDir.getIndex());
        });

        //write .index to convert the project space automatically to ne new format
        experimentIDs.stream().filter(ExperimentDirectory::isRewrite).forEach(expDir -> {
            try {
                LOG.info("Writing " + expDir.getDirectoryName() + "to update your project-space to the current version");
                if (expDir.hasNoIndex())
                    expDir.setIndex(++currentMaxIndex);
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
    @Override
    public ExperimentResult parseExperiment(ExperimentDirectory id) throws IOException {
        return reader.parseExperiment(id);
    }

    @Override
    public void deleteExperiment(ExperimentDirectory id) throws IOException {
        if (!experimentIDs.contains(id))
            throw new IllegalArgumentException("The project-space does not contain the given ID: " + id.getDirectoryName());

        writer.deleteExperiment(id);
        experimentIDs.remove(id);
    }

    @Override
    public void writeExperiment(final @NotNull ExperimentResult result) throws IOException {
        final ExperimentDirectory expDir = result.computeAnnotationIfAbsent(ExperimentDirectory.class, () -> createID(result));
        final String nuName = filenameFormatter.formatName(result, result.getAnnotation(ExperimentDirectory.class).getIndex());

        if (!nuName.equals(expDir.getDirectoryName())) { //rewrite with nu name and delete old
            ExperimentDirectory deleteKey = new ExperimentDirectory(expDir.getDirectoryName());
            expDir.setDirectoryName(nuName);
            writer.writeExperiment(result);
            deleteExperiment(deleteKey);
        } else { //override old
            writer.writeExperiment(result);
        }
    }

    @Override
    public void writeSummaries() {
        for (SummaryWriter summaryWriter : summaryWriters) {
            summaryWriter.writeSummary(experimentResults(), writer);
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
        writeSummaries();
        reader.close();
        writer.close();
    }

    @NotNull
    public Iterable<ExperimentResult> experimentResults() {
        return ExperimentIterator::new;
    }

    @NotNull
    @Override
    public Iterator<ExperimentDirectory> iterator() {
        return new IdIterator();
    }

    //internal classes
    public class IdIterator implements Iterator<ExperimentDirectory> {
        private final Iterator<ExperimentDirectory> baseIter = experimentIDs.iterator();
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
        private final Iterator<ExperimentDirectory> baseIter = experimentIDs.iterator();
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
