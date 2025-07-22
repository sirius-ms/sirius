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

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.InChISMILESUtils;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.fingerid.AddExternalStructureJJob;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.middleware.configuration.GlobalConfig;
import de.unijena.bioinf.ms.middleware.controller.mixins.TaggableController;
import de.unijena.bioinf.ms.middleware.model.annotations.*;
import de.unijena.bioinf.ms.middleware.model.compute.InstrumentProfile;
import de.unijena.bioinf.ms.middleware.model.events.ServerEvents;
import de.unijena.bioinf.ms.middleware.model.features.*;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import de.unijena.bioinf.ms.middleware.model.spectra.Spectrums;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.service.databases.ChemDbService;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.projectspace.FCandidate;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.spectraldb.SpectrumType;
import de.unijena.bioinf.spectraldb.entities.MergedReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.ReferenceFragmentationTree;
import de.unijena.bioinf.spectraldb.entities.ReferenceSpectrum;
import de.unijena.bioinf.webapi.WebAPI;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.exception.CDKException;
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
import java.util.stream.Stream;

import static de.unijena.bioinf.ChemistryBase.utils.Utils.isNullOrBlank;
import static de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils.removeNone;

@Slf4j
@RestController
@RequestMapping(value = "/api/projects/{projectId}/aligned-features")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Features", description = "This feature based API allows access features (aligned over runs) and there Annotations of " +
        "a specified project-space. This is the entry point to access all raw annotation results an there summaries.")
public class AlignedFeatureController implements TaggableController<AlignedFeature, AlignedFeature.OptField> {

    @Getter
    private final ProjectsProvider<?> projectsProvider;
    private final ChemDbService chemDbService;
    private final GlobalConfig globalConfig;
    private final EventService<?> eventService;
    private final WebAPI<?> webAPI;

    @Autowired
    public AlignedFeatureController(ProjectsProvider<?> projectsProvider, ChemDbService chemDbService, GlobalConfig globalConfig, EventService<?> eventService, WebAPI<?> webAPI) {
        this.projectsProvider = projectsProvider;
        this.chemDbService = chemDbService;
        this.globalConfig = globalConfig;
        this.eventService = eventService;
        this.webAPI = webAPI;
    }

