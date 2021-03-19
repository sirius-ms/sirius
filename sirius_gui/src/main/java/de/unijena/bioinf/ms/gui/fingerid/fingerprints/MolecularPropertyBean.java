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

package de.unijena.bioinf.ms.gui.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;

/**
 * This class is immutable, but we have to extend Property support,
 * because the reactive glazed lists do need the property change methods,
 * to register itself even if they do nothing. Otherwise the event lists throw an error.
 */
public class MolecularPropertyBean<P extends MolecularProperty> implements SiriusPCS, Comparable<MolecularPropertyBean<P>> {
    protected final ProbabilityFingerprint underlyingFingerprint;
    protected final int absoluteIndex;
    protected final double fscore;
    protected final int numberOfTrainingExamples;

    public MolecularPropertyBean(ProbabilityFingerprint underlyingFingerprint, int absoluteIndex, double fscore, int numberOfTrainingExamples) {
        this.underlyingFingerprint = underlyingFingerprint;
        this.absoluteIndex = absoluteIndex;
        this.fscore = fscore;
        this.numberOfTrainingExamples = numberOfTrainingExamples;
    }

    public int getNumberOfTrainingExamples() {
        return numberOfTrainingExamples;
    }

    public int getAbsoluteIndex() {
        return absoluteIndex;
    }

    public double getProbability() {
        return underlyingFingerprint.getProbability(absoluteIndex);
    }

    public P getMolecularProperty() {
        return (P) underlyingFingerprint.getFingerprintVersion().getMolecularProperty(absoluteIndex);
    }

    public double getFScore() {
        return fscore;
    }


    @Override
    public int compareTo(MolecularPropertyBean<P> o) {
        return Double.compare(o.getProbability(), getProbability());
    }

    @Override
    public String toString() {
        return absoluteIndex + ": " + getMolecularProperty().toString();
    }

    protected final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this,true);
    @Override
    public HiddenChangeSupport pcs() {
        return pcs;
    }

}
