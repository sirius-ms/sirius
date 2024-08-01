
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

package de.unijena.bioinf.MassDecomposer.Chemistry;


import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.MassDecomposer.ValencyAlphabet;

import java.util.Map;

public class ChemicalAlphabetWrapper implements ValencyAlphabet<Element> {

    private final ChemicalAlphabet chemicalAlphabet;

    public ChemicalAlphabetWrapper(ChemicalAlphabet chemicalAlphabet) {
        this.chemicalAlphabet = chemicalAlphabet;
    }

    @Override
    public int valenceOf(int i) {
        return chemicalAlphabet.get(i).getValence();
    }

    @Override
    public int size() {
        return chemicalAlphabet.size();
    }

    @Override
    public double weightOf(int i) {
        return chemicalAlphabet.get(i).getMass();
    }

    @Override
    public Element get(int i) {
        return chemicalAlphabet.get(i);
    }

    @Override
    public int indexOf(Element character) {
        return chemicalAlphabet.indexOf(character);
    }

    @Override
    public <S> Map<Element, S> toMap() {
        return chemicalAlphabet.toMap();
    }

    public ChemicalAlphabet getAlphabet() {
        return chemicalAlphabet;
    }
}
