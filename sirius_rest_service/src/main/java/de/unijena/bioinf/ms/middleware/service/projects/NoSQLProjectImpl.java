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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.lcms.msms.MergeGreedyStrategy;
import de.unijena.bioinf.lcms.msms.MergedSpectrum;
import de.unijena.bioinf.lcms.msms.MsMsQuerySpectrum;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.ms.middleware.model.annotations.CanopusPrediction;
import de.unijena.bioinf.ms.middleware.model.annotations.FormulaCandidate;
import de.unijena.bioinf.ms.middleware.model.annotations.*;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.features.*;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.middleware.model.spectra.Spectrums;
import de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import de.unijena.bioinf.ms.persistence.model.sirius.*;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;
import de.unijena.bioinf.storage.db.nosql.Database;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.similarity.LongestCommonSubsequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;


public class NoSQLProjectImpl implements Project<NoSQLProjectSpaceManager> {
    private static final AtomicLong ATOMIC_ZERO = new AtomicLong(0);
    @NotNull
    private final String projectId;

    @NotNull
    private final NoSQLProjectSpaceManager projectSpaceManager;

    private final Map<Class<?>, AtomicLong> totalCounts = Collections.synchronizedMap(new HashMap<>());

    private final Map<Class<?>, Map<Long, AtomicLong>> totalCountByFeature = Collections.synchronizedMap(new HashMap<>());

    private @NotNull BiFunction<Project<?>, String, Boolean> computeStateProvider;

    @SneakyThrows
    public NoSQLProjectImpl(@NotNull String projectId, @NotNull NoSQLProjectSpaceManager projectSpaceManager, @NotNull BiFunction<Project<?>, String, Boolean> computeStateProvider) {
        this.projectId = projectId;
        this.projectSpaceManager = projectSpaceManager;
        this.computeStateProvider = computeStateProvider;

        initCounter(de.unijena.bioinf.ms.persistence.model.core.Compound.class);
        initCounter(AlignedFeatures.class);

        initCounterByFeature(SpectraMatch.class, SpectraMatch::getAlignedFeatureId);
        initCounterByFeature(de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate::getAlignedFeatureId);
        initCounterByFeature(CsiStructureMatch.class, CsiStructureMatch::getAlignedFeatureId);
        initCounterByFeature(DenovoStructureMatch.class, DenovoStructureMatch::getAlignedFeatureId);
    }

    //using private methods instead of references for easier refactoring or changes.
    // compiler will inline the method call since projectmanager is final.
    private Database<?> storage() {
        return projectSpaceManager.getProject().getStorage();
    }

    private SiriusProjectDocumentDatabase<? extends Database<?>> project() {
        return projectSpaceManager.getProject();
    }

    @SneakyThrows
    private void initCounter(Class<?> clazz) {
        storage().read(() -> { // transaction to ensure that we do not miss anything before listener is registered
            this.totalCounts.put(clazz, new AtomicLong(storage().countAll(clazz)));
            storage().onInsert(clazz, c -> totalCounts.get(clazz).getAndIncrement());
            storage().onRemove(clazz, c -> totalCounts.get(clazz).getAndDecrement());
        });

    }

