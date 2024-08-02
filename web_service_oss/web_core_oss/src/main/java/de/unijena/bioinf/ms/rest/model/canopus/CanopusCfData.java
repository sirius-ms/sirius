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

package de.unijena.bioinf.ms.rest.model.canopus;

import de.unijena.bioinf.ChemistryBase.fp.ClassyFireFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.StandardFingerprintData;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;

public class CanopusCfData extends StandardFingerprintData<ClassyFireFingerprintVersion> {


    public CanopusCfData(@NotNull MaskedFingerprintVersion maskedFingerprintVersion) {
        super(maskedFingerprintVersion);
    }

    public ClassyFireFingerprintVersion getClassyFireFingerprintVersion() {
        return getBaseFingerprintVersion();
    }

    public static CanopusCfData readAndClose(Reader reader) {
        try (BufferedReader r = new BufferedReader(reader)) {
            return read(r);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static CanopusCfData read(BufferedReader reader) throws IOException {
        return readMask(reader, ClassyFireFingerprintVersion.getDefault(), CanopusCfData::new);
    }

    public static void write(@NotNull Writer writer, @NotNull final CanopusCfData canopusData) throws IOException {
        final String[] header = new String[]{"relativeIndex", "absoluteIndex", "id", "name", "parentId", "description"};
        final String[] row = header.clone();

        FileUtils.writeTable(writer, header, Arrays.stream(canopusData.getFingerprintVersion().allowedIndizes()).mapToObj(absoluteIndex -> {
            final ClassyfireProperty property = (ClassyfireProperty) canopusData.getFingerprintVersion().getMolecularProperty(absoluteIndex);
            final int relativeIndex = canopusData.getFingerprintVersion().getRelativeIndexOf(absoluteIndex);
            row[0] = String.valueOf(relativeIndex);
            row[1] = String.valueOf(absoluteIndex);
            row[2] = property.getChemontIdentifier();
            row[3] = property.getName();
            row[4] = property.getParent()!=null ? property.getParent().getChemontIdentifier() : "";
            row[5] = property.getDescription();
            return row;
        })::iterator);
    }
}
