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

package de.unijena.bioinf.ms.frontend.subtools.msnovelist;

import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.rest.model.license.AllowedFeatures;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.webapi.WebAPI;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * This is for MsNovelist candidate prediction and search of a predicted fingerprint against the predicted candidates.
 * <p>
 * They may be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "denovo-structures", aliases = {"msnovelist"}, description = "@|bold <COMPOUND TOOL>|@ Predict MsNovelist compound candidates and compare them against molecular fingerprint using CSI:FingerID scoring method. %n %n", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class MsNovelistOptions implements ToolChainOptions<MsNovelistSubToolJob, InstanceJob.Factory<MsNovelistSubToolJob>> {
    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public MsNovelistOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }

    @Option(names = {"-c", "--candidates"}, descriptionKey = "NumberOfMsNovelistCandidates", description = {"Number of MsNovelistCandidates to be predicted."})
    public void setNumberOfCandidates(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("NumberOfMsNovelistCandidates", value);
    }

    @Override
    public InstanceJob.Factory<MsNovelistSubToolJob> call() throws Exception {
        validateLicense();

        return new InstanceJob.Factory<>(
                MsNovelistSubToolJob::new,
                getInvalidator()
        );
    }

    @Override
    public Consumer<Instance> getInvalidator() {
        return Instance::deleteMsNovelistResult;
    }

    @Override
    public List<Class<? extends ToolChainOptions<?, ?>>> getDependentSubCommands() {
        return List.of();
    }

    private static void validateLicense(){
        boolean denovo = Optional.ofNullable(ApplicationCore.WEB_API()).map(WebAPI::getActiveSubscription)
                .map(Subscription::getAllowedFeatures)
                .map(AllowedFeatures::deNovo).orElse(false);

        if (!denovo){
            String message = "License ERROR: The active subscription does not contain the MsNovelist feature! Please remove MsNovelist from your toolchain, upgrade your active subscription or choose a different subscription.";
            System.out.println(message);
            throw new CommandLine.PicocliException("The active subscription does not contain the MsNovelist feature! Please remove MsNovelist from your toolchain.");
        }
    }
}