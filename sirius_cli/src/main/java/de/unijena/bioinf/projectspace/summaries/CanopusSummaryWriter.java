/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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

package de.unijena.bioinf.projectspace.summaries;

import com.google.common.base.Joiner;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.util.Iterators;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CanopusSummaryWriter extends CandidateSummarizer {

    protected static class CanopusSummaryRow {
        private final ProbabilityFingerprint[] cfClassifications;
        private final ProbabilityFingerprint[] npcClassifications;
        private final MolecularFormula[] molecularFormulas, precursorFormulas;
        private final ClassyfireProperty[] mostSpecificClasses;

        private final NPCFingerprintVersion.NPCProperty[][] bestNPCProps;

        private final double[][] bestNPCProbs;

        private final PrecursorIonType[] ionTypes;
        private final String id;
        private final String featureId;
        private final int best;

        private ClassyFireFingerprintVersion CLF;
        NPCFingerprintVersion NPCF;

        public CanopusSummaryRow(ProbabilityFingerprint[] cfClassifications, ProbabilityFingerprint[] npcClassifications, MolecularFormula[] molecularFormulas, MolecularFormula[] precursorFormulas, PrecursorIonType[] ionTypes, String id, String featureId) {
            this.cfClassifications = cfClassifications;
            this.npcClassifications = npcClassifications;
            this.molecularFormulas = molecularFormulas;
            this.precursorFormulas = precursorFormulas;
            this.mostSpecificClasses = new ClassyfireProperty[molecularFormulas.length];
            this.ionTypes = ionTypes;
            this.id = id;
            this.featureId = featureId;
            this.best = chooseBestAndAssignPrimaryClasses(cfClassifications);

            bestNPCProps = new NPCFingerprintVersion.NPCProperty[molecularFormulas.length][3];
            bestNPCProbs = new double[molecularFormulas.length][3];
            chooseBestNPCAssignments(npcClassifications);
        }

        private void chooseBestNPCAssignments(ProbabilityFingerprint[] npcClassifications) {

            FingerprintVersion v = npcClassifications[0].getFingerprintVersion();
            if (v instanceof MaskedFingerprintVersion) v = ((MaskedFingerprintVersion) v).getMaskedFingerprintVersion();
            NPCF = (NPCFingerprintVersion) v;
            //todo do we have to perform index mapping?
            for (int i = 0; i < npcClassifications.length; ++i) {
                ProbabilityFingerprint fp = npcClassifications[i];
                for (FPIter fpIter : fp) {
                    NPCFingerprintVersion.NPCProperty prop = ((NPCFingerprintVersion.NPCProperty) fpIter.getMolecularProperty());
                    if (fpIter.getProbability() >= bestNPCProbs[i][prop.level.level]) {
                        bestNPCProps[i][prop.level.level] = (NPCFingerprintVersion.NPCProperty) fpIter.getMolecularProperty();
                        bestNPCProbs[i][prop.level.level] = fpIter.getProbability();
                    }
                }
            }

        }

        private int chooseBestAndAssignPrimaryClasses(ProbabilityFingerprint[] classifications) {
            FingerprintVersion v = classifications[0].getFingerprintVersion();
            if (v instanceof MaskedFingerprintVersion) v = ((MaskedFingerprintVersion) v).getMaskedFingerprintVersion();
            CLF = (ClassyFireFingerprintVersion) v;
            ClassyfireProperty bestClass = CLF.getPrimaryClass(classifications[0]);
            mostSpecificClasses[0] = bestClass;

            // choose the classification with the highest probability for the most specific class, starting
            // with probabilities above 50%
            if (classifications.length == 1) return 0;

            int argmax = 0;
            double bestProb = classifications[0].getProbability(CLF.getIndexOfMolecularProperty(bestClass));
//            mostSpecificClassesProbs[0] = bestProb;
            for (int i = 1; i < classifications.length; ++i) {
                ClassyfireProperty primary = CLF.getPrimaryClass(classifications[i]);
                mostSpecificClasses[i] = primary;
                final double prob = classifications[i].getProbability(CLF.getIndexOfMolecularProperty(primary));
//                mostSpecificClassesProbs[i] = prob;
                final int ord = new ClassyfireProperty.CompareCompoundClassDescriptivity().compare(primary, bestClass);
                if (ord > 0 || (ord == 0 && prob > bestProb)) {
                    argmax = i;
                    bestClass = primary;
                    bestProb = prob;
                }
            }
            return argmax;
        }
    }

    private final List<CanopusSummaryRow> rowsBySiriusScoreAll;
    private final List<CanopusSummaryRow> rowsBySiriusScore;
    private final List<CanopusSummaryRow> rowsByCSIScore;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public CanopusSummaryWriter(boolean writeTopHitGlobal, boolean writeTopHitWithAdductsGlobal, boolean writeFullGlobal) {
        super(writeTopHitGlobal, writeTopHitWithAdductsGlobal, writeFullGlobal);
        rowsByCSIScore = writeTopHitGlobal || writeTopHitWithAdductsGlobal ? new ArrayList<>() : null;
        rowsBySiriusScore = writeTopHitGlobal || writeTopHitWithAdductsGlobal ? new ArrayList<>() : null;
        rowsBySiriusScoreAll = writeFullGlobal ? new ArrayList<>() : null;
    }

    @Override
    public List<Class<? extends DataAnnotation>> requiredFormulaResultAnnotations() {
        return List.of(CanopusResult.class);
    }

    @Override
    public void addWriteCompoundSummary(ProjectWriter writer, @NotNull CompoundContainer exp, List<? extends SScored<FormulaResult, ? extends FormulaScore>> results) throws IOException {
        if (!results.isEmpty()) {
            if (rowsBySiriusScore != null)
                addToRows(rowsBySiriusScore, FormulaScoring.reRankBy(results, List.of(SiriusScore.class), true), false);
            if (rowsByCSIScore != null)
                addToRows(rowsByCSIScore, FormulaScoring.reRankBy(results, List.of(TopCSIScore.class, SiriusScore.class), true), false);
            if (rowsBySiriusScoreAll != null)
                addToRows(rowsBySiriusScoreAll, FormulaScoring.reRankBy(results, List.of(SiriusScore.class), true), true);
        }
    }

    private void addToRows(List<CanopusSummaryRow> rows, List<? extends SScored<FormulaResult, ? extends FormulaScore>> results, boolean all) {
        // sometimes we have multiple results with same score (adducts!). In this case, we list all of them in
        // a separate summary file
        int i = 0;
        SScored<FormulaResult, ? extends FormulaScore> hit;
        ArrayList<ProbabilityFingerprint> cfFingerprints = new ArrayList<>();
        ArrayList<ProbabilityFingerprint> npcFingerprints = new ArrayList<>();
        ArrayList<MolecularFormula> formulas = new ArrayList<>(), preForms = new ArrayList<>();
        ArrayList<PrecursorIonType> ionTypes = new ArrayList<>();
        FormulaResultId id;
        do {
            hit = results.get(i);
            id = hit.getCandidate().getId();
            final Optional<CanopusResult> cr = hit.getCandidate().getAnnotation(CanopusResult.class);
            final var cid = id;
            cr.ifPresent(canopusResult -> {
                cfFingerprints.add(canopusResult.getCanopusFingerprint());
                npcFingerprints.add(canopusResult.getNpcFingerprint().orElseThrow());
                formulas.add(cid.getMolecularFormula());
                ionTypes.add(cid.getIonType());
                preForms.add(cid.getPrecursorFormula());
            });
            ++i;
        } while (i < results.size() && (results.get(i).getCandidate().getId().getPrecursorFormula().equals(results.get(0).getCandidate().getId().getPrecursorFormula()) || all));
        if (cfFingerprints.size() > 0) {
            lock.writeLock().lock();
            try {
                rows.add(new CanopusSummaryRow(
                        cfFingerprints.toArray(ProbabilityFingerprint[]::new),
                        npcFingerprints.toArray(ProbabilityFingerprint[]::new),
                        formulas.toArray(MolecularFormula[]::new),
                        preForms.toArray(MolecularFormula[]::new),
                        ionTypes.toArray(PrecursorIonType[]::new),
                        id.getParentId().getDirectoryName(),
                        id.getParentId().getFeatureId().orElse("N/A")
                ));
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    @Override
    public void writeProjectSpaceSummary(ProjectWriter writer) throws IOException {
        lock.readLock().lock();
        try {
            if (writeTopHitGlobal && rowsBySiriusScore != null)
                writer.table(SummaryLocations.CANOPUS_FORMULA_SUMMARY, HEADER, Iterators.capture(new IterateOverFormulas(rowsBySiriusScore)));
            if (writeTopHitGlobal && rowsByCSIScore != null)
                writer.table(SummaryLocations.CANOPUS_COMPOUND_SUMMARY, HEADER, Iterators.capture(new IterateOverFormulas(rowsByCSIScore)));
            if (writeTopHitWithAdductsGlobal && rowsBySiriusScore != null)
                writer.table(SummaryLocations.CANOPUS_FOMRULA_SUMMARY_ADDUCTS, HEADER2, Iterators.capture(new IterateOverAdducts(rowsBySiriusScore)));
            if (writeFullGlobal && rowsBySiriusScoreAll != null)
                writer.table(SummaryLocations.CANOPUS_FOMRULA_SUMMARY_ALL, HEADER2, Iterators.capture(new IterateOverAdducts(rowsBySiriusScoreAll)));
        } finally {
            lock.readLock().unlock();
        }
    }

    private final static String[]
            HEADER = new String[]{"id", "molecularFormula", "adduct",
            "NPC#pathway", "NPC#pathway Probability", "NPC#superclass", "NPC#superclass Probability",
            "NPC#class", "NPC#class Probability",
            "ClassyFire#most specific class", "ClassyFire#most specific class Probability", "ClassyFire#level 5",
            "ClassyFire#level 5 Probability", "ClassyFire#subclass", "ClassyFire#subclass Probability",
            "ClassyFire#class", "ClassyFire#class Probability", "ClassyFire#superclass", "ClassyFire#superclass probability",
            /*"NPC#all classifications",*/ "ClassyFire#all classifications", "featureId"},
            HEADER2 = new String[]{"id", "molecularFormula", "adduct", "precursorFormula",
                    "NPC#pathway", "NPC#pathway Probability", "NPC#superclass", "NPC#superclass Probability",
                    "NPC#class", "NPC#class Probability",
                    "ClassyFire#most specific class", "ClassyFire#most specific class Probability", "ClassyFire#level 5",
                    "ClassyFire#level 5 Probability", "ClassyFire#subclass", "ClassyFire#subclass Probability",
                    "ClassyFire#class", "ClassyFire#class Probability", "ClassyFire#superclass", "ClassyFire#superclass probability",
                    /*"NPC#all classifications",*/ "ClassyFire#all classifications", "featureId"};

    public static class IterateOverFormulas implements Iterator<String[]> {
        int k = 0;
        String[] cols = new String[HEADER.length];
        final List<CanopusSummaryRow> rows;

        public IterateOverFormulas(List<CanopusSummaryRow> rows) {
            this.rows = rows;
        }

        @Override
        public boolean hasNext() {
            return k < rows.size();
        }

        @Override
        public String[] next() {
            try {
                final CanopusSummaryRow row = rows.get(k);
                final ClassyfireProperty primaryClass = row.mostSpecificClasses[row.best];
                final ClassyfireProperty[] lineage = primaryClass.getLineage();

                int i = 0;
                cols[i++] = row.id;
                cols[i++] = row.molecularFormulas[row.best].toString();
                cols[i++] = row.ionTypes[row.best].toString();

                cols[i++] = row.bestNPCProps[row.best][0].getName();
                cols[i++] = Double.toString(row.bestNPCProbs[row.best][0]);

                cols[i++] = row.bestNPCProps[row.best][1].getName();
                cols[i++] = Double.toString(row.bestNPCProbs[row.best][1]);

                cols[i++] = row.bestNPCProps[row.best][2].getName();
                cols[i++] = Double.toString(row.bestNPCProbs[row.best][2]);

                cols[i++] = primaryClass.getName();
                cols[i++] = Double.toString(row.cfClassifications[row.best].getProbability(row.CLF.getIndexOfMolecularProperty(primaryClass)));

                if (lineage.length > 5) {
                    cols[i++] = lineage[5].getName();
                    cols[i++] = Double.toString(row.cfClassifications[row.best].getProbability(row.CLF.getIndexOfMolecularProperty(lineage[5])));
                } else {
                    cols[i++] = "";
                    cols[i++] = "";
                }

                if (lineage.length > 4) {
                    cols[i++] = lineage[4].getName();
                    cols[i++] = Double.toString(row.cfClassifications[row.best].getProbability(row.CLF.getIndexOfMolecularProperty(lineage[4])));
                } else {
                    cols[i++] = "";
                    cols[i++] = "";
                }

                if (lineage.length > 3) {
                    cols[i++] = lineage[3].getName();
                    cols[i++] = Double.toString(row.cfClassifications[row.best].getProbability(row.CLF.getIndexOfMolecularProperty(lineage[3])));
                } else {
                    cols[i++] = "";
                    cols[i++] = "";
                }

                if (lineage.length > 2) {
                    cols[i++] = lineage[2].getName();
                    cols[i++] = Double.toString(row.cfClassifications[row.best].getProbability(row.CLF.getIndexOfMolecularProperty(lineage[2])));
                } else {
                    cols[i++] = "";
                    cols[i++] = "";
                }

                cols[i++] = Joiner.on("; ").join(row.cfClassifications[row.best].asDeterministic().asArray().presentFingerprints().asMolecularPropertyIterator());
//                cols[i++] = Joiner.on("; ").join(row.npcClassifications[row.best].asDeterministic().asArray().presentFingerprints().asMolecularPropertyIterator());

                cols[i++] = row.featureId;
                ++k;
                return cols;

            } catch (ClassCastException e) {
                LoggerFactory.getLogger(CanopusSummaryWriter.class).error("Cannot cast CANOPUS fingerprint to ClassyFireFingerprintVersion.");
                ++k;
                return new String[0];
            }
        }
    }

    public static class IterateOverAdducts implements Iterator<String[]> {
        int k = 0, j = 0;
        String[] cols = new String[HEADER2.length];
        final List<CanopusSummaryRow> rows;

        public IterateOverAdducts(List<CanopusSummaryRow> rows) {
            this.rows = rows;
        }

        @Override
        public boolean hasNext() {
            return k < rows.size();
        }

        @Override
        public String[] next() {
            try {
                final CanopusSummaryRow row = rows.get(k);
                final ClassyfireProperty primaryClass = row.mostSpecificClasses[j];
                final ClassyfireProperty[] lineage = primaryClass.getLineage();
                int i = 0;

                cols[i++] = row.id;
                cols[i++] = row.molecularFormulas[j].toString();
                cols[i++] = row.ionTypes[j].toString();
                cols[i++] = row.precursorFormulas[j].toString();

                cols[i++] = row.bestNPCProps[j][0].getName();
                cols[i++] = Double.toString(row.bestNPCProbs[j][0]);

                cols[i++] = row.bestNPCProps[j][1].getName();
                cols[i++] = Double.toString(row.bestNPCProbs[j][1]);

                cols[i++] = row.bestNPCProps[j][2].getName();
                cols[i++] = Double.toString(row.bestNPCProbs[j][2]);

                cols[i++] = primaryClass.getName();
                cols[i++] = Double.toString(row.cfClassifications[j].getProbability(row.CLF.getIndexOfMolecularProperty(primaryClass)));

                if (lineage.length > 5) {
                    cols[i++] = lineage[5].getName();
                    cols[i++] = Double.toString(row.cfClassifications[j].getProbability(row.CLF.getIndexOfMolecularProperty(lineage[5])));
                } else {
                    cols[i++] = "";
                    cols[i++] = "";
                }

                if (lineage.length > 4) {
                    cols[i++] = lineage[4].getName();
                    cols[i++] = Double.toString(row.cfClassifications[j].getProbability(row.CLF.getIndexOfMolecularProperty(lineage[4])));
                } else {
                    cols[i++] = "";
                    cols[i++] = "";
                }

                if (lineage.length > 3) {
                    cols[i++] = lineage[3].getName();
                    cols[i++] = Double.toString(row.cfClassifications[j].getProbability(row.CLF.getIndexOfMolecularProperty(lineage[3])));
                } else {
                    cols[i++] = "";
                    cols[i++] = "";
                }

                if (lineage.length > 2) {
                    cols[i++] = lineage[2].getName();
                    cols[i++] = Double.toString(row.cfClassifications[j].getProbability(row.CLF.getIndexOfMolecularProperty(lineage[2])));
                } else {
                    cols[i++] = "";
                    cols[i++] = "";
                }

                cols[i++] = Joiner.on("; ").join(row.cfClassifications[j].asDeterministic().asArray().presentFingerprints().asMolecularPropertyIterator());
//                cols[i++] = Joiner.on("; ").join(row.npcClassifications[j].asDeterministic().asArray().presentFingerprints().asMolecularPropertyIterator());

                cols[i++] = row.featureId;


                ++j;
                if (j >= rows.get(k).cfClassifications.length) {
                    j = 0;
                    ++k;
                }
                return cols;
            } catch (ClassCastException e) {
                LoggerFactory.getLogger(CanopusSummaryWriter.class).error("Cannot cast CANOPUS fingerprint to ClassyFireFingerprintVersion.");
                ++k;
                return new String[0];
            }
        }
    }
}