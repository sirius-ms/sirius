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

import de.unijena.bioinf.ms.middleware.model.annotations.*;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.compute.InstrumentProfile;
import de.unijena.bioinf.ms.middleware.model.features.*;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import de.unijena.bioinf.ms.middleware.model.statistics.FoldChange;
import de.unijena.bioinf.ms.middleware.model.statistics.StatisticsTable;
import de.unijena.bioinf.ms.middleware.model.tags.*;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.Taggable;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectSourceFormats;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectType;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils.toEnumSet;

public interface Project<PSM extends ProjectSpaceManager> {
    default boolean isTempProject() {
        return getProjectSpaceManager().isTempProject();
    }
    @Nullable default SearchService getSearchService(){
        return null;
    }

    @SneakyThrows
    void createSearchIndex(boolean force);

    @SneakyThrows
    void addToSearchIndex(@Nullable Collection<String> alignedFeaturesToUpdate, @Nullable Collection<String> runIds);

    @SneakyThrows
    void updateSearchIndex(Collection<String> alignedFeaturesToUpdate);

    @SneakyThrows
    void removeFromSearchIndex(@Nullable Collection<String> alignedFeaturesIds, @Nullable Collection<String> compoundIds, @Nullable Collection<String> runIds);

    /**
     * Technical Identifier for the project file/directory on the filesystem/dbms
     * @return Project system uid
     */
    @NotNull String getSystemUID();

    @NotNull String getProjectId();

    @NotNull PSM getProjectSpaceManager();

    Optional<QuantTable> getQuantification(QuantMeasure type, QuantRowType rowType);

    Optional<QuantTable> getQuantificationForAlignedFeatureOrCompound(String objectId, QuantMeasure type, QuantRowType rowType);

    Optional<TraceSet> getTraceSetForAlignedFeature(String alignedFeatureId, boolean includeAll);
    Optional<TraceSet> getTraceSetForCompound(String compoundId, Optional<String> featureId);
    Optional<TraceSet> getTraceSetsForFeatureWithCorrelatedIons(String alignedFeatureId);

    Page<Compound> findCompounds(@Nullable String searchQuery,
                                 Pageable pageable,
                                 boolean msDataSearchPrepared,
                                 @NotNull EnumSet<Compound.OptField> optFields,
                                 @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields);
    Page<Compound> findCompounds(Pageable pageable,
                                 boolean msDataSearchPrepared,
                                 @NotNull EnumSet<Compound.OptField> optFields,
                                 @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields);
    Page<Compound> findCompoundsByGroup(@NotNull String groupName, Pageable pageable, boolean msDataSearchPrepared, @NotNull EnumSet<Compound.OptField> optFields, @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields);

    default List<Compound> addCompounds(@NotNull List<CompoundImport> compounds,
                                @Nullable InstrumentProfile profile,
                                @NotNull EnumSet<Compound.OptField> optFields,
                                @NotNull EnumSet<AlignedFeature.OptField> optFieldsFeatures){
        return addCompounds(compounds, profile, optFields, optFieldsFeatures, ProjectSourceFormats.GENERIC_DIRECT_IMPORT);
    };

    List<Compound> addCompounds(@NotNull List<CompoundImport> compounds,
                                @Nullable InstrumentProfile profile,
                                @NotNull EnumSet<Compound.OptField> optFields,
                                @NotNull EnumSet<AlignedFeature.OptField> optFieldsFeatures,
                                @NotNull String importSource);

    default Page<Compound> findCompounds(Pageable pageable, Compound.OptField... optFields) {
        return findCompounds(pageable, false, toEnumSet(Compound.OptField.class, optFields),
                EnumSet.of(AlignedFeature.OptField.topAnnotations));
    }

    default Compound findCompoundById(String compoundId, Compound.OptField... optFields) {
        return findCompoundById(compoundId,false, toEnumSet(Compound.OptField.class, optFields),
                EnumSet.of(AlignedFeature.OptField.topAnnotations));
    }

