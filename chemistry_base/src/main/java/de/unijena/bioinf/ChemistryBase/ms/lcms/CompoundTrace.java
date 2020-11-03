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

package de.unijena.bioinf.ChemistryBase.ms.lcms;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * A compound has traces of adducts and in-source fragments
 */
public class CompoundTrace extends IonTrace implements Iterable<IonTrace> {

    @Nonnull protected final IonTrace[] adducts;
    @Nonnull protected final IonTrace[] inSourceFragments;

    public CompoundTrace(@Nonnull Trace[] isotopes, @Nonnull IonTrace[] adducts, @Nonnull IonTrace[] inSourceFragments) {
        super(isotopes);
        this.adducts = adducts;
        this.inSourceFragments = inSourceFragments;
    }

    @Nonnull
    public IonTrace[] getAdducts() {
        return adducts;
    }

    @Nonnull
    public IonTrace[] getInSourceFragments() {
        return inSourceFragments;
    }

    @NotNull
    @Override
    public Iterator<IonTrace> iterator() {
        return Iterators.concat(Collections.singleton(this).iterator(), Arrays.asList(adducts).iterator(),Arrays.asList(inSourceFragments).iterator());
    }

    @Override
    public void forEach(Consumer<? super IonTrace> action) {
        action.accept(this);
        for (IonTrace tr : adducts) action.accept(tr);
        for (IonTrace tr : inSourceFragments) action.accept(tr);
    }
}
