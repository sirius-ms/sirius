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

package de.unijena.bioinf.ms.middleware.service.annotations;

import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.ms.middleware.model.annotations.*;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class AnnotationUtils {
    private AnnotationUtils() {
        // just to prevent instantiation
    }

    public static <T extends Enum<T>> EnumSet<T> removeNone(EnumSet<T> optFields) {
        optFields.removeIf(e -> e.name().equals("none"));
        return optFields;
    }

    public static <T extends Enum<T>> EnumSet<T> toEnumSet(Class<T> clz, T... input) {
        return input != null && input.length > 0
                ? EnumSet.copyOf(List.of(input))
                : EnumSet.noneOf(clz);
    }

    public static ConsensusAnnotationsDeNovo buildConsensusAnnotationsDeNovo(Collection<AlignedFeature> features) {
        //todo structure based consensus for Denovo structures
        //formula based consensus
        Map<String, List<AlignedFeature>> formulaAnnotationAgreement = features.stream()
                .collect(Collectors.groupingBy(f -> Optional.of(f)
                        .map(AlignedFeature::getTopAnnotations)
                        .map(FeatureAnnotations::getFormulaAnnotation)
                        .map(FormulaCandidate::getMolecularFormula)
                        .orElse(""), Collectors.toList()));
        //filter features with no valid formula candidate
        formulaAnnotationAgreement.remove("");

        if (!formulaAnnotationAgreement.isEmpty()) {
            Map.Entry<String, List<AlignedFeature>> max = formulaAnnotationAgreement.entrySet().stream()
                    .max(Comparator.comparing(e -> e.getValue().size())).orElseThrow();

            if (formulaAnnotationAgreement.values().stream().filter(v -> v.size() == max.getValue().size()).count() == 1) {
                return consensusDeNovo(max.getValue(), max.getValue().size() == 1
                        ? ConsensusAnnotationsDeNovo.Criterion.SINGLETON_FORMULA
                        : ConsensusAnnotationsDeNovo.Criterion.MAJORITY_FORMULA);
            }
            return consensusDeNovo(
                    formulaAnnotationAgreement.values().stream().flatMap(List::stream).toList(),
                    ConsensusAnnotationsDeNovo.Criterion.TOP_FORMULA);
        }
        //empty results
        return ConsensusAnnotationsDeNovo.builder().build();
    }

    public static ConsensusAnnotationsCSI buildConsensusAnnotationsCSI(Collection<AlignedFeature> features) {
        {
            Map<String, List<AlignedFeature>> structureAnnotationAgreement = features.stream()
                    .collect(Collectors.groupingBy(f -> Optional.of(f)
                            .map(AlignedFeature::getTopAnnotations)
                            .map(FeatureAnnotations::getStructureAnnotation)
                            .map(StructureCandidateScored::getInchiKey)
                            .orElse(""), Collectors.toList()));

            //filter features with no valid structure candidate
            structureAnnotationAgreement.remove("");

            //structure based consensus
            if (!structureAnnotationAgreement.isEmpty()) {
                Map.Entry<String, List<AlignedFeature>> max = structureAnnotationAgreement.entrySet().stream()
                        .max(Comparator.comparing(e -> e.getValue().size())).orElseThrow();

                if (structureAnnotationAgreement.values().stream().filter(v -> v.size() == max.getValue().size()).count() == 1) {
                    return consensusByStructureCSI(max.getValue(), max.getValue().size() == 1
                            ? ConsensusAnnotationsCSI.Criterion.SINGLETON_STRUCTURE
                            : ConsensusAnnotationsCSI.Criterion.MAJORITY_STRUCTURE);
                }
                return consensusByStructureCSI(
                        structureAnnotationAgreement.values().stream().flatMap(List::stream).toList(),
                        ConsensusAnnotationsCSI.Criterion.CONFIDENCE_STRUCTURE);
            }
        }
        //formula based consensus
        Map<String, List<AlignedFeature>> formulaAnnotationAgreement = features.stream()
                .collect(Collectors.groupingBy(f -> Optional.of(f)
                        .map(AlignedFeature::getTopAnnotations)
                        .map(FeatureAnnotations::getFormulaAnnotation)
                        .map(FormulaCandidate::getMolecularFormula)
                        .orElse(""), Collectors.toList()));
        //filter features with no valid formula candidate
        formulaAnnotationAgreement.remove("");

        if (!formulaAnnotationAgreement.isEmpty()) {
            Map.Entry<String, List<AlignedFeature>> max = formulaAnnotationAgreement.entrySet().stream()
                    .max(Comparator.comparing(e -> e.getValue().size())).orElseThrow();

            if (formulaAnnotationAgreement.values().stream().filter(v -> v.size() == max.getValue().size()).count() == 1) {
                return consensusByFormulaCSI(max.getValue(), max.getValue().size() == 1
                        ? ConsensusAnnotationsCSI.Criterion.SINGLETON_FORMULA
                        : ConsensusAnnotationsCSI.Criterion.MAJORITY_FORMULA);
            }
            return consensusByFormulaCSI(
                    formulaAnnotationAgreement.values().stream().flatMap(List::stream).toList(),
                    ConsensusAnnotationsCSI.Criterion.TOP_FORMULA);
        }
        //empty results
        return ConsensusAnnotationsCSI.builder().build();
    }

    private static ConsensusAnnotationsDeNovo consensusDeNovo(Collection<AlignedFeature> features,
                                                              ConsensusAnnotationsDeNovo.Criterion type) {
        //prefer candidate with compound classes
        AlignedFeature top = features.stream()
                .filter(f -> f.getTopAnnotations().getCompoundClassAnnotation() != null)
                .max(Comparator.comparing(f -> f.getTopAnnotations().getFormulaAnnotation().getSiriusScore()))
                .orElse(null);

        // fallback to non compound class candidate
        if (top == null)
            top = features.stream()
                    .max(Comparator.comparing(f -> f.getTopAnnotations().getFormulaAnnotation().getSiriusScore()))
                    .orElseThrow(() -> new IllegalStateException("No Formula Candidate Found!"));

        return ConsensusAnnotationsDeNovo.builder()
                .selectionCriterion(type)
                .compoundClasses(top.getTopAnnotations().getCompoundClassAnnotation())
                .molecularFormula(top.getTopAnnotations().getFormulaAnnotation().getMolecularFormula())
                .supportingFeatureIds(ConsensusAnnotationsDeNovo.Criterion.TOP_FORMULA == type
                        ? List.of(top.getAlignedFeatureId())
                        : features.stream().map(AlignedFeature::getAlignedFeatureId).toList()
                ).build();
    }


    private static ConsensusAnnotationsCSI consensusByFormulaCSI(Collection<AlignedFeature> features,
                                                                 ConsensusAnnotationsCSI.Criterion type) {

        AlignedFeature top = features.stream()
                .max(Comparator.comparing(f -> f.getTopAnnotations().getFormulaAnnotation().getSiriusScore()))
                .orElseThrow(() -> new IllegalStateException("No Formula Candidate Found!"));


        return ConsensusAnnotationsCSI.builder()
                .selectionCriterion(type)
                .molecularFormula(top.getTopAnnotations().getFormulaAnnotation().getMolecularFormula())
                .compoundClasses(top.getTopAnnotations().getCompoundClassAnnotation())
                .supportingFeatureIds(ConsensusAnnotationsCSI.Criterion.TOP_FORMULA == type
                        ? List.of(top.getAlignedFeatureId())
                        : features.stream().map(AlignedFeature::getAlignedFeatureId).toList()
                ).build();
    }

    private static ConsensusAnnotationsCSI consensusByStructureCSI(Collection<AlignedFeature> features,
                                                                   ConsensusAnnotationsCSI.Criterion type) {
        final boolean mixedStructures = ConsensusAnnotationsCSI.Criterion.CONFIDENCE_STRUCTURE == type;
        AlignedFeature topConf;
        //prefer approx confidence if the same number of features have a valid value here.
        if (features.stream().map(AlignedFeature::getTopAnnotations).map(FeatureAnnotations::getConfidenceApproxMatch)
                .filter(c -> c != null && !Double.isNaN(c)).count() >=
                features.stream().map(AlignedFeature::getTopAnnotations).map(FeatureAnnotations::getConfidenceExactMatch)
                        .filter(c -> c != null && !Double.isNaN(c)).count())
        {
            topConf = features.stream()
                    .filter(f -> f.getTopAnnotations() != null && f.getTopAnnotations().getConfidenceApproxMatch() != null && !Double.isNaN(f.getTopAnnotations().getConfidenceApproxMatch()))
                    .max(Comparator.comparing(f -> f.getTopAnnotations().getConfidenceApproxMatch()))
                    .orElseThrow(() -> new IllegalStateException("No Structure Candidate Found!"));
        } else {
            topConf = features.stream()
                    .filter(f -> f.getTopAnnotations() != null && f.getTopAnnotations().getConfidenceExactMatch() != null && !Double.isNaN(f.getTopAnnotations().getConfidenceExactMatch()))
                    .max(Comparator.comparing(f -> f.getTopAnnotations().getConfidenceExactMatch()))
                    .orElseThrow(() -> new IllegalStateException("No Structure Candidate Found!"));
        }

        Double topConfExact = mixedStructures
                ? topConf.getTopAnnotations().getConfidenceExactMatch()
                : features.stream()
                .map(AlignedFeature::getTopAnnotations)
                .map(FeatureAnnotations::getConfidenceExactMatch)
                .filter(c -> c != null && !Double.isNaN(c))
                .max(Double::compareTo)
                .orElse(null);
        Double topConfApprox = mixedStructures
                ? topConf.getTopAnnotations().getConfidenceApproxMatch()
                : features.stream()
                .map(AlignedFeature::getTopAnnotations)
                .map(FeatureAnnotations::getConfidenceApproxMatch)
                .filter(c -> c != null && !Double.isNaN(c))
                .max(Double::compareTo)
                .orElse(null);

        return ConsensusAnnotationsCSI.builder()
                .selectionCriterion(type)
                .csiFingerIdStructure(topConf.getTopAnnotations().getStructureAnnotation())
                .compoundClasses(topConf.getTopAnnotations().getCompoundClassAnnotation())
                .confidenceExactMatch(topConfExact)
                .confidenceApproxMatch(topConfApprox)
                .molecularFormula(topConf.getTopAnnotations().getFormulaAnnotation().getMolecularFormula())
                .supportingFeatureIds(mixedStructures
                        ? List.of(topConf.getAlignedFeatureId())
                        : features.stream().map(AlignedFeature::getAlignedFeatureId).toList()
                ).build();
    }

    public static BinaryFingerprint asBinaryFingerprint(Fingerprint fingerprint) {
        BinaryFingerprint fp = new BinaryFingerprint();
        fp.setLength(fingerprint.getFingerprintVersion().size());
        short[] absIdx = fingerprint.toIndizesArray();
        short[] relativeIdx = new short[absIdx.length];
        for (int i = 0; i < absIdx.length; i++)
            relativeIdx[i] = (short) fingerprint.getFingerprintVersion().getRelativeIndexOf(absIdx[i]);

        fp.setBitsSet(relativeIdx);
        return fp;
    }

    public static LipidAnnotation asLipidAnnotation(FTree fTree) {
        return fTree.getAnnotation(LipidSpecies.class).map(ls -> LipidAnnotation.builder()
                .lipidSpecies(ls.toString())
                .lipidMapsId(ls.getLipidClass().getLipidMapsId())
                .lipidClassName(ls.getLipidClass().longName())
                .chainsUnknown(ls.chainsUnknown())
                .hypotheticalStructure(ls.generateHypotheticalStructure().orElse(null))
                .build()
        ).orElse(LipidAnnotation.builder().build());
    }
}
