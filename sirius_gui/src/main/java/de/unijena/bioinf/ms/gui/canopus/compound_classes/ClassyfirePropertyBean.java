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

package de.unijena.bioinf.ms.gui.canopus.compound_classes;

import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.ms.gui.fingerid.fingerprints.MolecularPropertyBean;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a Canopus fingerprint (CompoundClasses) with PropertyChangeSupport, to be a GlazedLists compatible list element.
 */
public class ClassyfirePropertyBean extends MolecularPropertyBean<ClassyfireProperty> {
    public ClassyfirePropertyBean(ProbabilityFingerprint underlyingFingerprint, int absoluteIndex, double fscore, int numberOfTrainingExamples) {
        super(underlyingFingerprint, absoluteIndex, fscore, numberOfTrainingExamples);
    }

    public static List<ClassyfirePropertyBean> fromCanopusResult(@NotNull ProbabilityFingerprint canopusFP) {
        List<ClassyfirePropertyBean> list = new ArrayList<>(canopusFP.cardinality());
        canopusFP.forEach(cc -> list.add(new ClassyfirePropertyBean(
                canopusFP,
                cc.getIndex(),
                Double.NaN,
                0
        )));

        return list;
    }

    public static List<ClassyfirePropertyBean> fromCanopusResult(@NotNull CanopusResult res) {
        return fromCanopusResult(res.getCanopusFingerprint());
    }


}
