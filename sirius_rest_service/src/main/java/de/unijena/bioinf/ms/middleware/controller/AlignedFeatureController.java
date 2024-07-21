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

package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.middleware.configuration.GlobalConfig;
import de.unijena.bioinf.ms.middleware.model.annotations.*;
import de.unijena.bioinf.ms.middleware.model.compute.InstrumentProfile;
import de.unijena.bioinf.ms.middleware.model.features.*;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import de.unijena.bioinf.ms.middleware.model.spectra.Spectrums;
import de.unijena.bioinf.ms.middleware.service.databases.ChemDbService;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils.removeNone;

@RestController
@RequestMapping(value = "/api/projects/{projectId}/aligned-features")
@Tag(name = "Features", description = "This feature based API allows access features (aligned over runs) and there Annotations of " +
        "a specified project-space. This is the entry point to access all raw annotation results an there summaries.")
public class AlignedFeatureController {

    private final ProjectsProvider<?> projectsProvider;
    private final ChemDbService chemDbService;
    private final GlobalConfig globalConfig;

    @Autowired
    public AlignedFeatureController(ProjectsProvider<?> projectsProvider, ChemDbService chemDbService, GlobalConfig globalConfig) {
        this.projectsProvider = projectsProvider;
        this.chemDbService = chemDbService;
        this.globalConfig = globalConfig;
    }

