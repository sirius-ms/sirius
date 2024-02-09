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

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.SpectrumAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * A Wrapper spectrum that allows to add annotations functionality to an existing spectrum object
 */
public final class AnnotatedWrapperSpectrum<S extends Spectrum<P>, P extends Peak> implements AnnotatedSpectrum<P> {

    private final S sourceSpectrum;

    private final Annotations<SpectrumAnnotation> annotations;

    @Override
    public Annotations<SpectrumAnnotation> annotations() {
        return annotations;
    }

    public AnnotatedWrapperSpectrum(S spectrumToWrap, Annotations<SpectrumAnnotation> annotations) {
        sourceSpectrum = spectrumToWrap;
        this.annotations = annotations != null ? annotations : new Annotations<>();
    }


    @Override
    public double getMzAt(int index) {
        return sourceSpectrum.getMzAt(index);
    }

    @Override
    public double getIntensityAt(int index) {
        return sourceSpectrum.getIntensityAt(index);
    }

    @Override
    public P getPeakAt(int index) {
        return sourceSpectrum.getPeakAt(index);
    }

    @Override
    public int size() {
        return sourceSpectrum.size();
    }

    @Override
    public boolean isEmpty() {
        return sourceSpectrum.isEmpty();
    }

    @Override
    public CollisionEnergy getCollisionEnergy() {
        return sourceSpectrum.getCollisionEnergy();
    }

    @Override
    public int getMsLevel() {
        return sourceSpectrum.getMsLevel();
    }

    @Override
    public double getMaxIntensity() {
        return sourceSpectrum.getMaxIntensity();
    }

    @NotNull
    @Override
    public Iterator<P> iterator() {
        return sourceSpectrum.iterator();
    }

    @Override
    public void forEach(Consumer<? super P> action) {
        sourceSpectrum.forEach(action);
    }

    @Override
    public Spliterator<P> spliterator() {
        return sourceSpectrum.spliterator();
    }

    public S getSourceSpectrum() {
        return sourceSpectrum;
    }

    public static <S extends Spectrum<P>, P extends Peak> AnnotatedWrapperSpectrum<S, P> of(S spectrumToWrap) {
        Annotations<SpectrumAnnotation> anno = new Annotations<>();
        if (spectrumToWrap instanceof Annotated) {
            try {//add annotations if available
                anno = ((Annotated<SpectrumAnnotation>) spectrumToWrap).annotations();
            } catch (ClassCastException e) {
                LoggerFactory.getLogger(AnnotatedWrapperSpectrum.class).error("Error when importing annotations", e);
            }
        }
        return of(spectrumToWrap, anno);
    }
    public static <S extends Spectrum<P>, P extends Peak> AnnotatedWrapperSpectrum<S, P> of(
            @NotNull S spectrumToWrap, @Nullable Annotations<SpectrumAnnotation> annotations) {
        return new AnnotatedWrapperSpectrum<>(spectrumToWrap, annotations);
    }
}
