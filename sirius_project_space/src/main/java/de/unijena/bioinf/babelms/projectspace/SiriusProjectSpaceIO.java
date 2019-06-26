package de.unijena.bioinf.babelms.projectspace;

import de.unijena.bioinf.babelms.MultiSourceIterator;
import de.unijena.bioinf.babelms.SiriusInputIterator;
import de.unijena.bioinf.sirius.ExperimentResult;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class SiriusProjectSpaceIO {
    protected static final Logger LOG = LoggerFactory.getLogger(SiriusProjectSpace.class);


    //region create Project Spaces
    public static @NotNull SiriusProjectSpace create(@Nullable FilenameFormatter filenameFormatter, @NotNull final File projectSpaceRoot, MetaDataSerializer... metaDataSerializers) throws IOException {
        return create(filenameFormatter, projectSpaceRoot, (currentProgress, maxProgress, Message) -> {
        }, metaDataSerializers);
    }

    public static @NotNull SiriusProjectSpace create(@Nullable FilenameFormatter filenameFormatter, @NotNull final File projectSpaceRoot, @NotNull ProgressListener progress, MetaDataSerializer... metaDataSerializers) throws IOException {
        SiriusProjectSpace space = new SiriusProjectSpace(projectSpaceRoot, filenameFormatter, metaDataSerializers);
        space.loadIntoProjectSpace(progress);
        return space;
    }

    public static SiriusProjectSpace create(@NotNull final File rootOutPath, @NotNull final Collection<File> rootInputPaths, @Nullable final FilenameFormatter filenameFormatter, MetaDataSerializer... metaDataSerializers) throws IOException {
        return create(rootOutPath, rootInputPaths, filenameFormatter, (currentProgress, maxProgress, Message) -> {
        }, metaDataSerializers);
    }

    public static SiriusProjectSpace create(@NotNull final File rootOutPath, @NotNull Collection<File> rootInputPaths, @Nullable final FilenameFormatter filenameFormatter, @NotNull ProgressListener progress, MetaDataSerializer... metaDataSerializers) throws IOException {
        final SiriusProjectSpace merged = new SiriusProjectSpace(rootOutPath, filenameFormatter, metaDataSerializers);
        final TIntSet ids = new TIntHashSet();
        merged.loadIntoProjectSpace(merged.reader, ids, false, progress);//todo correct progress stack
        final String absPath = rootOutPath.getAbsolutePath();
        rootInputPaths = rootInputPaths.stream().filter(p -> !p.getAbsolutePath().equals(absPath)).collect(Collectors.toSet());
        merged.load(progress, ids, rootInputPaths);
        return merged;
    }


    public static @NotNull SiriusProjectSpace create(@Nullable Iterable<ExperimentResult> toInsert, @Nullable FilenameFormatter filenameFormatter, @NotNull final File projectSpaceRoot, MetaDataSerializer... metaDataSerializers) throws IOException {
        return create(toInsert, filenameFormatter, projectSpaceRoot, (currentProgress, maxProgress, Message) -> {
        }, metaDataSerializers);
    }

    public static @NotNull SiriusProjectSpace create(@Nullable Iterable<ExperimentResult> toInsert, @Nullable FilenameFormatter filenameFormatter, @NotNull final File projectSpaceRoot, @NotNull ProgressListener progress, MetaDataSerializer... metaDataSerializers) throws IOException {
        SiriusProjectSpace space = new SiriusProjectSpace(projectSpaceRoot, filenameFormatter, metaDataSerializers);
        space.loadIntoProjectSpace(progress);
        if (toInsert != null)
            writeExperimentsToProjectSapce(toInsert.iterator(), space);
        return space;
    }
    //endregion


    //region Read/Write
    public static Iterator<ExperimentResult> readInputAndProjectSpace(@NotNull Collection<File> siriusInput, SiriusProjectSpace space, double maxMz, boolean ignoreFormula) {
        return readInputAndProjectSpace(siriusInput, Collections.singleton(space), maxMz, ignoreFormula);
    }

    public static Iterator<ExperimentResult> readInputAndProjectSpace(@NotNull Collection<File> siriusInput, Collection<SiriusProjectSpace> spaces, double maxMz, boolean ignoreFormula) {
        final List<Iterator<ExperimentResult>> all = spaces.stream().map(SiriusProjectSpace::parseExperimentIterator).collect(Collectors.toList());
        all.add(0, new SiriusInputIterator(siriusInput, maxMz, ignoreFormula).asExpResultIterator());
        return new MultiSourceIterator(all);
    }

    public static void writeInputToProjectSapce(@NotNull Collection<File> siriusInput, final @NotNull SiriusProjectSpace space, double maxMz, boolean ignoreFormula) {
        Iterator<ExperimentResult> inputIter = new SiriusInputIterator(siriusInput, maxMz, ignoreFormula).asExpResultIterator();
        writeExperimentsToProjectSapce(inputIter, space);
    }

    public static void writeExperimentsToProjectSapce(final @NotNull Iterator<ExperimentResult> toInsert, final @NotNull SiriusProjectSpace space) {
        toInsert.forEachRemaining(er -> {
            try {
                space.writeExperiment(er);
            } catch (IOException e) {
                LOG.error("Could not write Experiment with name: " + er.getExperiment().getName() +
                        ". Experiment is not saved in project-space", e);
            }
        });
    }


    //region Helper Methods
    public static void exortToZip(SiriusProjectSpace projectSpace, File zipFile) throws IOException {
        if (!isCompressedProjectSpaceName(zipFile.getName()))
            throw new IllegalArgumentException("Filename needs to be a valid zipped ProjectSpace output.");
        if (zipFile.exists()) {
            if (zipFile.isFile())
                zipFile.delete();
            else throw new IllegalArgumentException("Output path is not a file!");
        }

        projectSpace.copyToZip(zipFile);
    }

    /**
     * Check for a compressed project-space by file ending
     */
    public static boolean isCompressedProjectSpaceName(String fileName) {
        final String lowercaseName = fileName.toLowerCase();
        return lowercaseName.endsWith(".workspace") || lowercaseName.endsWith(".zip") || lowercaseName.endsWith(".sirius");
    }

    /**
     * Just a quick check to discriminate a project-space for an arbitrary folder
     */
    public static boolean isSiriusWorkspaceDirectory(File f) {
        final File fv = new File(f, "version.txt");
        if (!fv.exists()) return false;
        try (final BufferedReader br = new BufferedReader(new FileReader(fv), 512)) {
            String line = br.readLine();
            if (line == null) return false;
            line = line.toUpperCase();
            if (line.startsWith("SIRIUS")) return true;
            else return false;
        } catch (IOException e) {
            // not critical: if file cannot be read, it is not a valid workspace
            LOG.error(e.getMessage(), e);
            return false;
        }
    }
    //endregion
}
