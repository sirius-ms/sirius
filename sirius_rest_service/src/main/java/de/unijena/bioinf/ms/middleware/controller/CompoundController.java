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
import de.unijena.bioinf.ms.middleware.controller.mixins.TaggableController;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.compute.InstrumentProfile;
import de.unijena.bioinf.ms.middleware.model.events.ServerEvents;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.QuantRowType;
import de.unijena.bioinf.ms.middleware.model.features.QuantTable;
import de.unijena.bioinf.ms.middleware.model.features.TraceSet;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
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
public class CompoundController implements TaggableController<Compound, Compound.OptField> {

    @Getter
    private final ProjectsProvider<?> projectsProvider;
    private final GlobalConfig globalConfig;
    private final EventService<?> eventService;

    @Autowired
    public CompoundController(ProjectsProvider<?> projectsProvider, GlobalConfig globalConfig, EventService<?> eventService) {
        this.projectsProvider = projectsProvider;
        this.globalConfig = globalConfig;
        this.eventService = eventService;
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
                                       @RequestParam(defaultValue = "none") EnumSet<Compound.OptField> optFields,
                                       @RequestParam(defaultValue = "none") EnumSet<AlignedFeature.OptField> optFieldsFeatures) {
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
                                       @RequestParam(defaultValue = "none") EnumSet<Compound.OptField> optFields,
                                       @RequestParam(defaultValue = "none") EnumSet<AlignedFeature.OptField> optFieldsFeatures) {
        return getCompoundsPaged(projectId, globalConfig.unpaged(), optFields, optFieldsFeatures)
                .stream().toList();
    }

