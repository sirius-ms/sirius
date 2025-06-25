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

package de.unijena.bioinf.ms.frontend.subtools.summaries;

import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.sirius.CanopusPrediction;
import de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class NoSqlCanopusSummaryWriter extends SummaryTable {

    final static List<String> HEADER = List.of(
            "formulaRank",
            "molecularFormula",
            "adduct",
            "precursorFormula",
            "NPC#pathway",
            "NPC#pathway Probability",
            "NPC#superclass",
            "NPC#superclass Probability",
            "NPC#class",
            "NPC#class Probability",
            "ClassyFire#superclass",
            "ClassyFire#superclass probability",
            "ClassyFire#class",
            "ClassyFire#class Probability",
            "ClassyFire#subclass",
            "ClassyFire#subclass Probability",
            "ClassyFire#level 5",
            "ClassyFire#level 5 Probability",
            "ClassyFire#most specific class",
            "ClassyFire#most specific class Probability",
            "ClassyFire#all classifications",
            // metadata for mapping
            "ionMass",
            "retentionTimeInSeconds",
            "retentionTimeInMinutes",
            "formulaId",
            "alignedFeatureId",
            "compoundId",
            "mappingFeatureId",
            "overallFeatureQuality");

    NoSqlCanopusSummaryWriter(SummaryTableWriter writer) {
        super(writer);
    }

    public void writeHeader() throws IOException {
        writer.writeHeader(HEADER);
    }

    public void writeCanopusPredictions(AlignedFeatures f, FormulaCandidate fc, CanopusPrediction cp) throws IOException {
        List<Object> row = new ArrayList<>();
        row.add(fc.getFormulaRank());
        row.add(fc.getMolecularFormula().toString());
        row.add(fc.getAdduct().toString());
        row.add(fc.getPrecursorFormulaWithCharge());
        writeCanopus(row, cp);
        row.add(f.getAverageMass());
        row.add(Optional.ofNullable(f.getRetentionTime()).map(rt -> Math.round(rt.getMiddleTime())).orElse(null));
        row.add(Optional.ofNullable(f.getRetentionTime()).map(rt -> rt.getMiddleTime() / 60d).orElse(null));
        row.add(String.valueOf(fc.getFormulaId()));
        row.add(String.valueOf(f.getAlignedFeatureId()));
        row.add(String.valueOf(f.getCompoundId()));
        row.add(getMappingIdOrFallback(f));
        row.add(f.getDataQuality());

        writer.writeRow(row);
    }

    private void writeCanopus(List<Object> row, CanopusPrediction cp) {
        @Nullable ProbabilityFingerprint npcClassification = cp.getNpcFingerprint();
        if (npcClassification != null) {
            double[] perLevelProp = new double[NPCFingerprintVersion.NPCLevel.values().length];
            NPCFingerprintVersion.NPCProperty[] perLevelProps = new NPCFingerprintVersion.NPCProperty[NPCFingerprintVersion.NPCLevel.values().length];
//            int[] indices = new int[NPCFingerprintVersion.NPCLevel.values().length];
            for (FPIter fpIter : npcClassification) {
                NPCFingerprintVersion.NPCProperty prop = ((NPCFingerprintVersion.NPCProperty) fpIter.getMolecularProperty());

                if (fpIter.getProbability() >= perLevelProp[prop.level.level]) {
                    perLevelProp[prop.level.level] = fpIter.getProbability();
                    perLevelProps[prop.level.level] = prop;
//                    indices[prop.level.level] = fpIter.getIndex();
                }
            }

            row.add(perLevelProps[0].getName());
            row.add(perLevelProp[0]);
            row.add(perLevelProps[1].getName());
            row.add(perLevelProp[1]);
            row.add(perLevelProps[2].getName());
            row.add(perLevelProp[2]);
        }

        @Nullable ProbabilityFingerprint cfClassification = cp.getCfFingerprint();
        if (cfClassification != null) {
            FingerprintVersion v = cfClassification.getFingerprintVersion();
            if (v instanceof MaskedFingerprintVersion) v = ((MaskedFingerprintVersion) v).getMaskedFingerprintVersion();
            ClassyFireFingerprintVersion CLF = (ClassyFireFingerprintVersion) v;
            ClassyfireProperty primaryClass = CLF.getPrimaryClass(cfClassification);
            final List<ClassyfireProperty> lineage = Stream.of(primaryClass.getLineageRootToNode(false)).toList();
            final Set<ClassyfireProperty> alternatives =
                    Stream.of(CLF.getPredictedLeafs(cfClassification, 0.5)).collect(Collectors.toSet());
            lineage.forEach(alternatives::remove);

            if (!lineage.isEmpty()) {
                Iterator<ClassyfireProperty> lineageIterator = lineage.iterator();
                if (lineageIterator.hasNext())
                    lineageIterator.next(); //skip organic compounds class
                for (int i = 0; i < 4; i++) {
                    if (lineageIterator.hasNext()){
                        ClassyfireProperty cfc = lineageIterator.next();
                        row.add(cfc.getName());
                        row.add(cfClassification.getProbability(CLF.getIndexOfMolecularProperty(cfc)));
                    }else {
                        row.add(null);
                        row.add(null);
                    }

                }
            }
            row.add(primaryClass.getName());
            row.add(cfClassification.getProbability(CLF.getIndexOfMolecularProperty(primaryClass)));
            row.add(StreamSupport.stream(cfClassification.asDeterministic().asArray().presentFingerprints().spliterator(), false)
                    .map(FPIter::getMolecularProperty)
                    .map(MolecularProperty::toString)
                    .collect(Collectors.joining("; ")));
        }
    }
}