    @SneakyThrows
    private <T> void initCounterByFeature(Class<T> clazz, Function<T, Long> featureIdGetter) {
        storage().read(() -> { // transaction to ensure that we do not miss anything before listener is registered
            Map<Long, AtomicLong> counts = Collections.synchronizedMap(new HashMap<>());
            for (AlignedFeatures af : this.storage().findAll(AlignedFeatures.class)) {
                counts.put(af.getAlignedFeatureId(), new AtomicLong(this.project().countByFeatureId(af.getAlignedFeatureId(), clazz)));
            }
            this.totalCountByFeature.put(clazz, counts);
            this.storage().onInsert(clazz, c -> this.totalCountByFeature.get(clazz).get(featureIdGetter.apply(c)).getAndIncrement());
            this.storage().onRemove(clazz, c -> this.totalCountByFeature.get(clazz).get(featureIdGetter.apply(c)).getAndDecrement());
        });
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

        Database.SortOrder so = order.get().getDirection().isAscending() ? Database.SortOrder.ASCENDING : Database.SortOrder.DESCENDING;
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

    private Pair<String, Database.SortOrder> sortMatch(Sort sort) {
        return sort(sort, Pair.of("similarity.similarity", Database.SortOrder.DESCENDING), s -> switch (s) {
            case "similarity" -> "similarity.similarity";
            case "sharedPeaks" -> "similarity.sharedPeaks";
            default -> s;
        });
    }

    private Pair<String, Database.SortOrder> sortFormulaCandidate(Sort sort) {
        return sort(sort, Pair.of("formulaRank", Database.SortOrder.ASCENDING), Function.identity());
    }

    private Pair<String, Database.SortOrder> sortStructureMatch(Sort sort) {
        return sort(sort, Pair.of("structureRank", Database.SortOrder.ASCENDING), Function.identity());
    }

    private Compound convertCompound(de.unijena.bioinf.ms.persistence.model.core.Compound compound,
                                     @NotNull EnumSet<Compound.OptField> optFields,
                                     @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields) {
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

        compound.getAdductFeatures().ifPresent(features -> builder.features(features.stream()
                .map(f -> convertToApiFeature(f, optFeatureFields)).toList()));

        return builder.build();
    }

    private de.unijena.bioinf.ms.persistence.model.core.Compound convertCompound(CompoundImport compoundImport) {
        List<AlignedFeatures> features = compoundImport.getFeatures().stream().map(this::convertToProjectFeature).toList();

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

    private AlignedFeatures convertToProjectFeature(FeatureImport featureImport) {

        AlignedFeatures.AlignedFeaturesBuilder<?, ?> builder = AlignedFeatures.builder()
                .name(featureImport.getName())
                .externalFeatureId(featureImport.getFeatureId())
                .averageMass(featureImport.getIonMass());

        MSData.MSDataBuilder msDataBuilder = MSData.builder();

        if (featureImport.getMergedMs1() != null) {
            SimpleSpectrum mergedMs1 = new SimpleSpectrum(featureImport.getMergedMs1().getMasses(), featureImport.getMergedMs1().getIntensities());
            msDataBuilder.mergedMs1Spectrum(mergedMs1);

            if (featureImport.getMs2Spectra() != null && !featureImport.getMs2Spectra().isEmpty()) {
                List<MutableMs2Spectrum> msnSpectra = new ArrayList<>();
                List<CollisionEnergy> ce = new ArrayList<>();
                DoubleList pmz = new DoubleArrayList();
                for (int i = 0; i < featureImport.getMs2Spectra().size(); i++) {
                    BasicSpectrum spectrum = featureImport.getMs2Spectra().get(i);
                    MutableMs2Spectrum mutableMs2 = new MutableMs2Spectrum(spectrum);
                    mutableMs2.setMsLevel(spectrum.getMsLevel());
                    if (spectrum.getScanNumber() != null) {
                        mutableMs2.setScanNumber(spectrum.getScanNumber());
                    }
                    if (spectrum.getCollisionEnergy() != null) {
                        mutableMs2.setCollisionEnergy(spectrum.getCollisionEnergy());
                        ce.add(spectrum.getCollisionEnergy());
                    }
                    if (spectrum.getPrecursorMz() != null) {
                        mutableMs2.setPrecursorMz(spectrum.getPrecursorMz());
                        pmz.add(spectrum.getPrecursorMz());
                    }
                    msnSpectra.add(mutableMs2);
                    msDataBuilder.msnSpectra(msnSpectra);

                    if (featureImport.getMs2Spectra().size() == 1) {
                        MergedMSnSpectrum mergedMSnSpectrum = MergedMSnSpectrum.of(new SimpleSpectrum(featureImport.getMs2Spectra().get(0)), ce.toArray(CollisionEnergy[]::new), null, pmz.toDoubleArray());
                        msDataBuilder.mergedMSnSpectrum(mergedMSnSpectrum);
                    } else {
                        List<MsMsQuerySpectrum> queries = featureImport.getMs2Spectra().stream().map(s -> new MsMsQuerySpectrum(
                                new Ms2SpectrumHeader("", 0, true, s.getCollisionEnergy(), null, 0, s.getPrecursorMz(), s.getPrecursorMz(), 0d), 0, new SimpleSpectrum(s), mergedMs1)
                        ).toList();
                        MergedSpectrum mergedMS2 = MergeGreedyStrategy.merge(queries);
                        MergedMSnSpectrum mergedMSnSpectrum = MergedMSnSpectrum.of(new SimpleSpectrum(mergedMS2), ce.toArray(CollisionEnergy[]::new), null, pmz.toDoubleArray());
                        msDataBuilder.mergedMSnSpectrum(mergedMSnSpectrum);
                    }
                }
            }
        }

        builder.msData(msDataBuilder.build());


        if (featureImport.getRtStartSeconds() != null && featureImport.getRtEndSeconds() != null) {
            builder.retentionTime(new RetentionTime(featureImport.getRtStartSeconds(), featureImport.getRtEndSeconds()));
        }

        if (featureImport.getAdduct() != null) {
            builder.ionType(PrecursorIonType.fromString(featureImport.getAdduct()));
        }
        return builder.build();
    }

    private AlignedFeature convertToApiFeature(AlignedFeatures features, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        final String fid = String.valueOf(features.getAlignedFeatureId());
        AlignedFeature.AlignedFeatureBuilder builder = AlignedFeature.builder()
                .alignedFeatureId(fid)
                .name(features.getName())
                .ionMass(features.getAverageMass())
                .computing(computeStateProvider.apply(this, fid));

        if (features.getIonType() != null)
            builder.adduct(features.getIonType().toString()); //is called adduct but refers to iontype (input setting) -> maybe rename

        RetentionTime rt = features.getRetentionTime();
        if (rt != null) {
            if (rt.isInterval() && Double.isFinite(rt.getStartTime()) && Double.isFinite(rt.getEndTime())) {
                builder.rtStartSeconds(rt.getStartTime());
                builder.rtEndSeconds(rt.getEndTime());
            } else {
                builder.rtStartSeconds(rt.getMiddleTime());
                builder.rtEndSeconds(rt.getMiddleTime());
            }
        }

        features.getMSData().map(this::convertMSData).ifPresent(builder::msData);

        if (optFields.contains(AlignedFeature.OptField.topAnnotations))
            builder.topAnnotations(extractTopCsiNovoAnnotations(features.getAlignedFeatureId()));
        if (optFields.contains(AlignedFeature.OptField.topAnnotationsDeNovo))
            builder.topAnnotationsDeNovo(extractTopDeNovoAnnotations(features.getAlignedFeatureId()));

        return builder.build();
    }

    private FeatureAnnotations extractTopCsiNovoAnnotations(long longAFIf) {
            return extractTopAnnotations(longAFIf, CsiStructureMatch.class);
    }
    private FeatureAnnotations extractTopDeNovoAnnotations(long longAFIf) {
        return extractTopAnnotations(longAFIf, DenovoStructureMatch.class);

    }
    private FeatureAnnotations extractTopAnnotations(long longAFIf, Class<? extends StructureMatch> clzz) {
        final FeatureAnnotations cSum = new FeatureAnnotations();

        StructureMatch structureMatch = project().findByFeatureIdStr(longAFIf, clzz)
                .findFirst().orElse(null);


        de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate formulaCandidate;
        if (structureMatch != null) {
            formulaCandidate = project().findByFormulaIdStr(structureMatch.getFormulaId(), de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class)
                    .findFirst().orElseThrow();

            //set Structure match
            cSum.setStructureAnnotation(convertStructureMatch(structureMatch, EnumSet.of(StructureCandidateScored.OptField.dbLinks, StructureCandidateScored.OptField.libraryMatches)));

            if (structureMatch instanceof CsiStructureMatch) //csi only but not denovo
                project().findByFeatureIdStr(longAFIf, CsiStructureSearchResult.class)
                        .findFirst().ifPresent(it -> {
                            cSum.setConfidenceExactMatch(it.getConfidenceExact());
                            cSum.setConfidenceApproxMatch(it.getConfidenceApprox());
                            cSum.setExpansiveSearchState(it.getExpansiveSearchConfidenceMode());
                            //todo add searched database and expanded databases
                        });
        } else {
            Pair<String, Database.SortOrder> formSort = sortFormulaCandidate(null); //null == default
            formulaCandidate = project().findByFeatureIdStr(longAFIf, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class, formSort.getLeft(), formSort.getRight())
                    .findFirst().orElse(null); //todo should we call a page of size one instead?
        }

        //get Canopus result. either for
        if (formulaCandidate != null) {
            cSum.setFormulaAnnotation(convertFormulaCandidate(formulaCandidate));
            if (structureMatch != null)
                cSum.getFormulaAnnotation().setTopCSIScore(structureMatch.getCsiScore());
            project().findByFormulaIdStr(formulaCandidate.getFormulaId(), de.unijena.bioinf.ms.persistence.model.sirius.CanopusPrediction.class)
                    .findFirst().map(cc -> CompoundClasses.of(cc.getNpcFingerprint(), cc.getCfFingerprint()))
                    .ifPresent(cSum::setCompoundClassAnnotation);
        }
        return cSum;
    }

    private MsData convertMSData(MSData msData) {
        MsData.MsDataBuilder builder = MsData.builder();
        if (msData.getMergedMs1Spectrum() != null)
            builder.mergedMs1(Spectrums.createMs1(msData.getMergedMs1Spectrum()));
        if (msData.getMergedMSnSpectrum() != null)
            builder.mergedMs2(Spectrums.createMergedMsMs(msData.getMergedMSnSpectrum()));
        if (msData.getMsnSpectra() != null)
            builder.ms2Spectra(msData.getMsnSpectra().stream().map(Spectrums::createMsMs).toList());
        return builder.build();
    }

    private static final EnumSet<FormulaCandidate.OptField> needTree = EnumSet.of(
            FormulaCandidate.OptField.fragmentationTree, FormulaCandidate.OptField.annotatedSpectrum,
            FormulaCandidate.OptField.isotopePattern, FormulaCandidate.OptField.lipidAnnotation,
            FormulaCandidate.OptField.statistics
    );

    private FormulaCandidate convertFormulaCandidate(de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate candidate) {
        return convertFormulaCandidate(null, candidate, EnumSet.noneOf(FormulaCandidate.OptField.class));
    }

    private FormulaCandidate convertFormulaCandidate(@Nullable MSData msData, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate candidate, EnumSet<FormulaCandidate.OptField> optFields) {
        final long fid = candidate.getFormulaId();
        FormulaCandidate.FormulaCandidateBuilder builder = FormulaCandidate.builder()
                .formulaId(String.valueOf(fid))
                .molecularFormula(candidate.getMolecularFormula().toString())
                .adduct(candidate.getAdduct().toString())
                .siriusScore(candidate.getSiriusScore())
                .isotopeScore(candidate.getIsotopeScore())
                .treeScore(candidate.getTreeScore())
                .zodiacScore(candidate.getZodiacScore());

        //todo post 6.0: we need the scores in the gui without the tree -> do we want to store stats separately from the tree?
        final FTree ftree = optFields.stream().anyMatch(needTree::contains)
                ? project().findByFormulaIdStr(fid, FTreeResult.class).findFirst().map(FTreeResult::getFTree).orElse(null)
                : null;

        if (ftree != null) {
            if (optFields.contains(FormulaCandidate.OptField.statistics)) {
                FTreeMetricsHelper scores = new FTreeMetricsHelper(ftree);
                builder.numOfExplainablePeaks(scores.getNumberOfExplainablePeaks())
                        .numOfExplainedPeaks(scores.getNumOfExplainedPeaks())
                        .totalExplainedIntensity(scores.getExplainedIntensityRatio())
                        .medianMassDeviation(scores.getMedianMassDeviation());
            }
            if (optFields.contains(FormulaCandidate.OptField.fragmentationTree))
                builder.fragmentationTree(FragmentationTree.fromFtree(ftree));
            if (optFields.contains(FormulaCandidate.OptField.lipidAnnotation))
                builder.lipidAnnotation(AnnotationUtils.asLipidAnnotation(ftree));
            if (optFields.contains(FormulaCandidate.OptField.annotatedSpectrum))
                //todo this is not efficient an loads spectra a second time as well as the whole experiment. we need no change spectra annotation code to improve this.
                builder.annotatedSpectrum(findAnnotatedMsMsSpectrum(-1, null, candidate.getFormulaId(), candidate.getAlignedFeatureId()));
            if (msData != null && optFields.contains(FormulaCandidate.OptField.isotopePattern)) {
                SimpleSpectrum isotopePattern = msData.getIsotopePattern();
                if (isotopePattern != null) {
                    builder.isotopePatternAnnotation(Spectrums.createIsotopePatternAnnotation(isotopePattern, ftree));
                }
            }
        }


        if (optFields.contains(FormulaCandidate.OptField.predictedFingerprint))
            project().findByFormulaIdStr(fid, CsiPrediction.class).findFirst()
                    .map(fpp -> fpp.getFingerprint().toProbabilityArray()).ifPresent(builder::predictedFingerprint);


        if (optFields.contains(FormulaCandidate.OptField.canopusPredictions)) {
            project().findByFormulaIdStr(fid, de.unijena.bioinf.ms.persistence.model.sirius.CanopusPrediction.class)
                    .findFirst().ifPresent(cr -> {
                        if (optFields.contains(FormulaCandidate.OptField.canopusPredictions))
                            builder.canopusPrediction(CanopusPrediction.of(cr.getNpcFingerprint(), cr.getCfFingerprint()));
                        if (optFields.contains(FormulaCandidate.OptField.compoundClasses))
                            builder.compoundClasses(CompoundClasses.of(cr.getNpcFingerprint(), cr.getCfFingerprint()));
                    });
        }
        return builder.build();

    }

    @SneakyThrows
    @Override
    public Page<Compound> findCompounds(Pageable pageable,
                                        @NotNull EnumSet<Compound.OptField> optFields,
                                        @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields) {
        Pair<String, Database.SortOrder> sort = sortCompound(pageable.getSort());
        Stream<de.unijena.bioinf.ms.persistence.model.core.Compound> stream;
        if (pageable.isPaged()) {
            stream = storage().findAllStr(de.unijena.bioinf.ms.persistence.model.core.Compound.class, pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight());
        } else {
            stream = storage().findAllStr(de.unijena.bioinf.ms.persistence.model.core.Compound.class, sort.getLeft(), sort.getRight());
        }
        stream = stream.peek(project()::fetchAdductFeatures);

        if (optFeatureFields.contains(AlignedFeature.OptField.msData)) {
            stream = stream.peek(c -> c.getAdductFeatures().ifPresent(features -> features.forEach(project()::fetchMsData)));
        }

        List<Compound> compounds = stream.map(c -> convertCompound(c, optFields, optFeatureFields)).toList();

        // TODO annotations
        long total = totalCounts.get(de.unijena.bioinf.ms.persistence.model.core.Compound.class).get();

        return new PageImpl<>(compounds, pageable, total);
    }

    @SneakyThrows
    @Override
    public List<Compound> addCompounds(@NotNull List<CompoundImport> compounds, @NotNull EnumSet<Compound.OptField> optFields, @NotNull EnumSet<AlignedFeature.OptField> optFieldsFeatures) {
        List<de.unijena.bioinf.ms.persistence.model.core.Compound> dbc = compounds.stream().map(this::convertCompound).toList();
        project().importCompounds(dbc);
        return dbc.stream().map(c -> convertCompound(c, optFields, optFieldsFeatures)).toList();
    }

    @SneakyThrows
    @Override
    public Compound findCompoundById(String compoundId, @NotNull EnumSet<Compound.OptField> optFields, @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields) {
        long id = Long.parseLong(compoundId);
        return storage().getByPrimaryKey(id, de.unijena.bioinf.ms.persistence.model.core.Compound.class)
                .map(c -> {
                    project().fetchAdductFeatures(c);
                    if (optFeatureFields.contains(AlignedFeature.OptField.msData)) {
                        c.getAdductFeatures().ifPresent(features -> features.forEach(project()::fetchMsData));
                    }
                    return convertCompound(c, optFields, optFeatureFields);
                })
                // TODO annotations
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no compound '" + compoundId + "' in project " + projectId + "."));
    }

    @SneakyThrows
    @Override
    public void deleteCompoundById(String compoundId) {
        project().cascadeDeleteCompound(Long.parseLong(compoundId));
    }

    @Override
    public Page<AlignedFeatureQuality> findAlignedFeaturesQuality(Pageable pageable, @NotNull EnumSet<AlignedFeatureQuality.OptField> optFields) {
        // TODO POST 6.0?
        return null;
    }

    @Override
    public AlignedFeatureQuality findAlignedFeaturesQualityById(String alignedFeatureId, @NotNull EnumSet<AlignedFeatureQuality.OptField> optFields) {
        // TODO POST 6.0?
        return null;
    }

    @SneakyThrows
    @Override
    public Page<AlignedFeature> findAlignedFeatures(Pageable pageable, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        Stream<AlignedFeatures> stream;
        if (pageable.isUnpaged() && pageable.getSort().isUnsorted()) {
            stream = storage().findAllStr(AlignedFeatures.class);
        } else {
            Pair<String, Database.SortOrder> sort = sortFeature(pageable.getSort());
            stream = storage().findAllStr(AlignedFeatures.class, pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight());
        }

        if (optFields.contains(AlignedFeature.OptField.msData))
            stream = stream.peek(project()::fetchMsData);


        List<AlignedFeature> features = stream.map(alf -> convertToApiFeature(alf, optFields)).toList();

        long total = totalCounts.get(AlignedFeatures.class).get();

        return new PageImpl<>(features, pageable, total);
    }

    @Override
    public List<AlignedFeature> addAlignedFeatures(@NotNull List<FeatureImport> features, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        LongestCommonSubsequence lcs = new LongestCommonSubsequence();
        String name = features.stream().map(FeatureImport::getName).reduce((a, b) -> lcs.longestCommonSubsequence(a, b).toString()).orElse("");
        if (name.isBlank())
            name = "Compound";

        CompoundImport ci = CompoundImport.builder().name(name).features(features).build();
        Compound compound = addCompounds(List.of(ci), EnumSet.of(Compound.OptField.none), optFields).stream().findFirst().orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compound could not be imported to " + projectId + ".")
        );
        return compound.getFeatures();
    }

    @SneakyThrows
    @Override
    public AlignedFeature findAlignedFeaturesById(String alignedFeatureId, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        long id = Long.parseLong(alignedFeatureId);
        return storage().getByPrimaryKey(id, AlignedFeatures.class)
                .map(a -> {
                    if (optFields.contains(AlignedFeature.OptField.msData)) {
                        project().fetchMsData(a);
                    }
                    return convertToApiFeature(a, optFields);
                }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no aligned feature '" + alignedFeatureId + "' in project " + projectId + "."));
    }

    @SneakyThrows
    @Override
    public void deleteAlignedFeaturesById(String alignedFeatureId) {
        project().cascadeDeleteAlignedFeatures(Long.parseLong(alignedFeatureId));
    }

    @SneakyThrows
    @Override
    public Page<SpectralLibraryMatch> findLibraryMatchesByFeatureId(String alignedFeatureId, Pageable pageable) {
        long longId = Long.parseLong(alignedFeatureId);
        Pair<String, Database.SortOrder> sort = sortMatch(pageable.getSort());
        List<SpectralLibraryMatch> matches;
        if (pageable.isPaged()) {
            matches = project().findByFeatureIdStr(longId, SpectraMatch.class, pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight())
                    .map(SpectraMatch::getSearchResult).map(SpectralLibraryMatch::of).toList();
        } else {
            matches = project().findByFeatureIdStr(longId, SpectraMatch.class, sort.getLeft(), sort.getRight())
                    .map(SpectraMatch::getSearchResult).map(SpectralLibraryMatch::of).toList();
        }
        long total = totalCountByFeature.get(SpectraMatch.class).getOrDefault(longId, ATOMIC_ZERO).get();
        return new PageImpl<>(matches, pageable, total);
    }

    @SneakyThrows
    @Override
    public Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<FormulaCandidate.OptField> optFields) {
        long longAFId = Long.parseLong(alignedFeatureId);
        Pair<String, Database.SortOrder> sort = sortFormulaCandidate(pageable.getSort());

        //load ms data only once per formula candidate
        final MSData msData = Stream.of(/*FormulaCandidate.OptField.annotatedSpectrum,*/ FormulaCandidate.OptField.isotopePattern).anyMatch(optFields::contains)
                ? project().findByFeatureIdStr(longAFId, MSData.class).findFirst().orElse(null) : null;

        List<FormulaCandidate> candidates;
        if (pageable.isPaged()) {
            candidates = project().findByFeatureIdStr(longAFId, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class, pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight())
                    .map(fc -> convertFormulaCandidate(msData, fc, optFields)).toList();
        } else {
            candidates = project().findByFeatureIdStr(longAFId, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class, sort.getLeft(), sort.getRight())
                    .map(fc -> convertFormulaCandidate(msData, fc, optFields)).toList();
        }
        long total = totalCountByFeature.get(de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class).getOrDefault(longAFId, ATOMIC_ZERO).get();

        return new PageImpl<>(candidates, pageable, total);
    }

    @SneakyThrows
    @Override
    public FormulaCandidate findFormulaCandidateByFeatureIdAndId(String formulaId, String alignedFeatureId, @NotNull EnumSet<FormulaCandidate.OptField> optFields) {
        long longFId = Long.parseLong(formulaId);
        long longAFId = Long.parseLong(alignedFeatureId);

        final MSData msData = Stream.of(/*FormulaCandidate.OptField.annotatedSpectrum,*/ FormulaCandidate.OptField.isotopePattern).anyMatch(optFields::contains)
                ? project().findByFeatureIdStr(longAFId, MSData.class).findFirst().orElse(null) : null;

        return project().findByFormulaIdStr(longFId, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class)
                .peek(fc -> {
                    if (fc.getAlignedFeatureId() != longAFId)
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formula candidate exists but FormulaID does not belong to the requested FeatureID. Are you using the correct Ids?");
                }).map(fc -> convertFormulaCandidate(msData, fc, optFields)).findFirst().orElse(null);
    }

    @Override
    public Page<StructureCandidateScored> findStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return findStructureCandidatesByFeatureIdAndFormulaId(CsiStructureMatch.class, formulaId, alignedFeatureId, pageable, optFields);
    }

