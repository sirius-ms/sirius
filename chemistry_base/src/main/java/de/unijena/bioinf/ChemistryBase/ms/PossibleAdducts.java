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

package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.IonMode;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.annotations.ProcessedInputAnnotation;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Can be attached to a Ms2Experiment or ProcessedInput. If PrecursorIonType is unknown, CSI:FingerID will use this
 * object and for all different adducts.
 */
public final class PossibleAdducts implements Iterable<PrecursorIonType>, ProcessedInputAnnotation {

    private final static PossibleAdducts EMPTY = new PossibleAdducts();
    public static PossibleAdducts empty() {
        return EMPTY;
    }

    protected final LinkedHashSet<PrecursorIonType> value;

    public PossibleAdducts(Collection<? extends PrecursorIonType> c) {
        this.value = new LinkedHashSet<>(c);
    }

    public PossibleAdducts(PrecursorIonType... possibleAdducts) {
        this(Arrays.asList(possibleAdducts));
    }

    public PossibleAdducts(PossibleAdducts pa) {
        this(pa.value);
    }

    public PossibleAdducts() {
        this(new LinkedHashSet<>());
    }

    private PossibleAdducts(LinkedHashSet<PrecursorIonType> adducts) {
        this.value = adducts;
    }

    public Set<PrecursorIonType> getAdducts() {
        return Collections.unmodifiableSet(value);
    }

    public Set<PrecursorIonType> getAdducts(Ionization ionMode) {
        return value.stream().filter((a) -> a.getIonization().equals(ionMode)).collect(Collectors.toSet());
    }

    public boolean hasPositiveCharge() {
        for (PrecursorIonType a : value)
            if (a.getCharge() > 0)
                return true;
        return false;
    }

    public boolean hasNegativeCharge() {
        for (PrecursorIonType a : value)
            if (a.getCharge() < 0)
                return true;
        return false;
    }

    public boolean hasUnknownIontype() {
        return value.stream().anyMatch(PrecursorIonType::isIonizationUnknown);
    }

    public PossibleAdducts keepOnlyPositive() {
        return new PossibleAdducts(value.stream().filter(it -> it.getCharge() > 1).collect(Collectors.toSet()));
    }

    public PossibleAdducts keepOnlyNegative() {
        return new PossibleAdducts(value.stream().filter(it -> it.getCharge() < 1).collect(Collectors.toSet()));
    }

    public PossibleAdducts keepOnly(final int charge) {
        return new PossibleAdducts(value.stream().filter(it -> it.getCharge() == charge).collect(Collectors.toSet()));
    }

    public Set<IonMode> getIonModes() {
        final Set<IonMode> ions = new HashSet<>();
        for (PrecursorIonType a : value) {
            final Ionization ion = a.getIonization();
            if (ion instanceof IonMode) {
                ions.add((IonMode) ion);
            }
        }
        return ions;
    }

    @Override
    public Iterator<PrecursorIonType> iterator() {
        return value.iterator();
    }

    @Override
    public String toString() {
        if (value.isEmpty())
            return ",";
        return value.toString();
    }

    public static PossibleAdducts union(PossibleAdducts p1, Set<PrecursorIonType> p2) {
        return new PossibleAdducts(Stream.concat(p1.value.stream(),p2.stream()).collect(Collectors.toSet()));
    }

    public static PossibleAdducts union(PossibleAdducts p1, PossibleAdducts p2) {
        return new PossibleAdducts(Stream.concat(p1.value.stream(),p2.value.stream()).collect(Collectors.toSet()));
    }

    public static PossibleAdducts intersection(PossibleAdducts p1, Set<PrecursorIonType> p2) {
        return new PossibleAdducts(p1.value.stream().filter(p2::contains).collect(Collectors.toSet()));
    }

    public static PossibleAdducts intersection(PossibleAdducts p1, PossibleAdducts p2) {
        return new PossibleAdducts(p1.value.stream().filter(p2::contains).collect(Collectors.toSet()));
    }

    public int size() {
        return value.size();
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }

    public boolean contains(PrecursorIonType o) {
        return value.contains(o);
    }


    //if the list are a single PrecursorIonType we can convert it into one
    public boolean isPrecursorIonType() {
        return size() == 1;
    }

    public PrecursorIonType asPrecursorIonType() {
        if (isPrecursorIonType()) return value.iterator().next();
        return null;
    }

    public boolean hasOnlyPlainIonizationsWithoutModifications() {
        for (PrecursorIonType precursorIonType : value) {
            if (!precursorIonType.hasNeitherAdductNorInsource()) return false;
        }
        return true;
    }
}