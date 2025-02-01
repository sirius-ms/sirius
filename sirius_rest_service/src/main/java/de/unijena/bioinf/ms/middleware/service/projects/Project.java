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
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils.toEnumSet;

public interface Project<PSM extends ProjectSpaceManager> {

    @NotNull
    String getProjectId();

    @NotNull
    PSM getProjectSpaceManager();

    Optional<QuantTable> getQuantification(QuantMeasure type, QuantRowType rowType);

    Optional<QuantTable> getQuantificationForAlignedFeatureOrCompound(String objectId, QuantMeasure type, QuantRowType rowType);

    Optional<TraceSet> getTraceSetForAlignedFeature(String alignedFeatureId, boolean includeAll);
    Optional<TraceSet> getTraceSetForCompound(String compoundId, Optional<String> featureId);
    Optional<TraceSet> getTraceSetsForFeatureWithCorrelatedIons(String alignedFeatureId);

    Page<Compound> findCompounds(Pageable pageable, @NotNull EnumSet<Compound.OptField> optFields,
                                 @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields);

    List<Compound> addCompounds(@NotNull List<CompoundImport> compounds,
                                @Nullable InstrumentProfile profile,
                                @NotNull EnumSet<Compound.OptField> optFields,
                                @NotNull EnumSet<AlignedFeature.OptField> optFieldsFeatures);

    default Page<Compound> findCompounds(Pageable pageable, Compound.OptField... optFields) {
        return findCompounds(pageable, toEnumSet(Compound.OptField.class, optFields),
                EnumSet.of(AlignedFeature.OptField.topAnnotations));
    }

    Compound findCompoundById(String compoundId, @NotNull EnumSet<Compound.OptField> optFields,
                              @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields);

    default Compound findCompoundById(String compoundId, Compound.OptField... optFields) {
        return findCompoundById(compoundId, toEnumSet(Compound.OptField.class, optFields),
                EnumSet.of(AlignedFeature.OptField.topAnnotations));
    }

    void deleteCompoundById(String compoundId);

    Page<AlignedFeatureQuality> findAlignedFeaturesQuality(Pageable pageable);

    AlignedFeatureQuality findAlignedFeaturesQualityById(String alignedFeatureId);

    Page<AlignedFeature> findAlignedFeatures(Pageable pageable, @NotNull EnumSet<AlignedFeature.OptField> optFields);

    List<Feature> findFeaturesByAlignedFeatureId(String alignedFeatureId);

    List<AlignedFeature> addAlignedFeatures(@NotNull List<FeatureImport> features,
                                            @Nullable InstrumentProfile profile,
                                            @NotNull EnumSet<AlignedFeature.OptField> optFields);

    default Page<AlignedFeature> findAlignedFeatures(Pageable pageable, AlignedFeature.OptField... optFields) {
        return findAlignedFeatures(pageable, toEnumSet(AlignedFeature.OptField.class, optFields));
    }

    AlignedFeature findAlignedFeaturesById(String alignedFeatureId, @NotNull EnumSet<AlignedFeature.OptField> optFields);

    default AlignedFeature findAlignedFeaturesById(String alignedFeatureId, AlignedFeature.OptField... optFields) {
        return findAlignedFeaturesById(alignedFeatureId, toEnumSet(AlignedFeature.OptField.class, optFields));
    }

    void deleteAlignedFeaturesById(String alignedFeatureId);
    void deleteAlignedFeaturesByIds(List<String> alignedFeatureId);

    Page<Run> findRuns(Pageable pageable, @NotNull EnumSet<Run.OptField> optFields);

    default Page<Run> findRuns(Pageable pageable, Run.OptField... optFields) {
        return findRuns(pageable, toEnumSet(Run.OptField.class, optFields));
    }

    Run findRunById(String runId, @NotNull EnumSet<Run.OptField> optFields);

    default Run findRunById(String runId, Run.OptField... optFields) {
        return findRunById(runId, toEnumSet(Run.OptField.class, optFields));
    }

    <T, O extends Enum<O>> Page<T> findObjectsByTagFilter(Class<?> target, @NotNull String filter, Pageable pageable, @NotNull EnumSet<O> optFields);

