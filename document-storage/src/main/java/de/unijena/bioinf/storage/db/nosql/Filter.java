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

import lombok.Getter;
import lombok.Setter;

import jakarta.annotation.Nullable;
import java.util.Arrays;

/**
 * A class to specify filtering criteria during {@link Database}'s
 * find, count and remove operations.
 * <p>
 * Each filtering criteria is based on a field in the object.
 * Please note that is the users responsibility to make sure that filter methods
 * with Object value parameters are called with the same value type as the field type.
 * Otherwise, the filtering might fail.
 */
@Setter
@Getter
public class Filter {

    private FilterNode parent;

    public Filter(FilterNode parent) {
        this.parent = parent;
    }

    public interface FilterNode {
        FilterNode getParent();

        void setParent(FilterNode parent);
    }

    @Getter
    public static class FilterLiteral implements FilterNode {

        public enum Type {
            EQ, NOT_EQ, GT, GTE, LT, LTE, BETWEEN, TEXT, REGEX, IN, NOT_IN, ELEM_MATCH
        }

        @Setter
        private FilterNode parent;

        private final String field;

        private Object[] values;

        protected Type type;

        private FilterLiteral(String field, @Nullable FilterNode parent) {
            this.parent = parent;
            this.field = field;
        }

        protected Filter singleValue(Object value, Type type) {
            this.values = new Object[]{value};
            this.type = type;
            return new Filter(this);
        }

        private Filter multiValue(Object[] values, Type type) {
            if (values.length == 0) {
                throw new IllegalArgumentException("not enough values");
            }
            this.values = values;
            this.type = type;
            return new Filter(this);
        }

        public Filter eq(Object value) {
            return singleValue(value, Type.EQ);
        }

        public Filter notEq(Object value) {
            return singleValue(value, Type.NOT_EQ);
        }

        public Filter gt(Comparable<?> value) {
            return singleValue(value, Type.GT);
        }

        public Filter gte(Comparable<?> value) {
            return singleValue(value, Type.GTE);
        }

        public Filter lt(Comparable<?> value) {
            return singleValue(value, Type.LT);
        }

        public Filter lte(Comparable<?> value) {
            return singleValue(value, Type.LTE);
        }

        public <T extends Comparable<T>> Filter beetween(T left, T right) {
            if (left.compareTo(right) >= 0) {
                throw new IllegalArgumentException(left + " must be > " + right);
            }
            return multiValue(new Object[]{left, right, false, false}, Type.BETWEEN);
        }

        public <T extends Comparable<T>> Filter beetweenLeftInclusive(T left, T right) {
            if (left.compareTo(right) >= 0) {
                throw new IllegalArgumentException(left + " must be > " + right);
            }
            return multiValue(new Object[]{left, right, true, false}, Type.BETWEEN);
        }

        public <T extends Comparable<T>> Filter beetweenRightInclusive(T left, T right) {
            if (left.compareTo(right) >= 0) {
                throw new IllegalArgumentException(left + " must be > " + right);
            }
            return multiValue(new Object[]{left, right, false, true}, Type.BETWEEN);
        }

        public <T extends Comparable<T>> Filter beetweenBothInclusive(T left, T right) {
            if (left.compareTo(right) > 0) {
                throw new IllegalArgumentException(left + " must be >= " + right);
            }
            return multiValue(new Object[]{left, right, true, true}, Type.BETWEEN);
        }

        public Filter text(String value) {
            return singleValue(value, Type.TEXT);
        }

        public Filter regex(String value) {
            return singleValue(value, Type.REGEX);
        }

        public Filter in(Comparable<?>... values) {
            return multiValue(values, Type.IN);
        }

        public Filter notIn(Comparable<?>... values) {
            return multiValue(values, Type.NOT_IN);
        }

        public FilterLiteral elemMatch() {
            FilterLiteral literal = new FilterLiteral("$", this);
            this.values = new Object[]{literal};
            this.type = Type.ELEM_MATCH;
            return literal;
        }

        // TODO possible issue: elemMatchAnd() and elemMatchOr() do not enforce child filters to have "$" fields
        public FilterClause elemMatchAnd(Filter... filters) {
            FilterClause clause = clause(filters, FilterClause.Type.AND);
            clause.setParent(this);
            this.values = new Object[]{clause};
            this.type = Type.ELEM_MATCH;
            return clause;
        }

        public FilterClause elemMatchOr(Filter... filters) {
            FilterClause clause = clause(filters, FilterClause.Type.OR);
            clause.setParent(this);
            this.values = new Object[]{clause};
            this.type = Type.ELEM_MATCH;
            return clause;
        }

    }

    @Getter
    public static class FilterClause extends Filter implements FilterNode {

        public enum Type {
            AND, OR
        }

        private final FilterNode[] children;

        private final Type type;

        private FilterClause(FilterNode[] children, @Nullable FilterNode parent, Type type) {
            super(parent);
            this.children = children;
            this.type = type;
        }

    }

    public static FilterLiteral where(String field) {
        return new FilterLiteral(field, null);
    }

    public static FilterLiteral where$() {
        return new FilterLiteral("$", null);
    }

    private static FilterClause clause(Filter[] filters, FilterClause.Type type) {
        if (filters.length < 2) {
            throw  new IllegalArgumentException("Not enough arguments");
        }
        FilterNode[] f = Arrays.stream(filters).map(filter -> {
            FilterNode root = (filter instanceof FilterClause) ? (FilterNode) filter : filter.getParent();
            while (root.getParent() != null)
                root = root.getParent();
            return root;
        }).toArray(FilterNode[]::new);
        FilterClause clause = new FilterClause(f, null, type);
        for (FilterNode filter : f) {
            filter.setParent(clause);
        }
        return clause;
    }

    public static FilterClause and(Filter... filters) {
        return clause(filters, FilterClause.Type.AND);
    }

    public static FilterClause or(Filter... filters) {
        return clause(filters, FilterClause.Type.OR);
    }

}
