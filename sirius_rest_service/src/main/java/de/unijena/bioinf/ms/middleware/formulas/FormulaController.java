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

package de.unijena.bioinf.ms.middleware.formulas;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.properties.FinalConfig;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.FBCandidateFingerprints;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.middleware.BaseApiController;
import de.unijena.bioinf.ms.middleware.compute.model.ComputeContext;
import de.unijena.bioinf.ms.middleware.formulas.model.*;
import de.unijena.bioinf.ms.middleware.spectrum.AnnotatedSpectrum;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.projectspace.fingerid.FBCandidateNumber;
import de.unijena.bioinf.sirius.Sirius;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/projects/{projectId}/compounds/{compoundId}")
@Tag(name = "Formula Results", description = "Access results for all formula candidates of a given compound (aka feature).")
public class FormulaController extends BaseApiController {

    private final ComputeContext computeContext;

    @Autowired
    public FormulaController(ComputeContext computeContext) {
        super(computeContext.siriusContext);
        this.computeContext = computeContext;
    }

    //todo add order by parameter?

    /**
     * List of all FormulaResultContainers available for this compound/feature with minimal information.
     * Can be enriched with an optional results overview.
     *
     * @param projectId        project-space to read from.
     * @param compoundId       compound/feature the formula result belongs to.
     * @param resultOverview   add ResultOverview to the FormulaResultContainers
     * @param formulaCandidate add extended formula candidate information to the FormulaResultContainers
     * @return All FormulaResultContainers of this compound/feature with.
     */
    @GetMapping(value = "/formulas", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FormulaResultContainer> getFormulaIds(@PathVariable String projectId, @PathVariable String compoundId,
                                                      @RequestParam(defaultValue = "true") boolean resultOverview,
                                                      @RequestParam(defaultValue = "false") boolean formulaCandidate) {
        LoggerFactory.getLogger(FormulaController.class).info("Started collecting formulas...");
        Instance instance = loadInstance(projectId, compoundId);
        return instance.loadFormulaResults().stream().map(SScored::getCandidate).map(fr -> {
            FormulaResultContainer formulaResultContainer = new FormulaResultContainer(fr.getId());
            if (resultOverview)
                fr.getAnnotation(FormulaScoring.class).ifPresent(fs -> formulaResultContainer.setResultOverview(new ResultOverview(fs)));
            if (formulaCandidate) formulaResultContainer.setCandidate(FormulaCandidate.of(fr));
            return formulaResultContainer;
        }).collect(Collectors.toList());
    }

    /**
     * FormulaResultContainers for the given 'formulaId' with minimal information.
     * Can be enriched with an optional results overview and formula candidate information.
     *
     * @param projectId        project-space to read from.
     * @param compoundId       compound/feature the formula result belongs to.
     * @param formulaId        identifier of the requested formula result
     * @param resultOverview   add ResultOverview to the FormulaResultContainer
     * @param formulaCandidate add extended formula candidate information to the FormulaResultContainer
     * @return FormulaResultContainers of this compound/feature with.
     */
    @GetMapping(value = "/formulas/{formulaId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public FormulaResultContainer getFormulaResult(@PathVariable String projectId, @PathVariable String compoundId, @PathVariable String formulaId,
                                                   @RequestParam(defaultValue = "true") boolean resultOverview,
                                                   @RequestParam(defaultValue = "true") boolean formulaCandidate
    ) {
        Instance instance = loadInstance(projectId, compoundId);
        return instance.loadFormulaResult(parseFID(instance, formulaId), FormulaScoring.class).map(fr -> {
            FormulaResultContainer formulaResultContainerObject = new FormulaResultContainer(fr.getId());
            if (resultOverview)
                fr.getAnnotation(FormulaScoring.class).ifPresent(fs -> formulaResultContainerObject.setResultOverview(new ResultOverview(fs)));
            if (formulaCandidate) formulaResultContainerObject.setCandidate(FormulaCandidate.of(fr));
            return formulaResultContainerObject;
        }).orElse(null);
    }

    /**
     * List of StructureCandidates the given 'formulaId' with minimal information.
     * StructureCandidates can be enriched with molecular fingerprint, structure database links and pubmed ids,
     *
     * @param projectId   project-space to read from.
     * @param compoundId  compound/feature the formula result belongs to.
     * @param formulaId   identifier of the requested formula result
     * @param fingerprint add molecular fingerprint to StructureCandidates
     * @param dbLinks     add dbLinks to StructureCandidates
     * @param pubMedIds   add PubMedIds (citation count) to StructureCandidates
     * @param topK        retrieve only the top k StructureCandidates
     * @return FormulaResultContainers of this compound/feature with specified extensions.
     */
    @GetMapping(value = "/formulas/{formulaId}/structures", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<StructureCandidate> getStructureCandidates(@PathVariable String projectId, @PathVariable String compoundId, @PathVariable String formulaId,
                                                           @RequestParam(defaultValue = "false") boolean fingerprint,
                                                           @RequestParam(defaultValue = "false") boolean dbLinks,
                                                           @RequestParam(defaultValue = "false") boolean pubMedIds,

                                                           @RequestParam(defaultValue = "-1") int topK) {
        List<Class<? extends DataAnnotation>> para = (fingerprint ? List.of(FormulaScoring.class, FBCandidates.class, FBCandidateFingerprints.class) : List.of(FormulaScoring.class, FBCandidates.class));
        Instance instance = loadInstance(projectId, compoundId);
        FormulaResultId fidObj = parseFID(instance, formulaId);
        fidObj.setAnnotation(FBCandidateNumber.class, topK <= 0 ? FBCandidateNumber.ALL : new FBCandidateNumber(topK));
        FormulaResult fr = instance.loadFormulaResult(fidObj, (Class<? extends DataAnnotation>[]) para.toArray(Class[]::new)).orElseThrow();
        return fr.getAnnotation(FBCandidates.class).map(FBCandidates::getResults).map(l -> {
            List<StructureCandidate> candidates = new ArrayList();
            Iterator<Scored<CompoundCandidate>> it = l.iterator();

            if (fingerprint) {
                Iterator<Fingerprint> fps = fr.getAnnotationOrThrow(FBCandidateFingerprints.class).getFingerprints().iterator();

                if (it.hasNext())//tophit
                    candidates.add(StructureCandidate.of(it.next(), fps.next(),
                            fr.getAnnotationOrThrow(FormulaScoring.class), dbLinks, pubMedIds));

                while (it.hasNext())
                    candidates.add(StructureCandidate.of(it.next(), fps.next(),
                            null, dbLinks, pubMedIds));
            } else {
                if (it.hasNext())//tophit
                    candidates.add(StructureCandidate.of(it.next(), null,
                            fr.getAnnotationOrThrow(FormulaScoring.class), dbLinks, pubMedIds));

                while (it.hasNext())
                    candidates.add(StructureCandidate.of(it.next(), null,
                            null, dbLinks, pubMedIds));
            }
            return candidates;
        }).orElse(null);
    }


    //todo add order by parameter?

    /**
     * Best Scoring StructureCandidate over all molecular formular resutls that belong to the specified
     * compound/feature (compoundId).
     *
     * @param projectId   project-space to read from.
     * @param compoundId  compound/feature the formula result belongs to.
     * @param fingerprint add molecular fingerprint to StructureCandidates
     * @param dbLinks     add dbLinks to StructureCandidates
     * @param pubMedIds   add PubMedIds (citation count) to StructureCandidates
     * @return Best scoring FormulaResultContainers of this compound/feature with specified extensions.
     */
    @GetMapping(value = "/top-structure", produces = MediaType.APPLICATION_JSON_VALUE)
    public StructureCandidate getTopStructureCandidate(@PathVariable String projectId, @PathVariable String compoundId, @RequestParam(defaultValue = "false") boolean fingerprint, @RequestParam(defaultValue = "false") boolean dbLinks, @RequestParam(defaultValue = "false") boolean pubMedIds) {

        List<Class<? extends DataAnnotation>> para = (fingerprint ? List.of(FormulaScoring.class, FBCandidates.class, FBCandidateFingerprints.class) : List.of(FormulaScoring.class, FBCandidates.class));
        Instance instance = loadInstance(projectId, compoundId);

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
    }

    @Hidden
    @GetMapping(value = "/formulas/{formulaId}/sirius-tree", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getFTree(@PathVariable String projectId, @PathVariable String compoundId, @PathVariable String formulaId) {
        Instance instance = loadInstance(projectId, compoundId);
        final FTJsonWriter ftWriter = new FTJsonWriter();
        return instance.loadFormulaResult(parseFID(instance, formulaId), FTree.class).flatMap(fr -> fr.getAnnotation(FTree.class)).map(ftWriter::treeToJsonString).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FragmentationTree for '" + idString(projectId, compoundId, formulaId) + "' not found!"));
    }

    /**
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier
     * This tree is used to rank formula candidates (treeScore).
     *
     * @param projectId  project-space to read from.
     * @param compoundId compound/feature the formula result belongs to.
     * @param formulaId  identifier of the requested formula result
     * @return Fragmentation Tree
     */
    @GetMapping(value = "/formulas/{formulaId}/tree", produces = MediaType.APPLICATION_JSON_VALUE)
    public FragmentationTree getFragTree(@PathVariable String projectId, @PathVariable String compoundId, @PathVariable String formulaId) {
        Instance instance = loadInstance(projectId, compoundId);
        return instance.loadFormulaResult(parseFID(instance, formulaId), FTree.class).flatMap(fr -> fr.getAnnotation(FTree.class)).map(FragmentationTree::fromFtree).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FragmentationTree for '" + idString(projectId, compoundId, formulaId) + "' not found!"));
    }

    /**
     * Returns simulated isotope pattern (SIRIUS) for the given formula result identifier.
     * This simulated isotope pattern is used to rank formula candidates (treeScore).
     *
     * @param projectId  project-space to read from.
     * @param compoundId compound/feature the formula result belongs to.
     * @param formulaId  identifier of the requested formula result
     * @return Simulated isotope pattern
     */
    @GetMapping(value = "/formulas/{formulaId}/isotope-pattern", produces = MediaType.APPLICATION_JSON_VALUE)
    public AnnotatedSpectrum getSimulatedIsotopePattern(@PathVariable String projectId, @PathVariable String compoundId, @PathVariable String formulaId) {
        Instance instance = loadInstance(projectId, compoundId);
        Sirius sirius = ApplicationCore.SIRIUS_PROVIDER.sirius(instance.loadCompoundContainer(ProjectSpaceConfig.class).getAnnotationOrThrow(ProjectSpaceConfig.class).config.getConfigValue("AlgorithmProfile"));
        Optional<FormulaResult> fResult = instance.loadFormulaResult(parseFID(instance, formulaId), FTree.class);
        return fResult.map(FormulaResult::getId).map(id -> sirius.simulateIsotopePattern(
                        id.getMolecularFormula(), id.getIonType().getIonization())).map(AnnotatedSpectrum::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Isotope Pattern for '" + idString(projectId, compoundId, formulaId) + "' not found!"));
    }

    /**
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier
     * This fingerprint is used to perfom structure database search and predict compound classes.
     *
     * @param projectId  project-space to read from.
     * @param compoundId compound/feature the formula result belongs to.
     * @param formulaId  identifier of the requested formula result
     * @return probabilistic fingerprint predicted by CSI:FingerID
     */
    @GetMapping(value = "/formulas/{formulaId}/fingerprint", produces = MediaType.APPLICATION_JSON_VALUE)
    public double[] getFingerprintPrediction(@PathVariable String projectId, @PathVariable String compoundId, @PathVariable String formulaId) {
        Instance instance = loadInstance(projectId, compoundId);
        Optional<FormulaResult> fResult = instance.loadFormulaResult(parseFID(instance, formulaId), FingerprintResult.class);
        return fResult.flatMap(fr -> fr.getAnnotation(FingerprintResult.class).map(fpResult -> fpResult.fingerprint.toProbabilityArray()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fingerprint for '" + idString(projectId, compoundId, formulaId) + "' not found!"));
    }

    /**
     * All predicted compound classes (CANOPUS) from ClassyFire and NPC and their probabilities,
     *
     * @param projectId  project-space to read from.
     * @param compoundId compound/feature the formula result belongs to.
     * @param formulaId  identifier of the requested formula result
     * @return Predicted compound classes
     */
    @GetMapping(value = "/formulas/{formulaId}/canopus-predictions", produces = MediaType.APPLICATION_JSON_VALUE)
    public CanopusPredictions getCanopusPredictions(@PathVariable String projectId, @PathVariable String compoundId, @PathVariable String formulaId) {
        Instance instance = loadInstance(projectId, compoundId);
        Optional<FormulaResult> fResult = instance.loadFormulaResult(parseFID(instance, formulaId), CanopusResult.class);
        return fResult.flatMap(fr -> fr.getAnnotation(CanopusResult.class).map(CanopusPredictions::of))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compound Classes for '" + idString(projectId, compoundId, formulaId) + "' not found!"));
    }

    /**
     * Best matching compound classes,
     * Set of the highest scoring compound classes CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology,
     *
     * @param projectId  project-space to read from.
     * @param compoundId compound/feature the formula result belongs to.
     * @param formulaId  identifier of the requested formula result
     * @return Best matching Predicted compound classes
     */
    @GetMapping(value = "/formulas/{formulaId}/best-canopus-predictions", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompoundClasses getBestMatchingCanopusPredictions(@PathVariable String projectId, @PathVariable String compoundId, @PathVariable String formulaId) {
        Instance instance = loadInstance(projectId, compoundId);
        Optional<FormulaResult> fResult = instance.loadFormulaResult(parseFID(instance, formulaId), CanopusResult.class);
        return fResult.flatMap(fr -> fr.getAnnotation(CanopusResult.class).map(CompoundClasses::of))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compound Classes for '" + idString(projectId, compoundId, formulaId) + "' not found!"));
    }
}