    List<Tag> addTagsToObject(Class<?> target, String objectId, List<Tag> tags);

    void removeTagsFromObject(Class<?> taggedObjectClass, String taggedObjectId, List<String> tagNames);

    List<Tag> findTagsByObject(Class<?> target, String objectId);

    List<TagDefinition> findTags();

    List<TagDefinition> findTagsByType(String tagType);

    TagDefinition findTagByName(String tagName);

    List<TagDefinition> createTags(List<TagDefinitionImport> tagDefinitions, boolean editable);

    default List<TagDefinition> createTag(TagDefinitionImport tagDefinition, boolean editable) {
        return createTags(List.of(tagDefinition), editable);
    }

    void deleteTags(String tagName);

    TagDefinition addPossibleValuesToTagDefinition(String tagName, List<?> values);

    <T, O extends Enum<O>> Page<T> findObjectsByTagGroup(Class<?> target, @NotNull String group, Pageable pageable, @NotNull EnumSet<O> optFields);

    List<TagGroup> findTagGroups();

    List<TagGroup> findTagGroupsByType(String type);

    TagGroup findTagGroup(String name);

    TagGroup addTagGroup(String name, String query, String type);

    void deleteTagGroup(String name);

    StatisticsTable getFoldChangeTable(Class<?> target, AggregationType aggregation, QuantMeasure quantification);

    <F extends FoldChange> Page<F> listFoldChanges(Class<?> target, Pageable pageable);

    <F extends FoldChange> List<F> getFoldChanges(Class<?> target, String objectId);

    void deleteFoldChange(Class<?> target, String left, String right, AggregationType aggregation, QuantMeasure quantification);

    SpectralLibraryMatchSummary summarizeLibraryMatchesByFeatureId(String alignedFeatureId, int minSharedPeaks, double minSimilarity);

    SpectralLibraryMatchSummary summarizeLibraryMatchesByFeatureIdAndInchi(String alignedFeatureId, String candidateInchi, int minSharedPeaks, double minSimilarity);

    Page<SpectralLibraryMatch> findLibraryMatchesByFeatureId(String alignedFeatureId, int minSharedPeaks, double minSimilarity, Pageable pageable);

    Page<SpectralLibraryMatch> findLibraryMatchesByFeatureIdAndInchi(String alignedFeatureId, String candidateInchi, int minSharedPeaks, double minSimilarity, Pageable pageable);

    SpectralLibraryMatch findLibraryMatchesByFeatureIdAndMatchId(String alignedFeatureId, String matchId);

    Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<FormulaCandidate.OptField> optFields);

    default Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, FormulaCandidate.OptField... optFields) {
        return findFormulaCandidatesByFeatureId(alignedFeatureId, pageable, toEnumSet(FormulaCandidate.OptField.class, optFields));
    }

    FormulaCandidate findFormulaCandidateByFeatureIdAndId(String formulaId, String alignedFeatureId, @NotNull EnumSet<FormulaCandidate.OptField> optFields);

    default FormulaCandidate findFormulaCandidateByFeatureIdAndId(String formulaId, String alignedFeatureId, FormulaCandidate.OptField... optFields) {
        return findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId, toEnumSet(FormulaCandidate.OptField.class, optFields));
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

    default AnnotatedSpectrum findAnnotatedSpectrumByFormulaId(int specIndex, @NotNull String formulaId, @NotNull String alignedFeatureId) {
        return findAnnotatedSpectrumByStructureId(specIndex, null, formulaId, alignedFeatureId);
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
    AnnotatedSpectrum findAnnotatedSpectrumByStructureId(int specIndex, @Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId);

    default AnnotatedMsMsData findAnnotatedMsMsDataByFormulaId(@NotNull String formulaId, @NotNull String alignedFeatureId) {
        return findAnnotatedMsMsDataByStructureId(null, formulaId, alignedFeatureId);
    }

    AnnotatedMsMsData findAnnotatedMsMsDataByStructureId(@Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId);

    String getFingerIdDataCSV(int charge);

    String getCanopusClassyFireDataCSV(int charge);

    String getCanopusNpcDataCSV(int charge);

    @Deprecated
    String findSiriusFtreeJsonById(String formulaId, String alignedFeatureId);
}
