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

package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * Describes how to deal with Adducts:
 * Pos Examples: [M+H]+,[M]+,[M+K]+,[M+Na]+,[M+H-H2O]+,[M+Na2-H]+,[M+2K-H]+,[M+NH4]+,[M+H3O]+,[M+MeOH+H]+,[M+ACN+H]+,[M+2ACN+H]+,[M+IPA+H]+,[M+ACN+Na]+,[M+DMSO+H]+
 * Neg Examples: [M-H]-,[M]-,[M+K-2H]-,[M+Cl]-,[M-H2O-H]-,[M+Na-2H]-,M+FA-H]-,[M+Br]-,[M+HAc-H]-,[M+TFA-H]-,[M+ACN-H]-
 */
public class AdductSettings implements Ms2ExperimentAnnotation {

    @NotNull protected final Set<PrecursorIonType> enforced;
    @NotNull protected final Set<PrecursorIonType> detectable;
    @NotNull protected final Set<PrecursorIonType> fallback;
    @NotNull protected final boolean prioritizeInputFileAdducts;

    /**
     * @param enforced   Enforced ion modes that are always considered.
     * @param detectable Detectable ion modes which are only considered if there is an indication in the MS1 scan (e.g. correct mass delta).
     * @param fallback   Fallback ion modes which are considered if the auto detection did not find any indication for an ion mode.
     * @param prioritizeInputFileAdducts Adducts specified in the input file are used as is independent of what enforced/detectable/fallback adducts are set.
     */
    @DefaultInstanceProvider
    public static AdductSettings newInstance(@DefaultProperty(propertyKey = "enforced") Set<PrecursorIonType> enforced, @DefaultProperty(propertyKey = "detectable") Set<PrecursorIonType> detectable, @DefaultProperty(propertyKey = "fallback") Set<PrecursorIonType> fallback, @DefaultProperty(propertyKey = "prioritizeInputFileAdducts") boolean prioritizeInputFileAdducts) {
        return new AdductSettings(enforced, detectable, fallback, prioritizeInputFileAdducts);
    }

    public AdductSettings withEnforced(Set<PrecursorIonType> enforced) {
        final HashSet<PrecursorIonType> enforcedJoin = new HashSet<>(this.enforced);
        enforcedJoin.addAll(enforced);
        return new AdductSettings(enforcedJoin, detectable, fallback, prioritizeInputFileAdducts);
    }


    protected AdductSettings() {
        this.enforced = new HashSet<>();
        this.detectable = new HashSet<>();
        this.fallback = new HashSet<>();
        this.prioritizeInputFileAdducts = true;
    }

    public AdductSettings(@NotNull Set<PrecursorIonType> enforced, @NotNull Set<PrecursorIonType> detectable, @NotNull Set<PrecursorIonType> fallback, @NotNull boolean prioritizeInputFileAdducts) {
        this.enforced = enforced;
        this.detectable = detectable;
        this.fallback = fallback;
        this.prioritizeInputFileAdducts = prioritizeInputFileAdducts;
    }

    public Set<PrecursorIonType> getEnforced() {
        return enforced;
    }

    public Set<PrecursorIonType> getDetectable() {
        return detectable;
    }

    public Set<PrecursorIonType> getFallback() {
        return fallback;
    }


    public Set<PrecursorIonType> getEnforced(int polarity) {
        return ensureRightPolarity(enforced, polarity);
    }

    public Set<PrecursorIonType> getDetectable(int polarity) {
        return ensureRightPolarity(detectable, polarity);
    }

    public Set<PrecursorIonType> getFallback(int polarity) {
        return ensureRightPolarity(fallback, polarity);
    }

    public Set<PrecursorIonType> getEnforced(Iterable<? extends Ionization> ionizations) {
        return availableAdductsForIonizations(enforced, ionizations);
    }

    public Set<PrecursorIonType> getDetectable(Iterable<? extends Ionization> ionizations) {
        return availableAdductsForIonizations(detectable, ionizations);
    }

    public Set<PrecursorIonType> getFallback(Iterable<? extends Ionization> ionizations) {
        return availableAdductsForIonizations(getFallback(), ionizations);
    }

    private Set<PrecursorIonType> ensureRightPolarity(Set<PrecursorIonType> set, int polarity) {
        return set.stream().filter(x->x.getCharge()==polarity).collect(Collectors.toSet());
    }

    private Set<PrecursorIonType> availableAdductsForIonizations(Set<PrecursorIonType> set, Iterable<? extends Ionization> ionizations){
        Set<PrecursorIonType> availableAdducts = new HashSet<>();
        for (Ionization ionization : ionizations) {
            for (PrecursorIonType ionType : set) {
                if (ionType.getIonization().equals(ionization)){
                    availableAdducts.add (ionType);

                }
            }
        }
        return availableAdducts;
    }

    @NotNull
    public boolean isPrioritizeInputFileAdducts() {
        return prioritizeInputFileAdducts;
    }
}
