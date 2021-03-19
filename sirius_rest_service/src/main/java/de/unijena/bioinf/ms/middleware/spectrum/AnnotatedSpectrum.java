/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.spectrum;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.SimplePeak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;

public class AnnotatedSpectrum implements OrderedSpectrum<Peak> {

    protected double[] masses;
    protected double[] intensities;
    protected HashMap<Integer, PeakAnnotation> peakAnnotations;

    public AnnotatedSpectrum(double[] masses, double[] intensities, HashMap<Integer, PeakAnnotation> peakAnnotations) {
        this.masses = masses;
        this.intensities = intensities;
        this.peakAnnotations = peakAnnotations;
    }

    public double[] getMasses() {
        return masses;
    }

    public double[] getIntensities() {
        return intensities;
    }

    @Override
    public double getMzAt(int index) {
        return masses[index];
    }

    @Override
    public double getIntensityAt(int index) {
        return intensities[index];
    }

    @Override
    public Peak getPeakAt(int index) {
        return new SimplePeak(masses[index], intensities[index]);
    }

    @Override
    public int size() {
        return masses.length;
    }

    @NotNull
    @Override
    public Iterator<Peak> iterator() {
        return new Iterator<Peak>() {
            int index=0;
            @Override
            public boolean hasNext() {
                return index < masses.length;
            }

            @Override
            public Peak next() {
                return getPeakAt(index++);
            }
        };
    }
}
