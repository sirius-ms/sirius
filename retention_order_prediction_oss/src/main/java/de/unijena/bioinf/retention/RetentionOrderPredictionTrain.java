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

package de.unijena.bioinf.retention;

import de.unijena.bioinf.ChemistryBase.math.KernelCentering;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.retention.kernels.MoleculeKernel;
import de.unijena.bioinf.retention.kernels.ShortestPathKernel;
import de.unijena.bioinf.retention.kernels.SubstructureKernel;

import java.util.ArrayList;
import java.util.List;

public class RetentionOrderPredictionTrain {

    protected final RetentionOrderDataset dataset;
    private double[][] kernel;
    private KernelCentering centering[];
    private MoleculeKernel<?>[] kernels;

    public RetentionOrderPredictionTrain(RetentionOrderDataset dataset) {
        this.dataset = dataset;
        this.kernels = new MoleculeKernel[]{new SubstructureKernel(), new ShortestPathKernel()};
    }

    public BasicJJob<Object> computeKernel() {
        return new BasicMasterJJob<>(JJob.JobType.SCHEDULER) {
            @Override
            protected Object compute() throws Exception {
                int[] usedIndizes = dataset.getUsedIndizes();

                final List<JJob<double[][]>> kernelComputations = new ArrayList<>();
                for (MoleculeKernel<?> k : kernels) {
                    kernelComputations.add(submitSubJob(dataset.computeTrainKernel(k)));
                }
                centering = new KernelCentering[kernels.length];
                for (int k=0; k < kernelComputations.size(); ++k) {
                    double[][] K = kernelComputations.get(k).takeResult();
                    centering[k] = new KernelCentering(K, true);
                    centering[k].applyToTrainMatrix(K);
                    if (kernel==null) kernel = K;
                    else MatrixUtils.applySum(kernel, K);
                }
                MatrixUtils.applyScale(kernel, 1d/kernels.length);
                return true;
            }
        };
    }


}
