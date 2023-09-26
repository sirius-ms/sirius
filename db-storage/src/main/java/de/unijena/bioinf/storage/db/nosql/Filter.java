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

import org.dizitart.no2.NitriteId;
import org.dizitart.no2.objects.ObjectRepository;

import javax.validation.constraints.NotNull;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * A class to specify filtering criteria during {@link Database}'s
 * find, count and remove operations.
 * <p>
 * Each filtering criteria is based on a field in the object.
 * Please note that is the users responsibility to make sure that filter methods
 * with Object value parameters are called with the same value type as the field type.
 * Otherwise, the filtering might fail.
 */
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

    /**
     *
     * @return new Instance of the Filter class.
     */
    public static Filter build(){
        return new Filter();
    }

    public Deque<FilterElement> filterChain = new ArrayDeque<>();

    public Filter() {}

    /**
     * Concatenate multiple filters using an and operation, for example: {@code Filter.build().and().eq(...).gt(...).in(...)end()}.
     * To avoid confusion, the end should be marked with {@link #end()}.
     *
     * @return Filter object.
     */
    public Filter and() {
        this.filterChain.addLast(new FilterElement(FilterType.AND));
        return this;
    }

    /**
     * Concatenate multiple filters using an or operation, for example: {@code Filter.build().or().eq(...).gt(...).in(...)end()}.
     * To avoid confusion, the end should be marked with {@link #end()}.
     *
     * @return Filter object.
     */
    public Filter or() {
        this.filterChain.addLast(new FilterElement(FilterType.OR));
        return this;
    }

    /**
     * Mark the end of multiple filters concatenated by {@link #and()} or {@link #or()}.
     *
     * @return Filter object.
     */
    public Filter end() {
        this.filterChain.addLast(new FilterElement(FilterType.END));
        return this;
    }

    /**
     * Negate the following filter.
     *
     * @return Filter object.
     */
    public Filter not() {
        this.filterChain.addLast(new FilterElement(FilterType.NOT));
        return this;
    }

    /**
     * Equality filter. Please make sure that value is of the same type as the field type.
     *
     * @return Filter object.
     */
    public Filter eq(String field, Object value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.EQ, field, value));
        return this;
    }

    /**
     * Greater than filter. Please make sure that value is of the same type as the field type.
     *
     * @return Filter object.
     */
    public Filter gt(String field, Object value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.GT, field, value));
        return this;
    }

    /**
     * Greater than equals filter. Please make sure that value is of the same type as the field type.
     *
     * @return Filter object.
     */
    public Filter gte(String field, Object value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.GTE, field, value));
        return this;
    }

    /**
     * Less then filter. Please make sure that value is of the same type as the field type.
     *
     * @return Filter object.
     */
    public Filter lt(String field, Object value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.LT, field, value));
        return this;
    }

    /**
     * Less than equals filter. Please make sure that value is of the same type as the field type.
     *
     * @return Filter object.
     */
    public Filter lte(String field, Object value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.LTE, field, value));
        return this;
    }

    /**
     * Full text search filter.
     *
     * @return Filter object.
     */
    public Filter text(String field, String value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.TEXT, field, value));
        return this;
    }

    /**
     * Regex search filter.
     *
     * @return Filter object.
     */
    public Filter regex(String field, String value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.REGEX, field, value));
        return this;
    }

    /**
     * In filter, field value may be one of the given values.
     *
     * @return Filter object.
     */
    public Filter inByte(String field, byte... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, objects));
        return this;
    }

    /**
     * In filter, field value may be one of the given values.
     *
     * @return Filter object.
     */
    public Filter inShort(String field, short... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, objects));
        return this;
    }

    /**
     * In filter, field value may be one of the given values.
     *
     * @return Filter object.
     */
    public Filter inInt(String field, int... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, Arrays.stream(values).boxed().toArray()));
        return this;
    }

    /**
     * In filter, field value may be one of the given values.
     *
     * @return Filter object.
     */
    public Filter inLong(String field, long... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, Arrays.stream(values).boxed().toArray()));
        return this;
    }

    /**
     * In filter, field value may be one of the given values.
     *
     * @return Filter object.
     */
    public Filter inFloat(String field, float... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, objects));
        return this;
    }

    /**
     * In filter, field value may be one of the given values.
     *
     * @return Filter object.
     */
    public Filter inDouble(String field, double... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, Arrays.stream(values).boxed().toArray()));
        return this;
    }

    /**
     * In filter, field value may be one of the given values.
     *
     * @return Filter object.
     */
    public Filter inBool(String field, boolean... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, objects));
        return this;
    }

    /**
     * In filter, field value may be one of the given values.
     *
     * @return Filter object.
     */
    public Filter inChar(String field, char... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, objects));
        return this;
    }

    /**
     * In filter, field may be one of the values.
     *
     * @return Filter object.
     */
    public Filter in(String field, Object... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, values));
        return this;
    }

    /**
     * Negated in filter, field value must not be one of the values.
     *
     * @return Filter object.
     */
    public Filter notInByte(String field, byte... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, objects));
        return this;
    }

    /**
     * Negated in filter, field value must not be one of the values.
     *
     * @return Filter object.
     */
    public Filter notInShort(String field, short... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, objects));
        return this;
    }

    /**
     * Negated in filter, field value must not be one of the values.
     *
     * @return Filter object.
     */
    public Filter notInInt(String field, int... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, Arrays.stream(values).boxed().toArray()));
        return this;
    }

    /**
     * Negated in filter, field value must not be one of the values.
     *
     * @return Filter object.
     */
    public Filter notInLong(String field, long... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, Arrays.stream(values).boxed().toArray()));
        return this;
    }

    /**
     * Negated in filter, field value must not be one of the values.
     *
     * @return Filter object.
     */
    public Filter notInFloat(String field, float... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, objects));
        return this;
    }

    /**
     * Negated in filter, field value must not be one of the values.
     *
     * @return Filter object.
     */
    public Filter notInDouble(String field, double... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, Arrays.stream(values).boxed().toArray()));
        return this;
    }

    /**
     * Negated in filter, field value must not be one of the values.
     *
     * @return Filter object.
     */
    public Filter notInBool(String field, boolean... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, objects));
        return this;
    }

    /**
     * Negated in filter, field value must not be one of the values.
     *
     * @return Filter object.
     */
    public Filter notInChar(String field, char... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, objects));
        return this;
    }

    /**
     * Negated in filter, field value must not be one of the values.
     *
     * @return Filter object.
     */
    public Filter notIn(String field, Object... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, values));
        return this;
    }

    /**
     * Array and list matching filter. The following filter will be applied to all entries in the field.
     * The array/list elements must be referenced using "$".
     * <p>
     * Example usage: {@code Filter.build().elemMatch("arrField").eq("$", value) }
     *
     * @return Filter object
     */
    public Filter elemMatch(String field) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.ELEM_MATCH, field));
        return this;
    }

}
