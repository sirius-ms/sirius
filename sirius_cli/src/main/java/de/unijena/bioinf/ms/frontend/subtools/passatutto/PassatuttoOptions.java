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

package de.unijena.bioinf.ms.frontend.subtools.passatutto;

import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.fingerprint.FingerprintOptions;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.projectspace.Instance;
import picocli.CommandLine;

import java.util.List;
import java.util.function.Consumer;

@CommandLine.Command(name = "passatutto", description = "<COMPOUND_TOOL> Compute a decoy spectra based on the fragmentation trees of the given input spectra. If no molecular formula is provided in the input, the top scoring computed formula is used.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class PassatuttoOptions implements ToolChainOptions<PassatuttoSubToolJob, InstanceJob.Factory<PassatuttoSubToolJob>> {

    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public PassatuttoOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }

    @Override
    public InstanceJob.Factory<PassatuttoSubToolJob> call() {
        return new InstanceJob.Factory<>(
                PassatuttoSubToolJob::new,
                getInvalidator()
        );
    }

    @Override
    public Consumer<Instance> getInvalidator() {
        return inst -> inst.deleteFromFormulaResults(Decoy.class);
    }

    @Override
    public List<Class<? extends ToolChainOptions<?, ?>>> getFollowupSubCommands() {
        return List.of(FingerprintOptions.class);
    }

    @Override
    public List<Class<? extends ToolChainOptions<?, ?>>> getDependentSubCommands() {
        return List.of();
    }
}
