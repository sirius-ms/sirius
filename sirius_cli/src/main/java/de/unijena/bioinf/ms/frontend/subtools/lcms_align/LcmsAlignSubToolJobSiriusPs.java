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

package de.unijena.bioinf.ms.frontend.subtools.lcms_align;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import de.unijena.bioinf.ChemistryBase.exceptions.InvalidInputData;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Warning;
import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.ChemistryBase.ms.lcms.workflows.LCMSWorkflow;
import de.unijena.bioinf.ChemistryBase.ms.lcms.workflows.MixedWorkflow;
import de.unijena.bioinf.ChemistryBase.ms.lcms.workflows.PooledMs2Workflow;
import de.unijena.bioinf.ChemistryBase.ms.lcms.workflows.RemappingWorkflow;
import de.unijena.bioinf.io.lcms.LCMSParsing;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.lcms.*;
import de.unijena.bioinf.lcms.align.Aligner;
import de.unijena.bioinf.lcms.align.Aligner2;
import de.unijena.bioinf.lcms.align.Cluster;
import de.unijena.bioinf.lcms.ionidentity.AdductResolver;
import de.unijena.bioinf.lcms.quality.LCMSQualityCheck;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.model.lcms.*;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.networks.Correlation;
import de.unijena.bioinf.networks.MolecularNetwork;
import de.unijena.bioinf.networks.NetworkNode;
import de.unijena.bioinf.networks.serialization.ConnectionTable;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.sirius.validation.Ms2Validator;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import org.apache.commons.io.function.IOSupplier;
import org.apache.commons.math3.distribution.RealDistribution;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LcmsAlignSubToolJobSiriusPs extends PreprocessingJob<ProjectSpaceManager> {
    List<Path> inputFiles;
    Path workingDir;

    protected final @Nullable LCMSWorkflow workflow;
    protected final @Nullable Path statistics;
    protected final List<SiriusProjectSpaceInstance> importedCompounds = new ArrayList<>();
    private final IOSupplier<SiriusProjectSpaceManager> projectSupplier;
    private SiriusProjectSpaceManager space;


    public LcmsAlignSubToolJobSiriusPs(InputFilesOptions input, @NotNull IOSupplier<SiriusProjectSpaceManager> projectSupplier, LcmsAlignOptions options) {
        this(getWorkingDirectory(input), input.msInput.msParserfiles.keySet().stream().sorted().collect(Collectors.toList()), projectSupplier, options.getWorkflow().orElse(null), options.statistics != null ? options.statistics.toPath() : null);
    }

    public LcmsAlignSubToolJobSiriusPs(@NotNull Path workingDir, @NotNull List<Path> inputFiles, @NotNull IOSupplier<SiriusProjectSpaceManager> projectSupplier, @Nullable LCMSWorkflow workflow, @Nullable Path statistics) {
        super();
        this.workingDir = workingDir;
        this.inputFiles = inputFiles;
        this.projectSupplier = projectSupplier;
        this.workflow = workflow;
        this.statistics = statistics;
    }

    @Override
    protected SiriusProjectSpaceManager compute() throws Exception {
        importedCompounds.clear();
        space = projectSupplier.get();
        final LCMSProccessingInstance i = new LCMSProccessingInstance();
        if (statistics != null) i.trackStatistics();

        if (workflow != null) {
            return computeWorkflow(i, workflow);
        } else {
            LoggerFactory.getLogger(LcmsAlignSubToolJobSiriusPs.class).info("No workflow specified. Use 'default' workflow: mixed-mode with alignment.");
            return computeMixedWorkflow(i, inputFiles, true);
        }
    }

    /*
     * @Markus: can we implement that in a nicer way?
     */
    private static Path getWorkingDirectory(InputFilesOptions input) {
        if (input != null && input.msInput.getRawInputFiles().size() == 1) {
            return input.msInput.getRawInputFiles().get(0);
        } else return new File(".").toPath();
    }

    private SiriusProjectSpaceManager computeWorkflow(LCMSProccessingInstance i, LCMSWorkflow lcmsWorkflow) throws IOException {
        if (lcmsWorkflow instanceof PooledMs2Workflow) {
            return computePooledWorkflow(i, (PooledMs2Workflow) lcmsWorkflow);
        } else if (lcmsWorkflow instanceof MixedWorkflow) {
            return computeMixedWorkflow(i, (MixedWorkflow) lcmsWorkflow);
        } else if (lcmsWorkflow instanceof RemappingWorkflow) {
            return computeRemappingWorkflow(i, (RemappingWorkflow) lcmsWorkflow);
        } else throw new IllegalArgumentException("Unknown workflow: " + lcmsWorkflow.getClass().getName());
    }

    private SiriusProjectSpaceManager computeMixedWorkflow(LCMSProccessingInstance i, MixedWorkflow lcmsWorkflow) throws IOException {
        final List<Path> files = Arrays.stream(lcmsWorkflow.getFiles()).map(x -> workingDir.resolve(x)).collect(Collectors.toList());
        return computeMixedWorkflow(i, files, lcmsWorkflow.isAlign());
    }

    private SiriusProjectSpaceManager computeMixedWorkflow(LCMSProccessingInstance i, List<Path> files, boolean align) throws IOException {
        final ArrayList<BasicJJob<?>> jobs = new ArrayList<>();
        updateProgress(0, files.size(), 0, "Parse LC/MS runs");
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger featureCounter = new AtomicInteger(0);
        for (Path f : files) {
            jobs.add(SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<>() {
                @Override
                protected Object compute() {
                    try {
                        MemoryFileStorage storage = new MemoryFileStorage();
                        final LCMSRun parse = LCMSParsing.parseRun(f.toUri(), storage);
                        final ProcessedSample sample = i.addSample(parse, storage);
                        i.detectFeatures(sample);
                        storage.backOnDisc();
                        storage.dropBuffer();
                        final int c = counter.incrementAndGet();
                        LcmsAlignSubToolJobSiriusPs.this.updateProgress(0, files.size(), c, "Parse LC/MS runs");
                        if (!align) {
                            Iterator<FragmentedIon> ions = sample.ions.stream().filter(i -> i != null && Math.abs(i.getChargeState()) <= 1 && i.getMsMsQuality().betterThan(Quality.BAD)).iterator();
                            while (ions.hasNext()) {
                                final FragmentedIon ion = ions.next();
                                AdductResolver.resolve(i, ion);
                                Feature feature = i.makeFeature(sample, ion, false);
                                final int featureId = featureCounter.incrementAndGet();
                                MutableMs2Experiment experiment = feature.toMsExperiment(
                                        sample.run.getIdentifier() + "_" + featureId, String.valueOf(featureId)).mutate();
                                new Ms2Validator().validate(experiment, Warning.Logger, true);
                                importCompound(space, experiment);
                            }
                        }
                    } catch (Throwable e) {
                        LoggerFactory.getLogger(LcmsAlignSubToolJobSiriusPs.class).error("Error while parsing file '" + f + "': " + e.getMessage());
                        if (!(e instanceof InvalidInputData)) {
                            //stacktrace for unexpected errors
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }
                    return "";
                }
            }));
        }
        for (BasicJJob<?> j : jobs) j.takeResult();
        if (align) {
            MultipleSources sourcelocation = MultipleSources.leastCommonAncestor(files.toArray(Path[]::new));
            i.getMs2Storage().backOnDisc();
            i.getMs2Storage().dropBuffer();

            if (i.getSamples().size() == 0) {
                LoggerFactory.getLogger(LcmsAlignSubToolJobSiriusPs.class).error("No input data available to be aligned.");
                return space;
            }

            Cluster alignment = i.alignAndGapFilling(this);
            updateProgress(0, 2, 0, "Assign adducts.");
            i.detectAdductsWithGibbsSampling(alignment);
            updateProgress(0, 2, 1, "Merge features.");
            alignment = alignment.deleteDuplicateRows();
            return importIntoProjectSpace(i, alignment, sourcelocation);
        }
        return space;
    }

    private SiriusProjectSpaceManager computePooledWorkflow(LCMSProccessingInstance instance, PooledMs2Workflow lcmsWorkflow) {
        // read all files
        final JobManager jm = SiriusJobs.getGlobalJobManager();
        final ProcessedSample[] ms2Samples = Arrays.stream(lcmsWorkflow.getPooledMs2())
                .map(filename -> jm.submitJob(processRunJob(instance, filename)))
                .toList().stream()
                .map(JJob::takeResult).toArray(ProcessedSample[]::new);
        System.out.println("MS2 DONE");
        final ProcessedSample[] ms1Samples = Arrays.stream(lcmsWorkflow.getPooledMs1())
                .map(filename -> jm.submitJob(processRunJob(instance, filename)))
                .toList().stream()
                .map(JJob::takeResult).toArray(ProcessedSample[]::new);
        final ProcessedSample[] remainingSamples = Arrays.stream(lcmsWorkflow.getRemainingMs1())
                .map(filename -> jm.submitJob(processRunJob(instance, filename)))
                .toList().stream()
                .map(JJob::takeResult).toArray(ProcessedSample[]::new);
        if (ms1Samples.length > 1) {
            LoggerFactory.getLogger(LcmsAlignSubToolJobSiriusPs.class).warn("Multiple pooled MS1 samples are not supported yet. We will just process the first one.");
        }
        // now merge ms2 into ms1
        final Ms1Ms2Pairing ms1Ms2Pairing = new Ms1Ms2Pairing(ms1Samples[0], ms2Samples);
        ms1Ms2Pairing.run(instance);
        // attach remaining ms1
        RealDistribution error = ms1Ms2Pairing.attachRemainingMs1(instance, remainingSamples);
        // start alignment
        jm.submitJob(new Aligner(false).prealignAndFeatureCutoff2(instance.getSamples(), new Aligner2(error).maxRetentionError(), 1)).takeResult();
        Cluster cluster = jm.submitJob(new Aligner2(error).align(instance.getSamples())).takeResult().deleteRowsWithNoMsMs().deleteDuplicateRows();
        instance.detectAdductsWithGibbsSampling(cluster);
        cluster = cluster.deleteDuplicateRows();
        final MultipleSources sourcelocation = MultipleSources.leastCommonAncestor(Arrays.stream(lcmsWorkflow.getPooledMs2())
                .map(s -> workingDir.getFileSystem().getPath(s))
                .toArray(Path[]::new));
        return importIntoProjectSpace(instance, cluster, sourcelocation);
    }


    private SiriusProjectSpaceManager computeRemappingWorkflow(LCMSProccessingInstance lcmsInstance, RemappingWorkflow lcmsWorkflow) {
        // read all files
        final JobManager jm = SiriusJobs.getGlobalJobManager();
        final ProcessedSample[] ms1Samples = Arrays.stream(lcmsWorkflow.getFiles())
                .map(filename -> jm.submitJob(processRunJob(lcmsInstance, filename)))
                .toList().stream()
                .map(JJob::takeResult).toArray(ProcessedSample[]::new);

        final List<Ms2Experiment> exps = new ArrayList<>();
        final List<LCMSPeakInformation> peaks = new ArrayList<>();
        final List<String> ids = new ArrayList<>();
        space.forEach(inst -> {
            final CompoundContainer next = ((SiriusProjectSpaceInstance)inst).loadCompoundContainer(LCMSPeakInformation.class, Ms2Experiment.class);
            if (next.getAnnotation(Ms2Experiment.class).isPresent() && next.getAnnotation(LCMSPeakInformation.class).isPresent()) {
                exps.add(next.getAnnotation(Ms2Experiment.class).get());
                peaks.add(next.getAnnotation(LCMSPeakInformation.class).get());
                ids.add(next.getId().getDirectoryName()); //todo getDirectoryName correct
            }
        });

        final LCMSPeakInformation[] replaced = Ms1Remapping.remapMS1(lcmsInstance, ms1Samples, peaks.toArray(LCMSPeakInformation[]::new), exps.toArray(Ms2Experiment[]::new), true);
        for (int i = 0; i < ids.size(); ++i) {
            final String instanceId = ids.get(i);
            LCMSPeakInformation replacement = replaced[i];
            space.findInstance(instanceId).ifPresent(instance -> {
                CompoundContainer compound = instance.loadCompoundContainer();
                compound.setAnnotation(LCMSPeakInformation.class, replacement);
                instance.updateCompound(compound, LCMSPeakInformation.class);
            });
        }
        return space;
    }

    private void importCompound(SiriusProjectSpaceManager space, MutableMs2Experiment experiment) {
        if (isInvalidExp(experiment)) {
            LoggerFactory.getLogger(getClass()).warn("Skipping invalid experiment '" + experiment.getName() + "'.");
            return;
        }

        final SiriusProjectSpaceInstance compound = space.importInstanceWithUniqueId(experiment, false);
        importedCompounds.add(compound);
    }

    private SiriusProjectSpaceManager importIntoProjectSpace(LCMSProccessingInstance i, Cluster alignment, MultipleSources sourcelocation) {
        final ConsensusFeature[] consensusFeatures = i.makeConsensusFeatures(alignment);
        logInfo(consensusFeatures.length + "Feature left after merging.");

        int totalFeatures = 0, goodFeatures = 0;
        //save
        updateProgress(0, consensusFeatures.length, 0, "Write project space.");
        int progress = 0;
        final HashMap<ConsensusFeature, Instance> feature2Instance = new HashMap<>();
        List<LCMSCompoundSummary> allSummaries = new ArrayList<>();
        for (final ConsensusFeature feature : consensusFeatures) {
            final Ms2Experiment experiment = feature.toMs2Experiment();
            if (isInvalidExp(experiment)) {
                LoggerFactory.getLogger(getClass()).warn("Skipping invalid experiment '" + experiment.getName() + "'.");
                continue;
            }
            final LCMSPeakInformation lcmsPeakInformation = feature.getLCMSPeakInformation();
            // set quality flags
            {
                // just look at the top 5 most intensive samples
                List<Integer> indizes = new ArrayList<>();
                for (int k = 0; k < lcmsPeakInformation.length(); ++k) {
                    if (lcmsPeakInformation.getTracesFor(k).isPresent()) indizes.add(k);
                }
                indizes.sort(Comparator.comparingDouble(lcmsPeakInformation::getIntensityOf));
                Collections.reverse(indizes);
                boolean badPeakShape = true;
                LCMSCompoundSummary bestSummary = null;
                for (int k = 0; k < Math.min(indizes.size(), 5); ++k) {
                    final CoelutingTraceSet traceSet = lcmsPeakInformation.getTracesFor(k).get();
                    LCMSCompoundSummary summary = new LCMSCompoundSummary(traceSet, traceSet.getIonTrace(), experiment);
                    if (bestSummary == null || summary.points() > bestSummary.points())
                        bestSummary = summary;

                    if (summary.getPeakQuality().ordinal() > LCMSQualityCheck.Quality.LOW.ordinal()) {
                        badPeakShape = false;
                        break;
                    }
                }
                allSummaries.add(bestSummary);
                if (badPeakShape) {
                    experiment.setAnnotation(CompoundQuality.class, experiment.getAnnotation(CompoundQuality.class).orElse(new CompoundQuality()).updateQuality(CompoundQuality.CompoundQualityFlag.BadPeakShape));
                }
            }
            ++totalFeatures;
            if (experiment.getAnnotation(CompoundQuality.class, CompoundQuality::new).isNotBadQuality()) {
                ++goodFeatures;
            }
            // set name to common prefix
            // kaidu: this is super slow, so we just ignore the filename
            experiment.setAnnotation(SpectrumFileSource.class, new SpectrumFileSource(sourcelocation.value));

            final SiriusProjectSpaceInstance instance = space.importInstanceWithUniqueId(experiment, false);
            importedCompounds.add(instance);
            final CompoundContainer compoundContainer = instance.loadCompoundContainer(LCMSPeakInformation.class);
            compoundContainer.setAnnotation(LCMSPeakInformation.class, lcmsPeakInformation);
            instance.updateCompound(compoundContainer, LCMSPeakInformation.class);
            instance.clearCompoundCache(); //clear cache after storage since we keep the instance.
            feature2Instance.put(feature, instance);
            updateProgress(0, consensusFeatures.length, ++progress, "Write project space.");
        }
        if (i.getInternalStatistics().isPresent() && statistics != null) {
            i.getInternalStatistics().get().collectFromSummary(allSummaries);
            try (final JsonGenerator generator = new MappingJsonFactory().createGenerator(Files.newOutputStream(statistics), JsonEncoding.UTF8)) {
                generator.writeObject(i.getInternalStatistics().get());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        // add connection tables
        {
            final MolecularNetwork.NetworkBuilder network = new MolecularNetwork.NetworkBuilder();
            for (Instance inst : feature2Instance.values()) {
                network.addNode(inst.getId(), inst.getIonMass());
            }
            final Object2FloatOpenHashMap<Instance> others = new Object2FloatOpenHashMap<>();
            for (Map.Entry<ConsensusFeature, Instance> entry : feature2Instance.entrySet()) {
                final NetworkNode left = network.getNode(entry.getValue().getId());
                others.clear();
                for (IonConnection<ConsensusFeature> connection : entry.getKey().getConnections()) {
                    final Instance other = feature2Instance.get(connection.getRight());
                    if (other != null && connection.getType() == IonConnection.ConnectionType.IN_SOURCE_OR_ADDUCT) {
                        float prev = others.get(other);
                        others.put(other, Math.max(prev, connection.getWeight()));
                    }
                }
                others.forEach((key, weight) -> {
                    final NetworkNode right = network.getNode(key.getId());
                    if (left.getVertexId() < right.getVertexId()) {
                        network.addEdge(left.getVertexId(), right.getVertexId(), new Correlation(weight));
                    }
                });

            }
            final MolecularNetwork M = network.done(true);
            final ConnectionTable[] connectionTables = M.toConnectionTables();
            for (ConnectionTable t : connectionTables) {
                space.findInstance(t.id).ifPresent(x -> {
                    final CompoundContainer c = x.loadCompoundContainer();
                    c.setAnnotation(ConnectionTable.class, t);
                    x.updateCompound(c, ConnectionTable.class);
                });
            }
        }
        return space;
    }

    private BasicJJob<ProcessedSample> processRunJob(LCMSProccessingInstance instance, String filename) {
        return new BasicJJob<ProcessedSample>() {
            @Override
            protected ProcessedSample compute() throws Exception {
                try {
                    final MemoryFileStorage storage = new MemoryFileStorage();
                    System.out.println("parse file " + filename);
                    LCMSRun run = LCMSParsing.parseRun(workingDir.resolve(filename).toUri(), storage);
                    System.out.println("Start processing");
                    final ProcessedSample pr = instance.addSample(run, storage, false);
                    System.out.println("Finish processing");
                    storage.backOnDisc();
                    storage.dropBuffer();
                    return pr;
                } catch (IOException | InvalidInputData e) {
                    LoggerFactory.getLogger(LcmsAlignSubToolJobSiriusPs.class).error(e.getMessage(), e);
                    throw new RuntimeException("Stop processing");
                }
            }
        };
    }

    public List<SiriusProjectSpaceInstance> getImportedCompounds() {
        return importedCompounds;
    }

    private static boolean isInvalidExp(Ms2Experiment exp) {
        return exp.getMs2Spectra() == null || exp.getMs2Spectra().isEmpty() ||
                exp.getPrecursorIonType() == null ||
                exp.getIonMass() == 0d;
    }
}