    @Override
    public Page<StructureCandidateScored> findDeNovoStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return findStructureCandidatesByFeatureIdAndFormulaId(DenovoStructureMatch.class, formulaId, alignedFeatureId, pageable, optFields);
    }

    private <T extends StructureMatch> Page<StructureCandidateScored> findStructureCandidatesByFeatureIdAndFormulaId(Class<T> clzz, String formulaId, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        long longAFId = Long.parseLong(alignedFeatureId);
        long longFId = Long.parseLong(formulaId);
        Pair<String, Database.SortOrder> sort = sortStructureMatch(pageable.getSort());
        List<StructureCandidateScored> candidates = project().findByFeatureIdAndFormulaIdStr(longAFId, longFId, clzz, pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight())
                .map(s -> convertStructureMatch(s, optFields)).map(s -> (StructureCandidateScored) s).toList();

        long total = totalCountByFeature.get(clzz).getOrDefault(longAFId, ATOMIC_ZERO).get();

        return new PageImpl<>(candidates, pageable, total);
    }


    @Override
    public Page<StructureCandidateFormula> findStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return findStructureCandidatesByFeatureId(CsiStructureMatch.class, alignedFeatureId, pageable, optFields);
    }

    @Override
    public Page<StructureCandidateFormula> findDeNovoStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return findStructureCandidatesByFeatureId(DenovoStructureMatch.class, alignedFeatureId, pageable, optFields);
    }

    private <T extends StructureMatch> Page<StructureCandidateFormula> findStructureCandidatesByFeatureId(Class<T> clzz, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        long longAFId = Long.parseLong(alignedFeatureId);
        Pair<String, Database.SortOrder> sort = sortStructureMatch(pageable.getSort());

        Long2ObjectMap<de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate> fidToFC = new Long2ObjectOpenHashMap<>();

        List<StructureCandidateFormula> candidates = project().findByFeatureIdStr(longAFId, clzz, pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight())
                .map(candidate -> {
                    de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate fc = fidToFC
                            .computeIfAbsent(candidate.getFormulaId(), k -> project()
                                    .findByFormulaIdStr(k, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class)
                                    .findFirst().orElseThrow());
                    return convertStructureMatch(fc.getMolecularFormula(), fc.getAdduct(), candidate, optFields);
                }).toList();

        long total = totalCountByFeature.get(DenovoStructureMatch.class).getOrDefault(longAFId, ATOMIC_ZERO).get();

        return new PageImpl<>(candidates, pageable, total);
    }


    @Override
    public StructureCandidateScored findTopStructureCandidateByFeatureId(String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        long longAFId = Long.parseLong(alignedFeatureId);
        Pair<String, Database.SortOrder> sort = sortStructureMatch(Sort.by(Sort.Direction.DESC, "csiScore"));
        return project().findByFeatureIdStr(longAFId, CsiStructureMatch.class, sort.getLeft(), sort.getRight())
                .findFirst().map(s -> convertStructureMatch(s, optFields)).orElse(null);
    }

    @Override
    public StructureCandidateScored findStructureCandidateById(@NotNull String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        long longAFId = Long.parseLong(alignedFeatureId);
        long longFId = Long.parseLong(formulaId);
        CsiStructureMatch match = project().findByFeatureIdAndFormulaIdAndInChIStr(longAFId, longFId, inchiKey, CsiStructureMatch.class)
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Structure Candidate with InChIKey: " + inchiKey + "| formulaId: " + formulaId + "| alignedFeatureId: " + alignedFeatureId + " could not be found!"));
        return convertStructureMatch(match, optFields);
    }

    private StructureCandidateFormula convertStructureMatch(MolecularFormula molecularFormula, PrecursorIonType adduct, StructureMatch match, EnumSet<StructureCandidateScored.OptField> optFields) {
        StructureCandidateFormula sSum = convertStructureMatch(match, optFields);
        if (molecularFormula != null)
            sSum.setMolecularFormula(molecularFormula.toString());
        if (adduct != null)
            sSum.setAdduct(adduct.toString());
        return sSum;
    }

    private StructureCandidateFormula convertStructureMatch(StructureMatch match, EnumSet<StructureCandidateScored.OptField> optFields) {
        final StructureCandidateFormula sSum = new StructureCandidateFormula();
        //FP
        if (match.getCandidate() == null)
            project().fetchFingerprintCandidate(match, optFields.contains(StructureCandidateScored.OptField.fingerprint));

        if (optFields.contains(StructureCandidateScored.OptField.fingerprint))
            sSum.setFingerprint(AnnotationUtils.asBinaryFingerprint(match.getCandidate().getFingerprint()));

        sSum.setFormulaId(String.valueOf(match.getFormulaId()));
        // scores
        sSum.setCsiScore(match.getCsiScore());
        sSum.setTanimotoSimilarity(match.getTanimotoSimilarity());

        if (match instanceof CsiStructureMatch csi)
            sSum.setMcesDistToTopHit(csi.getMcesDistToTopHit());
//        else if (match instanceof DenovoStructureMatch mn)
        //todo do we want to add dnn score for denovo?


        //Structure information
        //check for "null" strings since the database might not be perfectly curated
        final String n = match.getCandidate().getName();
        if (n != null && !n.isEmpty() && !n.equals("null"))
            sSum.setStructureName(n);

        sSum.setSmiles(match.getCandidate().getSmiles());
        sSum.setInchiKey(match.getCandidateInChiKey());
        sSum.setXlogP(match.getCandidate().getXlogp());

        //meta data
        if (optFields.contains(StructureCandidateScored.OptField.dbLinks))
            sSum.setDbLinks(match.getCandidate().getLinks());

        // spectral library matches
        if (optFields.contains(StructureCandidateScored.OptField.libraryMatches)) {
            List<SpectralLibraryMatch> libraryMatches = project().findByInChIStr(sSum.getInchiKey(), SpectraMatch.class)
                    .map(SpectraMatch::getSearchResult).map(SpectralLibraryMatch::of).toList();
            sSum.setSpectralLibraryMatches(libraryMatches);
        }

        return sSum;
    }

    @Override
    public AnnotatedSpectrum findAnnotatedSpectrumByStructureId(int specIndex, @Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId) {
        long longFId = Long.parseLong(formulaId);
        long longAFId = Long.parseLong(alignedFeatureId);
        return findAnnotatedMsMsSpectrum(specIndex, inchiKey, longFId, longAFId);
    }

    @SneakyThrows
    private AnnotatedSpectrum findAnnotatedMsMsSpectrum(int specIndex, @Nullable String inchiKey, long formulaId, long alignedFeatureId) {
        //todo we want to do this without ms2 experiment
        Ms2Experiment exp = project().findAlignedFeatureAsMsExperiment(alignedFeatureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not load ms data needed to create annotated spectrum for id: " + alignedFeatureId));

        FTree ftree = project().findByFormulaIdStr(formulaId, FTreeResult.class).findFirst().map(FTreeResult::getFTree)
                .orElse(null);

        //todo we retrieve the complete candidate just for the smile. Maybe add smiles to match?
        String smiles = storage().getByPrimaryKey(inchiKey, FingerprintCandidate.class)
                .map(CompoundCandidate::getSmiles)
                .orElse(null);

        if (specIndex < 0)
            return Spectrums.createMergedMsMsWithAnnotations(exp, ftree, smiles);
        else
            return Spectrums.createMsMsWithAnnotations(exp.getMs2Spectra().get(specIndex), ftree, smiles);
    }

    @SneakyThrows
    @Override
    public AnnotatedMsMsData findAnnotatedMsMsDataByStructureId(@Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId) {
        long longFId = Long.parseLong(formulaId);
        long longAFId = Long.parseLong(alignedFeatureId);

        //todo we want to do this without ms2 experiment
        Ms2Experiment exp = project().findAlignedFeatureAsMsExperiment(longAFId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not load ms data needed to create annotated spectrum for id: " + alignedFeatureId));

        FTree ftree = project().findByFormulaIdStr(longFId, FTreeResult.class).findFirst().map(FTreeResult::getFTree)
                .orElse(null);

        //todo we retrieve the complete candidate just for the smile. Maybe add smiles to match?
        String smiles = storage().getByPrimaryKey(inchiKey, FingerprintCandidate.class)
                .map(CompoundCandidate::getSmiles)
                .orElse(null);

        return AnnotatedMsMsData.of(exp, ftree, smiles);
    }

    @SneakyThrows
    @Override
    public String getFingerIdDataCSV(int charge) {
        Optional<FingerIdData> dataOpt = projectSpaceManager.getProject().findFingerprintData(FingerIdData.class, charge);
        if (dataOpt.isEmpty())
            return null;
        StringWriter writer = new StringWriter();
        FingerIdData.write(writer, dataOpt.get()); //sneaky throws because it's a string writer and no real io.
        return writer.toString();
    }

    @SneakyThrows
    @Override
    public String getCanopusClassyFireDataCSV(int charge) {
        Optional<CanopusCfData> dataOpt = projectSpaceManager.getProject().findFingerprintData(CanopusCfData.class, charge);
        if (dataOpt.isEmpty())
            return null;
        StringWriter writer = new StringWriter();
        CanopusCfData.write(writer, dataOpt.get()); //sneaky throws because it's a string writer and no real io.
        return writer.toString();
    }

    @SneakyThrows
    @Override
    public String getCanopusNpcDataCSV(int charge) {
        Optional<CanopusNpcData> dataOpt = projectSpaceManager.getProject().findFingerprintData(CanopusNpcData.class, charge);
        if (dataOpt.isEmpty())
            return null;
        StringWriter writer = new StringWriter();
        CanopusNpcData.write(writer, dataOpt.get()); //sneaky throws because it's a string writer and no real io.
        return writer.toString();
    }

    @SneakyThrows
    @Override
    public String findSiriusFtreeJsonById(String formulaId, String alignedFeatureId) {
        long formId = Long.parseLong(formulaId);
        return project().findByFormulaIdStr(formId, FTreeResult.class).findFirst()
                .map(ftreeRes -> {
                    if (ftreeRes.getAlignedFeatureId() != Long.parseLong(alignedFeatureId))
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tree exists but FormulaID does not belong to the requested FeatureID. Are you using the correct Ids?");
                    return new FTJsonWriter().treeToJsonString(ftreeRes.getFTree());
                }).orElse(null);
    }
}
