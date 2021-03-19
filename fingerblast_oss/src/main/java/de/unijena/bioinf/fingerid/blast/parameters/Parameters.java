/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.fingerid.blast.parameters;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.blast.FingerblastScoring;
import de.unijena.bioinf.fingerid.blast.FingerblastScoringMethod;

public interface Parameters {
    Parameters EMPTY = new Parameters() {
    };

    @FunctionalInterface
    interface FP {
        //this is the predicted fingerprint;
        ProbabilityFingerprint getFP();
    }


    @FunctionalInterface
    interface Stats {
        PredictionPerformance[] getStatistics();
    }

    @FunctionalInterface
    interface WithMolecularFormula {
        MolecularFormula getFormula();
    }

    interface WithMFandFP<S extends FingerblastScoring<?>> extends FP, WithMolecularFormula {
    }

    interface PreparedScoring<S extends FingerblastScoring<?>> extends FP {
        S getPreparedScoring();
    }


    interface UnpreparedScoring<S extends FingerblastScoringMethod<? extends FingerblastScoring<P>>, P extends Parameters.FP> extends FP {
        S getScoring();

        P getScorerParameter();

        @Override
        default ProbabilityFingerprint getFP() {
            return getScorerParameter().getFP();
        }
    }


    interface ScorerParameterProvider{
        <P> P getScorerParameter(Class<? extends FingerblastScoring<P>> key);
    }

    interface NestedParameters<P> {
        P getNested();
    }
}
