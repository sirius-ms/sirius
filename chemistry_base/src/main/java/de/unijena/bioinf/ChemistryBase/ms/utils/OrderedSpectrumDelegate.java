/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * A wrapper OrderedSpectrum that delegates all calls to the nested Spectrum
 */
public class OrderedSpectrumDelegate<T extends Peak> implements OrderedSpectrum<T> {

    private final Spectrum<T> delegate;

    public OrderedSpectrumDelegate(Spectrum<T> delegate) {
        if (delegate instanceof OrderedSpectrum<T> || Spectrums.isMassOrderedSpectrum(delegate)) {
            this.delegate = delegate;
        } else {
            throw new IllegalArgumentException("Expected an ordered spectrum, got " + delegate);
        }
    }

    @Override
    public double getMzAt(int index) {
        return delegate.getMzAt(index);
    }

    @Override
    public double getIntensityAt(int index) {
        return delegate.getIntensityAt(index);
    }

    @Override
    public T getPeakAt(int index) {
        return delegate.getPeakAt(index);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return delegate.iterator();
    }
}
