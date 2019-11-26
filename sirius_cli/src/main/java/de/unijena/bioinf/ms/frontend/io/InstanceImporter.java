package de.unijena.bioinf.ms.frontend.io;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.ms.frontend.io.projectspace.Instance;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class InstanceImporter {
    protected static final Logger LOG = LoggerFactory.getLogger(InstanceImporter.class);
    private final ProjectSpaceManager importTarget;

    private double maxMzToImport = Double.MAX_VALUE;
    private boolean ignoreFormulaInMs = false;


    public InstanceImporter(ProjectSpaceManager importTarget, double maxMzToImport, boolean ignoreFormula) {
        this.importTarget = importTarget;
        this.maxMzToImport = maxMzToImport;
        this.ignoreFormulaInMs = ignoreFormula;
    }

    public ImportInstancesJJob makeImportFromFilesJJob(@NotNull final List<File> files) {
        return makeImportJJob(expandInputFromFile(files));
    }

    public ImportInstancesJJob makeImportFromPathsJJob(@NotNull final List<Path> files) {
        return makeImportJJob(expandInput(files));
    }


    public ImportInstancesJJob makeImportJJob(@NotNull InputFiles files) {
        return new ImportInstancesJJob(files);
    }

    public class ImportInstancesJJob extends BasicJJob<Boolean> {
        private InputFiles inputFiles;
        private int max, current;


        public ImportInstancesJJob(InputFiles inputFiles) {
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


        public void importMultipleSources(@Nullable final InputFiles input) {
            if (input == null)
                return;
            importMsParserInput(input.msParserfiles);
            importProjectsInput(input.projects);
        }


        public void importMsParserInput(@Nullable List<Path> files) {
            if (files == null || files.isEmpty())
                return;

            final InstanceIteratorMS2Exp it = new MS2ExpInputIterator(files, maxMzToImport, ignoreFormulaInMs).asInstanceIterator(importTarget);

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
            if (file.equals(importTarget.projectSpace().getLocation())) {
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
            Iterator<CompoundContainerId> psIter = inputSpace.filteredIterator((cid) -> cid.getIonMass().orElse(0d) <= maxMzToImport);
            while (psIter.hasNext()) {
                final CompoundContainerId sourceId = psIter.next();
                if (importTarget.compoundFilter.test(sourceId)) {
                    final CompoundContainer sourceComp = inputSpace.getCompound(sourceId, importTarget.projectSpace().getRegisteredCompoundComponents());

                    // create compound
                    @NotNull Instance inst = importTarget.newCompoundWithUniqueId(sourceComp.getAnnotationOrThrow(Ms2Experiment.class));
                    inst.getID().setAllNonFinal(sourceId);
                    inst.updateCompoundID();

                    // add compund annotations
                    final CompoundContainer targetConf = inst.loadCompoundContainer();
                    targetConf.addAnnotationsFrom(sourceComp);

                    // create Results if available and add them to compoundContainer
                    for (FormulaResultId sourceRid : sourceComp.getResults().values()) {
                        final FormulaResult sourceRes = inputSpace.getFormulaResult(sourceRid, importTarget.projectSpace().getRegisteredFormulaResultComponents());
                        inst.newFormulaResultWithUniqueId(sourceRes.getAnnotationOrThrow(FTree.class)).
                                ifPresent(tres -> {
                                    tres.setAnnotationsFrom(sourceRes);
                                    inst.updateFormulaResult(tres, tres.annotations().getKeysArray());
                                });
                    }

                    inst.updateCompound(targetConf, Arrays.stream(targetConf.annotations().getKeysArray()).filter((it) -> !Ms2Experiment.class.equals(it)).toArray(Class[]::new));
                }
            }
        }
    }


    //expanding input files
    public static InputFiles expandInputFromFile(@NotNull final List<File> files) {
        return SiriusJobs.getGlobalJobManager().submitJob(makeExpandFilesJJob(files)).takeResult();
    }

    public static InputFiles expandInput(@NotNull final List<Path> files) {
        return SiriusJobs.getGlobalJobManager().submitJob(makeExpandPathsJJob(files)).takeResult();
    }

    public static InputExpanderJJob makeExpandFilesJJob(@NotNull final List<File> files) {
        return makeExpandPathsJJob(files.stream().map(File::toPath).collect(Collectors.toList()));
    }

    public static InputExpanderJJob makeExpandPathsJJob(@NotNull final List<Path> files) {
        return new InputExpanderJJob(files);
    }

    public static class InputExpanderJJob extends BasicJJob<InputFiles> {

        private final List<Path> input;

        public InputExpanderJJob(List<Path> input) {
            super(JobType.TINY_BACKGROUND);
            this.input = input;
        }


        @Override
        protected InputFiles compute() throws Exception {
            final InputFiles expandedFiles = new InputFiles();
            return expandInput(input, expandedFiles);
        }

        private InputFiles expandInput(@NotNull final List<Path> files, @NotNull final InputFiles inputFiles) {
            int p = 0;
            updateProgress(0, files.size(), p, "Expanding Input Files...");
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

            updateProgress(0, files.size(), files.size(), "...Input Files expanded!");

            return inputFiles;
        }
    }

}
