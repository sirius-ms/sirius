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

package de.unijena.bioinf.ms.middleware.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.SpectrumFileSource;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.middleware.BaseApiController;
import de.unijena.bioinf.ms.middleware.features.model.AlignedFeature;
import de.unijena.bioinf.ms.middleware.compute.model.ComputeContext;
import de.unijena.bioinf.ms.middleware.compute.model.JobId;
import de.unijena.bioinf.ms.middleware.projectspace.model.ProjectSpaceId;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/projects")
@Tag(name = "Projects", description = "Manage SIRIUS projects.")
public class ProjectSpaceController extends BaseApiController {
    //todo add access to fingerprint definitions aka molecular property names
    private final ComputeContext computeContext;

    @Autowired
    public ProjectSpaceController(ComputeContext context) {
        super(context.siriusContext);
        this.computeContext = context;
    }

    /**
     * List all opened project spaces.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<ProjectSpaceId> getProjectSpaces(@ParameterObject Pageable pageable) {
        final List<ProjectSpaceId> all = context.listAllProjectSpaces();
        return new PageImpl<>(
                all.stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).toList(), pageable, all.size()
        );
    }

    /**
     * Get project space info by its projectId.
     *
     * @param projectId unique name/identifier tof the project-space to be accessed.
     */
    @GetMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectSpaceId getProjectSpace(@PathVariable String projectId) {
        //todo add infos like size and number of compounds?
        return context.getProjectSpace(projectId).map(x -> ProjectSpaceId.of(projectId, x.projectSpace().getLocation())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no project space with name '" + projectId + "'"));
    }

    /**
     * Open an existing project-space and make it accessible via the given projectId.
     *
     * @param projectId unique name/identifier that shall be used to access the opened project-space.
     */
    @PutMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectSpaceId openProjectSpace(@PathVariable String projectId, @RequestParam String pathToProject) throws IOException {
        return context.openProjectSpace(new ProjectSpaceId(projectId, pathToProject));
    }

    /**
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     *
     * @param projectId unique name/identifier that shall be used to access the newly created project-space.
     */
    @PostMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectSpaceId createProjectSpace(@PathVariable String projectId, @RequestParam String pathToProject, @RequestParam(required = false) @Nullable String pathToSourceProject, @RequestParam(required = false, defaultValue = "true") boolean awaitImport) throws IOException {
        InputFilesOptions inputFiles = null;
        if (pathToSourceProject != null) {
            inputFiles = new InputFilesOptions();
            inputFiles.msInput = new InputFilesOptions.MsInput();
            inputFiles.msInput.setAllowMS1Only(true);
            inputFiles.msInput.setInputPath(List.of(Path.of(pathToSourceProject)));

            if (!inputFiles.msInput.isSingleProject())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported input! 'pathToSourceProject' needs to point to a valid SIRIUS project-space");

        }

        ProjectSpaceId pid = context.createProjectSpace(projectId, Path.of(pathToProject));
        ProjectSpaceManager<?> space = projectSpace(projectId);
        if (inputFiles != null) {
            JobId id = computeContext.createAndSubmitJob(space, List.of("project-space", "--keep-open"),
                    null, inputFiles, true, true, true);
            if (awaitImport) {
                try {
                    computeContext.getJob(space, id.getId()).awaitResult();
                    computeContext.deleteJob(id.getId(), false, false, false, false, true);
                } catch (ExecutionException e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error when waiting for import jobs '" + id.getId() + "'.", e);
                }
            }
        }
        return pid;
    }

    /**
     * Close project-space and remove it from application. Project-space will NOT be deleted from disk.
     *
     * @param projectId unique name/identifier of the  project-space to be closed.
     */
    @DeleteMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void closeProjectSpace(@PathVariable String projectId) throws IOException {
        context.closeProjectSpace(projectId);
    }

    /**
     * Import ms/ms data in given format from local filesystem into the specified project-space.
     * The import will run in a background job
     * Possible formats (ms, mgf, cef, msp, mzML, mzXML, project-space)
     * <p>
     *
     * @param projectId     project-space to import into.
     * @param inputPaths    List of file and directory paths to import
     * @param alignLCMSRuns If true, multiple LCMS Runs (mzML, mzXML) will be aligned during import/feature finding
     * @return JobId background job that imports given run/compounds/features.
     */
    @PostMapping(value = "/{projectId}/import/from-local-path", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public JobId importCompounds(@PathVariable String projectId,
                                 @RequestParam(required = false, defaultValue = "false") boolean alignLCMSRuns,
                                 @RequestParam(required = false, defaultValue = "true") boolean allowMs1OnlyData,
                                 @RequestParam(required = false, defaultValue = "false") boolean ignoreFormulas,
                                 @RequestBody List<String> inputPaths) throws IOException {

        InputFilesOptions inputFiles = new InputFilesOptions();
        inputFiles.msInput = new InputFilesOptions.MsInput();
        inputFiles.msInput.setAllowMS1Only(allowMs1OnlyData);
        inputFiles.msInput.setIgnoreFormula(ignoreFormulas);
        inputFiles.msInput.setInputPath(inputPaths.stream().map(Path::of).collect(Collectors.toList()));

        alignLCMSRuns = alignLCMSRuns && inputFiles.msInput.msParserfiles.keySet().stream()
                .anyMatch(p -> p.getFileName().toString().toLowerCase().endsWith("mzml")
                        || p.getFileName().toString().toLowerCase().endsWith("mzxml"));
        System.out.println("Alignment: " + alignLCMSRuns);

        return computeContext.createAndSubmitJob(projectSpace(projectId), alignLCMSRuns ? List.of("lcms-align") : List.of("project-space", "--keep-open"),
                null, inputFiles, true, true, true);
    }

    /**
     * Import ms/ms data from the given format into the specified project-space
     * Possible formats (ms, mgf, cef, msp, mzML, mzXML)
     *
     * @param projectId  project-space to import into.
     * @param format     data format specified by the usual file extension of the format (without [.])
     * @param sourceName name that specifies the data source. Can e.g. be a file path or just a name.
     * @param body       data content in specified format
     * @return CompoundIds of the imported run/compounds/feature.
     */
    @PostMapping(value = "/{projectId}/import/from-string", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.TEXT_PLAIN_VALUE)
    //Todo return compounds instead? or may ean object of created entity Ids?
    //todo maybe return background job
    public List<AlignedFeature> importCompoundsFromString(@PathVariable String projectId, @RequestParam String format, @RequestParam(required = false) String sourceName, @RequestBody String body) throws IOException {
        List<AlignedFeature> ids = new ArrayList<>();
        final ProjectSpaceManager<?> space = projectSpace(projectId);
        GenericParser<Ms2Experiment> parser = new MsExperimentParser().getParserByExt(format.toLowerCase());

        try (BufferedReader bodyStream = new BufferedReader(new StringReader(body))) {
            try (CloseableIterator<Ms2Experiment> it = parser.parseIterator(bodyStream, null)) {
                while (it.hasNext()) {
                    Ms2Experiment next = it.next();
                    if (sourceName != null)     //todo import handling needs to be improved ->  this naming hassle is ugly
                        next.setAnnotation(SpectrumFileSource.class,
                                new SpectrumFileSource(
                                        new File("./" + (sourceName.endsWith(format) ? sourceName : sourceName + "." + format.toLowerCase())).toURI()));

                    @NotNull Instance inst = space.newCompoundWithUniqueId(next);
                    ids.add(AlignedFeature.of(inst.getID()));
                }
            }
            return ids;
        }
    }

}
