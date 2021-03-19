
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.MassDecomposer;

/**
 * A decomposition validator. It allows only decomposition which form a fully connected graph using a valency alphabet
 */
public class ValenceValidator<T> implements DecompositionValidator<T> {

    private final int minValence;

    public ValenceValidator(double minValence) {
        this.minValence = (int)(2*minValence);
    }

    public ValenceValidator() {
        this(-0.5d);
    }

    public int getMinValence() {
        return minValence;
    }

    @Override
    public boolean validate(int[] compomere, int[] characterIds, Alphabet<T> alphabet) {
        if (!(alphabet instanceof ValencyAlphabet))
            throw new RuntimeException("Validator can only be used for valency alphabets");
        final ValencyAlphabet<T> characters = (ValencyAlphabet<T>)alphabet;
        // rdbe * 2 = number of unsaturated bounds in molecular graph.
        int rdbe = 2;
        for (int i=0; i < compomere.length; ++i) {
            final int valence = characters.valenceOf(characterIds[i]);
            rdbe += (valence-2) * compomere[i];
        }
        return rdbe >= minValence;
    }
}
