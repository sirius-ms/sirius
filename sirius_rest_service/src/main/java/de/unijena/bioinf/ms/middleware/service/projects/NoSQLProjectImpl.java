/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.babelms.inputresource.PathInputResource;
import de.unijena.bioinf.ms.middleware.model.annotations.FormulaCandidate;
import de.unijena.bioinf.ms.middleware.model.annotations.SpectralLibraryMatch;
import de.unijena.bioinf.ms.middleware.model.annotations.StructureCandidateFormula;
import de.unijena.bioinf.ms.middleware.model.annotations.StructureCandidateScored;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.features.*;
import de.unijena.bioinf.ms.middleware.model.projects.ImportResult;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.sirius.SpectraMatch;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Stream;

public class NoSQLProjectImpl implements Project<NoSQLProjectSpaceManager> {

    @NotNull
    private final String projectId;

    @NotNull
    private final NoSQLProjectSpaceManager projectSpaceManager;

    private final SiriusProjectDocumentDatabase<? extends Database<?>> database;

    @Getter
    private final Database<?> storage;
    private final Map<Class<?>, AtomicLong> totalCounts = Collections.synchronizedMap(new HashMap<>());

    private final Map<Class<?>, Map<Long, AtomicLong>> totalCountByFeature = Collections.synchronizedMap(new HashMap<>());

    @SneakyThrows
    public NoSQLProjectImpl(@NotNull String projectId, @NotNull NoSQLProjectSpaceManager projectSpaceManager) {
        this.projectId = projectId;
        this.projectSpaceManager = projectSpaceManager;
        this.database = projectSpaceManager.getProject();
        this.storage = database.getStorage();

        initCounter(de.unijena.bioinf.ms.persistence.model.core.Compound.class);
        initCounter(AlignedFeatures.class);

        initCounterByFeature(SpectraMatch.class, SpectraMatch::getAlignedFeatureId);
    }

    @SneakyThrows
    private void initCounter(Class<?> clazz) {
        this.totalCounts.put(clazz, new AtomicLong(this.storage.countAll(clazz)));
        this.storage.onInsert(clazz, c -> totalCounts.get(clazz).getAndIncrement());
        this.storage.onRemove(clazz, c -> totalCounts.get(clazz).getAndDecrement());
    }

    @SneakyThrows
    private <T> void initCounterByFeature(Class<T> clazz, Function<T, Long> featureIdGetter) {
        Map<Long, AtomicLong> counts = Collections.synchronizedMap(new HashMap<>());
        for (AlignedFeatures af : this.storage.findAll(AlignedFeatures.class)) {
            counts.put(af.getAlignedFeatureId(), new AtomicLong(this.storage.count(Filter.where("alignedFeatureId").eq(af.getAlignedFeatureId()), clazz)));
        }
        this.totalCountByFeature.put(clazz, counts);
        this.storage.onInsert(clazz, c -> this.totalCountByFeature.get(clazz).get(featureIdGetter.apply(c)).getAndIncrement());
        this.storage.onRemove(clazz, c -> this.totalCountByFeature.get(clazz).get(featureIdGetter.apply(c)).getAndDecrement());
    }

    @Override
    public @NotNull String getProjectId() {
        return projectId;
    }

    @Override
    public @NotNull NoSQLProjectSpaceManager getProjectSpaceManager() {
        return projectSpaceManager;
    }

    private Pair<String, Database.SortOrder> sort(Sort sort, Pair<String, Database.SortOrder> defaults, Function<String, String> translator) {
        if (sort == null)
            return defaults;

        if (sort == Sort.unsorted())
            return defaults;

        Optional<Sort.Order> order = sort.stream().findFirst();
        if (order.isEmpty())
            return defaults;

        String property = order.get().getProperty();
        if (property.isEmpty() || property.isBlank()) {
            return defaults;
        }

        Database.SortOrder so = order.get().getDirection().isAscending() ? Database.SortOrder.ASCENDING :Database.SortOrder.DESCENDING;
        return Pair.of(translator.apply(property), so);
    }

    private Pair<String, Database.SortOrder> sortCompound(Sort sort) {
        return sort(sort, Pair.of("name", Database.SortOrder.ASCENDING), s -> switch (s) {
            case "rtStartSeconds" -> "rt.start";
            case "rtEndSeconds" -> "rt.end";
            default -> s;
        });
    }

