package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.jjobs.MasterJJob;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MSExperimentParsingJJob extends MasterJJob<List<Ms2Experiment>> {
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
        Iterator<JJob> it = subJobs.iterator();
        //we do not want to check for interruption within the subjob iteration hence this is checked by the subjobs
        int fileindex = 0;
        while (it.hasNext()) {
            JJob<List<Ms2Experiment>> subJob = (JJob<List<Ms2Experiment>>) it.next();
            List<Ms2Experiment> r = null; //todo do we want to fail if one import fails or skip it??
            try {
                r = subJob.awaitResult();
                if (r == null || r.isEmpty()) {
                    LoggerFactory.getLogger(MSExperimentParsingJJob.class).warn(inputFiles.get(fileindex).getName() + " contains no Spectra!");
                } else {
                    results.addAll(r);
                }
            } catch (Throwable e) {
                LoggerFactory.getLogger(MSExperimentParsingJJob.class).error("Error parsing file (" + inputFiles.get(fileindex).getName() + "). No spectra returned", e);
            } finally {
                removeSubJob(subJob);
            }
            fileindex++;
        }

        checkForInterruption();

        return results;
    }

    @Override
    protected JobManager jobManager() {
        return SiriusJobs.getGlobalJobManager();
    }
}
