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
import de.unijena.bioinf.projectspace.CompoundContainer;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.spectraldb.SpectralAlignmentType;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import picocli.CommandLine;

import java.util.List;
import java.util.function.Consumer;

@CommandLine.Command(name = "spectra-search", aliases = {"spectral-search"}, description = "<STANDALONE> Computes the similarity between all compounds/features in the project-space (queries) one vs all compounds/features in the second project-space (library).",  versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class SpectraSearchOptions implements ToolChainOptions<SpectraSearchSubtoolJob, InstanceJob.Factory<SpectraSearchSubtoolJob>> {

    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public SpectraSearchOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }

    @CommandLine.Option(names = {"--database", "-d", "--db"}, descriptionKey = "SpectrumSearchDB" , paramLabel = DataSourceCandidates.PATAM_LABEL, completionCandidates = DataSourceCandidates.class,
            description = {"Search spectra in the Union of the given databases. If no database is given, all database are used.", DataSourceCandidates.VALID_DATA_STRING})
    public void setDatabase(DefaultParameter dbList) throws Exception {
        defaultConfigOptions.changeOption("SpectralSearchDB", dbList);
    }

    @CommandLine.Option(names = "--ppm-max", descriptionKey = "MS1MassDeviation.allowedMassDeviation", description = "Maximum allowed mass deviation in ppm for matching peaks.")
    public void setPpmMax(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("MS1MassDeviation.allowedMassDeviation", value + "ppm");
    }

    @CommandLine.Option(names = "--ppm-max-ms2", descriptionKey = "MS2MassDeviation.allowedMassDeviation", description = "Maximum allowed mass deviation in ppm for matching the precursor. If not specified, the same value as for the peaks is used.")
    public void setPpmMaxMs2(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("MS2MassDeviation.allowedMassDeviation", value + "ppm");
    }

    @CommandLine.Option(names = "--print", descriptionKey = "SpectralSearchLog", description = "Number of matches to print per experiment.")
    public void setLogNum(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("SpectralSearchLog", value);
    }

    @CommandLine.Option(names = "--scorer", descriptionKey = "SpectralAlignmentScorer", description = "Scoring function for alignment. Valid values: ${COMPLETION-CANDIDATES}.", defaultValue = "MODIFIED_COSINE")
    public void setScorer(SpectralAlignmentType alignmentType) throws Exception {
        defaultConfigOptions.changeOption("SpectralAlignmentScorer", alignmentType.toString());
    }

    @Override
    public Consumer<Instance> getInvalidator() {
        return instance -> {
            CompoundContainer container = instance.loadCompoundContainer(SpectralSearchResult.class);
            if (container.hasAnnotation(SpectralSearchResult.class)) {
                container.removeAnnotation(SpectralSearchResult.class);
            }
        };
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
