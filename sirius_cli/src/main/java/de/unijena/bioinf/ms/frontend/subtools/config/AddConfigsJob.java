package de.unijena.bioinf.ms.frontend.subtools.config;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.ChemistryBase.ms.properties.FinalConfig;
import de.unijena.bioinf.babelms.ms.MsFileConfig;
import de.unijena.bioinf.fingerid.annotations.FormulaResultRankingScore;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.frontend.subtools.Instance;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.fingerid.annotations.UserFormulaResultRankingScore;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.ProjectSpaceConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class AddConfigsJob extends InstanceJob {
    private ParameterConfig cliConfig;

    public AddConfigsJob(ParameterConfig cliConfig) {
        this.cliConfig = cliConfig;
    }

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        final Ms2Experiment exp = inst.getExperiment();
        final Optional<ProjectSpaceConfig> psConfig = inst.loadConfig();


        ParameterConfig baseConfig;

        if (psConfig.isPresent()) //override defaults
            baseConfig = psConfig.get().config.newIndependentInstance(cliConfig);
        else
            baseConfig = cliConfig;

        if (exp.hasAnnotation(MsFileConfig.class))
            baseConfig = baseConfig.newIndependentInstance(exp.getAnnotationOrThrow(MsFileConfig.class).config);

        baseConfig = baseConfig.newIndependentInstance("RUNTIME_CONFIGS:" + inst.getID()); //runtime modification layer,  that does not effect the other configs
        //fill all annotations
        exp.setAnnotation(FinalConfig.class, new FinalConfig(baseConfig));
        exp.addAnnotationsFrom(baseConfig, Ms2ExperimentAnnotation.class);

        //reduce basic list of possible Adducts to charge
        exp.getAnnotationOrThrow(PossibleAdducts.class).keepOnly(exp.getPrecursorIonType().getCharge());

        // convert csi ranking score
        if (exp.getAnnotationOrThrow(UserFormulaResultRankingScore.class).isDefined()) {
            exp.setAnnotation(FormulaResultRankingScore.class, new FormulaResultRankingScore(exp.getAnnotationOrThrow(UserFormulaResultRankingScore.class).getScoreClass()));
            exp.getAnnotationOrThrow(FinalConfig.class).config.changeConfig("FormulaResultRankingScore", exp.getAnnotationOrThrow(UserFormulaResultRankingScore.class).getScoreClass().getName());
        }


        inst.updateExperiment();
        inst.updateConfig();
    }
}