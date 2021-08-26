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
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.io.lcms.LCMSParsing;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.MemoryFileStorage;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.lcms.align.Cluster;
import de.unijena.bioinf.model.lcms.ConsensusFeature;
import de.unijena.bioinf.model.lcms.IonConnection;
import de.unijena.bioinf.model.lcms.LCMSRun;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.networks.Correlation;
import de.unijena.bioinf.networks.MolecularNetwork;
import de.unijena.bioinf.networks.NetworkNode;
import de.unijena.bioinf.networks.serialization.ConnectionTable;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.projectspace.CompoundContainer;
import gnu.trove.map.hash.TObjectFloatHashMap;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LcmsAlignSubToolJob extends PreprocessingJob<ProjectSpaceManager> {
    protected final InputFilesOptions input;
    protected final ProjectSpaceManager space;
    protected final List<CompoundContainerId> importedCompounds = new ArrayList<>();
    public LcmsAlignSubToolJob(RootOptions<?, ?, ?> rootCLI) {
        this(rootCLI.getInput(), rootCLI.getProjectSpace());
    }

    public LcmsAlignSubToolJob(InputFilesOptions input, ProjectSpaceManager space) {
        super();
        this.input = input;
        this.space = space;
    }

    @Override
    protected ProjectSpaceManager compute() throws Exception {
        importedCompounds.clear();
        final ArrayList<BasicJJob<?>> jobs = new ArrayList<>();
        final LCMSProccessingInstance i = new LCMSProccessingInstance();
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
                ++totalFeatures;
            if (experiment.getAnnotation(CompoundQuality.class, CompoundQuality::new).isNotBadQuality()) {
                ++goodFeatures;
            }
            // set name to common prefix
            // kaidu: this is super slow, so we just ignore the filename
            experiment.setAnnotation(SpectrumFileSource.class, new SpectrumFileSource(sourcelocation.value));
            final Instance compound = space.newCompoundWithUniqueId(experiment);
            importedCompounds.add(compound.getID());
            final CompoundContainer compoundContainer = compound.loadCompoundContainer(LCMSPeakInformation.class);
            compoundContainer.setAnnotation(LCMSPeakInformation.class, feature.getLCMSPeakInformation());
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

    public List<CompoundContainerId> getImportedCompounds() {
        return importedCompounds;
    }

    private static boolean isInvalidExp(Ms2Experiment exp) {
        return exp.getMs2Spectra() == null || exp.getMs2Spectra().isEmpty() ||
                exp.getPrecursorIonType() == null ||
                exp.getIonMass() == 0d;
    }
}


