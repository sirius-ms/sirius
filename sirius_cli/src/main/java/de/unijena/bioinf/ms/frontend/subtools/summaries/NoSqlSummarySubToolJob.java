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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.PostprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.sirius.*;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.NoSQLInstance;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class NoSqlSummarySubToolJob extends PostprocessingJob<Boolean> implements Workflow {
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

    @Setter
    @Getter
    private boolean standalone = false;

    @Override
    protected Boolean compute() throws Exception {
        if (instances == null)
            instances = SiriusJobs.getGlobalJobManager().submitJob(preprocessingJob).awaitResult();

        if (!instances.iterator().hasNext())
            return null;

        final NoSQLProjectSpaceManager project;
        if (instances instanceof NoSQLProjectSpaceManager ps) {
            project = ps;
        } else {
            Instance inst = instances.iterator().next();
            if (inst.getProjectSpaceManager() instanceof NoSQLProjectSpaceManager ps)
                project = ps;
            else {
                throw new IllegalArgumentException("This summary job only supports the SIRIUS NoSQL projectSpace!");
            }
        }

        if (instances instanceof List<? extends Instance> il) {
            il.sort(Comparator.comparing(i -> i.getRT().orElse(RetentionTime.NA())));
        }

        try {
            int maxProgress = (int) Math.ceil(project.countFeatures() * 1.01d); //upper bound on number of features. selected instances could be much lower. but iterator has no count
            logInfo("Writing summary files...");
            StopWatch w = new StopWatch();
            w.start();

            Path location = options.location;
            if (location == null)
                location = Path.of(project.getLocation()).getParent().resolve(project.getName());

            Files.createDirectories(location);
            try (
                    NoSqlFormulaSummaryWriter formulaTopHit = options.topHitSummary
                            ? initFormulaSummaryWriter(location, "formula_identifications") : null;
                    NoSqlFormulaSummaryWriter formulaTopHitAdducts = options.topHitWithAdductsSummary
                            ? initFormulaSummaryWriter(location, "formula_identifications_adducts") : null;
                    NoSqlFormulaSummaryWriter formulaAll = options.fullSummary
                            ? initFormulaSummaryWriter(location, "formula_identifications_all") : null;
                    NoSqlFormulaSummaryWriter formulaTopK = options.topK > 1
                            ? initFormulaSummaryWriter(location, "formula_identifications_top-" + options.topK) : null;

                    NoSqlStructureSummaryWriter structureTopHit = options.topHitSummary
                            ? initStructureSummaryWriter(location, "structure_identifications") : null;
                    NoSqlStructureSummaryWriter structureAll = options.fullSummary
                            ? initStructureSummaryWriter(location, "structure_identifications_all") : null;
                    NoSqlStructureSummaryWriter structureTopK = options.topK > 1
                            ? initStructureSummaryWriter(location, "structure_identifications_top-" + options.topK) : null;

                    NoSqlDeNovoSummaryWriter deNovoTopHit = options.topHitSummary
                            ? initDeNovoSummaryWriter(location, "denovo_structure_identifications") : null;
                    NoSqlDeNovoSummaryWriter deNovoAll = options.fullSummary
                            ? initDeNovoSummaryWriter(location, "denovo_structure_identifications_all") : null;
                    NoSqlDeNovoSummaryWriter deNovoTopK = options.topK > 1
                            ? initDeNovoSummaryWriter(location, "denovo_structure_identifications_top-" + options.topK) : null;

                    NoSqlCanopusSummaryWriter canopusFormula = options.topHitSummary
                            ? initCanopusSummaryWriter(location, "canopus_formula_summary") : null;
                    NoSqlCanopusSummaryWriter canopusFormulaAll = options.fullSummary
                            ? initCanopusSummaryWriter(location, "canopus_formula_summary_all") : null;
                    NoSqlCanopusSummaryWriter canopusFormulaTopK = options.topK > 0
                            ? initCanopusSummaryWriter(location, "canopus_formula_summary-" + options.topK) : null;

                    // we do not stor top k or all for canopus structure since it would be redundant to the formula results.
                    NoSqlCanopusSummaryWriter canopusStructure = options.topHitSummary
                            ? initCanopusSummaryWriter(location, "canopus_structure_summary") : null;

                    NoSqlSpectrumSummaryWriter refSpectrum = options.topHitSummary
                            ? initSpectrumSummaryWriter(location, "spectral_matches") : null;
                    NoSqlSpectrumSummaryWriter refSpectrumAll = options.fullSummary
                            ? initSpectrumSummaryWriter(location, "spectral_matches_all") : null;
                    NoSqlSpectrumSummaryWriter refSpectrumTopK = options.topK > 0
                            ? initSpectrumSummaryWriter(location, "spectral_matches_top-" + options.topK) : null;

                    DataQualitySummaryWriter qualityWriter = options.qualitySummary
                            ? initQualitySummaryWriter(location, "feature_quality") : null;

                    ChemVistaSummaryWriter chemVistaWriter = options.chemVista
                            ? initChemVistaWriter(location, "chemvista_summary") : null
            ) {
                //we load all data on demand from project db without manual caching or re-usage.
                //if this turns out to be too slow we can cache e.g. the formula candidates.
                //However, without caching we ensure that the memory consumption is always the same no matter how large the dataset or the results are.
                int instanceCounter = 1;
                for (Instance inst : instances) {
                    updateProgress(maxProgress, instanceCounter++, "Writing Feature '" + inst.getExternalFeatureId().orElseGet(inst::getName) + "'...");
                    AlignedFeatures f = ((NoSQLInstance) inst).getAlignedFeatures();

                    { //formula summary
                        boolean first = true;
                        MolecularFormula lastPrecursorFormula = null;
                        //we use the formula rank for search because its index, and we do not know whether siriusScore or zodiacScore was used for ranking.
                        Filter.FilterClause sortingFilter = Filter.and(
                                Filter.where("alignedFeatureId").eq(f.getAlignedFeatureId()),
                                Filter.where("formulaRank").gt(0));

                        for (FormulaCandidate fc : project.getProject().getStorage().find(sortingFilter, FormulaCandidate.class)) {
                            boolean nothingWritten = true;

                            MolecularFormula currentPrecursorFormula = fc.getAdduct()
                                    .neutralMoleculeToMeasuredNeutralMolecule(fc.getMolecularFormula());

                            FTree ftree = project.getProject().getStorage().getByPrimaryKey(fc.getFormulaId(), FTreeResult.class)
                                    .map(FTreeResult::getFTree)
                                    .orElseThrow();

                            // write top hits
                            if (formulaTopHit != null && first) {
                                formulaTopHit.writeFormulaCandidate(f, fc, ftree);
                                nothingWritten = false;
                            }
                            if (canopusFormula != null && first) {
                                CanopusPrediction cp = project.getProject().findByFormulaIdStr(fc.getFormulaId(), CanopusPrediction.class).findFirst().orElse(null);
                                if (cp != null)
                                    canopusFormula.writeCanopusPredictions(f, fc, cp);
                                nothingWritten = false;
                            }
                            if (formulaTopHitAdducts != null && (first || currentPrecursorFormula.equals(lastPrecursorFormula))) {
                                formulaTopHitAdducts.writeFormulaCandidate(f, fc, ftree);
                                lastPrecursorFormula = currentPrecursorFormula;
                                nothingWritten = false;
                            }


                            // write top k hits
                            if (formulaTopK != null && fc.getFormulaRank() <= options.getTopK()) {
                                formulaTopK.writeFormulaCandidate(f, fc, ftree);
                                nothingWritten = false;
                            }
                            if(canopusFormulaTopK != null && fc.getFormulaRank() <= options.getTopK()){
                                CanopusPrediction cp = project.getProject().findByFormulaIdStr(fc.getFormulaId(), CanopusPrediction.class).findFirst().orElse(null);
                                if (cp != null)
                                    canopusFormulaTopK.writeCanopusPredictions(f, fc, cp);
                                nothingWritten = false;
                            }

                            // write top all hits
                            if (formulaAll != null) {
                                formulaAll.writeFormulaCandidate(f, fc, ftree);
                                nothingWritten = false;
                            }
                            if(canopusFormulaAll != null){
                                CanopusPrediction cp = project.getProject().findByFormulaIdStr(fc.getFormulaId(), CanopusPrediction.class).findFirst().orElse(null);
                                if (cp != null)
                                    canopusFormulaAll.writeCanopusPredictions(f, fc, cp);
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
                            Filter.FilterClause sortingFilter = Filter.and(
                                    Filter.where("alignedFeatureId").eq(f.getAlignedFeatureId()),
                                    Filter.where("structureRank").gt(0));

                            for (CsiStructureMatch sc : project.getProject().getStorage().find(sortingFilter, CsiStructureMatch.class)) {
                                project.getProject().fetchFingerprintCandidate(sc, false);
                                boolean nothingWritten = true;
                                FormulaCandidate fc = (lastFc != null && lastFc.getFormulaId() == sc.getFormulaId())
                                        ? lastFc : project.getProject().findByFormulaIdStr(sc.getFormulaId(), FormulaCandidate.class).findFirst().orElseThrow();

                                if (structureTopHit != null && first) {
                                    structureTopHit.writeStructureCandidate(f, fc, sc, ssr);
                                    nothingWritten = false;
                                }
                                if (canopusStructure != null && first) {
                                    CanopusPrediction cp = project.getProject().findByFormulaIdStr(fc.getFormulaId(), CanopusPrediction.class).findFirst().orElse(null);
                                    if (cp != null)
                                        canopusStructure.writeCanopusPredictions(f, fc, cp);
                                    nothingWritten = false;
                                }
                                if (chemVistaWriter != null && first) {
                                    chemVistaWriter.writeStructureCandidate(f, fc, sc, ssr);
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
                    {// Denovo summary
                        boolean first = true;
                        int rank = 1;
                        FormulaCandidate lastFc = null;
                        for (DenovoStructureMatch sc : project.getProject().findByFeatureId(f.getAlignedFeatureId(), DenovoStructureMatch.class, "structureRank", Database.SortOrder.ASCENDING)) {
                            project.getProject().fetchFingerprintCandidate(sc, false);
                            boolean nothingWritten = true;
                            FormulaCandidate fc = (lastFc != null && lastFc.getFormulaId() == sc.getFormulaId())
                                    ? lastFc : project.getProject().findByFormulaIdStr(sc.getFormulaId(), FormulaCandidate.class).findFirst().orElseThrow();

                            if (deNovoTopHit != null && first) {
                                deNovoTopHit.writeStructureCandidate(f, fc, sc);
                                nothingWritten = false;
                            }

                            if (formulaTopK != null && rank <= options.getTopK()) {
                                deNovoTopK.writeStructureCandidate(f, fc, sc);
                                nothingWritten = false;
                            }
                            if (deNovoAll != null) {
                                deNovoAll.writeStructureCandidate(f, fc, sc);
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

                    if (options.topK > 0 || options.fullSummary || options.topHitSummary) {// spectral match summary
                        List<MutableMs2Spectrum> queries = inst.getExperiment().getMs2Spectra();
                        int rank = 1;

                        Iterable<SpectraMatch> matches = project.getProject().getStorage().find(Filter.where("alignedFeatureId").eq(f.getAlignedFeatureId()), SpectraMatch.class, new String[]{"searchResult.similarity.similarity", "searchResult.similarity.sharedPeaks"}, new Database.SortOrder[]{Database.SortOrder.DESCENDING, Database.SortOrder.DESCENDING});
                        Set<String> dbs = StreamSupport.stream(matches.spliterator(), false).map(SpectraMatch::getDbName).collect(Collectors.toSet());
                        Map<String, CustomDataSources.Source> sources = new HashMap<>();
                        dbs.forEach(name -> {
                            CustomDataSources.Source source = CustomDataSources.getSourceFromName(name);
                            if (source != null) {
                                sources.put(name, source);
                            } else {
                                LoggerFactory.getLogger(this.getClass()).warn("Custom library " + name + " not found!");
                            }
                        });

                        for (SpectraMatch match : matches) {

                            if (match.getQuerySpectrumIndex() >= queries.size())
                                continue;

                            MutableMs2Spectrum query = queries.get(match.getQuerySpectrumIndex());
                            Ms2ReferenceSpectrum reference;
                            try {
                                if (sources.containsKey(match.getDbName())) {
                                    reference = ApplicationCore.WEB_API.getChemDB().getMs2ReferenceSpectrum(sources.get(match.getDbName()), match.getUuid());
                                } else {
                                    reference = null;
                                }
                            } catch (ChemicalDatabaseException e) {
                                LoggerFactory.getLogger(this.getClass()).warn("Spectral match not written to summary file. Feature ID: " + f.getAlignedFeatureId() + ". Error: " + e.getMessage());
                                continue;
                            }

                            boolean nothingWritten = true;

                            if (refSpectrum != null && rank == 1) {
                                refSpectrum.writeSpectralMatch(f, match, query, reference);
                                nothingWritten = false;
                            }

                            if (refSpectrumAll != null) {
                                refSpectrumAll.writeSpectralMatch(f, match, query, reference);
                                nothingWritten  = false;
                            }

                            if (refSpectrumTopK != null && rank <= options.getTopK()) {
                                refSpectrumTopK.writeSpectralMatch(f, match, query, reference);
                                nothingWritten = false;
                            }

                            if (nothingWritten)
                                break;

                            rank++;
                        }
                    }

                    // data quality summary
                    if (qualityWriter != null) {
                        QualityReport qr = project.getProject().findByFeatureIdStr(f.getAlignedFeatureId(), QualityReport.class).findFirst().orElse(null);
                        qualityWriter.writeFeatureQuality(f, qr);
                    }
                }

                if (formulaTopHit != null) formulaTopHit.flush();
                if (formulaTopHitAdducts != null) formulaTopHitAdducts.flush();
                if (formulaAll != null) formulaAll.flush();
                if (formulaTopK != null) formulaTopK.flush();

                if (structureTopHit != null) structureTopHit.flush();
                if (structureAll != null) structureAll.flush();
                if (structureTopK != null) structureTopK.flush();

                if (deNovoTopHit != null) deNovoTopHit.flush();
                if (deNovoAll != null) deNovoAll.flush();
                if (deNovoTopK != null) deNovoTopK.flush();

                if (canopusFormula != null) canopusFormula.flush();
                if (canopusStructure != null) canopusStructure.flush();

                if (refSpectrum != null) refSpectrum.flush();
                if (refSpectrumAll != null) refSpectrumAll.flush();
                if (refSpectrumTopK != null) refSpectrumTopK.flush();

                if (qualityWriter != null) qualityWriter.flush();
                if (chemVistaWriter != null) chemVistaWriter.flush();

                w.stop();
                log.info("Summaries written in: {}", w);
                updateProgress(maxProgress, maxProgress, "Summaries written in: " + w);
                return true;
            }
        } finally {
            if (!standalone && project != null)
                project.close(); // close project if this is a postprocessor
        }
    }

    NoSqlFormulaSummaryWriter initFormulaSummaryWriter(Path location, String filename) throws IOException {
        NoSqlFormulaSummaryWriter formulaSummaryWriter = new NoSqlFormulaSummaryWriter(makeTableWriter(location, filename));
        formulaSummaryWriter.writeHeader();
        return formulaSummaryWriter;
    }

    NoSqlCanopusSummaryWriter initCanopusSummaryWriter(Path location, String filename) throws IOException {
        NoSqlCanopusSummaryWriter canopusSummaryWriter = new NoSqlCanopusSummaryWriter(makeTableWriter(location, filename));
        canopusSummaryWriter.writeHeader();
        return canopusSummaryWriter;
    }

    NoSqlStructureSummaryWriter initStructureSummaryWriter(Path location, String filename) throws IOException {
        NoSqlStructureSummaryWriter structureSummaryWriter = new NoSqlStructureSummaryWriter(makeTableWriter(location, filename));
        structureSummaryWriter.writeHeader();
        return structureSummaryWriter;
    }

    NoSqlDeNovoSummaryWriter initDeNovoSummaryWriter(Path location, String filename) throws IOException {
        NoSqlDeNovoSummaryWriter denovoSummaryWriter = new NoSqlDeNovoSummaryWriter(makeTableWriter(location, filename));
        denovoSummaryWriter.writeHeader();
        return denovoSummaryWriter;
    }

    NoSqlSpectrumSummaryWriter initSpectrumSummaryWriter(Path location, String filename) throws IOException {
        NoSqlSpectrumSummaryWriter spectrumSummaryWriter = new NoSqlSpectrumSummaryWriter(makeTableWriter(location, filename));
        spectrumSummaryWriter.writeHeader();
        return spectrumSummaryWriter;
    }

    DataQualitySummaryWriter initQualitySummaryWriter(Path location, String filename) throws IOException {
        DataQualitySummaryWriter writer = new DataQualitySummaryWriter(makeTableWriter(location, filename));
        writer.writeHeader();
        return writer;
    }

    ChemVistaSummaryWriter initChemVistaWriter(Path location, String filename) throws IOException {
        ChemVistaSummaryWriter writer = new ChemVistaSummaryWriter(new CsvTableWriter(location, filename, options.quoteStrings));
        writer.writeHeader();
        return writer;
    }

    private SummaryTableWriter makeTableWriter(Path location, String filename) throws IOException {
        return switch (options.format) {
            case TSV -> new TsvTableWriter(location, filename, options.quoteStrings);
            case ZIP -> new ZipTableWriter(location, filename, options.quoteStrings);
            case CSV -> new CsvTableWriter(location, filename, options.quoteStrings);
            case XLSX -> new XlsxTableWriter(location, filename);
        };
    }


    @Override
    public void cancel() {
        cancel(false);
    }

    @Override
    protected void cleanup() {
        instances = null;
        preprocessingJob = null;
        super.cleanup();
    }

    @Override
    public void run() {
        setStandalone(true);
        SiriusJobs.getGlobalJobManager().submitJob(this).takeResult();
    }
}
