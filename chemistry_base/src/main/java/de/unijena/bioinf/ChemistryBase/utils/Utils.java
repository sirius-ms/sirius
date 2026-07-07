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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.utils;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class Utils {
    public static final Comparator<String> ALPHANUMERIC_COMPARATOR_NULL_LAST = Comparator.nullsLast(new AlphanumComparator());
    public static final Comparator<Double> DOUBLE_DESC_NULL_LAST = Comparator.nullsLast(Comparator.reverseOrder());
    public static final Comparator<Double> DOUBLE_ASC_NULL_LAST = Comparator.nullsLast(Comparator.naturalOrder());


    public static String toScreamingSnakeCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return input.trim()
                .replaceAll("\\s+", "_") // Replace whitespace with underscore
                .toUpperCase(); // Convert to uppercase
    }

    public static String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String[] words = input.trim().split("\\s+");
        StringBuilder camelCaseString = new StringBuilder(words[0].toLowerCase());

        for (int i = 1; i < words.length; i++) {
            camelCaseString.append(words[i].substring(0, 1).toUpperCase())
                    .append(words[i].substring(1).toLowerCase());
        }

        return camelCaseString.toString();
    }

    public static ZonedDateTime epochLongToZonedDateTime(long epochMillis) {
        return epochLongToZonedDateTime(epochMillis, ZoneId.systemDefault());
    }
    public static ZonedDateTime epochLongToZonedDateTime(long epochMillis, ZoneId zone) {
        // Convert milliseconds to Instant
        Instant instant = Instant.ofEpochMilli(epochMillis);
        // Convert Instant to ZonedDateTime
        return instant.atZone(zone);
    }

    public static int[] shortsToInts(short[] shorts){
        int[] ints = new int[shorts.length];
        for (int i = 0; i < shorts.length; i++) {
            ints[i] = shorts[i];
        }
        return ints;
    }

    public static void withTime(String text, Consumer<StopWatch> exec) {
        withTimeR(text, (w) -> {
            exec.accept(w);
            return null;
        });
    }

    public static <R> R withTimeR(String text, Function<StopWatch, R> exec) {
        StopWatch w = new StopWatch();
        w.start();
        R r = exec.apply(w);
        w.stop();
        System.out.println(text + " " + w);
        return r;
    }

    public static <R> R withTimeRIo(String text, IOFunctions.IOFunction<StopWatch, R> exec) throws IOException {
        StopWatch w = new StopWatch();
        w.start();
        R r = exec.apply(w);
        w.stop();
        System.out.println(text + " " + w);
        return r;
    }

    public static double parseDoubleWithUnknownDezSep(String input) {
        input = input.replace(',', '.');
        int decimalSeperator = input.lastIndexOf('.');

        if (decimalSeperator > -1)
            input = input.substring(0, decimalSeperator).replace(".", "") + input.substring(decimalSeperator);


        return Double.parseDouble(input);
    }

    public static <T> ArrayList<Pair<T, T>> pairsHalfNoDiag(Iterable<T> iterable) {
        return pairsHalfNoDiag(iterable, null);
    }

    /**
     * Compute upper half of an all against all matrix (without diagonal) in (n^2-n)/2 time without needing index access.
     *
     * @param values Iterable of elements to create pairs from.
     * @param size Optional size of the Iterable.
     * @return List of pairs
     */
    public static <T> ArrayList<Pair<T, T>> pairsHalfNoDiag(@NotNull Iterable<T> values, @Nullable Integer size) {
        ArrayList<Pair<T, T>> pairs = size == null ? new ArrayList<>() : new ArrayList<>((size * size - size) / 2);

        ArrayList<T> input;
        if (values instanceof ArrayList) {
            input = (ArrayList<T>) values;
        } else {
            input = new ArrayList<>();
            values.forEach(input::add);
        }

        if (input.size() < 2) {
            return pairs;
        }

        for (int left = 0; left < input.size() - 1; left++) {
            for (int right = left + 1; right < input.size(); right++) {
                pairs.add(Pair.of(input.get(left), input.get(right)));
            }
        }

        return pairs;
    }

    public static boolean isNullOrEmpty(@Nullable final Collection<?> c) {
        return c == null || c.isEmpty();
    }

    public static boolean isNullOrEmpty(@Nullable final Map<?, ?> m) {
        return m == null || m.isEmpty();
    }

    public static boolean isNullOrEmpty(@Nullable final CharSequence s) {
        return s == null || s.isEmpty();
    }

    public static boolean isNullOrEmpty(@Nullable final String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isNullOrBlank(@Nullable final String s) {
        return s == null || s.isBlank();
    }

    public static <T> boolean isNullOrEmpty(@Nullable final T[] s) {
        return s == null || s.length == 0;
    }

    public static boolean notNullOrEmpty(@Nullable final Collection<?> c) {
        return !isNullOrEmpty(c);
    }

    public static boolean notNullOrEmpty(@Nullable final Map<?, ?> m) {
        return !isNullOrEmpty(m);
    }

    public static boolean notNullOrEmpty(@Nullable final CharSequence s) {
        return !isNullOrEmpty(s);
    }

    public static boolean notNullOrEmpty(@Nullable final String s) {
        return !isNullOrEmpty(s);
    }

    @NotNull
    public static String notNullOrEmpty(@Nullable final String s, @NotNull String fallback) {
        return notNullOrEmpty(s) ? s : fallback;
    }

    public static boolean notNullOrBlank(@Nullable final String s) {
        return !isNullOrBlank(s);
    }

    @NotNull
    public static String notNullOrBlank(@Nullable final String s, @NotNull String fallback) {
        return notNullOrBlank(s) ? s : fallback;
    }

    public static <T> boolean notNullOrEmpty(@Nullable final T[] s) {
        return !isNullOrEmpty(s);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> Set<T> getIfIdenticalOrNull(Collection<T>... sets) {
        // Filter out null sets
        List<? extends Set<T>> nonNullSets = Arrays.stream(sets).filter(Objects::nonNull).map(HashSet::new).toList();

        // If there are 0 or 1 sets after filtering, they are trivially identical
        if (nonNullSets.size() == 1)
            return nonNullSets.getFirst();
        if (nonNullSets.isEmpty())
            return Set.of();

        // Compare the first set with all others
        Set<T> firstSet = nonNullSets.getFirst();
        if (nonNullSets.stream().skip(1).allMatch(firstSet::equals))
            return firstSet;
        return null;
    }

    public static <T> Optional<Set<T>> getIfIdentical(Collection<T>... sets) {
        return Optional.ofNullable(getIfIdenticalOrNull(sets));
    }


    public static final int LARGE_BATCH_SIZE = 50_000;


    public static  <T> void forEachBatch(Stream<T> streamToPage, Consumer<List<T>> batchProcessor){
        forEachBatch(LARGE_BATCH_SIZE, streamToPage, batchProcessor);

    }
    public static  <T> void forEachBatch(Iterable<T> steamToBatch, Consumer<List<T>> batchProcessor){
        forEachBatch(LARGE_BATCH_SIZE, steamToBatch, batchProcessor);
    }

    public static  <T> void forEachBatch(Iterator<T> iteratorToBatch, Consumer<List<T>> batchProcessor){
        forEachBatch(LARGE_BATCH_SIZE, iteratorToBatch, batchProcessor);
    }

    public static  <T> void forEachBatch(int batchSize, Iterable<T> iterableToBatch, Consumer<List<T>> pageProcessor){
        forEachBatch(batchSize, iterableToBatch.iterator(), pageProcessor);
    }

    public static  <T> void forEachBatch(int batchSize, Stream<T> streamToPage, Consumer<List<T>> batchProcessor){
        forEachBatch(batchSize, streamToPage.iterator(), batchProcessor);
    }

    public static  <T> void forEachBatch(int batchSize, Iterator<T> iteratorToBatch, Consumer<List<T>> batchProcessor){
        List<T> batch = new ArrayList<>(batchSize);

        while (iteratorToBatch.hasNext()) {
            batch.add(iteratorToBatch.next());
            if (batch.size() >= batchSize) {
                batchProcessor.accept(batch);
                batch = new ArrayList<>(batchSize);
            }
        }
        if (!batch.isEmpty())
            batchProcessor.accept(batch);
    }
}
