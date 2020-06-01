package de.unijena.bioinf.ms.frontend.subtools.lcms_align;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.CompoundQuality;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MultipleSources;
import de.unijena.bioinf.ChemistryBase.ms.SpectrumFileSource;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.io.lcms.LCMSParsing;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.MemoryFileStorage;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.lcms.align.Cluster;
import de.unijena.bioinf.model.lcms.ConsensusFeature;
import de.unijena.bioinf.model.lcms.LCMSRun;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
        i.setDetectableIonTypes(PropertyManager.DEFAULTS.createInstanceWithDefaults(AdductSettings.class).getDetectable());
        final List<Path> files = input.msInput.msParserfiles.stream().sorted().collect(Collectors.toList());
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
                        e.printStackTrace();
                        throw new RuntimeException(e);

                    }
                    return "";
                }
            }));
        }
        MultipleSources sourcelocation = MultipleSources.leastCommonAncestor(input.getAllFilesStream().map(Path::toFile).toArray(File[]::new));
        for (BasicJJob<?> j : jobs) j.takeResult();
        i.getMs2Storage().backOnDisc();
        i.getMs2Storage().dropBuffer();
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
        for (ConsensusFeature feature : consensusFeatures) {
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
            importedCompounds.add(space.newCompoundWithUniqueId(experiment).getID());
            updateProgress(0, consensusFeatures.length, ++progress, "Write project space.");
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


