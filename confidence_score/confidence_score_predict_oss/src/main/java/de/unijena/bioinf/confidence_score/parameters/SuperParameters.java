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

package de.unijena.bioinf.confidence_score.parameters;

import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.blast.FingerblastScoring;
import de.unijena.bioinf.fingerid.blast.parameters.BayesnetDynamicParameters;
import de.unijena.bioinf.fingerid.blast.parameters.Parameters;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.util.Arrays;

public interface SuperParameters extends Parameters.ScorerParameterProvider {
    <P> Parameters.NestedParameters<P> asNestedScorerParameter(Class<? extends FingerblastScoring<P>> key);

    class Default extends FpIdResultParameters<IdentificationResult<?>> implements SuperParameters {

        public Default(ProbabilityFingerprint query, IdentificationResult<?> idresult) {
            super(query, idresult);
        }

        @Override
        public <P> NestedParameters<P> asNestedScorerParameter(Class<? extends FingerblastScoring<P>> key) {
            return new DefaultAsNested<>(this) {
                @Override
                public P getNested() {
                    return getScorerParameter(key);
                }
            };
        }

        @Override
        public <P> P getScorerParameter(Class<? extends FingerblastScoring<P>> key) {
            Class<?> clz = Arrays.stream(key.getDeclaredMethods()).filter(m -> m.getName().equals("prepare")).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported Fingerblast Scoring" + key.getName()))
                    .getParameterTypes()[0];

            if (clz.isAssignableFrom(Parameters.class))
                return (P) Parameters.EMPTY;
            if (clz.isAssignableFrom(FP.class)) {
                return (P) (FP) () -> getFP();
            } else if (clz.isAssignableFrom(BayesnetDynamicParameters.class)) {
                //todo return bayesnet Scoring parameters in needed
            }

            throw new IllegalArgumentException("Unsupported Fingerblast Scoring" + key.getName());
        }
    }

    abstract class DefaultAsNested<P> extends Default implements Parameters.NestedParameters<P> {
        public DefaultAsNested(Default def) {
            this(def.getFP(), def.getIdResult());

        }

        public DefaultAsNested(ProbabilityFingerprint query, IdentificationResult<?> idresult) {
            super(query, idresult);
        }
    }
}
