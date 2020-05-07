package de.unijena.bioinf.chemdb;

import java.util.function.Function;
import java.util.function.Predicate;

public class ChemDBs {
    public static boolean inFilter(long entityBits, long filterBits) {
        return filterBits == 0 || (entityBits & filterBits) != 0;
    }

    public static <T> Predicate<T> inFilter(Function<T, Long> bitProvider, long filterBits) {
        return t -> inFilter(bitProvider.apply(t),filterBits);
    }
}