package de.unijena.bioinf.ms.frontend.io;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.ms.frontend.io.projectspace.Instance;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectSpaceIO;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InstanceImporter {
    protected static final Logger LOG = LoggerFactory.getLogger(InstanceImporter.class);
    private final ProjectSpaceManager importTarget;

    private double maxMzToImport = Double.MAX_VALUE;
    private boolean ignoreFormulaInMs = false;


    public InstanceImporter(ProjectSpaceManager importTarget) {
        this.importTarget = importTarget;
    }

    public void importMultipleSources(@Nullable final List<Path> files) {
        if (files == null)
            return;
        importMultipleSources(expandInput(files));
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
        new MS2ExpInputIterator(files, maxMzToImport, ignoreFormulaInMs).asInstanceIterator(importTarget).importAll();
    }

    public void importProjectsInput(@Nullable List<Path> files) {
        if (files == null || files.isEmpty())
            return;
        files.forEach(f -> {
            try {
                importProject(f);
            } catch (IOException e) {
                LOG.error("Could not Unpack archived Project `" + f.toString() + "'. Skipping this location!", e);
            }
        });
    }

    public void importProject(@NotNull Path file) throws IOException {
        if (FileSystems.getDefault().equals(importTarget.projectSpace().getRootPath().getFileSystem())) {
            if (file.equals(importTarget.projectSpace().getRootPath())) {
                LOG.warn("target location '" + importTarget.projectSpace().getRootPath().toString() + "' was also part of the INPUT and will be ignored!");
                return;
            }
        } else {
            if (file.equals(Path.of(importTarget.projectSpace().getRootPath().getFileSystem().toString()))) {
                LOG.warn("target location '" + importTarget.projectSpace().getRootPath().getFileSystem().toString() + "' was also part of the INPUT and will be ignored!");
                return;
            }
        }

        final SiriusProjectSpace ps = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).openExistingProjectSpace(file);
        importProject(ps);
    }

    public void importProject(SiriusProjectSpace inputSpace) throws IOException {
        for (CompoundContainerId sourceId : inputSpace) {
            if (importTarget.compoundFilter.test(sourceId)) {
                final CompoundContainer sourceComp = inputSpace.getCompound(sourceId, importTarget.projectSpace().getRegisteredCompoundComponents());

                // create compound
                @NotNull Instance inst = importTarget.newCompoundWithUniqueId(sourceComp.getAnnotationOrThrow(Ms2Experiment.class));
                inst.getID().setAllNonFinal(sourceId);
                inst.updateCompoundID();

                final CompoundContainer targetConf = inst.loadCompoundContainer();
                targetConf.addAnnotationsFrom(sourceComp);
                inst.updateCompound(targetConf, Arrays.stream(targetConf.annotations().getKeysArray()).filter((it) -> !Ms2Experiment.class.equals(it)).toArray(Class[]::new));


                // create Results if available
                for (FormulaResultId sourceRid : sourceComp.getResults().values()) {
                    final FormulaResult sourceRes = inputSpace.getFormulaResult(sourceRid, importTarget.projectSpace().getRegisteredFormulaResultComponents());
                    inst.newFormulaResultWithUniqueId(sourceRes.getAnnotationOrThrow(FTree.class)).
                            ifPresent(tres -> {
                                tres.addAnnotationsFrom(sourceRes);
                                inst.updateFormulaResult(tres, tres.annotations().getKeysArray());
                            });
                }
            }
        }
    }


    public static InputFiles expandInputFromFile(@NotNull final List<File> files) {
        return expandInput(files.stream().map(File::toPath).collect(Collectors.toList()));
    }

    public static InputFiles expandInput(@NotNull final List<Path> files) {
        final InputFiles expandedFiles = new InputFiles();
        return expandInput(files, expandedFiles);
    }

    private static InputFiles expandInput(@NotNull final List<Path> files, @NotNull final InputFiles inputFiles) {
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
                        if (!ins.isEmpty())
                            expandInput(ins, inputFiles);
                    } catch (IOException e) {
                        LOG.warn("Could not list directory content of '" + g.toString() + "'. Skipping location!");
                    }

                }
            } else {
                //check whether files are lcms runs copressed project-spaces or standard ms/mgf files
                final String name = g.getFileName().toString();
                if (MsExperimentParser.isSupportedFileName(name)) {
                    inputFiles.msParserfiles.add(g);
                } else if (ProjectSpaceIO.isZipProjectSpace(g)) {
                    //compressed spaces are read only and can be handled as simple input
                    inputFiles.projects.add(g);
                } else {
                    inputFiles.unknownFiles.add(g);
//                    LOG.warn("File with the name \"" + name + "\" is not in a supported format or has a wrong file extension. File is skipped");
                }
            }
        }
        return inputFiles;
    }

}
