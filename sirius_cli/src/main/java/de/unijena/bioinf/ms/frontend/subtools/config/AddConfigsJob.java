/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.config;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.properties.FinalConfig;
import de.unijena.bioinf.babelms.ms.InputFileConfig;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceConfig;
import de.unijena.bioinf.projectspace.sirius.FormulaResultRankingScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class AddConfigsJob extends InstanceJob {
    private final ParameterConfig cliConfig;

    public AddConfigsJob(ParameterConfig cliConfig) {
        super(SiriusJobs.getGlobalJobManager());
        this.cliConfig = cliConfig;
    }

    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        return false;
    }

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        final Ms2Experiment exp = inst.getExperiment();
        final Optional<ProjectSpaceConfig> psConfig = inst.loadConfig();

        ParameterConfig baseConfig;

        //override defaults
        baseConfig = psConfig
                .map(projectSpaceConfig -> projectSpaceConfig.config.newIndependentInstance(cliConfig,true))
                .orElse(cliConfig);

        if (exp.hasAnnotation(InputFileConfig.class)){
            @NotNull InputFileConfig msConf = exp.getAnnotationOrThrow(InputFileConfig.class);
            if (!baseConfig.containsConfiguration(msConf.config.getLocalConfigName()))
                baseConfig = baseConfig.newIndependentInstance(msConf.config, false);
        }

        baseConfig = baseConfig.newIndependentInstance("RUNTIME_CONFIGS:" + inst.getID(),true); //runtime modification layer,  that does not effect the other configs
        //fill all annotations
        exp.setAnnotation(FinalConfig.class, new FinalConfig(baseConfig));
        exp.setAnnotationsFrom(baseConfig, Ms2ExperimentAnnotation.class);


        final FormulaResultRankingScore it = exp.getAnnotation(FormulaResultRankingScore.class).orElse(FormulaResultRankingScore.AUTO);
        // this value is a commandline parameter that specifies how to handle the ranking score. If auto we decide how to
        // handle, otherwise we set the user defined value
        if (it.isAuto()) {
            if (inst.getID().getRankingScoreTypes().isEmpty()) //set a default if nothing else is already set
                inst.getID().setRankingScoreTypes(SiriusScore.class);
        } else {
            inst.getID().setRankingScoreTypes(it.value);
        }

        inst.updateExperiment(); //todo we should optize this, so that this is not needed anymore
        inst.updateConfig();
    }

    @Override
    public String getToolName() {
        return "Config Job";
    }

}