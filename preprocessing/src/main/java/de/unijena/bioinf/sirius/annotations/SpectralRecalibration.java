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

package de.unijena.bioinf.sirius.annotations;

import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.RecalibrationFunction;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.sirius.MS2Peak;
import de.unijena.bioinf.sirius.ProcessedPeak;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Recalibration function for MS/MS spectra. Each MS/MS spectrum might have its own separate recalibration function.
 * The merged spectrum has a recalibration function as well which can be used instead.
 */
public class SpectralRecalibration implements DataAnnotation {

    private final static SpectralRecalibration NONE = new SpectralRecalibration(null,null);

    public static SpectralRecalibration none() {
        return NONE;
    }
    @Nullable protected final RecalibrationFunction[] recalibrationFunctions;
    @NotNull  protected final RecalibrationFunction mergedFunc;

    public SpectralRecalibration(RecalibrationFunction[] recalibrationFunctions, RecalibrationFunction mergedFunc) {
        this.recalibrationFunctions = simplify(recalibrationFunctions, mergedFunc);
        this.mergedFunc = mergedFunc==null ? RecalibrationFunction.identity() : mergedFunc;
    }

    private static RecalibrationFunction[] simplify(RecalibrationFunction[] recalibrationFunctions, RecalibrationFunction merged) {
        if (recalibrationFunctions==null) return null;
        recalibrationFunctions = recalibrationFunctions.clone();
        int nonNull = 0;
        for (int i=0; i < recalibrationFunctions.length; ++i) {
            final RecalibrationFunction f = recalibrationFunctions[i];
            if (f!=null) {
                if (f.equals(merged)) {
                    recalibrationFunctions[i] = null;
                } else {
                    ++nonNull;
                }
            }
        }
        if (nonNull==0) return null;
        else return recalibrationFunctions;
    }

    public RecalibrationFunction getMergedRecalibrationFunction() {
        return mergedFunc;
    }

    @Nullable public RecalibrationFunction[] getSingleSpectrumRecalibrationFunctions() {
        return recalibrationFunctions==null ? null : recalibrationFunctions.clone();
    }

    public RecalibrationFunction getRecalibrationFunctionFor(MutableMs2Spectrum spec) {
        if (recalibrationFunctions==null) return mergedFunc;
        final RecalibrationFunction f = recalibrationFunctions[spec.getScanNumber()];
        if (f==null) return mergedFunc;
        else return f;
    }

    public double recalibrate(ProcessedPeak peak) {
        if (this==NONE) return peak.getMass();
        // 1. check if most intensive original peak can be recalibrated
        MS2Peak mostIntensive = null;
        for (MS2Peak m : peak.getOriginalPeaks()) {
            if (mostIntensive==null || m.getIntensity() > mostIntensive.getIntensity())
                mostIntensive = m;
        }
        if (mostIntensive!=null) {
            final int sc = ((MutableMs2Spectrum)mostIntensive.getSpectrum()).getScanNumber();
            if (recalibrationFunctions!=null && recalibrationFunctions[sc]!=null) {
                return recalibrationFunctions[sc].apply(peak.getMass());
            }
        }
        // 2. use merged recalibration function
        return mergedFunc.apply(peak.getMass());
    }

}
