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

    public interface IOCallable<T> {
        public T call() throws IOException;
    }

    public interface IORunnable {
        public void run() throws IOException;
    }

    public interface IOFunction<A,B> {
        public B apply(A a) throws IOException;
    }

    public interface IOConsumer<A> {
        public void accept(A a) throws IOException;
    }

    public interface IOSupplier<A> {
        public A get() throws IOException;
    }

    public static interface ClassValueProducer {
        public <T extends DataAnnotation> Optional<T> apply(Class<T> klass) throws IOException;
    }

    public static interface ClassValueConsumer {
        public <T extends DataAnnotation> void apply(Class<T> klass, T value) throws IOException;
    }

}
