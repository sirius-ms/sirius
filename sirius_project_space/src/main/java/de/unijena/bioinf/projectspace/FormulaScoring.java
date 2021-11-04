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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * contains any score associated with a molecular formula. This includes:
 * - tree score
 * - isotope scores
 * - zodiac scores
 * - top csi score
 * - confidence score
 *
 * Scores are stored in a HashMap with a class as key. Each score can be logarithmic or probabilistic
 *
 * All subclasses of FormulaScore have the following restrictions:
 * - constructor with single double parameter
 * - final
 */
public class FormulaScoring implements Iterable<FormulaScore>, Annotated<FormulaScore>, DataAnnotation {
    private final Annotations<FormulaScore> scores;

    public FormulaScoring(Set<FormulaScore> scores) {
        this();
        scores.forEach(score -> setAnnotation((Class<FormulaScore>) score.getClass(), score));
    }

    public FormulaScoring() {
        this.scores = new Annotations<>();
    }


    public <T extends FormulaScore> void addAnnotation(Class<T> klass, double value) {
        try {
            addAnnotation(klass, klass.getConstructor(double.class).newInstance(value));
        } catch (NoSuchMethodException|IllegalAccessException| InvocationTargetException|InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Annotations<FormulaScore> annotations() {
        return scores;
    }

    @NotNull
    @Override
    public Iterator<FormulaScore> iterator() {
        return annotations().valueIterator();
    }

    public static Comparator<FormulaScoring> comparingMultiScore(List<Class<? extends FormulaScore>> scoreTypes) {
        return comparingMultiScore(scoreTypes,true);
    }

    public static Comparator<FormulaScoring> comparingMultiScore(List<Class<? extends FormulaScore>> scoreTypes, boolean descending) {
        if (scoreTypes == null || scoreTypes.isEmpty())
            throw new IllegalArgumentException("NO score type given");

        Comparator<FormulaScoring> comp = Comparator.comparing(s -> s == null ? FormulaScore.NA(scoreTypes.get(0)): s.getAnnotationOr(scoreTypes.get(0), FormulaScore::NA));
        for (Class<? extends FormulaScore> type : scoreTypes.subList(1, scoreTypes.size()))
            comp = comp.thenComparing(s -> s == null ? FormulaScore.NA(type): s.getAnnotationOr(type, FormulaScore::NA));

        return descending ? comp.reversed() : comp;
    }


    public static List<SScored<FormulaResult, ? extends FormulaScore>> reRankBy(@Nullable Collection<? extends SScored<FormulaResult, ? extends FormulaScore>> data, @NotNull Class<? extends FormulaScore> scoreType, boolean descending) {
        return FormulaScoring.reRankBy(data, Collections.singletonList(scoreType), descending);
    }

    public static List<SScored<FormulaResult, ? extends FormulaScore>> reRankBy(@Nullable Collection<? extends SScored<FormulaResult, ? extends FormulaScore>> data, @NotNull List<Class<? extends FormulaScore>> scoreTypes, boolean descending) {
        if (data == null)
            return null;

        return rankBy(data.stream().map(SScored::getCandidate), scoreTypes, descending);
    }

    public static List<SScored<FormulaResult, ? extends FormulaScore>> rankBy(@Nullable Collection<FormulaResult> data, @NotNull Class<? extends FormulaScore> scoreType, boolean descending) {
        return FormulaScoring.rankBy(data, Collections.singletonList(scoreType), descending);
    }

    public static List<SScored<FormulaResult, ? extends FormulaScore>> rankBy(@Nullable Collection<FormulaResult> data, @NotNull List<Class<? extends FormulaScore>> scoreTypes, boolean descending) {
        if (data == null)
            return null;

        return rankBy(data.stream(), scoreTypes, descending);
    }

    public static List<SScored<FormulaResult, ? extends FormulaScore>> rankBy(@NotNull Stream<FormulaResult> dataStream, @NotNull List<Class<? extends FormulaScore>> scoreTypes, boolean descending) {
        return rankBy(dataStream, scoreTypes, descending, formulaResult -> formulaResult.getAnnotationOrNull(FormulaScoring.class));
    }

    public static <E> List<SScored<E, ? extends FormulaScore>> rankBy(@NotNull Stream<E> dataStream, @NotNull List<Class<? extends FormulaScore>> scoreTypes, boolean descending, Function<E, FormulaScoring> conv) {
        if (scoreTypes.isEmpty())
            return dataStream.map(c -> new SScored<>(c, FormulaScore.NA(SiriusScore.class))).collect(Collectors.toList());

        return dataStream
                .sorted((c1, c2) -> comparingMultiScore(scoreTypes, descending).compare(conv.apply(c1), conv.apply(c2)))
                .map(c -> conv.apply(c) != null
                        ? new SScored<>(c, conv.apply(c).getAnnotationOr(scoreTypes.get(0), FormulaScore::NA))
                        : new SScored<>(c, FormulaScore.NA(scoreTypes.get(0)))).collect(Collectors.toList());
    }
}
