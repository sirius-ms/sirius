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

package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * A generic wrapper spectrum that allow to represent a list of arbitrary objects as Spectrum
 * @param <P> Some object from which the basic peak information (m/z and intensity) can be extracted
 */

public class WrapperSpectrum<P> implements OrderedSpectrum<WrapperSpectrum<P>.WrapperPeak> {
    private final Function<P, Double> massMapper;
    private final Function<P, Double> intensityMapper;
    private final List<P> wrappedPeaks;

    public static <P> WrapperSpectrum<P> of(@NotNull List<P> orderedPeaks, @NotNull Function<P, Double> massMapper, @NotNull Function<P, Double> intensityMapper) {
        return new WrapperSpectrum<>(orderedPeaks, massMapper, intensityMapper);
    }

    public WrapperSpectrum(@NotNull List<P> orderedPeaks, @NotNull Function<P, Double> massMapper, @NotNull Function<P, Double> intensityMapper) {
        this.massMapper = massMapper;
        this.intensityMapper = intensityMapper;
        this.wrappedPeaks = orderedPeaks;
    }

    @Override
    public double getMzAt(int i) {
        return massMapper.apply(wrappedPeaks.get(i));
    }

    @Override
    public double getIntensityAt(int i) {
        return intensityMapper.apply(wrappedPeaks.get(i));
    }

    @Override
    public WrapperPeak getPeakAt(int i) {
        if (wrappedPeaks.get(i) == null)
            return null;
        return new WrapperPeak(wrappedPeaks.get(i));
    }

    @Override
    public int size() {
        return wrappedPeaks.size();
    }

    @NotNull
    @Override
    public Iterator<WrapperPeak> iterator() {
        return wrappedPeaks.stream().map(WrapperPeak::new).iterator();
    }

    public List<P> getWrappedPeaks() {
        return wrappedPeaks;
    }

    public class WrapperPeak implements Peak {
        private  final P peak;

        private WrapperPeak(P peak) {
            this.peak = peak;
        }

        @Override
        public double getMass() {
            return massMapper.apply(peak);
        }

        @Override
        public double getIntensity() {
            return intensityMapper.apply(peak);
        }
    }
}
