package de.unijena.bioinf.retention;

import de.unijena.bioinf.ChemistryBase.math.KernelCentering;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;

import java.util.ArrayList;
import java.util.List;

public class RetentionOrderPredictionTrain {

    protected final RetentionOrderDataset dataset;
    private double[][] kernel;
    private KernelCentering centering[];
    private MoleculeKernel[] kernels;

    public RetentionOrderPredictionTrain(RetentionOrderDataset dataset) {
        this.dataset = dataset;
        this.kernels = new MoleculeKernel[]{new SubstructureKernel(), new ShortestPathKernel()};
    }

    public BasicJJob computeKernel() {
        return new BasicMasterJJob(JJob.JobType.SCHEDULER) {
            @Override
            protected Object compute() throws Exception {
                int[] usedIndizes = dataset.getUsedIndizes();

                final List<JJob<double[][]>> kernelComputations = new ArrayList<>();
                for (MoleculeKernel k : kernels) {
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
