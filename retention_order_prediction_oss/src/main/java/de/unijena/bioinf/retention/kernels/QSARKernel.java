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

package de.unijena.bioinf.retention.kernels;

import de.unijena.bioinf.retention.PredictableCompound;
import gnu.trove.list.array.TDoubleArrayList;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.descriptors.molecular.*;
import org.openscience.cdk.qsar.result.DoubleArrayResult;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

public class QSARKernel implements MoleculeKernel<QSARKernel.Prepared> {


    @Override
    public Prepared prepare(PredictableCompound compound) {
        return new Prepared(compound);
    }

    @Override
    public double compute(PredictableCompound left, PredictableCompound right, Prepared preparedLeft, Prepared preparedRight) {
        double uv = 0d;
        double uu=0d, vv=0d;
        for (int k=0; k < preparedLeft.vector.length; ++k) {
            uv += preparedLeft.vector[k] * preparedRight.vector[k];
            uu += preparedLeft.vector[k] * preparedLeft.vector[k];
            vv += preparedRight.vector[k] * preparedRight.vector[k];
        }
        if (uu==0 || vv==0) return 0d;
        return uv / Math.sqrt(vv*uu);

    }

    public static class Prepared {

        public double[] vector;
        protected final static double[] MEANS = new double[]{
                1.070283000000001, 2.301810808679999, 110.19450687000014, 57.44792567640004, 20.881746627118382, 9.394255903180758, 5.04238130844363, 82.61320000000002, 33.6477113236, 3.0253837999999966
    };
        protected final static double[] SCALE = new double[]{
                1.075316283049319, 2.8274211553982354, 17.97499150683021, 9.715539974068038, 3.399416968156456, 1.7483158312021772, 1.1129729913518018, 23.973827203014434, 7.618998079000571, 1.3431225277306444

    };

        public Prepared(PredictableCompound compound) {
            final TDoubleArrayList vec = new TDoubleArrayList();
            {
                try {
                    ALOGPDescriptor logp = new ALOGPDescriptor();
                    final IAtomContainer copy = AtomContainerManipulator.copyAndSuppressedHydrogens(compound.getMolecule());
                    AtomContainerManipulator.percieveAtomTypesAndConfigureUnsetProperties(copy);
                    Aromaticity.cdkLegacy().apply(copy);
                    AtomContainerManipulator.convertImplicitToExplicitHydrogens(copy);
                    final DoubleArrayResult value = (DoubleArrayResult)logp.calculate(compound.getMolecule()).getValue();
                    vec.add(value.get(0));
                    vec.add(value.get(1));
                    vec.add(value.get(2));

                    final APolDescriptor apol = new APolDescriptor();
                    vec.add(((DoubleResult)(apol.calculate(copy).getValue())).doubleValue());

                    final KappaShapeIndicesDescriptor kappa = new KappaShapeIndicesDescriptor();
                    final DoubleArrayResult re = (DoubleArrayResult) kappa.calculate(copy).getValue();
                    vec.add(re.get(0));
                    vec.add(re.get(1));
                    vec.add(re.get(2));

                    final TPSADescriptor tpsa = new TPSADescriptor();
                    vec.add( ((DoubleResult) tpsa.calculate(copy).getValue()).doubleValue() );

                    final BPolDescriptor bpol = new BPolDescriptor();
                    vec.add(((DoubleResult)(bpol.calculate(copy).getValue())).doubleValue());

                    final XLogPDescriptor xlog = new XLogPDescriptor();
                    vec.add(((DoubleResult)(xlog.calculate(copy).getValue())).doubleValue());

                    this.vector = vec.toArray();
                    for (int k=0; k < vector.length; ++k) {
                        vector[k]-= MEANS[k];
                        vector[k]/= SCALE[k];
                    }

                } catch (CDKException e) {
                    e.printStackTrace();
                }
            }

        }
    }

}
