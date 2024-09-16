/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ms.frontend.subtools.summaries;

import de.unijena.bioinf.ms.frontend.subtools.PostprocessingTool;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import lombok.Getter;
import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(name = "summaries", aliases = {"write-summaries", "W"}, description = "@|bold <STANDALONE, POSTPROCESSING>|@ Write Summary files from a given project-space into the given project-space or a custom location. %n %n", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class SummaryOptions implements PostprocessingTool<NoSqlSummarySubToolJob>, StandaloneTool<Workflow> {

    public enum Format { TSV, ZIP, CSV, XLSX }

    //specify negated  name since default is true ->  special picocli behavior
    //https://picocli.info/#_negatable_options
    @Getter
    @CommandLine.Option(names = {"--no-top-hit-summary"}, description = "Write summary files with all Top Hits.", defaultValue = "true", negatable = true)
    protected boolean topHitSummary;

    @Getter
    @CommandLine.Option(names = {"--top-hit-adduct-summary"}, description = "Write summary files with all Top Hits and their adducts", defaultValue = "false", negatable = true)
    protected boolean topHitWithAdductsSummary;

    @Getter
    @CommandLine.Option(names = {"--full-summary"}, description = {"Write summary files with ALL Hits. ", "(Use with care! Might create large files and consume large amounts of memory for large projects.)"}, defaultValue = "false", negatable = true)
    protected boolean fullSummary;

    @Getter
    @CommandLine.Option(names = {"--top-k-summary"}, description = {"Write summary files with top k hits . ", "(Use with care! Using large 'k' might create large files and consume large amounts of memory for large projects.)"})
    protected int topK = -1;

    @CommandLine.Option(names = {"--feature-quality-summary"}, description = "Write a summary file with feature quality metrics. One line per feature regardless of other option.", defaultValue = "false")
    protected boolean qualitySummary;

    @CommandLine.Option(names = {"--chemvista"}, description = "Export a summary file for importing structure annotations into ChemVista (always CSV and Top Hits regardless of other options).", defaultValue = "false")
    protected boolean chemVista;

    //todo enable when implementing spectral match export, per compound candidate
//    @Getter
//    @CommandLine.Option(names = {"--all-spectra"}, description = {"Write project wide summary files with ALL reference spectrum hits. ", "(Use with care! Might create large files and consume large amounts of memory for large projects.)"}, defaultValue = "false", negatable = true)
//    protected boolean allSpectra;
//
//    @Getter
//    @CommandLine.Option(names = {"--top-k-spectra"}, description = {"Write project wide summary files with top k reference spectrum hits . ", "(Use with care! Using large 'k' might create large files and consume large amounts of memory for large projects.)"})
//    protected int topKSpectra = -1;

    @CommandLine.Option(names = {"--output", "-o"}, description = {"Specify location for writing summary files.", "By default summaries are written to a directory with project name near the project file."})
    protected Path location;

    @CommandLine.Option(names = {"--format"}, description = {"Output format for summaries. Valid values: ${COMPLETION-CANDIDATES}.", "ZIP produces zipped TSV files."}, defaultValue = "tsv")
    protected Format format;

    @CommandLine.Option(names = {"--quote-strings"}, description = {"Enclose all strings in quotation marks (for TSV and CSV)."})
    protected boolean quoteStrings;



    @Override
    public NoSqlSummarySubToolJob makePostprocessingJob() {
        return new NoSqlSummarySubToolJob(SummaryOptions.this);
    }

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return new NoSqlSummarySubToolJob(rootOptions.makeDefaultPreprocessingJob(), SummaryOptions.this);
    }
}
