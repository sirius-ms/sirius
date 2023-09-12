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

package de.unijena.bioinf.ms.persistence.model.core;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
@Getter
@Setter
@NoArgsConstructor
public class Run {
    //todo are there other types like validation or calibration???
    enum Type {
        SAMPLE,
        BLANK
    }

    /**
     * Informative, human-readable name of this run. Defaults to file name.
     */
    private String name;

    /**
     * indicated that this run is a Blank run
     */
    protected Type runType;

    private ChromatographyType chromatography;
    private IonizationType ionization;
    private FragmentationType fragmentation;
    private MassAnalyzerType massAnalyzer;

    /**
     * Source file. If source file is embedded this object contains the gzipped datastream.
     * If not it contains the path to the source file.
     */
    @NotNull
    SourceFile sourceFile;
}
