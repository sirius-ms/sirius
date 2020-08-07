/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.ms_viewer.data;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import gnu.trove.list.array.TIntArrayList;

public class SiriusIsotopePattern extends SiriusSingleSpectrumModel{

    protected SimpleSpectrum isotopePattern;
    protected MolecularFormula patternFormula;
    protected int[] indizes;

    public SiriusIsotopePattern(FTree tree, Spectrum<? extends Peak> spectrum) {
        super(spectrum);
        annotate(tree);
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

    private void annotate(FTree tree) {
        final IsotopePattern pattern = tree.getAnnotationOrNull(IsotopePattern.class);
        if (pattern!=null) {
            isotopePattern = pattern.getPattern();
            patternFormula = pattern.getCandidate();
            final TIntArrayList indizes = new TIntArrayList();
            // find isotope peaks in spectrum
            for (Peak p : pattern.getPattern()) {
                final int i = findIndexOfPeak(p.getMass(), 0.1);
                if (i>=0) indizes.add(i);
            }
            this.indizes = indizes.toArray();
        } else {
            indizes = new int[0];
        }
    }
}