    private Pair<String, Database.SortOrder> sortFeature(Sort sort) {
        return sort(sort, Pair.of("name", Database.SortOrder.ASCENDING), s -> switch (s) {
            case "rtStartSeconds" -> "retentionTime.start";
            case "rtEndSeconds" -> "retentionTime.end";
            case "ionMass" -> "averageMass";
            default -> s;
        });
    }

    private Compound convertCompound(de.unijena.bioinf.ms.persistence.model.core.Compound compound) {
        Compound.CompoundBuilder builder = Compound.builder()
                .compoundId(String.valueOf(compound.getCompoundId()))
                .name(compound.getName())
                .neutralMass(compound.getNeutralMass());

        RetentionTime rt = compound.getRt();
        if (rt != null) {
            if (Double.isFinite(rt.getStartTime()) && Double.isFinite(rt.getEndTime())) {
                builder.rtStartSeconds(rt.getStartTime());
                builder.rtEndSeconds(rt.getEndTime());
            } else {
                builder.rtStartSeconds(rt.getMiddleTime());
                builder.rtEndSeconds(rt.getMiddleTime());
            }
        }

        compound.getAdductFeatures().ifPresent(features -> builder.features(features.stream().map(this::convertFeature).toList()));

        return builder.build();
    }

    private de.unijena.bioinf.ms.persistence.model.core.Compound convertCompound(CompoundImport compoundImport) {
        List<AlignedFeatures> features = compoundImport.getFeatures().stream().map(this::convertFeature).toList();

        de.unijena.bioinf.ms.persistence.model.core.Compound.CompoundBuilder builder = de.unijena.bioinf.ms.persistence.model.core.Compound.builder()
                .name(compoundImport.getName())
                .adductFeatures(features);

        List<RetentionTime> rts = features.stream().map(AlignedFeatures::getRetentionTime).filter(Objects::nonNull).toList();
        double start = rts.stream().mapToDouble(RetentionTime::getStartTime).min().orElse(Double.NaN);
        double end = rts.stream().mapToDouble(RetentionTime::getEndTime).min().orElse(Double.NaN);

        if (Double.isFinite(start) && Double.isFinite(end)) {
            builder.rt(new RetentionTime(start, end));
        }

        features.stream().mapToDouble(AlignedFeatures::getAverageMass).average().ifPresent(builder::neutralMass);

        return builder.build();
    }

    private AlignedFeatures convertFeature(FeatureImport featureImport) {

        AlignedFeatures.AlignedFeaturesBuilder<?, ?> builder = AlignedFeatures.builder()
                .name(featureImport.getName())
                .externalFeatureId(featureImport.getFeatureId())
                .averageMass(featureImport.getIonMass());

        if (featureImport.getMergedMs1() != null || featureImport.getMs2Spectra() != null) {
            MSData.MSDataBuilder msDataBuilder = MSData.builder();

            if (featureImport.getMergedMs1() != null)
                msDataBuilder.mergedMs1Spectrum(new SimpleSpectrum(featureImport.getMergedMs1().getMasses(), featureImport.getMergedMs1().getIntensities()));

            if (featureImport.getMs2Spectra() != null)
                msDataBuilder.msnSpectra(featureImport.getMs2Spectra().stream().map(s -> new MutableMs2Spectrum(s, s.getPrecursorMz(), s.getCollisionEnergy(), s.getMsLevel(), s.getScanNumber())).toList());

            builder.msData(msDataBuilder.build());
        }

        if (featureImport.getRtStartSeconds() != null && featureImport.getRtEndSeconds() != null) {
            builder.retentionTime(new RetentionTime(featureImport.getRtStartSeconds(), featureImport.getRtEndSeconds()));
        }
        // TODO detected adducts
        return builder.build();
    }

    private AlignedFeature convertFeature(AlignedFeatures features) {
        AlignedFeature.AlignedFeatureBuilder builder = AlignedFeature.builder()
                .alignedFeatureId(String.valueOf(features.getAlignedFeatureId()))
                .name(features.getName())
                .ionMass(features.getAverageMass());

        if (features.getDetectedAdducts() != null)
                builder.adduct(features.getIonType().toString()); //is called adduct but refers to iontype (input setting) -> maybe rename

        RetentionTime rt = features.getRetentionTime();
        if (rt != null) {
            if (Double.isFinite(rt.getStartTime()) && Double.isFinite(rt.getEndTime())) {
                builder.rtStartSeconds(rt.getStartTime());
                builder.rtEndSeconds(rt.getEndTime());
            } else {
                builder.rtStartSeconds(rt.getMiddleTime());
                builder.rtEndSeconds(rt.getMiddleTime());
            }
        }

        features.getMSData().map(this::convertMSData).ifPresent(builder::msData);

        return builder.build();
    }