    Compound findCompoundById(String compoundId, boolean msDataSearchPrepared, @NotNull EnumSet<Compound.OptField> optFields, @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields);

    void deleteCompoundById(String compoundId);

    Page<AlignedFeatureQuality> findAlignedFeaturesQuality(Pageable pageable);

    AlignedFeatureQuality findAlignedFeaturesQualityById(String alignedFeatureId);

    Page<AlignedFeature> findAlignedFeatures(@Nullable String searchQuery, Pageable pageable, boolean msDataAsCosineQuery, @NotNull EnumSet<AlignedFeature.OptField> optFields);

    default Page<AlignedFeature> findAlignedFeatures(@Nullable String searchQuery, Pageable pageable, boolean msDataAsCosineQuery, @NotNull AlignedFeature.OptField... optFields){
        return findAlignedFeatures(searchQuery, pageable, msDataAsCosineQuery, toEnumSet(AlignedFeature.OptField.class, optFields));
    }

    Page<AlignedFeature> findAlignedFeatures(Pageable pageable, boolean msDataSearchPrepared, @NotNull EnumSet<AlignedFeature.OptField> optFields);

    default Page<AlignedFeature> findAlignedFeatures(Pageable pageable, boolean msDataAsCosineQuery, AlignedFeature.OptField... optFields) {
        return findAlignedFeatures(pageable, msDataAsCosineQuery, toEnumSet(AlignedFeature.OptField.class, optFields));
    }

    List<Feature> findFeaturesByAlignedFeatureId(String alignedFeatureId);

    Page<AlignedFeature> findAlignedFeaturesByGroup(@NotNull String groupName, Pageable pageable, boolean msDataAsCosineQuery, @NotNull EnumSet<AlignedFeature.OptField> optFields);


    /**
     * Imports features without compound grouping. Since grouping is unknows each feature needs to belong to its own compound.
     * To group features as compounds together, please use add compounds instead.
     * @param features the features to be imported into the project
     * @param profile the instrument the features have been measured on.
     * @param optFields opt fields to be returned as part of the imported features/
     * @return imported features with selected opt fields and UUIDs for features and compounds.
     */
    default List<AlignedFeature> addAlignedFeatures(@NotNull List<FeatureImport> features,
                                            @Nullable InstrumentProfile profile,
                                            @NotNull EnumSet<AlignedFeature.OptField> optFields){
        return addAlignedFeatures(features, profile, optFields, ProjectSourceFormats.GENERIC_DIRECT_IMPORT);
    }

    List<AlignedFeature> addAlignedFeatures(@NotNull List<FeatureImport> features,
                                            @Nullable InstrumentProfile profile,
                                            @NotNull EnumSet<AlignedFeature.OptField> optFields,
                                            @NotNull String importSource);


    default AlignedFeature findAlignedFeaturesById(String alignedFeatureId, boolean msDataSearchPrepared, AlignedFeature.OptField... optFields) {
        return findAlignedFeaturesById(alignedFeatureId, msDataSearchPrepared, toEnumSet(AlignedFeature.OptField.class, optFields));
    }

    AlignedFeature findAlignedFeaturesById(String alignedFeatureId, boolean msDataSearchPrepared, @NotNull EnumSet<AlignedFeature.OptField> optFields);

    void deleteAlignedFeaturesById(String alignedFeatureId);
    void deleteAlignedFeaturesByIds(List<String> alignedFeatureId);

    Page<Run> findRuns(@Nullable String searchQuery, Pageable pageable, @NotNull EnumSet<Run.OptField> optFields);

    Page<Run> findRuns(Pageable pageable, @NotNull EnumSet<Run.OptField> optFields);

    default Page<Run> findRuns(Pageable pageable, Run.OptField... optFields) {
        return findRuns(pageable, toEnumSet(Run.OptField.class, optFields));
    }

    Page<Run>  findRunsByGroup(@NotNull String groupName, Pageable pageable, @NotNull EnumSet<Run.OptField> optFields);

