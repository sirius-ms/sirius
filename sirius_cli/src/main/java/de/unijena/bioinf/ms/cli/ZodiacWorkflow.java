package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.GibbsSampling.ZodiacUtils;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.GibbsSampling.model.distributions.*;
import de.unijena.bioinf.GibbsSampling.model.scorer.CommonFragmentAndLossScorer;
import de.unijena.bioinf.GibbsSampling.model.scorer.EdgeScorings;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.projectspace.ExperimentResult;
import de.unijena.bioinf.sirius.projectspace.ExperimentResultJJob;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class ZodiacWorkflow implements Workflow<ExperimentResult> {

    private static final  org.slf4j.Logger LOG = LoggerFactory.getLogger(Zodiac.class);
    private ZodiacOptions options;

    private ZodiacInstanceProcessor zodiacIP;

    @Override
    public boolean setup() {
        zodiacIP.setup();
        return true;
    }

    @Override
    public boolean validate() {
        zodiacIP.validate();
        return true;
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
            allExperimentResults = zodiacIP.updateQuality(allExperimentResults, originalSpectraPath);
        } catch (IOException e) {
            LOG.error("IOException while estimating data quality.", e);
            return;
        }
        ZodiacJJob zodiacJJob = zodiacIP.makeZodiacJob(allExperimentResults);

        try {
            ZodiacResultsWithClusters zodiacResults = SiriusJobs.getGlobalJobManager().submitJob(zodiacJJob).awaitResult();
            zodiacIP.writeResults(allExperimentResults, zodiacResults);
        } catch (ExecutionException e) {
            LOG.error("An error occurred while running ZODIAC.", e);
        }catch (IOException e) {
            LOG.error("Error writing ZODIAC output.", e);

        }
    }


}
