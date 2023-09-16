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

package de.unijena.bioinf.ms.middleware.features;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.FBCandidateFingerprints;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.lcms.LCMSCompoundSummary;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.middleware.BaseApiController;
import de.unijena.bioinf.ms.middleware.compute.model.ComputeContext;
import de.unijena.bioinf.ms.middleware.features.model.AlignedFeature;
import de.unijena.bioinf.ms.middleware.features.model.LCMSFeatureQuality;
import de.unijena.bioinf.ms.middleware.features.model.MsData;
import de.unijena.bioinf.ms.middleware.features.model.annotations.*;
import de.unijena.bioinf.ms.middleware.spectrum.AnnotatedSpectrum;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.projectspace.fingerid.FBCandidateNumber;
import de.unijena.bioinf.sirius.Sirius;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.LoggerFactory;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping(value = "/api/projects/{projectId}/aligned-features")
@Tag(name = "Feature based API", description = "Access features (aligned over runs) and there Annotations of " +
        "a specified project-space. This is the entry point to access all raw annotation results an there summaries.")
public class AlignedFeaturesController extends BaseApiController {

    private final ComputeContext computeContext;

    @Autowired
    public AlignedFeaturesController(ComputeContext context) {
        super(context.siriusContext);
        this.computeContext = context;
    }


    /**
     * Get all available features (aligned over runs) in the given project-space.
     *
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included
     * @return AlignedFeatures with additional annotations and MS/MS data (if specified).
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<AlignedFeature> getAlignedFeatures(
            @PathVariable String projectId,
            @ParameterObject Pageable pageable,
            @RequestParam(required = false, defaultValue = "") EnumSet<AlignedFeature.OptFields> optFields
    ) {
        System.out.println(optFields == null);
        LoggerFactory.getLogger(AlignedFeaturesController.class).info("Started collecting aligned features...");
        final ProjectSpaceManager<?> space = projectSpace(projectId);

        final List<AlignedFeature> alignedFeatures = StreamSupport.stream(space.projectSpace().spliterator(), false)
                .skip(pageable.getOffset()).limit(pageable.getPageSize())
                .map(ccid -> asCompoundId(ccid, space, optFields))
                .toList();

        LoggerFactory.getLogger(AlignedFeaturesController.class).info("Finished parsing aligned features...");
        return new PageImpl(alignedFeatures, pageable, space.size());
    }


    /**
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     *
     * @param projectId      project-space to read from.
     * @param alignFeatureId identifier of feature (aligned over runs) to access.
     * @param optFields      set of optional fields to be included
     * @return AlignedFeature with additional annotations and MS/MS data (if specified).
     */
    @GetMapping(value = "/{alignFeatureId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AlignedFeature getAlignedFeatures(
            @PathVariable String projectId, @PathVariable String alignFeatureId,
            @RequestParam(required = false, defaultValue = "") EnumSet<AlignedFeature.OptFields> optFields
    ) {
        final ProjectSpaceManager<?> space = projectSpace(projectId);
        final CompoundContainerId ccid = parseCID(space, alignFeatureId);
        return asCompoundId(ccid, space, optFields);
    }

