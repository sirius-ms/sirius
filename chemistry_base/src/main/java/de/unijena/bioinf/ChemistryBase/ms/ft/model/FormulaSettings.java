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

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * These configurations hold the information how to autodetect elements based on the given formula constraints.
 * Note: If the compound is already assigned to a specific molecular formula, this annotation is ignored.
 */
public class FormulaSettings implements Ms2ExperimentAnnotation {

    public final static String EXTENDED_ORGANIC_ELEMENT_FILTER_ENFORCED_CHNOPFI_STRING = "CHNOPFI";
    public final static String EXTENDED_ORGANIC_ELEMENT_FILTER_DETECTABLE_SBBrCl_STRING = "SBBrCl"; //todo NewWorkflow: add Se?

    /**
     * This Formula contraint allowing "only organic" formulas can e.g. be used as a default if the element filter for database search is activated in the GUI compute dialog
     */
    public final static FormulaConstraints ORGANIC_ELEMENT_FILTER_CHNOPSBBrClIF = FormulaConstraints.fromString(EXTENDED_ORGANIC_ELEMENT_FILTER_ENFORCED_CHNOPFI_STRING+EXTENDED_ORGANIC_ELEMENT_FILTER_DETECTABLE_SBBrCl_STRING);

    @NotNull protected final FormulaConstraints enforced;
    @NotNull protected final FormulaConstraints fallback;
    @NotNull protected final ChemicalAlphabet detectable;

    /**
     * @param enforced   Enforced elements are always considered
     * @param detectable Detectable elements are added to the chemical alphabet, if there are indications for them (e.g. in isotope pattern)
     * @param fallback   Fallback elements are used, if the auto-detection fails (e.g. no isotope pattern available)
     */
    @DefaultInstanceProvider
    public static FormulaSettings newInstance(@DefaultProperty(propertyKey = "enforced") FormulaConstraints enforced, @DefaultProperty(propertyKey = "detectable") ChemicalAlphabet detectable, @DefaultProperty(propertyKey = "fallback") FormulaConstraints fallback) {
        return new FormulaSettings(enforced, detectable, fallback);
    }

    public FormulaSettings(@NotNull FormulaConstraints enforced, @NotNull ChemicalAlphabet detectable, @NotNull FormulaConstraints fallback) {
        this.enforced = enforced;
        this.fallback = fallback;
        this.detectable = detectable;
    }

    public FormulaConstraints getEnforcedAlphabet() {
        return enforced;
    }

    public FormulaConstraints getFallbackAlphabet() {
        return fallback;
    }

    @NotNull public Set<Element> getAutoDetectionElements() {
        return detectable.toSet();
    }


    public boolean isElementDetectionEnabled() {
        return detectable.size() > 0;
    }

    @NotNull public FormulaSettings autoDetect(Element... elems) {
        return new FormulaSettings(enforced,detectable.extend(elems),fallback);
    }

    @NotNull public FormulaSettings withoutAutoDetect() {
        return new FormulaSettings(enforced, new ChemicalAlphabet(new Element[0]), fallback);
    }

    public ChemicalAlphabet getAutoDetectionAlphabet() {
        return detectable;
    }

    public FormulaSettings enforce(FormulaConstraints constraints) {
        return new FormulaSettings(constraints, detectable, fallback);
    }

    public FormulaSettings withFallback(FormulaConstraints constraints) {
        return new FormulaSettings(enforced, detectable, constraints);
    }
}
