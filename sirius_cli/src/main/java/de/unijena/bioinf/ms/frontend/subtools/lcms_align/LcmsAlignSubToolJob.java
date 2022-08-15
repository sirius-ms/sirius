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

import de.unijena.bioinf.ChemistryBase.exceptions.InvalidInputData;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.CompoundQuality;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MultipleSources;
import de.unijena.bioinf.ChemistryBase.ms.SpectrumFileSource;
import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.ChemistryBase.ms.lcms.workflows.LCMSWorkflow;
import de.unijena.bioinf.ChemistryBase.ms.lcms.workflows.MixedWorkflow;
import de.unijena.bioinf.ChemistryBase.ms.lcms.workflows.PooledMs2Workflow;
import de.unijena.bioinf.ChemistryBase.ms.lcms.workflows.RemappingWorkflow;
import de.unijena.bioinf.babelms.ms.InputFileConfig;
import de.unijena.bioinf.io.lcms.LCMSParsing;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.lcms.*;
import de.unijena.bioinf.lcms.align.Aligner;
import de.unijena.bioinf.lcms.align.Aligner2;
import de.unijena.bioinf.lcms.align.Cluster;
import de.unijena.bioinf.model.lcms.ConsensusFeature;
import de.unijena.bioinf.model.lcms.IonConnection;
import de.unijena.bioinf.model.lcms.LCMSRun;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.networks.Correlation;
import de.unijena.bioinf.networks.MolecularNetwork;
import de.unijena.bioinf.networks.NetworkNode;
import de.unijena.bioinf.networks.serialization.ConnectionTable;
import de.unijena.bioinf.projectspace.*;
import gnu.trove.map.hash.TObjectFloatHashMap;
import org.apache.commons.math3.distribution.RealDistribution;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LcmsAlignSubToolJob extends PreprocessingJob<ProjectSpaceManager<?>> {
    protected final InputFilesOptions input;
    protected final ParameterConfig config;
    protected final ProjectSpaceManager<?> space;
    protected final LcmsAlignOptions options;
    protected final List<CompoundContainerId> importedCompounds = new ArrayList<>();

    public LcmsAlignSubToolJob(InputFilesOptions input, ProjectSpaceManager<?> space, ParameterConfig config, LcmsAlignOptions options) {
        super();
        this.config = config;
        this.input = input;
        this.space = space;
        this.options = options;
    }

    @Override
    protected ProjectSpaceManager<?> compute() throws Exception {
        importedCompounds.clear();
        final ArrayList<BasicJJob<?>> jobs = new ArrayList<>();
        final LCMSProccessingInstance i = new LCMSProccessingInstance();

        final Optional<LCMSWorkflow> workflow = options.getWorkflow();
        if (workflow.isPresent()) {
            return computeWorkflow(workflow.get());
        }
        LoggerFactory.getLogger(LcmsAlignSubToolJob.class).warn("No workflow specified. Use 'default' workflow: mixed-mode with alignment.");


        //i.setDetectableIonTypes(PropertyManager.DEFAULTS.createInstanceWithDefaults(AdductSettings.class).getDetectable());
        final List<Path> files = input.msInput.msParserfiles.keySet().stream().sorted().collect(Collectors.toList());
        updateProgress(0, files.size(), 1, "Parse LC/MS runs");
        AtomicInteger counter = new AtomicInteger(0);
        for (Path f : files) {
            jobs.add(SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<>() {
                @Override
                protected Object compute() {
                    try {
                        MemoryFileStorage storage = new MemoryFileStorage();
                        final LCMSRun parse = LCMSParsing.parseRun(f.toFile(), storage);
                        final ProcessedSample sample = i.addSample(parse, storage);
                        i.detectFeatures(sample);
                        storage.backOnDisc();
                        storage.dropBuffer();
                        final int c = counter.incrementAndGet();
                        LcmsAlignSubToolJob.this.updateProgress(0, files.size(), c, "Parse LC/MS runs");
                    } catch (Throwable e) {
                        LoggerFactory.getLogger(LcmsAlignSubToolJob.class).error("Error while parsing file '" + f + "': " + e.getMessage());
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
        MultipleSources sourcelocation = MultipleSources.leastCommonAncestor(input.getAllFilesStream().map(Path::toFile).toArray(File[]::new));
        for (BasicJJob<?> j : jobs) j.takeResult();
        i.getMs2Storage().backOnDisc();
        i.getMs2Storage().dropBuffer();

        if (i.getSamples().size()==0) {
            LoggerFactory.getLogger(LcmsAlignSubToolJob.class).error("No input data available to be aligned.");
            return space;
        }

        Cluster alignment = i.alignAndGapFilling(this);
        updateProgress(0, 2, 0, "Assign adducts.");
        i.detectAdductsWithGibbsSampling(alignment);
        updateProgress(0, 2, 1, "Merge features.");
        alignment = alignment.deleteDuplicateRows();
        return importIntoProjectSpace(i,alignment,sourcelocation);
    }

    private ProjectSpaceManager<?> computeWorkflow(LCMSWorkflow lcmsWorkflow) {
        if (lcmsWorkflow instanceof PooledMs2Workflow) {
            return computePooledWorkflow((PooledMs2Workflow) lcmsWorkflow);
        } else if (lcmsWorkflow instanceof MixedWorkflow) {
            return computeMixedWorkflow((MixedWorkflow) lcmsWorkflow);
        } else if (lcmsWorkflow instanceof RemappingWorkflow) {
            return computeRemappingWorkflow((RemappingWorkflow) lcmsWorkflow);
        } else throw new IllegalArgumentException("Unknown workflow: " + lcmsWorkflow.getClass().getName());
    }

    private ProjectSpaceManager<?> computeMixedWorkflow(MixedWorkflow lcmsWorkflow) {
        return null;
    }

    private ProjectSpaceManager<?> computePooledWorkflow(PooledMs2Workflow lcmsWorkflow) {
        final LCMSProccessingInstance instance = new LCMSProccessingInstance();
        // read all files
        final JobManager jm = SiriusJobs.getGlobalJobManager();
        final ProcessedSample[] ms2Samples = Arrays.stream(lcmsWorkflow.getPooledMs2()).map(filename->jm.submitJob(processRunJob(instance,filename))).collect(Collectors.toList()).stream().map(JJob::takeResult).toArray(ProcessedSample[]::new);
        System.out.println("MS2 DONE");
        final ProcessedSample[] ms1Samples = Arrays.stream(lcmsWorkflow.getPooledMs1()).map(filename->jm.submitJob(processRunJob(instance,filename))).collect(Collectors.toList()).stream().map(JJob::takeResult).toArray(ProcessedSample[]::new);
        final ProcessedSample[] remainingSamples = Arrays.stream(lcmsWorkflow.getRemainingMs1()).map(filename->jm.submitJob(processRunJob(instance,filename))).collect(Collectors.toList()).stream().map(JJob::takeResult).toArray(ProcessedSample[]::new);
        if (ms1Samples.length>1) {
            LoggerFactory.getLogger(LcmsAlignSubToolJob.class).warn("Multiple pooled MS1 samples are not supported yet. We will just process the first one.");
        }
        // now merge ms2 into ms1
        final Ms1Ms2Pairing ms1Ms2Pairing = new Ms1Ms2Pairing(ms1Samples[0], ms2Samples);
        ms1Ms2Pairing.run(instance);
        // attach remaining ms1
        RealDistribution error = ms1Ms2Pairing.attachRemainingMs1(instance, remainingSamples);
        // start alignment
        int deleted = jm.submitJob(new Aligner(false).prealignAndFeatureCutoff2(instance.getSamples(), new Aligner2(error).maxRetentionError(), 1)).takeResult();
        Cluster cluster = jm.submitJob(new Aligner2(error).align(instance.getSamples())).takeResult().deleteRowsWithNoMsMs().deleteDuplicateRows();
        instance.detectAdductsWithGibbsSampling(cluster);
        cluster=cluster.deleteDuplicateRows();
        final MultipleSources sourcelocation = MultipleSources.leastCommonAncestor(Arrays.stream(lcmsWorkflow.getPooledMs2()).map(File::new).toArray(File[]::new));
        return importIntoProjectSpace(instance, cluster, sourcelocation);
    }


    private ProjectSpaceManager<?> computeRemappingWorkflow(RemappingWorkflow lcmsWorkflow) {
        final LCMSProccessingInstance instance = new LCMSProccessingInstance();
        // read all files
        final JobManager jm = SiriusJobs.getGlobalJobManager();
        final ProcessedSample[] ms1Samples = Arrays.stream(lcmsWorkflow.getFiles()).map(filename->jm.submitJob(processRunJob(instance,filename))).collect(Collectors.toList()).stream().map(JJob::takeResult).toArray(ProcessedSample[]::new);
        final Iterator<CompoundContainer> compoundContainerIterator = space.projectSpace().compoundIterator(LCMSPeakInformation.class, Ms2Experiment.class);
        final List<Ms2Experiment> exps = new ArrayList<>();
        final List<LCMSPeakInformation> peaks = new ArrayList<>();
        final List<CompoundContainerId> ids = new ArrayList<>();
        while (compoundContainerIterator.hasNext()) {
            final CompoundContainer next = compoundContainerIterator.next();
            if (next.getAnnotation(Ms2Experiment.class).isEmpty() || next.getAnnotation(LCMSPeakInformation.class).isEmpty()) continue;
            exps.add(next.getAnnotation(Ms2Experiment.class).get());
            peaks.add(next.getAnnotation(LCMSPeakInformation.class).get());
            ids.add(next.getId());
        }
        LCMSPeakInformation[] replaced = Ms1Remapping.remapMS1(instance, ms1Samples, peaks.toArray(LCMSPeakInformation[]::new), exps.toArray(Ms2Experiment[]::new), true);
        for (int i=0; i < ids.size(); ++i) {
            final CompoundContainerId compoundContainerId = ids.get(i);
            final CompoundContainer compound;
            try {
                compound = space.projectSpace().getCompound(compoundContainerId, Ms2Experiment.class, LCMSPeakInformation.class);
                compound.setAnnotation(LCMSPeakInformation.class, replaced[i]);
                space.projectSpace().updateCompound(compound, LCMSPeakInformation.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return space;
    }

    private ProjectSpaceManager<?> importIntoProjectSpace(LCMSProccessingInstance i, Cluster alignment, MultipleSources sourcelocation) {
        final ConsensusFeature[] consensusFeatures = i.makeConsensusFeatures(alignment);
        logInfo(consensusFeatures.length + "Feature left after merging.");

        int totalFeatures = 0, goodFeatures = 0;
        //save
        updateProgress(0, consensusFeatures.length, 0, "Write project space.");
        int progress=0;
        final HashMap<ConsensusFeature, CompoundContainerId> feature2compoundId = new HashMap<>();
        for (int K=0; K  < consensusFeatures.length; ++K) {
            final ConsensusFeature feature = consensusFeatures[K];
            final Ms2Experiment experiment = feature.toMs2Experiment();
            if (isInvalidExp(experiment)){
                LoggerFactory.getLogger(getClass()).warn("Skipping invalid experiment '" + experiment.getName() + "'.");
                continue;
            }
            final LCMSPeakInformation lcmsPeakInformation = feature.getLCMSPeakInformation();
            // set quality flags
            {
                // just look at the top 5 most intensive samples
                List<Integer> indizes = new ArrayList<>();
                for (int k=0; k < lcmsPeakInformation.length(); ++k) {
                    if (lcmsPeakInformation.getTracesFor(k).isPresent()) indizes.add(k);
                }
                indizes.sort(Comparator.comparingDouble(lcmsPeakInformation::getIntensityOf));
                Collections.reverse(indizes);
                boolean badPeakShape = true;
                for (int k=0; k < Math.min(indizes.size(),5); ++k) {
                    final CoelutingTraceSet traceSet = lcmsPeakInformation.getTracesFor(k).get();
                    LCMSCompoundSummary summary = new LCMSCompoundSummary(traceSet, traceSet.getIonTrace(), experiment);
                    if (summary.peakQuality.ordinal() > LCMSCompoundSummary.Quality.LOW.ordinal()) {
                        badPeakShape = false;
                        break;
                    }
                }
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

            materializeProperties(experiment);

            final Instance compound = space.newCompoundWithUniqueId(experiment);
            importedCompounds.add(compound.getID());
            final CompoundContainer compoundContainer = compound.loadCompoundContainer(LCMSPeakInformation.class);
            compoundContainer.setAnnotation(LCMSPeakInformation.class, lcmsPeakInformation);
            compound.updateCompound(compoundContainer,LCMSPeakInformation.class);

            feature2compoundId.put(feature, compound.getID());
            updateProgress(0, consensusFeatures.length, ++progress, "Write project space.");
        }
        // add connection tables
        {
            final MolecularNetwork.NetworkBuilder network = new MolecularNetwork.NetworkBuilder();
            for (CompoundContainerId id : feature2compoundId.values()) {
                network.addNode(id.getDirectoryName(), id.getIonMass().orElse(0d));
            }
            final TObjectFloatHashMap<CompoundContainerId> others = new TObjectFloatHashMap<>(5, 0.75f, 0f);
            for (Map.Entry<ConsensusFeature, CompoundContainerId> entry : feature2compoundId.entrySet()) {
                final NetworkNode left = network.getNode(entry.getValue().getDirectoryName());
                others.clear();
                for (IonConnection<ConsensusFeature> connection : entry.getKey().getConnections()) {
                    final CompoundContainerId other = feature2compoundId.get(connection.getRight());
                    if (other!=null && connection.getType()== IonConnection.ConnectionType.IN_SOURCE_OR_ADDUCT) {
                        float prev = others.get(other);
                        others.put(other, Math.max(prev, connection.getWeight()));
                    }
                }
                others.forEachEntry((key,weight)->{
                    final NetworkNode right = network.getNode(key.getDirectoryName());
                    if (left.getVertexId() < right.getVertexId()) {
                        network.addEdge(left.getVertexId(),right.getVertexId(), new Correlation(weight));
                    }
                    return true;
                });

            }
            final MolecularNetwork M = network.done(true);
            final ConnectionTable[] connectionTables = M.toConnectionTables();
            for (ConnectionTable t : connectionTables) {
                final SiriusProjectSpace ps = space.projectSpace();
                ps.findCompound(t.id).ifPresent(x->{
                    try {
                        final CompoundContainer c = ps.getCompound(x);
                        c.setAnnotation(ConnectionTable.class, t);
                        ps.updateCompound(c, ConnectionTable.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
                    LCMSRun run = LCMSParsing.parseRun(new File(filename), storage);
                    System.out.println("Start processing");
                    final ProcessedSample pr = instance.addSample(run, storage, false);
                    System.out.println("Finish processing");
                    storage.backOnDisc();
                    storage.dropBuffer();
                    return pr;
                } catch (IOException | InvalidInputData e) {
                    LoggerFactory.getLogger(LcmsAlignSubToolJob.class).error(e.getMessage(),e);
                    throw new RuntimeException("Stop processing");
                }
            }
        };
    }

    protected void materializeProperties(Ms2Experiment experiment) {
        // TODO: @Markus: what can we do if config is null??
        if (config==null)  {

        } else {
            final ParameterConfig parameterConfig = config.newIndependentInstance("LCMS-ALIGN", true);
            parameterConfig.changeConfig("CompoundQuality", experiment.getAnnotation(CompoundQuality.class).orElse(new CompoundQuality()).toString());
            // TODO: MsInstrumentation is missing
            experiment.setAnnotation(InputFileConfig.class, new InputFileConfig(parameterConfig));
        }
    }

    public List<CompoundContainerId> getImportedCompounds() {
        return importedCompounds;
    }

    private static boolean isInvalidExp(Ms2Experiment exp) {
        return exp.getMs2Spectra() == null || exp.getMs2Spectra().isEmpty() ||
                exp.getPrecursorIonType() == null ||
                exp.getIonMass() == 0d;
    }
}


