package de.unijena.bioinf.ms.frontend.subtools.harvester;

import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.ms.frontend.subtools.PostprocessingTool;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.subtools.summaries.SummarySubToolJob;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@CommandLine.Command(name = "harvest", description = "<STANDALONE, POSTPROCESSING> Collect spectra with confident annotations for self-training.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class HarvesterOptions implements PostprocessingTool<HarvesterWorkflow>, StandaloneTool<Workflow> {

    Path location;
    @CommandLine.Option(names = {"--output", "-o"}, required = true, description = "Specify the target directory for harvester to writing the collected spectra.")
    public void setSummaryLocation(Path summaryLocation) throws Exception {
        this.location = summaryLocation;
    }

    Path lipidPath;
    @CommandLine.Option(names = {"--lipids"}, required = true, description = "Export all spectra predicted to be lipids by el gordo to the specified location.")
    public void setLipidLocation(Path summaryLocation) throws Exception {
        this.lipidPath = summaryLocation;
    }

    Path fluorinePath;
    @CommandLine.Option(names = {"--fluorine"}, required = true, description = "Export all spectra predicted to be fluorine containing compounds to the specified location.")
    public void setFluorineLocation(Path summaryLocation) throws Exception {
        this.fluorinePath = summaryLocation;
    }

    Path locationForm=null;
    @CommandLine.Option(names = {"--output-confident-formulas"}, required = false, description = "If specified, Harvester will also export low confident annotations for which at least the molecular formula is higly confidently annotated")
    public void setConfidentFormulaSummaryLocation(Path summaryLocation) throws Exception {
        this.locationForm = summaryLocation;
    }



    @CommandLine.Option(names = "--high-confidence", required = false, defaultValue = "0.7", description = "Confidence Threshold for considering a compound as highly confidently annotated.")
    public float highConfidenceThreshold;

    @CommandLine.Option(names = "--min-confidence", required = false, defaultValue = "0.2", description = "Minimal confidence for a compound to be considered at all.")
    public float minConfidenceThreshold;

    @CommandLine.Option(names = "--high-tanimoto", required = false, defaultValue = "0.7", description = "Tanimoto Threshold for considering a compound as highly confidently annotated.")
    public float highTanimotoThreshold;

    @CommandLine.Option(names = "--min-tanimoto", required = false, defaultValue = "0.5", description = "Tanimoto Threshold for a compound to be considered at all.")
    public float minTanimotoThreshold;

    @CommandLine.Option(names = "--high-epimetheus-peaks", required = false, defaultValue = "0.95", description = "Minimum percentage of annotated epimetheus peaks for considering a compound as highly confidently annotated.")
    public float highEpimetheusPeakThreshold;

    @CommandLine.Option(names = "--min-epimetheus-peaks", required = false, defaultValue = "0.95", description = "Minimum percentage of annotated epimetheus peaks for considering a compound at all.")
    public float minEpimetheusPeakThreshold;

    @CommandLine.Option(names = "--min-epimetheus-intensity", required = false, defaultValue = "0.9", description = "Minimum amount of explained Epimetheus intensity for considering a compound at all.")
    public float minEpimetheusIntensityThreshold;

    @CommandLine.Option(names="--prefix", required = false, defaultValue = "", description = "prefix for each exported ms file")
    public String prefix;

    @CommandLine.Option(names = "--priority-databases", required = false, defaultValue = "knapsack,hmdb,kegg,biocyc,chebi", description = "The presence of a compound in one of these databases is a strong indication for a correct annotation.")
    public void setPriorityDatabases(String allDatabaseNames) {
        List<String> databaseNames = Arrays.asList(allDatabaseNames.split("\\s*,\\s*"));
        priorityDbFlag = 0;
        priorityDBNames.addAll(databaseNames);
        priorityDbPattern = Pattern.compile(String.join("|", priorityDBNames), Pattern.CASE_INSENSITIVE);
        for (String dbName : databaseNames) {
            boolean found = false;
            for (DataSource db : DataSource.values()) {
                if (dbName.toLowerCase().equals(db.realName.toLowerCase())) {
                    priorityDbFlag |= db.flag;
                    found=true;
                    break;
                }
            }
            if (!found) {
                LoggerFactory.getLogger(HarvesterOptions.class).warn("Unknown database: '" + dbName + "'");
            }
        }
    }

    public int priorityDbFlag;
    public List<String> priorityDBNames = new ArrayList<>();
    public Pattern priorityDbPattern;

    @Override
    public HarvesterWorkflow makePostprocessingJob(RootOptions<?, ?, ?, ?> rootOptions, ParameterConfig config) {
        return new HarvesterWorkflow(rootOptions, config, this);
    }

    @Override
    public Workflow makeWorkflow(RootOptions<?, ?, ?, ?> rootOptions, ParameterConfig config) {
        return makePostprocessingJob(rootOptions, config);
    }
}
