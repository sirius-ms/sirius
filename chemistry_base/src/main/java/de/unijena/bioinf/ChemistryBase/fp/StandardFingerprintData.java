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

package de.unijena.bioinf.ChemistryBase.fp;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Function;

public abstract class StandardFingerprintData<FP extends FingerprintVersion> implements FingerprintData<FP> {
    protected final MaskedFingerprintVersion maskedFingerprintVersion;
    protected final FP specificBaseFingerprintVersion;

    protected StandardFingerprintData(@NotNull MaskedFingerprintVersion maskedFingerprintVersion) {
        this.maskedFingerprintVersion = maskedFingerprintVersion;
        this.specificBaseFingerprintVersion = (FP) maskedFingerprintVersion.getMaskedFingerprintVersion();
    }

    public MaskedFingerprintVersion getFingerprintVersion() {
        return maskedFingerprintVersion;
    }

    @Override
    public FP getBaseFingerprintVersion() {
        return specificBaseFingerprintVersion;
    }

    protected static <FP extends FingerprintVersion, Data extends StandardFingerprintData<?>> Data readMask(@NotNull BufferedReader reader, @NotNull FP basePrint, @NotNull Function<MaskedFingerprintVersion, Data> dataBuilder) throws IOException {
        final MaskedFingerprintVersion.Builder builder = MaskedFingerprintVersion.buildMaskFor(basePrint);
        builder.disableAll();

        FileUtils.readTable(reader, true, (row) -> {
            final int abs = Integer.parseInt(row[1]);
            builder.enable(abs);
        });

        return dataBuilder.apply(builder.toMask());
    }
}