    Run findRunById(String runId, @NotNull EnumSet<Run.OptField> optFields);

    default Run findRunById(String runId, Run.OptField... optFields) {
        return findRunById(runId, toEnumSet(Run.OptField.class, optFields));
    }

    /**
     * Add/Updates tags to/of a target object identified by target class und object id.
     * @param taggedObjectClass class of the target
     * @param objectId id of the target
     * @param tags tags to be added
     * @return return all tags of the target object that has been modified.
     */
    List<Tag> addTagsToObject(Class<? extends Taggable> taggedObjectClass, String objectId, List<Tag> tags);

    void addTagsToObjects(Class<? extends Taggable> taggedObjectClass, List<TagSubmission> tags);

    void removeTagsFromObject(Class<? extends Taggable> taggedObjectClass, String taggedObjectId, List<String> tagNames);

    List<Tag> findTagsByObject(Class<? extends Taggable> taggedObjectClass, String objectId);

    List<TagDefinition> findTags();

    List<TagDefinition> findTagsByType(String tagType);

    TagDefinition findTagByName(String tagName);

    List<TagDefinition> createTags(List<TagDefinitionImport> tagDefinitions, boolean editable);

    default TagDefinition createTag(TagDefinitionImport tagDefinition, boolean editable) {
        return createTags(List.of(tagDefinition), editable).getFirst();
    }

    void deleteTags(String tagName);

    TagDefinition addPossibleValuesToTagDefinition(String tagName, List<?> values);

    List<TagGroup> findTagGroups();

    List<TagGroup> findTagGroupsByType(String type);

    TagGroup findTagGroup(String name);

    TagGroup addTagGroup(String name, String query, String type);

    void deleteTagGroup(String name);

    StatisticsTable getFoldChangeTable(QuantRowType statsTarget, AggregationType aggregation, QuantMeasure quantification);

    <F extends FoldChange> Page<F> listFoldChanges(QuantRowType statsTarget, Pageable pageable);

    <F extends FoldChange> List<F> getFoldChanges(QuantRowType statsTarget, String objectId);

    void deleteFoldChange(QuantRowType statsTarget, String left, String right, AggregationType aggregation, QuantMeasure quantification);

    SpectralLibraryMatchSummary summarizeLibraryMatchesByFeatureId(String alignedFeatureId, int minSharedPeaks, double minSimilarity);

    SpectralLibraryMatchSummary summarizeLibraryMatchesByFeatureIdAndInchi(String alignedFeatureId, String candidateInchi, int minSharedPeaks, double minSimilarity);

    Page<SpectralLibraryMatch> findLibraryMatchesByFeatureId(String alignedFeatureId, int minSharedPeaks, double minSimilarity, Pageable pageable);

    Page<SpectralLibraryMatch> findLibraryMatchesByFeatureIdAndInchi(String alignedFeatureId, String candidateInchi, int minSharedPeaks, double minSimilarity, Pageable pageable);

