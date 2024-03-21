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

package de.unijena.bioinf.ms.frontend.subtools.fingerblast;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.ConfidenceScoreApproximate;
import de.unijena.bioinf.fingerid.StructureSearchResult;
import de.unijena.bioinf.fingerid.blast.FBCandidateFingerprints;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.completion.DataSourceCandidates;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.msnovelist.MsNovelistOptions;
import de.unijena.bioinf.projectspace.FormulaResultRankingScore;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.Instance;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.function.Consumer;

/**
 * This is for CSI:FingerID (structure database search) specific parameters.
 * <p>
 * They may be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "structure", aliases = {"search-structure-db"}, description = "<COMPOUND_TOOL> Search in molecular structure db for each compound Individually using CSI:FingerID structure database search.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class FingerblastOptions implements ToolChainOptions<FingerblastSubToolJob, InstanceJob.Factory<FingerblastSubToolJob>> {
    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public FingerblastOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }

    @Option(names = {"-d", "--database", "--db"}, descriptionKey = "StructureSearchDB", paramLabel = DataSourceCandidates.PATAM_LABEL, completionCandidates = DataSourceCandidates.class,
            description = {"Search structure in the union of the given databases. If no database is given the default database(s) are used.", DataSourceCandidates.VALID_DATA_STRING})
    public void setDatabase(DefaultParameter dbList) throws Exception {
        defaultConfigOptions.changeOption("StructureSearchDB", dbList);
    }

    //todo implement candidate number restriction in FingerIDJJob after confidence calculation?
    //this would result in an projectspace where some score cannot be reconstructed form data anymore?
    @Option(names = {"-c", "--candidates"}, descriptionKey = "NumberOfStructureCandidates", hidden = true, description = {"Number of molecular structure candidates in the output."})
    public void setNumberOfCandidates(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("NumberOfStructureCandidates", value);
    }

    @Option(names = {"-p", "--structure-predictors"}, hidden = true,
            description = "Predictors used to search structures. Currently only CSI:FingerID is working.")
    public void setPredictors(List<String> predictors) throws Exception {
        defaultConfigOptions.changeOption("StructurePredictors", predictors);
    }

    @Option(names = {"-e", "--exp"}, descriptionKey = "ExpansiveSearchConfidenceMode.confidenceScoreSimilarityMode",
            description = {"Confidence mode that is used for expansive search. OFF -> no expansive search. EXACT -> Exact mode confidence score is used for expansive search. APPROXIMATE ->  Approximate mode confidence score is used for expansive search"})
    public void setExpansiveSearchConfMode(String expansiveSearchConfMode) throws Exception {
        defaultConfigOptions.changeOption("ExpansiveSearchConfidenceMode.confidenceScoreSimilarityMode", expansiveSearchConfMode);
    }

    @Override
    public InstanceJob.Factory<FingerblastSubToolJob> call() throws Exception {
        return new InstanceJob.Factory<>(
                FingerblastSubToolJob::new,
                getInvalidator()
        );
    }

    @Override
    public Consumer<Instance> getInvalidator() {
        return inst -> {
            inst.deleteFromFormulaResults(FBCandidates.class, FBCandidateFingerprints.class, StructureSearchResult.class);
            inst.loadFormulaResults(FormulaScoring.class).stream().map(SScored::getCandidate)
                    .forEach(it -> it.getAnnotation(FormulaScoring.class).ifPresent(z -> {
                        if (z.removeAnnotation(TopCSIScore.class) != null || z.removeAnnotation(ConfidenceScore.class) != null || z.removeAnnotation(ConfidenceScoreApproximate.class) != null)
                            inst.updateFormulaResult(it, FormulaScoring.class); //update only if there was something to remove
                    }));
            if (inst.getExperiment().getAnnotation(FormulaResultRankingScore.class).orElse(FormulaResultRankingScore.AUTO).isAuto()) {
                inst.getCompoundContainerId().getRankingScoreTypes().removeAll(List.of(TopCSIScore.class, ConfidenceScore.class, ConfidenceScoreApproximate.class));
                inst.getCompoundContainerId().setConfidenceScore(null);
                inst.getCompoundContainerId().setConfidenceScoreApproximate(null);
                inst.getCompoundContainerId().setUseApproximate(false); //todo proper default
                inst.updateCompoundID();
            } else if (inst.getCompoundContainerId().getConfidenceScore().isPresent()) {
                inst.getCompoundContainerId().setConfidenceScore(null);
                inst.getCompoundContainerId().setConfidenceScoreApproximate(null);
                inst.getCompoundContainerId().setUseApproximate(false); //todo proper default
                inst.updateCompoundID();
            }
        };
    }

    @Override
    public List<Class<? extends ToolChainOptions<?, ?>>> getDependentSubCommands() {
        return List.of();
    }

    @Override
    public List<Class<? extends ToolChainOptions<?, ?>>> getFollowupSubCommands() {
        return List.of(MsNovelistOptions.class);
    }
}
