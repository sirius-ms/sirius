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

package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.BooleanFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

public class BayesnetScoringTrainingData {
    public final MolecularFormula[] formulasReferenceData;
    public final ArrayFingerprint[] trueFingerprintsReferenceData;
    public final ProbabilityFingerprint[] estimatedFingerprintsReferenceData;

    public final PredictionPerformance[] predictionPerformances;

    public BayesnetScoringTrainingData(MolecularFormula[] formulasReferenceData, ArrayFingerprint[] trueFingerprintsReferenceData, ProbabilityFingerprint[] estimatedFingerprintsReferenceData, PredictionPerformance[] predictionPerformances) {
        this.formulasReferenceData = formulasReferenceData;
        this.trueFingerprintsReferenceData = trueFingerprintsReferenceData;
        this.estimatedFingerprintsReferenceData = estimatedFingerprintsReferenceData;
        this.predictionPerformances = predictionPerformances;
    }


}