    /**
     * Import Compounds and its contained features. Compounds and Features must not exist in the project.
     * Otherwise, they will exist twice.
     * @param projectId project-space to import into.
     * @param compounds the compound data to be imported
     * @param profile profile describing the instrument used to measure the data. Used to merge spectra.
     * @param optFields set of optional fields to be included. Use 'none' to override defaults.
     * @param optFieldsFeatures set of optional fields of the nested features to be included. Use 'none' to override defaults.
     * @return the Compounds that have been imported with specified optional fields
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Compound> addCompounds(@PathVariable String projectId, @Valid @RequestBody List<CompoundImport> compounds,
                                       @RequestParam(required = false) InstrumentProfile profile,
                                       @RequestParam(defaultValue = "none") EnumSet<Compound.OptField> optFields,
                                       @RequestParam(defaultValue = "none") EnumSet<AlignedFeature.OptField> optFieldsFeatures
    ) {
        List<Compound> importedCompounds = projectsProvider.getProjectOrThrow(projectId).addCompounds(compounds, profile, removeNone(optFields), removeNone(optFieldsFeatures));

        // Prepare and Send SSE Event
        List<String> fids = importedCompounds.stream().flatMap(c -> c.getFeatures().stream()).map(AlignedFeature::getAlignedFeatureId).toList();
        List<String> cids = importedCompounds.stream().map(Compound::getCompoundId).distinct().toList();
        eventService.sendEvent(ServerEvents.newImportEvent(cids, fids, projectId));

        return importedCompounds;
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
                                @RequestParam(required = false, defaultValue = "none") EnumSet<Compound.OptField> optFields,
                                @RequestParam(required = false, defaultValue = "none") EnumSet<AlignedFeature.OptField> optFieldsFeatures) {
        return projectsProvider.getProjectOrThrow(projectId).findCompoundById(compoundId, removeNone(optFields), removeNone(optFieldsFeatures));
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

    /**
     * [EXPERIMENTAL] Returns a single quantification table row for the given compound.
     * <p>
     * The quantification table contains a quantification of the feature within all
     * samples it is contained in.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
     * 
     * @param projectId project-space to read from.
     * @param compoundId compound which should be read out
     * @param type quantification type.
     * @return
     */
    @Operation(operationId = "getCompoundQuantTableRowExperimental")
    @GetMapping(value = "/{compoundId}/quant-table-row", produces = MediaType.APPLICATION_JSON_VALUE)
    public QuantTable getQuantTableRow(@PathVariable String projectId, @PathVariable String compoundId, @RequestParam(defaultValue = "APEX_HEIGHT") QuantMeasure type) {
        Optional<QuantTable> quantificationForAlignedFeature = projectsProvider.getProjectOrThrow(projectId).getQuantificationForAlignedFeatureOrCompound(compoundId, type, QuantRowType.COMPOUNDS);
        if (quantificationForAlignedFeature.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No quantification information available for " + idString(projectId, compoundId) + " and quantification type " + type );
        else return quantificationForAlignedFeature.get();
    }

    /**
     * [EXPERIMENTAL] Returns the full quantification table of compounds.
     * <p>
     * The quantification table contains a quantification of the features within all
     * runs they are contained in.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
     *
     * @param projectId project-space to read from.
     * @param type quantification type.
     * @return
     */
    @Operation(operationId = "getCompoundQuantTableExperimental")
    @GetMapping(value = "/quant-table", produces = MediaType.APPLICATION_JSON_VALUE)
    public QuantTable getQuantTable(@PathVariable String projectId, @RequestParam(defaultValue = "APEX_HEIGHT") QuantMeasure type) {
        Optional<QuantTable> quantificationForAlignedFeature = projectsProvider.getProjectOrThrow(projectId).getQuantification(type, QuantRowType.COMPOUNDS);
        if (quantificationForAlignedFeature.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No quantification information available for " + projectId + " and quantification type " + type );
        else return quantificationForAlignedFeature.get();
    }

    /**
     * [EXPERIMENTAL] Returns the traces of the given compound.
     * <p>
     * A trace consists of m/z and intensity values over the retention
     * time axis. All the returned traces are 'projected', which means they refer not to the original retention time axis,
     * but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.
     * However, this also means that all traces can be directly compared against each other, as they all lie in the same
     * retention time axis.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
     * @param projectId project-space to read from.
     * @param compoundId compound which intensities should be read out
     * @return Traces of the given compound.
     */
    @Operation(operationId = "getCompoundTracesExperimental")
    @GetMapping(value = "/{compoundId}/traces", produces = MediaType.APPLICATION_JSON_VALUE)
    public TraceSet getCompoundTraces(@PathVariable String projectId, @PathVariable String compoundId, @RequestParam(required = false, defaultValue = "") String featureId) {
        Optional<TraceSet> traceSet = projectsProvider.getProjectOrThrow(projectId).getTraceSetForCompound(compoundId, featureId.isBlank() ? Optional.empty() : Optional.of(featureId));
        if (traceSet.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No trace information available for project id = " + projectId + " and compound id = " + compoundId );
        else return traceSet.get();
    }

    /**
     *
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag.
     *
     * <h2>Supported filter syntax</h2>
     *
     * <p>The filter string must contain one or more clauses. A clause is prefíxed
     * by a field name. Possible field names are:</p>
     *
     * <ul>
     *   <li><strong>tagName</strong> - name of the tag</li>
     *   <li><strong>bool</strong>, <strong>integer</strong>, <strong>real</strong>, <strong>text</strong>, <strong>date</strong>, or <strong>time</strong> - tag value</li>
     * </ul>
     *
     * <p>The format of the <strong>date</strong> type is {@code yyyy-MM-dd} and of the <strong>time</strong> type is {@code HH\:mm\:ss}.</p>
     *
     * <p>A clause may be:</p>
     * <ul>
     *     <li>a <strong>term</strong>: field name followed by a colon and the search term, e.g. {@code tagName:my_name}</li>
     *     <li>a <strong>phrase</strong>: field name followed by a colon and the search phrase in doublequotes, e.g. {@code text:"new york"}</li>
     *     <li>a <strong>regular expression</strong>: field name followed by a colon and the regex in slashes, e.g. {@code text:/[mb]oat/}</li>
     *     <li>a <strong>comparison</strong>: field name followed by a comparison operator and a value, e.g. {@code integer<3}</li>
     *     <li>a <strong>range</strong>: field name followed by a colon and an open (indiced by {@code [ } and {@code ] }) or (semi-)closed range (indiced by <code>{</code> and <code>}</code>), e.g. {@code integer:[* TO 3] }</li>
     * </ul>
     *
     * <p>Clauses may be <strong>grouped</strong> with brackets {@code ( } and {@code ) } and / or <strong>joined</strong> with {@code AND} or {@code OR } (or {@code && } and {@code || })</p>
     *
     * <h3>Example</h3>
     *
     * <p>The syntax allows to build complex filter queries such as:</p>
     *
     * <p>{@code (tagName:hello || tagName:world) && text:"new york" AND text:/[mb]oat/ AND integer:[1 TO *] OR real<=3 OR date:2024-01-01 OR date:[2023-10-01 TO 2023-12-24] OR date<2022-01-01 OR time:12\:00\:00 OR time:[12\:00\:00 TO 14\:00\:00] OR time<10\:00\:00 }</p>
     *
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId    project space to get compounds (group of ion identities) from.
     * @param filter       tag filter.
     * @param pageable     pageable.
     * @param optFields    set of optional fields to be included. Use 'none' only to override defaults.
     * @return tagged compounds (group of ion identities)
     */
    @Operation(operationId = "getCompoundsByTagExperimental")
    @Override
    public Page<Compound> getObjectsByTag(String projectId, String filter, Pageable pageable, EnumSet<Compound.OptField> optFields) {
        return TaggableController.super.getObjectsByTag(projectId, filter, pageable, optFields);
    }

    /**
     *
     * [EXPERIMENTAL] Tags with the same name will be overwritten.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId  project-space to add to.
     * @param compoundId compound (group of ion identities) to add tags to.
     * @param tags       tags to add.
     * @return the tags that have been added
     */
    @Operation(operationId = "addTagsToCompoundExperimental")
    @PutMapping(value = "/tags/{compoundId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public List<? extends de.unijena.bioinf.ms.middleware.model.tags.Tag> addTags(String projectId, String compoundId, List<? extends de.unijena.bioinf.ms.middleware.model.tags.Tag> tags) {
        return TaggableController.super.addTags(projectId, compoundId, tags);
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId     project-space to delete from.
     * @param compoundId    compound (group of ion identities) to delete tag from.
     * @param tagName  name of the tag to delete.
     */
    @Operation(operationId = "removeTagFromCompoundExperimental")
    @DeleteMapping(value = "/tags/{compoundId}/{tagName}")
    @Override
    public void removeTags(String projectId, String compoundId, String tagName) {
        TaggableController.super.removeTags(projectId, compoundId, tagName);
    }

    /**
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag group.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId project-space to delete from.
     * @param group     tag group name.
     * @param pageable  pageable.
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     * @return tagged compounds (group of ion identities)
     */
    @Operation(operationId = "getCompoundsByGroupExperimental")
    @Override
    public Page<Compound> getObjectsByGroup(String projectId, String group, Pageable pageable, EnumSet<Compound.OptField> optFields) {
        return TaggableController.super.getObjectsByGroup(projectId, group, pageable, optFields);
    }

    @Override
    public Class<Compound> getTagTarget() {
        return Compound.class;
    }

    //region helpers
    protected static String idString(String pid, String cid) {
        return "'" + pid + "/" + cid + "'";
    }
    //endregion

}
