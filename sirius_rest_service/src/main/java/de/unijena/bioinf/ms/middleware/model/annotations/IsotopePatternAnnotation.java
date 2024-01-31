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
import de.unijena.bioinf.ChemistryBase.ms.MS1MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FastIsotopePatternGenerator;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.middleware.model.spectra.Spectrums;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class IsotopePatternAnnotation {
    protected BasicSpectrum isotopePattern;
    protected BasicSpectrum simulatedPattern;

    @NotNull
    public static IsotopePatternAnnotation create(@NotNull Ms2Experiment exp, @Nullable FTree tree) {
        IsotopePatternAnnotation it = new IsotopePatternAnnotation();
        it.annotate(exp, tree);
        if (tree != null && it.isotopePattern != null)
            it.simulate(exp, tree);
        return it;
    }

    @JsonIgnore
    private void simulate(@NotNull Ms2Experiment exp, @NotNull FTree tree) {
        final MolecularFormula formula = extractPrecursorFormula(tree, exp.getPrecursorIonType());
        final FastIsotopePatternGenerator gen = new FastIsotopePatternGenerator(Normalization.Max);
        gen.setMinimalProbabilityThreshold(Math.min(0.005, de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.getMinimalIntensity(isotopePattern)));
        gen.setMaximalNumberOfPeaks(Math.max(4, isotopePattern.size()));
        this.simulatedPattern = new BasicSpectrum(gen.simulatePattern(formula,
                tree.getAnnotation(PrecursorIonType.class).orElse(exp.getPrecursorIonType()).getIonization()));
    }

    public static MolecularFormula extractPrecursorFormula(@NotNull FTree tree, PrecursorIonType fallback) {
        PrecursorIonType ionType = tree.getAnnotation(PrecursorIonType.class).orElse(fallback);
        return tree.getRoot().getFormula().subtract(ionType.getInSourceFragmentation()).add(ionType.getAdduct());
    }

    @JsonIgnore
    private void annotate(@NotNull Ms2Experiment exp, @Nullable FTree tree) {
        final IsotopePattern pattern = tree != null ? tree.getAnnotationOrNull(IsotopePattern.class) : null;

        if (pattern != null) {
            isotopePattern = new BasicSpectrum(pattern.getPattern());
        } else {
            BasicSpectrum msms = Spectrums.createMergedMsMs(exp);
            isotopePattern = new BasicSpectrum(de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.extractIsotopePattern(
                    msms,
                    exp.getAnnotationOrDefault(MS1MassDeviation.class),
                    exp.getIonMass(),
                    exp.getPrecursorIonType().getCharge(),
                    true));
        }
    }
}
