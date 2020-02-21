package de.unijena.bioinf.ms.frontend.io;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.ms.frontend.io.projectspace.Instance;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.FilenameFormatter;
import de.unijena.bioinf.projectspace.ProjectSpaceIO;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.SiriusLocations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class InstanceImporter {
    protected static final Logger LOG = LoggerFactory.getLogger(InstanceImporter.class);
    private final ProjectSpaceManager importTarget;
    private final double maxMz;

    public InstanceImporter(ProjectSpaceManager importTarget, double maxMzToImport/*, boolean ignoreFormula*/) {
        this.importTarget = importTarget;
        this.maxMz = maxMzToImport;
    }

    public ImportInstancesJJob makeImportJJob(@NotNull InputFilesOptions files) {
        return new ImportInstancesJJob(files);
    }

    public class ImportInstancesJJob extends BasicJJob<Boolean> {
        private InputFilesOptions inputFiles;
        private int max, current;


        public ImportInstancesJJob(InputFilesOptions inputFiles) {
            super(JobType.TINY_BACKGROUND);
            this.inputFiles = inputFiles;
        }

        @Override
        protected Boolean compute() throws Exception {
            max = (int) inputFiles.getAllFilesStream().count();
            current = 0;

            importMultipleSources(inputFiles);
            return true;
        }


        public void importMultipleSources(@Nullable final InputFilesOptions input) {
            if (input == null)
                return;
            if (input.msInput != null) {
                importMsParserInput(input.msInput.msParserfiles);
                importProjectsInput(input.msInput.projects);
            }
            importCSVInput(input.csvInputs);
        }

        private void importCSVInput(List<InputFilesOptions.CsvInput> csvInputs) {
            if (csvInputs == null || csvInputs.isEmpty())
                return;
            final InstanceIteratorMS2Exp it = new CsvMS2ExpIterator(csvInputs, maxMz).asInstanceIterator(importTarget);

            long count = 0;
            while (it.hasNext()) {
                it.next();
                if (count++ > csvInputs.size())
                    max++;
                updateProgress(0, max, ++current);
            }
        }

        public void importMsParserInput(@Nullable List<Path> files) {
            if (files == null || files.isEmpty())
                return;

            final InstanceIteratorMS2Exp it = new MS2ExpInputIterator(files, maxMz, inputFiles.msInput.ignoreFormula).asInstanceIterator(importTarget);

            long count = 0;
            while (it.hasNext()) {
                it.next();
                if (count++ > files.size())
                    max++;
                updateProgress(0, max, ++current);
            }
        }

        public void importProjectsInput(@Nullable List<Path> files) {
            if (files == null || files.isEmpty())
                return;
            files.forEach(f -> {
                try {
                    importProject(f);
                    updateProgress(0, max, ++current);
                } catch (IOException e) {
                    LOG.error("Could not Unpack archived Project `" + f.toString() + "'. Skipping this location!", e);
                }
            });
        }

        public void importProject(@NotNull Path file) throws IOException {
            if (file.toAbsolutePath().equals(importTarget.projectSpace().getLocation().toAbsolutePath())) {
                LOG.warn("target location '" + importTarget.projectSpace().getLocation() + "' was also part of the INPUT and will be ignored!");
                return;
            }

            try (final SiriusProjectSpace ps = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).openExistingProjectSpace(file)) {
                importProject(ps);
                // rescale progress to have at least some weighting regarding throu the compounds
                current += ps.size();
                max += ps.size();
            }
        }

        public void importProject(SiriusProjectSpace inputSpace) throws IOException {
            Files.list(inputSpace.getRootPath()).filter(Files::isRegularFile).filter(p -> !p.getFileName().toString().equals(FilenameFormatter.PSPropertySerializer.FILENAME))
                    .forEach(s -> {
                        final Path t = importTarget.projectSpace().getRootPath().resolve(s.getFileName());
                        try {
                            if (Files.notExists(t))
                                Files.copy(s, t);
                        } catch (IOException e) {
                            LOG.error("Could not Copy `" + s.toString() + "` to new location `" + t.toString() + "` Project might be corrupted!", e);
                        }
                    });

            Iterator<CompoundContainerId> psIter = inputSpace.filteredIterator((cid) -> cid.getIonMass().orElse(0d) <= maxMz);
            while (psIter.hasNext()) {
                final CompoundContainerId sourceId = psIter.next();
                if (importTarget.compoundFilter.test(sourceId)) {
                    final CompoundContainer sourceComp = inputSpace.getCompound(sourceId, importTarget.projectSpace().getRegisteredCompoundComponents());

                    // create compound
                    @NotNull Instance inst = importTarget.newCompoundWithUniqueId(sourceComp.getAnnotationOrThrow(Ms2Experiment.class));
                    inst.getID().setAllNonFinal(sourceId);
                    inst.updateCompoundID();

                    Files.list(inputSpace.getRootPath().resolve(sourceId.getDirectoryName()))
                            .filter(p -> !p.getFileName().toString().equals(SiriusLocations.COMPOUND_INFO) && !p.getFileName().toString().equals(SiriusLocations.MS2_EXPERIMENT))
                            .forEach(s -> {
                                final Path t = importTarget.projectSpace().getRootPath().resolve(inst.getID().getDirectoryName()).resolve(s.getFileName());
                                try {
                                    Files.createDirectories(t);
                                    FileUtils.copyFolder(s, t);
                                    inst.reloadCompoundCache(Ms2Experiment.class);
                                } catch (IOException e) {
                                    LOG.error("Could not Copy instance `" + inst.getID().getDirectoryName() + "` to new location `" + t.toString() + "` Results might be missing!", e);
                                }
                            });
                }
            }
        }
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
                            final List<Path> ins = Files.list(g).filter(Files::isRegularFile).sorted().collect(Collectors.toList());
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
