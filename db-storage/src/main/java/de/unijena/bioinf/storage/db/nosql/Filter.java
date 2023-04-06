/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.storage.db.nosql;

import javax.validation.constraints.NotNull;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class Filter {

    public enum FilterType {
        AND, OR, END, NOT, EQ, GT, GTE, LT, LTE, TEXT, REGEX, IN, NOT_IN, ELEM_MATCH
    }

    public static class FilterElement {

        public FilterType filterType;

        public FilterElement(FilterType filterType) {
            this.filterType = filterType;
        }
    }

    public static class FieldFilterElement extends FilterElement {

        public String field;
        public Object[] values;

        public FieldFilterElement(@NotNull FilterType filterType, @NotNull String field, Object... values) {
            super(filterType);
            this.field = field;
            this.values = values;
        }

    }

    public Deque<FilterElement> filterChain = new ArrayDeque<>();

    public Filter() {}

    public Filter and() {
        this.filterChain.addLast(new FilterElement(FilterType.AND));
        return this;
    }

    public Filter or() {
        this.filterChain.addLast(new FilterElement(FilterType.OR));
        return this;
    }

    public Filter end() {
        this.filterChain.addLast(new FilterElement(FilterType.END));
        return this;
    }

    public Filter not() {
        this.filterChain.addLast(new FilterElement(FilterType.NOT));
        return this;
    }

    public Filter eq(String field, Object value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.EQ, field, value));
        return this;
    }

    public Filter gt(String field, Object value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.GT, field, value));
        return this;
    }

    public Filter gte(String field, Object value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.GTE, field, value));
        return this;
    }

    public Filter lt(String field, Object value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.LT, field, value));
        return this;
    }

    public Filter lte(String field, Object value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.LTE, field, value));
        return this;
    }

    public Filter text(String field, String value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.TEXT, field, value));
        return this;
    }

    public Filter regex(String field, String value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.REGEX, field, value));
        return this;
    }

    public Filter inByte(String field, byte... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, objects));
        return this;
    }

    public Filter inShort(String field, short... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, objects));
        return this;
    }

    public Filter inInt(String field, int... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, Arrays.stream(values).boxed().toArray()));
        return this;
    }

    public Filter inLong(String field, long... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, Arrays.stream(values).boxed().toArray()));
        return this;
    }

    public Filter inFloat(String field, float... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, objects));
        return this;
    }

    public Filter inDouble(String field, double... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, Arrays.stream(values).boxed().toArray()));
        return this;
    }

    public Filter inBool(String field, boolean... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, objects));
        return this;
    }

    public Filter inChar(String field, char... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, objects));
        return this;
    }

    public Filter in(String field, Object... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, values));
        return this;
    }

    public Filter notInByte(String field, byte... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, objects));
        return this;
    }

    public Filter notInShort(String field, short... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, objects));
        return this;
    }

    public Filter notInInt(String field, int... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, Arrays.stream(values).boxed().toArray()));
        return this;
    }

    public Filter notInLong(String field, long... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, Arrays.stream(values).boxed().toArray()));
        return this;
    }

    public Filter notInFloat(String field, float... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, objects));
        return this;
    }

    public Filter notInDouble(String field, double... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, Arrays.stream(values).boxed().toArray()));
        return this;
    }

    public Filter notInBool(String field, boolean... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, objects));
        return this;
    }

    public Filter notInChar(String field, char... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, objects));
        return this;
    }

    public Filter notIn(String field, Object... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, values));
        return this;
    }

    public Filter elemMatch(String field) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.ELEM_MATCH, field));
        return this;
    }

}
