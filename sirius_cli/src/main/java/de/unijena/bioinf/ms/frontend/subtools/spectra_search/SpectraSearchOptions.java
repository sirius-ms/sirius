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

import de.unijena.bioinf.chemdb.annotations.SpectralSearchDB;
import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.completion.DataSourceCandidates;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.sirius.SiriusOptions;
import de.unijena.bioinf.projectspace.Instance;
import picocli.CommandLine;

import java.util.List;
import java.util.function.Consumer;

@CommandLine.Command(name = "spectra-search", aliases = {"library-search"}, description = "@|bold <COMPOUND TOOL>|@ Computes the similarity between all compounds/features in the project-space (queries) one vs all spectra in the selected databases. %n %n",  versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class SpectraSearchOptions implements ToolChainOptions<SpectraSearchSubtoolJob, InstanceJob.Factory<SpectraSearchSubtoolJob>> {

    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public SpectraSearchOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }

    @CommandLine.Option(names = {"--database", "-d", "--db"}, descriptionKey = "SpectrumSearchDB" , paramLabel = DataSourceCandidates.PARAM_LABEL, completionCandidates = DataSourceCandidates.class,
            description = {"Search spectra in the union of the given databases. If no database is given, all database are used.", DataSourceCandidates.VALID_DATA_STRING})
    public void setDatabase(DefaultParameter dbList) throws Exception {
        defaultConfigOptions.changeOption("SpectralSearchDB", dbList);
    }


    @CommandLine.Option(names = "--ppm-max-precursor", descriptionKey = "IdentitySearchSettings.precursorDeviation")
    public void setPpmMaxIdentity(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("IdentitySearchSettings.precursorDeviation", value);
    }

    @CommandLine.Option(names = "--min-similarity", descriptionKey = "IdentitySearchSettings.minSimilarity")
    public void setMinSimilarityIdentity(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("IdentitySearchSettings.minSimilarity", value);
    }

    @CommandLine.Option(names = "--min-peaks", descriptionKey = "IdentitySearchSettings.minNumOfPeaks")
    public void setMinNumPeaksIdentity(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("IdentitySearchSettings.minNumOfPeaks", value);
    }

    @CommandLine.Option(names = "--max-hits", descriptionKey = "IdentitySearchSettings.maxNumOfHits")
    public void setMaxNumHitsIdentity(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("IdentitySearchSettings.maxNumOfHits", value);
    }


    @CommandLine.Option(names = "--analogue-search", descriptionKey = "AnalogueSearchSettings.enabled")
    public void setEnableAnalogueSearch(boolean value) throws Exception {
        defaultConfigOptions.changeOption("AnalogueSearchSettings.enabled", String.valueOf(value));
    }

    @CommandLine.Option(names = "--min-similarity-analogue", descriptionKey = "AnalogueSearchSettings.minSimilarity")
    public void setMinSimilarityAnalogue(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("AnalogueSearchSettings.minSimilarity", value);
    }

    @CommandLine.Option(names = "--min-peaks-analogue", descriptionKey = "AnalogueSearchSettings.minNumOfPeaks")
    public void setMinNumPeaksAnalogue(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("AnalogueSearchSettings.minNumOfPeaks", value);
    }

    @CommandLine.Option(names = "--max-hits-analogue", descriptionKey = "AnalogueSearchSettings.maxNumOfHits")
    public void setMaxNumHitsAnalogue(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("AnalogueSearchSettings.maxNumOfHits", value);
    }

    @CommandLine.Option(names = "--print", descriptionKey = "SpectralSearchLog", description = "Number of matches to print per experiment.")
    public void setLogNum(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("SpectralSearchLog", value);
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
        SpectraCache cache = new SpectraCache(
                ApplicationCore.WEB_API.getChemDB(),
                defaultConfigOptions.config.createInstanceWithDefaults(SpectralSearchDB.class).searchDBs
        );
        return new InstanceJob.Factory<>(sub -> new SpectraSearchSubtoolJob(sub, cache), getInvalidator());
    }

}