    private MsData convertMSData(MSData msData) {
        return MsData.builder()
                .mergedMs1(new BasicSpectrum(msData.getMergedMs1Spectrum()))
                .mergedMs2(new BasicSpectrum(msData.getMergedMSnSpectrum().getPeaks()))
                // TODO ms1 spectra
                .ms2Spectra(msData.getMsnSpectra().stream().map(BasicSpectrum::new).toList())
                .build();
    }

    @SneakyThrows
    @Override
    public Page<Compound> findCompounds(Pageable pageable, @NotNull EnumSet<Compound.OptField> optFields, @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields) {
        Pair<String, Database.SortOrder> sort = sortCompound(pageable.getSort());
        Stream<de.unijena.bioinf.ms.persistence.model.core.Compound> stream;
        if (pageable.isPaged()) {
            stream = storage.findAllStr(de.unijena.bioinf.ms.persistence.model.core.Compound.class, (int) pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight());
        } else {
            stream = storage.findAllStr(de.unijena.bioinf.ms.persistence.model.core.Compound.class, sort.getLeft(), sort.getRight());
        }
        stream = stream.peek(database::fetchAdductFeatures);

        if (optFeatureFields.contains(AlignedFeature.OptField.msData)) {
            stream = stream.peek(c -> c.getAdductFeatures().ifPresent(features -> features.forEach(database::fetchMsData)));
        }

        List<Compound> compounds = stream.map(this::convertCompound).toList();

        // TODO annotations
        long total = totalCounts.get(de.unijena.bioinf.ms.persistence.model.core.Compound.class).get();

        return new PageImpl<>(compounds, pageable, total);
    }

    @SneakyThrows
    @Override
    public List<Compound> addCompounds(@NotNull List<CompoundImport> compounds, @NotNull EnumSet<Compound.OptField> optFields, @NotNull EnumSet<AlignedFeature.OptField> optFieldsFeatures) {
        List<de.unijena.bioinf.ms.persistence.model.core.Compound> dbc = compounds.stream().map(this::convertCompound).toList();
        database.importCompounds(dbc);
        // TODO test if all ids are correctly set!
        return dbc.stream().map(this::convertCompound).toList();
    }

    @Override
    public ImportResult importPreprocessedData(Collection<InputResource<?>> inputResources, boolean ignoreFormulas, boolean allowMs1OnlyData) {
        // TODO
        return null;
    }

    @Override
    public ImportResult importMsRunData(Collection<PathInputResource> inputResources, boolean alignRuns, boolean allowMs1OnlyData) {
        return null;
    }

    @SneakyThrows
    @Override
    public Compound findCompoundById(String compoundId, @NotNull EnumSet<Compound.OptField> optFields, @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields) {
        long id = Long.parseLong(compoundId);
        return storage.getByPrimaryKey(id, de.unijena.bioinf.ms.persistence.model.core.Compound.class)
                .map(c -> {
                    database.fetchAdductFeatures(c);
                    if (optFeatureFields.contains(AlignedFeature.OptField.msData)) {
                        c.getAdductFeatures().ifPresent(features -> features.forEach(database::fetchMsData));
                    }
                    return convertCompound(c);
                })
                // TODO annotations
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no compound '" + compoundId + "' in project " + projectId + "."));
    }

    @SneakyThrows
    @Override
    public void deleteCompoundById(String compoundId) {
        long id = Long.parseLong(compoundId);
        storage.removeAll(Filter.where("compoundId").eq(id), de.unijena.bioinf.ms.persistence.model.core.Compound.class);
        // TODO cascade delete?
    }

    @Override
    public Page<AlignedFeatureQuality> findAlignedFeaturesQuality(Pageable pageable, @NotNull EnumSet<AlignedFeatureQuality.OptField> optFields) {
        return null;
    }

