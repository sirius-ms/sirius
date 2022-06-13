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

package de.unijena.bioinf.ms.middleware.compounds;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.SpectrumFileSource;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.ms.middleware.BaseApiController;
import de.unijena.bioinf.ms.middleware.SiriusContext;
import de.unijena.bioinf.ms.middleware.compounds.model.CompoundAnnotation;
import de.unijena.bioinf.ms.middleware.compounds.model.CompoundId;
import de.unijena.bioinf.ms.middleware.compounds.model.MsData;
import de.unijena.bioinf.ms.middleware.formulas.model.CompoundClasses;
import de.unijena.bioinf.ms.middleware.formulas.model.FormulaCandidate;
import de.unijena.bioinf.ms.middleware.formulas.model.StructureCandidate;
import de.unijena.bioinf.ms.middleware.spectrum.AnnotatedSpectrum;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.fingerid.FBCandidateNumber;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/projects/{projectId}")
@Tag(name = "Compounds", description = "Access compounds (aka features) of a specified project-space.")
public class CompoundController extends BaseApiController {

    @Autowired
    public CompoundController(SiriusContext context) {
        super(context);
    }


    /**
     * Get all available compounds/features in the given project-space.
     *
     * @param projectId     project-space to read from.
     * @param topAnnotation include the top annotation of this feature into the output (if available).
     * @param msData        include corresponding source data (MS and MS/MS) into the output.
     * @return CompoundIds with additional annotations and MS/MS data (if specified).
     */
    @GetMapping(value = "/compounds", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CompoundId> getCompounds(@PathVariable String projectId, @RequestParam(required = false) boolean topAnnotation, @RequestParam(required = false) boolean msData) {
        LoggerFactory.getLogger(CompoundController.class).info("Started collecting compounds...");
        final ProjectSpaceManager space = projectSpace(projectId);

        final ArrayList<CompoundId> compoundIds = new ArrayList<>();
        space.projectSpace().forEach(ccid -> compoundIds.add(asCompoundId(ccid, space, topAnnotation, msData)));

        LoggerFactory.getLogger(CompoundController.class).info("Finished parsing compounds...");
        return compoundIds;
    }

    /**
     * Import ms/ms data from the given format into the specified project-space
     * Possible formats (ms, mgf, cef, msp, mzML, mzXML)
     *
     * @param projectId  project-space to import into.
     * @param format     data format specified by the usual file extension of the format (without [.])
     * @param sourceName name that specifies the data source. Can e.g. be a file path or just a name.
     * @param body       data content in specified format
     * @return CompoundIds of the imported compounds/features.
     */
    @PostMapping(value = "/compounds", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<CompoundId> importCompounds(@PathVariable String projectId, @RequestParam String format, @RequestParam(required = false) String sourceName, @RequestBody MultipartFile body) throws IOException {
        List<CompoundId> ids = new ArrayList<>();
        final ProjectSpaceManager space = projectSpace(projectId);
        GenericParser<Ms2Experiment> parser = new MsExperimentParser().getParserByExt(format.toLowerCase());
        try (InputStream bodyStream = body.getInputStream()) {
            try (CloseableIterator<Ms2Experiment> it = parser.parseIterator(bodyStream, null)) {
                while (it.hasNext()) {
                    Ms2Experiment next = it.next();
                    if (sourceName != null)     //todo import handling needs to be improved ->  this naming hassle is ugly
                        next.setAnnotation(SpectrumFileSource.class,
                                new SpectrumFileSource(
                                        new File("./" + (sourceName.endsWith(format) ? sourceName : sourceName + "." + format.toLowerCase())).toURI()));

                    @NotNull Instance inst = space.newCompoundWithUniqueId(next);
                    ids.add(CompoundId.of(inst.getID()));
                }
            }
            return ids;
        }
    }

    /**
     * Get compound/feature with the given identifier from the specified project-space.
     *
     * @param projectId     project-space to read from.
     * @param cid           identifier of compound to access.
     * @param topAnnotation include the top annotation of this feature into the output (if available).
     * @param msData        include corresponding source data (MS and MS/MS) into the output.
     * @return CompoundId with additional annotations and MS/MS data (if specified).
     */
    @GetMapping(value = "/compounds/{cid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompoundId getCompound(@PathVariable String projectId, @PathVariable String cid,
                                  @RequestParam(required = false, defaultValue = "false") boolean topAnnotation,
                                  @RequestParam(required = false, defaultValue = "false") boolean msData) {
        final ProjectSpaceManager space = projectSpace(projectId);
        final CompoundContainerId ccid = parseCID(space, cid);
        return asCompoundId(ccid, space, topAnnotation, msData);
    }

    /**
     * Delete compound/feature with the given identifier from the specified project-space.
     *
     * @param projectId     project-space to delete from.
     * @param cid           identifier of compound to delete.
     */
    @DeleteMapping(value = "/compounds/{cid}")
    public void deleteCompound(@PathVariable String projectId, @PathVariable String cid) throws IOException {
        final ProjectSpaceManager space = projectSpace(projectId);
        CompoundContainerId compound = space.projectSpace().findCompound(cid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "Compound with id '" + cid + "' does not exist in '" + projectId + "'."));
        space.projectSpace().deleteCompound(compound);
    }


    private CompoundAnnotation asCompoundSummary(Instance inst) {
        return inst.loadTopFormulaResult(List.of(TopCSIScore.class)).map(de.unijena.bioinf.projectspace.FormulaResult::getId).flatMap(frid -> {
            frid.setAnnotation(FBCandidateNumber.class, new FBCandidateNumber(1));
            return inst.loadFormulaResult(frid, FormulaScoring.class, FTree.class, FBCandidates.class, CanopusResult.class)
                    .map(topHit -> {
                        final CompoundAnnotation cSum = new CompoundAnnotation();
//
                        //add formula summary
                        cSum.setFormulaAnnotation(FormulaCandidate.of(topHit));

                        // fingerid result
                        topHit.getAnnotation(FBCandidates.class).map(FBCandidates::getResults)
                                .filter(l -> !l.isEmpty()).map(r -> r.get(0)).map(s ->
                                        StructureCandidate.of(s, topHit.getAnnotationOrThrow(FormulaScoring.class),
                                                true, true))
                                .ifPresent(cSum::setStructureAnnotation);

                        topHit.getAnnotation(CanopusResult.class).map(CompoundClasses::of).
                                ifPresent(cSum::setCompoundClassAnnotation);
                        return cSum;

                    });
        }).orElse(null);
    }

    private MsData asCompoundMsData(Instance instance) {
        return instance.loadCompoundContainer(Ms2Experiment.class)
                .getAnnotation(Ms2Experiment.class).map(exp -> new MsData(
                        opt(exp.getMergedMs1Spectrum(), this::asSpectrum).orElse(null),
                        null,
                        exp.getMs1Spectra().stream().map(this::asSpectrum).collect(Collectors.toList()),
                        exp.getMs2Spectra().stream().map(this::asSpectrum).collect(Collectors.toList()))).orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Compound with ID '" + instance + "' has no input Data!"));
    }

    private CompoundId asCompoundId(CompoundContainerId cid, ProjectSpaceManager ps, boolean includeSummary, boolean includeMsData) {
        final CompoundId compoundId = CompoundId.of(cid);
        if (includeSummary || includeMsData) {
            Instance instance = ps.newInstanceFromCompound(cid);
            if (includeSummary)
                compoundId.setTopAnnotation(asCompoundSummary(instance));
            if (includeMsData)
                compoundId.setMsData(asCompoundMsData(instance));
        }
        return compoundId;
    }

    private <S, T> Optional<T> opt(S input, Function<S, T> convert) {
        return Optional.ofNullable(input).map(convert);
    }

    private AnnotatedSpectrum asSpectrum(Spectrum<Peak> spec) {
        return new AnnotatedSpectrum(Spectrums.copyMasses(spec), Spectrums.copyIntensities(spec), new HashMap<>());
    }
}

