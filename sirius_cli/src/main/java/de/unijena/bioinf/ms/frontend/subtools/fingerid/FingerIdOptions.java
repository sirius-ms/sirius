package de.unijena.bioinf.ms.frontend.subtools.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.FBCandidateFingerprints;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.completion.DataSourceCandidates;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainOptions;
import de.unijena.bioinf.ms.frontend.subtools.canopus.CanopusOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.sirius.FormulaResultRankingScore;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.function.Consumer;

/**
 * This is for CSI:FingerID specific parameters.
 * <p>
 * They may be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "structure", aliases = {"fingerid", "S"}, description = "<COMPOUND_TOOL> Identify molecular structure for each compound Individually using CSI:FingerID.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class FingerIdOptions implements ToolChainOptions<FingeridSubToolJob, InstanceJob.Factory<FingeridSubToolJob>> {
    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public FingerIdOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }


    // todo Print info about webservice and quit (like help)
    @Option(names = {"--info", "--webservice-info"}, hidden = true, description = "Information about connection of CSI:FingerID Webservice")
    public boolean fingeridInfo;

    @Option(names = {"-d", "--database", "--db"}, descriptionKey = "StructureSearchDB", paramLabel = DataSourceCandidates.PATAM_LABEL, completionCandidates = DataSourceCandidates.class,
            description = {"Search structure in the union og the given databases. If no database is given 'ALL' internal databases are used.", DataSourceCandidates.VALID_DATA_STRING})
    public void setDatabase(DefaultParameter dbList) throws Exception {
        defaultConfigOptions.changeOption("StructureSearchDB", dbList);
    }

    //todo implement candidate number restriction in FingerIDJJob after confidence calculation?
    //this would result in an projectspace where some score cannot be reconstructed form data anymore?
    @Option(names = {"-c", "--candidates"}, descriptionKey = "NumberOfStructureCandidates", hidden = true, description = {"Number of molecular structure candidates in the output."})
    public void setNumberOfCandidates(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("NumberOfStructureCandidates", value);
    }

    @Option(names = {"-p", "--structure-predictors"}, hidden = true,
            description = "Predictors used to search structures. Currently only CSI:FingerID is working.")
    public void setPredictors(List<String> predictors) throws Exception {
        defaultConfigOptions.changeOption("StructurePredictors", predictors);
    }

    @Override
    public InstanceJob.Factory<FingeridSubToolJob> call() throws Exception {
        return new InstanceJob.Factory<>(
                FingeridSubToolJob::new,
                getInvalidator()
        );
    }

    @Override
    public Consumer<Instance> getInvalidator() {
        return inst -> {
            inst.deleteFromFormulaResults(FingerprintResult.class, FBCandidates.class, FBCandidateFingerprints.class);
            inst.loadFormulaResults(FormulaScoring.class).stream().map(SScored::getCandidate)
                    .forEach(it -> it.getAnnotation(FormulaScoring.class).ifPresent(z -> {
                        if (z.removeAnnotation(TopCSIScore.class) != null || z.removeAnnotation(ConfidenceScore.class) != null)
                            inst.updateFormulaResult(it, FormulaScoring.class); //update only if there was something to remove
                    }));
            if (inst.getExperiment().getAnnotation(FormulaResultRankingScore.class).orElse(FormulaResultRankingScore.AUTO).isAuto()) {
                inst.getID().getRankingScoreTypes().removeAll(List.of(TopCSIScore.class, ConfidenceScore.class));
                inst.updateCompoundID();
            }
        };
    }

    @Override
    public List<Class<? extends ToolChainOptions<?, ?>>> getSubCommands() {
        return List.of(CanopusOptions.class);
    }
}
