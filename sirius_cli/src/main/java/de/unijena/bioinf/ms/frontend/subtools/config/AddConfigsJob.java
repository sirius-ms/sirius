/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.config;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ms.annotations.RecomputeResults;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.properties.ConfigType;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.Instance;
import org.apache.commons.configuration2.CombinedConfiguration;
import org.jetbrains.annotations.NotNull;
import java.util.Optional;

public class AddConfigsJob extends InstanceJob {
    private final ParameterConfig computeConfig;

    public AddConfigsJob(ParameterConfig computeConfig) {
        super(SiriusJobs.getGlobalJobManager());
        this.computeConfig = computeConfig;
        asIO();
    }

    @Override
    protected boolean needsMs2() {
        return false;
    }

    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        return false;
    }

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        ParameterConfig baseConfig = computeConfig;

        {
            //override defaults
            // CLI_CONFIG might already exist from previous runs and needs to be updated.
            Optional<ParameterConfig> projectSpaceConfigOpt = inst.loadProjectConfig();
            checkForInterruption();
            if (projectSpaceConfigOpt.isPresent()){
                ParameterConfig projectSpaceConfig = projectSpaceConfigOpt.get();
                if (!computeConfig.getLocalConfigName().equals(ConfigType.CLI.name()) && computeConfig.containsConfiguration(ConfigType.CLI.name())) {
                    projectSpaceConfig = projectSpaceConfig.newIndependentInstance(ConfigType.CLI.name());
                    projectSpaceConfig.updateConfig(ConfigType.CLI.name(), ((CombinedConfiguration) computeConfig.getConfigs()).getConfiguration(ConfigType.CLI.name()));
                }
                baseConfig = projectSpaceConfig.newIndependentInstance(computeConfig, true);
                //remove runtime configs from previous analyses
                baseConfig.getConfigNames().stream().filter(s -> s.startsWith(ConfigType.RUNTIME.name())).forEach(baseConfig::removeConfig);
            }
        }



        //input file configs are intended to be immutable, we still reload to ensure that it is on top position after CLI config
        {
            final Optional<ParameterConfig> msConf = inst.loadInputFileConfig();
            checkForInterruption();
            if (msConf.isPresent()) {
                baseConfig.removeConfig(msConf.get().getLocalConfigName());
                baseConfig = baseConfig.newIndependentInstance(msConf.get(), false);
            }
        }
        checkForInterruption();

        //runtime modification layer,  that does not affect the other configs, needs to be cleared before further analyses starts
        //name cannot be based on the ID because people might rename their compounds
        baseConfig = baseConfig.newIndependentInstance(ConfigType.RUNTIME.name(), true);
        //writes full config stack to be used as base config when rerunning computations
        inst.updateProjectConfig(baseConfig);
        inst.setRecompute(baseConfig.createInstanceWithDefaults(RecomputeResults.class).value());
    }

    @Override
    public String getToolName() {
        return "Config Job";
    }
}