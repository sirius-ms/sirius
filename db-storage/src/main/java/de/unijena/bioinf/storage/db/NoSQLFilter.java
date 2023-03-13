package de.unijena.bioinf.storage.db;

import java.util.ArrayDeque;
import java.util.Deque;

public class NoSQLFilter {

    public enum FilterType {
        AND, OR, NOT, EQ, GT, GTE, LT, LTE, TEXT, REGEX, IN, NOT_IN, ELEM_MATCH
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

        public FieldFilterElement(FilterType filterType, String field, Object... values) {
            super(filterType);
            this.field = field;
            this.values = values;
        }

    }

    public Deque<FilterElement> filterChain = new ArrayDeque<>();

    public NoSQLFilter() {}

    public NoSQLFilter and() {
        this.filterChain.addLast(new FilterElement(FilterType.AND));
        return this;
    }

    public NoSQLFilter or() {
        this.filterChain.addLast(new FilterElement(FilterType.OR));
        return this;
    }

    public NoSQLFilter not() {
        this.filterChain.addLast(new FilterElement(FilterType.NOT));
        return this;
    }

    public NoSQLFilter eq(String field, Object value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.EQ, field, value));
        return this;
    }

    public NoSQLFilter gt(String field, Object value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.GT, field, value));
        return this;
    }

    public NoSQLFilter gte(String field, Object value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.GTE, field, value));
        return this;
    }

    public NoSQLFilter lt(String field, Object value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.LT, field, value));
        return this;
    }

    public NoSQLFilter lte(String field, Object value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.LTE, field, value));
        return this;
    }

    public NoSQLFilter text(String field, String value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.TEXT, field, value));
        return this;
    }

    public NoSQLFilter regex(String field, String value) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.REGEX, field, value));
        return this;
    }

    public NoSQLFilter in(String field, Object... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.IN, field, values));
        return this;
    }

    public NoSQLFilter notIn(String field, Object... values) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.NOT_IN, field, values));
        return this;
    }

    public NoSQLFilter elemMatch(String field) {
        this.filterChain.addLast(new FieldFilterElement(FilterType.ELEM_MATCH, field));
        return this;
    }

}