    SpectralLibraryMatch findLibraryMatchesByFeatureIdAndMatchId(String alignedFeatureId, String matchId);

    Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, boolean msDataSearchPrepared, @NotNull EnumSet<FormulaCandidate.OptField> optFields);

    default Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, boolean msDataSearchPrepared, FormulaCandidate.OptField... optFields) {
        return findFormulaCandidatesByFeatureId(alignedFeatureId, pageable, msDataSearchPrepared, toEnumSet(FormulaCandidate.OptField.class, optFields));
    }

    FormulaCandidate findFormulaCandidateByFeatureIdAndId(String formulaId, String alignedFeatureId, boolean msDataSearchPrepared, @NotNull EnumSet<FormulaCandidate.OptField> optFields);

    default FormulaCandidate findFormulaCandidateByFeatureIdAndId(String formulaId, String alignedFeatureId, boolean msDataSearchPrepared, FormulaCandidate.OptField... optFields) {
        return findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId, msDataSearchPrepared, toEnumSet(FormulaCandidate.OptField.class, optFields));
    }

    Page<StructureCandidateScored> findStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields);

    default Page<StructureCandidateScored> findStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, StructureCandidateScored.OptField... optFields) {
        return findStructureCandidatesByFeatureIdAndFormulaId(formulaId, alignedFeatureId, pageable, toEnumSet(StructureCandidateScored.OptField.class, optFields));
    }

    Page<StructureCandidateScored> findDeNovoStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields);

    default Page<StructureCandidateScored> findDeNovoStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, StructureCandidateScored.OptField... optFields) {
        return findStructureCandidatesByFeatureIdAndFormulaId(formulaId, alignedFeatureId, pageable, toEnumSet(StructureCandidateScored.OptField.class, optFields));
    }

    Page<StructureCandidateFormula> findStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields);

    default Page<StructureCandidateFormula> findStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, StructureCandidateScored.OptField... optFields) {
        return findStructureCandidatesByFeatureId(alignedFeatureId, pageable, toEnumSet(StructureCandidateScored.OptField.class, optFields));
    }

    Page<StructureCandidateFormula> findDeNovoStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields);

    default Page<StructureCandidateFormula> findDeNovoStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, StructureCandidateScored.OptField... optFields) {
        return findDeNovoStructureCandidatesByFeatureId(alignedFeatureId, pageable, toEnumSet(StructureCandidateScored.OptField.class, optFields));
    }

    StructureCandidateScored findTopStructureCandidateByFeatureId(String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields);

    default StructureCandidateScored findTopStructureCandidateByFeatureId(String alignedFeatureId, StructureCandidateScored.OptField... optFields) {
        return findTopStructureCandidateByFeatureId(alignedFeatureId, toEnumSet(StructureCandidateScored.OptField.class, optFields));
    }

    StructureCandidateScored findStructureCandidateById(@NotNull String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields);

    default StructureCandidateScored findStructureCandidateById(@NotNull String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId, StructureCandidateScored.OptField... optFields) {
        return findStructureCandidateById(inchiKey, formulaId, alignedFeatureId, toEnumSet(StructureCandidateScored.OptField.class, optFields));
    }

    /**
     * Return Annotated MsMs Spectrum (Fragments and Structure)
     *
     * @param specIndex        index of the spectrum to annotate if < 0 a Merged Ms/Ms over all spectra will be used
     * @param inchiKey         of the structure candidate that will be used
     * @param formulaId        of the formula candidate to retrieve the fragments from
     * @param alignedFeatureId the feature the spectrum belongs to
     * @return Annotated MsMs Spectrum (Fragments and Structure)
     */
    AnnotatedSpectrum findAnnotatedSpectrumByStructureId(int specIndex, @Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId, boolean searchPrepared);

    default AnnotatedSpectrum findAnnotatedSpectrumByFormulaId(int specIndex, @NotNull String formulaId, @NotNull String alignedFeatureId, boolean searchPrepared) {
        return findAnnotatedSpectrumByStructureId(specIndex, null, formulaId, alignedFeatureId, searchPrepared);
    }

    AnnotatedMsMsData findAnnotatedMsMsDataByStructureId(@Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId, boolean searchPrepared);


    default AnnotatedMsMsData findAnnotatedMsMsDataByFormulaId(@NotNull String formulaId, @NotNull String alignedFeatureId, boolean searchPrepared) {
        return findAnnotatedMsMsDataByStructureId(null, formulaId, alignedFeatureId, searchPrepared);
    }

    String getFingerIdDataCSV(int charge);

    String getCanopusClassyFireDataCSV(int charge);

    String getCanopusNpcDataCSV(int charge);

    Optional<ProjectType> getProjectType();
    Optional<ProjectSourceFormats> getProjectSourceFormats();

    @Deprecated
    String findSiriusFtreeJsonById(String formulaId, String alignedFeatureId);
}
