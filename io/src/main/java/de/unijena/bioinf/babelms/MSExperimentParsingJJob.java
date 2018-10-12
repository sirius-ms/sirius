package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MSExperimentParsingJJob extends BasicMasterJJob<List<Ms2Experiment>> {
    private final List<File> inputFiles;

    public MSExperimentParsingJJob(List<File> filesToParse) {
        super(JobType.IO);
        this.inputFiles = filesToParse;
    }

    @Override
    protected List<Ms2Experiment> compute() throws Exception {
        MsExperimentParser p = new MsExperimentParser();
        checkForInterruption();
        for (File file : inputFiles) {
            submitSubJob(new GenericParserJJob<>(p.getParser(file), file));
        }

        checkForInterruption();

        List<Ms2Experiment> results = new LinkedList<>();
//        Iterator<JJob> it = subJobs.iterator();
        //we do not want to check for interruption within the subjob iteration hence this is checked by the subjobs
        final AtomicInteger fileindex = new AtomicInteger(0);
        forEachSubJob((it) -> {
            JJob<List<Ms2Experiment>> subJob = (JJob<List<Ms2Experiment>>) it;
            List<Ms2Experiment> r = null; //todo do we want to fail if one import fails or skip it??
            try {
                r = subJob.awaitResult();
                if (r == null || r.isEmpty()) {
                    LOG().warn(inputFiles.get(fileindex.get()).getName() + " contains no Spectra!");
                } else {
                    results.addAll(r);
                }
            } catch (Exception e) {
                LOG().error("Error parsing file (" + inputFiles.get(fileindex.get()).getName() + "). No spectra resurned", e);
            }
            fileindex.incrementAndGet();
        });
        //todo it would be nice to remove the subjobs to save memory
        checkForInterruption();

        return results;
    }
}
