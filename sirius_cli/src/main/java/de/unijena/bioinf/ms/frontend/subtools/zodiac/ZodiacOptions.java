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

package de.unijena.bioinf.ms.frontend.subtools.zodiac;

import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.fingerprint.FingerprintOptions;
import de.unijena.bioinf.projectspace.Instance;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

/**
 * This is for Zodiac specific parameters.
 * <p>
 * They may be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "zodiac", aliases = {"rerank-formulas"}, description = "@|bold <DATASET TOOL>|@ Identify Molecular formulas of all compounds in a dataset together using ZODIAC. %n %n", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class ZodiacOptions implements ToolChainOptions<ZodiacSubToolJob, DataSetJob.Factory<ZodiacSubToolJob>> {
    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public ZodiacOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }


    @Option(names = "--considered-candidates-at-300", descriptionKey = "ZodiacNumberOfConsideredCandidatesAt300Mz",
            description = {"Maximum number of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are considered by ZODIAC for compounds below 300 m/z."})
    public void setNumberOfConsideredCandidatesBelow300(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacNumberOfConsideredCandidatesAt300Mz", value);
    }

    @Option(names = "--considered-candidates-at-800", descriptionKey = "ZodiacNumberOfConsideredCandidatesAt800Mz",
            description = {"Maximum number of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are considered by ZODIAC for compounds above 800 m/z."})
    public void setNumberOfConsideredCandidatesAbove800(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacNumberOfConsideredCandidatesAt800Mz", value);
    }

    //ZodiacRatioOfConsideredCandidatesPerIonization only available via config

    ////////////////////////////
    // Identity library hits //
    ///////////////////////////

    @Option(names = "--identity-search-anchors", descriptionKey = "ZodiacLibraryScoring.enable",
            description = {"Enable spectral library hits as anchors in ZODIAC network."})
    public void setEnableLibrary(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacLibraryScoring.enable", value);
    }

    @Option(names = "--min-cosine-anchors", descriptionKey = "ZodiacLibraryScoring.minCosine",
            description = {"Spectral library hits must have at least this cosine or higher to be considered as anchors in scoring.", "Value must be in [0,1]."})
    public void setMinCosine(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacLibraryScoring.minCosine", value);
    }

    @Option(names = "--lambda-anchors", descriptionKey = "ZodiacLibraryScoring.lambda",
            description = {"Lambda used in the scoring function of spectral library hits. The higher this value the higher are library hits weighted in ZODIAC scoring."})
    public void setLambda(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacLibraryScoring.lambda", value);
    }

    ///////////////////////////
    // Analog library hits  //
    //////////////////////////

    @Option(names = "--analogue-search-nodes", descriptionKey = "ZodiacAnalogueNodes.enable",
            description = {"Enable Analogue search hits as additional informative nodes in ZODIAC network.", "Value must be in [0,1]."})
    public void setEnableAnalogueNodes(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacAnalogueNodes.enable", value);
    }

    @Option(names = "--min-cosine-analogue", descriptionKey = "ZodiacAnalogueNodes.minModifiedCosine",
            description = {"Analogue search hits must have at least this cosine or higher to be considered as additional node in ZODIAC network scoring.", "Value must be in [0,1]."})
    public void setAnalogueMinModCosine(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacAnalogueNodes.minModifiedCosine", value);
    }

    @Option(names = "--min-peaks-analogue", descriptionKey = "ZodiacAnalogueNodes.minSharedPeaks",
            description = {"Analogue search hits must have at least this number of shared peaks to be considered as additional node in ZODIAC network scoring."})
    public void setAnalogueMinSharedPeaks(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacAnalogueNodes.minSharedPeaks", value);
    }


    ///////////////////////
    //number of epochs///
    /////////////////////
    @Option(names = "--iterations", descriptionKey = "ZodiacEpochs.iterations",
            description = {"Number of epochs to run the Gibbs sampling. When multiple Markov chains are computed, all chains' iterations sum up to this value."})
    public void setIterationSteps(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacEpochs.iterations", value);
    }

    @Option(names = "--burn-in", descriptionKey = "ZodiacEpochs.burnInPeriod",
            description = {"Number of epochs considered as 'burn-in period'."})
    public void setBurnInSteps(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacEpochs.burnInPeriod", value);
    }

    @Option(names = "--separateRuns", hidden = true, descriptionKey = "ZodiacEpochs.numberOfMarkovChains",
            description = {"Number of separate Gibbs sampling runs."})
    public void setSeparateRuns(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacEpochs.numberOfMarkovChains", value);
    }


    ///////////////////////////
    //edge filter parameters//
    /////////////////////////
    //
    @Option(names = "--thresholdFilter", descriptionKey = "ZodiacEdgeFilterThresholds.thresholdFilter",
            description = {"Defines the proportion of edges of the complete network which will be ignored."})
    public void setThresholdFilter(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacEdgeFilterThresholds.thresholdFilter", value);
    }

    //0d for filtering on the fly
    @Option(names = "--minLocalConnections", descriptionKey = "ZodiacEdgeFilterThresholds.minLocalConnections",
            description = {"Minimum number of compounds to which at least one candidate per compound must be connected to."})
    public void setMinLocalConnections(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacEdgeFilterThresholds.minLocalConnections", value);
    }


    ///////////////////////////
    // others               //
    /////////////////////////

    @Option(names = "--ignore-spectra-quality",
            description = {"As default ZODIAC runs a 2-step approach. First running 'good quality compounds' only, and afterwards including the remaining."})
    public void disableZodiacTwoStepApproach(boolean disable) throws Exception {
        if (disable)
            defaultConfigOptions.changeOption("ZodiacRunInTwoSteps", "false");
    }

    public Path summaryFile;

    @Option(names = "--summary", hidden = true, description = {"Write a ZODIAC summary CSV file."})
    public void setSummaryFile(String filePath) throws Exception {
        summaryFile = Paths.get(filePath);
    }

    public Path bestMFSimilarityGraphFile;

    @Option(names = "--graph", hidden = true,
            description = {"Writes the similarity graph based on the top molecular formula annotations of each compound."})
    public void setSimilarityGraphFile(String filePath) throws Exception {
        bestMFSimilarityGraphFile = Paths.get(filePath);
    }

    @Override
    public DataSetJob.Factory<ZodiacSubToolJob> call() throws Exception {
        return new DataSetJob.Factory<>(
                sub -> new ZodiacSubToolJob(this, sub),
                getInvalidator()
        );
    }

    @Override
    public Consumer<Instance> getInvalidator() {
        return Instance::deleteZodiacResult;
    }

    @Override
    public List<Class<? extends ToolChainOptions<?, ?>>> getDependentSubCommands() {
        return List.of(/*PassatuttoOptions.class, */FingerprintOptions.class);
    }
}
