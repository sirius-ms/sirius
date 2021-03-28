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
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.Summarizer;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.util.Iterators;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class CanopusSummaryWriter implements Summarizer {

    protected static class CanopusSummaryRow {
        private final ArrayFingerprint[] classifications;
        private final MolecularFormula[] molecularFormulas, precursorFormulas;
        private final ClassyfireProperty[] mostSpecificClasses;
        private final PrecursorIonType[] ionTypes;
        private final String id;
        private final int best;

        public CanopusSummaryRow(ProbabilityFingerprint[] classifications, MolecularFormula[] molecularFormulas, MolecularFormula[] precursorFormulas, PrecursorIonType[] ionTypes, String id) {
            this.classifications = Arrays.stream(classifications).map(x->x.asDeterministic().asArray()).toArray(ArrayFingerprint[]::new);
            this.molecularFormulas = molecularFormulas;
            this.precursorFormulas = precursorFormulas;
            this.mostSpecificClasses = new ClassyfireProperty[molecularFormulas.length];
            this.ionTypes = ionTypes;
            this.id = id;
            this.best = chooseBestAndAssignPrimaryClasses(classifications);
        }

        private int chooseBestAndAssignPrimaryClasses(ProbabilityFingerprint[] classifications) {
            final ClassyFireFingerprintVersion CLF;
            FingerprintVersion v = classifications[0].getFingerprintVersion();
            if (v instanceof MaskedFingerprintVersion)  v = ((MaskedFingerprintVersion)v).getMaskedFingerprintVersion();
            CLF = (ClassyFireFingerprintVersion) v;
            ClassyfireProperty bestClass = CLF.getPrimaryClass(classifications[0]);
            mostSpecificClasses[0] = bestClass;
            // choose the classification with the highest probability for the most specific class, starting
            // with probabilities above 50%
            if (classifications.length==1) return 0;

            int argmax = 0;
            double bestProb = classifications[0].getProbability(CLF.getIndexOfMolecularProperty(bestClass));
            for (int i=1; i < classifications.length; ++i) {
                ClassyfireProperty primary = CLF.getPrimaryClass(classifications[i]);
                mostSpecificClasses[i] = primary;
                final double prob = classifications[i].getProbability(CLF.getIndexOfMolecularProperty(primary));
                final int ord = new ClassyfireProperty.CompareCompoundClassDescriptivity().compare(primary, bestClass);
                if (ord > 0  || (ord == 0 && prob > bestProb )) {
                    argmax = i;
                    bestClass = primary;
                    bestProb = prob;
                }
            }
            return argmax;
        }
    }

    private List<CanopusSummaryRow> rows;

    public CanopusSummaryWriter() {
        this.rows = new ArrayList<>();
    }

    @Override
    public List<Class<? extends DataAnnotation>> requiredFormulaResultAnnotations() {
        return Arrays.asList(CanopusResult.class);
    }

    @Override
    public void addWriteCompoundSummary(ProjectWriter writer, @NotNull CompoundContainer exp, List<? extends SScored<FormulaResult, ? extends FormulaScore>> results) throws IOException {
        if (results.size()>0) {
            // sometimes we have multiple results with same score (adducts!). In this case, we list all of them in
            // a separate summary file
            int i=0;
            SScored<FormulaResult, ? extends FormulaScore> hit;
            ArrayList<ProbabilityFingerprint> fingerprints = new ArrayList<>();
            ArrayList<MolecularFormula> formulas = new ArrayList<>(), preForms = new ArrayList<>();
            ArrayList<PrecursorIonType> ionTypes = new ArrayList<>();
            FormulaResultId id;
            do {
                hit = results.get(i);
                id = hit.getCandidate().getId();
                final Optional<CanopusResult> cr = hit.getCandidate().getAnnotation(CanopusResult.class);
                final var cid = id;
                cr.ifPresent(canopusResult -> {
                    fingerprints.add(canopusResult.getCanopusFingerprint());
                    formulas.add(cid.getMolecularFormula());
                    ionTypes.add(cid.getIonType());
                    preForms.add(cid.getPrecursorFormula());
                });
                ++i;
            } while (i < results.size() && results.get(i).getScoreObject().compareTo(hit.getScoreObject()) >= 0);
            if (fingerprints.size()>0) {
                this.rows.add(new CanopusSummaryRow(fingerprints.toArray(ProbabilityFingerprint[]::new), formulas.toArray(MolecularFormula[]::new), preForms.toArray(MolecularFormula[]::new), ionTypes.toArray(PrecursorIonType[]::new), id.getParentId().getDirectoryName()));
            }
        }

    }

    @Override
    public void writeProjectSpaceSummary(ProjectWriter writer) throws IOException {
        writer.table(SummaryLocations.CANOPUS_SUMMARY, HEADER, Iterators.capture(new IterateOverFormulas()));
        writer.table(SummaryLocations.CANOPUS_SUMMARY_ADDUCT, HEADER2, Iterators.capture(new IterateOverAdducts()));
    }

    private final static String[] HEADER = new String[]{ "name","molecularFormula", "adduct", "most specific class", "level 5", "subclass", "class",
            "superclass", "all classifications"},
            HEADER2 = new String[]{ "name","molecularFormula", "adduct", "precursorFormula", "most specific class", "level 5", "subclass", "class",
                    "superclass", "all classifications"};

    public class IterateOverFormulas implements Iterator<String[]> {
            int k = 0;
            String[] cols = new String[HEADER.length];

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

                    cols[0] = row.id;
                    cols[1] = row.molecularFormulas[row.best].toString();
                    cols[2] = row.ionTypes[row.best].toString();
                    cols[3] = primaryClass.getName();

                    cols[7] = lineage.length > 2 ? lineage[2].getName() : "";
                    cols[6] = lineage.length > 3 ? lineage[3].getName() : "";
                    cols[5] = lineage.length > 4 ? lineage[4].getName() : "";
                    cols[4] = lineage.length > 5 ? lineage[5].getName() : "";

                    cols[8] = Joiner.on("; ").join(row.classifications[row.best].presentFingerprints().asMolecularPropertyIterator());
                    ++k;
                    return cols;

                } catch (ClassCastException e) {
                    LoggerFactory.getLogger(CanopusSummaryWriter.class).error("Cannot cast CANOPUS fingerprint to ClassyFireFingerprintVersion.");
                    ++k;
                    return new String[0];
                }
            }
        }
    public class IterateOverAdducts implements Iterator<String[]> {
        int k = 0, j=0;
        String[] cols = new String[HEADER2.length];

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

                cols[0] = row.id;
                cols[1] = row.molecularFormulas[j].toString();
                cols[2] = row.ionTypes[j].toString();
                cols[3] = row.precursorFormulas[j].toString();
                cols[4] = primaryClass.getName();

                cols[8] = lineage.length > 2 ? lineage[2].getName() : "";
                cols[7] = lineage.length > 3 ? lineage[3].getName() : "";
                cols[6] = lineage.length > 4 ? lineage[4].getName() : "";
                cols[5] = lineage.length > 5 ? lineage[5].getName() : "";

                cols[9] = Joiner.on("; ").join(row.classifications[j].presentFingerprints().asMolecularPropertyIterator());
                ++j;
                if (j >= rows.get(k).classifications.length) {
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
