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

import de.unijena.bioinf.ChemistryBase.fp.FingerprintData;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.babelms.inputresource.PathInputResource;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.BasicJJob;
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
import de.unijena.bioinf.rest.NetUtils;
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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InstanceImporter {
    protected static final Logger LOG = LoggerFactory.getLogger(InstanceImporter.class);
    private final ProjectSpaceManager importTarget;
    private final Predicate<Ms2Experiment> expFilter;
    private final Predicate<CompoundContainerId> cidFilter;

    public InstanceImporter(ProjectSpaceManager importTarget, Predicate<Ms2Experiment> expFilter, Predicate<CompoundContainerId> cidFilter) {
        this.importTarget = importTarget;
        this.expFilter = expFilter;
        this.cidFilter = cidFilter;
    }

    public ImportInstancesJJob makeImportJJob(@Nullable Collection<InputResource<?>> msInput, boolean ignoreFormulas, boolean allowMs1Only) {
        return new ImportInstancesJJob(msInput, ignoreFormulas, allowMs1Only);
    }

    public ImportInstancesJJob makeImportJJob(@NotNull InputFilesOptions files) {
        return new ImportInstancesJJob(files);
    }

    public List<Instance> doImport(InputFilesOptions projectInput, JobProgressEventListener listener) throws ExecutionException {
        if (projectInput == null)
            return null;
        final ImportInstancesJJob j = makeImportJJob(projectInput);
        j.addJobProgressListener(listener);
        List<Instance> cids = SiriusJobs.getGlobalJobManager().submitJob(j).awaitResult();
        j.removeJobProgressListener(listener);
        return cids;
    }

    public List<Instance> doImport(InputFilesOptions projectInput) throws ExecutionException {
        if (projectInput == null)
            return null;
        return SiriusJobs.getGlobalJobManager().submitJob(makeImportJJob(projectInput)).awaitResult();
    }

    public class ImportInstancesJJob extends BasicJJob<List<Instance>> {
        private @Nullable Collection<InputResource<?>> msInput = null;

        boolean ignoreFormulas, allowMs1Only;
        @Nullable
        private List<InputFilesOptions.CsvInput> csvInput = null;
        private JobProgressMerger prog;


        public ImportInstancesJJob(@Nullable InputFilesOptions input) {
            super(JobType.TINY_BACKGROUND);
            if (input != null) {
                this.msInput = input.msInput.msParserfiles.keySet().stream().sorted().map(PathInputResource::new).collect(Collectors.toList());
                this.csvInput = input.csvInputs;
                this.ignoreFormulas = input.msInput.isIgnoreFormula();
                this.allowMs1Only = input.msInput.isAllowMS1Only();
            }
        }


        public ImportInstancesJJob(@Nullable Collection<InputResource<?>> msInput, boolean ignoreFormulas, boolean allowMs1Only) {
            super(JobType.TINY_BACKGROUND);
            this.msInput = msInput;
            this.ignoreFormulas = ignoreFormulas;
            this.allowMs1Only = allowMs1Only;
        }

        @Override
        protected List<Instance> compute() throws Exception {
            prog = new JobProgressMerger(pcs);
            return importMultipleSources();
        }


        private List<Instance> importMultipleSources() {
            List<Instance> list = new ArrayList<>();

            if (msInput != null) {
                msInput.forEach(f -> prog.addPreload(f, 0, f.getSize()));
                list.addAll(importMsParserInput(msInput.stream().toList()));
            }

            if (csvInput != null) {
                csvInput.forEach(i -> prog.addPreload(i, 0, i.ms2.size()));
                list.addAll(importCSVInput(csvInput));
            }
            return list;

        }

        private List<Instance> importCSVInput(List<InputFilesOptions.CsvInput> csvInputs) {
            if (csvInputs == null || csvInputs.isEmpty())
                return List.of();

            final InstanceImportIteratorMS2Exp it = new CsvMS2ExpIterator(csvInputs, expFilter, prog).asInstanceIterator(importTarget, (c) -> cidFilter.test(c.getId()));
            final List<Instance> ll = new ArrayList<>();

            // no progress
            while (it.hasNext())
                ll.add(it.next());

            return ll;
        }

        private List<Instance> importMsParserInput(@Nullable Collection<InputResource<?>> files) {
            if (files == null || files.isEmpty())
                return List.of();

            try (final MS2ExpInputIterator iit = new MS2ExpInputIterator(files, expFilter, ignoreFormulas, allowMs1Only, prog)) {
                InstanceImportIteratorMS2Exp it = iit.asInstanceIterator(importTarget, (c) -> cidFilter.test(c.getId()));
                final List<Instance> ll = new ArrayList<>();

                if (prog.isDone())
                    prog.indeterminateProgress(); // just to show something in case only one small file
                while (it.hasNext()) {
                    Instance id = it.next();
                    if (prog.isDone())
                        prog.indeterminateProgress(id.getName());
                    else
                        prog.progressMessage(id.getName());
                    ll.add(id);
                }

                return ll;
            }
        }
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


    public static Predicate<String> checkDataCompatibility(@NotNull Path toImportPath, @Nullable ProjectSpaceManager importTarget, NetUtils.InterruptionCheck interrupted) throws IOException {
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
    public static Predicate<String> checkDataCompatibility(@NotNull SiriusProjectSpace toImport, @Nullable ProjectSpaceManager importTarget, NetUtils.InterruptionCheck interrupted) {
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
    Predicate<String> checkDataCompatibility(@Nullable P importFd, Class<D> dataClz, @NotNull IOFunctions.IOFunction<PredictorType, D> dataLoader, @Nullable ProjectSpaceManager importTarget, NetUtils.InterruptionCheck interrupted) {
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
                    LOG.warn("Path \"" + g + "\" does not exist and will be skipped");
                    continue;
                }

                if (Files.isDirectory(g)) {
                    try {
                        final List<Path> ins =
                                FileUtils.listAndClose(g, l -> l.filter(Files::isRegularFile).sorted().collect(Collectors.toList()));

                        if (ins.contains(Path.of(PSLocations.FORMAT)))
                            throw new IOException("Unreadable project found!");

                        if (!ins.isEmpty())
                            expandInput(ins, inputFiles);
                    } catch (IOException e) {
                        LOG.warn("Could not list directory content of '" + g + "'. Skipping location! " + e.getMessage());
                    }
                } else {
                    //check whether files are lcms runs or standard ms/mgf files
                    try {
                        final String name = g.getFileName().toString();
                        if (MsExperimentParser.isSupportedFileName(name)) {
                            inputFiles.msParserfiles.put(g, (int) Files.size(g));
                        } else {
                            inputFiles.unknownFiles.put(g, (int) Files.size(g));
                        }
                    } catch (IOException e) {
                        LOG.warn("Could not read '" + g + "'. Skipping location! " + e.getMessage(), e);
                    }
                }
                updateProgress(0, files.size(), ++p);
            }
        }
    }
}
