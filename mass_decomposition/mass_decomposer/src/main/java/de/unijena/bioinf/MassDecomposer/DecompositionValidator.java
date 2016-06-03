/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.MassDecomposer;

/**
 * Filter out all decompositions which can't be valid/meaningful
 */
public interface DecompositionValidator<T> {

    /**
     * @param compomere a decomposition. compomere[i] is corresponding to character c_{characterIds[i]} in the alphabet
     * @param characterIds an immutable(!!!) array with indizes of the used characters in the same order as in the compomere
     * @return true, if the decomposition should be contained in the decomposition output
     */
    public boolean validate(int[] compomere, int[] characterIds, Alphabet<T> alphabet);

}
