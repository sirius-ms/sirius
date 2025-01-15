package de.unijena.bioinf.storage.db.nosql.nitrite.filters;

import lombok.Data;
import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteId;
import org.dizitart.no2.common.tuples.Pair;
import org.dizitart.no2.common.util.Comparables;
import org.dizitart.no2.exceptions.FilterException;
import org.dizitart.no2.filters.Filter;
import org.dizitart.no2.filters.FluentFilter;
import org.dizitart.no2.filters.NitriteFilter;
import org.dizitart.no2.filters.SortingAwareFilter;
import org.dizitart.no2.index.IndexMap;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

import static org.dizitart.no2.common.util.Numbers.compare;

public class RangeFilter<T extends Comparable<T>> extends SortingAwareFilter  {

    private final Bound<T> bound;

    public RangeFilter(String field, Bound<T> bound) {
        super(field, bound);
        this.bound = bound;
    }
    public RangeFilter(String field, T from, T to, boolean fromInclusive, boolean toInclusive) {
        this(field, new Bound<>(from, to, fromInclusive, toInclusive));
    }

    @Override
    public List<?> applyOnIndex(IndexMap indexMap) {
        List<NavigableMap<Comparable<?>, Object>> subMap = new ArrayList<>();
        List<NitriteId> nitriteIds = new ArrayList<>();

        if (isReverseScan()) {
            Comparable floorKey = bound.upperInclusive ? indexMap.floorKey(bound.upperBound) : indexMap.lowerKey(bound.upperBound);
            while (floorKey != null) {
                if (Comparables.compare(floorKey, bound.lowerBound) < 0) {
                    break;
                }
                // get the starting value, it can be a navigable-map (compound index)
                // or list (single field index)
                Object value = indexMap.get(floorKey);
                processIndexValue(value, subMap, nitriteIds);
                floorKey = indexMap.lowerKey(floorKey);
            }
        } else {
            Comparable ceilingKey = bound.lowerInclusive ? indexMap.ceilingKey(bound.lowerBound) : indexMap.higherKey(bound.lowerBound);
            while (ceilingKey != null) {
                if (Comparables.compare(ceilingKey, bound.upperBound) > 0) {
                    break;
                }
                // get the starting value, it can be a navigable-map (compound index)
                // or list (single field index)
                Object value = indexMap.get(ceilingKey);
                processIndexValue(value, subMap, nitriteIds);
                ceilingKey = indexMap.higherKey(ceilingKey);
            }
        }

        if (!subMap.isEmpty()) {
            // if sub-map is populated then filtering on compound index, return sub-map
            return subMap;
        } else {
            // else it is filtering on either single field index,
            // or it is a terminal filter on compound index, return only nitrite-ids
            return nitriteIds;
        }
    }

    @Override
    public boolean apply(Pair<NitriteId, Document> element) {
        Document document = element.getSecond();
        Object fieldValue = document.get(getField());
        if (fieldValue != null) {
            if (fieldValue instanceof Number) {
                int a = compare((Number) fieldValue, (Number)bound.lowerBound);
                if (!(bound.lowerInclusive ? a >= 0 : a>0)) {
                    return false;
                }
                int b = compare((Number) fieldValue, (Number) bound.upperBound);
                if (!(bound.upperInclusive ? b <= 0 : b<0)) {
                    return false;
                }
                return true;
            } else if (fieldValue instanceof Comparable) {
                Comparable c = (Comparable)fieldValue;
                int a = c.compareTo(bound.lowerBound);
                if (!(bound.lowerInclusive ? a >= 0 : a>0)) {
                    return false;
                }
                int b = c.compareTo(bound.upperBound);
                if (!(bound.upperInclusive ? b <= 0 : b<0)) {
                    return false;
                }
                return true;
            } else {
                throw new FilterException(fieldValue + " is not comparable");
            }
        }

        return false;
    }

    @Override
    public Filter not() {
        FluentFilter where = FluentFilter.where(getField());
        NitriteFilter f;
        if (bound.lowerInclusive) f = where.lt(bound.lowerBound);
        else f = where.lte(bound.lowerBound);
        NitriteFilter g;
        if (bound.upperInclusive) g = where.gt(bound.upperBound);
        else g = where.gte(bound.upperBound);
        return f.or(g);

    }

    @Data
    public static class Bound<T> {
        private T upperBound;
        private T lowerBound;
        private boolean upperInclusive;
        private boolean lowerInclusive;

        public Bound(T lowerBound, T upperBound) {
            this(lowerBound, upperBound, true);
        }

        public Bound(T lowerBound, T upperBound, boolean inclusive) {
            this(lowerBound, upperBound, inclusive, inclusive);
        }

        public Bound(T lowerBound, T upperBound, boolean lowerInclusive, boolean upperInclusive) {
            this.upperBound = upperBound;
            this.lowerBound = lowerBound;
            this.upperInclusive = upperInclusive;
            this.lowerInclusive = lowerInclusive;
        }
    }
}
