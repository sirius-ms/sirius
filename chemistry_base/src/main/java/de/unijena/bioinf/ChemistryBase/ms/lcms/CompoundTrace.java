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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A compound has traces of adducts and in-source fragments
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CompoundTrace extends IonTrace implements Iterable<IonTrace> {

    @Nonnull
    @JsonProperty protected final IonTrace[] adducts;
    @Nonnull @JsonProperty protected final IonTrace[] inSourceFragments;

    @JsonCreator
    public CompoundTrace(@JsonProperty("isotopes") Trace[] isotopes, @JsonProperty("correlationScores") double[] correlationScores , @JsonProperty("adducts") IonTrace[] adducts, @JsonProperty("inSourceFragments") IonTrace[] inSourceFragments) {
        super(isotopes, correlationScores);
        this.adducts = adducts==null ? new IonTrace[0] : adducts;
        this.inSourceFragments = inSourceFragments==null ? new IonTrace[0] : inSourceFragments;
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
        return Stream.concat(Stream.of(this), Stream.concat(Stream.of(adducts), Stream.of(inSourceFragments))).iterator();
    }

    @Override
    public void forEach(Consumer<? super IonTrace> action) {
        action.accept(this);
        for (IonTrace tr : adducts) action.accept(tr);
        for (IonTrace tr : inSourceFragments) action.accept(tr);
    }
}
