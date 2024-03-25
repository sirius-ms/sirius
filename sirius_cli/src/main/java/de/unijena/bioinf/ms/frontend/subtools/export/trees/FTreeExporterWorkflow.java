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

package de.unijena.bioinf.ms.frontend.subtools.export.trees;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.projectspace.Instance;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Standalone-Tool to export spectra to mgf format.
 */
public class FTreeExporterWorkflow implements Workflow {
    private final PreprocessingJob<?> ppj;
    private final FTreeExporterOptions options;


    public FTreeExporterWorkflow(PreprocessingJob<?> ppj, FTreeExporterOptions options) {
        this.options = options;
        this.ppj = ppj;
    }


    @Override
    public void run() {
        final Path outputPath = options.output;
        try {
            final Iterable<? extends Instance> ps = SiriusJobs.getGlobalJobManager().submitJob(ppj).awaitResult();
            if (Files.notExists(outputPath))
                Files.createDirectories(outputPath);
            if (!Files.isDirectory(outputPath))
                throw new IOException("The output path needs to be a directory.");

            FTDotWriter dotWriter = new FTDotWriter();
            FTJsonWriter jsonWriter = new FTJsonWriter();
            for (Instance inst : ps) {
                try {
                    List<NamedFTree> trees =  options.exportAllTrees
                            ? inst.loadFormulaResults(FTree.class).stream().map(SScored::getCandidate).map(res ->  res.getAnnotation(FTree.class).map(t -> NamedFTree.of(t, res.getId().fileName())))
                                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())
                            : inst.loadTopFormulaResult(FTree.class).flatMap(res ->  res.getAnnotation(FTree.class).map(t -> NamedFTree.of(t, res.getId().fileName()))).map(List::of).orElse(List.of());

                    if (trees.isEmpty())
                        continue;


                    for (NamedFTree nTree : trees) {
                        if (options.exportJson){
                            try (final BufferedWriter writer = Files.newBufferedWriter(outputPath.resolve(inst.getId() + "_" + nTree.name + ".json"))) {
                                jsonWriter.writeTree(writer, nTree.tree);
                            }
                        }

                        if (options.exportDot) {
                            try (final BufferedWriter writer = Files.newBufferedWriter(outputPath.resolve(inst.getId() + "_" + nTree.name + ".dot"))) {
                                dotWriter.writeTree(writer, nTree.tree);
                            }
                        }
                    }

                } catch (Exception e) {
                    LoggerFactory.getLogger(getClass()).warn("Invalid instance '" + inst + "'. Skipping this instance!", e);
                } finally {
                    inst.clearCompoundCache();
                    inst.clearFormulaResultsCache();
                }

            }
        } catch (ExecutionException e) {
            LoggerFactory.getLogger(getClass()).error("Error when reading input project!", e);
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Error when writing tree output file to: " + outputPath.toString(), e);
        }
    }

    private static class NamedFTree {
        private final FTree tree;
        private final String name;

        private NamedFTree(FTree tree, String name) {
            this.tree = tree;
            this.name = name;
        }

        public static NamedFTree of(FTree tree, String name){
            return new NamedFTree(tree, name);
        }
    }
}
