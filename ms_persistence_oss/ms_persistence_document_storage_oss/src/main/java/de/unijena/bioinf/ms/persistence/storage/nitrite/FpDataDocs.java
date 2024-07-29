/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.persistence.storage.nitrite;

import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import org.dizitart.no2.collection.Document;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

public class FpDataDocs {
    private static Map<Class<? extends FingerprintData<?>>, Function<Document, FingerprintData<?>>> MAPPERS = Map.of(
            FingerIdData.class, FpDataDocs::toFingerIdData,
            CanopusCfData.class, FpDataDocs::toCanopusCfData,
            CanopusNpcData.class, FpDataDocs::toCanopusNpcData
    );

    public static <T extends FingerprintData<?>> Function<Document, T> toDataFunction(Class<T> clzz) {
        return (Function<Document, T>) MAPPERS.get(clzz);
    }

    public static <T extends FingerprintData<?>> T toData(Class<T> clzz, Document doc) {
        return toDataFunction(clzz).apply(doc);
    }

    public static Document toDoc(FingerIdData data, int charge) {
        int[] absoluteIndices = data.getFingerprintVersion().allowedIndizes();
        double[] tps = new double[absoluteIndices.length];
        double[] fps = new double[absoluteIndices.length];
        double[] tns = new double[absoluteIndices.length];
        double[] fns = new double[absoluteIndices.length];
        double[] pseudoCounts = new double[absoluteIndices.length];

        for (int i = 0; i < absoluteIndices.length; i++) {
            int abs = absoluteIndices[i];
            int rel = data.getFingerprintVersion().getRelativeIndexOf(abs);
            PredictionPerformance performance = data.getPerformances()[rel];

            tps[i] = performance.getTp();
            fps[i] = performance.getFp();
            tns[i] = performance.getTn();
            fns[i] = performance.getFn();
            pseudoCounts[i] = performance.getPseudoCount();
        }

        return Document.createDocument()
                .put("type", data.getClass().getSimpleName())
                .put("charge", charge)
                .put("absIndices", absoluteIndices)
                .put("tps", tps)
                .put("fps", fps)
                .put("tns", tns)
                .put("fns", fns)
                .put("pseudoCounts", pseudoCounts);
    }

    public static Document toDoc(StandardFingerprintData<?> data, int charge) {
        return Document.createDocument()
                .put("type", data.getClass().getSimpleName())
                .put("charge", charge)
                .put("absIndices", data.getFingerprintVersion().allowedIndizes());
    }

    public static FingerIdData toFingerIdData(Document doc) {
        int[] absoluteIndices = doc.get("absIndices", int[].class);
        double[] tps = doc.get("tps", double[].class);
        double[] fps = doc.get("fps", double[].class);
        double[] tns = doc.get("tns", double[].class);
        double[] fns = doc.get("fns", double[].class);
        double[] pseudoCounts = doc.get("pseudoCounts", double[].class);

        final PredictionPerformance[] performances = new PredictionPerformance[absoluteIndices.length];
        for (int i = 0; i < absoluteIndices.length; i++)
            performances[i] = new PredictionPerformance(tps[i], fps[i], tns[i], fns[i], pseudoCounts[i]);

        return new FingerIdData(readMask(CdkFingerprintVersion.getDefault(), absoluteIndices), performances);
    }

    public static CanopusCfData toCanopusCfData(Document doc) {
        return new CanopusCfData(readMask(ClassyFireFingerprintVersion.getDefault(), doc));
    }

    public static CanopusNpcData toCanopusNpcData(Document doc) {
        return new CanopusNpcData(readMask(NPCFingerprintVersion.get(), doc));
    }


    protected static <FP extends FingerprintVersion> MaskedFingerprintVersion readMask(@NotNull FP basePrint, Document doc) {
        int[] absIndices = doc.get("absIndices", int[].class);
        return readMask(basePrint, absIndices);
    }

    protected static <FP extends FingerprintVersion> MaskedFingerprintVersion readMask(@NotNull FP basePrint, int[] absIndices) {
        final MaskedFingerprintVersion.Builder builder = MaskedFingerprintVersion.buildMaskFor(basePrint);
        builder.disableAll();
        for (int abs : absIndices)
            builder.enable(abs);

        return builder.toMask();
    }
}