    @Override
    public AlignedFeatureQuality findAlignedFeaturesQualityById(String alignedFeatureId, @NotNull EnumSet<AlignedFeatureQuality.OptField> optFields) {
        return null;
    }

    @SneakyThrows
    @Override
    public Page<AlignedFeature> findAlignedFeatures(Pageable pageable, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        Pair<String, Database.SortOrder> sort = sortFeature(pageable.getSort());
        Stream<AlignedFeatures> stream;
        if (pageable.isPaged()) {
            stream = storage.findAllStr(AlignedFeatures.class, (int) pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight());
        } else {
            stream = storage.findAllStr(AlignedFeatures.class, sort.getLeft(), sort.getRight());
        }

        if (optFields.contains(AlignedFeature.OptField.msData)) {
            stream = stream.peek(database::fetchMsData);
        }

        List<AlignedFeature> features = stream.map(this::convertFeature).toList();
        // TODO annotations
        long total = totalCounts.get(AlignedFeatures.class).get();

        return new PageImpl<>(features, pageable, total);
    }

    @Override
    public List<AlignedFeature> addAlignedFeatures(@NotNull List<FeatureImport> features, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        return null;
    }

    @Override
    public AlignedFeature findAlignedFeaturesById(String alignedFeatureId, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        return null;
    }

    @Override
    public void deleteAlignedFeaturesById(String alignedFeatureId) {

    }

    @SneakyThrows
    @Override
    public Page<SpectralLibraryMatch> findLibraryMatchesByFeatureId(String alignedFeatureId, Pageable pageable) {
//        long longId = Long.parseLong(alignedFeatureId);
//        Pair<String, Database.SortOrder> sort = SortConverter.convert(pageable.getSort(), SpectraMatch.class);
//        List<SpectralLibraryMatch> results = storage.findStr(
//                Filter.where("alignedFeatureId").eq(longId), SpectraMatch.class, (int) pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight()
//        ).map(s -> typeConverter.convert(s, SpectralLibraryMatch.class)).toList();
//        long total = totalCountByFeature.get(SpectraMatch.class).getOrDefault(longId, new AtomicLong(0)).get();
//
//        return new PageImpl<>(results, pageable, total);
        return null;
    }

    @SneakyThrows
    @Override
    public Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<FormulaCandidate.OptField> optFields) {
//        long longId = Long.parseLong(alignedFeatureId);
//        List<FormulaCandidate> results = storage.findStr(
//                Filter.where("alignedFeatureId").eq(longId), de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class, (int) pageable.getOffset(), pageable.getPageSize(), "siriusScore", Database.SortOrder.DESCENDING
//        ).map(this::toMiddleWareFCandidate).toList();
//        long total = totalCountByFeature.get(de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class).getOrDefault(longId, new AtomicLong(0)).get();
//
//        return new PageImpl<>(results, pageable, total);
        return null;
    }

    @Override
    public FormulaCandidate findFormulaCandidateByFeatureIdAndId(String formulaId, String alignedFeatureId, @NotNull EnumSet<FormulaCandidate.OptField> optFields) {
        return null;
    }

    @Override
    public Page<StructureCandidateScored> findStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return null;
    }

    @Override
    public Page<StructureCandidateScored> findDeNovoStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return null;
    }

    @Override
    public Page<StructureCandidateFormula> findStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return null;
    }

    @Override
    public Page<StructureCandidateFormula> findDeNovoStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return null;
    }

    @Override
    public StructureCandidateScored findTopStructureCandidateByFeatureId(String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return null;
    }

    @Override
    public StructureCandidateScored findStructureCandidateById(@NotNull String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return null;
    }

    @Override
    public AnnotatedSpectrum findAnnotatedSpectrumByStructureId(int specIndex, @Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId) {
        return null;
    }

    @Override
    public AnnotatedMsMsData findAnnotatedMsMsDataByStructureId(@Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId) {
        return null;
    }

    @Override
    public String getFingerIdDataCSV(int charge) {
        return null;
    }

    @Override
    public String getCanopusClassyFireDataCSV(int charge) {
        return null;
    }

    @Override
    public String getCanopusNpcDataCSV(int charge) {
        return null;
    }

    @Override
    public String findSiriusFtreeJsonById(String formulaId, String alignedFeatureId) {
        return null;
    }
}
