package de.unijena.bioinf.ms.frontend.subtools.lcms_align;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.babelms.ProjectSpaceManager;
import de.unijena.bioinf.io.lcms.MzXMLParser;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.MemoryFileStorage;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.model.lcms.ConsensusFeature;
import de.unijena.bioinf.model.lcms.LCMSRun;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LcmsAlignSubToolJob extends PreprocessingJob {

    public LcmsAlignSubToolJob(@Nullable List<File> input, @Nullable ProjectSpaceManager space) {
        super(input, space);
    }

    @Override
    protected ProjectSpaceManager compute() throws Exception {
        final ArrayList<BasicJJob> jobs = new ArrayList<>();
        final LCMSProccessingInstance i = new LCMSProccessingInstance();
        for (File f : input) {
            jobs.add(SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<>() {
                @Override
                protected Object compute() throws Exception {
                    try {
                        MemoryFileStorage storage = new MemoryFileStorage();
                        final LCMSRun parse = new MzXMLParser().parse(f, storage);
                        final ProcessedSample sample = i.addSample(parse, storage);
                        i.detectFeatures(sample);
                        storage.backOnDisc();
                        storage.dropBuffer();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    return "";
                }
            }));
        }
        for (BasicJJob j : jobs) j.takeResult();
        i.getMs2Storage().backOnDisc();
        i.getMs2Storage().dropBuffer();
        final ConsensusFeature[] consensusFeatures = i.makeConsensusFeatures(i.alignAndGapFilling());
        LOG().info("Gapfilling Done.");

        //save to project space
        Arrays.stream(consensusFeatures).forEach(cons -> space.newUniqueCompoundId(cons.toMs2Experiment()));
        return space;
    }
}