    /**
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     *
     * @param projectId      project-space to delete from.
     * @param alignFeatureId identifier of feature (aligned over runs) to delete.
     */
    @DeleteMapping(value = "/{alignFeatureId}") //todo how to handle grouped features?
    public void deleteAlignedFeature(@PathVariable String projectId, @PathVariable String alignFeatureId) throws IOException {
        final ProjectSpaceManager<?> space = projectSpace(projectId);
        CompoundContainerId compound = space.projectSpace().findCompound(alignFeatureId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "AlignedFeature with id '" + alignFeatureId + "' does not exist in '" + projectId + "'."));
        space.projectSpace().deleteCompound(compound);
    }


    /**
     * List of all FormulaResultContainers available for this feature with minimal information.
     * Can be enriched with an optional results overview.
     *
     * @param projectId      project-space to read from.
     * @param alignFeatureId feature (aligned over runs) the formula result belongs to.
     * @param optFields      set of optional fields to be included
     * @return All FormulaCandidate of this feature with.
     */
    @GetMapping(value = "/{alignFeatureId}/formulas", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<FormulaCandidate> getFormulaCandidates(
            @PathVariable String projectId, @PathVariable String alignFeatureId,
            @ParameterObject Pageable pageable,
            @RequestParam(required = false, defaultValue = "") EnumSet<FormulaCandidate.OptFields> optFields
    ) {
        //todo filtering
        //todo ordering
        LoggerFactory.getLogger(getClass()).info("Started collecting formulas...");
        Instance instance = loadInstance(projectId, alignFeatureId);
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

        if (Stream.of(FormulaCandidate.OptFields.statistics, FormulaCandidate.OptFields.fragmentationTree, FormulaCandidate.OptFields.simulatedIsotopePattern).anyMatch(optFields::contains)) {
            return new PageImpl<>(paged.peek(fr -> instance.loadFormulaResult(fr.getId(), FTree.class)
                    .ifPresent(fr::setAnnotationsFrom)).map(FormulaCandidate::of).toList(), pageable, size);
        } else {
            return new PageImpl<>(paged
                    .map(fr -> FormulaCandidate.of(fr.getId(), fr.getAnnotation(FormulaScoring.class).orElse(null)))
                    .toList(), pageable, size);
        }
        //todo add optional fields
    }

    /**
     * FormulaResultContainers for the given 'formulaId' with minimal information.
     * Can be enriched with an optional results overview and formula candidate information.
     *
     * @param projectId      project-space to read from.
     * @param alignFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId      identifier of the requested formula result
     * @param optFields      set of optional fields to be included
     * @return FormulaCandidate of this feature (aligned over runs) with.
     */
    @GetMapping(value = "/{alignFeatureId}/formulas/{formulaId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public FormulaCandidate getFormulaCandidate(
            @PathVariable String projectId, @PathVariable String alignFeatureId, @PathVariable String formulaId,
            @RequestParam(required = false, defaultValue = "") EnumSet<FormulaCandidate.OptFields> optFields

    ) {
        //todo filtering
        //todo ordering
        Instance instance = loadInstance(projectId, alignFeatureId);
        if (Stream.of(FormulaCandidate.OptFields.statistics, FormulaCandidate.OptFields.fragmentationTree, FormulaCandidate.OptFields.simulatedIsotopePattern).anyMatch(optFields::contains)) {
            return instance.loadFormulaResult(parseFID(instance, formulaId), FormulaScoring.class, FTree.class)
                    .map(FormulaCandidate::of)
                    .orElse(null);
        } else {
            return instance.loadFormulaResult(parseFID(instance, formulaId), FormulaScoring.class)
                    .map(fr -> FormulaCandidate.of(fr.getId(), fr.getAnnotation(FormulaScoring.class).orElse(null)))
                    .orElse(null);
        }
        //todo add optional fields
    }

    /**
     * List of StructureCandidates the given 'formulaId' with minimal information.
     * StructureCandidates can be enriched with molecular fingerprint, structure database links and pubmed ids,
     *
     * @param projectId      project-space to read from.
     * @param alignFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId      identifier of the requested formula result
     * @param optFields      set of optional fields to be included
     * @return StructureCandidate of this formula candidate with specified optional fields.
     */
    @GetMapping(value = "/{alignFeatureId}/formulas/{formulaId}/structures", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<StructureCandidate> getStructureCandidatesByFormula(
            @PathVariable String projectId, @PathVariable String alignFeatureId, @PathVariable String formulaId,
            @ParameterObject Pageable pageable,
            @RequestParam(required = false, defaultValue = "") EnumSet<StructureCandidate.OptFields> optFields
    ) {

        long topK = pageable.getOffset() + pageable.getPageSize();
        List<Class<? extends DataAnnotation>> para = (optFields.contains(StructureCandidate.OptFields.fingerprint)
                ? List.of(FormulaScoring.class, FBCandidates.class, FBCandidateFingerprints.class)
                : List.of(FormulaScoring.class, FBCandidates.class));

        Instance instance = loadInstance(projectId, alignFeatureId);
        FormulaResultId fidObj = parseFID(instance, formulaId);
        fidObj.setAnnotation(FBCandidateNumber.class, topK <= 0 ? FBCandidateNumber.ALL : new FBCandidateNumber((int) topK));
        FormulaResult fr = instance.loadFormulaResult(fidObj, (Class<? extends DataAnnotation>[]) para.toArray(Class[]::new)).orElseThrow();
        return fr.getAnnotation(FBCandidates.class).map(FBCandidates::getResults).map(l -> {
                    List<StructureCandidate> candidates = new ArrayList();

                    Iterator<Scored<CompoundCandidate>> it =
                            l.stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).iterator();

                    if (optFields.contains(StructureCandidate.OptFields.fingerprint)) {
                        Iterator<Fingerprint> fps = fr.getAnnotationOrThrow(FBCandidateFingerprints.class).getFingerprints()
                                .stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).iterator();

                        if (it.hasNext())//tophit
                            candidates.add(StructureCandidate.of(it.next(), fps.next(),
                                    fr.getAnnotationOrNull(FormulaScoring.class), optFields));

                        while (it.hasNext())
                            candidates.add(StructureCandidate.of(it.next(), fps.next(),
                                    null,optFields));
                    } else {
                        if (it.hasNext())//tophit
                            candidates.add(StructureCandidate.of(it.next(), null,
                                    fr.getAnnotationOrNull(FormulaScoring.class), optFields));

                        while (it.hasNext())
                            candidates.add(StructureCandidate.of(it.next(), null,
                                    null, optFields));
                    }
                    return candidates;
                }).map(it -> (Page<StructureCandidate>) new PageImpl<>(it))
                .orElse(Page.empty(pageable)); //todo number of candidates for page.

    }


    //todo add order by parameter?

