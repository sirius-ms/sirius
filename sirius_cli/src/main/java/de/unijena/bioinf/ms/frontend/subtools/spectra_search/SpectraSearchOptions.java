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

package de.unijena.bioinf.ms.frontend.subtools.spectra_search;

import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.completion.DataSourceCandidates;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.sirius.SiriusOptions;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bionf.spectral_alignment.SpectralMatchingType;
import picocli.CommandLine;

import java.util.List;
import java.util.function.Consumer;

@CommandLine.Command(name = "spectra-search", aliases = {"library-search"}, description = "@|bold <COMPOUND TOOL>|@ Computes the similarity between all compounds/features in the project-space (queries) one vs all spectra in the selected databases. %n %n",  versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class SpectraSearchOptions implements ToolChainOptions<SpectraSearchSubtoolJob, InstanceJob.Factory<SpectraSearchSubtoolJob>> {

    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public SpectraSearchOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }

    @CommandLine.Option(names = {"--database", "-d", "--db"}, descriptionKey = "SpectrumSearchDB" , paramLabel = DataSourceCandidates.PATAM_LABEL, completionCandidates = DataSourceCandidates.class,
            description = {"Search spectra in the union of the given databases. If no database is given, all database are used.", DataSourceCandidates.VALID_DATA_STRING})
    public void setDatabase(DefaultParameter dbList) throws Exception {
        defaultConfigOptions.changeOption("SpectralSearchDB", dbList);
    }

    @CommandLine.Option(names = "--ppm-max-peaks", descriptionKey = "SpectralMatchingMassDeviation.allowedPeakDeviation")
    public void setPpmMax(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("SpectralMatchingMassDeviation.allowedPeakDeviation", value + "ppm");
    }

    @CommandLine.Option(names = "--ppm-max-precursor", descriptionKey = "SpectralMatchingMassDeviation.allowedPrecursorDeviation")
    public void setPpmMaxMs2(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("SpectralMatchingMassDeviation.allowedPrecursorDeviation", value + "ppm");
    }

    @CommandLine.Option(names = "--print", descriptionKey = "SpectralSearchLog", description = "Number of matches to print per experiment.")
    public void setLogNum(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("SpectralSearchLog", value);
    }

    @CommandLine.Option(names = "--scorer", descriptionKey = "SpectralMatchingScorer", description = "Scoring function for alignment. Valid values: ${COMPLETION-CANDIDATES}.")
    public void setScorer(SpectralMatchingType matchingType) throws Exception {
        defaultConfigOptions.changeOption("SpectralMatchingScorer", matchingType.toString());
    }

    @Override
    public Consumer<Instance> getInvalidator() {
        return Instance::deleteSpectraSearchResult;
    }

    @Override
    public List<Class<? extends ToolChainOptions<?, ?>>> getDependentSubCommands() {
        return List.of(SiriusOptions.class);
    }

    @Override
    public InstanceJob.Factory<SpectraSearchSubtoolJob> call() throws Exception {
        return new InstanceJob.Factory<>(SpectraSearchSubtoolJob::new, getInvalidator());
    }

}
