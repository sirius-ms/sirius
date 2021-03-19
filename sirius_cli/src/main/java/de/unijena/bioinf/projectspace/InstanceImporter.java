/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintData;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobProgressMerger;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.canopus.CanopusDataProperty;
import de.unijena.bioinf.projectspace.canopus.CanopusLocations;
import de.unijena.bioinf.projectspace.fingerid.FingerIdDataProperty;
import de.unijena.bioinf.projectspace.fingerid.FingerIdLocations;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.projectspace.sirius.SiriusLocations;
import de.unijena.bioinf.projectspace.summaries.SummaryLocations;
import de.unijena.bioinf.utils.NetUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
    private final boolean updateFingerprintData; //try to move instead of copy the data where possible


    public InstanceImporter(ProjectSpaceManager importTarget, Predicate<Ms2Experiment> expFilter, Predicate<CompoundContainerId> cidFilter, boolean move, boolean updateFingerprintData) {
        this.importTarget = importTarget;
        this.expFilter = expFilter;
        this.cidFilter = cidFilter;
        this.move = move;
        this.updateFingerprintData = updateFingerprintData;
    }

    public InstanceImporter(ProjectSpaceManager importTarget, Predicate<Ms2Experiment> expFilter, Predicate<CompoundContainerId> cidFilter) {
        this(importTarget, expFilter, cidFilter, false, false);
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
        private JobProgressMerger prog;


        public ImportInstancesJJob(InputFilesOptions inputFiles) {
            super(JobType.TINY_BACKGROUND);
            this.inputFiles = inputFiles;
        }

        @Override
        protected List<CompoundContainerId> compute() throws Exception {
            prog = new JobProgressMerger(pcs, this);
            return importMultipleSources(inputFiles);
        }


        public List<CompoundContainerId> importMultipleSources(@Nullable final InputFilesOptions input) {
            List<CompoundContainerId> list = new ArrayList<>();
            if (input != null) {
                if (input.msInput != null) {
                    input.msInput.msParserfiles.forEach((p, n) -> prog.addPreload(p, 0, n));
                    input.msInput.projects.forEach((p, n) -> prog.addPreload(p, 0, n));

                    list.addAll(importMsParserInput(input.msInput.msParserfiles.keySet().stream().sorted().collect(Collectors.toList())));
                    list.addAll(importProjectsInput(input.msInput.projects.keySet().stream().sorted().collect(Collectors.toList())));
                }

                if (input.csvInputs != null) {
                    input.csvInputs.forEach(i -> prog.addPreload(i, 0, i.ms2.size()));
                    list.addAll(importCSVInput(input.csvInputs));
                }
            }
            return list;

        }

        public List<CompoundContainerId> importCSVInput(List<InputFilesOptions.CsvInput> csvInputs) {
            if (csvInputs == null || csvInputs.isEmpty())
                return List.of();

            final InstanceImportIteratorMS2Exp it = new CsvMS2ExpIterator(csvInputs, expFilter, prog).asInstanceIterator(importTarget, (c) -> cidFilter.test(c.getId()));
            final List<CompoundContainerId> ll = new ArrayList<>();

            // no progress
            while (it.hasNext())
                ll.add(it.next().getID());

            return ll;
        }

        public List<CompoundContainerId> importMsParserInput(List<Path> files) {
            if (files == null || files.isEmpty())
                return List.of();

            final InstanceImportIteratorMS2Exp it = new MS2ExpInputIterator(files, expFilter, inputFiles.msInput.isIgnoreFormula(), prog)
                    .asInstanceIterator(importTarget, (c) -> cidFilter.test(c.getId()));
            final List<CompoundContainerId> ll = new ArrayList<>();

            if (prog.isDone())
                prog.indeterminateProgress(); // just to show something in case only one small file
            while (it.hasNext()) {
                CompoundContainerId id = it.next().getID();
                if (prog.isDone())
                    prog.indeterminateProgress(id.getCompoundName());
                else
                    prog.progressMessage(id.getCompoundName());
                ll.add(id);
            }


            return ll;
        }


        public List<CompoundContainerId> importProjectsInput(List<Path> files) {
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
                l = InstanceImporter.importProject(ps, importTarget, cidFilter, move, updateFingerprintData, prog);
            }
            if (move)
                FileUtils.deleteRecursively(file);
            return l;
        }
    }

    public static List<CompoundContainerId> importProject(
            @NotNull SiriusProjectSpace inputSpace, @NotNull ProjectSpaceManager importTarget,
            @NotNull Predicate<CompoundContainerId> cidFilter, boolean move, boolean updateFingerprintVersion) throws IOException {

        return importProject(inputSpace, importTarget, cidFilter, move, updateFingerprintVersion, null);
    }

    // we do not exp level filter here since we want to prevent reading the spectrum file
    // we do file system level copies where we can here
    public static List<CompoundContainerId> importProject(
            @NotNull SiriusProjectSpace inputSpace, @NotNull ProjectSpaceManager importTarget,
            @NotNull Predicate<CompoundContainerId> cidFilter, boolean move, boolean updateFingerprintVersion, @Nullable JobProgressMerger prog) throws IOException {

        final int size = inputSpace.size();
        int progress = 0;

        //check is fingerprint data is compatible and clean if not.
        @Nullable Predicate<String> resultsToSkip = checkDataCompatibility(inputSpace, importTarget, NetUtils.checkThreadInterrupt(Thread.currentThread()));
        final List<CompoundContainerId> imported = new ArrayList<>(inputSpace.size());

        if (resultsToSkip == null || updateFingerprintVersion) {
            List<Path> globalFiles = FileUtils.listAndClose(inputSpace.getRootPath(), l -> l.filter(Files::isRegularFile).filter(p ->
                    !p.getFileName().toString().equals(FilenameFormatter.PSPropertySerializer.FILENAME) &&
                            !p.getFileName().toString().equals(SummaryLocations.COMPOUND_SUMMARY_ADDUCTS) &&
                            !p.getFileName().toString().equals(SummaryLocations.COMPOUND_SUMMARY) &&
                            !p.getFileName().toString().equals(SummaryLocations.FORMULA_SUMMARY) &&
                            !p.getFileName().toString().equals(SummaryLocations.CANOPUS_SUMMARY) &&
                            !p.getFileName().toString().equals(SummaryLocations.MZTAB_SUMMARY) &&
                            (resultsToSkip == null || DATA_FILES_TO_SKIP.test(p.getFileName().toString())) //skip data files if incompatible
            ).collect(Collectors.toList()));

            for (Path s : globalFiles) {
                final Path t = importTarget.projectSpace().getRootPath().resolve(s.getFileName().toString());
                try {
                    if (Files.notExists(t))
                        Files.copy(s, t); // no moving here because this may be needed multiple times
                } catch (IOException e) {
                    LOG.error("Could not Copy `" + s.toString() + "` to new location `" + t.toString() + "` Project might be corrupted!", e);
                }
            }


            final Iterator<CompoundContainerId> psIter = inputSpace.filteredIterator(cidFilter);


            while (psIter.hasNext()) {
                final CompoundContainerId sourceId = psIter.next();
                // create compound
                CompoundContainerId id = importTarget.projectSpace().newUniqueCompoundId(sourceId.getCompoundName(), (idx) -> importTarget.namingScheme.apply(idx, sourceId.getCompoundName())).orElseThrow();
                id.setAllNonFinal(sourceId);
                importTarget.projectSpace().updateCompoundContainerID(id);

                final List<Path> files = FileUtils.listAndClose(inputSpace.getRootPath().resolve(sourceId.getDirectoryName()), l -> l
                        .filter(p -> !p.getFileName().toString().equals(SiriusLocations.COMPOUND_INFO))
                        .filter(it -> resultsToSkip == null || resultsToSkip.test(it.getFileName().toString()))
                        .collect(Collectors.toList()));

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
                if (resultsToSkip != null) {
                    LoggerFactory.getLogger(InstanceImporter.class).info("Updating Compound score of '" + id.toString() + "' after deleting Fingerprint related results...");
                    Instance inst = importTarget.newInstanceFromCompound(id);
                    List<FormulaResult> l = inst.loadFormulaResults(FormulaScoring.class).stream().map(SScored::getCandidate)
                            .filter(r -> r.getAnnotation(FormulaScoring.class).map(s -> (s.removeAnnotation(TopCSIScore.class) != null)
                                    || (s.removeAnnotation(ConfidenceScore.class) != null)).orElse(false))
                            .collect(Collectors.toList());

                    l.forEach(r -> inst.updateFormulaResult(r, FormulaScoring.class));
                    LoggerFactory.getLogger(InstanceImporter.class).info("Updating Compound score of '" + id.toString() + "' DONE!");
                }

                imported.add(id);
                if (prog != null)
                    prog.progressChanged(new JobProgressEvent(inputSpace.getRootPath(), 0, size, ++progress, id.toString()));
                importTarget.projectSpace().fireCompoundCreated(id);

                if (move)
                    inputSpace.deleteCompound(sourceId);
            }
        } else {
            LoggerFactory.getLogger(ProjectSpaceManager.class).warn(
                    "INCOMPATIBLE INPUT: The Fingerprint version of the project location you trying to import '" + inputSpace.getLocation() +
                            "'ist incompatible to the one at the target location '" + importTarget.projectSpace().getLocation() +
                            "'. or with the one used by this SIRIUS version. Nothing has been imported!" +
                            " Try again `--update-fingerprint-version` to exclude incompatible parts from the import." +
                            " WARNING: This will exclude all Fingerprint related results like CSI:FingerID and CANOPUS from the import.");
        }

        if (prog != null)
            prog.progressChanged(new JobProgressEvent(inputSpace.getRootPath(), 0, size, size, inputSpace.getRootPath().getFileName().toString() + " Done"));
        return imported;
    }


    private final static Predicate<String> DATA_FILES_TO_SKIP = n -> !n.equals(FingerIdLocations.FINGERID_CLIENT_DATA) && !n.equals(FingerIdLocations.FINGERID_CLIENT_DATA_NEG)
            && !n.equals(CanopusLocations.CANOPUS_CLIENT_DATA) && !n.equals(CanopusLocations.CANOPUS_CLIENT_DATA_NEG);

    private final static Predicate<String> RESULTS_TO_SKIP = n -> !n.equals(SummaryLocations.STRUCTURE_CANDIDATES) && !n.equals(SummaryLocations.STRUCTURE_CANDIDATES_TOP)
            && !n.equals(FingerIdLocations.FINGERBLAST.relDir()) && !n.equals(FingerIdLocations.FINGERBLAST_FPs.relDir()) && !n.equals(FingerIdLocations.FINGERPRINTS.relDir())
            && !n.equals(CanopusLocations.CANOPUS.relDir()) && !n.equals(CanopusLocations.NPC.relDir());

    private static <D extends FingerprintData<?>> D checkAnReadData(Path inputFile, IOFunctions.IOFunction<BufferedReader, D> read) throws IOException {
        if (Files.exists(inputFile)) {
            try (BufferedReader r = Files.newBufferedReader(inputFile)) {
                return read.apply(r);
            }

        }
        return null;
    }


    public static Predicate<String> checkDataCompatibility(@NotNull Path toImportPath, @Nullable ProjectSpaceManager importTarget, NetUtils.InterruptionCheck interrupted) throws IOException {
        try {
            if (FileUtils.isZipArchive(toImportPath))
                toImportPath = FileUtils.asZipFS(toImportPath, false);
            FingerIdData fdPos = checkAnReadData(toImportPath.resolve(FingerIdLocations.FINGERID_CLIENT_DATA), FingerIdData::read);
            FingerIdData fdNeg = checkAnReadData(toImportPath.resolve(FingerIdLocations.FINGERID_CLIENT_DATA_NEG), FingerIdData::read);
            CanopusData cdPos = checkAnReadData(toImportPath.resolve(CanopusLocations.CANOPUS_CLIENT_DATA), CanopusData::read);
            CanopusData cdNeg = checkAnReadData(toImportPath.resolve(CanopusLocations.CANOPUS_CLIENT_DATA_NEG), CanopusData::read);
            return checkDataCompatibility(
                    (fdNeg != null || fdPos != null) ? new FingerIdDataProperty(fdPos, fdNeg) : null,
                    (cdNeg != null || cdPos != null) ? new CanopusDataProperty(cdPos, cdNeg) : null, importTarget, interrupted);
        } finally {
            FileUtils.closeIfNotDefaultFS(toImportPath);
        }
    }

    /**
     * This checks whether the data files of the input project are compatible with the import target or them on the server.
     * <p>
     *
     * @param toImport     project to be imported that should be checked for compatibility
     * @param importTarget project to compare with, If null or data not server input is compared to server version
     * @param interrupted  Tell the waiting job how it can check if it was interrupted
     * @return null if compatible, predicate checking for paths to be skipped during import.
     */
    public static Predicate<String> checkDataCompatibility(@NotNull SiriusProjectSpace toImport, @Nullable ProjectSpaceManager importTarget, NetUtils.InterruptionCheck interrupted) {
        return checkDataCompatibility(toImport.getProjectSpaceProperty(FingerIdDataProperty.class).orElse(null),
                toImport.getProjectSpaceProperty(CanopusDataProperty.class).orElse(null), importTarget, interrupted);
    }

    public static Predicate<String> checkDataCompatibility(@NotNull SiriusProjectSpace toImport, NetUtils.InterruptionCheck interrupted) {
        return checkDataCompatibility(toImport, null, interrupted);
    }


    public static Predicate<String> checkDataCompatibility(@Nullable final FingerIdDataProperty importFd, @Nullable final CanopusDataProperty importCd, @Nullable ProjectSpaceManager importTarget, NetUtils.InterruptionCheck interrupted) {
        try {
            //check finerid
            if (importFd != null) {
                FingerIdDataProperty targetFd = importTarget != null ? importTarget.getProjectSpaceProperty(FingerIdDataProperty.class).orElse(null) : null;
                if (targetFd == null)
                    targetFd = new FingerIdDataProperty(
                            NetUtils.tryAndWait(() -> ApplicationCore.WEB_API.getFingerIdData(PredictorType.CSI_FINGERID_POSITIVE), interrupted),
                            NetUtils.tryAndWait(() -> ApplicationCore.WEB_API.getFingerIdData(PredictorType.CSI_FINGERID_NEGATIVE), interrupted));

                if (!importFd.compatible(targetFd))
                    return RESULTS_TO_SKIP;

                //check canopus
                if (importCd != null) {
                    CanopusDataProperty targetCd = importTarget != null ? importTarget.getProjectSpaceProperty(CanopusDataProperty.class).orElse(null) : null;
                    if (targetCd == null)
                        targetCd = new CanopusDataProperty(
                                NetUtils.tryAndWait(() -> ApplicationCore.WEB_API.getCanopusdData(PredictorType.CSI_FINGERID_POSITIVE), interrupted),
                                NetUtils.tryAndWait(() -> ApplicationCore.WEB_API.getCanopusdData(PredictorType.CSI_FINGERID_NEGATIVE), interrupted));

                    if (!importCd.compatible(targetCd))
                        return RESULTS_TO_SKIP;
                }
            }
            return null;
        } catch (Exception e) {
            LoggerFactory.getLogger(InstanceImporter.class).warn("Could not retrieve FingerprintData from server! Importing without checking for already outdated fingerprint data" + e.getMessage());
            return null;
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
        private final InputFilesOptions.MsInput expandedFiles;

        public InputExpanderJJob(List<Path> input, InputFilesOptions.MsInput expandTo) {
            super(JobType.TINY_BACKGROUND);
            this.input = input;
            this.expandedFiles = expandTo;
        }


        @Override
        protected InputFilesOptions.MsInput compute() throws Exception {
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
                    int size = ProjectSpaceIO.isExistingProjectspaceDirectoryNum(g);
                    if (size > 0) {
                        inputFiles.projects.put(g, size);
                    } else {
                        try {
                            final List<Path> ins =
                                    FileUtils.listAndClose(g, l -> l.filter(Files::isRegularFile).sorted().collect(Collectors.toList()));

                            if (ins.contains(Path.of(FilenameFormatter.PSPropertySerializer.FILENAME)))
                                throw new IOException("Unreadable project found!");

                            if (!ins.isEmpty())
                                expandInput(ins, inputFiles);
                        } catch (IOException e) {
                            LOG.warn("Could not list directory content of '" + g.toString() + "'. Skipping location! " + e.getMessage());
                        }
                    }
                } else {
                    //check whether files are lcms runs copressed project-spaces or standard ms/mgf files
                    try {
                        final String name = g.getFileName().toString();
                        if (ProjectSpaceIO.isZipProjectSpace(g)) {
                            //compressed spaces are read only and can be handled as simple input
                            try (SiriusProjectSpace ps = new ProjectSpaceIO(new ProjectSpaceConfiguration()).openExistingProjectSpace(g)) {
                                inputFiles.projects.put(g, ps.size()); //todo estimate size
                            }
                        } else if (MsExperimentParser.isSupportedFileName(name)) {
                            inputFiles.msParserfiles.put(g, (int) Files.size(g));
                        } else {
                            inputFiles.unknownFiles.put(g, (int) Files.size(g));
                            //                    LOG.warn("File with the name \"" + name + "\" is not in a supported format or has a wrong file extension. File is skipped");
                        }
                    } catch (IOException e) {
                        LOG.warn("Could not read '" + g.toString() + "'. Skipping location! " + e.getMessage());
                    }
                }
                updateProgress(0, files.size(), ++p);
            }
//            return inputFiles;
        }
    }

}
