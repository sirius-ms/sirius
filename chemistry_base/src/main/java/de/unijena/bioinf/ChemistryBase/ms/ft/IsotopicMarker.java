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

package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ms.annotations.DataAnnotation;

/**
 * An annotation for pseudo losses and pseudo fragments.
 * These objects have to be removed from the tree after computation. But their information might be
 * transferred into some annotation object of the tree.
 */
public class IsotopicMarker implements DataAnnotation {

    private static final IsotopicMarker IS_ISOTOPE = new IsotopicMarker(true), IS_NOT = new IsotopicMarker(false);

    private final boolean isotope;

    protected IsotopicMarker() {
        this(false);
    }

    private IsotopicMarker(boolean is) {
        this.isotope = is;
    }

    public boolean isIsotope() {
        return isotope;
    }

    public static IsotopicMarker is() {
        return IS_ISOTOPE;
    }

    public static IsotopicMarker isNot() {
        return IS_NOT;
    }



}
