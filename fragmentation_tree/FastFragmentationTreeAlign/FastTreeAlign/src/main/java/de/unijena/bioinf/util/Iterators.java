
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * @author Kai Dührkop
 */
public class Iterators {

    public static <T> ListIterator<T> singleton(T instance) {
        return new SingletonIterator<T>(instance);
    }
    
    public static <T> ListIterator<T> empty() {
        return new EmptyIterator<T>();
    }
    
    public static <T> ListIterator<T> pair(T left, T right) {
        return new Iterator2<T>(left, right);
    }
    
    public static <T> ListIterator<T> array(T... array) {
        switch (array.length) {
            case 0: return new EmptyIterator<T>();
            case 1: return new SingletonIterator<T>(array[0]);
            case 2: return new Iterator2<T>(array[0], array[1]);
            default: return Arrays.asList(array).listIterator();
        }
    }

    public static <T> Iterable<T> capture(final Iterator<T> iterable) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return iterable;
            }
        };
    }

    public static <T> void rewind(ListIterator<T> iter) {
        while (iter.hasPrevious()) iter.previous();
    }

    public static <T> void forward(Iterator<T> iter) {
        while (iter.hasNext()) iter.next();
    }

    private static abstract class BidirectionalIterator<T> implements ListIterator<T> {

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(T t) {
            throw new UnsupportedOperationException();
        }
    }

    private final static class EmptyIterator<T> extends BidirectionalIterator<T> {

        @Override
        public boolean hasPrevious() {
            return false;
        }

        @Override
        public T previous() {
            throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
            return 0;
        }

        @Override
        public int previousIndex() {
            return -1;
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public T next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    

    private final static class SingletonIterator<T> extends BidirectionalIterator<T> {

        private final T instance;
        private boolean hasNext;
        
        private SingletonIterator(T instance) {
            this.instance = instance;
            this.hasNext = true;
        }
        
        @Override
        public boolean hasPrevious() {
            return !hasNext;
        }

        @Override
        public T previous() {
            if (hasNext) throw new NoSuchElementException();
            hasNext = true;
            return instance;
        }

        @Override
        public int nextIndex() {
            return hasNext ? 0 : 1;
        }

        @Override
        public int previousIndex() {
            return hasNext ? -1 : 0;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public T next() {
            if (!hasNext) throw new NoSuchElementException();
            hasNext = false;
            return instance;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final static class Iterator2<T> extends BidirectionalIterator<T> {
        private final T e1;
        private final T e2;
        private byte i;

        private Iterator2(T e1, T e2) {
            this.e1 = e1;
            this.e2 = e2;
            this.i = 0;
        }

        @Override
        public boolean hasPrevious() {
            return i > 0;
        }

        @Override
        public T previous() {
            if (i <= 0) throw new NoSuchElementException();
            --i;
            return (i == 1 ? e1 : e2);
        }

        @Override
        public int nextIndex() {
            return i;
        }

        @Override
        public int previousIndex() {
            return i-1;
        }

        @Override
        public boolean hasNext() {
            return i < 2;
        }

        @Override
        public T next() {
            if (i >= 2) throw new NoSuchElementException();
            ++i;
            return (i == 1 ? e1 : e2);
        }
    }

}
