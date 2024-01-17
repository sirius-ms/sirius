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

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.FBCandidateFingerprints;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.lcms.LCMSCompoundSummary;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.middleware.controller.AlignedFeatureController;
import de.unijena.bioinf.ms.middleware.model.annotations.*;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeatureQuality;
import de.unijena.bioinf.ms.middleware.model.features.LCMSFeatureQuality;
import de.unijena.bioinf.ms.middleware.model.features.MsData;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.projectspace.canopus.CanopusCfDataProperty;
import de.unijena.bioinf.projectspace.canopus.CanopusNpcDataProperty;
import de.unijena.bioinf.projectspace.fingerid.FBCandidateNumber;
import de.unijena.bioinf.projectspace.fingerid.FBCandidatesTopK;
import de.unijena.bioinf.projectspace.fingerid.FingerIdDataProperty;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class SiriusProjectSpaceImpl implements Project {

    @NotNull
    private final ProjectSpaceManager<?> projectSpaceManager;
    @NotNull
    private final String projectId;

    public SiriusProjectSpaceImpl(@NotNull String projectId, @NotNull ProjectSpaceManager<?> projectSpaceManager) {
        this.projectSpaceManager = projectSpaceManager;
        this.projectId = projectId;
    }


    @NotNull
    @Override
    public String getProjectId() {
        return projectId;
    }

    public @NotNull ProjectSpaceManager<?> getProjectSpaceManager() {
        return projectSpaceManager;
    }


    @Override
    public Page<Compound> findCompounds(Pageable pageable, @NotNull EnumSet<Compound.OptField> optFields,
                                        @NotNull EnumSet<AlignedFeature.OptField> featureOptFields) {
        Map<String, List<CompoundContainerId>> featureGroups = projectSpaceManager.projectSpace()
                .stream().filter(c -> c.getGroupId().isPresent())
                .collect(Collectors.groupingBy(c -> c.getGroupId().get()));

        List<Compound> compounds = featureGroups.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .skip(pageable.getOffset()).limit(pageable.getPageSize())
                .map(e -> asCompound(e.getValue(), optFields, featureOptFields))
                .toList();

        return new PageImpl<>(compounds, pageable, featureGroups.size());
    }

    @Override
    public Compound findCompoundById(String compoundId, @NotNull EnumSet<Compound.OptField> optFields,
                                     @NotNull EnumSet<AlignedFeature.OptField> featureOptFields) {
        List<CompoundContainerId> groupFeatures = projectSpaceManager.projectSpace()
                .stream().filter(c -> c.getGroupId().map(compoundId::equals).orElse(false))
                .toList();
        if (groupFeatures.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No Features found that belong to Compound with id '" + compoundId + "'. Compound does not exist.");

        return asCompound(groupFeatures, optFields, featureOptFields);
    }

    @Override
    public void deleteCompoundById(String compoundId) {
        Compound compound;
        try {
            compound = findCompoundById(compoundId, EnumSet.noneOf(Compound.OptField.class), EnumSet.noneOf(AlignedFeature.OptField.class));
        } catch (ResponseStatusException e) {
            if (e.getStatus().equals(HttpStatus.NOT_FOUND))
                throw new ResponseStatusException(HttpStatus.NO_CONTENT, "AlignedFeature with id '" + compoundId + "' does not exist. Already removed?");
            throw e;
        }
        compound.getFeatures().forEach(f -> deleteAlignedFeaturesById(f.getAlignedFeatureId()));
    }

    @Override
    public Page<AlignedFeatureQuality> findAlignedFeaturesQuality(Pageable pageable, @NotNull EnumSet<AlignedFeatureQuality.OptField> optFields) {
        LoggerFactory.getLogger(AlignedFeatureController.class).info("Started collecting aligned features quality...");
        final List<AlignedFeatureQuality> alignedFeatureQualities = projectSpaceManager.projectSpace().stream()
                .skip(pageable.getOffset()).limit(pageable.getPageSize())
                .map(ccid -> asAlignedFeatureQuality(ccid, optFields))
                .toList();
        LoggerFactory.getLogger(AlignedFeatureController.class).info("Finished parsing aligned features quality...");

        return new PageImpl<>(alignedFeatureQualities, pageable, projectSpaceManager.size());
    }

    @Override
    public AlignedFeatureQuality findAlignedFeaturesQualityById(String alignedFeatureId, @NotNull EnumSet<AlignedFeatureQuality.OptField> optFields) {
        final CompoundContainerId ccid = parseCID(alignedFeatureId);
        return asAlignedFeatureQuality(ccid, optFields);
    }

    @Override
    public Page<AlignedFeature> findAlignedFeatures(Pageable pageable, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        LoggerFactory.getLogger(AlignedFeatureController.class).info("Started collecting aligned features...");
        final List<AlignedFeature> alignedFeatures = projectSpaceManager.projectSpace().stream()
                .skip(pageable.getOffset()).limit(pageable.getPageSize())
                .map(ccid -> asAlignedFeature(ccid, optFields))
                .toList();
        LoggerFactory.getLogger(AlignedFeatureController.class).info("Finished parsing aligned features...");

        return new PageImpl<>(alignedFeatures, pageable, projectSpaceManager.size());
    }

    @Override
    public AlignedFeature findAlignedFeaturesById(String alignedFeatureId, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        final CompoundContainerId ccid = parseCID(alignedFeatureId);
        return asAlignedFeature(ccid, optFields);
    }

    @Override
    public void deleteAlignedFeaturesById(String alignedFeatureId) {
        CompoundContainerId compound = projectSpaceManager.projectSpace().findCompound(alignedFeatureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT, "AlignedFeature with id '" + alignedFeatureId + "' does not exist. Already removed?"));
        try {
            projectSpaceManager.projectSpace().deleteCompound(compound);
        } catch (IOException e) {
            log.error("Error when deleting feature with Id " + alignedFeatureId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error when deleting feature with Id " + alignedFeatureId);
        }
    }

    @Override
    public Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<FormulaCandidate.OptField> optFields) {
        LoggerFactory.getLogger(getClass()).info("Started collecting formulas...");
        Class<? extends DataAnnotation>[] annotations = resolveFormulaCandidateAnnotations(optFields);
        Instance instance = loadInstance(alignedFeatureId);
        Stream<FormulaResult> paged;
        int size;
        {
            List<? extends SScored<FormulaResult, ? extends FormulaScore>> tmpSource = instance.loadFormulaResults();
            paged = tmpSource.stream()
                    .skip(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .map(SScored::getCandidate);
            size = tmpSource.size();
        }

        return new PageImpl<>(
                paged.peek(fr -> instance.loadFormulaResult(fr.getId(), annotations).ifPresent(fr::setAnnotationsFrom))
                        .map(res -> makeFormulaCandidate(instance, res, optFields))
                        .map(FormulaCandidate.FormulaCandidateBuilder::build)
                        .toList(), pageable, size);
    }

    @Override
    public FormulaCandidate findFormulaCandidateByFeatureIdAndId(String formulaId, String alignedFeatureId, @NotNull EnumSet<FormulaCandidate.OptField> optFields) {
        Class<? extends DataAnnotation>[] annotations = resolveFormulaCandidateAnnotations(optFields);
        Instance instance = loadInstance(alignedFeatureId);
        return instance.loadFormulaResult(parseFID(instance, formulaId), annotations)
                .map(res -> makeFormulaCandidate(instance, res, optFields))
                .map(FormulaCandidate.FormulaCandidateBuilder::build)
                .orElse(null);
    }

    @Override
    public Page<StructureCandidateScored> findStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        List<Class<? extends DataAnnotation>> para = (optFields.contains(StructureCandidateScored.OptField.fingerprint)
                ? List.of(FormulaScoring.class, FBCandidatesTopK.class, FBCandidateFingerprints.class)
                : List.of(FormulaScoring.class, FBCandidatesTopK.class));

        Instance instance = loadInstance(alignedFeatureId);
        FormulaResultId fidObj = parseFID(instance, formulaId);
        return loadStructureCandidates(instance, fidObj, pageable, para, optFields)
                .map(l -> l.stream().map(c -> (StructureCandidateScored) c).toList())
                .map(it -> (Page<StructureCandidateScored>) new PageImpl<>(it, pageable, Long.MAX_VALUE))
                .orElse(Page.empty(pageable)); //todo number of candidates for page.
    }

    @Override
    public Page<StructureCandidateFormula> findStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        List<Class<? extends DataAnnotation>> para = (optFields.contains(StructureCandidateScored.OptField.fingerprint)
                ? List.of(FormulaScoring.class, FBCandidatesTopK.class, FBCandidateFingerprints.class)
                : List.of(FormulaScoring.class, FBCandidatesTopK.class));

        Instance instance = loadInstance(alignedFeatureId);
        List<StructureCandidateFormula> candidates = instance.loadFormulaResults(FormulaScoring.class).stream()
                .filter(fr -> fr.getCandidate().getAnnotation(FormulaScoring.class)
                        .flatMap(s -> s.getAnnotation(TopCSIScore.class)).isPresent())
                .map(fr -> fr.getCandidate().getId())
                .map(fid -> loadStructureCandidates(instance, fid, pageable, para, optFields))
                .filter(Optional::isPresent).flatMap(Optional::stream).flatMap(List::stream)
                .sorted(Comparator.comparing(StructureCandidateScored::getCsiScore).reversed())
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize()).toList();

        return new PageImpl<>(candidates, pageable, Long.MAX_VALUE);  //todo number of candidates for page.
    }

    @Override
    public StructureCandidateFormula findTopStructureCandidateByFeatureId(String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        List<Class<? extends DataAnnotation>> para = (optFields.contains(StructureCandidateScored.OptField.fingerprint)
                ? List.of(FormulaScoring.class, FBCandidatesTopK.class, FBCandidateFingerprints.class)
                : List.of(FormulaScoring.class, FBCandidatesTopK.class));

        Instance instance = loadInstance(alignedFeatureId);

        return instance.loadTopFormulaResult(List.of(TopCSIScore.class)).flatMap(fr -> {
            fr.getId().setAnnotation(FBCandidateNumber.class, new FBCandidateNumber(1));
            return instance.loadFormulaResult(fr.getId(), (Class<? extends DataAnnotation>[]) para.toArray(Class[]::new))
                    .flatMap(fr2 -> fr2.getAnnotation(FBCandidatesTopK.class).map(FBCandidatesTopK::getResults)
                            .filter(l -> !l.isEmpty()).map(r -> r.get(0))
                            .map(sc -> StructureCandidateFormula.of(sc,
                                    fr2.getAnnotation(FBCandidateFingerprints.class)
                                            .map(FBCandidateFingerprints::getFingerprints)
                                            .map(fps -> fps.isEmpty() ? null : fps.get(0))
                                            .orElse(null),
                                    fr.getAnnotationOrThrow(FormulaScoring.class), optFields, fr.getId()))
                    );
        }).orElseThrow();
    }

    private AlignedFeature asAlignedFeature(CompoundContainerId cid, EnumSet<AlignedFeature.OptField> optFields) {
        final AlignedFeature alignedFeature = asAlignedFeature(cid);
        if (!optFields.isEmpty()) {
            Instance instance = projectSpaceManager.getInstanceFromCompound(cid);
            if (optFields.contains(AlignedFeature.OptField.topAnnotations))
                alignedFeature.setTopAnnotations(extractTopAnnotations(instance));
            if (optFields.contains(AlignedFeature.OptField.topAnnotationsDeNovo))
                alignedFeature.setTopAnnotationsDeNovo(extractTopAnnotationsDeNovo(instance));
            if (optFields.contains(AlignedFeature.OptField.msData))
                alignedFeature.setMsData(asCompoundMsData(instance));
        }
        return alignedFeature;
    }

    private AlignedFeatureQuality asAlignedFeatureQuality(CompoundContainerId cid, EnumSet<AlignedFeatureQuality.OptField> optFields) {
        final AlignedFeatureQuality.AlignedFeatureQualityBuilder builder = AlignedFeatureQuality.builder()
                .alignedFeatureId(cid.getDirectoryName());
        if (!optFields.isEmpty()) {
            Instance instance = projectSpaceManager.getInstanceFromCompound(cid);
            if (optFields.contains(AlignedFeatureQuality.OptField.lcmsFeatureQuality))
                builder.lcmsFeatureQuality(asCompoundLCMSFeatureQuality(instance));
            if (optFields.contains(AlignedFeatureQuality.OptField.qualityFlags))
                builder.qualityFlags(asCompoundQualityData(instance));
        }
        return builder.build();
    }

    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#0.000");

    private Compound asCompound(List<CompoundContainerId> cids, @NotNull EnumSet<Compound.OptField> optFields,
                                @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields) {
        //compound with ID
        Compound.CompoundBuilder c = Compound.builder()
                .compoundId(cids.stream().map(CompoundContainerId::getGroupId)
                        .filter(Optional::isPresent).flatMap(Optional::stream).findFirst().orElseThrow());

        {
            // merge optional field config
            final EnumSet<AlignedFeature.OptField> mergedFeatureFields = EnumSet.copyOf(optFeatureFields);
            if (optFields.contains(Compound.OptField.consensusAnnotations))
                mergedFeatureFields.add(AlignedFeature.OptField.topAnnotations);
            if (optFields.contains(Compound.OptField.consensusAnnotationsDeNovo))
                mergedFeatureFields.add(AlignedFeature.OptField.topAnnotationsDeNovo);

            // features
            List<AlignedFeature> features = cids.stream().map(cid -> asAlignedFeature(cid, mergedFeatureFields)).toList();
            c.features(features);

            if (optFields.contains(Compound.OptField.consensusAnnotations))
                c.consensusAnnotations(AnnotationUtils.buildConsensusAnnotationsCSI(features));
            if (optFields.contains(Compound.OptField.consensusAnnotationsDeNovo))
                c.consensusAnnotationsDeNovo(AnnotationUtils.buildConsensusAnnotationsDeNovo(features));
            if (optFields.contains(Compound.OptField.customAnnotations))
                c.customAnnotations(ConsensusAnnotationsCSI.builder().build()); //todo implement custom annotations -> storage needed

            //remove optionals if not requested
            if (!optFeatureFields.contains(AlignedFeature.OptField.topAnnotations))
                features.forEach(f -> f.setTopAnnotations(null));
            if (!optFeatureFields.contains(AlignedFeature.OptField.topAnnotationsDeNovo))
                features.forEach(f -> f.setTopAnnotationsDeNovo(null));
        }

        //compound RT
        RetentionTime rt = cids.stream().map(CompoundContainerId::getGroupRt)
                .filter(Optional::isPresent).flatMap(Optional::stream).filter(RetentionTime::isInterval).findFirst()
                .orElse(cids.stream().map(CompoundContainerId::getRt).filter(Optional::isPresent)
                        .flatMap(Optional::stream).reduce(RetentionTime::merge).orElse(null)
                );

        if (rt != null) {
            if (rt.isInterval()) {
                c.rtStartSeconds(rt.getStartTime());
                c.rtEndSeconds(rt.getEndTime());
            } else {
                c.rtStartSeconds(rt.getMiddleTime());
                c.rtEndSeconds(rt.getMiddleTime());
            }
        }

        //neutral mass
        DoubleArrayList neutralMasses = cids.stream()
                .filter(cid -> cid.getIonType().map(p -> !p.isIonizationUnknown()).orElse(false))
                .map(cid -> cid.getIonMass().map(m -> cid.getIonType().get().precursorMassToNeutralMass(m)))
                .flatMap(Optional::stream).sorted().collect(Collectors.toCollection(DoubleArrayList::new));

        if (neutralMasses.size() == 1)
            c.neutralMass(neutralMasses.getDouble(0));
        else if (!neutralMasses.isEmpty()) {
            if (!new Deviation(10).inErrorWindow(neutralMasses.getDouble(0), neutralMasses.topDouble()))
                log.warn("Mass deviation of calculated neutral mass of compound with id '" + c.build() + "' higher than 10ppm");
            c.neutralMass(neutralMasses.getDouble(neutralMasses.size() / 2));
        }

        Compound co = c.build();
        co.setName("rt" + Optional.ofNullable(rt).map(r -> NUMBER_FORMAT.format(r.getMiddleTime() / 60)).orElse("N/A") + "-m" + NUMBER_FORMAT.format(co.getNeutralMass()));
        return co;
    }


    protected Instance loadInstance(String cid) {
        return projectSpaceManager.getInstanceFromCompound(parseCID(cid));
    }

    protected CompoundContainerId parseCID(String cid) {
        return projectSpaceManager.projectSpace().findCompound(cid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "There is no Compound with ID '" + cid + "' in project with name '" +
                                projectSpaceManager.projectSpace().getLocation() + "'"));
    }

    protected FormulaResultId parseFID(String cid, String fid) {
        return parseFID(loadInstance(cid), fid);
    }

    protected FormulaResultId parseFID(Instance instance, String fid) {
        return instance.loadCompoundContainer().findResult(fid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "FormulaResult with FID '" + fid + "' not found!"));

    }

    @Override
    public String getFingerIdDataCSV(int charge) {
        StringWriter w = new StringWriter();
        writeFingerIdData(w, charge);
        return w.toString();
    }

    @Override
    public String getCanopusClassyFireDataCSV(int charge) {
        StringWriter w = new StringWriter();
        writeCanopusClassyFireData(w, charge);
        return w.toString();
    }

    @Override
    public String getCanopusNpcDataCSV(int charge) {
        StringWriter w = new StringWriter();
        writeCanopusNpcData(w, charge);
        return w.toString();
    }

    @Override
    public String findSiriusFtreeJsonById(String formulaId, String alignedFeatureId) {
        final FTJsonWriter ftWriter = new FTJsonWriter();
        Instance instance = loadInstance(alignedFeatureId);
        return instance.loadFormulaResult(parseFID(instance, formulaId), FTree.class)
                .flatMap(fr -> fr.getAnnotation(FTree.class))
                .map(ftWriter::treeToJsonString).orElse(null);
    }

    public void writeFingerIdData(@NotNull Writer writer, int charge) {
        projectSpaceManager.getProjectSpaceProperty(FingerIdDataProperty.class).ifPresent(data -> {
            try {
                FingerIdData.write(writer, data.getByCharge(charge));
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error when extracting FingerIdData from project '" + projectId + "'. Message: " + e.getMessage());
            }
        });
    }

    public void writeCanopusClassyFireData(@NotNull Writer writer, int charge) {
        projectSpaceManager.getProjectSpaceProperty(CanopusCfDataProperty.class).ifPresent(data -> {
            try {
                CanopusCfData.write(writer, data.getByCharge(charge));
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error when extracting CanopusClassyFireData from project '" + projectId + "'. Message: " + e.getMessage());
            }
        });
    }

    public void writeCanopusNpcData(@NotNull Writer writer, int charge) {
        projectSpaceManager.getProjectSpaceProperty(CanopusNpcDataProperty.class).ifPresent(data -> {
            try {
                CanopusNpcData.write(writer, data.getByCharge(charge));
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error when extracting CanopusNpcData from project '" + projectId + "'. Message: " + e.getMessage());
            }
        });
    }

    private static Optional<List<StructureCandidateFormula>> loadStructureCandidates(
            Instance instance, FormulaResultId fidObj,
            Pageable pageable,
            List<Class<? extends DataAnnotation>> para,
            EnumSet<StructureCandidateScored.OptField> optFields
    ) {
        long topK = pageable.getOffset() + pageable.getPageSize();
        fidObj.setAnnotation(FBCandidateNumber.class, topK <= 0 ? FBCandidateNumber.ALL : new FBCandidateNumber((int) topK));
        FormulaResult fr = instance.loadFormulaResult(fidObj, (Class<? extends DataAnnotation>[]) para.toArray(Class[]::new)).orElseThrow();
        return fr.getAnnotation(FBCandidatesTopK.class).map(FBCandidatesTopK::getResults).map(l -> {
            List<StructureCandidateFormula> candidates = new ArrayList();

            Iterator<Scored<CompoundCandidate>> it =
                    l.stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).iterator();

            if (optFields.contains(StructureCandidateScored.OptField.fingerprint)) {
                Iterator<Fingerprint> fps = fr.getAnnotationOrThrow(FBCandidateFingerprints.class).getFingerprints()
                        .stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).iterator();

                if (it.hasNext())//tophit
                    candidates.add(StructureCandidateFormula.of(it.next(), fps.next(),
                            fr.getAnnotationOrNull(FormulaScoring.class), optFields, fidObj));

                while (it.hasNext())
                    candidates.add(StructureCandidateFormula.of(it.next(), fps.next(),
                            null, optFields, fidObj));
            } else {
                if (it.hasNext())//tophit
                    candidates.add(StructureCandidateFormula.of(it.next(), null,
                            fr.getAnnotationOrNull(FormulaScoring.class), optFields, fidObj));

                while (it.hasNext())
                    candidates.add(StructureCandidateFormula.of(it.next(), null,
                            null, optFields, fidObj));
            }
            return candidates;
        });
    }

    public static FormulaCandidate.FormulaCandidateBuilder makeFormulaCandidate(Instance inst, FormulaResult res, EnumSet<FormulaCandidate.OptField> optFields) {
        FormulaCandidate.FormulaCandidateBuilder candidate = optFields.contains(FormulaCandidate.OptField.statistics)
                ? asFormulaCandidate(res)
                : asFormulaCandidate(res.getId(), res.getAnnotationOrThrow(FormulaScoring.class));

        if (optFields.contains(FormulaCandidate.OptField.fragmentationTree))
            res.getAnnotation(FTree.class).map(FragmentationTree::fromFtree).ifPresent(candidate::fragmentationTree);
        if (optFields.contains(FormulaCandidate.OptField.simulatedIsotopePattern))
            asSimulatedIsotopePattern(inst, res).ifPresent(candidate::simulatedIsotopePattern);
        if (optFields.contains(FormulaCandidate.OptField.lipidAnnotation))
            res.getAnnotation(FTree.class).map(SiriusProjectSpaceImpl::asLipidAnnotation).ifPresent(candidate::lipidAnnotation);
        if (optFields.contains(FormulaCandidate.OptField.predictedFingerprint))
            res.getAnnotation(FingerprintResult.class).map(fpResult -> fpResult.fingerprint.toProbabilityArray())
                    .ifPresent(candidate::predictedFingerprint);
        if (optFields.contains(FormulaCandidate.OptField.canopusPredictions))
            res.getAnnotation(CanopusResult.class).map(CanopusPrediction::of).ifPresent(candidate::canopusPrediction);
        if (optFields.contains(FormulaCandidate.OptField.compoundClasses))
            res.getAnnotation(CanopusResult.class).map(CompoundClasses::of).ifPresent(candidate::compoundClasses);
        return candidate;
    }

    private static LipidAnnotation asLipidAnnotation(FTree fTree) {
        return fTree.getAnnotation(LipidSpecies.class).map(ls -> LipidAnnotation.builder()
                .lipidSpecies(ls.toString())
                .lipidMapsId(ls.getLipidClass().getLipidMapsId())
                .lipidClassName(ls.getLipidClass().longName())
                .chainsUnknown(ls.chainsUnknown())
                .hypotheticalStructure(ls.generateHypotheticalStructure().orElse(null))
                .build()
        ).orElse(LipidAnnotation.builder().build());
    }

    public static AlignedFeature asAlignedFeature(CompoundContainerId cid) {
        final AlignedFeature id = new AlignedFeature();
        id.setAlignedFeatureId(cid.getDirectoryName());
        id.setName(cid.getCompoundName());
        id.setIndex(cid.getCompoundIndex());
        id.setIonMass(cid.getIonMass().orElse(0d));
        id.setComputing(cid.hasFlag(CompoundContainerId.Flag.COMPUTING));
        cid.getIonType().map(PrecursorIonType::toString).ifPresent(id::setIonType);
        cid.getRt().ifPresent(rt -> {
            if (rt.isInterval()) {
                id.setRtStartSeconds(rt.getStartTime());
                id.setRtEndSeconds(rt.getEndTime());
            } else {
                id.setRtStartSeconds(rt.getRetentionTimeInSeconds());
                id.setRtEndSeconds(rt.getRetentionTimeInSeconds());
            }
        });
        return id;
    }

    public static FormulaCandidate.FormulaCandidateBuilder asFormulaCandidate(@NotNull FormulaResultId formulaId) {
        return FormulaCandidate.builder()
                .formulaId(formulaId.fileName())
                .molecularFormula(formulaId.getMolecularFormula().toString())
                .adduct(formulaId.getIonType().toString());
    }

    public static FormulaCandidate.FormulaCandidateBuilder asFormulaCandidate(@NotNull FormulaResultId formulaId, @Nullable FormulaScoring scorings) {
        final FormulaCandidate.FormulaCandidateBuilder frs = asFormulaCandidate(formulaId);

        if (scorings != null) {
            scorings.getAnnotation(SiriusScore.class).
                    ifPresent(sscore -> frs.siriusScore(sscore.score()));
            scorings.getAnnotation(IsotopeScore.class).
                    ifPresent(iscore -> frs.isotopeScore(iscore.score()));
            scorings.getAnnotation(TreeScore.class).
                    ifPresent(tscore -> frs.treeScore(tscore.score()));
            scorings.getAnnotation(ZodiacScore.class).
                    ifPresent(zscore -> frs.zodiacScore(zscore.score()));
            scorings.getAnnotation(TopCSIScore.class).
                    ifPresent(csiScore -> frs.topCSIScore(csiScore.score()));
        }

        return frs;
    }

    public static FormulaCandidate.FormulaCandidateBuilder asFormulaCandidate(@NotNull FormulaResult formulaResult) {
        @NotNull FormulaScoring scorings = formulaResult.getAnnotationOrThrow(FormulaScoring.class);

        final FormulaCandidate.FormulaCandidateBuilder frs = asFormulaCandidate(formulaResult.getId(), scorings);

        formulaResult.getAnnotation(FTree.class).
                ifPresent(fTree -> {
                    final FTreeMetricsHelper metrHelp = new FTreeMetricsHelper(fTree);
                    frs.numOfExplainedPeaks(metrHelp.getNumOfExplainedPeaks());
                    frs.numOfExplainablePeaks(metrHelp.getNumberOfExplainablePeaks());
                    frs.totalExplainedIntensity(metrHelp.getExplainedIntensityRatio());
                    frs.medianMassDeviation(metrHelp.getMedianMassDeviation());
                });

        return frs;
    }

    public static Class<? extends DataAnnotation>[] resolveFormulaCandidateAnnotations(EnumSet<FormulaCandidate.OptField> optFields) {
        List<Class<? extends DataAnnotation>> classes = new ArrayList<>();
        classes.add(FormulaScoring.class);
        if (Stream.of(
                        FormulaCandidate.OptField.statistics,
                        FormulaCandidate.OptField.fragmentationTree,
                        FormulaCandidate.OptField.simulatedIsotopePattern,
                        FormulaCandidate.OptField.lipidAnnotation)
                .anyMatch(optFields::contains))
            classes.add(FTree.class);

        if (optFields.contains(FormulaCandidate.OptField.predictedFingerprint))
            classes.add(FingerprintResult.class);

        if (Stream.of(FormulaCandidate.OptField.compoundClasses, FormulaCandidate.OptField.canopusPredictions)
                .anyMatch(optFields::contains))
            classes.add(CanopusResult.class);

        return classes.toArray(Class[]::new);
    }

    public static Optional<AnnotatedSpectrum> asSimulatedIsotopePattern(Instance instance, FormulaResult fResult) {
        Sirius sirius = ApplicationCore.SIRIUS_PROVIDER.sirius(instance.loadCompoundContainer(ProjectSpaceConfig.class).getAnnotationOrThrow(ProjectSpaceConfig.class).config.getConfigValue("AlgorithmProfile"));
        return Optional.of(fResult)
                .map(FormulaResult::getId)
                .map(id -> sirius.simulateIsotopePattern(id.getMolecularFormula(), id.getIonType().getIonization()))
                .map(AnnotatedSpectrum::new);
    }

    public static FeatureAnnotations extractTopAnnotationsDeNovo(Instance inst) {
        return inst.loadTopFormulaResult(List.of(SiriusScore.class)).map(FormulaResult::getId)
                .flatMap(frid -> inst.loadFormulaResult(frid, FormulaScoring.class, FTree.class, CanopusResult.class)
                        .map(topHit -> {
                            final FeatureAnnotations cSum = new FeatureAnnotations();
//
                            //add formula summary
                            cSum.setFormulaAnnotation(asFormulaCandidate(topHit).build());

                            // todo msnovelist: add msnovelist candidatas
//                        topHit.getAnnotation(FBCandidatesTopK.class).map(FBCandidatesTopK::getResults)
//                                .filter(l -> !l.isEmpty()).map(r -> r.get(0)).map(s ->
//                                        StructureCandidateFormula.of(s, topHit.getAnnotationOrThrow(FormulaScoring.class),
//                                                EnumSet.of(StructureCandidateScored.OptField.dbLinks, StructureCandidateScored.OptField.refSpectraLinks), topHit.getId()))
//                                .ifPresent(cSum::setStructureAnnotation);

                            topHit.getAnnotation(CanopusResult.class).map(CompoundClasses::of).
                                    ifPresent(cSum::setCompoundClassAnnotation);
                            return cSum;

                        })).orElseGet(FeatureAnnotations::new);
    }

    public static FeatureAnnotations extractTopAnnotations(Instance inst) {
        return inst.loadTopFormulaResult(List.of(TopCSIScore.class, SiriusScore.class)).map(FormulaResult::getId).flatMap(frid -> {
            frid.setAnnotation(FBCandidateNumber.class, new FBCandidateNumber(1));
            return inst.loadFormulaResult(frid, FormulaScoring.class, FTree.class, FBCandidatesTopK.class, CanopusResult.class)
                    .map(topHit -> {
                        final FeatureAnnotations cSum = new FeatureAnnotations();
//
                        //add formula summary
                        cSum.setFormulaAnnotation(asFormulaCandidate(topHit).build());

                        // fingerid result
                        topHit.getAnnotation(FBCandidatesTopK.class).map(FBCandidatesTopK::getResults)
                                .filter(l -> !l.isEmpty()).map(r -> r.get(0)).map(s ->
                                        StructureCandidateFormula.of(s, topHit.getAnnotationOrThrow(FormulaScoring.class),
                                                EnumSet.of(StructureCandidateScored.OptField.dbLinks, StructureCandidateScored.OptField.refSpectraLinks), topHit.getId()))
                                .ifPresent(cSum::setStructureAnnotation);

                        topHit.getAnnotation(CanopusResult.class).map(CompoundClasses::of).
                                ifPresent(cSum::setCompoundClassAnnotation);
                        return cSum;

                    });
        }).orElseGet(FeatureAnnotations::new);
    }

    public static MsData asCompoundMsData(Instance instance) {
        return instance.loadCompoundContainer(Ms2Experiment.class)
                .getAnnotation(Ms2Experiment.class).map(exp -> new MsData(
                        opt(exp.getMergedMs1Spectrum(), s -> {
                            AnnotatedSpectrum t = new AnnotatedSpectrum((Spectrum<Peak>) s);
                            t.setMsLevel(1);
                            return t;
                        }).orElse(null),
                        null,
                        exp.getMs1Spectra().stream().map(x -> {
                            AnnotatedSpectrum t = new AnnotatedSpectrum(x);
                            t.setMsLevel(1);
                            return t;
                        }).collect(Collectors.toList()),
                        exp.getMs2Spectra().stream().map(x -> {
                            AnnotatedSpectrum t = new AnnotatedSpectrum(x);
                            t.setCollisionEnergy(new CollisionEnergy(x.getCollisionEnergy()));
                            t.setMsLevel(2);
                            return t;
                        }).collect(Collectors.toList()))).orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Feature with ID '" + instance + "' has no input Data!"));
    }

    public static EnumSet<CompoundQuality.CompoundQualityFlag> asCompoundQualityData(Instance instance) {
        return instance.loadCompoundContainer(Ms2Experiment.class)
                .getAnnotation(Ms2Experiment.class)
                .flatMap(exp -> exp.getAnnotation(CompoundQuality.class))
                .map(CompoundQuality::getFlags)
                .orElse(EnumSet.of(CompoundQuality.CompoundQualityFlag.UNKNOWN));
    }

    public static LCMSFeatureQuality asCompoundLCMSFeatureQuality(Instance instance) {
        final LCMSPeakInformation peakInformation = instance.loadCompoundContainer(LCMSPeakInformation.class).getAnnotation(LCMSPeakInformation.class, LCMSPeakInformation::empty);
        Ms2Experiment experiment = instance.getExperiment();
        Optional<CoelutingTraceSet> traceSet = peakInformation.getTracesFor(0);
        if (traceSet.isPresent()) {
            final LCMSCompoundSummary summary = new LCMSCompoundSummary(traceSet.get(), traceSet.get().getIonTrace(), experiment);
            return new LCMSFeatureQuality(summary);
        } else {
            return null;
        }
    }

    private static <S, T> Optional<T> opt(S input, Function<S, T> convert) {
        return Optional.ofNullable(input).map(convert);
    }
}
