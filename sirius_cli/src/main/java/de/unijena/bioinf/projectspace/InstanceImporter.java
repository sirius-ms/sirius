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

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.babelms.inputresource.PathInputResource;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import de.unijena.bioinf.jjobs.JobProgressMerger;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectSourceFormats;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectType;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDatabaseImpl;
import de.unijena.bioinf.ms.persistence.storage.exceptions.ProjectTypeException;
import de.unijena.bioinf.storage.db.nosql.Database;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InstanceImporter {
    protected static final Logger LOG = LoggerFactory.getLogger(InstanceImporter.class);
    private final ProjectSpaceManager importTarget;
    private final Predicate<Ms2Experiment> expFilter;

    public InstanceImporter(ProjectSpaceManager importTarget, Predicate<Ms2Experiment> expFilter) {
        this.importTarget = importTarget;
        this.expFilter = expFilter;
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
            if (importTarget instanceof NoSQLProjectSpaceManager noSQLProject)
                setProjectTypeOrThrow(noSQLProject.getProject());

            prog = new JobProgressMerger(pcs);
            List<Instance> instances = importMultipleSources();

            if (msInput != null && importTarget instanceof NoSQLProjectSpaceManager nsql){
                SiriusProjectDatabaseImpl<? extends Database<?>> project = nsql.getProject();
                ProjectSourceFormats projectSources = project.findProjectSourceFormats().orElseGet(ProjectSourceFormats::new);
                msInput.stream().map(InputResource::getFilename)
                        .map(FileUtils::getFileExt)
                        .filter(MsExperimentParser::isSupportedEnding)
                        .filter(Objects::nonNull).forEach(projectSources::addFormat);
                project.upsertProjectSourceFormats(projectSources);
            }

            return instances;
        }

        private void setProjectTypeOrThrow(SiriusProjectDatabaseImpl<? extends Database<?>> ps) {
            Optional<ProjectType> psType = ps.findProjectType();
            if (psType.isPresent()) {
                switch (psType.get()) {
                    case ALIGNED_RUNS:
                    case UNALIGNED_RUNS:
                        throw new ProjectTypeException("Project already contains preprocessed MS runs (.mzml, .mzxml). Additional peak-list data cannot be added to such project. Please create a new project to import your data.", ProjectType.PEAKLISTS, psType.get());
                }
            }else {
                ps.upsertProjectType(ProjectType.PEAKLISTS);
            }
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

            final InstanceImportIteratorMS2Exp it = new CsvMS2ExpIterator(csvInputs, expFilter, prog).asInstanceIterator(importTarget);
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
                InstanceImportIteratorMS2Exp it = iit.asInstanceIterator(importTarget);
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
                            if (MsExperimentParser.isLCMSFile(name)) {
                                inputFiles.lcmsFiles.put(g, (int) Files.size(g));
                            } else {
                                inputFiles.msParserfiles.put(g, (int) Files.size(g));
                            }
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
