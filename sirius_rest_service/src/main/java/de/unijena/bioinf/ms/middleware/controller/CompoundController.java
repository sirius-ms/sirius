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

package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.ms.middleware.configuration.GlobalConfig;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.TraceSet;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils.removeNone;

@RestController
@RequestMapping(value = "/api/projects/{projectId}/compounds")
@Tag(name = "Compounds", description = "This compound based API allows to retrieve all AlignedFeatures that belong to the same "
        + "compound (also known as a group of ion identities). It also provides for each AlignedFeature the "
        + "corresponding annotation results (which are usually computed on a per-feature basis)")
public class CompoundController {


    private final ProjectsProvider<?> projectsProvider;
    private final GlobalConfig globalConfig;

    @Autowired
    public CompoundController(ProjectsProvider<?> projectsProvider, GlobalConfig globalConfig) {
        this.projectsProvider = projectsProvider;
        this.globalConfig = globalConfig;
    }

    /**
     * Page of available compounds (group of ion identities) in the given project-space.
     *
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     * @return Compounds with additional optional fields (if specified).
     */

    @GetMapping(value = "/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<Compound> getCompoundsPaged(@PathVariable String projectId, @ParameterObject Pageable pageable,
                                       @RequestParam(defaultValue = "") EnumSet<Compound.OptField> optFields,
                                       @RequestParam(defaultValue = "") EnumSet<AlignedFeature.OptField> optFieldsFeatures) {
        return projectsProvider.getProjectOrThrow(projectId).findCompounds(pageable, removeNone(optFields), removeNone(optFieldsFeatures));
    }

    /**
     * List of all available compounds (group of ion identities) in the given project-space.
     *
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     * @return Compounds with additional optional fields (if specified).
     */

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Compound> getCompounds(@PathVariable String projectId,
                                       @RequestParam(defaultValue = "") EnumSet<Compound.OptField> optFields,
                                       @RequestParam(defaultValue = "") EnumSet<AlignedFeature.OptField> optFieldsFeatures) {
        return getCompoundsPaged(projectId, globalConfig.unpaged(), optFields, optFieldsFeatures)
                .stream().toList();
    }

    /**
     * Import Compounds and its contained features. Compounds and Features must not exist in the project.
     * Otherwise, they will exist twice.
     * @param projectId project-space to import into.
     * @param compounds the compound data to be imported
     * @param optFields set of optional fields to be included. Use 'none' to override defaults.
     * @param optFieldsFeatures set of optional fields of the nested features to be included. Use 'none' to override defaults.
     * @return the Compounds that have been imported with specified optional fields
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Compound> addCompounds(@PathVariable String projectId, @Valid @RequestBody List<CompoundImport> compounds,
                                       @RequestParam(defaultValue = "") EnumSet<Compound.OptField> optFields,
                                       @RequestParam(defaultValue = "") EnumSet<AlignedFeature.OptField> optFieldsFeatures
    ) {
        return projectsProvider.getProjectOrThrow(projectId).addCompounds(compounds, removeNone(optFields), removeNone(optFieldsFeatures));
    }

    /**
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     *
     * @param projectId  project-space to read from.
     * @param compoundId identifier of the compound (group of ion identities) to access.
     * @param optFields  set of optional fields to be included. Use 'none' only to override defaults.
     * @return Compounds with additional optional fields (if specified).
     */
    @GetMapping(value = "/{compoundId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Compound getCompound(@PathVariable String projectId, @PathVariable String compoundId,
                                @RequestParam(required = false, defaultValue = "") EnumSet<Compound.OptField> optFields,
                                @RequestParam(required = false, defaultValue = "") EnumSet<AlignedFeature.OptField> optFieldsFeatures) {
        return projectsProvider.getProjectOrThrow(projectId).findCompoundById(compoundId, removeNone(optFields), removeNone(optFieldsFeatures));
    }

    @GetMapping(value = "/{compoundId}/traces", produces = MediaType.APPLICATION_JSON_VALUE)
    public TraceSet getTraces(@PathVariable String projectId, @PathVariable String compoundId) {
        Optional<TraceSet> traceSet = projectsProvider.getProjectOrThrow(projectId).getTraceSetForCompound(compoundId);
        if (traceSet.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No trace information available for project id = " + projectId + " and compound id = " + compoundId );
        else return traceSet.get();
    }


    /**
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the
     * specified project-space.
     *
     * @param projectId  project-space to delete from.
     * @param compoundId identifier of the compound to delete.
     */
    @DeleteMapping(value = "/{compoundId}")
    public void deleteCompound(@PathVariable String projectId, @PathVariable String compoundId) {
        projectsProvider.getProjectOrThrow(projectId).deleteCompoundById(compoundId);
    }
}
