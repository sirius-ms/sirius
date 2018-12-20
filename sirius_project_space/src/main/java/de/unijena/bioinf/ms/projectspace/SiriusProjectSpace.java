package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.ExperimentResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/*
 * This is the Sirius Project space. It operates only on uncompressed
 * directory based projects. For zip based projects: They should be
 * decompressed in some temp dir and copied/compressed back when all tasks are done (close method).
 *
 * */
public class SiriusProjectSpace implements ProjectSpace {
    protected static final Logger LOG = LoggerFactory.getLogger(SiriusProjectSpace.class);

    protected final LinkedHashSet<ExperimentDirectory> experimentIDs = new LinkedHashSet<>();
    protected final List<SummaryWriter> summaryWriters = new ArrayList<>();

    protected DirectoryWriter writer;
    protected DirectoryReader reader;
    protected FilenameFormatter filenameFormatter;

    protected int currentMaxIndex = ExperimentDirectory.NO_INDEX;

    //loads existing project-space from reader and uses given writer
    protected SiriusProjectSpace(@NotNull final String rootPath, @NotNull final FilenameFormatter filenameFormatter, MetaDataSerializer... metaDataSerializers) throws IOException {
        this.filenameFormatter = filenameFormatter;
        this.reader = new DirectoryReader(new SiriusZipFileReader(new File(rootPath)), metaDataSerializers);
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

    private ExperimentDirectory createID(@NotNull ExperimentResult result) {
        final int index = ++currentMaxIndex;
        final ExperimentDirectory dir = new ExperimentDirectory(filenameFormatter.formatName(result, index));
        dir.setIndex(index);
        return dir;
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
