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

package de.unijena.bioinf.ms.gui.ms_viewer.data;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FastIsotopePatternGenerator;
import gnu.trove.list.array.TIntArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SiriusIsotopePattern {
    protected final Spectrum<? extends Peak> spectrum;

    protected SimpleSpectrum isotopePattern;
    protected MolecularFormula patternFormula;
    protected SimpleSpectrum simulatedPattern;
    protected int[] indizes;

    @Nullable
    public static SiriusIsotopePattern create(@Nullable FTree tree, @Nullable Ms2Experiment exp, @Nullable Spectrum<? extends Peak> spectrum){
        if (spectrum == null)
            return null;
        if (tree == null)
            return null;
        SiriusIsotopePattern it = new SiriusIsotopePattern(spectrum);
        it.annotate(tree, exp);
        if (it.isotopePattern == null)
            return null;
        it.simulate(exp, tree);
        if (it.simulatedPattern == null)
            return null;
        return it;
    }

    protected SiriusIsotopePattern(Spectrum<? extends Peak> spectrum) {
        this.spectrum = spectrum;
    }

    private void simulate(Ms2Experiment exp, FTree tree) {
        final MolecularFormula formula = getPrecursorFormula(exp, tree);
        final FastIsotopePatternGenerator gen = new FastIsotopePatternGenerator(Normalization.Max);
        gen.setMinimalProbabilityThreshold(Math.min(0.005, Spectrums.getMinimalIntensity(isotopePattern)));
        gen.setMaximalNumberOfPeaks(Math.max(4, isotopePattern.size()));
        this.simulatedPattern = gen.simulatePattern(formula, tree.getAnnotation(PrecursorIonType.class).orElse(exp.getPrecursorIonType()).getIonization());
    }

    //TODO: we should make a method for that somewhere else
    private MolecularFormula getPrecursorFormula(Ms2Experiment exp, FTree tree) {
        PrecursorIonType ionType = tree.getAnnotation(PrecursorIonType.class).orElse(PrecursorIonType.unknown(exp.getPrecursorIonType().getCharge()));
        return tree.getRoot().getFormula().subtract(ionType.getInSourceFragmentation()).add(ionType.getAdduct());
    }

    public String getMolecularFormula(int index) {
        for (int j=0; j < indizes.length; ++j) {
            if (indizes[j]==index) {
                return patternFormula + " (" + (j==0 ? "monoisotopic" : ("+" + j + " isotope peak")) + ")";
            }
        }
        return null;
    }

	public SimpleSpectrum getIsotopePattern(){
		return isotopePattern;
	}

    private SimpleSpectrum annotate(@Nullable FTree tree, @NotNull Ms2Experiment exp) {
        final IsotopePattern pattern = tree != null ? tree.getAnnotationOrNull(IsotopePattern.class) : null;
        if (pattern != null) {
            isotopePattern = pattern.getPattern();
            patternFormula = pattern.getCandidate();
        } else {
            isotopePattern = Spectrums.extractIsotopePattern(spectrum, exp.getAnnotationOrDefault(MS1MassDeviation.class), exp.getIonMass(), exp.getPrecursorIonType().getCharge(), true);
            patternFormula = tree != null ? tree.getRoot().getFormula() : null;
        }
        final TIntArrayList indizes = new TIntArrayList();
        // find isotope peaks in spectrum
        if (isotopePattern != null) {
            for (Peak p : isotopePattern) {
                final int i =  Spectrums.mostIntensivePeakWithin(spectrum, p.getMass(), new Deviation(1, 0.1));
                if (i >= 0) indizes.add(i);
            }
        }
        this.indizes = indizes.toArray();
        return isotopePattern;
    }
}
