package de.unijena.bioinf.ms.frontend.subtools.export.mgf;

import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Options for the mgf exporter sub-tool.
 */
@CommandLine.Command(name = "mgf-export", aliases = {"MGF"}, description = "<STANDALONE> Exports the spectra of a given input as mgf.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, sortOptions = false)
public class MgfExporterOptions implements StandaloneTool<MgfExporterWorkflow> {
    protected Path output = null;
    protected double ppmDev = Double.NaN;

    public MgfExporterOptions() {
    }

    @CommandLine.Option(names = "--write-ms1", description = "Write MS1 spectra into file.")
    public boolean writeMs1;

    @CommandLine.Option(names = "--merge-ms2", description = "Merge all MS2 of a compound into one single spectrum.")
    public boolean mergeMs2;

    @CommandLine.Option(names = "--quant-table", description = "Quantification table file name for Feature Based Molecular Networking.")
    public File quantTable;

    @CommandLine.Option(names = "--merge-ppm", description = "Maximum allowed deviation (in ppm) for peaks of MS2 spectra to be merged.", defaultValue = "10")
    public void setMergePpm(DefaultParameter value) throws Exception {
        this.ppmDev = value.asDouble();
    }

    //fewer parameter is probably better // todo hidden then?
//    @CommandLine.Option(names = "--merge-abs", description = "Maximum allowed absolute difference for peaks of MS2 spectra to be merged.", defaultValue = "0.005")
//    public void setMergeAbs(DefaultParameter value) throws Exception {
//        this.absDev = value.asDouble();
//    }


    @CommandLine.Option(names = {"--output"}, description = "Specify the mgf file destination.")
    public void setOutput(String outputPath) {
        output = Paths.get(outputPath);
    }


    @Override
    public MgfExporterWorkflow makeWorkflow(RootOptions<?, ?, ?> rootOptions, ParameterConfig config) {
        return new MgfExporterWorkflow((PreprocessingJob<ProjectSpaceManager>) rootOptions.makeDefaultPreprocessingJob(), this, config);
    }
}
