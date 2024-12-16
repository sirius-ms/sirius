/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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

package de.unijena.bioinf.ms.gui.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.*;
import org.jetbrains.annotations.NotNull;

/**
 * This class is immutable, but we have to extend Property support,
 * because the reactive glazed lists do need the property change methods,
 * to register itself even if they do nothing. Otherwise the event lists throw an error.
 */
public class FingerIdPropertyBean extends MolecularPropertyBean<MolecularProperty> {
    protected final FingerprintVisualization visualization;

    public FingerIdPropertyBean(ProbabilityFingerprint underlyingFingerprint, FingerprintVisualization viz, int absoluteIndex, double fscore, int numberOfTrainingExamples) {
        super(underlyingFingerprint, absoluteIndex, fscore, numberOfTrainingExamples);
        this.visualization = viz;
    }

    public int getMatchSize() {
        if (visualization == null) return 0;
        return visualization.numberOfMatchesAtoms;
    }

    public int getMatchSizeDescription() {
        return Math.max(0, getMatchSize());
    }

    public CdkFingerprintVersion.USED_FINGERPRINTS getFingerprintType() {
        final CdkFingerprintVersion v;
        FingerprintVersion vv = underlyingFingerprint.getFingerprintVersion();
        if (vv instanceof MaskedFingerprintVersion) vv = ((MaskedFingerprintVersion) vv).getMaskedFingerprintVersion();
        if (vv instanceof CdkFingerprintVersion) v = (CdkFingerprintVersion) vv;
        else throw new RuntimeException("Can only deal with CDK fingerprints");
        return v.getFingerprintTypeFor(absoluteIndex);
    }

    public String getFingerprintTypeName() {
       return getFingerprintType().getDisplayName();
    }


    @Override
    public int compareTo(@NotNull MolecularPropertyBean<MolecularProperty> o) {
        int i = super.compareTo(o);
        if (o instanceof FingerIdPropertyBean) {
            if (i == 0)
                i = Integer.compare(((FingerIdPropertyBean) o).getMatchSize(), getMatchSize());
        }
        if (i == 0)
            return Integer.compare(absoluteIndex, o.absoluteIndex);
        return i;
    }
}
