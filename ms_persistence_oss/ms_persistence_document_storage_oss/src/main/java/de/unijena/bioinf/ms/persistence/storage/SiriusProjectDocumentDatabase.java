/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.persistence.storage;

import de.unijena.bioinf.ChemistryBase.chem.FeatureGroup;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintData;
import de.unijena.bioinf.ChemistryBase.fp.StandardFingerprintData;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.ms.InputFileConfig;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.sirius.*;
import de.unijena.bioinf.ms.persistence.model.sirius.serializers.CanopusPredictionDeserializer;
import de.unijena.bioinf.ms.persistence.model.sirius.serializers.CsiPredictionDeserializer;
import de.unijena.bioinf.chemdb.nitrite.serializers.NitriteCompoundSerializers;
import de.unijena.bioinf.ms.properties.ConfigType;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.Index;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import io.hypersistence.tsid.TSID;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface SiriusProjectDocumentDatabase<Storage extends Database<?>> extends NetworkingProjectDocumentDatabase<Storage> {
    String SIRIUS_PROJECT_SUFFIX = ".sirius";
    String FP_DATA_COLLECTION = "FP_DATA";

    static Metadata buildMetadata() throws IOException {
        return buildMetadata(Metadata.build());
    }

    static Metadata buildMetadata(@NotNull Metadata sourceMetadata) throws IOException {
        NetworkingProjectDocumentDatabase.buildMetadata(sourceMetadata)
                .addCollection(FP_DATA_COLLECTION, Index.unique("type", "charge"))

                .addRepository(Parameters.class, Index.unique("alignedFeatureId", "type"))

                .addRepository(ComputedSubtools.class, "alignedFeatureId")

                .addRepository(FormulaCandidate.class,
                        Index.nonUnique("alignedFeatureId"),
                        Index.nonUnique("formulaRank") //for fast sorted pages
//                        , Index.nonUnique("molecularFormula", "adduct") // reinstert if we really need this search feature.
                )
                .addRepository(FTreeResult.class, "formulaId", Index.nonUnique("alignedFeatureId"))

                .addRepository(CsiPrediction.class, "formulaId", Index.nonUnique("alignedFeatureId"))
                .addDeserializer(CsiPrediction.class,
                        new CsiPredictionDeserializer())

                .addRepository(CanopusPrediction.class, "formulaId", Index.nonUnique("alignedFeatureId"))
                .addDeserializer(CanopusPrediction.class,
                        new CanopusPredictionDeserializer())

                .addRepository(CsiStructureSearchResult.class, "alignedFeatureId")

                .addRepository(CsiStructureMatch.class,
                        Index.unique("alignedFeatureId", "formulaId", "candidateInChiKey"),
                        Index.nonUnique("structureRank")) //for fast sorted pages

                .addRepository(DenovoStructureMatch.class,
                        Index.unique("alignedFeatureId", "formulaId", "candidateInChiKey"),
                        Index.nonUnique("structureRank")) //for fast sorted pages

                .addRepository(SpectraMatch.class,
                        Index.nonUnique("searchResult.rank"), //sort index
                        Index.nonUnique("searchResult.candidateInChiKey"),
                        Index.nonUnique("alignedFeatureId"))

                .addRepository(FingerprintCandidate.class) //pk inchiKey
                .setOptionalFields(FingerprintCandidate.class, "fingerprint")
                .addSerialization(FingerprintCandidate.class,
                        new NitriteCompoundSerializers.FingerprintCandidateSerializer(),
                        new NitriteCompoundSerializers.FingerprintCandidateDeserializer(null)) //will be added later because it has to be read from project
        ;

        return sourceMetadata;
    }

    void insertFingerprintData(StandardFingerprintData<?> fpData, int charge);

    void insertFingerprintData(FingerIdData fpData, int charge);

    <T extends FingerprintData<?>> Optional<T> findFingerprintData(Class<T> dataClazz, int charge);

    @SneakyThrows
    default Optional<CsiStructureSearchResult> findCsiStructureSearchResult(long alignedFeatureId, boolean includeStructureMatches) {
        Optional<CsiStructureSearchResult> result = getStorage().getByPrimaryKey(alignedFeatureId, CsiStructureSearchResult.class);
        if (includeStructureMatches && result.isPresent())
            getStorage().fetchAllChildren(result.get(), "alignedFeatureId", "matches", CsiStructureMatch.class);
        return result;
    }

    @SneakyThrows
    default Optional<Ms2Experiment> fetchMsDataAndConfigsAsMsExperiment(@Nullable final AlignedFeatures feature) {
        if (feature == null)
            return Optional.empty();

        fetchMsData(feature);

        if (feature.getMSData().isEmpty())
            return Optional.empty();

        ParameterConfig config = getConfig(feature.getAlignedFeatureId(), ConfigType.PROJECT)
                .map(Parameters::newParameterConfig)
                .orElse(PropertyManager.DEFAULTS);

        return Optional.of(StorageUtils.toMs2Experiment(feature, config));
    }

    @SneakyThrows
    default Optional<Ms2Experiment> findAlignedFeatureAsMsExperiment(long alignedFeatureId) {
        return getStorage().getByPrimaryKey(alignedFeatureId, AlignedFeatures.class)
                .flatMap(this::fetchMsDataAndConfigsAsMsExperiment);
    }

    default <A extends StructureMatch> A fetchFingerprintCandidate(@NotNull final A structureMatch) {
        return fetchFingerprintCandidate(structureMatch, true);
    }

    @SneakyThrows
    default <A extends StructureMatch> A fetchFingerprintCandidate(@NotNull final A structureMatch, boolean includeFingerprint) {
        String[] opts = includeFingerprint ? new String[]{"fingerprint"} : new String[0];
        getStorage().fetchChild(structureMatch, "candidateInChiKey", "inchikey", "candidate", FingerprintCandidate.class, opts);
        return structureMatch;
    }

    @SneakyThrows
    default int upsertConfig(long alignedFeatureId, @NotNull ConfigType type, ParameterConfig config) {
        return upsertConfig(alignedFeatureId, type, config, true);
    }

    @SneakyThrows
    default int upsertConfig(long alignedFeatureId, @NotNull ConfigType type, ParameterConfig config, boolean modificationOnly) {
        getStorage().removeAll(Filter.and(
                Filter.where("alignedFeatureId").eq(alignedFeatureId),
                Filter.where("type").eq(type.name())), Parameters.class);
        return getStorage().insert(Parameters.of(config, type, alignedFeatureId, modificationOnly));
    }

    @SneakyThrows
    default Optional<Parameters> getConfig(long alignedFeatureId, @NotNull ConfigType type) {
        return getStorage().findStr(
                Filter.and(
                        Filter.where("alignedFeatureId").eq(alignedFeatureId),
                        Filter.where("type").eq(type.name())),
                Parameters.class
        ).findFirst();
    }

    @SneakyThrows
    default AlignedFeatures importMs2ExperimentAsAlignedFeature(Ms2Experiment exp) throws IOException {
        AlignedFeatures alignedFeature = StorageUtils.fromMs2Experiment(exp);

        final FeatureGroup fg = exp.getAnnotationOrNull(FeatureGroup.class);
        final long cuud;
        if (fg == null || fg.getGroupId() < 0)
            cuud = TSID.fast().toLong();
        else
            cuud = fg.getGroupId();

        if (!getStorage().containsPrimaryKey(cuud, Compound.class)) {
            //create new compound since compound does yet not exist.
            Compound.CompoundBuilder builder = Compound.builder()
                    .adductFeatures(List.of(alignedFeature))
                    .compoundId(cuud);
            // singleton feature
            if (fg == null)
                builder.name(alignedFeature.getName()).rt(alignedFeature.getRetentionTime());
            else
                builder.name(fg.getGroupName()).rt(fg.getGroupRt());

            importCompounds(List.of(builder.build()));
        } else {
            importAlignedFeatures(List.of(alignedFeature), cuud);
        }
        //add configs that might have been read from input file to project space
        Parameters config = exp.getAnnotation(InputFileConfig.class).map(InputFileConfig::config)
                .map(c -> Parameters.of(c, ConfigType.INPUT_FILE, true)).orElse(null);
        if (config != null) {
            config.setAlignedFeatureId(alignedFeature.getAlignedFeatureId());
            getStorage().insert(config);
        }
        return alignedFeature;
    }

    default List<AlignedFeatures> importMs2ExperimentsAsAlignedFeatures(List<Ms2Experiment> ms2Experiments) throws IOException {
        List<AlignedFeatures> alignedFeatures = ms2Experiments.stream().map(StorageUtils::fromMs2Experiment).toList();
        List<Compound> compounds = alignedFeatures.stream().map(
                alignedFeature -> Compound.builder()
                        .name(alignedFeature.getName())
                        .rt(alignedFeature.getRetentionTime())
                        .adductFeatures(List.of(alignedFeature))
                        .build()
        ).toList();
        importCompounds(compounds);
        return alignedFeatures;
    }


    default <T> Stream<T> findByFeatureIdStr(long alignedFeatureId, Class<T> clzz, String... optFields) {
        return stream(findByFeatureId(alignedFeatureId, clzz, optFields));
    }

    @SneakyThrows
    default <T> Iterable<T> findByFeatureId(long alignedFeatureId, Class<T> clzz, String... optFields) {
        return getStorage().find(Filter.where("alignedFeatureId").eq(alignedFeatureId), clzz, optFields);
    }

    default <T> Stream<T> findByFeatureIdStr(long alignedFeatureId, Class<T> clzz, String sortField, Database.SortOrder sortOrder, String... optFields) {
        return stream(findByFeatureId(alignedFeatureId, clzz, sortField, sortOrder, optFields));
    }

    @SneakyThrows
    default <T> Iterable<T> findByFeatureId(long alignedFeatureId, Class<T> clzz, String sortField, Database.SortOrder sortOrder, String... optFields) {
        return getStorage().find(Filter.where("alignedFeatureId").eq(alignedFeatureId), clzz, sortField, sortOrder, optFields);
    }

    default <T> Stream<T> findByFeatureIdStr(long alignedFeatureId, Class<T> clzz, long offset, int pageSize, String sortField, Database.SortOrder sortOrder, String... optFields) {
        return stream(findByFeatureId(alignedFeatureId, clzz, offset, pageSize, sortField, sortOrder, optFields));
    }

    @SneakyThrows
    default <T> Iterable<T> findByFeatureId(long alignedFeatureId, Class<T> clzz, long offset, int pageSize, String sortField, Database.SortOrder sortOrder, String... optFields) {
        return getStorage().find(Filter.where("alignedFeatureId").eq(alignedFeatureId), clzz, offset, pageSize, sortField, sortOrder, optFields);
    }

    default <T> Stream<T> findByFormulaIdStr(long formulaId, Class<T> clzz) {
        return stream(findByFormulaId(formulaId, clzz));
    }

    @SneakyThrows
    default <T> Iterable<T> findByFormulaId(long formulaId, Class<T> clzz) {
        return getStorage().find(Filter.where("formulaId").eq(formulaId), clzz);
    }

    default <T> Stream<T> findByInChIStr(@NotNull String candidateInChiKey, Class<T> clzz, String... optFields) {
        return stream(findByInChI(candidateInChiKey, clzz, optFields));
    }

    @SneakyThrows
    default <T> Iterable<T> findByInChI(@NotNull String candidateInChiKey, Class<T> clzz, String... optFields) {
        return getStorage().find(Filter.where("candidateInChiKey").eq(candidateInChiKey), clzz, optFields);
    }

    default <T> Stream<T> findByFeatureIdAndFormulaIdStr(long alignedFeatureId, long formulaId, Class<T> clzz, long offset, int pageSize, String sortField, Database.SortOrder sortOrder, String... optFields) {
        return stream(findByFeatureIdAndFormulaId(alignedFeatureId, formulaId, clzz, offset, pageSize, sortField, sortOrder, optFields));
    }

    @SneakyThrows
    default <T> Iterable<T> findByFeatureIdAndFormulaId(long alignedFeatureId, long formulaId, Class<T> clzz, long offset, int pageSize, String sortField, Database.SortOrder sortOrder, String... optFields) {
        return getStorage().find(Filter.and(
                Filter.where("alignedFeatureId").eq(alignedFeatureId),
                Filter.where("formulaId").eq(formulaId)), clzz, offset, pageSize, sortField, sortOrder, optFields);
    }

    default <T> Stream<T> findByFeatureIdAndFormulaIdStr(long alignedFeatureId, long formulaId, Class<T> clzz, String... optFields) {
        return stream(findByFeatureIdAndFormulaId(alignedFeatureId, formulaId, clzz, optFields));
    }

    @SneakyThrows
    default <T> Iterable<T> findByFeatureIdAndFormulaId(long alignedFeatureId, long formulaId, Class<T> clzz, String... optFields) {
        return getStorage().find(Filter.and(
                Filter.where("alignedFeatureId").eq(alignedFeatureId),
                Filter.where("formulaId").eq(formulaId)), clzz, optFields);
    }

    default <T> Stream<T> findByFeatureIdAndFormulaIdAndInChIStr(long alignedFeatureId, long formulaId, @Nullable String candidateInChiKey, Class<T> clzz, String... optFields) {
        return stream(findByFeatureIdAndFormulaIdAndInChI(alignedFeatureId, formulaId, candidateInChiKey, clzz, optFields));
    }

    @SneakyThrows
    default <T> Iterable<T> findByFeatureIdAndFormulaIdAndInChI(long alignedFeatureId, long formulaId, @Nullable String candidateInChiKey, Class<T> clzz, String... optFields) {
        if (candidateInChiKey == null)
            return findByFeatureIdAndFormulaId(alignedFeatureId, formulaId, clzz);
        return getStorage().find(Filter.and(
                Filter.where("alignedFeatureId").eq(alignedFeatureId),
                Filter.where("formulaId").eq(formulaId),
                Filter.where("candidateInChiKey").eq(candidateInChiKey)), clzz, optFields);
    }

    @SneakyThrows
    default <T> long countByFeatureId(long alignedFeatureId, Class<T> clzz) {
        return getStorage().count(Filter.where("alignedFeatureId").eq(alignedFeatureId), clzz);
    }

    @SneakyThrows
    default <T> long countByFormulaId(long formulaId, Class<T> clzz) {
        return getStorage().count(Filter.where("formulaId").eq(formulaId), clzz);
    }

    @SneakyThrows
    default <T> long deleteAllByFeatureId(long alignedFeatureId, Class<T> clzz) {
        return this.getStorage().removeAll(Filter.where("alignedFeatureId").eq(alignedFeatureId), clzz);
    }

    @SneakyThrows
    default <T> long deleteAllByFormulaId(long formulaId, Class<T> clzz) {
        return this.getStorage().removeAll(Filter.where("formulaId").eq(formulaId), clzz);
    }

    long cascadeDeleteCompound(long compoundId) throws IOException;

    long cascadeDeleteAlignedFeatures(long alignedFeatureId) throws IOException;

    long cascadeDeleteAlignedFeatures(List<Long> alignedFeatureIds) throws IOException;

}
