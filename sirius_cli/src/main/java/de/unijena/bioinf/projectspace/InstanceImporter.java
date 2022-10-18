/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintData;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import de.unijena.bioinf.jjobs.JobProgressMerger;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.canopus.CanopusCfDataProperty;
import de.unijena.bioinf.projectspace.canopus.CanopusLocations;
import de.unijena.bioinf.projectspace.canopus.CanopusNpcDataProperty;
import de.unijena.bioinf.projectspace.fingerid.FingerIdDataProperty;
import de.unijena.bioinf.projectspace.fingerid.FingerIdLocations;
import de.unijena.bioinf.projectspace.summaries.SummaryLocations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
    private final ProjectSpaceManager<?> importTarget;
    private final Predicate<Ms2Experiment> expFilter;
    private final Predicate<CompoundContainerId> cidFilter;
    private final boolean move; //try to move instead of copy the data where possible
    private final boolean updateFingerprintData; //try to move instead of copy the data where possible


    public InstanceImporter(ProjectSpaceManager<?> importTarget, Predicate<Ms2Experiment> expFilter, Predicate<CompoundContainerId> cidFilter, boolean move, boolean updateFingerprintData) {
        this.importTarget = importTarget;
        this.expFilter = expFilter;
        this.cidFilter = cidFilter;
        this.move = move;
        this.updateFingerprintData = updateFingerprintData;
    }

    public InstanceImporter(ProjectSpaceManager<?> importTarget, Predicate<Ms2Experiment> expFilter, Predicate<CompoundContainerId> cidFilter) {
        this(importTarget, expFilter, cidFilter, false, false);
    }

    public ImportInstancesJJob makeImportJJob(@NotNull InputFilesOptions files) {
        return new ImportInstancesJJob(files);
    }

    public List<CompoundContainerId> doImport(InputFilesOptions projectInput, JobProgressEventListener listener) throws ExecutionException {
        if (projectInput == null)
            return null;
        final ImportInstancesJJob j = makeImportJJob(projectInput);
        j.addJobProgressListener(listener);
        List<CompoundContainerId> cids = SiriusJobs.getGlobalJobManager().submitJob(j).awaitResult();
        j.removeJobProgressListener(listener);
        return cids;
    }
    public List<CompoundContainerId> doImport(InputFilesOptions projectInput) throws ExecutionException {
        if (projectInput == null)
            return null;
        return SiriusJobs.getGlobalJobManager().submitJob(makeImportJJob(projectInput)).awaitResult();
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
            prog = new JobProgressMerger(pcs);
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

            final InstanceImportIteratorMS2Exp it = new MS2ExpInputIterator(files, expFilter, inputFiles.msInput.isIgnoreFormula(), inputFiles.msInput.isAllowMS1Only(), prog)
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
            @NotNull SiriusProjectSpace inputSpace, @NotNull ProjectSpaceManager<?> importTarget,
            @NotNull Predicate<CompoundContainerId> cidFilter, boolean move, boolean updateFingerprintVersion) throws IOException {

        return importProject(inputSpace, importTarget, cidFilter, move, updateFingerprintVersion, null);
    }

    // we do not exp level filter here since we want to prevent reading the spectrum file
    // we do file system level copies where we can here
    public static List<CompoundContainerId> importProject(
            @NotNull SiriusProjectSpace inputSpace, @NotNull ProjectSpaceManager<?> importTarget,
            @NotNull Predicate<CompoundContainerId> cidFilter, boolean move, boolean updateFingerprintVersion, @Nullable JobProgressMerger prog) throws IOException {

        final int size = inputSpace.size();
        int progress = 0;

        //check is fingerprint data is compatible and clean if not.
        @Nullable Predicate<String> resultsToSkip = checkDataCompatibility(inputSpace, importTarget, NetUtils.checkThreadInterrupt(Thread.currentThread()));
        final List<CompoundContainerId> imported = new ArrayList<>(inputSpace.size());

        if (resultsToSkip == null || updateFingerprintVersion) {
            final ProjectWriter targetWriter = importTarget.projectSpace().ioProvider.newWriter(importTarget.projectSpace()::getProjectSpaceProperty);
            final ProjectReader sourceReader = inputSpace.ioProvider.newReader(inputSpace::getProjectSpaceProperty);
            //todo make use of glob
            List<String> globalFiles = sourceReader.listFiles("*").stream()
                    .filter(p -> !p.equals(PSLocations.FORMAT) &&
                            !p.equals(PSLocations.COMPRESSION) &&
                            !p.equals(SummaryLocations.COMPOUND_SUMMARY_ADDUCTS) &&
                            !p.equals(SummaryLocations.COMPOUND_SUMMARY) &&
                            !p.equals(SummaryLocations.FORMULA_SUMMARY) &&
                            !p.equals(SummaryLocations.CANOPUS_FORMULA_SUMMARY) &&
                            !p.equals(SummaryLocations.MZTAB_SUMMARY) &&
                            (resultsToSkip == null || DATA_FILES_TO_SKIP.test(p)) //skip data files if incompatible
                    ).collect(Collectors.toList());

            for (String relative : globalFiles) {
                try {
                    sourceReader.binaryFile(relative, r -> {
                        targetWriter.binaryFile(relative, r::transferTo);
                        return true;
                    });
                } catch (IOException e) {
                    LOG.error("Could not Copy '" + relative + "' from old location '" + inputSpace.getLocation() + "' to new location `" + importTarget.projectSpace().getLocation() + "' Project might be corrupted!", e);
                }
            }


            final Iterator<CompoundContainerId> psIter = inputSpace.filteredIterator(cidFilter);

            final boolean flatCopy = inputSpace.ioProvider.getCompressionFormat().equals(importTarget.projectSpace().ioProvider.getCompressionFormat())
                    && (inputSpace.ioProvider instanceof PathProjectSpaceIOProvider)
                    && (importTarget.projectSpace().ioProvider instanceof PathProjectSpaceIOProvider);

            while (psIter.hasNext()) {
                final CompoundContainerId sourceId = psIter.next();
                // create compound
                CompoundContainerId id = importTarget.projectSpace().newUniqueCompoundId(sourceId.getCompoundName(), (idx) -> importTarget.namingScheme.apply(idx, sourceId.getCompoundName())).orElseThrow();
                id.setAllNonFinal(sourceId);
                importTarget.projectSpace().updateCompoundContainerID(id);

                if (flatCopy) {
                    ((PathProjectSpaceIOProvider) inputSpace.ioProvider).fsManager.readFile(sourceId.getDirectoryName(), path -> {
                        List<Path> files = FileUtils.walkAndClose(s -> s
                                .filter(Files::isRegularFile)
                                .filter(p -> !p.getFileName().toString().equals(SiriusLocations.COMPOUND_INFO))
                                .filter(p -> !p.getFileName().toString().equals(SummaryLocations.FORMULA_CANDIDATES))
                                .filter(p -> !p.getFileName().toString().equals(SummaryLocations.STRUCTURE_CANDIDATES))
                                .filter(it -> resultsToSkip == null || resultsToSkip.test(it.getFileName().toString()))
                                .collect(Collectors.toList()), path);

                        @NotNull FileSystemManager m = ((PathProjectSpaceIOProvider) importTarget.projectSpace().ioProvider).fsManager;
                        for (Path sourceP : files) {
                            try (InputStream s = Files.newInputStream(sourceP)) {
                                m.writeFile(id.getDirectoryName(), targetRoot -> {
                                    Path targetP = targetRoot.resolve(path.relativize(sourceP).toString());
                                    if (targetP.getParent() != null)
                                        Files.createDirectories(targetP.getParent());
                                    try (OutputStream o = Files.newOutputStream(targetP)) {
                                        s.transferTo(o);
                                    }
                                });
                            }
                        }
                    });
                } else {
                    sourceReader.inDirectory(sourceId.getDirectoryName(), () -> {
                        List<String> files = sourceReader.listFilesRecursive("*").stream()
                                .filter(p -> !p.equals(SiriusLocations.COMPOUND_INFO))
                                .filter(p -> !p.equals(SummaryLocations.FORMULA_CANDIDATES))
                                .filter(p -> !p.equals(SummaryLocations.STRUCTURE_CANDIDATES))
                                .filter(it -> resultsToSkip == null || resultsToSkip.test(it))
                                .collect(Collectors.toList());
                        for (String relative : files) {
                            try {
                                sourceReader.binaryFile(relative, r ->
                                        targetWriter.inDirectory(id.getDirectoryName(), () -> {
                                            targetWriter.binaryFile(relative, r::transferTo);
                                            return null;
                                        }));//todo reimplement move support for dirs?
                            } catch (IOException e) {
                                LOG.error("Could not Copy instance'" + id.getDirectoryName() + "' from old location '" + inputSpace.getLocation() + "' to new location `" + importTarget.projectSpace().getLocation() + "' Results might be missing!", e);
                            }
                        }

                        return null;
                    });
                }

                if (resultsToSkip != null) {
                    LoggerFactory.getLogger(InstanceImporter.class).info("Updating Compound score of '" + id + "' after deleting Fingerprint related results...");
                    Instance inst = importTarget.getInstanceFromCompound(id);
                    List<FormulaResult> l = inst.loadFormulaResults(FormulaScoring.class).stream().map(SScored::getCandidate)
                            .filter(r -> r.getAnnotation(FormulaScoring.class).map(s -> (s.removeAnnotation(TopCSIScore.class) != null)
                                    || (s.removeAnnotation(ConfidenceScore.class) != null)).orElse(false))
                            .collect(Collectors.toList());

                    l.forEach(r -> inst.updateFormulaResult(r, FormulaScoring.class));
                    LoggerFactory.getLogger(InstanceImporter.class).info("Updating Compound score of '" + id + "' DONE!");
                }

                imported.add(id);
                if (prog != null)
                    prog.progressChanged(new JobProgressEvent(inputSpace.getLocation(), 0, size, ++progress, id.toString()));
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
            prog.progressChanged(new JobProgressEvent(inputSpace.getLocation(), 0, size, size, inputSpace.getLocation().getFileName().toString() + " Done"));
        return imported;
    }


    private final static Predicate<String> DATA_FILES_TO_SKIP = n ->
            !n.equals(FingerIdLocations.FINGERID_CLIENT_DATA) && !n.equals(FingerIdLocations.FINGERID_CLIENT_DATA_NEG)
                    && !n.equals(CanopusLocations.CF_CLIENT_DATA) && !n.equals(CanopusLocations.CF_CLIENT_DATA_NEG)
                    && !n.equals(CanopusLocations.NPC_CLIENT_DATA) && !n.equals(CanopusLocations.NPC_CLIENT_DATA_NEG);

    private final static Predicate<String> RESULTS_TO_SKIP = n ->
            !n.equals(SummaryLocations.STRUCTURE_CANDIDATES)
                    && !n.equals(FingerIdLocations.FINGERBLAST.relDir()) && !n.equals(FingerIdLocations.FINGERBLAST_FPs.relDir()) && !n.equals(FingerIdLocations.FINGERPRINTS.relDir())
                    && !n.equals(CanopusLocations.CF.relDir()) && !n.equals(CanopusLocations.NPC.relDir());

    private static <D extends FingerprintData<?>> D checkAnReadData(String relative, ProjectReader reader, IOFunctions.IOFunction<BufferedReader, D> read) throws IOException {
        if (reader.exists(relative))
            return reader.textFile(relative, read);
        return null;
    }


    public static Predicate<String> checkDataCompatibility(@NotNull Path toImportPath, @Nullable ProjectSpaceManager<?> importTarget, NetUtils.InterruptionCheck interrupted) throws IOException {
        try {
            final ProjectReader reader;
            if (FileUtils.isZipArchive(toImportPath))
                reader = ProjectSpaceIO.getDefaultZipProvider(toImportPath).newReader(null);
            else
                reader = new PathProjectSpaceIOProvider(toImportPath, null).newReader(null);

            FingerIdData fdPos = checkAnReadData(FingerIdLocations.FINGERID_CLIENT_DATA, reader, FingerIdData::read);
            FingerIdData fdNeg = checkAnReadData(FingerIdLocations.FINGERID_CLIENT_DATA_NEG, reader, FingerIdData::read);
            CanopusCfData cdPos = checkAnReadData(CanopusLocations.CF_CLIENT_DATA, reader, CanopusCfData::read);
            CanopusCfData cdNeg = checkAnReadData(CanopusLocations.CF_CLIENT_DATA_NEG, reader, CanopusCfData::read);
            CanopusNpcData npcPos = checkAnReadData(CanopusLocations.NPC_CLIENT_DATA, reader, CanopusNpcData::read);
            CanopusNpcData npcNeg = checkAnReadData(CanopusLocations.NPC_CLIENT_DATA_NEG, reader, CanopusNpcData::read);

            Predicate<String> r;
            r = checkDataCompatibility((fdNeg != null || fdPos != null) ? new FingerIdDataProperty(fdPos, fdNeg) : null,
                    FingerIdData.class, ApplicationCore.WEB_API::getFingerIdData, importTarget, interrupted);
            if (r != null) return r;

            r = checkDataCompatibility((cdNeg != null || cdPos != null) ? new CanopusCfDataProperty(cdPos, cdNeg) : null,
                    CanopusCfData.class, ApplicationCore.WEB_API::getCanopusCfData, importTarget, interrupted);
            if (r != null) return r;

            r = checkDataCompatibility((npcNeg != null || npcPos != null) ? new CanopusNpcDataProperty(npcPos, npcNeg) : null,
                    CanopusNpcData.class, ApplicationCore.WEB_API::getCanopusNpcData, importTarget, interrupted);
            return r;
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
    public static Predicate<String> checkDataCompatibility(@NotNull SiriusProjectSpace toImport, @Nullable ProjectSpaceManager<?> importTarget, NetUtils.InterruptionCheck interrupted) {
        Predicate<String> r;
        r = checkDataCompatibility(toImport.getProjectSpaceProperty(FingerIdDataProperty.class).orElse(null),
                FingerIdData.class, ApplicationCore.WEB_API::getFingerIdData, importTarget, interrupted);
        if (r != null) return r;
        r = checkDataCompatibility(toImport.getProjectSpaceProperty(CanopusCfDataProperty.class).orElse(null),
                CanopusCfData.class, ApplicationCore.WEB_API::getCanopusCfData, importTarget, interrupted);
        if (r != null) return r;
        r = checkDataCompatibility(toImport.getProjectSpaceProperty(CanopusNpcDataProperty.class).orElse(null),
                CanopusNpcData.class, ApplicationCore.WEB_API::getCanopusNpcData, importTarget, interrupted);
        return r;
    }

    public static Predicate<String> checkDataCompatibility(@NotNull SiriusProjectSpace toImport, NetUtils.InterruptionCheck interrupted) {
        return checkDataCompatibility(toImport, null, interrupted);
    }


    public static <F extends FingerprintVersion, D extends FingerprintData<F>, P extends PosNegFpProperty<F, D>>
    Predicate<String> checkDataCompatibility(@Nullable P importFd, Class<D> dataClz, @NotNull IOFunctions.IOFunction<PredictorType, D> dataLoader, @Nullable ProjectSpaceManager<?> importTarget, NetUtils.InterruptionCheck interrupted) {
        try {
            //check prop
            if (importFd != null) {
                P targetFd = importTarget != null ? (P) importTarget.getProjectSpaceProperty(importFd.getClass()).orElse(null) : null;
                if (targetFd == null)
                    targetFd = (P) importFd.getClass().getConstructor(dataClz, dataClz).newInstance(
                            NetUtils.tryAndWait(() -> dataLoader.apply(PredictorType.CSI_FINGERID_POSITIVE), interrupted),
                            NetUtils.tryAndWait(() -> dataLoader.apply(PredictorType.CSI_FINGERID_NEGATIVE), interrupted)
                    );

                if (!importFd.compatible(targetFd))
                    return RESULTS_TO_SKIP;
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
                    } else if (size < 0) {
                        try {
                            final List<Path> ins =
                                    FileUtils.listAndClose(g, l -> l.filter(Files::isRegularFile).sorted().collect(Collectors.toList()));

                            if (ins.contains(Path.of(PSLocations.FORMAT)))
                                throw new IOException("Unreadable project found!");

                            if (!ins.isEmpty())
                                expandInput(ins, inputFiles);
                        } catch (IOException e) {
                            LOG.warn("Could not list directory content of '" + g.toString() + "'. Skipping location! " + e.getMessage());
                        }
                    } else {
                        LOG.warn("Project Location  is empty '" + g.toString() + "'. Skipping location!");
                    }
                } else {
                    //check whether files are lcms runs copressed project-spaces or standard ms/mgf files
                    try {
                        final String name = g.getFileName().toString();
                        if (ProjectSpaceIO.isZipProjectSpace(g)) {
                            //compressed spaces are read only and can be handled as simple input
                            Path ps = FileUtils.asZipFSPath(g, false, false, null);
                            try {
                                int size = ProjectSpaceIO.isExistingProjectspaceDirectoryNum(ps);
                                if (size > 0) {
                                    inputFiles.projects.put(g, size);
                                } else if (size == 0) {
                                    LOG.warn("Project Location  is empty '" + g.toString() + "'. Skipping location!");
                                }
                            } finally {
                                if (ps != null)
                                    ps.getFileSystem().close();
                            }
                        } else if (MsExperimentParser.isSupportedFileName(name)) {
                            inputFiles.msParserfiles.put(g, (int) Files.size(g));
                        } else {
                            inputFiles.unknownFiles.put(g, (int) Files.size(g));
                            //                    LOG.warn("File with the name \"" + name + "\" is not in a supported format or has a wrong file extension. File is skipped");
                        }
                    } catch (IOException e) {
                        LOG.warn("Could not read '" + g.toString() + "'. Skipping location! " + e.getMessage(), e);
                    }
                }
                updateProgress(0, files.size(), ++p);
            }
        }
    }
}
