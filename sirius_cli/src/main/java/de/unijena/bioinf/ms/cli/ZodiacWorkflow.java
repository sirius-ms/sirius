package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.GibbsSampling.model.ZodiacResultsWithClusters;
import de.unijena.bioinf.sirius.projectspace.ExperimentResult;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ZodiacWorkflow implements Workflow<ExperimentResult> {

    private static final  org.slf4j.Logger LOG = LoggerFactory.getLogger(Zodiac.class);
    private ZodiacOptions options;

    private ZodiacInstanceProcessor zodiacIP;

    @Override
    public boolean setup() {
        return zodiacIP.setup();
    }

    @Override
    public boolean validate() {
        return zodiacIP.validate();
    }


    public ZodiacWorkflow(ZodiacOptions options) {
        this.options = options;
        this.zodiacIP = new ZodiacInstanceProcessor(options);
    }

    @Override
    public void compute(Iterator<ExperimentResult> experimentResultIterator) {
        Path originalSpectraPath = Paths.get(options.getSpectraFile());

        List<ExperimentResult> allExperimentResults = new ArrayList<>();
        while (experimentResultIterator.hasNext()) {
            allExperimentResults.add(experimentResultIterator.next());
        }
        //todo reads original experiments twice!
        try {
            Path outputPath = Paths.get(options.getOutput());
            if (options.getIsolationWindowWidth()!=null){
                double width = options.getIsolationWindowWidth();
                double shift = options.getIsolationWindowShift();
                allExperimentResults = zodiacIP.updateQuality(allExperimentResults, originalSpectraPath, width, shift, outputPath);
            } else {
                allExperimentResults = zodiacIP.updateQuality(allExperimentResults, originalSpectraPath, -1d, -1d, outputPath);
            }

        } catch (IOException e) {
            LOG.error("IOException while estimating data quality.", e);
            return;
        }

        if (options.isOnlyComputeStats()){
            return;
        }

        ZodiacJJob zodiacJJob = zodiacIP.makeZodiacJob(allExperimentResults);

        try {
            ZodiacResultsWithClusters zodiacResults = SiriusJobs.getGlobalJobManager().submitJob(zodiacJJob).awaitResult();
            if (zodiacResults==null) return; //no results. likely, empty input
            zodiacIP.writeResults(allExperimentResults, zodiacResults);
        } catch (ExecutionException e) {
            LOG.error("An error occurred while running ZODIAC.", e);
        }catch (IOException e) {
            LOG.error("Error writing ZODIAC output.", e);

        }
    }


}
