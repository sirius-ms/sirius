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

package de.unijena.bioinf.lcms.io;

import de.unijena.bioinf.ms.persistence.model.core.*;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface LCMSParser {

    void parse(
            File file,
            IOThrowingConsumer<Run> runConsumer,
            IOThrowingConsumer<Scan> scanConsumer,
            IOThrowingConsumer<MSMSScan> msmsScanConsumer,
            Run.RunBuilder defaultRun
    ) throws IOException;

    default void parse(
            File file,
            SourceFile.Format format,
            IOThrowingConsumer<SourceFile> sourceFileConsumer,
            IOThrowingConsumer<Run> runConsumer,
            IOThrowingConsumer<Scan> scanConsumer,
            IOThrowingConsumer<MSMSScan> msmsScanConsumer,
            Run.RunBuilder defaultRun
    ) throws IOException {
        SourceFile sourceFile = SourceFile.builder()
                .fileName(file.getName())
                .format(format)
                .data(Files.readAllBytes(file.toPath()))
                .build();
        sourceFileConsumer.consume(sourceFile);
        parse(file, run -> {
            run.setRunId(sourceFile.getRunId());
            runConsumer.consume(run);
        }, scanConsumer, msmsScanConsumer, defaultRun);
    }

    default Optional<IonizationType> matchIonizationType(String value) {
        return matchEnumType(value, IonizationType.class, IonizationType::fullName);
    }

    default Optional<MassAnalyzerType> matchMassAnalyzerType(String value) {
        return matchEnumType(value, MassAnalyzerType.class, MassAnalyzerType::fullName);
    }

    default Optional<FragmentationType> matchFragmentationType(String value) {
        return matchEnumType(value, FragmentationType.class, FragmentationType::fullName);
    }

    private <T extends Enum<T>> Optional<T> matchEnumType(String value, Class<T> enumClass, Function<T, String> fullName) {
        if (value != null && !value.isBlank()) {
            try {
                // try to get enum by its value
                return Optional.of(T.valueOf(enumClass, value.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // match enum types by matching the full name
                List<T> types =  Arrays.stream(enumClass.getEnumConstants()).sorted((a, b) -> Integer.compare(fullName.apply(b).length(), fullName.apply(a).length())).toList();
                for (T type : types) {
                    if (fullName.apply(type).equalsIgnoreCase(value)) {
                        return Optional.of(type);
                    }
                }
                // get enum with most matching words in the full name
                String lowerVal = value.toLowerCase();
                Optional<Pair<Long, T>> match = types.stream().map(type -> {
                    long count = Arrays.stream(fullName.apply(type).split(" ")).map(String::toLowerCase).filter(lowerVal::contains).count();
                    return Pair.of(count, type);
                }).min((a, b) -> Long.compare(b.getLeft(), a.getLeft()));
                if (match.isPresent() && match.get().getLeft() > 0) {
                    return Optional.of(match.get().getRight());
                }
            }
        }
        return Optional.empty();
    }

}