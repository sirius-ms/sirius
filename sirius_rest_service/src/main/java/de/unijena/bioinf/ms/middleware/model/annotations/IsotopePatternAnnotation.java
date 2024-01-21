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

package de.unijena.bioinf.ms.middleware.model.annotations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.MsExperiments;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FastIsotopePatternGenerator;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import gnu.trove.list.array.TIntArrayList;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class IsotopePatternAnnotation {
    protected AnnotatedSpectrum isotopePattern;
    protected AnnotatedSpectrum simulatedPattern;
    protected AnnotatedSpectrum ms1;
    protected int[] peaksInMs1;

    @NotNull
    public static IsotopePatternAnnotation create(@Nullable Ms2Experiment exp, @Nullable Spectrum<Peak> spectrum) {
        return create(null, exp, spectrum);
    }

    @NotNull
    public static IsotopePatternAnnotation create(@Nullable FTree tree, @Nullable Ms2Experiment exp) {
        if (exp == null)
            return new IsotopePatternAnnotation();
        return create(tree, exp, exp.getMergedMs1Spectrum());
    }

    @NotNull
    public static IsotopePatternAnnotation create(@Nullable FTree tree, @Nullable Ms2Experiment exp, @Nullable Spectrum<Peak> spectrum) {
        IsotopePatternAnnotation it = new IsotopePatternAnnotation();
        if (spectrum == null || exp == null)
            return it;
        it.setMs1(new AnnotatedSpectrum(spectrum));
        it.annotate(tree, spectrum, exp);
        if (it.isotopePattern != null)
            it.simulate(exp, tree);
        return it;
    }

    @JsonIgnore
    private void simulate(Ms2Experiment exp, FTree tree) {
        final MolecularFormula formula = MsExperiments.extractPrecursorFormula(exp, tree);
        final FastIsotopePatternGenerator gen = new FastIsotopePatternGenerator(Normalization.Max);
        gen.setMinimalProbabilityThreshold(Math.min(0.005, Spectrums.getMinimalIntensity(isotopePattern)));
        gen.setMaximalNumberOfPeaks(Math.max(4, isotopePattern.size()));
        this.simulatedPattern = new AnnotatedSpectrum(gen.simulatePattern(formula, tree.getAnnotation(PrecursorIonType.class).orElse(exp.getPrecursorIonType()).getIonization()));
    }

    @JsonIgnore
    private void annotate(@Nullable FTree tree, @NotNull Spectrum<Peak> spectrum, @NotNull Ms2Experiment exp) {
        final IsotopePattern pattern = tree != null ? tree.getAnnotationOrNull(IsotopePattern.class) : null;
        if (pattern != null) {
            isotopePattern = new AnnotatedSpectrum(pattern.getPattern());
//            patternFormula = pattern.getCandidate();
        } else {
            isotopePattern = new AnnotatedSpectrum(Spectrums.extractIsotopePattern(spectrum, exp.getAnnotationOrDefault(MS1MassDeviation.class), exp.getIonMass(), exp.getPrecursorIonType().getCharge(), true));
//            patternFormula = tree != null ? tree.getRoot().getFormula() : null;
        }
        final TIntArrayList indizes = new TIntArrayList();
        // find isotope peaks in spectrum
        for (Peak p : isotopePattern) {
            final int i = Spectrums.mostIntensivePeakWithin(spectrum, p.getMass(), new Deviation(1, 0.1));
            if (i >= 0) indizes.add(i);
        }
        this.peaksInMs1 = indizes.toArray();
    }
}
