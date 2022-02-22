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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ms.annotations.DataAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Predicate;

public class CompoundContainerIterator implements Iterator<CompoundContainer> {

    private final SiriusProjectSpace space;
    private final Iterator<CompoundContainerId> sourceIterator;
    private final Predicate<CompoundContainer> filter;
    private final Class<? extends DataAnnotation>[] components;

    CompoundContainer next = null;

    public CompoundContainerIterator(@NotNull SiriusProjectSpace space, @NotNull Class<? extends DataAnnotation>... components) {
        this.space = space;
        this.sourceIterator = this.space.iterator();
        this.filter = (c) -> true;
        this.components = components;
    }

    public CompoundContainerIterator(@NotNull SiriusProjectSpace space, @Nullable Predicate<CompoundContainerId> prefilter, @Nullable Predicate<CompoundContainer> filter, @NotNull Class<? extends DataAnnotation>... components) {
        this.space = space;
        this.sourceIterator = prefilter != null ? this.space.filteredIterator(prefilter) : this.space.iterator();
        this.filter = filter != null ? filter : (c) -> true;
        this.components = components;
    }

    @Override
    public boolean hasNext() {
        if (next != null)
            return true;

        if (sourceIterator.hasNext()) {
            final CompoundContainerId cid = sourceIterator.next();
            try {
                CompoundContainer c = space.getCompound(cid, components);
                if (!filter.test(c)) {
                    LoggerFactory.getLogger(getClass()).info("Skipping instance " + cid.getDirectoryName() + " because it does not match the Filter criterion.");
                    return hasNext();
                } else {
                    next = c;
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                LoggerFactory.getLogger(getClass()).debug("Could not parse Compound with ID '" + cid.getDirectoryName() + "' Skipping it!", e);
                LoggerFactory.getLogger(getClass()).error("Could not parse Compound with ID '" + cid.getDirectoryName() + "' Skipping it!");
                return hasNext();
            }
        }
        return false;
    }

    @Override
    public CompoundContainer next() {
        try {
            if (!hasNext())
                return null;
            return next;
        } finally {
            next = null;
        }
    }


}
