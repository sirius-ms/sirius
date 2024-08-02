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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class NoSqlCanopusSummaryWriter implements AutoCloseable {
    final static String DOUBLE_FORMAT = "%.3f";
    final static String LONG_FORMAT = "%d";
    final static String HEADER =
            "formulaRank\t" +
                    "molecularFormula\t" +
                    "adduct\t" +
                    "precursorFormula\t" +
                    "NPC#pathway\t" +
                    "NPC#pathway Probability\t" +
                    "NPC#superclass\t" +
                    "NPC#superclass Probability\t" +
                    "NPC#class\t" +
                    "NPC#class Probability\t" +
                    "ClassyFire#superclass\t" +
                    "ClassyFire#superclass probability\t" +
                    "ClassyFire#class\t" +
                    "ClassyFire#class Probability\t" +
                    "ClassyFire#subclass\t" +
                    "ClassyFire#subclass Probability\t" +
                    "ClassyFire#level 5\t" +
                    "ClassyFire#level 5 Probability\t" +
                    "ClassyFire#most specific class\t" +
                    "ClassyFire#most specific class Probability\t" +
                    "ClassyFire#all classifications\t" +
                    // metadata for mapping
                    "ionMass\t" +
                    "retentionTimeInSeconds\t" +
                    "retentionTimeInMinutes\t" +
                    "formulaId\t" +
                    "alignedFeatureId\t" +
                    "mappingFeatureId";

    private final BufferedWriter w;


    NoSqlCanopusSummaryWriter(BufferedWriter writer) {
        this.w = writer;
    }

    private NoSqlCanopusSummaryWriter(Writer w) {
        this.w = new BufferedWriter(w);
    }

    public void writeHeader() throws IOException {
        w.write(HEADER);
        w.newLine();
    }

    public void writeCanopusPredictions(AlignedFeatures f, FormulaCandidate fc, CanopusPrediction cp) throws IOException {
        w.write(String.valueOf(fc.getFormulaRank()));
        writeSep();
        w.write(fc.getMolecularFormula().toString());
        writeSep();
        w.write(fc.getAdduct().toString());
        writeSep();
        w.write(fc.getPrecursorFormulaWithCharge());
        writeSep();
        writeCanopus(cp);
        writeSep();
        w.write(String.format(DOUBLE_FORMAT, f.getAverageMass()));
        writeSep();
        w.write(Optional.ofNullable(f.getRetentionTime()).map(rt -> String.format("%.0f", rt.getMiddleTime())).orElse(""));
        writeSep();
        w.write(Optional.ofNullable(f.getRetentionTime()).map(rt -> String.format("%.2f", rt.getMiddleTime() / 60d)).orElse(""));
        writeSep();
        w.write(String.format(LONG_FORMAT, fc.getFormulaId()));
        writeSep();
        w.write(String.format(LONG_FORMAT, f.getAlignedFeatureId()));
        writeSep();
        w.write(Objects.requireNonNullElse(f.getExternalFeatureId(), String.format(LONG_FORMAT, f.getAlignedFeatureId())));
        w.newLine();
    }

    private void writeSep() throws IOException {
        w.write('\t');
    }

    @Override
    public void close() throws Exception {
        w.close();
    }


    private void writeCanopus(CanopusPrediction cp) throws IOException {
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

            w.write(perLevelProps[0].getName());
            writeSep();
            w.write(String.format(DOUBLE_FORMAT, perLevelProp[0]));
            writeSep();
            w.write(perLevelProps[1].getName());
            writeSep();
            w.write(String.format(DOUBLE_FORMAT, perLevelProp[1]));
            writeSep();
            w.write(perLevelProps[2].getName());
            writeSep();
            w.write(String.format(DOUBLE_FORMAT, perLevelProp[2]));
            writeSep();
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
                        w.write(cfc.getName());
                        writeSep();
                        w.write(String.format(DOUBLE_FORMAT, cfClassification.getProbability(CLF.getIndexOfMolecularProperty(cfc))));
                        writeSep();
                    }else {
                        writeSep();
                        writeSep();
                    }

                }
            }
            w.write(primaryClass.getName());
            writeSep();
            w.write(String.format(DOUBLE_FORMAT, cfClassification.getProbability(CLF.getIndexOfMolecularProperty(primaryClass))));
            writeSep();
            w.write(StreamSupport.stream(cfClassification.asDeterministic().asArray().presentFingerprints().spliterator(), false)
                    .map(FPIter::getMolecularProperty)
                    .map(MolecularProperty::toString)
                    .collect(Collectors.joining("; ")));
        }
    }
}
