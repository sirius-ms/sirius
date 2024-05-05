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

package de.unijena.bioinf.ChemistryBase.utils;

import de.unijena.bioinf.ms.annotations.DataAnnotation;

import java.io.IOException;
import java.util.Optional;

public abstract class IOFunctions {

    public interface IOCallable<R> {
        R call() throws IOException;
    }

    public interface IORunnable {
        void run() throws IOException;
    }

    public interface IOFunction<A, R> {
        R apply(A a) throws IOException;
    }

    public interface BiIOFunction<A, B, R> {
        R apply(A a, B b) throws IOException;
    }

    public interface TriIOFunction<A, B, C, R> {
        R apply(A a, B b, C c) throws IOException;
    }

    //Quad are already a bit too many parameters, we should not do more...
    public interface QuadIOFunction<A, B, C, D, R> {
        R apply(A a, B b, C c, D d) throws IOException;
    }

    public interface IOConsumer<A> {
        void accept(A a) throws IOException;
    }

    public interface BiIOConsumer<A, B> {
        void accept(A a, B b) throws IOException;
    }

    public interface IOSupplier<R> {
        R get() throws IOException;
    }

    public interface ClassValueProducer {
        <T extends DataAnnotation> Optional<T> apply(Class<T> klass) throws IOException;
    }

    public interface ClassValueConsumer {
        <T extends DataAnnotation> void apply(Class<T> klass, T value) throws IOException;
    }

}
