/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 * Chair of Bioinformatics, Friedrich-Schilller University.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.DecompositionScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.PredefinedPeak;
import de.unijena.bioinf.sirius.PeakAnnotation;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.DecompositionList;

public class PredefinedPeakPlugin extends SiriusPlugin {

    @Override
    public void initializePlugin(PluginInitializer initializer) {
        initializer.addFragmentScorer(new PredefinedPeakScorer());
    }

    @Override
    protected DecompositionList transformDecompositionList(ProcessedInput input, ProcessedPeak peak, DecompositionList list) {
        final PeakAnnotation<PredefinedPeak> peakAnnotation = input.getPeakAnnotations().get(PredefinedPeak.class);
        if (peakAnnotation!=null && peakAnnotation.get(peak)!=null) {
            return peakAnnotation.get(peak).toDecompositionList();
        } else return super.transformDecompositionList(input,peak,list);
    }

    @Called("Predefined")
    public static class PredefinedPeakScorer implements DecompositionScorer<PeakAnnotation<PredefinedPeak>> {
        @Override
        public PeakAnnotation<PredefinedPeak> prepare(ProcessedInput input) {
            return input.getPeakAnnotations().get(PredefinedPeak.class);
        }
        @Override
        public double score(MolecularFormula formula, Ionization ion, ProcessedPeak peak, ProcessedInput input, PeakAnnotation<PredefinedPeak> precomputed) {
            if (precomputed==null) return 0d;
            PredefinedPeak pk = precomputed.get(peak);
            if (pk != null && pk.getPeakFormula().equals(formula) && pk.getIonization().equals(ion)) {
                return 1000d;
            } else return 0d;
        }
        @Override
        public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }
    }
}
