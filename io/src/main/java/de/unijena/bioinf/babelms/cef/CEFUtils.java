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

package de.unijena.bioinf.babelms.cef;

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
public class CEFUtils {
    public static CollisionEnergy parseCE(Spectrum spec) {
        try {
            return CollisionEnergy.fromString(spec.msDetails.getCe().replace("V", "ev"));
        } catch (Exception e) {
            log.warn("Could not parse collision energy! Cause: " + e.getMessage());
            log.debug("Could not parse collision energy!", e);
            return CollisionEnergy.none();
        }
    }

    public static Optional<RetentionTime> parseRT(@NotNull Compound c) {
        return parseRT(c, null);
    }

    public static Optional<RetentionTime> parseRT(@NotNull Spectrum ms1) {
        return parseRT(null, ms1);
    }

    public static Optional<RetentionTime> parseRT(@Nullable Compound c, @Nullable Spectrum ms1) {
        if (c == null && ms1 == null)
            return Optional.empty();
        try {
            double middle = Optional.ofNullable(c).map(Compound::getLocation)
                    .map(Location::getRt).map(it -> it.doubleValue() * 60).orElse(Double.NaN);
            double min = Optional.ofNullable(ms1).map(Spectrum::getRTRanges)
                    .map(RTRanges::getRTRange).map(RTRange::getMin).map(it -> it.doubleValue() * 60).orElse(Double.NaN);
            double max = Optional.ofNullable(ms1).map(Spectrum::getRTRanges)
                    .map(RTRanges::getRTRange).map(RTRange::getMax).map(it -> it.doubleValue() * 60).orElse(Double.NaN);

            RetentionTime rt = null;
            if (Double.isNaN(middle)) {
                if (min < max)
                    rt = new RetentionTime(min, max);
                else if (!Double.isNaN(min))
                    rt = new RetentionTime(min);
                else if (!Double.isNaN(max))
                    rt = new RetentionTime(max);
            } else if (min < middle && middle < max) {
                rt = new RetentionTime(min, max, middle);
            } else if (Double.isNaN(min) || Double.isNaN(max) || min >= max) {
                rt = new RetentionTime(middle);
            }else {
                rt = new RetentionTime(min, max);
            }

            return Optional.ofNullable(rt);
        } catch (Exception e) {
            log.warn("Could not parse Retention time!", e);
            return Optional.empty();
        }
    }

    public static MutableMs2Spectrum makeMs2Spectrum(Spectrum spec) {
        return new MutableMs2Spectrum(
                makeMs1Spectrum(spec),
                spec.getMzOfInterest().getMz().doubleValue(),
                parseCE(spec),
                2
        );
    }

    public static SimpleSpectrum makeMs1Spectrum(Spectrum spec) {
        return new SimpleSpectrum(
                spec.msPeaks.p.stream().map(P::getX).mapToDouble(BigDecimal::doubleValue).toArray(),
                spec.msPeaks.p.stream().map(P::getY).mapToDouble(BigDecimal::doubleValue).toArray()
        );
    }
}
