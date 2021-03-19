
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

package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.TableSelection;

import java.util.*;

public class ElementMap<T> implements Map<Element, T> {

    private final Object[] values;
    private final TableSelection selection;
    private int size;

    public ElementMap(TableSelection selection) {
        this.selection = selection;
        this.values = new Object[selection.size()];
        this.size = 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(Object o) {
        return get(o) != null;
    }

    @Override
    public boolean containsValue(Object o) {
        if (o == null) throw new NullPointerException("Map does not allow null values");
        for (int i=0; i < values.length; ++i) {
            if (o.equals(values[i])) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
	@Override
    public T get(Object o) {
        if (o == null) throw new NullPointerException("Map does not allow null keys");
        if (!(o instanceof Element)) throw new ClassCastException("Map expect key of type Element");
        return (T)values[selection.indexOf((Element)o)];
    }

    @SuppressWarnings("unchecked")
	@Override
    public T put(Element entry, T t) {
        if (entry == null) throw new NullPointerException("Map does not allow null keys");
        if (t == null) throw new NullPointerException("Map does not allow null values");
        final int index = selection.indexOf(entry);
        final Object oldValue = values[index];
        if (oldValue == null) ++size;
        values[index] = t;
        return (T)oldValue;
    }

    @SuppressWarnings("unchecked")
	@Override
    public T remove(Object o) {
        if (o == null) throw new NullPointerException("Map does not allow null keys");
        if (!(o instanceof Element)) throw new ClassCastException("Map expect key of type Element");
        final int index = selection.indexOf((Element)o);
        final T value = (T)values[index];
        values[index] = null;
        if (value != null) --size;
        return value;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    public void putAll(Map<? extends Element, ? extends T> map) {
        if (map instanceof ElementMap) {
            putAll((ElementMap)map);
        } else {
            mergeMaps(map);
        }
    }

    public void putAll(ElementMap<T> map) {
        if (map.selection != selection) {
            mergeMaps(map);
        } else {
            for (int i=0; i < values.length; ++i) {
                if (map.values[i] != null) {
                    if (values[i] == null) ++size;
                    values[i] = map.values[i];
                }
            }
        }
    }

    private void mergeMaps(Map<? extends Element, ? extends T> map) {
        for (Map.Entry<? extends Element, ? extends T> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        Arrays.fill(values, null);
        size = 0;
    }

    @Override
    public Set<Element> keySet() {
        return new AbstractSet<Element>() {
            @Override
            public Iterator<Element> iterator() {
                return new KeyIterator();
            }

            @Override
            public boolean contains(Object o) {
                return get(o) != null;
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    @Override
    public Collection<T> values() {
        return new AbstractCollection<T>() {

            @Override
            public Iterator<T> iterator() {
                return new ValueIterator();
            }

            @Override
            public int size() {
                return ElementMap.this.size();
            }
        };
    }

    @Override
    public Set<Entry<Element, T>> entrySet() {
        return new AbstractSet<Entry<Element, T>>() {
            @Override
            public Iterator<Entry<Element, T>> iterator() {
                return new EntryIterator();
            }

            @Override
            public int size() {
                return ElementMap.this.size();
            }
        };
    }

    private final class EntryType implements Entry<Element, T> {
        private final int id;

        private EntryType(int id) {
            this.id = id;
        }

        @Override
        public Element getKey() {
            return selection.get(id);
        }

        @SuppressWarnings("unchecked")
		@Override
        public T getValue() {
            return (T)values[id];
        }

        @Override
        public T setValue(T t) {
            return put(getKey(), t);
        }
    }

    private abstract class AbstractIterator {
        private int i;
        private EntryType entry;
        private AbstractIterator() {
            i=0;
            while (i < values.length && values[i] == null) ++i;
            if (i < values.length) this.entry = new EntryType(i);
        }

        public boolean hasNext() {
            return i < values.length;
        }

        public EntryType nextEntry() {
            final EntryType oldEntry = entry;
            ++i;
            while (i < values.length && values[i] == null) ++i;
            if (i < values.length) this.entry = new EntryType(i);
            return oldEntry;
        }

        public void remove() {
            ElementMap.this.remove(entry.getKey());
        }
    }

    private final class EntryIterator extends AbstractIterator implements Iterator<Entry<Element, T>> {

        @Override
        public Entry<Element, T> next() {
            return nextEntry();
        }
    }

    private final class KeyIterator extends AbstractIterator implements Iterator<Element> {

        @Override
        public Element next() {
            return nextEntry().getKey();
        }
    }

    private final class ValueIterator extends AbstractIterator implements Iterator<T> {

        @Override
        public T next() {
            return nextEntry().getValue();
        }
    }

}