    /**
     * Get all available features (aligned over runs) in the given project-space.
     *
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     * @return AlignedFeatures with additional annotations and MS/MS data (if specified).
     */
    @GetMapping(value = "/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<AlignedFeature> getAlignedFeaturesPaged(
            @PathVariable String projectId, @ParameterObject Pageable pageable,
            @RequestParam(defaultValue = "") EnumSet<AlignedFeature.OptField> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId).findAlignedFeatures(pageable, removeNone(optFields));
    }

    /**
     * Get all available features (aligned over runs) in the given project-space.
     *
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     * @return AlignedFeatures with additional annotations and MS/MS data (if specified).
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AlignedFeature> getAlignedFeatures(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "") EnumSet<AlignedFeature.OptField> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId).findAlignedFeatures(Pageable.unpaged(), removeNone(optFields))
                .getContent();
    }

    /**
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     *
     * @param projectId        project-space to delete from.
     */
    @PutMapping(value = "/delete")
    public void deleteAlignedFeatures(@PathVariable String projectId, @RequestBody List<String> alignedFeatureIds) {
        projectsProvider.getProjectOrThrow(projectId).deleteAlignedFeaturesByIds(alignedFeatureIds);
    }

    /**
     * Import (aligned) features into the project. Features must not exist in the project.
     * Otherwise, they will exist twice.
     *
     * @param projectId project-space to import into.
     * @param features  the feature data to be imported
     * @param profile profile describing the instrument used to measure the data. Used to merge spectra.
     * @param optFields set of optional fields to be included. Use 'none' to override defaults.
     * @return the Features that have been imported with specified optional fields
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AlignedFeature> addAlignedFeatures(@PathVariable String projectId, @Valid @RequestBody List<FeatureImport> features,
                                                   @RequestParam(required = false) InstrumentProfile profile,
                                                   @RequestParam(defaultValue = "") EnumSet<AlignedFeature.OptField> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId).addAlignedFeatures(features, profile, removeNone(optFields));
    }


    /**
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @return AlignedFeature with additional annotations and MS/MS data (if specified).
     */
    @GetMapping(value = "/{alignedFeatureId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AlignedFeature getAlignedFeature(
            @PathVariable String projectId, @PathVariable String alignedFeatureId,
            @RequestParam(defaultValue = "") EnumSet<AlignedFeature.OptField> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId).findAlignedFeaturesById(alignedFeatureId, removeNone(optFields));
    }

    /**
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     *
     * @param projectId        project-space to delete from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to delete.
     */
    @DeleteMapping(value = "/{alignedFeatureId}")
    public void deleteAlignedFeature(@PathVariable String projectId, @PathVariable String alignedFeatureId) {
        projectsProvider.getProjectOrThrow(projectId).deleteAlignedFeaturesById(alignedFeatureId);
    }

    /**
     * Page of structure database search candidates ranked by CSI:FingerID score for the given 'alignedFeatureId' with minimal information.
     * StructureCandidates can be enriched with molecular fingerprint, structure database links.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @return StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     */
    @GetMapping(value = "/{alignedFeatureId}/db-structures/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<StructureCandidateFormula> getStructureCandidatesPaged(
            @PathVariable String projectId, @PathVariable String alignedFeatureId,
            @ParameterObject Pageable pageable,
            @RequestParam(defaultValue = "") EnumSet<StructureCandidateScored.OptField> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId)
                .findStructureCandidatesByFeatureId(alignedFeatureId, pageable, removeNone(optFields));
    }

    /**
     * List of structure database search candidates ranked by CSI:FingerID score for the given 'alignedFeatureId' with minimal information.
     * StructureCandidates can be enriched with molecular fingerprint, structure database links.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @return StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     */
    @GetMapping(value = "/{alignedFeatureId}/db-structures", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<StructureCandidateFormula> getStructureCandidates(
            @PathVariable String projectId, @PathVariable String alignedFeatureId,
            @RequestParam(defaultValue = "") EnumSet<StructureCandidateScored.OptField> optFields
    ) {
        return getStructureCandidatesPaged(projectId, alignedFeatureId, globalConfig.unpaged(), optFields).stream().toList();
    }

    /**
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given 'alignedFeatureId' with minimal information.
     * StructureCandidates can be enriched with molecular fingerprint.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @return StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     */
    @GetMapping(value = "/{alignedFeatureId}/denovo-structures/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<StructureCandidateFormula> getDeNovoStructureCandidatesPaged(
            @PathVariable String projectId, @PathVariable String alignedFeatureId,
            @ParameterObject Pageable pageable,
            @RequestParam(defaultValue = "") EnumSet<StructureCandidateScored.OptField> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId)
                .findDeNovoStructureCandidatesByFeatureId(alignedFeatureId, pageable, removeNone(optFields));
    }

    /**
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given 'alignedFeatureId' with minimal information.
     * StructureCandidates can be enriched with molecular fingerprint.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @return StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     */
    @GetMapping(value = "/{alignedFeatureId}/denovo-structures", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<StructureCandidateFormula> getDeNovoStructureCandidates(
            @PathVariable String projectId, @PathVariable String alignedFeatureId,
            @RequestParam(defaultValue = "") EnumSet<StructureCandidateScored.OptField> optFields
    ) {
        return getDeNovoStructureCandidatesPaged(projectId, alignedFeatureId, globalConfig.unpaged(), optFields).stream().toList();
    }

    /**
     * Summarize matched reference spectra for the given 'alignedFeatureId'.
     * If a 'candidateInChiKey' is provided, summarizes only matches for the database compound with the given InChI key.
     *
     * @param projectId         project-space to read from.
     * @param alignedFeatureId  feature (aligned over runs) the structure candidates belong to.
     * @param minSharedPeaks    min threshold of shared peaks.
     * @param minSimilarity     min spectral similarity threshold.
     * @param candidateInChiKey inchi key of the database compound.
     * @return Summary object with best match, number of spectral library matches, matched reference spectra and matched database compounds of this feature (aligned over runs).
     */
    @GetMapping(value = "/{alignedFeatureId}/spectral-library-matches/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public SpectralLibraryMatchSummary getSpectralLibraryMatchesSummary(
            @PathVariable String projectId,
            @PathVariable String alignedFeatureId,
            @RequestParam(defaultValue = "1") int minSharedPeaks,
            @RequestParam(defaultValue = "0.2") double minSimilarity,
            @RequestParam(defaultValue = "") @Nullable String candidateInChiKey
    ) {
        minSharedPeaks = Math.max(minSharedPeaks, 0);
        minSimilarity = Math.min(Math.max(minSimilarity, 0d), 1d);
        if (candidateInChiKey == null || candidateInChiKey.isEmpty() || candidateInChiKey.isBlank()) {
            return projectsProvider.getProjectOrThrow(projectId).summarizeLibraryMatchesByFeatureId(alignedFeatureId, minSharedPeaks, minSimilarity);
        } else {
            return projectsProvider.getProjectOrThrow(projectId).summarizeLibraryMatchesByFeatureIdAndInchi(alignedFeatureId, candidateInChiKey, minSharedPeaks, minSimilarity);
        }
    }

    /**
     * Page of spectral library matches for the given 'alignedFeatureId'.
     * If a 'candidateInChiKey' is provided, returns only matches for the database compound with the given InChI key.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @return Spectral library matches of this feature (aligned over runs).
     */
    @GetMapping(value = "/{alignedFeatureId}/spectral-library-matches/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<SpectralLibraryMatch> getSpectralLibraryMatchesPaged(
            @PathVariable String projectId,
            @PathVariable String alignedFeatureId,
            @ParameterObject Pageable pageable,
            @RequestParam(defaultValue = "1") int minSharedPeaks,
            @RequestParam(defaultValue = "0.2") double minSimilarity,
            @RequestParam(defaultValue = "") @Nullable String candidateInChiKey,
            @RequestParam(defaultValue = "") EnumSet<SpectralLibraryMatch.OptField> optFields
    ) {
        minSharedPeaks = Math.max(minSharedPeaks, 0);
        minSimilarity = Math.min(Math.max(minSimilarity, 0d), 1d);
        Page<SpectralLibraryMatch> matches;
        if (candidateInChiKey == null || candidateInChiKey.isEmpty() || candidateInChiKey.isBlank()) {
            matches = projectsProvider.getProjectOrThrow(projectId).findLibraryMatchesByFeatureId(alignedFeatureId, minSharedPeaks, minSimilarity, pageable);
        } else {
            matches = projectsProvider.getProjectOrThrow(projectId).findLibraryMatchesByFeatureIdAndInchi(alignedFeatureId, candidateInChiKey, minSharedPeaks, minSimilarity, pageable);
        }

        if (matches != null && optFields.contains(SpectralLibraryMatch.OptField.referenceSpectrum))
            matches.getContent().forEach(match -> CustomDataSources.getSourceFromNameOpt(match.getDbName()).ifPresentOrElse(
                    db -> {
                        try {
                            Ms2ReferenceSpectrum spec = chemDbService.db().getReferenceSpectrum(db, match.getUuid(), true);
                            match.setReferenceSpectrum(Spectrums.createMs2ReferenceSpectrum(spec));


                        } catch (ChemicalDatabaseException e) {
                            LoggerFactory.getLogger(getClass()).error("Could not load Spectrum: " + match.getUuid(), e);
                        }
                    }, () -> LoggerFactory.getLogger(getClass()).warn("Could not load Spectrum! Custom database not available: " + match.getDbName())
            ));
        return matches;
    }

    /**
     * List of spectral library matches for the given 'alignedFeatureId'.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @return Spectral library matches of this feature (aligned over runs).
     */
    @GetMapping(value = "/{alignedFeatureId}/spectral-library-matches", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SpectralLibraryMatch> getSpectralLibraryMatches(
            @PathVariable String projectId,
            @PathVariable String alignedFeatureId,
            @RequestParam(defaultValue = "1") int minSharedPeaks,
            @RequestParam(defaultValue = "0.2") double minSimilarity,
            @RequestParam(defaultValue = "") @Nullable String candidateInChiKey,
            @RequestParam(defaultValue = "") EnumSet<SpectralLibraryMatch.OptField> optFields
    ) {
        return getSpectralLibraryMatchesPaged(projectId, alignedFeatureId, globalConfig.unpaged(), minSharedPeaks, minSimilarity, candidateInChiKey, optFields).stream().toList();
    }

    /**
     * List of spectral library matches for the given 'alignedFeatureId'.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @return Spectral library matches of this feature (aligned over runs).
     */
    @GetMapping(value = "/{alignedFeatureId}/spectral-library-matches/{matchId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public SpectralLibraryMatch getSpectralLibraryMatch(
            @PathVariable String projectId, @PathVariable String alignedFeatureId, @PathVariable String matchId,
            @RequestParam(defaultValue = "") EnumSet<SpectralLibraryMatch.OptField> optFields
    ) {
        SpectralLibraryMatch match = projectsProvider.getProjectOrThrow(projectId)
                .findLibraryMatchesByFeatureIdAndMatchId(alignedFeatureId, matchId);


        if (optFields.contains(SpectralLibraryMatch.OptField.referenceSpectrum))
           CustomDataSources.getSourceFromNameOpt(match.getDbName()).ifPresentOrElse(
                    db -> {
                        try {
                            Ms2ReferenceSpectrum spec = chemDbService.db().getReferenceSpectrum(db, match.getUuid(), true);
                            match.setReferenceSpectrum(Spectrums.createMs2ReferenceSpectrum(spec));


                        } catch (ChemicalDatabaseException e) {
                            LoggerFactory.getLogger(getClass()).error("Could not load Spectrum: " + match.getUuid(), e);
                        }
                    }, () -> LoggerFactory.getLogger(getClass()).warn("Could not load Spectrum! Custom database not available: " + match.getDbName())
            );
        return match;
    }

    /**
     * Mass Spec data (input data) for the given 'alignedFeatureId' .
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the Mass Spec data belong sto.
     * @return Mass Spec data of this feature (aligned over runs).
     */
    @GetMapping(value = "/{alignedFeatureId}/ms-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public MsData getMsData(@PathVariable String projectId, @PathVariable String alignedFeatureId) {
        MsData msData = projectsProvider.getProjectOrThrow(projectId)
                .findAlignedFeaturesById(alignedFeatureId, AlignedFeature.OptField.msData).getMsData();
        if (msData == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MsData for '" + idString(projectId, alignedFeatureId) + "' not found!");
        return msData;
    }

    /**
     * Page of FormulaResultContainers available for this feature with minimal information.
     * Can be enriched with an optional results overview.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @return All FormulaCandidate of this feature with.
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<FormulaCandidate> getFormulaCandidatesPaged(
            @PathVariable String projectId, @PathVariable String alignedFeatureId, @ParameterObject Pageable pageable,
            @RequestParam(defaultValue = "") EnumSet<FormulaCandidate.OptField> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId)
                .findFormulaCandidatesByFeatureId(alignedFeatureId, pageable, removeNone(optFields));
    }

    /**
     * List of FormulaResultContainers available for this feature with minimal information.
     * Can be enriched with an optional results overview.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @return All FormulaCandidate of this feature with.
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FormulaCandidate> getFormulaCandidates(
            @PathVariable String projectId, @PathVariable String alignedFeatureId,
            @RequestParam(defaultValue = "") EnumSet<FormulaCandidate.OptField> optFields
    ) {
        return getFormulaCandidatesPaged(projectId, alignedFeatureId, globalConfig.unpaged(), optFields).stream().toList();
    }

    /**
     * FormulaResultContainers for the given 'formulaId' with minimal information.
     * Can be enriched with an optional results overview and formula candidate information.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @return FormulaCandidate of this feature (aligned over runs) with.
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public FormulaCandidate getFormulaCandidate(
            @PathVariable String projectId, @PathVariable String alignedFeatureId, @PathVariable String formulaId,
            @RequestParam(defaultValue = "") EnumSet<FormulaCandidate.OptField> optFields

    ) {
        return projectsProvider.getProjectOrThrow(projectId)
                .findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId, removeNone(optFields));
    }

    /**
     * Page of CSI:FingerID structure database search candidates for the given 'formulaId' with minimal information.
     * StructureCandidates can be enriched with molecular fingerprint, structure database links.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @return StructureCandidate of this formula candidate with specified optional fields.
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/db-structures/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<StructureCandidateScored> getStructureCandidatesByFormulaPaged(
            @PathVariable String projectId, @PathVariable String alignedFeatureId, @PathVariable String formulaId,
            @ParameterObject Pageable pageable,
            @RequestParam(defaultValue = "") EnumSet<StructureCandidateScored.OptField> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId)
                .findStructureCandidatesByFeatureIdAndFormulaId(formulaId, alignedFeatureId, pageable, removeNone(optFields));
    }

    /**
     * List of CSI:FingerID structure database search candidates for the given 'formulaId' with minimal information.
     * StructureCandidates can be enriched with molecular fingerprint, structure database links.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @return StructureCandidate of this formula candidate with specified optional fields.
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/db-structures", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<StructureCandidateScored> getStructureCandidatesByFormula(
            @PathVariable String projectId, @PathVariable String alignedFeatureId, @PathVariable String formulaId,
            @RequestParam(defaultValue = "") EnumSet<StructureCandidateScored.OptField> optFields
    ) {
        return getStructureCandidatesByFormulaPaged(projectId, alignedFeatureId,formulaId, globalConfig.unpaged(), optFields)
                .stream().toList();
    }

    /**
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given 'formulaId' with minimal information.
     * StructureCandidates can be enriched with molecular fingerprint.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @return StructureCandidate of this formula candidate with specified optional fields.
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/denovo-structures/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<StructureCandidateScored> getDeNovoStructureCandidatesByFormulaPaged(
            @PathVariable String projectId, @PathVariable String alignedFeatureId, @PathVariable String formulaId,
            @ParameterObject Pageable pageable,
            @RequestParam(defaultValue = "") EnumSet<StructureCandidateScored.OptField> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId)
                .findDeNovoStructureCandidatesByFeatureIdAndFormulaId(formulaId, alignedFeatureId, pageable, removeNone(optFields));
    }

    /**
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given 'formulaId' with minimal information.
     * StructureCandidates can be enriched with molecular fingerprint.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @return StructureCandidate of this formula candidate with specified optional fields.
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/denovo-structures", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<StructureCandidateScored> getDeNovoStructureCandidatesByFormula(
            @PathVariable String projectId, @PathVariable String alignedFeatureId, @PathVariable String formulaId,
            @RequestParam(defaultValue = "") EnumSet<StructureCandidateScored.OptField> optFields
    ) {
        return getDeNovoStructureCandidatesByFormulaPaged(projectId, alignedFeatureId, formulaId, globalConfig.unpaged(), optFields)
                .stream().toList();
    }

    /**
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier
     * These annotations are only available if a fragmentation tree is available.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @param inchiKey         2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @param spectrumIndex    index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex < 0 (default)
     * @return Fragmentation spectrum annotated with fragments and sub-structures.
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/structures/{inchiKey}/annotated-spectrum", produces = MediaType.APPLICATION_JSON_VALUE)
    public AnnotatedSpectrum getStructureAnnotatedSpectrum(@PathVariable String projectId,
                                                           @PathVariable String alignedFeatureId,
                                                           @PathVariable String formulaId,
                                                           @PathVariable String inchiKey,
                                                           @RequestParam(defaultValue = "-1") int spectrumIndex
    ) {
        AnnotatedSpectrum res = projectsProvider.getProjectOrThrow(projectId)
                .findAnnotatedSpectrumByStructureId(spectrumIndex, inchiKey, formulaId, alignedFeatureId);
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Annotated MS/MS Spectrum for '"
                    + idString(projectId, alignedFeatureId, formulaId)
                    + "' not available! Maybe because FragmentationTree is missing?");

        return res;
    }

    /**
     * Returns MS/MS Data (Merged MS/MS and list of measured MS/MS ) which are annotated with fragments and losses
     * for the given formula result identifier and structure candidate inChIKey.
     * These annotations are only available if a fragmentation tree and the structure candidate are available.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @param inchiKey         2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @return Fragmentation spectrum annotated with fragments and sub-structures.
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/structures/{inchiKey}/annotated-msmsdata", produces = MediaType.APPLICATION_JSON_VALUE)
    public AnnotatedMsMsData getStructureAnnotatedMsData(@PathVariable String projectId,
                                                         @PathVariable String alignedFeatureId,
                                                         @PathVariable String formulaId,
                                                         @PathVariable String inchiKey
    ) {
        AnnotatedMsMsData res = projectsProvider.getProjectOrThrow(projectId)
                .findAnnotatedMsMsDataByStructureId(inchiKey, formulaId, alignedFeatureId);
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Annotated MS/MS Spectrum for '"
                    + idString(projectId, alignedFeatureId, formulaId)
                    + "' not available! Maybe because FragmentationTree is missing?");

        return res;
    }


    /**
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier in SIRIUS' internal format.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @return Fragmentation Tree in internal format.
     * <p>
     * NOTE: This endpoint is likely to be removed in future versions of the API.
     */
    @Deprecated
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/sirius-fragtree", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getSiriusFragTree(@PathVariable String projectId, @PathVariable String alignedFeatureId, @PathVariable String formulaId) {
        String json = projectsProvider.getProjectOrThrow(projectId).findSiriusFtreeJsonById(formulaId, alignedFeatureId);
        if (json == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FragmentationTree for '" + idString(projectId, alignedFeatureId, formulaId) + "' not found!");
        return json;
    }

    /**
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier
     * This tree is used to rank formula candidates (treeScore).
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @return Fragmentation Tree
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/fragtree", produces = MediaType.APPLICATION_JSON_VALUE)
    public FragmentationTree getFragTree(@PathVariable String projectId, @PathVariable String alignedFeatureId, @PathVariable String formulaId) {
        FragmentationTree res = projectsProvider.getProjectOrThrow(projectId)
                .findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId, FormulaCandidate.OptField.fragmentationTree)
                .getFragmentationTree();
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FragmentationTree for '" + idString(projectId, alignedFeatureId, formulaId) + "' not found!");
        return res;
    }

    /**
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier
     * These annotations are only available if a fragmentation tree is available.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @param spectrumIndex    index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex < 0 (default)
     * @return Fragmentation spectrum annotated with fragment formulas and losses.
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/annotated-spectrum", produces = MediaType.APPLICATION_JSON_VALUE)
    public AnnotatedSpectrum getFormulaAnnotatedSpectrum(@PathVariable String projectId,
                                                         @PathVariable String alignedFeatureId,
                                                         @PathVariable String formulaId,
                                                         @RequestParam(defaultValue = "-1") int spectrumIndex) {
        AnnotatedSpectrum res = projectsProvider.getProjectOrThrow(projectId)
                .findAnnotatedSpectrumByFormulaId(spectrumIndex, formulaId, alignedFeatureId);
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Annotated MS/MS Spectrum for '"
                    + idString(projectId, alignedFeatureId, formulaId)
                    + "' not available! Maybe because FragmentationTree is missing?");

        return res;
    }

    /**
     * Returns MS/MS Spectrum (Merged MS/MS and measured MS/MS) which is annotated with fragments and losses
     * for the given formula result identifier
     * These annotations are only available if a fragmentation tree and the structure candidate are available.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @return Fragmentation spectra annotated with fragment formulas and losses.
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/annotated-msmsdata", produces = MediaType.APPLICATION_JSON_VALUE)
    public AnnotatedMsMsData getFormulaAnnotatedMsMsData(@PathVariable String projectId,
                                                         @PathVariable String alignedFeatureId,
                                                         @PathVariable String formulaId
    ) {
        AnnotatedMsMsData res = projectsProvider.getProjectOrThrow(projectId)
                .findAnnotatedMsMsDataByFormulaId(formulaId, alignedFeatureId);
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Annotated MS/MS Data for '"
                    + idString(projectId, alignedFeatureId, formulaId)
                    + "' not available! Maybe because FragmentationTree is missing?");

        return res;
    }

    /**
     * Returns Isotope pattern information (simulated isotope pattern, measured isotope pattern, isotope pattern highlighting)
     * for the given formula result identifier. This simulated isotope pattern is used to rank formula candidates (treeScore).
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @return Isotope pattern information
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/isotope-pattern", produces = MediaType.APPLICATION_JSON_VALUE)
    public IsotopePatternAnnotation getIsotopePatternAnnotation(@PathVariable String projectId, @PathVariable String alignedFeatureId, @PathVariable String formulaId) {
        IsotopePatternAnnotation res = projectsProvider.getProjectOrThrow(projectId)
                .findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId, FormulaCandidate.OptField.isotopePattern)
                .getIsotopePatternAnnotation();
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Isotope Pattern for '" + idString(projectId, alignedFeatureId, formulaId) + "' not found!");
        return res;
    }

    /**
     * Returns Lipid annotation (ElGordo) for the given formula result identifier.
     * ElGordo lipid annotation runs as part of the SIRIUS formula identification step.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @return LipidAnnotation
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/lipid-annotation", produces = MediaType.APPLICATION_JSON_VALUE)
    public LipidAnnotation getLipidAnnotation(@PathVariable String projectId, @PathVariable String alignedFeatureId, @PathVariable String formulaId) {
        LipidAnnotation res = projectsProvider.getProjectOrThrow(projectId)
                .findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId, FormulaCandidate.OptField.lipidAnnotation)
                .getLipidAnnotation();
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lipid annotation for '" + idString(projectId, alignedFeatureId, formulaId) + "' not found!");
        return res;
    }

    /**
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier
     * This fingerprint is used to perform structure database search and predict compound classes.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @return probabilistic fingerprint predicted by CSI:FingerID
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/fingerprint", produces = MediaType.APPLICATION_JSON_VALUE)
    public double[] getFingerprintPrediction(@PathVariable String projectId, @PathVariable String alignedFeatureId, @PathVariable String formulaId) {
        double[] res = projectsProvider.getProjectOrThrow(projectId)
                .findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId, FormulaCandidate.OptField.predictedFingerprint)
                .getPredictedFingerprint();
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fingerprint for '" + idString(projectId, alignedFeatureId, formulaId) + "' not found!");
        return res;
    }

    /**
     * All predicted compound classes (CANOPUS) from ClassyFire and NPC and their probabilities,
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @return Predicted compound classes
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/canopus-prediction", produces = MediaType.APPLICATION_JSON_VALUE)
    public CanopusPrediction getCanopusPrediction(@PathVariable String projectId, @PathVariable String alignedFeatureId, @PathVariable String formulaId) {
        CanopusPrediction res = projectsProvider.getProjectOrThrow(projectId)
                .findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId, FormulaCandidate.OptField.canopusPredictions)
                .getCanopusPrediction();
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Compound Classes for '" + idString(projectId, alignedFeatureId, formulaId) + "' not found!");
        return res;
    }

    /**
     * Best matching compound classes,
     * Set of the highest scoring compound classes (CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology,
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @return Best matching Predicted compound classes
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/best-compound-classes", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompoundClasses getBestMatchingCompoundClasses(@PathVariable String projectId, @PathVariable String alignedFeatureId, @PathVariable String formulaId) {
        CompoundClasses res = projectsProvider.getProjectOrThrow(projectId)
                .findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId, FormulaCandidate.OptField.compoundClasses)
                .getCompoundClasses();
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Compound Classes for '" + idString(projectId, alignedFeatureId, formulaId) + "' not found!");
        return res;
    }

    /*
        LCMS Stuff
     */

    @GetMapping(value = "/{alignedFeatureId}/quantification", produces = MediaType.APPLICATION_JSON_VALUE)
    public QuantificationTable getQuantification(@PathVariable String projectId, @PathVariable String alignedFeatureId, @RequestParam(defaultValue = "APEX_HEIGHT") QuantificationTable.QuantificationType type) {
        Optional<QuantificationTable> quantificationForAlignedFeature = projectsProvider.getProjectOrThrow(projectId).getQuantificationForAlignedFeature(alignedFeatureId, type);
        if (quantificationForAlignedFeature.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No quantification information available for " + idString(projectId, alignedFeatureId) + " and quantification type " + type );
        else return quantificationForAlignedFeature.get();
    }

    @GetMapping(value = "/{alignedFeatureId}/traces", produces = MediaType.APPLICATION_JSON_VALUE)
    public TraceSet getTraces(@PathVariable String projectId, @PathVariable String alignedFeatureId) {
        Optional<TraceSet> traceSet = projectsProvider.getProjectOrThrow(projectId).getTraceSetForAlignedFeature(alignedFeatureId);
        if (traceSet.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No trace information available for " + idString(projectId, alignedFeatureId) );
        else return traceSet.get();
    }

    protected static String idString(String pid, String fid) {
        return "'" + pid + "/" + fid + "'";
    }

    protected static String idString(String pid, String fid, String foId) {
        return "'" + pid + "/" + fid + "/" + foId + "'";
    }
}

