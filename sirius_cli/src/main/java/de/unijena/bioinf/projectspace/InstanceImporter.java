package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.projectspace.sirius.SiriusLocations;
import de.unijena.bioinf.projectspace.summaries.SummaryLocations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InstanceImporter {
    protected static final Logger LOG = LoggerFactory.getLogger(InstanceImporter.class);
    private final ProjectSpaceManager importTarget;
    private final Predicate<Ms2Experiment> expFilter;
    private final Predicate<CompoundContainerId> cidFilter;
    private final boolean move; //try to move instead of copy the data where possible


    public InstanceImporter(ProjectSpaceManager importTarget, Predicate<Ms2Experiment> expFilter, Predicate<CompoundContainerId> cidFilter, boolean move) {
        this.importTarget = importTarget;
        this.expFilter = expFilter;
        this.cidFilter = cidFilter;
        this.move = move;
    }

    public InstanceImporter(ProjectSpaceManager importTarget, Predicate<Ms2Experiment> expFilter, Predicate<CompoundContainerId> cidFilter) {
        this(importTarget, expFilter, cidFilter, false);
    }

    public ImportInstancesJJob makeImportJJob(@NotNull InputFilesOptions files) {
        return new ImportInstancesJJob(files);
    }

    public void doImport(InputFilesOptions projectInput) throws ExecutionException {
        if (projectInput == null)
            return;
        SiriusJobs.getGlobalJobManager().submitJob(makeImportJJob(projectInput)).awaitResult();
    }

    public class ImportInstancesJJob extends BasicJJob<List<CompoundContainerId>> {
        private InputFilesOptions inputFiles;


        private JobProgress prog;


        public ImportInstancesJJob(InputFilesOptions inputFiles) {
            super(JobType.TINY_BACKGROUND);
            this.inputFiles = inputFiles;
        }

        @Override
        protected List<CompoundContainerId> compute() throws Exception {
            prog = new JobProgress();
            List<CompoundContainerId> l = importMultipleSources(inputFiles);
            updateProgress(0, 100, 99);
            return l;
        }


        public List<CompoundContainerId> importMultipleSources(@Nullable final InputFilesOptions input) {
            List<CompoundContainerId> list = new ArrayList<>();
            if (input != null) {
                if (input.msInput != null) {
                    list.addAll(importMsParserInput(input.msInput.msParserfiles));
                    list.addAll(importProjectsInput(input.msInput.projects));
                }
                list.addAll(importCSVInput(input.csvInputs));
            }
            return list;

        }

        public List<CompoundContainerId> importCSVInput(List<InputFilesOptions.CsvInput> csvInputs) {
            if (csvInputs == null || csvInputs.isEmpty())
                return List.of();

            final InstanceImportIteratorMS2Exp it = new CsvMS2ExpIterator(csvInputs, expFilter).asInstanceIterator(importTarget, (c) -> cidFilter.test(c.getId()));
            final List<CompoundContainerId> ll = new ArrayList<>();

            while (it.hasNext()) {
                ll.add(it.next().getID());
                prog.accept();
            }
            prog.updateStats();
            return ll;
        }

        public List<CompoundContainerId> importMsParserInput(@Nullable List<Path> files) {
            if (files == null || files.isEmpty())
                return List.of();

            final InstanceImportIteratorMS2Exp it = new MS2ExpInputIterator(files, expFilter, inputFiles.msInput.ignoreFormula).asInstanceIterator(importTarget, (c) -> cidFilter.test(c.getId()));
            final List<CompoundContainerId> ll = new ArrayList<>();

            while (it.hasNext()) {
                ll.add(it.next().getID());
                prog.accept();
            }
            prog.updateStats();
            return ll;
        }


        public List<CompoundContainerId> importProjectsInput(@Nullable List<Path> files) {
            if (files == null || files.isEmpty())
                return List.of();

            List<CompoundContainerId> ll = new ArrayList<>();
            for (Path f : files) {
                try {
                    ll.addAll(importProject(f));
                } catch (IOException e) {
                    LOG.error("Could not Unpack archived Project `" + f.toString() + "'. Skipping this location!", e);
                }
            }
            return ll;
        }

        public List<CompoundContainerId> importProject(@NotNull Path file) throws IOException {
            if (file.toAbsolutePath().equals(importTarget.projectSpace().getLocation().toAbsolutePath())) {
                LOG.warn("target location '" + importTarget.projectSpace().getLocation() + "' was also part of the INPUT and will be ignored!");
                return List.of();
            }

            List<CompoundContainerId> l;
            try (final SiriusProjectSpace ps = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).openExistingProjectSpace(file)) {
                l = InstanceImporter.importProject(ps, importTarget, expFilter, cidFilter, move, prog);
            }
            if (move)
                FileUtils.deleteRecursively(file);
            return l;
        }

        class JobProgress implements Progress {
            private final int numOfFiles = (int) inputFiles.getAllFilesStream().count();
            private int numberOfCompounds = 0;
            private IntSummaryStatistics compoundStats = new IntSummaryStatistics();
            private int currentCount = 0;


            public void updateStats(int toAdd) {
                currentCount = toAdd;
                if (currentCount > 0)
                    updateStats();
            }

            public void updateStats() {
                compoundStats.accept(currentCount);
                currentCount = 0;
            }

            public void accept(Integer inc) {
                currentCount += inc;
                numberOfCompounds += inc;

                int max = (int) Math.ceil((currentCount * numOfFiles + 1) * 1.1d);
                if (compoundStats.getCount() > 0) {
                    max = Math.max((int) Math.ceil((compoundStats.getAverage() * numOfFiles + 1) * 1.1d), max);
                }
                updateProgress(0, max, numberOfCompounds);
            }
        }
    }

    @FunctionalInterface
    interface Progress {
        default void updateStats(int toAdd) {
            updateStats();
        }

        default void updateStats() {
        }

        default void accept() {
            accept(1);
        }

        void accept(Integer inc);
    }


    public static List<CompoundContainerId> importProject(@NotNull SiriusProjectSpace inputSpace, @NotNull ProjectSpaceManager importTarget, @NotNull Predicate<Ms2Experiment> expFilter, @NotNull Predicate<CompoundContainerId> cidFilter, boolean move) throws IOException {
        return importProject(inputSpace, importTarget, expFilter, cidFilter, move, (i) -> {
        });
    }

    public static List<CompoundContainerId> importProject(@NotNull SiriusProjectSpace inputSpace, @NotNull ProjectSpaceManager importTarget, @NotNull Predicate<Ms2Experiment> expFilter, @NotNull Predicate<CompoundContainerId> cidFilter, boolean move, @NotNull Progress prog) throws IOException {
        List<Path> globalFiles = FileUtils.listAndClose(inputSpace.getRootPath(), l -> l.filter(Files::isRegularFile).filter(p ->
                !p.getFileName().toString().equals(FilenameFormatter.PSPropertySerializer.FILENAME) &&
                        !p.getFileName().toString().equals(SummaryLocations.COMPOUND_SUMMARY_ADDUCTS) &&
                        !p.getFileName().toString().equals(SummaryLocations.COMPOUND_SUMMARY) &&
                        !p.getFileName().toString().equals(SummaryLocations.FORMULA_SUMMARY) &&
                        !p.getFileName().toString().equals(SummaryLocations.MZTAB_SUMMARY)
        ).collect(Collectors.toList()));

        for (Path s : globalFiles) {
            final Path t = importTarget.projectSpace().getRootPath().resolve(s.getFileName().toString());
            try {
                if (Files.notExists(t))
                    Files.copy(s, t); // no copying here because this may be needed multiple times
            } catch (IOException e) {
                LOG.error("Could not Copy `" + s.toString() + "` to new location `" + t.toString() + "` Project might be corrupted!", e);
            }
        }


        final Iterator<CompoundContainerId> psIter = inputSpace.filteredIterator(cidFilter);/*, expFilter*/
        final List<CompoundContainerId> imported = new ArrayList<>(inputSpace.size());
        prog.updateStats(inputSpace.size());

        while (psIter.hasNext()) {
            final CompoundContainerId sourceId = psIter.next();
            // create compound
            CompoundContainerId id = importTarget.projectSpace().newUniqueCompoundId(sourceId.getCompoundName(), (idx) -> importTarget.namingScheme.apply(idx, sourceId.getCompoundName())).orElseThrow();
            id.setAllNonFinal(sourceId);
            importTarget.projectSpace().updateCompoundContainerID(id);

            final List<Path> files = FileUtils.listAndClose(inputSpace.getRootPath().resolve(sourceId.getDirectoryName()), l ->
                    l.filter(p -> !p.getFileName().toString().equals(SiriusLocations.COMPOUND_INFO)).collect(Collectors.toList()));
            for (Path s : files) {
                final Path t = importTarget.projectSpace().getRootPath().resolve(id.getDirectoryName()).resolve(s.getFileName().toString());
                try {
                    Files.createDirectories(t);
                    if (move)
                        FileUtils.moveFolder(s, t);
                    else
                        FileUtils.copyFolder(s, t);
                } catch (IOException e) {
                    LOG.error("Could not Copy instance `" + id.getDirectoryName() + "` to new location `" + t.toString() + "` Results might be missing!", e);
                }
            }
            imported.add(id);
            prog.accept();
            importTarget.projectSpace().fireCompoundCreated(id);

            if (move)
                inputSpace.deleteCompound(sourceId);
        }
        prog.updateStats(0);
        return imported;
    }

    //expanding input files
    public static InputFilesOptions.MsInput expandInputFromFile(@NotNull final List<File> files) {
        return expandInputFromFile(files, new InputFilesOptions.MsInput());
    }

    public static InputFilesOptions.MsInput expandInputFromFile(@NotNull final List<File> files, @NotNull final InputFilesOptions.MsInput expandTo) {
        return SiriusJobs.getGlobalJobManager().submitJob(makeExpandFilesJJob(files, expandTo)).takeResult();
    }

    public static InputFilesOptions.MsInput expandInput(@NotNull final List<Path> files) {
        return expandInput(files, new InputFilesOptions.MsInput());
    }

    public static InputFilesOptions.MsInput expandInput(@NotNull final List<Path> files, @NotNull final InputFilesOptions.MsInput expandTo) {
        return SiriusJobs.getGlobalJobManager().submitJob(makeExpandPathsJJob(files, expandTo)).takeResult();
    }

    public static InputExpanderJJob makeExpandFilesJJob(@NotNull final List<File> files) {
        return makeExpandFilesJJob(files, new InputFilesOptions.MsInput());
    }
    public static InputExpanderJJob makeExpandFilesJJob(@NotNull final List<File> files, @NotNull final InputFilesOptions.MsInput expandTo) {
        return makeExpandPathsJJob(files.stream().map(File::toPath).collect(Collectors.toList()), expandTo);
    }

    public static InputExpanderJJob makeExpandPathsJJob(@NotNull final List<Path> files, @NotNull final InputFilesOptions.MsInput expandTo) {
        return new InputExpanderJJob(files, expandTo);
    }

    public static class InputExpanderJJob extends BasicJJob<InputFilesOptions.MsInput> {

        private final List<Path> input;
        //        private final AtomicInteger progress = new AtomicInteger(0);
        private final InputFilesOptions.MsInput expandedFiles;

        public InputExpanderJJob(List<Path> input, InputFilesOptions.MsInput expandTo) {
            super(JobType.TINY_BACKGROUND);
            this.input = input;
            this.expandedFiles = expandTo;
        }


        @Override
        protected InputFilesOptions.MsInput compute() throws Exception {
//            final  = new InputFiles();
            if (input != null && !input.isEmpty()) {
                updateProgress(0, input.size(), 0, "Expanding Input Files: '" + input.stream().map(Path::toString).collect(Collectors.joining(",")) + "'...");
                expandInput(input, expandedFiles);
                updateProgress(0, input.size(), input.size(), "...Input Files successfully expanded!");
            }
            return expandedFiles;
        }

        private void expandInput(@NotNull final List<Path> files, @NotNull final InputFilesOptions.MsInput inputFiles) {
            int p = 0;
//            updateProgress(0, files.size(), p, "Expanding Input Files...");
            for (Path g : files) {
                if (!Files.exists(g)) {
                    LOG.warn("Path \"" + g.toString() + "\" does not exist and will be skipped");
                    continue;
                }

                if (Files.isDirectory(g)) {
                    // check whether it is a workspace or a gerneric directory with some other input
                    if (ProjectSpaceIO.isExistingProjectspaceDirectory(g)) {
                        inputFiles.projects.add(g);
                    } else {
                        try {
                            final List<Path> ins =
                                    FileUtils.listAndClose(g, l -> l.filter(Files::isRegularFile).sorted().collect(Collectors.toList()));

                            if (ins.contains(Path.of(FilenameFormatter.PSPropertySerializer.FILENAME)))
                                throw new IOException("Unreadable project found!");

                            if (!ins.isEmpty())
                                expandInput(ins, inputFiles);
                        } catch (IOException e) {
                            LOG.warn("Could not list directory content of '" + g.toString() + "'. Skipping location!");
                        }
                    }
                } else {
                    //check whether files are lcms runs copressed project-spaces or standard ms/mgf files
                    final String name = g.getFileName().toString();
                    if (ProjectSpaceIO.isZipProjectSpace(g)) {
                        //compressed spaces are read only and can be handled as simple input
                        inputFiles.projects.add(g);
                    } else if (MsExperimentParser.isSupportedFileName(name)) {
                        inputFiles.msParserfiles.add(g);
                    } else {
                        inputFiles.unknownFiles.add(g);
//                    LOG.warn("File with the name \"" + name + "\" is not in a supported format or has a wrong file extension. File is skipped");
                    }
                }
                updateProgress(0, files.size(), ++p);
            }
//            return inputFiles;
        }
    }

}
