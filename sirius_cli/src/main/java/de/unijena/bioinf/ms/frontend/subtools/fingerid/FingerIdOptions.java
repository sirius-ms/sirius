package de.unijena.bioinf.ms.frontend.subtools.fingerid;

import de.unijena.bioinf.ms.frontend.completion.DataSourceCandidates;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.fingerid.options.FormulaResultRankingScoreType;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * This is for CSI:FingerID specific parameters.
 * <p>
 * They may be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "structure", aliases = {"fingerid", "S"}, description = "<COMPOUND_TOOL> Identify molecular structure for each compound Individually using CSI:FingerID.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class,  mixinStandardHelpOptions = true)
public class FingerIdOptions implements Callable<InstanceJob.Factory<FingeridSubToolJob>> {
    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public FingerIdOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }


    // info
    @Option(names = {"--info", "--webservice-info"}, description = "Information about connection of CSI:FingerID Webservice")
    public boolean fingeridInfo;

    @Option(names = {"-d", "--database", "--db"}, paramLabel = DataSourceCandidates.PATAM_LABEL, completionCandidates = DataSourceCandidates.class,
            description = {"Search structure in the union og the given databases. If no database is given 'ALL' internal databases are used.", DataSourceCandidates.VALID_DATA_STRING})
    public void setDatabase(String dbList) throws Exception {
        defaultConfigOptions.changeOption("StructureSearchDB", dbList);
    }

    @Option(names = {"-p", "--structure-predictors"}, description = "Predictors used to search structures. Currently only CSI:FingerID is working.", hidden = true)
    public void setPredictors(List<String> predictors) throws Exception {
        defaultConfigOptions.changeOption("StructurePredictors", predictors);
    }

    // input formula candidates
    @Option(names = {"-s", "--formula-score"}, description = "Specifies the Score that is used to rank the list Molecular Formula Identifications" +
            " before the thresholds for CSI:FingerID predictions are calculated.")
    public void setPredictors(FormulaResultRankingScoreType score) throws Exception {
        defaultConfigOptions.changeOption("FormulaResultRankingScore", score.simpleClazzName());
    }

    // candidates
    @Option(names = {"-c", "--candidates"}, description = "Number of molecular structure candidates in the output.")
    public void setNumberOfCandidates(String value) throws Exception {
        defaultConfigOptions.changeOption("NumberOfStructureCandidates", value);
    }


    @Override
    public InstanceJob.Factory<FingeridSubToolJob> call() throws Exception {
        return FingeridSubToolJob::new;
    }



}
