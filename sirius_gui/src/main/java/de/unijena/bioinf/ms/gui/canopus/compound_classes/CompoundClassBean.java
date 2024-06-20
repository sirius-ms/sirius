/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.canopus.compound_classes;

import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import de.unijena.bioinf.ms.nightsky.sdk.model.CompoundClass;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class CompoundClassBean implements SiriusPCS, Comparable<CompoundClassBean> {
    protected final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this, true);

    @Override
    public HiddenChangeSupport pcs() {
        return pcs;
    }

    @NotNull
    @Getter
    private final CompoundClass sourceClass;

    public CompoundClassBean(@NotNull CompoundClass sourceClass) {
        this.sourceClass = sourceClass;
    }


    public ClassyfireProperty getParent() {
        return null; //todo nightsky -> how do we find the parent, do we want to add it to sdk
    }

    public String getChemontIdentifier() {
        if (getSourceClass().getId() == null)
            return null;
        return String.format(Locale.US, "CHEMONT:%07d", getSourceClass().getId());
    }

    @Override
    public int compareTo(@NotNull CompoundClassBean o) {
        return Double.compare(
                o.getSourceClass().getProbability(),
                sourceClass.getProbability()
        );
    }
}
