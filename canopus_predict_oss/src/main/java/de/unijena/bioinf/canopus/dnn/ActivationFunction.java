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

package de.unijena.bioinf.canopus.dnn;

public interface ActivationFunction {

    void eval(float[] values);

    class Identity implements ActivationFunction {

        @Override
        public void eval(float[] values) {

        }
    }

    class SELU implements ActivationFunction {
        private final static double alpha = 1.6732632423543772848170429916717;
        private final static double lambda = 1.0507009873554804934193349852946;
        @Override
        public void eval(float[] values) {
            for (int i=0; i < values.length; ++i) {
                final double x = values[i];
                values[i] = (float)((x > 0) ? lambda*x : lambda*(alpha*Math.exp(x) - alpha));
            }
        }
    }

    class Tanh implements ActivationFunction {
        @Override
        public void eval(float[] values) {
            for (int i=0; i < values.length; ++i) {
                values[i] = (float)Math.tanh(values[i]);
            }
        }
    }

    class ReLu implements ActivationFunction {
        @Override
        public void eval(float[] values) {
            for (int i=0; i < values.length; ++i) {
                values[i] = Math.max(0, values[i]);
            }
        }
    }

}
