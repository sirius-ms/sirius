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

public class SiriusIsotopePattern extends SiriusSingleSpectrumModel{

    protected SimpleSpectrum isotopePattern;
    protected MolecularFormula patternFormula;
    protected SimpleSpectrum simulatedPattern;
    protected int[] indizes;

    public SiriusIsotopePattern(FTree tree, Ms2Experiment exp, Spectrum<? extends Peak> spectrum) {
        super(spectrum);
        annotate(tree, exp);
        simulate(exp, tree, isotopePattern);
    }

    @Override
    public double minMz() {
        return Math.min(simulatedPattern.getMzAt(0), isotopePattern.getMzAt(0));
    }

    @Override
    public double maxMz() {
        return Math.max(simulatedPattern.getMzAt(simulatedPattern.size()-1), isotopePattern.getMzAt(isotopePattern.size()-1));
    }

    private void simulate(Ms2Experiment exp, FTree tree, SimpleSpectrum measuredPattern) {
        final MolecularFormula formula = getPrecursorFormula(exp, tree);
        final FastIsotopePatternGenerator gen = new FastIsotopePatternGenerator(Normalization.Max);
        gen.setMinimalProbabilityThreshold(Math.min(0.005, Spectrums.getMinimalIntensity(measuredPattern)));
        gen.setMaximalNumberOfPeaks(Math.max(4, measuredPattern.size()));
        this.simulatedPattern = gen.simulatePattern(formula, tree.getAnnotation(PrecursorIonType.class).orElse(exp.getPrecursorIonType()).getIonization());
    }

    //TODO: we should make a method for that somewhere else
    private MolecularFormula getPrecursorFormula(Ms2Experiment exp, FTree tree) {
        PrecursorIonType ionType = tree.getAnnotation(PrecursorIonType.class).orElse(PrecursorIonType.unknown(exp.getPrecursorIonType().getCharge()));
        return tree.getRoot().getFormula().subtract(ionType.getInSourceFragmentation()).add(ionType.getAdduct());
    }

    @Override
    public String getMolecularFormula(int index) {
        for (int j=0; j < indizes.length; ++j) {
            if (indizes[j]==index) {
                return patternFormula + " (" + (j==0 ? "monoisotopic" : ("+" + j + " isotope peak")) + ")";
            }
        }
        return null;
    }

    @Override
    public boolean isIsotope(int index) {
        for (int i : indizes)
            if (i==index) return true;
        return false;
    }

	public SimpleSpectrum getIsotopePattern(){
		return isotopePattern;
	}

    private SimpleSpectrum annotate(FTree tree, Ms2Experiment exp) {
        final IsotopePattern pattern = tree.getAnnotationOrNull(IsotopePattern.class);
        if (pattern != null) {
            isotopePattern = pattern.getPattern();
            patternFormula = pattern.getCandidate();
        } else {
            isotopePattern = Spectrums.extractIsotopePattern(spectrum, exp.getAnnotationOrDefault(MS1MassDeviation.class), exp.getIonMass(), exp.getPrecursorIonType().getCharge(), true);
            patternFormula = tree.getRoot().getFormula();
        }
        final TIntArrayList indizes = new TIntArrayList();
        // find isotope peaks in spectrum
        if (isotopePattern != null) {
            for (Peak p : isotopePattern) {
                final int i = findIndexOfPeak(p.getMass(), 0.1);
                if (i >= 0) indizes.add(i);
            }
        }
        this.indizes = indizes.toArray();
        return isotopePattern;
    }
}
