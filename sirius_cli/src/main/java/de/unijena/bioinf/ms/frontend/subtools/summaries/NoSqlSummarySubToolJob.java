/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.summaries;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.frontend.subtools.PostprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.sirius.CsiStructureMatch;
import de.unijena.bioinf.ms.persistence.model.sirius.CsiStructureSearchResult;
import de.unijena.bioinf.ms.persistence.model.sirius.FTreeResult;
import de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.NoSQLInstance;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.projectspace.SiriusProjectSpaceInstance;
import de.unijena.bioinf.storage.db.nosql.Database;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class NoSqlSummarySubToolJob extends PostprocessingJob<Boolean> {
    private static final Logger LOG = LoggerFactory.getLogger(NoSqlSummarySubToolJob.class);
    private final SummaryOptions options;

    private @Nullable PreprocessingJob<?> preprocessingJob;
    private Iterable<? extends Instance> instances;

    public NoSqlSummarySubToolJob(@Nullable PreprocessingJob<?> preprocessingJob, SummaryOptions options) {
        this.preprocessingJob = preprocessingJob;
        this.options = options;
    }

    public NoSqlSummarySubToolJob(SummaryOptions options) {
        this(null, options);
    }

    @Override
    public void setInput(Iterable<? extends Instance> instances, ParameterConfig config) {
        this.instances = instances;
    }

    private boolean standalone = false;

    public boolean isStandalone() {
        return standalone;
    }

    public void setStandalone(boolean standalone) {
        this.standalone = standalone;
    }

    @Override
    protected Boolean compute() throws Exception {
        if (instances == null)
            instances = SiriusJobs.getGlobalJobManager().submitJob(preprocessingJob).awaitResult();

        if (!instances.iterator().hasNext())
            return null;

        final NoSQLProjectSpaceManager project;
        if (instances instanceof NoSQLProjectSpaceManager) {
            project = (NoSQLProjectSpaceManager) instances;
        } else {
            Instance inst = instances.iterator().next();
            if (inst instanceof SiriusProjectSpaceInstance)
                project = (NoSQLProjectSpaceManager) inst.getProjectSpaceManager();
            else {
                throw new IllegalArgumentException("This summary job only supports the SIRIUS projectSpace!");
            }
        }

        try {
            int maxProgress = Utils.withTimeR("Counting Feature took: ", w -> project.countFeatures());
            //use all experiments in workspace to create summaries
            LOG.info("Writing summary files...");
            StopWatch w = new StopWatch();
            w.start();

            Files.createDirectories(options.location);
            try (
                    NoSqlFormulaSummaryWriter formulaTopHit = options.topHitSummary
                            ? initFormulaSummaryWriter("formula_identifications.tsv") : null;
                    NoSqlFormulaSummaryWriter formulaTopHitAdducts = options.topHitWithAdductsSummary
                            ? initFormulaSummaryWriter("formula_identifications_adducts.tsv") : null;
                    NoSqlFormulaSummaryWriter formulaAll = options.fullSummary
                            ? initFormulaSummaryWriter("formula_identifications_all.tsv") : null;
                    NoSqlFormulaSummaryWriter formulaTopK = options.topK > 1
                            ? initFormulaSummaryWriter("formula_identifications_top-" + options.topK + ".tsv") : null;
                    NoSqlStructureSummaryWriter structureTopHit = options.topHitSummary
                            ? initStructureSummaryWriter("compound_identifications.tsv") : null;
                    NoSqlStructureSummaryWriter structureAll = options.fullSummary
                            ? initStructureSummaryWriter("compound_identifications_all.tsv") : null;
                    NoSqlStructureSummaryWriter structureTopK = options.topK > 1
                            ? initStructureSummaryWriter("compound_identifications_top-" + options.topK + ".tsv") : null;

            ) {
                //we load all data on demand from project db without manual caching or re-usage.
                //if this turns out to be too slow we can cache e.g. the formula candidates.
                //However, without caching we ensure that the memory consumption is always the same no matter how large the dataset or the results are.
                int instanceCounter = 1;
                for (Instance inst : project) {
                    AlignedFeatures f = ((NoSQLInstance) inst).getAlignedFeatures();

                    { //formula summary
                        boolean first = true;
                        MolecularFormula lastPrecursorFormula = null;
                        //we use the formula rank for search because its index an we do not know whether siriusScore or zodiacScore was used for ranking.
                        for (FormulaCandidate fc : project.getProject().findByFeatureId(f.getAlignedFeatureId(), FormulaCandidate.class, "formulaRank", Database.SortOrder.ASCENDING)) {
                            boolean nothingWritten = true;

                            MolecularFormula currentPrecursorFormula = fc.getAdduct()
                                    .neutralMoleculeToMeasuredNeutralMolecule(fc.getMolecularFormula());

                            FTree ftree = project.getProject().getStorage().getByPrimaryKey(fc.getFormulaId(), FTreeResult.class)
                                    .map(FTreeResult::getFTree)
                                    .orElseThrow();

                            if (formulaTopHit != null && first) {
                                formulaTopHit.writeFormulaCandidate(f, fc, ftree);
                                nothingWritten = false;
                            }
                            if (formulaTopHitAdducts != null && (first || currentPrecursorFormula.equals(lastPrecursorFormula))) {
                                formulaTopHitAdducts.writeFormulaCandidate(f, fc, ftree);
                                lastPrecursorFormula = currentPrecursorFormula;
                                nothingWritten = false;
                            }
                            if (formulaTopK != null && fc.getFormulaRank() <= options.getTopK()) {
                                formulaTopK.writeFormulaCandidate(f, fc, ftree);
                                nothingWritten = false;
                            }
                            if (formulaAll != null) {
                                formulaAll.writeFormulaCandidate(f, fc, ftree);
                                nothingWritten = false;
                            }
                            if (nothingWritten)
                                break;

                            //iterating
                            first = false;

                        }
                    }

                    {// structure summary
                        CsiStructureSearchResult ssr = project.getProject().findByFeatureIdStr(f.getAlignedFeatureId(), CsiStructureSearchResult.class).findFirst().orElse(null);
                        if (ssr != null) {
                            boolean first = true;
                            int rank = 1;
                            FormulaCandidate lastFc = null;
                            for (CsiStructureMatch sc : project.getProject().findByFeatureId(f.getAlignedFeatureId(), CsiStructureMatch.class, "structureRank", Database.SortOrder.ASCENDING)) {
                                project.getProject().fetchFingerprintCandidate(sc, false);
                                boolean nothingWritten = true;
                                FormulaCandidate fc = (lastFc != null && lastFc.getFormulaId() == sc.getFormulaId())
                                        ? lastFc : project.getProject().findByFormulaIdStr(sc.getFormulaId(), FormulaCandidate.class).findFirst().orElseThrow();

                                if (structureTopHit != null && first) {
                                    structureTopHit.writeStructureCandidate(f, fc, sc, ssr);
                                    nothingWritten = false;
                                }
                                if (formulaTopK != null && rank <= options.getTopK()) {
                                    structureTopK.writeStructureCandidate(f, fc, sc, ssr);
                                    nothingWritten = false;
                                }
                                if (structureAll != null) {
                                    structureAll.writeStructureCandidate(f, fc, sc, ssr);
                                    nothingWritten = false;
                                }
                                if (nothingWritten)
                                    break;

                                //iterating
                                lastFc = fc;
                                rank++;
                                first = false;
                            }
                        }
                    }
                    updateProgress(maxProgress, instanceCounter++, "Finished Feature: " + f.getExternalFeatureId());
                }
                w.stop();
                LOG.info("Project-Space summaries successfully written in: " + w);
                return true;
            }
        } finally {
            if (!standalone && project != null)
                project.close(); // close project if this is a postprocessor
        }
    }

    NoSqlFormulaSummaryWriter initFormulaSummaryWriter(String filename) throws IOException {
        NoSqlFormulaSummaryWriter formulaSummaryWriter = new NoSqlFormulaSummaryWriter(
                Files.newBufferedWriter(options.location.resolve(filename), StandardCharsets.UTF_8));
        formulaSummaryWriter.writeHeader();
        return formulaSummaryWriter;
    }

    NoSqlStructureSummaryWriter initStructureSummaryWriter(String filename) throws IOException {
        NoSqlStructureSummaryWriter structureSummaryWriter = new NoSqlStructureSummaryWriter(
                Files.newBufferedWriter(options.location.resolve(filename), StandardCharsets.UTF_8));
        structureSummaryWriter.writeHeader();
        return structureSummaryWriter;
    }


    @Override
    public void cancel() {
        cancel(true);
    }

    @Override
    protected void cleanup() {
        instances = null;
        preprocessingJob = null;
        super.cleanup();
    }
}
