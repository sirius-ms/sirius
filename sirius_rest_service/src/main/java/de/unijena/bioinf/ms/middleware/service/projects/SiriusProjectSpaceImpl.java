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

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.FeatureGroup;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.ms.CompoundQuality;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.ConfidenceScoreApproximate;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.StructureSearchResult;
import de.unijena.bioinf.fingerid.blast.*;
import de.unijena.bioinf.lcms.LCMSCompoundSummary;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.middleware.controller.AlignedFeatureController;
import de.unijena.bioinf.ms.middleware.model.annotations.*;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.compute.InstrumentProfile;
import de.unijena.bioinf.ms.middleware.model.features.*;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import de.unijena.bioinf.ms.middleware.model.spectra.Spectrums;
import de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.projectspace.fingerid.FBCandidateNumber;
import de.unijena.bioinf.projectspace.fingerid.FBCandidatesTopK;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import io.hypersistence.tsid.TSID;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class SiriusProjectSpaceImpl implements Project<SiriusProjectSpaceManager> {

    @NotNull
    private final SiriusProjectSpaceManager projectSpaceManager;
    @NotNull
    private final String projectId;

    private final @NotNull BiFunction<Project<?>, String, Boolean> computeStateProvider;

    public SiriusProjectSpaceImpl(@NotNull String projectId, @NotNull SiriusProjectSpaceManager projectSpaceManager, @NotNull BiFunction<Project<?>, String, Boolean> computeStateProvider) {
        this.projectSpaceManager = projectSpaceManager;
        this.projectId = projectId;
        this.computeStateProvider = computeStateProvider;
    }

    @Override
    public SpectralLibraryMatch findLibraryMatchesByFeatureIdAndMatchId(String alignedFeatureId, String matchId) {
        throw new UnsupportedOperationException("Finde by matchId not supported by the project");
    }

    @NotNull
    @Override
    public String getProjectId() {
        return projectId;
    }

    @Override
    public @NotNull SiriusProjectSpaceManager getProjectSpaceManager() {
        return projectSpaceManager;
    }

    @Override
    public Optional<QuantificationTable> getQuantificationForAlignedFeature(String alignedFeatureId, QuantificationTable.QuantificationType type) {
        throw new UnsupportedOperationException("getQuantificationForAlignedFeature not supported by the project");
    }

    @Override
    public Optional<TraceSet> getTraceSetForAlignedFeature(String alignedFeatureId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TraceSet> getTraceSetForCompound(String compoundId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<Compound> findCompounds(Pageable pageable, @NotNull EnumSet<Compound.OptField> optFields,
                                        @NotNull EnumSet<AlignedFeature.OptField> featureOptFields) {
        Map<String, List<CompoundContainerId>> featureGroups = projectSpaceManager.getProjectSpaceImpl()
                .stream().filter(c -> c.getGroupId().isPresent())
                .collect(Collectors.groupingBy(c -> c.getGroupId().get()));

        List<Compound> compounds = featureGroups.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .skip(pageable.getOffset()).limit(pageable.getPageSize())
                .map(e -> asCompound(e.getValue(), optFields, featureOptFields))
                .toList();

        return new PageImpl<>(compounds, pageable, featureGroups.size());
    }

    @Override
    public List<Compound> addCompounds(@NotNull List<CompoundImport> compounds, InstrumentProfile profile, @NotNull EnumSet<Compound.OptField> optFields, @NotNull EnumSet<AlignedFeature.OptField> optFieldsFeatures) {
        return compounds.stream().map(c -> {
            final FeatureGroup fg = FeatureGroup.builder().groupName(c.getName()).groupId(TSID.fast().toLong()).build();
            return FeatureImports.toExperimentsStr(c.getFeatures())
                    .peek(exp -> exp.annotate(fg))
                    .map(projectSpaceManager::importInstanceWithUniqueId)
                    .map(SiriusProjectSpaceInstance::getCompoundContainerId).toList();
        }).map(cids -> asCompound(cids, optFields, optFieldsFeatures)).toList();
    }

    @Override
    public Compound findCompoundById(String compoundId, @NotNull EnumSet<Compound.OptField> optFields,
                                     @NotNull EnumSet<AlignedFeature.OptField> featureOptFields) {
        List<CompoundContainerId> groupFeatures = projectSpaceManager.getProjectSpaceImpl()
                .stream().filter(c -> c.getGroupId().map(compoundId::equals).orElse(false))
                .toList();
        if (groupFeatures.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No Features found that belong to Compound with id '" + compoundId + "'. Compound does not exist.");

        return asCompound(groupFeatures, optFields, featureOptFields);
    }

    @Override
    public void deleteCompoundById(String compoundId) {
        Compound compound;
        try {
            compound = findCompoundById(compoundId, EnumSet.noneOf(Compound.OptField.class), EnumSet.noneOf(AlignedFeature.OptField.class));
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND))
                throw new ResponseStatusException(HttpStatus.NO_CONTENT, "AlignedFeature with id '" + compoundId + "' does not exist. Already removed?");
            throw e;
        }
        compound.getFeatures().forEach(f -> deleteAlignedFeaturesById(f.getAlignedFeatureId()));
    }

    @Override
    public Page<AlignedFeatureQuality> findAlignedFeaturesQuality(Pageable pageable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AlignedFeatureQuality findAlignedFeaturesQualityById(String alignedFeatureId) {
       throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Page<AlignedFeature> findAlignedFeatures(Pageable pageable, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        LoggerFactory.getLogger(AlignedFeatureController.class).info("Started collecting aligned features...");
        final List<AlignedFeature> alignedFeatures = projectSpaceManager.getProjectSpaceImpl().stream()
                .skip(pageable.getOffset()).limit(pageable.getPageSize())
                .map(ccid -> asAlignedFeature(ccid, optFields))
                .toList();
        LoggerFactory.getLogger(AlignedFeatureController.class).info("Finished parsing aligned features...");

        return new PageImpl<>(alignedFeatures, pageable, projectSpaceManager.size());
    }

    @Override
    public List<AlignedFeature> addAlignedFeatures(@NotNull List<FeatureImport> features, @Nullable InstrumentProfile profile, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        return FeatureImports.toExperimentsStr(features)
                .map(projectSpaceManager::importInstanceWithUniqueId)
                .map(SiriusProjectSpaceInstance::getCompoundContainerId).map(cid -> asAlignedFeature(cid, optFields))
                .toList();
    }


    @Override
    public AlignedFeature findAlignedFeaturesById(String alignedFeatureId, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        final CompoundContainerId ccid = parseCID(alignedFeatureId);
        return asAlignedFeature(ccid, optFields);
    }

    @Override
    public void deleteAlignedFeaturesById(String alignedFeatureId) {
        CompoundContainerId compound = projectSpaceManager.getProjectSpaceImpl().findCompound(alignedFeatureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "AlignedFeature with id '" + alignedFeatureId + "' does not exist. Already removed?"));
        try {
            projectSpaceManager.getProjectSpaceImpl().deleteCompound(compound);
        } catch (IOException e) {
            log.error("Error when deleting feature with Id " + alignedFeatureId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error when deleting feature with Id " + alignedFeatureId);
        }
    }

    @Override
    public void deleteAlignedFeaturesByIds(List<String> alignedFeatureId) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED");
    }

    @Override
    public Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<FormulaCandidate.OptField> optFields) {
        LoggerFactory.getLogger(getClass()).info("Started collecting formulas...");
        Class<? extends DataAnnotation>[] annotations = resolveFormulaCandidateAnnotations(optFields);
        SiriusProjectSpaceInstance instance = loadInstance(alignedFeatureId);
        Stream<FormulaResult> paged;
        int size;
        {
            List<? extends SScored<FormulaResult, ? extends FormulaScore>> tmpSource = instance.loadFormulaResults();
            paged = tmpSource.stream()
                    .skip(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .map(SScored::getCandidate);
            size = tmpSource.size();
        }

        return new PageImpl<>(
                paged.peek(fr -> instance.loadFormulaResult(fr.getId(), annotations).ifPresent(fr::setAnnotationsFrom))
                        .map(res -> makeFormulaCandidate(instance, res, optFields))
                        .map(FormulaCandidate.FormulaCandidateBuilder::build)
                        .toList(), pageable, size);
    }

    @Override
    public SpectralLibraryMatchSummary summarizeLibraryMatchesByFeatureId(String alignedFeatureId, int minSharedPeaks, double minSimilarity) {
        List<SpectralSearchResult.SearchResult> searchResults = loadSpectalLibraryMachtes(loadInstance(alignedFeatureId)).map(SpectralSearchResult::getResults)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Could not find spectral library search results for '" + alignedFeatureId + "'! Maybe library search has not been executed"));
        LongSet refSpecSet = new LongOpenHashSet();
        Set<String> compoundSet = new HashSet<>();
        SpectralLibraryMatch bestMatch = searchResults.stream().filter(
                searchResult -> searchResult.getSimilarity().similarity >= minSimilarity && searchResult.getSimilarity().sharedPeaks >= minSharedPeaks
        ).peek(searchResult -> {
            refSpecSet.add(searchResult.getUuid());
            compoundSet.add(searchResult.getCandidateInChiKey());
        }).findFirst().map(searchResult -> SpectralLibraryMatch.of(searchResult, null)).orElse(null);
        return SpectralLibraryMatchSummary.builder()
                .bestMatch(bestMatch)
                .spectralMatchCount((long) searchResults.size())
                .referenceSpectraCount(refSpecSet.size())
                .databaseCompoundCount(compoundSet.size()).build();
    }

    @Override
    public SpectralLibraryMatchSummary summarizeLibraryMatchesByFeatureIdAndInchi(String alignedFeatureId, String candidateInchi, int minSharedPeaks, double minSimilarity) {
        List<SpectralSearchResult.SearchResult> searchResults = loadSpectalLibraryMachtes(loadInstance(alignedFeatureId)).map(SpectralSearchResult::getResults)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Could not find spectral library search results for '" + alignedFeatureId + "'! Maybe library search has not been executed"));
        LongSet refSpecSet = new LongOpenHashSet();
        SpectralLibraryMatch bestMatch = searchResults.stream().filter(
                searchResult -> searchResult.getCandidateInChiKey().equals(candidateInchi) && searchResult.getSimilarity().similarity >= minSimilarity && searchResult.getSimilarity().sharedPeaks >= minSharedPeaks
        ).peek(searchResult -> {
            refSpecSet.add(searchResult.getUuid());
        }).findFirst().map(searchResult -> SpectralLibraryMatch.of(searchResult, null)).orElse(null);
        return SpectralLibraryMatchSummary.builder()
                .bestMatch(bestMatch)
                .spectralMatchCount((long) searchResults.size())
                .referenceSpectraCount(refSpecSet.size())
                .databaseCompoundCount(!refSpecSet.isEmpty() ? 1 : 0).build();
    }

    @Override
    public Page<SpectralLibraryMatch> findLibraryMatchesByFeatureIdAndInchi(String alignedFeatureId, String candidateInchi, int minSharedPeaks, double minSimilarity, Pageable pageable) {
        SpectralSearchResult searchresult = loadSpectalLibraryMachtes(loadInstance(alignedFeatureId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Could not find spectral library search results for '" + alignedFeatureId + "'! Maybe library search has not been executed"));

        return new PageImpl<>(
                searchresult.getResults().stream()
                        .filter(searchResult -> searchResult.getCandidateInChiKey().equals(candidateInchi) && searchResult.getSimilarity().similarity >= minSimilarity && searchResult.getSimilarity().sharedPeaks >= minSharedPeaks)
                        .skip(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .map(it  -> SpectralLibraryMatch.of(it, null))
                        .toList(),
                pageable,
                searchresult.getResults().size()
        );
    }

    @Override
    public Page<SpectralLibraryMatch> findLibraryMatchesByFeatureId(String alignedFeatureId, int minSharedPeaks, double minSimilarity, Pageable pageable) {
        SpectralSearchResult searchresult = loadSpectalLibraryMachtes(loadInstance(alignedFeatureId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Could not find spectral library search results for '" + alignedFeatureId + "'! Maybe library search has not been executed"));

        return new PageImpl<>(
                searchresult.getResults().stream().filter(searchResult -> searchResult.getSimilarity().similarity >= minSimilarity && searchResult.getSimilarity().sharedPeaks >= minSharedPeaks)
                        .skip(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .map(it  -> SpectralLibraryMatch.of(it, null))
                        .toList(),
                pageable,
                searchresult.getResults().size()
        );
    }

    @Override
    public FormulaCandidate findFormulaCandidateByFeatureIdAndId(String formulaId, String alignedFeatureId, @NotNull EnumSet<FormulaCandidate.OptField> optFields) {
        Class<? extends DataAnnotation>[] annotations = resolveFormulaCandidateAnnotations(optFields);
        SiriusProjectSpaceInstance instance = loadInstance(alignedFeatureId);
        return instance.loadFormulaResult(parseFID(instance, formulaId), annotations)
                .map(res -> makeFormulaCandidate(instance, res, optFields))
                .map(FormulaCandidate.FormulaCandidateBuilder::build)
                .orElse(null);
    }

    @Override
    public Page<StructureCandidateScored> findStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        List<Class<? extends DataAnnotation>> para = (optFields.contains(StructureCandidateScored.OptField.fingerprint)
                ? List.of(FormulaScoring.class, FBCandidatesTopK.class, FBCandidateFingerprints.class)
                : List.of(FormulaScoring.class, FBCandidatesTopK.class));

        SiriusProjectSpaceInstance instance = loadInstance(alignedFeatureId);
        FormulaResultId fidObj = parseFID(instance, formulaId);
        return loadStructureCandidates(instance, fidObj, pageable, FBCandidatesTopK.class, FBCandidateFingerprints.class, para, optFields)
                .map(l -> l.stream().map(c -> (StructureCandidateScored) c).toList())
                .map(it -> (Page<StructureCandidateScored>) new PageImpl<>(it, pageable, Long.MAX_VALUE))
                .orElse(Page.empty(pageable)); //todo nightsky: number of candidates for page -> do only for new project space.
    }

    @Override
    public Page<StructureCandidateFormula> findStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        List<Class<? extends DataAnnotation>> para = (optFields.contains(StructureCandidateScored.OptField.fingerprint)
                ? List.of(FormulaScoring.class, FBCandidatesTopK.class, FBCandidateFingerprints.class)
                : List.of(FormulaScoring.class, FBCandidatesTopK.class));

        SiriusProjectSpaceInstance instance = loadInstance(alignedFeatureId);
        List<StructureCandidateFormula> candidates = instance.loadFormulaResults(FormulaScoring.class).stream()
                .filter(fr -> fr.getCandidate().getAnnotation(FormulaScoring.class)
                        .flatMap(s -> s.getAnnotation(TopCSIScore.class)).isPresent())
                .map(fr -> fr.getCandidate().getId())
                .map(fid -> loadStructureCandidates(instance, fid, pageable, FBCandidatesTopK.class, FBCandidateFingerprints.class, para, optFields))
                .filter(Optional::isPresent).flatMap(Optional::stream).flatMap(List::stream)
                .sorted(Comparator.comparing(StructureCandidateScored::getCsiScore).reversed())
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize()).toList();

        return new PageImpl<>(candidates, pageable, Long.MAX_VALUE);  //todo nightsky: number of candidates for page -> do only for new project space.
    }

    //todo nightsky: we want to annotate DeNovo hits with db link meta information if available -> might need nu database.
    @Override
    public Page<StructureCandidateScored> findDeNovoStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        List<Class<? extends DataAnnotation>> para = (optFields.contains(StructureCandidateScored.OptField.fingerprint)
                ? List.of(FormulaScoring.class, MsNovelistFBCandidates.class, MsNovelistFBCandidateFingerprints.class)
                : List.of(FormulaScoring.class, MsNovelistFBCandidates.class));

        SiriusProjectSpaceInstance instance = loadInstance(alignedFeatureId);
        FormulaResultId fidObj = parseFID(instance, formulaId);
        return loadStructureCandidates(instance, fidObj, pageable, MsNovelistFBCandidates.class, MsNovelistFBCandidateFingerprints.class, para, optFields)
                .map(l -> l.stream().map(c -> (StructureCandidateScored) c).toList())
                .map(it -> (Page<StructureCandidateScored>) new PageImpl<>(it, pageable, Long.MAX_VALUE))
                .orElse(Page.empty(pageable)); //todo nightsky: number of candidates for page -> do only for new project space.
    }

    @Override
    public Page<StructureCandidateFormula> findDeNovoStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        List<Class<? extends DataAnnotation>> para = (optFields.contains(StructureCandidateScored.OptField.fingerprint)
                ? List.of(FormulaScoring.class, MsNovelistFBCandidates.class, MsNovelistFBCandidateFingerprints.class)
                : List.of(FormulaScoring.class, MsNovelistFBCandidates.class));

        SiriusProjectSpaceInstance instance = loadInstance(alignedFeatureId);
        List<StructureCandidateFormula> candidates = instance.loadFormulaResults(FormulaScoring.class).stream()
                .filter(fr -> fr.getCandidate().getAnnotation(FormulaScoring.class)
                        .flatMap(s -> s.getAnnotation(TopMsNovelistScore.class)).isPresent())
                .map(fr -> fr.getCandidate().getId())
                .map(fid -> loadStructureCandidates(instance, fid, pageable, MsNovelistFBCandidates.class, MsNovelistFBCandidateFingerprints.class, para, optFields))
                .filter(Optional::isPresent).flatMap(Optional::stream).flatMap(List::stream)
                .sorted(Comparator.comparing(StructureCandidateScored::getCsiScore).reversed())
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize()).toList();

        return new PageImpl<>(candidates, pageable, Long.MAX_VALUE);  //todo nightsky: number of candidates for page -> do only for new project space.
    }

    @Override
    public StructureCandidateFormula findTopStructureCandidateByFeatureId(String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        List<Class<? extends DataAnnotation>> para = optFields.contains(StructureCandidateScored.OptField.fingerprint)
                ? List.of(FormulaScoring.class, FBCandidatesTopK.class, FBCandidateFingerprints.class)
                : List.of(FormulaScoring.class, FBCandidatesTopK.class);

        SiriusProjectSpaceInstance instance = loadInstance(alignedFeatureId);
        SpectralSearchResult spectralSearchResult = optFields.contains(StructureCandidateScored.OptField.libraryMatches) ?
                loadSpectalLibraryMachtes(instance).orElse(null) : null;

        return instance.loadTopFormulaResult(List.of(TopCSIScore.class)).flatMap(fr -> {
            fr.getId().setAnnotation(FBCandidateNumber.class, new FBCandidateNumber(1));
            return instance.loadFormulaResult(fr.getId(), (Class<? extends DataAnnotation>[]) para.toArray(Class[]::new))
                    .flatMap(fr2 -> fr2.getAnnotation(FBCandidatesTopK.class).map(FBCandidatesTopK::getResults)
                            .filter(l -> !l.isEmpty()).map(r -> r.get(0))
                            .map(sc -> StructureCandidateFormula.of(sc,
                                    fr2.getAnnotation(FBCandidateFingerprints.class)
                                            .map(FBCandidateFingerprints::getFingerprints)
                                            .map(fps -> fps.isEmpty() ? null : fps.get(0))
                                            .orElse(null),
                                    spectralSearchResult, optFields, fr.getId()))
                    );
        }).orElse(null);
    }

    @Override
    public AnnotatedSpectrum findAnnotatedSpectrumByStructureId(int specIndex, @Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId) {
        SiriusProjectSpaceInstance instance = loadInstance(alignedFeatureId);
        Optional<FormulaResult> fr;
        if (inchiKey == null || inchiKey.isBlank()) {
            fr = instance.loadFormulaResult(parseFID(instance, formulaId), FTree.class);
        } else {
            fr = instance.loadFormulaResult(parseFID(instance, formulaId), FTree.class, FBCandidates.class);
        }

        FTree ftree = fr.flatMap(f -> f.getAnnotation(FTree.class)).orElse(null);
        String smiles = fr.flatMap(f -> f.getAnnotation(FBCandidates.class)).stream()
                .flatMap(fb -> fb.getResults().stream())
                .map(SScored::getCandidate)
                .filter(c -> Objects.equals(c.getInchiKey2D(), inchiKey))
                .findFirst().map(CompoundCandidate::getSmiles).orElse(null);

        return asAnnotatedSpectrum(specIndex, instance, ftree, smiles);
    }

    @Override
    public AnnotatedMsMsData findAnnotatedMsMsDataByStructureId(@Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId) {
        SiriusProjectSpaceInstance instance = loadInstance(alignedFeatureId);
        Optional<FormulaResult> fr;
        if (inchiKey == null || inchiKey.isBlank()) {
            fr = instance.loadFormulaResult(parseFID(instance, formulaId), FTree.class);
        } else {
            fr = instance.loadFormulaResult(parseFID(instance, formulaId), FTree.class, FBCandidates.class);
        }

        FTree ftree = fr.flatMap(f -> f.getAnnotation(FTree.class)).orElse(null);
        String smiles = fr.flatMap(f -> f.getAnnotation(FBCandidates.class)).stream()
                .flatMap(fb -> fb.getResults().stream())
                .map(SScored::getCandidate)
                .filter(c -> Objects.equals(c.getInchiKey2D(), inchiKey))
                .findFirst().map(CompoundCandidate::getSmiles).orElse(null);

        return AnnotatedMsMsData.of(loadExperiment(instance), ftree, smiles);
    }

    @Override
    public StructureCandidateScored findStructureCandidateById(@NotNull String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        SiriusProjectSpaceInstance instance = loadInstance(alignedFeatureId);
        SpectralSearchResult spectralSearchResult = optFields.contains(StructureCandidateScored.OptField.libraryMatches) ?
                loadSpectalLibraryMachtes(instance).orElse(null) : null;
        FormulaResultId fid = parseFID(instance, formulaId);
        return instance.loadFormulaResult(fid, FormulaScoring.class, FBCandidates.class)
                .map(fr -> {
                    FBCandidates candidates = fr.getAnnotation(FBCandidates.class).orElse(null);

                    if (candidates != null) {
                        AtomicInteger index = new AtomicInteger(0);
                        for (Scored<CompoundCandidate> result : candidates.getResults()) {
                            if (inchiKey.equals(result.getCandidate().getInchiKey2D())) {
                                Fingerprint fp = null;
                                if (optFields.contains(StructureCandidateScored.OptField.fingerprint))
                                    fp = instance.loadFormulaResult(fid, FBCandidateFingerprints.class)
                                            .flatMap(frr -> frr.getAnnotation(FBCandidateFingerprints.class))
                                            .map(fps -> fps.getFingerprints().get(index.get())).orElse(null);

                                return StructureCandidateFormula.of(result, fp, spectralSearchResult, optFields, fid);
                            }
                            index.incrementAndGet();
                        }
                    }
                    return null;
                }).orElseThrow();
    }

    private AlignedFeature asAlignedFeature(CompoundContainerId cid, EnumSet<AlignedFeature.OptField> optFields) {
        final AlignedFeature alignedFeature = asAlignedFeature(cid);
        if (!optFields.isEmpty()) {
            SiriusProjectSpaceInstance instance = projectSpaceManager.getInstanceFromCompound(cid);
            if (optFields.contains(AlignedFeature.OptField.topAnnotations))
                alignedFeature.setTopAnnotations(extractTopAnnotations(instance));
            if (optFields.contains(AlignedFeature.OptField.topAnnotationsDeNovo))
                alignedFeature.setTopAnnotationsDeNovo(extractTopDeNovoAnnotations(instance));
            if (optFields.contains(AlignedFeature.OptField.msData))
                alignedFeature.setMsData(asCompoundMsData(instance));
        }
        return alignedFeature;
    }


    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#0.000");

    private Compound asCompound(List<CompoundContainerId> cids, @NotNull EnumSet<Compound.OptField> optFields,
                                @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields) {
        //compound with ID
        Compound.CompoundBuilder c = Compound.builder()
                .compoundId(cids.stream().map(CompoundContainerId::getGroupId)
                        .filter(Optional::isPresent).flatMap(Optional::stream).findFirst().orElseThrow());

        {
            // merge optional field config
            final EnumSet<AlignedFeature.OptField> mergedFeatureFields = EnumSet.copyOf(optFeatureFields);
            if (optFields.contains(Compound.OptField.consensusAnnotations))
                mergedFeatureFields.add(AlignedFeature.OptField.topAnnotations);
            if (optFields.contains(Compound.OptField.consensusAnnotationsDeNovo))
                mergedFeatureFields.add(AlignedFeature.OptField.topAnnotationsDeNovo);

            // features
            List<AlignedFeature> features = cids.stream().map(cid -> asAlignedFeature(cid, mergedFeatureFields)).toList();
            c.features(features);

            if (optFields.contains(Compound.OptField.consensusAnnotations))
                c.consensusAnnotations(AnnotationUtils.buildConsensusAnnotationsCSI(features));
            if (optFields.contains(Compound.OptField.consensusAnnotationsDeNovo))
                c.consensusAnnotationsDeNovo(AnnotationUtils.buildConsensusAnnotationsDeNovo(features));
            if (optFields.contains(Compound.OptField.customAnnotations))
                c.customAnnotations(ConsensusAnnotationsCSI.builder().build()); //todo implement custom annotations -> storage needed

            //remove optionals if not requested
            if (!optFeatureFields.contains(AlignedFeature.OptField.topAnnotations))
                features.forEach(f -> f.setTopAnnotations(null));
            if (!optFeatureFields.contains(AlignedFeature.OptField.topAnnotationsDeNovo))
                features.forEach(f -> f.setTopAnnotationsDeNovo(null));
        }

        //compound RT
        RetentionTime rt = cids.stream().map(CompoundContainerId::getGroupRt)
                .filter(Optional::isPresent).flatMap(Optional::stream).filter(RetentionTime::isInterval).findFirst()
                .orElse(cids.stream().map(CompoundContainerId::getRt).filter(Optional::isPresent)
                        .flatMap(Optional::stream).reduce(RetentionTime::merge).orElse(null)
                );

        if (rt != null) {
            if (rt.isInterval()) {
                c.rtStartSeconds(rt.getStartTime());
                c.rtEndSeconds(rt.getEndTime());
            } else {
                c.rtStartSeconds(rt.getMiddleTime());
                c.rtEndSeconds(rt.getMiddleTime());
            }
        }

        //neutral mass
        DoubleArrayList neutralMasses = cids.stream()
                .filter(cid -> cid.getIonType().map(p -> !p.isIonizationUnknown()).orElse(false))
                .map(cid -> cid.getIonMass().map(m -> cid.getIonType().get().precursorMassToNeutralMass(m)))
                .flatMap(Optional::stream).sorted().collect(Collectors.toCollection(DoubleArrayList::new));

        if (neutralMasses.size() == 1)
            c.neutralMass(neutralMasses.getDouble(0));
        else if (!neutralMasses.isEmpty()) {
            if (!new Deviation(10).inErrorWindow(neutralMasses.getDouble(0), neutralMasses.topDouble()))
                log.warn("Mass deviation of calculated neutral mass of compound with id '" + c.build() + "' higher than 10ppm");
            c.neutralMass(neutralMasses.getDouble(neutralMasses.size() / 2));
        }

        Compound co = c.build();
        co.setName(cids.stream().map(CompoundContainerId::getGroupName)
                .filter(Optional::isPresent).flatMap(Optional::stream).findFirst()
                .orElse("rt" + Optional.ofNullable(rt).map(r -> NUMBER_FORMAT.format(r.getMiddleTime() / 60)).orElse("N/A")
                        + "-m" + Optional.ofNullable(co.getNeutralMass()).map(NUMBER_FORMAT::format).orElse("N/A")));

        return co;
    }


    protected static Optional<SpectralSearchResult> loadSpectalLibraryMachtes(SiriusProjectSpaceInstance instance) {
        return instance.loadCompoundContainer(SpectralSearchResult.class).getAnnotation(SpectralSearchResult.class);
    }

    protected static Ms2Experiment loadExperiment(SiriusProjectSpaceInstance instance) {
        return instance.loadCompoundContainer(Ms2Experiment.class).getAnnotation(Ms2Experiment.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Could not find spectra data for '" + instance.getExternalFeatureId().orElseGet(instance::getId) + "'!"));
    }

    private SiriusProjectSpaceInstance loadInstance(String alignedFeatureId) {
        try {
            return projectSpaceManager.getInstanceFromCompound(parseCID(alignedFeatureId));
        } catch (RuntimeException e) {
           throw new ResponseStatusException(HttpStatus.NO_CONTENT, "Instance with id '" + alignedFeatureId + "' does not exist!'.");
        }
    }

    protected CompoundContainerId parseCID(String cid) {
        return projectSpaceManager.getProjectSpaceImpl().findCompound(cid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "There is no Compound with ID '" + cid + "' in project with name '" +
                                projectSpaceManager.getProjectSpaceImpl().getLocation() + "'"));
    }

    protected FormulaResultId parseFID(String cid, String fid) {
        return parseFID(loadInstance(cid), fid);
    }

    protected FormulaResultId parseFID(SiriusProjectSpaceInstance instance, String fid) {
        return instance.loadCompoundContainer().findResult(fid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "FormulaResult with FCandidate '" + fid + "' not found!"));

    }

    @Override
    public String getFingerIdDataCSV(int charge) {
        StringWriter w = new StringWriter();
        writeFingerIdData(w, charge);
        return w.toString();
    }

    @Override
    public String getCanopusClassyFireDataCSV(int charge) {
        StringWriter w = new StringWriter();
        writeCanopusClassyFireData(w, charge);
        return w.toString();
    }

    @Override
    public String getCanopusNpcDataCSV(int charge) {
        StringWriter w = new StringWriter();
        writeCanopusNpcData(w, charge);
        return w.toString();
    }

    @Override
    public String findSiriusFtreeJsonById(String formulaId, String alignedFeatureId) {
        final FTJsonWriter ftWriter = new FTJsonWriter();
        SiriusProjectSpaceInstance instance = loadInstance(alignedFeatureId);
        return instance.loadFormulaResult(parseFID(instance, formulaId), FTree.class)
                .flatMap(fr -> fr.getAnnotation(FTree.class))
                .map(ftWriter::treeToJsonString).orElse(null);
    }

    public void writeFingerIdData(@NotNull Writer writer, int charge) {
        projectSpaceManager.getFingerIdData(charge).ifPresent(data -> {
            try {
                FingerIdData.write(writer, data);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error when extracting FingerIdData from project '" + projectId + "'. Message: " + e.getMessage());
            }
        });
    }

    public void writeCanopusClassyFireData(@NotNull Writer writer, int charge) {
        projectSpaceManager.getCanopusCfData(charge).ifPresent(data -> {
            try {
                CanopusCfData.write(writer, data);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error when extracting CanopusClassyFireData from project '" + projectId + "'. Message: " + e.getMessage());
            }
        });
    }

    public void writeCanopusNpcData(@NotNull Writer writer, int charge) {
        projectSpaceManager.getCanopusNpcData(charge).ifPresent(data -> {
            try {
                CanopusNpcData.write(writer, data);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error when extracting CanopusNpcData from project '" + projectId + "'. Message: " + e.getMessage());
            }
        });
    }

    private static <C extends CompoundCandidate> Optional<List<StructureCandidateFormula>> loadStructureCandidates(
            SiriusProjectSpaceInstance instance, FormulaResultId fidObj,
            Pageable pageable,
            Class<? extends AbstractFBCandidates<C>> candidateType,
            Class<? extends AbstractFBCandidateFingerprints> candidateFpType,
            List<Class<? extends DataAnnotation>> para,
            EnumSet<StructureCandidateScored.OptField> optFields
    ) {
        long topK = pageable.getOffset() + pageable.getPageSize();
        fidObj.setAnnotation(FBCandidateNumber.class, topK <= 0 ? FBCandidateNumber.ALL : new FBCandidateNumber((int) topK));
        FormulaResult fr = instance.loadFormulaResult(fidObj, (Class<? extends DataAnnotation>[]) para.toArray(Class[]::new)).orElseThrow();
        SpectralSearchResult spectralSearchResult = optFields.contains(StructureCandidateScored.OptField.libraryMatches) ?
                loadSpectalLibraryMachtes(instance).orElse(null) : null;

        return fr.getAnnotation(candidateType).map(AbstractFBCandidates::getResults).map(l -> {
            List<StructureCandidateFormula> candidates = new ArrayList<>();

            Iterator<Scored<C>> it =
                    l.stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).iterator();

            if (optFields.contains(StructureCandidateScored.OptField.fingerprint)) {
                Iterator<Fingerprint> fps = fr.getAnnotationOrThrow(candidateFpType).getFingerprints()
                        .stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).iterator();

                if (it.hasNext())//tophit
                    candidates.add(StructureCandidateFormula.of(it.next(), fps.next(), spectralSearchResult, optFields, fidObj));

                while (it.hasNext())
                    candidates.add(StructureCandidateFormula.of(it.next(), fps.next(), spectralSearchResult, optFields, fidObj));
            } else {
                if (it.hasNext())//tophit
                    candidates.add(StructureCandidateFormula.of(it.next(), null, spectralSearchResult, optFields, fidObj));

                while (it.hasNext())
                    candidates.add(StructureCandidateFormula.of(it.next(), null, spectralSearchResult, optFields, fidObj));
            }
            return candidates;
        });
    }

    public static FormulaCandidate.FormulaCandidateBuilder makeFormulaCandidate(SiriusProjectSpaceInstance inst, FormulaResult res, EnumSet<FormulaCandidate.OptField> optFields) {
        FormulaCandidate.FormulaCandidateBuilder candidate = optFields.contains(FormulaCandidate.OptField.statistics)
                ? asFormulaCandidate(res)
                : asFormulaCandidate(res.getId(), res.getAnnotationOrThrow(FormulaScoring.class));

        if (optFields.contains(FormulaCandidate.OptField.fragmentationTree))
            res.getAnnotation(FTree.class).map(FragmentationTree::fromFtree).ifPresent(candidate::fragmentationTree);
        if (optFields.contains(FormulaCandidate.OptField.annotatedSpectrum))
            candidate.annotatedSpectrum(asAnnotatedSpectrum(-1, inst, res.getAnnotation(FTree.class).orElse(null), null));
        if (optFields.contains(FormulaCandidate.OptField.isotopePattern))
            candidate.isotopePatternAnnotation(asIsotopePatternAnnotation(inst, res.getAnnotation(FTree.class).orElse(null)));
        if (optFields.contains(FormulaCandidate.OptField.lipidAnnotation))
            res.getAnnotation(FTree.class).map(AnnotationUtils::asLipidAnnotation).ifPresent(candidate::lipidAnnotation);
        if (optFields.contains(FormulaCandidate.OptField.predictedFingerprint))
            res.getAnnotation(FingerprintResult.class).map(fpResult -> fpResult.fingerprint.toProbabilityArray())
                    .ifPresent(candidate::predictedFingerprint);
        if (optFields.contains(FormulaCandidate.OptField.canopusPredictions))
            res.getAnnotation(CanopusResult.class).map(CanopusPrediction::of).ifPresent(candidate::canopusPrediction);
        if (optFields.contains(FormulaCandidate.OptField.compoundClasses))
            res.getAnnotation(CanopusResult.class).map(CompoundClasses::of).ifPresent(candidate::compoundClasses);
        return candidate;
    }

    public AlignedFeature asAlignedFeature(CompoundContainerId cid) {
        final AlignedFeature id = AlignedFeature.builder()
                .alignedFeatureId(cid.getDirectoryName())
                .name(cid.getCompoundName())
                .externalFeatureId(cid.getFeatureId().orElse(null))
                .ionMass(cid.getIonMass().orElse(0d))
                .computing(computeStateProvider.apply(this, cid.getDirectoryName()))
                .detectedAdducts(Set.of())
                .build();

        cid.getIonType().map(PrecursorIonType::getCharge).ifPresent(id::setCharge);
        cid.getRt().ifPresent(rt -> {
            if (rt.isInterval()) {
                id.setRtStartSeconds(rt.getStartTime());
                id.setRtEndSeconds(rt.getEndTime());
            } else {
                id.setRtStartSeconds(rt.getRetentionTimeInSeconds());
                id.setRtEndSeconds(rt.getRetentionTimeInSeconds());
            }
        });
        return id;
    }

    public static FormulaCandidate.FormulaCandidateBuilder asFormulaCandidate(@NotNull FormulaResultId formulaId) {
        return FormulaCandidate.builder()
                .formulaId(formulaId.fileName())
                .molecularFormula(formulaId.getMolecularFormula().toString())
                .adduct(formulaId.getIonType().toString());
    }

    public static FormulaCandidate.FormulaCandidateBuilder asFormulaCandidate(@NotNull FormulaResultId formulaId, @Nullable FormulaScoring scorings) {
        final FormulaCandidate.FormulaCandidateBuilder frs = asFormulaCandidate(formulaId);

        if (scorings != null) {
            scorings.getAnnotation(SiriusScore.class).
                    ifPresent(sscore -> frs.siriusScore(sscore.score()));
            scorings.getAnnotation(IsotopeScore.class).
                    ifPresent(iscore -> frs.isotopeScore(iscore.score()));
            scorings.getAnnotation(TreeScore.class).
                    ifPresent(tscore -> frs.treeScore(tscore.score()));
            scorings.getAnnotation(ZodiacScore.class).
                    ifPresent(zscore -> frs.zodiacScore(zscore.score()));
//            scorings.getAnnotation(TopCSIScore.class).
//                    ifPresent(csiScore -> frs.topCSIScore(csiScore.score()));
        }

        return frs;
    }

    public static FormulaCandidate.FormulaCandidateBuilder asFormulaCandidate(@NotNull FormulaResult formulaResult) {
        @NotNull FormulaScoring scorings = formulaResult.getAnnotationOrThrow(FormulaScoring.class);

        final FormulaCandidate.FormulaCandidateBuilder frs = asFormulaCandidate(formulaResult.getId(), scorings);

        formulaResult.getAnnotation(FTree.class).
                ifPresent(fTree -> {
                    final FTreeMetricsHelper metrHelp = new FTreeMetricsHelper(fTree);
                    frs.numOfExplainedPeaks(metrHelp.getNumOfExplainedPeaks());
                    frs.numOfExplainablePeaks(metrHelp.getNumberOfExplainablePeaks());
                    frs.totalExplainedIntensity(metrHelp.getExplainedIntensityRatio());
                    frs.medianMassDeviation(metrHelp.getMedianMassDeviation());
                });

        return frs;
    }

    public static Class<? extends DataAnnotation>[] resolveFormulaCandidateAnnotations(EnumSet<FormulaCandidate.OptField> optFields) {
        List<Class<? extends DataAnnotation>> classes = new ArrayList<>();
        classes.add(FormulaScoring.class);
        if (Stream.of(
                        FormulaCandidate.OptField.statistics,
                        FormulaCandidate.OptField.fragmentationTree,
                        FormulaCandidate.OptField.annotatedSpectrum,
                        FormulaCandidate.OptField.isotopePattern,
                        FormulaCandidate.OptField.lipidAnnotation)
                .anyMatch(optFields::contains))
            classes.add(FTree.class);

        if (optFields.contains(FormulaCandidate.OptField.predictedFingerprint))
            classes.add(FingerprintResult.class);

        if (Stream.of(FormulaCandidate.OptField.compoundClasses, FormulaCandidate.OptField.canopusPredictions)
                .anyMatch(optFields::contains))
            classes.add(CanopusResult.class);

        return classes.toArray(Class[]::new);
    }

    public static IsotopePatternAnnotation asIsotopePatternAnnotation(SiriusProjectSpaceInstance instance, FTree ftree) {
        return Spectrums.createIsotopePatternAnnotation(loadExperiment(instance), ftree);
    }

    public static AnnotatedSpectrum asAnnotatedSpectrum(int specIndex, SiriusProjectSpaceInstance instance, FTree ftree, String structureSmiles) {
        Ms2Experiment exp = loadExperiment(instance);
        if (specIndex < 0)
            return Spectrums.createMergedMsMsWithAnnotations(exp, ftree, structureSmiles);
        else
            return Spectrums.createMsMsWithAnnotations(exp.getMs2Spectra().get(specIndex), ftree, structureSmiles);
    }

    @NotNull
    public static FeatureAnnotations extractTopDeNovoAnnotations(SiriusProjectSpaceInstance inst) {
        return inst.loadTopFormulaResult(List.of(SiriusScore.class)).map(FormulaResult::getId)
                .flatMap(frid -> inst.loadFormulaResult(frid, FormulaScoring.class, FTree.class, CanopusResult.class)
                        .map(topHit -> {
                            final FeatureAnnotations cSum = new FeatureAnnotations();
//
                            //add formula summary
                            cSum.setFormulaAnnotation(asFormulaCandidate(topHit).build());

                            topHit.getAnnotation(MsNovelistFBCandidates.class).map(MsNovelistFBCandidates::getResults)
                                    .filter(l -> !l.isEmpty()).map(r -> r.get(0)).map(s ->
                                            StructureCandidateFormula.of(s, EnumSet.noneOf(StructureCandidateScored.OptField.class), topHit.getId()))
                                    .ifPresent(cSum::setStructureAnnotation);

                            topHit.getAnnotation(CanopusResult.class).map(CompoundClasses::of).
                                    ifPresent(cSum::setCompoundClassAnnotation);
                            return cSum;

                        })).orElseGet(FeatureAnnotations::new);
    }

    @NotNull
    public static FeatureAnnotations extractTopAnnotations(SiriusProjectSpaceInstance inst) {
        return inst.loadTopFormulaResult(List.of(TopCSIScore.class, SiriusScore.class)).map(FormulaResult::getId).flatMap(frid -> {
            frid.setAnnotation(FBCandidateNumber.class, new FBCandidateNumber(1));
            return inst.loadFormulaResult(frid, FormulaScoring.class, FTree.class, FBCandidatesTopK.class, CanopusResult.class, StructureSearchResult.class)
                    .map(topHit -> {
                        final FeatureAnnotations cSum = new FeatureAnnotations();
//
                        //add formula summary
                        cSum.setFormulaAnnotation(asFormulaCandidate(topHit).build());

                        // fingerid result
                        topHit.getAnnotation(FBCandidatesTopK.class).map(FBCandidatesTopK::getResults)
                                .filter(l -> !l.isEmpty()).map(r -> r.get(0)).map(s ->
                                        StructureCandidateFormula.of(s,
                                                EnumSet.of(StructureCandidateScored.OptField.dbLinks, StructureCandidateScored.OptField.libraryMatches), topHit.getId()))
                                .ifPresent(cSum::setStructureAnnotation);

                        topHit.getAnnotation(CanopusResult.class).map(CompoundClasses::of).
                                ifPresent(cSum::setCompoundClassAnnotation);

                        // Add list specific results: confidences, expansive search state

                        topHit.getAnnotation(FormulaScoring.class).get().getAnnotation(ConfidenceScore.class).ifPresent(c -> cSum.setConfidenceExactMatch(c.score()));
                        topHit.getAnnotation(FormulaScoring.class).get().getAnnotation(ConfidenceScoreApproximate.class).ifPresent(c -> cSum.setConfidenceApproxMatch(c.score()));
                        topHit.getAnnotation(StructureSearchResult.class).ifPresent(c -> cSum.setExpansiveSearchState(c.getExpansiveSearchConfidenceMode()));

                        return cSum;

                    });
        }).orElseGet(FeatureAnnotations::new);
    }

    public static MsData asCompoundMsData(SiriusProjectSpaceInstance instance) {
        return instance.loadCompoundContainer(Ms2Experiment.class)
                .getAnnotation(Ms2Experiment.class).map(MsData::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Feature with ID '" + instance + "' has no input Data!"));
    }

    public static EnumSet<CompoundQuality.CompoundQualityFlag> asCompoundQualityData(SiriusProjectSpaceInstance instance) {
        return instance.loadCompoundContainer(Ms2Experiment.class)
                .getAnnotation(Ms2Experiment.class)
                .flatMap(exp -> exp.getAnnotation(CompoundQuality.class))
                .map(CompoundQuality::getFlags)
                .orElse(EnumSet.of(CompoundQuality.CompoundQualityFlag.UNKNOWN));
    }

    public static LCMSFeatureQuality asCompoundLCMSFeatureQuality(SiriusProjectSpaceInstance instance) {
        final LCMSPeakInformation peakInformation = instance.loadCompoundContainer(LCMSPeakInformation.class).getAnnotation(LCMSPeakInformation.class, LCMSPeakInformation::empty);
        Ms2Experiment experiment = instance.getExperiment();
        Optional<CoelutingTraceSet> traceSet = peakInformation.getTracesFor(0);
        if (traceSet.isPresent()) {
            final LCMSCompoundSummary summary = new LCMSCompoundSummary(traceSet.get(), traceSet.get().getIonTrace(), experiment);
            return new LCMSFeatureQuality(summary);
        } else {
            return null;
        }
    }

    private static <S, T> Optional<T> opt(S input, Function<S, T> convert) {
        return Optional.ofNullable(input).map(convert);
    }
}
