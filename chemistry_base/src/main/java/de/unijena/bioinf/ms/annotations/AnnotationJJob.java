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

package de.unijena.bioinf.ms.annotations;

import de.unijena.bioinf.jjobs.JJob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutionException;

public interface AnnotationJJob<D extends DataAnnotation, A extends Annotated> extends JJob<D> {
    default D annotate(@Nullable final D result, @NotNull final A annotateable) {
        if (result != null) {
            Class<D> clzz = (Class<D>) result.getClass();
            annotateable.setAnnotation(clzz, result);
        }
        return result;
    }

    default D awaitAndAnnotateResult(@NotNull final A annotateable) throws ExecutionException {
        return annotate(awaitResult(), annotateable);
    }

    default D takeAndAnnotateResult(@NotNull final A annotateable) {
        return annotate(takeResult(), annotateable);
    }
}
