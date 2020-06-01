package de.unijena.bioinf.ms.frontend.subtools.export.mgf;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.babelms.mgf.MgfWriter;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

/**
 * Standalone-Tool to export spectra to mgf format.
 */
public class MgfExporterWorkflow implements Workflow {
    private final Path outputPath;
    private final MgfWriter mgfWriter;
    private final PreprocessingJob<ProjectSpaceManager> ppj;


    public MgfExporterWorkflow(PreprocessingJob<ProjectSpaceManager> ppj, MgfExporterOptions options, ParameterConfig config) {
        outputPath = options.output;
        Deviation mergeMs2Deviation = new Deviation(options.ppmDev);
        mgfWriter = new MgfWriter(options.writeMs1, options.mergeMs2, mergeMs2Deviation);
        this.ppj = ppj;
    }

    @Override
    public void run() {
        try {
            final ProjectSpaceManager ps = SiriusJobs.getGlobalJobManager().submitJob(ppj).awaitResult();
            try (final BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
                for (Instance inst : ps){
                    try {
                        mgfWriter.write(writer, inst.getExperiment());
                    } catch (IOException e) {
                        throw e;
                    } catch (Exception e) {
                        LoggerFactory.getLogger(getClass()).warn("Invalid instance '" + inst.getID() + "'. Skipping this instance!", e);
                    } finally {
                        inst.clearCompoundCache();
                        inst.clearFormulaResultsCache();
                    }
                }
            }
        } catch (ExecutionException e) {
            LoggerFactory.getLogger(getClass()).error("Error when reading input project!", e);
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Error when writing the MGF file to: " + outputPath.toString(), e);
        }
    }
}
