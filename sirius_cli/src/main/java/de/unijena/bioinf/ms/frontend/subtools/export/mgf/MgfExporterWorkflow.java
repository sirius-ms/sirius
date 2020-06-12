package de.unijena.bioinf.ms.frontend.subtools.export.mgf;

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Quantification;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.mgf.MgfWriter;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import org.apache.commons.text.translate.CsvTranslators;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Standalone-Tool to export spectra to mgf format.
 */
public class MgfExporterWorkflow implements Workflow {
    private final Path outputPath;
    private final MgfWriter mgfWriter;
    private final PreprocessingJob<ProjectSpaceManager> ppj;
    private final Optional<Path> quantPath;


    public MgfExporterWorkflow(PreprocessingJob<ProjectSpaceManager> ppj, MgfExporterOptions options, ParameterConfig config) {
        outputPath = options.output;
        Deviation mergeMs2Deviation = new Deviation(options.ppmDev);
        mgfWriter = new MgfWriter(options.writeMs1, options.mergeMs2, mergeMs2Deviation);
        this.ppj = ppj;
        this.quantPath = Optional.ofNullable(options.quantTable).map(File::toPath);
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
            quantPath.ifPresent(path-> {
                try {
                    writeQuantifiactionTable(ps, path);
                } catch (IOException e) {
                    LoggerFactory.getLogger(MgfExporterWorkflow.class).error(e.getMessage(),e);
                }
            });
        } catch (ExecutionException e) {
            LoggerFactory.getLogger(getClass()).error("Error when reading input project!", e);
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Error when writing the MGF file to: " + outputPath.toString(), e);
        }
    }

    private void writeQuantifiactionTable(ProjectSpaceManager ps, Path path) throws IOException {
        final HashMap<String, double[]> compounds = new HashMap<>();
        final ArrayList<String> samples = new ArrayList<>();
        final HashMap<String, Integer> sampleNames = new HashMap<>();

        try (BufferedWriter bw = FileUtils.getWriter(path.toFile())) {
            for (Instance i : ps) {
                final Ms2Experiment experiment = i.getExperiment();
                // TODO: this will change when quants are not longer written into ms files!!!
                experiment.getAnnotation(Quantification.class).ifPresent(quant->{
                    for (String s : quant.getSamples()) {
                        if (!sampleNames.containsKey(s)) {
                            sampleNames.put(s, samples.size());
                            samples.add(s);
                        }
                    }
                    final double[] vec = new double[samples.size() + 2];
                    int j=0;
                    vec[j++] = experiment.getIonMass();
                    vec[j++] = experiment.getAnnotation(RetentionTime.class).orElse(new RetentionTime(0d)).getRetentionTimeInSeconds();

                    for (String n : samples) {
                        vec[j++] = quant.getQuantificationFor(n);
                    }
                    compounds.put(experiment.getName(), vec);
                });
            }
            // now write data
            ArrayList<String> compoundNames = new ArrayList<>(compounds.keySet());
            Collections.sort(compoundNames);
            ArrayList<String> sampleNameList = new ArrayList<>(samples);
            Collections.sort(sampleNameList);
            bw.write("row ID,row m/z,row retention time");
            CsvTranslators.CsvEscaper escaper = new CsvTranslators.CsvEscaper();
            for (String sample : sampleNameList) {
                bw.write(",");
                escaper.translate(sample,bw);
            }
            bw.newLine();
            for (String compoundId : compoundNames) {
                final double[] vector = compounds.get(compoundId);
                escaper.translate(compoundId);
                bw.write(",");
                bw.write(String.valueOf(vector[0]));
                bw.write(",");
                bw.write(String.valueOf(vector[1]));
                for (String sampleName : sampleNameList) {
                    bw.write(',');
                    bw.write(String.valueOf(vector[sampleNames.get(sampleName)+2]));
                }
                bw.newLine();
            }
        }
    }
}