    /**
     * Get all available features (aligned over runs) in the given project-space.
     *
     * @param projectId project-space to read from.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast
     *                            Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch
     *                            peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     *
     * @return AlignedFeatures with additional annotations and MS/MS data (if specified).
     */
    @GetMapping(value = "/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<AlignedFeature> getAlignedFeaturesPaged(
            @PathVariable String projectId, @ParameterObject Pageable pageable,
            @RequestParam(defaultValue = "false", required = false) boolean msDataSearchPrepared,
            @RequestParam(defaultValue = "none") EnumSet<AlignedFeature.OptField> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId).findAlignedFeatures(pageable, msDataSearchPrepared, removeNone(optFields));
    }

    /**
     * Get all available features (aligned over runs) in the given project-space.
     *
     * @param projectId project-space to read from.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast
     *                            Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch
     *                            peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     * @return AlignedFeatures with additional annotations and MS/MS data (if specified).
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AlignedFeature> getAlignedFeatures(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "false", required = false) boolean msDataSearchPrepared,
            @RequestParam(defaultValue = "none") EnumSet<AlignedFeature.OptField> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId).findAlignedFeatures(Pageable.unpaged(), msDataSearchPrepared, removeNone(optFields))
                .getContent();
    }

    /**
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     *
     * @param projectId project-space to delete from.
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
                                                   @RequestParam(defaultValue = "none") EnumSet<AlignedFeature.OptField> optFields
    ) {
        List<AlignedFeature> importedFeatures = projectsProvider.getProjectOrThrow(projectId).addAlignedFeatures(features, profile, removeNone(optFields));

        // Prepare and Send SSE Event
        List<String> fids = importedFeatures.stream().map(AlignedFeature::getAlignedFeatureId).toList();
        List<String> cids = importedFeatures.stream().map(AlignedFeature::getCompoundId).distinct().toList();
        eventService.sendEvent(ServerEvents.newImportEvent(cids, fids, projectId));

        return importedFeatures;
    }


    /**
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast
     *                            Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch
     *                            peak assignments and reference spectra.
     * @return AlignedFeature with additional annotations and MS/MS data (if specified).
     */
    @GetMapping(value = "/{alignedFeatureId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AlignedFeature getAlignedFeature(
            @PathVariable String projectId, @PathVariable String alignedFeatureId,
            @RequestParam(defaultValue = "false", required = false) boolean msDataSearchPrepared,
            @RequestParam(defaultValue = "none") EnumSet<AlignedFeature.OptField> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId).findAlignedFeaturesById(alignedFeatureId, msDataSearchPrepared, removeNone(optFields));
    }

    /**
     * Get list of features that were aligned over runs with the given identifier from the specified project-space.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @return AlignedFeature with additional annotations and MS/MS data (if specified).
     */
    @Hidden
    @GetMapping(value = "/{alignedFeatureId}/features", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Feature> getFeatures(
            @PathVariable String projectId, @PathVariable String alignedFeatureId
    ) {
        return projectsProvider.getProjectOrThrow(projectId).findFeaturesByAlignedFeatureId(alignedFeatureId);
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
            @RequestParam(defaultValue = "none") EnumSet<StructureCandidateScored.OptField> optFields
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
            @RequestParam(defaultValue = "none") EnumSet<StructureCandidateScored.OptField> optFields
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
            @RequestParam(defaultValue = "none") EnumSet<StructureCandidateScored.OptField> optFields
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
            @RequestParam(defaultValue = "none") EnumSet<StructureCandidateScored.OptField> optFields
    ) {
        return getDeNovoStructureCandidatesPaged(projectId, alignedFeatureId, globalConfig.unpaged(), optFields).stream().toList();
    }

    /**
     * [EXPERIMENTAL] Add molecular structures (as SMILES) to the list of de novo structures. This starts a scoring job to incorporate the structures in the de novo results list.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param smiles        smiles
     * @return StructureCandidate of this feature candidate with specified optional fields.
     */
    @Operation(operationId = "addDeNovoStructureCandidate")
    @PutMapping(value = "/{alignedFeatureId}/denovo-structures", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<StructureCandidateFormula> addDeNovoStructureCandidates(
            @PathVariable String projectId, @PathVariable String alignedFeatureId,
            @RequestParam(defaultValue = "none") String smiles
    ) {
        Project<?> project = projectsProvider.getProjectOrThrow(projectId);
        Instance instance = project.getProjectSpaceManager().findInstance(alignedFeatureId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance with ID " + alignedFeatureId + " not found in project + " + projectId + "."));

        try {
            if (existingStructureCandidate(project, alignedFeatureId, smiles)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Structure candidate already exists.");
            }
        } catch (CDKException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid SMILES.");
        }

        List<FCandidate<?>> inputData = instance.getMsNovelistInput().stream()
                 .filter(c -> c.hasAnnotation(FingerprintResult.class))
                .filter(c -> c.hasAnnotation(FTree.class))
                .peek(c -> c.annotate(c.asFingerIdResult()))
                .toList();
        List<FingerIdResult> idResults = inputData.stream().map(d -> d.getAnnotationOrThrow(FingerIdResult.class)).toList();

        Scored<FingerprintCandidate> candidates = SiriusJobs.runInBackground(new AddExternalStructureJJob(smiles, idResults, instance.getCharge(), webAPI).asType(JJob.JobType.TINY_BACKGROUND)).takeResult();
        if (candidates == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fingerprint candidate count not be computed");

        instance.addMsNovelistResult(inputData); //only one FCandidate with a result
        return null; //todo what to return
    }

    /**
     * Summarize matched reference spectra for the given 'alignedFeatureId'.
     * If a 'inchiKey' (2D) is provided, summarizes only contains matches for the database compound with the given InChI key.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param minSharedPeaks   min threshold of shared peaks.
     * @param minSimilarity    min spectral similarity threshold.
     * @param inchiKey         2D inchi key of the compound in the structure database.
     * @return Summary object with best match, number of spectral library matches, matched reference spectra and matched database compounds of this feature (aligned over runs).
     */
    @GetMapping(value = "/{alignedFeatureId}/spectral-library-matches/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public SpectralLibraryMatchSummary getSpectralLibraryMatchesSummary(
            @PathVariable String projectId,
            @PathVariable String alignedFeatureId,
            @RequestParam(defaultValue = "1") int minSharedPeaks,
            @RequestParam(defaultValue = "0.2") double minSimilarity,
            @RequestParam(defaultValue = "") @Nullable String inchiKey
    ) {
        minSharedPeaks = Math.max(minSharedPeaks, 0);
        minSimilarity = Math.min(Math.max(minSimilarity, 0d), 1d);
        if (isNullOrBlank(inchiKey)) {
            return projectsProvider.getProjectOrThrow(projectId).summarizeLibraryMatchesByFeatureId(alignedFeatureId, minSharedPeaks, minSimilarity);
        } else {
            return projectsProvider.getProjectOrThrow(projectId).summarizeLibraryMatchesByFeatureIdAndInchi(alignedFeatureId, inchiKey, minSharedPeaks, minSimilarity);
        }
    }

    /**
     * Page of spectral library matches for the given 'alignedFeatureId'.
     * If a 'inchiKey' (2D) is provided, returns only matches for the database compound with the given InChI key.
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
            @RequestParam(defaultValue = "") @Nullable String inchiKey,
            @RequestParam(defaultValue = "none") EnumSet<SpectralLibraryMatch.OptField> optFields
    ) {
        minSharedPeaks = Math.max(minSharedPeaks, 0);
        minSimilarity = Math.min(Math.max(minSimilarity, 0d), 1d);
        Page<SpectralLibraryMatch> matches;
        if (isNullOrBlank(inchiKey)) {
            matches = projectsProvider.getProjectOrThrow(projectId).findLibraryMatchesByFeatureId(alignedFeatureId, minSharedPeaks, minSimilarity, pageable);
        } else {
            matches = projectsProvider.getProjectOrThrow(projectId).findLibraryMatchesByFeatureIdAndInchi(alignedFeatureId, inchiKey, minSharedPeaks, minSimilarity, pageable);
        }

        if (matches != null && removeNone(optFields).contains(SpectralLibraryMatch.OptField.referenceSpectrum))
            matches.getContent().forEach(this::extractRefSpectraSneaky);
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
            @RequestParam(defaultValue = "") @Nullable String inchiKey,
            @RequestParam(defaultValue = "none") EnumSet<SpectralLibraryMatch.OptField> optFields
    ) {
        return getSpectralLibraryMatchesPaged(projectId, alignedFeatureId, globalConfig.unpaged(), minSharedPeaks, minSimilarity, inchiKey, optFields).stream().toList();
    }

    /**
     * Spectral library match for the given 'alignedFeatureId'.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param matchId          id of the library match to be returned.
     * @return Spectral library match with requested mathcId.
     */
    @GetMapping(value = "/{alignedFeatureId}/spectral-library-matches/{matchId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public SpectralLibraryMatch getSpectralLibraryMatch(
            @PathVariable String projectId, @PathVariable String alignedFeatureId, @PathVariable String matchId,
            @RequestParam(defaultValue = "none") EnumSet<SpectralLibraryMatch.OptField> optFields
    ) {
        SpectralLibraryMatch match = projectsProvider.getProjectOrThrow(projectId)
                .findLibraryMatchesByFeatureIdAndMatchId(alignedFeatureId, matchId);


        if (removeNone(optFields).contains(SpectralLibraryMatch.OptField.referenceSpectrum))
            extractRefSpectraSneaky(match);
        return match;
    }

    /**
     * [EXPERIMENTAL] Spectral library match for the given 'alignedFeatureId' with additional molecular formula and substructure annotations.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param matchId          id of the library match to be returned.
     * @return Spectral library match with requested mathcId.
     */
    @Operation(operationId = "getStructureAnnotatedSpectralLibraryMatchExperimental")
    @GetMapping(value = "/{alignedFeatureId}/spectral-library-matches/{matchId}/annotated", produces = MediaType.APPLICATION_JSON_VALUE)
    public AnnotatedSpectrum getStructureAnnotatedSpectralLibraryMatch(
            @PathVariable String projectId, @PathVariable String alignedFeatureId, @PathVariable String matchId
    ) {
        SpectralLibraryMatch match = projectsProvider.getProjectOrThrow(projectId)
                .findLibraryMatchesByFeatureIdAndMatchId(alignedFeatureId, matchId);

        ReferenceSpectrum spec = extractRefSpectra(match);

        return CustomDataSources.getSourceFromNameOpt(match.getDbName()).map(db -> {
            try {
                ReferenceFragmentationTree refTree;
                if (match.getReferenceSpectrumType() == SpectrumType.MERGED_SPECTRUM) {
                    refTree = chemDbService.db().getReferenceTree(db, match.getUuid());
                } else {
                    // todo this does needs more queries then necessary. We should maybe allow for direct tree retrieval.
                    MergedReferenceSpectrum mergedSpec = chemDbService.db().getMergedReferenceSpectrum(db, spec.getCandidateInChiKey(), spec.getPrecursorIonType(), false);
                    refTree = chemDbService.db().getReferenceTree(db, mergedSpec.getUuid());
                }
                return Spectrums.createReferenceMsMsWithAnnotations(spec, refTree.asFTree());
            } catch (ChemicalDatabaseException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Could not find ftree for spectrum: '%s' in database '%s'.", match.getUuid(), match.getDbName()));
            }
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Could not load ftree for Spectrum: '%s'. Database '%s' does not exist!", match.getUuid(), match.getDbName())));
    }


    /**
     * Mass Spec data (input data) for the given 'alignedFeatureId' .
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the Mass Spec data belongs to.
     * @param msDataSearchPrepared    Returns all fragment spectra in a preprocessed form as used for fast
     *                         Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch
     *                         peak assignments and reference spectra.
     * @return Mass Spec data of this feature (aligned over runs).
     */
    @GetMapping(value = "/{alignedFeatureId}/ms-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public MsData getMsData(@PathVariable String projectId, @PathVariable String alignedFeatureId,
                            @RequestParam(defaultValue = "false", required = false) boolean msDataSearchPrepared
    ) {
        MsData msData = projectsProvider.getProjectOrThrow(projectId)
                .findAlignedFeaturesById(alignedFeatureId, msDataSearchPrepared, AlignedFeature.OptField.msData).getMsData();
        if (msData == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MsData for '" + idString(projectId, alignedFeatureId) + "' not found!");
        return msData;
    }

    /**
     * Page of FormulaResultContainers available for this feature with minimal information.
     * Can be enriched with an optional results overview.
     *
     * @param projectId           project-space to read from.
     * @param alignedFeatureId    feature (aligned over runs) the formula result belongs to.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast
     *                            Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch
     *                            peak assignments and reference spectra.
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @return All FormulaCandidate of this feature with.
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<FormulaCandidate> getFormulaCandidatesPaged(
            @PathVariable String projectId, @PathVariable String alignedFeatureId, @ParameterObject Pageable pageable,
            @RequestParam(defaultValue = "false", required = false) boolean msDataSearchPrepared,
            @RequestParam(defaultValue = "none") EnumSet<FormulaCandidate.OptField> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId)
                .findFormulaCandidatesByFeatureId(alignedFeatureId, pageable, msDataSearchPrepared, removeNone(optFields));
    }

    /**
     * List of FormulaResultContainers available for this feature with minimal information.
     * Can be enriched with an optional results overview.
     *
     * @param projectId           project-space to read from.
     * @param alignedFeatureId    feature (aligned over runs) the formula result belongs to.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast
     *                            Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch
     *                            peak assignments and reference spectra.
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @return All FormulaCandidate of this feature with.
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FormulaCandidate> getFormulaCandidates(
            @PathVariable String projectId, @PathVariable String alignedFeatureId,
            @RequestParam(defaultValue = "false", required = false) boolean msDataSearchPrepared,
            @RequestParam(defaultValue = "none") EnumSet<FormulaCandidate.OptField> optFields
    ) {
        return getFormulaCandidatesPaged(projectId, alignedFeatureId, globalConfig.unpaged(), msDataSearchPrepared, optFields).stream().toList();
    }

    /**
     * FormulaResultContainers for the given 'formulaId' with minimal information.
     * Can be enriched with an optional results overview and formula candidate information.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast
     *                            Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch
     *                            peak assignments and reference spectra.
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @return FormulaCandidate of this feature (aligned over runs) with.
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public FormulaCandidate getFormulaCandidate(
            @PathVariable String projectId, @PathVariable String alignedFeatureId, @PathVariable String formulaId,
            @RequestParam(defaultValue = "false", required = false) boolean msDataSearchPrepared,
            @RequestParam(defaultValue = "none") EnumSet<FormulaCandidate.OptField> optFields

    ) {
        return projectsProvider.getProjectOrThrow(projectId)
                .findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId, msDataSearchPrepared, removeNone(optFields));
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
            @RequestParam(defaultValue = "none") EnumSet<StructureCandidateScored.OptField> optFields
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
            @RequestParam(defaultValue = "none") EnumSet<StructureCandidateScored.OptField> optFields
    ) {
        return getStructureCandidatesByFormulaPaged(projectId, alignedFeatureId, formulaId, globalConfig.unpaged(), optFields)
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
            @RequestParam(defaultValue = "none") EnumSet<StructureCandidateScored.OptField> optFields
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
            @RequestParam(defaultValue = "none") EnumSet<StructureCandidateScored.OptField> optFields
    ) {
        return getDeNovoStructureCandidatesByFormulaPaged(projectId, alignedFeatureId, formulaId, globalConfig.unpaged(), optFields)
                .stream().toList();
    }

    /**
     * [EXPERIMENTAL] Returns a fragmentation spectrum annotated with fragments and losses for the given formulaId and inChIKey
     * <p>
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the selected formula result
     * These annotations are only available if a fragmentation tree is available.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @param inchiKey         2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @param spectrumIndex    index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex < 0 (default)
     * @return Fragmentation spectrum annotated with fragments and sub-structures.
     */
    @Operation(operationId = "getStructureAnnotatedSpectrumExperimental")
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/structures/{inchiKey}/annotated-spectrum", produces = MediaType.APPLICATION_JSON_VALUE)
    public AnnotatedSpectrum getStructureAnnotatedSpectrum(@PathVariable String projectId,
                                                           @PathVariable String alignedFeatureId,
                                                           @PathVariable String formulaId,
                                                           @PathVariable String inchiKey,
                                                           @RequestParam(defaultValue = "-1") int spectrumIndex,
                                                           @RequestParam(defaultValue = "false") boolean searchPrepared
    ) {
        AnnotatedSpectrum res = projectsProvider.getProjectOrThrow(projectId)
                .findAnnotatedSpectrumByStructureId(spectrumIndex, inchiKey, formulaId, alignedFeatureId, searchPrepared);
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Annotated MS/MS Spectrum for '"
                    + idString(projectId, alignedFeatureId, formulaId)
                    + "' not available! Maybe because FragmentationTree is missing?");

        return res;
    }

    /**
     * [EXPERIMENTAL] Returns MS/MS Data annotated with fragments and losses for given formulaId and inChIKey.
     * <p>
     * Returns MS/MS Data (Merged MS/MS and list of measured MS/MS ) which are annotated with fragments and losses
     * for the given formula result identifier and structure candidate inChIKey.
     * These annotations are only available if a fragmentation tree and the structure candidate are available.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @param inchiKey         2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @param msDataSearchPrepared    Returns all fragment spectra in a preprocessed form as used for fast
     *                         Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch
     *                         peak assignments and reference spectra.
     * @return Fragmentation spectrum annotated with fragments and sub-structures.
     */
    @Operation(operationId = "getStructureAnnotatedMsDataExperimental")
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/structures/{inchiKey}/annotated-msmsdata", produces = MediaType.APPLICATION_JSON_VALUE)
    public AnnotatedMsMsData getStructureAnnotatedMsData(
            @PathVariable String projectId,
            @PathVariable String alignedFeatureId,
            @PathVariable String formulaId,
            @PathVariable String inchiKey,
            @RequestParam(defaultValue = "false", required = false) boolean msDataSearchPrepared
    ) {
        AnnotatedMsMsData res = projectsProvider.getProjectOrThrow(projectId)
                .findAnnotatedMsMsDataByStructureId(inchiKey, formulaId, alignedFeatureId, msDataSearchPrepared);
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Annotated MS/MS Spectrum for '"
                    + idString(projectId, alignedFeatureId, formulaId)
                    + "' not available! Maybe because FragmentationTree is missing?");

        return res;
    }


    /**
     * [INTERNAL] Returns fragmentation tree (SIRIUS) for the given formula result identifier in SIRIUS' internal format.
     * <p>
     * [INTERNAL]: This is an internal api endpoint and not part of the official public API. It might be changed or removed at any time.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @return Fragmentation Tree in internal format.
     */
    @Operation(operationId = "getSiriusFragTreeInternal")
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/sirius-fragtree", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getSiriusFragTree(@PathVariable String projectId, @PathVariable String alignedFeatureId, @PathVariable String formulaId) {
        String json = projectsProvider.getProjectOrThrow(projectId).findSiriusFtreeJsonById(formulaId, alignedFeatureId);
        if (json == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FragmentationTree for '" + idString(projectId, alignedFeatureId, formulaId) + "' not found!");
        return json;
    }

    /**
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier
     * <p>
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
                .findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId, false, FormulaCandidate.OptField.fragmentationTree)
                .getFragmentationTree();
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FragmentationTree for '" + idString(projectId, alignedFeatureId, formulaId) + "' not found!");
        return res;
    }

    /**
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier
     * <p>
     * These annotations are only available if a fragmentation tree is available.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @param spectrumIndex    index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex < 0 (default)
     * @param searchPrepared    Returns all fragment spectra in a preprocessed form as used for fast
     *                         Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch
     *                         peak assignments and reference spectra.
     * @return Fragmentation spectrum annotated with fragment formulas and losses.
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/annotated-spectrum", produces = MediaType.APPLICATION_JSON_VALUE)
    public AnnotatedSpectrum getFormulaAnnotatedSpectrum(
            @PathVariable String projectId,
            @PathVariable String alignedFeatureId,
            @PathVariable String formulaId,
            @RequestParam(defaultValue = "-1") int spectrumIndex,
            @RequestParam(defaultValue = "false", required = false) boolean searchPrepared
    ) {
        AnnotatedSpectrum res = projectsProvider.getProjectOrThrow(projectId)
                .findAnnotatedSpectrumByFormulaId(spectrumIndex, formulaId, alignedFeatureId, searchPrepared);
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Annotated MS/MS Spectrum for '"
                    + idString(projectId, alignedFeatureId, formulaId)
                    + "' not available! Maybe because FragmentationTree is missing?");

        return res;
    }

    /**
     * Returns MS/MS Spectrum annotated with fragments and losses for provided formulaId.
     * <p>
     * Returns MS/MS Spectrum (Merged MS/MS and measured MS/MS) which is annotated with fragments and losses
     * for the given formula result identifier
     * These annotations are only available if a fragmentation tree and the structure candidate are available.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @param msDataSearchPrepared    Returns all fragment spectra in a preprocessed form as used for fast
     *                         Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch
     *                         peak assignments and reference spectra.
     * @return Fragmentation spectra annotated with fragment formulas and losses.
     */
    @GetMapping(value = "/{alignedFeatureId}/formulas/{formulaId}/annotated-msmsdata", produces = MediaType.APPLICATION_JSON_VALUE)
    public AnnotatedMsMsData getFormulaAnnotatedMsMsData(
            @PathVariable String projectId,
            @PathVariable String alignedFeatureId,
            @PathVariable String formulaId,
            @RequestParam(defaultValue = "false", required = false) boolean msDataSearchPrepared

    ) {
        AnnotatedMsMsData res = projectsProvider.getProjectOrThrow(projectId)
                .findAnnotatedMsMsDataByFormulaId(formulaId, alignedFeatureId, msDataSearchPrepared);
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Annotated MS/MS Data for '"
                    + idString(projectId, alignedFeatureId, formulaId)
                    + "' not available! Maybe because FragmentationTree is missing?");

        return res;
    }

    /**
     * Returns Isotope pattern information for given formulaId
     * <p>
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
                .findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId, false, FormulaCandidate.OptField.isotopePattern)
                .getIsotopePatternAnnotation();
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Isotope Pattern for '" + idString(projectId, alignedFeatureId, formulaId) + "' not found!");
        return res;
    }

    /**
     * Returns Lipid annotation (ElGordo) for the given formulaId.
     * <p>
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
                .findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId,false, FormulaCandidate.OptField.lipidAnnotation)
                .getLipidAnnotation();
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lipid annotation for '" + idString(projectId, alignedFeatureId, formulaId) + "' not found!");
        return res;
    }

    /**
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier (formulaId)
     * <p>
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
                .findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId, false, FormulaCandidate.OptField.predictedFingerprint)
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
                .findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId, false, FormulaCandidate.OptField.canopusPredictions)
                .getCanopusPrediction();
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Compound Classes for '" + idString(projectId, alignedFeatureId, formulaId) + "' not found!");
        return res;
    }

    /**
     * Return Best matching compound classes for given formulaId.
     * <p>
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
                .findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId, false, FormulaCandidate.OptField.compoundClasses)
                .getCompoundClasses();
        if (res == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Compound Classes for '" + idString(projectId, alignedFeatureId, formulaId) + "' not found!");
        return res;
    }

    /**
     * [EXPERIMENTAL] Returns data quality information for given feature (alignedFeatureId)
     * <p>
     * Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @return AlignedFeatureQuality quality information of the respective feature.
     */
    @Operation(operationId = "getAlignedFeatureQualityExperimental")
    @GetMapping(value = "/{alignedFeatureId}/quality-report", produces = MediaType.APPLICATION_JSON_VALUE)
    public AlignedFeatureQuality getAlignedFeaturesQuality(
            @PathVariable String projectId, @PathVariable String alignedFeatureId
    ) {
        return projectsProvider.getProjectOrThrow(projectId).findAlignedFeaturesQualityById(alignedFeatureId);
    }

    /*
        LCMS Stuff
     */

    /**
     * [EXPERIMENTAL] Returns a single quantification table row for the given feature (alignedFeatureId).
     * <p>
     * The quantification table contains a quantity of the feature within all samples it is contained in.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature which quantity should be read out
     * @param type             quantification type. Currently, only APEX_HEIGHT is supported, which is the intensity of the feature at its apex.
     * @return Quant table row for this feature
     */
    @Operation(operationId = "getQuantTableRowExperimental")
    @GetMapping(value = "/{alignedFeatureId}/quant-table-row", produces = MediaType.APPLICATION_JSON_VALUE)
    public QuantTable getQuantification(@PathVariable String projectId, @PathVariable String alignedFeatureId, @RequestParam(defaultValue = "APEX_HEIGHT") QuantMeasure type) {
        Optional<QuantTable> quantificationForAlignedFeature = projectsProvider.getProjectOrThrow(projectId).getQuantificationForAlignedFeatureOrCompound(alignedFeatureId, type, QuantRowType.FEATURES);
        if (quantificationForAlignedFeature.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No quantification information available for " + idString(projectId, alignedFeatureId) + " and quantification type " + type);
        else return quantificationForAlignedFeature.get();
    }

    /**
     * [EXPERIMENTAL]  Returns the full quantification table for the given feature (alignedFeatureId).
     * <p>
     * Returns the full quantification table. The quantification table contains a quantities of the features within all
     * runs they are contained in.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId project-space to read from.
     * @param type      quantification type.
     * @return Quant table if akk feature in this project
     */
    @GetMapping(value = "/quant-table", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "getFeatureQuantTableExperimental")
    public QuantTable getQuantification(@PathVariable String projectId, @RequestParam(defaultValue = "APEX_HEIGHT") QuantMeasure type) {
        Optional<QuantTable> quantificationForAlignedFeature = projectsProvider.getProjectOrThrow(projectId).getQuantification(type, QuantRowType.FEATURES);
        if (quantificationForAlignedFeature.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No quantification information available for " + projectId + " and quantification type " + type);
        else return quantificationForAlignedFeature.get();
    }


    /**
     * [EXPERIMENTAL] Returns the traces of the given feature (alignedFeatureId).
     * <p>
     * Returns the traces of the given feature. A trace consists of m/z and intensity values over the retention
     * time axis. All the returned traces are 'projected', which means they refer not to the original retention time axis,
     * but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.
     * However, this also means that all traces can be directly compared against each other, as they all lie in the same
     * retention time axis.
     * By default, this method only returns traces of samples the aligned feature appears in. When includeAll is set,
     * it also includes samples in which the same trace appears in.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId feature which intensities should be read out
     * @param includeAll       when true, return all samples that belong to the same merged trace. when false, only return samples which contain the aligned feature.
     * @return Traces of the given feature.
     */
    @Operation(operationId = "getTracesExperimental")
    @GetMapping(value = "/{alignedFeatureId}/traces", produces = MediaType.APPLICATION_JSON_VALUE)
    public TraceSet getTraces(@PathVariable String projectId, @PathVariable String alignedFeatureId, @RequestParam(defaultValue = "false") boolean includeAll) {
        Optional<TraceSet> traceSet = projectsProvider.getProjectOrThrow(projectId).getTraceSetForAlignedFeature(alignedFeatureId, includeAll);
        if (traceSet.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No trace information available for " + idString(projectId, alignedFeatureId));
        else return traceSet.get();
    }

    /**
     * [EXPERIMENTAL] Returns the adduct network for a given alignedFeatureId together with all merged traces contained in the network.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId one feature that is considered the main feature of the adduct network
     */
    @Operation(operationId = "getAdductNetworkWithMergedTracesExperimental")
    @GetMapping(value = "/{alignedFeatureId}/adducts", produces = MediaType.APPLICATION_JSON_VALUE)
    public TraceSet getAdductNetworkWithMergedTraces(@PathVariable String projectId, @PathVariable String alignedFeatureId) {
        Optional<TraceSet> traceSet = projectsProvider.getProjectOrThrow(projectId).getTraceSetsForFeatureWithCorrelatedIons(alignedFeatureId);
        if (traceSet.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No trace information available for " + idString(projectId, alignedFeatureId));
        else return traceSet.get();
    }

    //region tags and groups

    /**
     * [EXPERIMENTAL] Get features (aligned over runs) by tag.
     *
     * <h2>Supported filter syntax</h2>
     *
     * <p>The filter string must contain one or more clauses. A clause is prefíxed
     * by a field name.
     * </p>
     * <p>
     * Currently the only searchable fields are names of tags ({@code tagName}) followed by a clause that is valued for the value type of the tag (See TagDefinition).
     * Tag name based field need to be prefixed with the namespace {@code tags.}.
     * Possible value types of tags are <strong>bool</strong>, <strong>integer</strong>, <strong>real</strong>, <strong>text</strong>, <strong>date</strong>, or <strong>time</strong> - tag value
     *
     * <p>The format of the <strong>date</strong> type is {@code yyyy-MM-dd} and of the <strong>time</strong> type is {@code HH\:mm\:ss}.</p>
     *
     * <p>A clause may be:</p>
     * <ul>
     *     <li>a <strong>term</strong>: field name followed by a colon and the search term, e.g. {@code tags.MyTagA:sample}</li>
     *     <li>a <strong>phrase</strong>: field name followed by a colon and the search phrase in doublequotes, e.g. {@code tags.MyTagA:"Some Text"}</li>
     *     <li>a <strong>regular expression</strong>: field name followed by a colon and the regex in slashes, e.g. {@code tags.MyTagA:/[mb]oat/}</li>
     *     <li>a <strong>comparison</strong>: field name followed by a comparison operator and a value, e.g. {@code tags.MyTagB<3}</li>
     *     <li>a <strong>range</strong>: field name followed by a colon and an open (indiced by {@code [ } and {@code ] }) or (semi-)closed range (indiced by <code>{</code> and <code>}</code>), e.g. {@code tags.MyTagB:[* TO 3] }</li>
     * </ul>
     *
     * <p>Clauses may be <strong>grouped</strong> with brackets {@code ( } and {@code ) } and / or <strong>joined</strong> with {@code AND} or {@code OR } (or {@code && } and {@code || })</p>
     *
     * <h3>Example</h3>
     *
     * <p>The syntax allows to build complex filter queries such as:</p>
     *
     * <p>{@code tags.city:"new york" AND tags.ATextTag:/[mb]oat/ AND tags.count:[1 TO *] OR tags.realNumberTag<=3.2 OR tags.MyDateTag:2024-01-01 OR tags.MyDateTag:[2023-10-01 TO 2023-12-24] OR tags.MyDateTag<2022-01-01 OR tags.time:12\:00\:00 OR tags.time:[12\:00\:00 TO 14\:00\:00] OR tags.time<10\:00\:00 }</p>
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId project space to get features (aligned over runs) from.
     * @param filter    tag filter.
     * @param pageable  pageable.
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     * @return tagged features (aligned over runs)
     */
    @Operation(operationId = "getAlignedFeaturesByTagExperimental")
    @Override
    public Page<AlignedFeature> getObjectsByTag(String projectId, String filter, Pageable pageable, EnumSet<AlignedFeature.OptField> optFields) {
        return TaggableController.super.getObjectsByTag(projectId, filter, pageable, optFields);
    }


    /**
     * [EXPERIMENTAL] Get all tags associated with this Object
     *
     * @param projectId project-space to get from.
     * @param objectId  object to get tags for.
     * @return the tags of the requested object
     */
    @Operation(operationId = "getTagsForAlignedFeaturesExperimental")
    @Override
    public List<Tag> getTags(String projectId, String objectId) {
        return TaggableController.super.getTags(projectId, objectId);
    }

    /**
     * [EXPERIMENTAL] Add tags to a feature (aligned over runs) in the project. Tags with the same name will be overwritten.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId        project-space to add to.
     * @param alignedFeatureId run to add tags to.
     * @param tags             tags to add.
     * @return the tags that have been added
     */
    @Operation(operationId = "addTagsToAlignedFeatureExperimental")
    @PutMapping(value = "/tags/{alignedFeatureId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public List<Tag> addTags(String projectId, String alignedFeatureId, List<? extends de.unijena.bioinf.ms.middleware.model.tags.Tag> tags) {
        return TaggableController.super.addTags(projectId, alignedFeatureId, tags);
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the feature (aligned over runs) with the specified ID in the specified project-space.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId        project-space to delete from.
     * @param alignedFeatureId feature (aligned over runs) to delete tag from.
     * @param tagName          name of the tag to delete.
     */
    @Operation(operationId = "removeTagFromAlignedFeatureExperimental")
    @DeleteMapping(value = "/tags/{alignedFeatureId}/{tagName}")
    @Override
    public void removeTags(String projectId, String alignedFeatureId, String tagName) {
        TaggableController.super.removeTags(projectId, alignedFeatureId, tagName);
    }

    /**
     * [EXPERIMENTAL] Get features (aligned over runs) by tag group.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId project-space to delete from.
     * @param groupName tag group name.
     * @param pageable  pageable.
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     * @return tagged features (aligned over runs)
     */

    @Operation(operationId = "getAlignedFeaturesByGroupExperimental")
    @Override
    public Page<AlignedFeature> getObjectsByGroup(String projectId, String groupName, Pageable pageable, EnumSet<AlignedFeature.OptField> optFields) {
        return TaggableController.super.getObjectsByGroup(projectId, groupName, pageable, optFields);
    }

    @Override
    public Class<AlignedFeature> getTagTarget() {
        return AlignedFeature.class;
    }
    //endregion

    //region helpers
    protected static String idString(String pid, String fid) {
        return "'" + pid + "/" + fid + "'";
    }

    protected static String idString(String pid, String fid, String foId) {
        return "'" + pid + "/" + fid + "/" + foId + "'";
    }


    @NotNull
    private ReferenceSpectrum extractRefSpectra(SpectralLibraryMatch match) throws ResponseStatusException {
        return CustomDataSources.getSourceFromNameOpt(match.getDbName()).map(db -> {
            try {
                ReferenceSpectrum spec = chemDbService.db().getReferenceSpectrum(db, match.getUuid(), match.getReferenceSpectrumType());
                if (spec.getSearchPreparedSpectrum() != null)
                    match.setReferenceSpectrum(Spectrums.createReferenceMsMs(spec));
                return spec;
            } catch (ChemicalDatabaseException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Could not find Spectrum: '%s' in database '%s'. %s", match.getUuid(), match.getDbName(), e.getMessage()));
            }
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Could not load Spectrum: '%s'. Database '%s' does not exist!", match.getUuid(), match.getDbName())));
    }

    @Nullable
    private ReferenceSpectrum extractRefSpectraSneaky(SpectralLibraryMatch match) {
        try {
            return extractRefSpectra(match);
        } catch (ResponseStatusException e) {
            log.error(e.getReason(), e);
            return null;
        }
    }

    /**
     * Checks if there is a structure candidate for the given feature corresponding to the given smiles (by comparing inchi key)
     */
    private boolean existingStructureCandidate(Project<?> project, String alignedFeatureId, String smiles) throws CDKException {
        String inchiKey = InChISMILESUtils.getInchiFromSmiles(smiles, false).key.substring(0, 14);

        return Stream.concat(project.findStructureCandidatesByFeatureId(alignedFeatureId, Pageable.unpaged()).stream(),
                             project.findDeNovoStructureCandidatesByFeatureId(alignedFeatureId, Pageable.unpaged()).stream())
                .map(StructureCandidate::getInchiKey)
                .anyMatch(inchiKey::equals);
    }

    // endregion

}

