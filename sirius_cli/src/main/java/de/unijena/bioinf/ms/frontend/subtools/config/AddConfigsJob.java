package de.unijena.bioinf.ms.frontend.subtools.config;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.properties.FinalConfig;
import de.unijena.bioinf.babelms.ms.MsFileConfig;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.frontend.subtools.canopus.CanopusOptions;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.ProjectSpaceConfig;
import de.unijena.bioinf.projectspace.sirius.FormulaResultRankingScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
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

        //override defaults
        baseConfig = psConfig
                .map(projectSpaceConfig -> projectSpaceConfig.config.newIndependentInstance(cliConfig))
                .orElseGet(() -> cliConfig);

        if (exp.hasAnnotation(MsFileConfig.class))
            baseConfig = baseConfig.newIndependentInstance(exp.getAnnotationOrThrow(MsFileConfig.class).config);

        baseConfig = baseConfig.newIndependentInstance("RUNTIME_CONFIGS:" + inst.getID()); //runtime modification layer,  that does not effect the other configs
        //fill all annotations
        exp.setAnnotation(FinalConfig.class, new FinalConfig(baseConfig));
        exp.addAnnotationsFrom(baseConfig, Ms2ExperimentAnnotation.class);

        //reduce basic list of possible Adducts to charge
//        exp.getAnnotation(PossibleAdducts.class).ifPresent(add -> add.keepOnly(exp.getPrecursorIonType().getCharge()));

        final FormulaResultRankingScore it = exp.getAnnotation(FormulaResultRankingScore.class).orElse(FormulaResultRankingScore.AUTO);
        // this value is a commandline parameter that specifies how to handle the ranking score. If auto we decide how to
        // handle, otherwise we set the user defined value
        if (it.isAuto()) {
            if (inst.getID().getRankingScoreTypes().isEmpty()) //set a default if nothing else is already set
                inst.getID().setRankingScoreTypes(SiriusScore.class);
        } else {
            inst.getID().setRankingScoreTypes(it.value);
        }

        inst.updateExperiment();
        inst.updateConfig();
//        inst.updateCompoundID();
    }

    @Override
    public String getToolName() {
        return "Config Job";
    }

}