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

package de.unijena.bioinf.ms.rest.model.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Has to be set by CLI when first running CSI via WebAPI call
 */
public class FingerIdData implements FingerprintData<CdkFingerprintVersion> {

    protected MaskedFingerprintVersion fingerprintVersion;
    protected CdkFingerprintVersion cdkFingerprintVersion;
    protected PredictionPerformance[] performances;

    public FingerIdData(MaskedFingerprintVersion fingerprintVersion, PredictionPerformance[] performances) {
        this.fingerprintVersion = fingerprintVersion;
        this.cdkFingerprintVersion = (CdkFingerprintVersion) fingerprintVersion.getMaskedFingerprintVersion();
        this.performances = performances;
    }

    public MaskedFingerprintVersion getFingerprintVersion() {
        return fingerprintVersion;
    }

    public CdkFingerprintVersion getCdkFingerprintVersion() {
        return cdkFingerprintVersion;
    }

    @Override
    public CdkFingerprintVersion getBaseFingerprintVersion() {
        return getCdkFingerprintVersion();
    }

    public PredictionPerformance[] getPerformances() {
        return performances;
    }

    public static FingerIdData readAndClose(Reader reader) {
        try (BufferedReader r = new BufferedReader(reader)) {
            return read(r);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static FingerIdData read(BufferedReader reader) throws IOException {
        final ArrayList<PredictionPerformance> performances = new ArrayList<>();
        final CdkFingerprintVersion V = CdkFingerprintVersion.getDefault();
        final MaskedFingerprintVersion.Builder builder = MaskedFingerprintVersion.buildMaskFor(V);
        builder.disableAll();
        FileUtils.readTable(reader, true, (row) -> {
            final int abs = Integer.parseInt(row[1]);
            builder.enable(abs);
            final PredictionPerformance performance = new PredictionPerformance(
                    Double.parseDouble(row[3]), Double.parseDouble(row[4]), Double.parseDouble(row[5]), Double.parseDouble(row[6])
            );
            performances.add(performance);
        });
        return new FingerIdData(
                builder.toMask(), performances.toArray(PredictionPerformance[]::new)
        );
    }

    public static void write(@NotNull Writer writer, @NotNull final FingerIdData clientData) throws IOException {
        final String[] header = new String[]{"relativeIndex", "absoluteIndex", "description", "TP", "FP", "TN", "FN", "Acc", "MCC", "F1", "Recall", "Precision", "Count"};
        final String[] row = header.clone();
        FileUtils.writeTable(writer, header, Arrays.stream(clientData.fingerprintVersion.allowedIndizes()).mapToObj(absoluteIndex -> {
            final MolecularProperty property = clientData.fingerprintVersion.getMolecularProperty(absoluteIndex);
            final int relativeIndex = clientData.fingerprintVersion.getRelativeIndexOf(absoluteIndex);
            row[0] = String.valueOf(relativeIndex);
            row[1] = String.valueOf(absoluteIndex);
            row[2] = property.getDescription();
            PredictionPerformance P = clientData.performances[relativeIndex];
            row[3] = String.valueOf(P.getTp());
            row[4] = String.valueOf(P.getFp());
            row[5] = String.valueOf(P.getTn());
            row[6] = String.valueOf(P.getFn());
            row[7] = String.valueOf(P.getAccuracy());
            row[8] = String.valueOf(P.getMcc());
            row[9] = String.valueOf(P.getF());
            row[10] = String.valueOf(P.getRecall());
            row[11] = String.valueOf(P.getPrecision());
            row[12] = String.valueOf(P.getCount());
            return row;
        })::iterator);
    }
}