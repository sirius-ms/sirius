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

import com.fasterxml.jackson.core.JsonProcessingException;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.ms.middleware.BaseApiController;
import de.unijena.bioinf.ms.middleware.SiriusContext;
import de.unijena.bioinf.ms.middleware.formulas.model.FormulaId;
import de.unijena.bioinf.ms.middleware.formulas.model.FormulaResultScores;
import de.unijena.bioinf.ms.middleware.formulas.model.FragTree;
import de.unijena.bioinf.projectspace.FormulaResult;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.fingerid.FBCandidateNumber;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/projects/{pid}/compounds/{cid}")
@Tag(name = "Formulas Results", description = "Access results for all formula candidates of a given compound (aka feature).")
public class FormulaResultController extends BaseApiController {

    @Autowired
    public FormulaResultController(SiriusContext context) {
        super(context);
    }

    //todo add order by parameter?
    @GetMapping(value = "/formulas", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FormulaId> getFormulaIds(@PathVariable String pid, @PathVariable String cid, @RequestParam(required = false) boolean includeFormulaScores) {
        LoggerFactory.getLogger(FormulaResultController.class).info("Started collecting formulas...");
        Instance instance = loadInstance(pid, cid);
        return instance.loadFormulaResults().stream().map(SScored::getCandidate).map(fr -> {
            FormulaId formulaId = new FormulaId(fr.getId());
            if (includeFormulaScores)
                fr.getAnnotation(FormulaScoring.class).ifPresent(fs -> formulaId.setResultScores(new FormulaResultScores(fs)));
            return formulaId;
        }).collect(Collectors.toList());
    }

    @GetMapping(value = "/formulas/{fid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public FormulaId getFormulaResult(@PathVariable String pid, @PathVariable String cid, @PathVariable String fid) {
        Instance instance = loadInstance(pid, cid);
        return instance.loadFormulaResult(parseFID(instance, fid), FormulaScoring.class)
                .map(fr -> {
                    FormulaId formulaId = new FormulaId(fr.getId());
                    fr.getAnnotation(FormulaScoring.class).ifPresent(fs -> formulaId.setResultScores(new FormulaResultScores(fs)));
                    return formulaId;
                }).orElse(null);
    }

    @GetMapping(value = "/formulas/{fid}/scores", produces = MediaType.APPLICATION_JSON_VALUE)
    public FormulaResultScores getFormulaResultScores(@PathVariable String pid, @PathVariable String cid, @PathVariable String fid) {
        Instance instance = loadInstance(pid, cid);
        return instance.loadFormulaResult(parseFID(instance, fid), FormulaScoring.class)
                .flatMap(fr -> fr.getAnnotation(FormulaScoring.class).map(FormulaResultScores::new))
                .orElseThrow();
    }

    @GetMapping(value = "/formulas/{fid}/candidates", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CompoundCandidate> getStructureCandidates(@PathVariable String pid, @PathVariable String cid, @PathVariable String fid, @RequestParam(defaultValue = "-1") int topK) {
        Instance instance = loadInstance(pid, cid);
        FormulaResultId fidObj = parseFID(instance, fid);
        fidObj.setAnnotation(FBCandidateNumber.class, topK <= 0 ? FBCandidateNumber.ALL : new FBCandidateNumber(topK));
        return instance.loadFormulaResult(fidObj, FBCandidates.class)
                .map(fr -> fr.getAnnotation(FBCandidates.class).map(FBCandidates::getResults)
                        .map(List::stream).map(s -> s.map(SScored::getCandidate).collect(Collectors.toList()))
                        .orElse(Collections.emptyList())).orElse(null);
    }

    //todo add order by parameter?
    @GetMapping(value = "/top-candidate", produces = MediaType.APPLICATION_JSON_VALUE)
    public Scored<CompoundCandidate> getTopHitCandidate(@PathVariable String pid, @PathVariable String cid) throws JsonProcessingException {
        Instance instance = loadInstance(pid, cid);
        return instance.loadTopFormulaResult().flatMap(fr -> {
            fr.getId().setAnnotation(FBCandidateNumber.class, new FBCandidateNumber(1));
            return instance.loadFormulaResult(fr.getId(), FBCandidates.class).flatMap(fr2 -> fr2.getAnnotation(FBCandidates.class))
                    .map(FBCandidates::getResults).map(r -> r.get(0));
        }).orElseThrow();
    }

    @GetMapping(value = "formulas/{fid}/sirius-tree", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getFTree(@PathVariable String pid, @PathVariable String cid, @PathVariable String fid) {
        Instance instance = loadInstance(pid, cid);
        final FTJsonWriter ftWriter = new FTJsonWriter();
        return instance.loadFormulaResult(parseFID(instance, fid), FTree.class).flatMap(fr -> fr.getAnnotation(FTree.class))
                .map(ftWriter::treeToJsonString)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find FTree for '" + idString(pid, cid, fid) + "' not found!"));

    }

    @GetMapping(value = "formulas/{fid}/tree", produces = MediaType.APPLICATION_JSON_VALUE)
    public FragTree getFragTree(@PathVariable String pid, @PathVariable String cid, @PathVariable String fid) {
        Instance instance = loadInstance(pid, cid);
        return instance.loadFormulaResult(parseFID(instance, fid), FTree.class)
                .flatMap(fr -> fr.getAnnotation(FTree.class)).map(FragTree::fromFtree)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find FTree for '" + idString(pid, cid, fid) + "' not found!"));
    }

    @GetMapping(value = "formulas/{fid}/fingerprint", produces = MediaType.APPLICATION_JSON_VALUE)
    public double[] getFingerprint(@PathVariable String pid, @PathVariable String cid, @PathVariable String fid, @RequestParam(required = false) boolean asDeterministic) {
        Instance instance = loadInstance(pid, cid);
        Optional<FormulaResult> fResult = instance.loadFormulaResult(parseFID(instance, fid), FingerprintResult.class);
        return fResult.flatMap(fr -> fr.getAnnotation(FingerprintResult.class).
                map(fpResult -> {
                    if (asDeterministic) {
                        boolean[] boolFingerprint = fpResult.fingerprint.asDeterministic().toBooleanArray();
                        double[] fingerprint = new double[boolFingerprint.length];
                        for (int idx = 0; idx < fingerprint.length; idx++) {
                            fingerprint[idx] = boolFingerprint[idx] ? 1 : 0;
                        }
                        return fingerprint;
                    } else {
                        return fpResult.fingerprint.toProbabilityArray();
                    }
                })).orElse(null);
    }
}