//    /**
//     * Best Scoring StructureCandidate over all molecular formular results that belong to the specified
//     * feature (aligned over runs).
//     *
//     * @param projectId      project-space to read from.
//     * @param alignFeatureId feature (aligned over runs) the formula result belongs to.
//     * @param fingerprint    add molecular fingerprint to StructureCandidates
//     * @param dbLinks        add dbLinks to StructureCandidates
//     * @param pubMedIds      add PubMedIds (citation count) to StructureCandidates
//     * @return Best scoring FormulaResultContainers of this feature (aligned over runs) with specified extensions.
//     */
    /*@Deprecated(forRemoval = true)
    @GetMapping(value = "/{alignFeatureId}/top-structure", produces = MediaType.APPLICATION_JSON_VALUE)
    public StructureCandidate getTopStructureCandidate(@PathVariable String projectId, @PathVariable String alignFeatureId, @RequestParam(defaultValue = "false") boolean fingerprint, @RequestParam(defaultValue = "false") boolean dbLinks, @RequestParam(defaultValue = "false") boolean pubMedIds) {

        List<Class<? extends DataAnnotation>> para = (fingerprint ? List.of(FormulaScoring.class, FBCandidates.class, FBCandidateFingerprints.class) : List.of(FormulaScoring.class, FBCandidates.class));
        Instance instance = loadInstance(projectId, alignFeatureId);

        return instance.loadTopFormulaResult(List.of(TopCSIScore.class)).flatMap(fr -> {
            fr.getId().setAnnotation(FBCandidateNumber.class, new FBCandidateNumber(1));
            return instance.loadFormulaResult(fr.getId(), (Class<? extends DataAnnotation>[]) para.toArray(Class[]::new))
                    .flatMap(fr2 -> fr2.getAnnotation(FBCandidates.class).map(FBCandidates::getResults)
                            .filter(l -> !l.isEmpty()).map(r -> r.get(0))
                            .map(sc -> StructureCandidate.of(sc, fingerprint
                                    ? fr2.getAnnotationOrThrow(FBCandidateFingerprints.class).getFingerprints().get(0)
                                    : null, fr.getAnnotationOrThrow(FormulaScoring.class), dbLinks, pubMedIds))
                    );
        }).orElseThrow();
    }*/

    @Hidden
    @GetMapping(value = "/{alignFeatureId}/formulas/{formulaId}/sirius-tree", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getFTree(@PathVariable String projectId, @PathVariable String alignFeatureId, @PathVariable String formulaId) {
        Instance instance = loadInstance(projectId, alignFeatureId);
        final FTJsonWriter ftWriter = new FTJsonWriter();
        return instance.loadFormulaResult(parseFID(instance, formulaId), FTree.class).flatMap(fr -> fr.getAnnotation(FTree.class)).map(ftWriter::treeToJsonString).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FragmentationTree for '" + idString(projectId, alignFeatureId, formulaId) + "' not found!"));
    }

    /**
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier
     * This tree is used to rank formula candidates (treeScore).
     *
     * @param projectId      project-space to read from.
     * @param alignFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId      identifier of the requested formula result
     * @return Fragmentation Tree
     */
    @GetMapping(value = "/{alignFeatureId}/formulas/{formulaId}/tree", produces = MediaType.APPLICATION_JSON_VALUE)
    public FragmentationTree getFragTree(@PathVariable String projectId, @PathVariable String alignFeatureId, @PathVariable String formulaId) {
        Instance instance = loadInstance(projectId, alignFeatureId);
        return instance.loadFormulaResult(parseFID(instance, formulaId), FTree.class).flatMap(fr -> fr.getAnnotation(FTree.class)).map(FragmentationTree::fromFtree).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FragmentationTree for '" + idString(projectId, alignFeatureId, formulaId) + "' not found!"));
    }

    /**
     * Returns simulated isotope pattern (SIRIUS) for the given formula result identifier.
     * This simulated isotope pattern is used to rank formula candidates (treeScore).
     *
     * @param projectId      project-space to read from.
     * @param alignFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId      identifier of the requested formula result
     * @return Simulated isotope pattern
     */
    @GetMapping(value = "/{alignFeatureId}/formulas/{formulaId}/isotope-pattern", produces = MediaType.APPLICATION_JSON_VALUE)
    public AnnotatedSpectrum getSimulatedIsotopePattern(@PathVariable String projectId, @PathVariable String alignFeatureId, @PathVariable String formulaId) {
        Instance instance = loadInstance(projectId, alignFeatureId);
        return instance.loadFormulaResult(parseFID(instance, formulaId), FTree.class)
                .flatMap(fr -> asSimulatedIsotopePattern(instance, fr))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Isotope Pattern for '" + idString(projectId, alignFeatureId, formulaId) + "' not found!"));
    }

    /**
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier
     * This fingerprint is used to perform structure database search and predict compound classes.
     *
     * @param projectId      project-space to read from.
     * @param alignFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId      identifier of the requested formula result
     * @return probabilistic fingerprint predicted by CSI:FingerID
     */
    @GetMapping(value = "/{alignFeatureId}/formulas/{formulaId}/fingerprint", produces = MediaType.APPLICATION_JSON_VALUE)
    public double[] getFingerprintPrediction(@PathVariable String projectId, @PathVariable String alignFeatureId, @PathVariable String formulaId) {
        Instance instance = loadInstance(projectId, alignFeatureId);
        Optional<FormulaResult> fResult = instance.loadFormulaResult(parseFID(instance, formulaId), FingerprintResult.class);
        return fResult.flatMap(fr -> fr.getAnnotation(FingerprintResult.class).map(fpResult -> fpResult.fingerprint.toProbabilityArray()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fingerprint for '" + idString(projectId, alignFeatureId, formulaId) + "' not found!"));
    }

    /**
     * All predicted compound classes (CANOPUS) from ClassyFire and NPC and their probabilities,
     *
     * @param projectId      project-space to read from.
     * @param alignFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId      identifier of the requested formula result
     * @return Predicted compound classes
     */
    @GetMapping(value = "/{alignFeatureId}/formulas/{formulaId}/canopus-prediction", produces = MediaType.APPLICATION_JSON_VALUE)
    public CanopusPrediction getCanopusPrediction(@PathVariable String projectId, @PathVariable String alignFeatureId, @PathVariable String formulaId) {
        Instance instance = loadInstance(projectId, alignFeatureId);
        Optional<FormulaResult> fResult = instance.loadFormulaResult(parseFID(instance, formulaId), CanopusResult.class);
        return fResult.flatMap(fr -> fr.getAnnotation(CanopusResult.class).map(CanopusPrediction::of))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compound Classes for '" + idString(projectId, alignFeatureId, formulaId) + "' not found!"));
    }

    /**
     * Best matching compound classes,
     * Set of the highest scoring compound classes (CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology,
     *
     * @param projectId      project-space to read from.
     * @param alignFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId      identifier of the requested formula result
     * @return Best matching Predicted compound classes
     */
    @GetMapping(value = "/{alignFeatureId}/formulas/{formulaId}/best-compound-classes", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompoundClasses getBestMatchingCompoundClasses(@PathVariable String projectId, @PathVariable String alignFeatureId, @PathVariable String formulaId) {
        Instance instance = loadInstance(projectId, alignFeatureId);
        Optional<FormulaResult> fResult = instance.loadFormulaResult(parseFID(instance, formulaId), CanopusResult.class);
        return fResult.flatMap(fr -> fr.getAnnotation(CanopusResult.class).map(CompoundClasses::of))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compound Classes for '" + idString(projectId, alignFeatureId, formulaId) + "' not found!"));
    }


    public Optional<AnnotatedSpectrum> asSimulatedIsotopePattern(Instance instance, FormulaResult fResult) {
        Sirius sirius = ApplicationCore.SIRIUS_PROVIDER.sirius(instance.loadCompoundContainer(ProjectSpaceConfig.class).getAnnotationOrThrow(ProjectSpaceConfig.class).config.getConfigValue("AlgorithmProfile"));
        return Optional.of(fResult)
                .map(FormulaResult::getId)
                .map(id -> sirius.simulateIsotopePattern(id.getMolecularFormula(), id.getIonType().getIonization()))
                .map(AnnotatedSpectrum::new);
    }

    private Annotations asCompoundSummary(Instance inst) {
        return inst.loadTopFormulaResult(List.of(TopCSIScore.class)).map(de.unijena.bioinf.projectspace.FormulaResult::getId).flatMap(frid -> {
            frid.setAnnotation(FBCandidateNumber.class, new FBCandidateNumber(1));
            return inst.loadFormulaResult(frid, FormulaScoring.class, FTree.class, FBCandidates.class, CanopusResult.class)
                    .map(topHit -> {
                        final Annotations cSum = new Annotations();
//
                        //add formula summary
                        cSum.setFormulaAnnotation(FormulaCandidate.of(topHit));

                        // fingerid result
                        topHit.getAnnotation(FBCandidates.class).map(FBCandidates::getResults)
                                .filter(l -> !l.isEmpty()).map(r -> r.get(0)).map(s ->
                                        StructureCandidate.of(s, topHit.getAnnotationOrThrow(FormulaScoring.class),
                                                EnumSet.of(StructureCandidate.OptFields.dbLinks, StructureCandidate.OptFields.pubmedIds, StructureCandidate.OptFields.refSpectraLinks)))
                                .ifPresent(cSum::setStructureAnnotation);

                        topHit.getAnnotation(CanopusResult.class).map(CompoundClasses::of).
                                ifPresent(cSum::setCompoundClassAnnotation);
                        return cSum;

                    });
        }).orElseGet(Annotations::new);
    }

    private MsData asCompoundMsData(Instance instance) {
        return instance.loadCompoundContainer(Ms2Experiment.class)
                .getAnnotation(Ms2Experiment.class).map(exp -> new MsData(
                        opt(exp.getMergedMs1Spectrum(), s -> {
                            AnnotatedSpectrum t = new AnnotatedSpectrum((Spectrum<Peak>) s);
                            t.setMsLevel(1);
                            return t;
                        }).orElse(null),
                        null,
                        exp.getMs1Spectra().stream().map(x -> {
                            AnnotatedSpectrum t = new AnnotatedSpectrum(x);
                            t.setMsLevel(1);
                            return t;
                        }).collect(Collectors.toList()),
                        exp.getMs2Spectra().stream().map(x -> {
                            AnnotatedSpectrum t = new AnnotatedSpectrum(x);
                            t.setCollisionEnergy(new CollisionEnergy(x.getCollisionEnergy()));
                            t.setMsLevel(2);
                            return t;
                        }).collect(Collectors.toList()))).orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Feature with ID '" + instance + "' has no input Data!"));
    }

    private EnumSet<CompoundQuality.CompoundQualityFlag> asCompoundQualityData(Instance instance) {
        return instance.loadCompoundContainer(Ms2Experiment.class)
                .getAnnotation(Ms2Experiment.class)
                .flatMap(exp -> exp.getAnnotation(CompoundQuality.class))
                .map(CompoundQuality::getFlags)
                .orElse(EnumSet.of(CompoundQuality.CompoundQualityFlag.UNKNOWN));
    }

    private LCMSFeatureQuality asCompoundLCMSFeatureQuality(Instance instance) {
        final LCMSPeakInformation peakInformation = instance.loadCompoundContainer(LCMSPeakInformation.class).getAnnotation(LCMSPeakInformation.class, LCMSPeakInformation::empty);
        Ms2Experiment experiment = instance.getExperiment();
        Optional<CoelutingTraceSet> traceSet = peakInformation.getTracesFor(0);
        if (traceSet.isPresent()) {
            final LCMSCompoundSummary summary = new LCMSCompoundSummary(traceSet.get(), traceSet.get().getIonTrace(), experiment);
            return new LCMSFeatureQuality(summary);
        } else {
            //todo is this allowed???
            return null;
        }
    }


    private AlignedFeature asCompoundId(CompoundContainerId cid, ProjectSpaceManager<?> ps, EnumSet<AlignedFeature.OptFields> optFields) {
        final AlignedFeature alignedFeature = AlignedFeature.of(cid);
        if (!optFields.isEmpty()) {
            Instance instance = ps.getInstanceFromCompound(cid);
            if (optFields.contains(AlignedFeature.OptFields.topAnnotations))
                alignedFeature.setTopAnnotations(asCompoundSummary(instance));
            if (optFields.contains(AlignedFeature.OptFields.msData))
                alignedFeature.setMsData(asCompoundMsData(instance));
            if (optFields.contains(AlignedFeature.OptFields.lcmsFeatureQuality))
                alignedFeature.setLcmsFeatureQuality(asCompoundLCMSFeatureQuality(instance));
            if (optFields.contains(AlignedFeature.OptFields.qualityFlags))
                alignedFeature.setQualityFlags(asCompoundQualityData(instance));
        }
        return alignedFeature;
    }

    private <S, T> Optional<T> opt(S input, Function<S, T> convert) {
        return Optional.ofNullable(input).map(convert);
    }
}

