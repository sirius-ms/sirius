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

package de.unijena.bioinf.ms.frontend.subtools.similarity;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FPIter2;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.ftalign.StandardScoring;
import de.unijena.bioinf.ftalign.analyse.Pearson;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.config.AddConfigsJob;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.treealign.AbstractBacktrace;
import de.unijena.bioinf.treealign.multijoin.DPMultiJoin;
import de.unijena.bionf.spectral_alignment.CosineQuerySpectrum;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class SimilarityMatrixWorkflow implements Workflow {

    private final static List<Class<? extends FormulaScore>> rankSores =
            List.of(TopCSIScore.class, ZodiacScore.class, SiriusScore.class);
    protected final SimilarityMatrixOptions options;
    protected ProjectSpaceManager ps;
    protected final ParameterConfig config;

    protected final PreprocessingJob<? extends ProjectSpaceManager> ppj;
    protected final ProjectSpaceManagerFactory<?> projectFactory;

    public SimilarityMatrixWorkflow(PreprocessingJob<? extends ProjectSpaceManager> ppj, ProjectSpaceManagerFactory<?> projectFactory, SimilarityMatrixOptions options, ParameterConfig config) {
        this.ppj = ppj;
        this.options = options;
        this.config = config;
        this.projectFactory = projectFactory;
    }

    @Override
    public void run() {
        System.out.println("Fixed model 2.");
        /*List<Instance> xs = new ArrayList<>();

        try {
            ps = SiriusJobs.getGlobalJobManager().submitJob(ppj).awaitResult();
            ps.forEach(xs::add);
            if (options.useCosine)
                cosine(xs, options.useMinPeaks);
            //filter all instances without a single fragTree
            xs = xs.stream().filter(i -> i.loadCompoundContainer().hasResults())
                .filter(x-> x.loadTopFormulaResult(rankSores, FTree.class).map(y->y.hasAnnotation(FTree.class)).orElse(false))
                    .collect(Collectors.toList());
            FTree[] trees = xs.stream().map(x->x.loadTopFormulaResult(rankSores, FTree.class).get().getAnnotationOrThrow(FTree.class))
                    .toArray(FTree[]::new);

            if (options.useAlignment)
                align(xs, trees);
            if (options.useFtblast != null)
                ftblast(xs, trees);
            if (options.useTanimoto)
                tanimoto(xs);
            if (options.useCanopus)
                tanimotoCanopus(xs);
        } catch (ExecutionException e) {
            LoggerFactory.getLogger(this.getClass()).error("Error when parsing project space");
        }*/
    }

    /*private void tanimoto(List<Instance> xs) {
        final JobManager jobManager = SiriusJobs.getGlobalJobManager();
        xs.removeIf(x -> x.loadTopFormulaResult(rankSores, FingerprintResult.class).filter(y -> y.hasAnnotation(FingerprintResult.class)).isEmpty());

        if (xs.isEmpty()){
            LoggerFactory.getLogger(getClass()).warn("No Compounds with predicted Fingerprints found! You might want to run CSI:FingerID first. Skipping tanimoto computation!");
            return;
        }
        final ArrayList<ProbabilityFingerprint> fingerprintValues = new ArrayList<>();
        for (SiriusProjectSpaceInstance x : xs) {
            fingerprintValues.add(x.loadTopFormulaResult(rankSores, FingerprintResult.class)
                    .map(it -> it.getAnnotationOrThrow(FingerprintResult.class).fingerprint).orElseThrow());
        }

        final ProbabilityFingerprint[] fps = fingerprintValues.toArray(ProbabilityFingerprint[]::new);
        final double[][] M = new double[xs.size()][xs.size()];
        final TIntHashSet pubchemMaccs = new TIntHashSet();
        for (int index : CdkFingerprintVersion.getDefault().getMaskFor(CdkFingerprintVersion.USED_FINGERPRINTS.PUBCHEM, CdkFingerprintVersion.USED_FINGERPRINTS.MACCS).allowedIndizes()) {
            pubchemMaccs.add(index);
        }
        jobManager.submitJob(MatrixUtils.parallelizeSymmetricMatrixComputation(M, (i, j) -> fpcos(fps[i],fps[j]))).takeResult();
        //MatrixUtils.normalize(M);
        writeMatrix("tanimoto", M, xs.stream().map(Instance::getName).toArray(String[]::new), options.digits);
    }


    private void tanimotoCanopus(List<Instance> xs) {
        final JobManager jobManager = SiriusJobs.getGlobalJobManager();
        xs.removeIf(x -> x.loadTopFormulaResult(rankSores, CanopusResult.class).filter(y -> y.hasAnnotation(CanopusResult.class)).isEmpty());

        if (xs.isEmpty()){
            LoggerFactory.getLogger(getClass()).warn("No Compounds with predicted Fingerprints found! You might want to run CSI:FingerID first. Skipping tanimoto computation!");
            return;
        }
        final ArrayList<ProbabilityFingerprint> fingerprintValues = new ArrayList<>();
        for (SiriusProjectSpaceInstance x : xs) {
            fingerprintValues.add(x.loadTopFormulaResult(rankSores, CanopusResult.class)
                    .map(it -> it.getAnnotationOrThrow(CanopusResult.class).getCanopusFingerprint()).orElseThrow());
        }

        final ProbabilityFingerprint[] fps = fingerprintValues.toArray(ProbabilityFingerprint[]::new);
        final double[][] M = new double[xs.size()][xs.size()];
        final TIntHashSet pubchemMaccs = new TIntHashSet();
        for (int index : CdkFingerprintVersion.getDefault().getMaskFor(CdkFingerprintVersion.USED_FINGERPRINTS.PUBCHEM, CdkFingerprintVersion.USED_FINGERPRINTS.MACCS).allowedIndizes()) {
            pubchemMaccs.add(index);
        }
        jobManager.submitJob(MatrixUtils.parallelizeSymmetricMatrixComputation(M, (i, j) -> fps[i].asDeterministic().tanimoto(fps[j].asDeterministic()))).takeResult();
        //MatrixUtils.normalize(M);
        writeMatrix("canopus", M, xs.stream().map(Instance::getName).toArray(String[]::new), options.digits);
    }

    private static double specialTanimoto(ProbabilityFingerprint left, ProbabilityFingerprint right, double varianceLeft, double varianceRight) {
        double union = 0d, intersection = 0d;
        for (FPIter2 f : left.foreachPair(right)) {
            union += 1d - (1d-f.getLeftProbability())*(1d-f.getRightProbability());
            intersection += f.getLeftProbability()*f.getRightProbability();
        }
        union += (varianceLeft+varianceRight);
        return intersection/union;
    }

    private void writeMatrix(String name, double[][] M, String[] header, int digits) {
        final File file = new File(options.outputDirectory, name + (options.numpy ? ".txt" : ".tsv"));

        try {
            Files.createDirectories(options.outputDirectory.toPath());
            try (BufferedWriter bw = FileUtils.getWriter(file)) {
                if (options.numpy) {
                    bw.write('#');
                    bw.write(header[0]);
                    for (int i = 1; i < header.length; ++i) {
                        bw.write("\t");
                        bw.write(header[i]);
                    }
                    bw.newLine();
                    FileUtils.writeDoubleMatrix(bw, M);
                } else {
                    bw.write("FeatureName");
                    for (String h : header) {
                        bw.write('\t');
                        bw.write(h);
                    }
                    bw.newLine();
                    for (int i = 0; i < M.length; ++i) {
                        bw.write(header[i]);
                        for (int j = 0; j < M.length; ++j) {
                            bw.write('\t');
                            if (options.digits >= 0) {
                                BigDecimal v = BigDecimal.valueOf(M[i][j]).setScale(digits, RoundingMode.HALF_UP);
                                bw.write(v.toString());
                            }else {
                                bw.write(String.valueOf(M[i][j]));
                            }
                        }
                        bw.newLine();
                    }
                }
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(SimilarityMatrixWorkflow.class).error(file.getAbsolutePath() + " cannot be written due to: " + e.getMessage(), e);
            System.err.println("Cannot write file '" + file + "' due to IO error: " + e.getMessage());
        }
    }

    private void ftblast(List<Instance> xs, FTree[] trees) {
        final JobManager Jobs= SiriusJobs.getGlobalJobManager();
        final List<FTree> libTrees = new ArrayList<>();
        if (ProjectSpaceIO.isExistingProjectspaceDirectory(options.useFtblast.toPath())) {
            try {
                ProjectSpaceManager passatuttoProject = projectFactory.createOrOpen(options.useFtblast.toPath());

                passatuttoProject.forEach(instance ->
                        instance.loadTopFormulaResult(FTree.class)
                                .flatMap(fr -> fr.getAnnotation(FTree.class))
                                .ifPresent(libTrees::add));
            } catch (IOException e) {
                throw new RuntimeException("Cannot open project space at '" + options.useFtblast + "'");
            }
        } else {
            final HashMap<String, MolecularFormula> cache = new HashMap<>();
            // just take all json files from the directory
            for (File f : options.useFtblast.listFiles()) {
                if (f.getName().endsWith(".json")) {
                   try (BufferedReader br = FileUtils.getReader(f)){
                       libTrees.add(new FTJsonReader(cache).parse(br, f.toURI()));
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
                }
            }
        }

        System.out.println("Library consists of " + libTrees.size() + " fragmentation libTrees");
        Collections.addAll(libTrees, trees);

        final double[][] M = new double[trees.length][trees.length];
        final double[][] C = new double[trees.length][libTrees.size()];
        final StandardScoring standardScoring = new StandardScoring(true);
        final ArrayList<BasicJJob<Double>> jobs = new ArrayList<>();
        for (int i = 0; i < trees.length; ++i) {
            final int I = i;
            final double selfScore = standardScoring.selfAlignScore(trees[i].getRoot());
            for (int j = 0; j < libTrees.size(); ++j) {
                final int J = j;
                jobs.add(Jobs.submitJob(new BasicJJob<Double>() {
                    @Override
                    protected Double compute() throws Exception {
                        final double score = new DPMultiJoin<>(standardScoring, 2, trees[I].getRoot(),libTrees.get(J).getRoot(),trees[I].treeAdapter()).compute();
                        C[I][J] = score / Math.sqrt(Math.min(selfScore, standardScoring.selfAlignScore(libTrees.get(J).getRoot())));
                        return C[I][J];
                    }
                }));
            }
        }
        jobs.forEach(BasicJJob::takeResult);
        jobs.clear();

        Jobs.submitJob(MatrixUtils.parallelizeSymmetricMatrixComputation(M, (i,j)->Pearson.pearson(C[i],C[j]))).takeResult();
        writeMatrix("ftblast", M, xs.stream().map(x->x.getName()).toArray(String[]::new), options.digits);
    }

    private void align(List<Instance> xs, FTree[] trees) {
        final JobManager J = SiriusJobs.getGlobalJobManager();
        final double[][] M = new double[trees.length][trees.length];
        J.submitJob(MatrixUtils.parallelizeSymmetricMatrixComputation(M, (i,j)->
        {
            final DPMultiJoin<Fragment> dp = new DPMultiJoin<>(new StandardScoring(true), 2, trees[i].getRoot(), trees[j].getRoot(), trees[i].treeAdapter());
            final double result = dp.compute();
            int[] numberOfMatchingLosses = new int[]{0};
            dp.backtrace(new AbstractBacktrace<>(){
                @Override
                public void match(float score, Fragment left, Fragment right) {
                    ++numberOfMatchingLosses[0];
                }

                @Override
                public void join(float score, Iterator<Fragment> left, Iterator<Fragment> right, int leftNumber, int rightNumber) {
                    ++numberOfMatchingLosses[0];
                }
            });
            return numberOfMatchingLosses[0]>=6 ? result : 0d;
        })).takeResult();
        final double[] norm = MatrixUtils.selectDiagonal(M);
        for (int i=0; i < M.length; ++i) {
            for (int j=0; j < M.length; ++j) {
                M[i][j] = (norm[i]==0 || norm[j]==0) ? 0 :  M[i][j] / Math.sqrt(norm[i] * norm[j]);
            }
        }
        writeMatrix("ftalign", M, xs.stream().map(Instance::getName).toArray(String[]::new), options.digits);

    }

    private void cosine(List<Instance> xs, int minPeaks) {
        final JobManager J = SiriusJobs.getGlobalJobManager();

        List<Pair<Instance, CosineQuerySpectrum>> pairs = J.submitJobsInBatches(xs.stream().map(this::getSpectrum).collect(Collectors.toList())).stream().map(JJob::getResult).filter(Objects::nonNull).filter(c -> c.getRight().getSelfSimilarity() > 0 && c.getRight().getSelfSimilarityLosses() > 0).collect(Collectors.toList());
        xs = pairs.stream().map(Pair::getLeft).collect(Collectors.toList());
        final CosineQuerySpectrum[] cosineQuerySpectrums = pairs.stream().map(Pair::getRight).toArray(CosineQuerySpectrum[]::new);
        final CosineQueryUtils cosineQueryUtils = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(config.createInstanceWithDefaults(MS2MassDeviation.class).allowedMassDeviation.multiply(2)));
        final double[][] M = new double[xs.size()][xs.size()];
        J.submitJob(MatrixUtils.parallelizeSymmetricMatrixComputation(M, (i,j)-> withAtLeast(cosineQueryUtils.cosineProductWithLosses(cosineQuerySpectrums[i],cosineQuerySpectrums[j]), minPeaks))).takeResult();
        writeMatrix("cosine", M, xs.stream().map(Instance::getName).toArray(String[]::new), options.digits);
    }

    private static double withAtLeast(SpectralSimilarity similarity, int minPeaks) {
        if (similarity.sharedPeaks < minPeaks) return 0d;
        else return similarity.similarity;
    }

    private BasicJJob<Pair<Instance,CosineQuerySpectrum>> getSpectrum(Instance i) {
        return new BasicMasterJJob<>(JJob.JobType.CPU) {
            @Override
            protected Pair<Instance,CosineQuerySpectrum> compute() throws Exception {
                final AddConfigsJob addConfigsJob = new AddConfigsJob(config);
                submitSubJob(addConfigsJob.addRequiredJob((Callable<Instance>) () -> i)).takeResult();
                submitSubJob(addConfigsJob).takeResult();

                final Ms2Experiment exp = i.getExperiment();
                final Sirius sirius = ApplicationCore.SIRIUS_PROVIDER.sirius(config.getConfigValue("AlgorithmProfile"));
                final CosineQueryUtils cosineQueryUtils = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(config.createInstanceWithDefaults(MS2MassDeviation.class).allowedMassDeviation.multiply(2)));
                ProcessedInput processedInput = sirius.preprocessForMs2Analysis(exp);
                return Pair.of(i,cosineQueryUtils.createQueryWithIntensityTransformation(Spectrums.from(processedInput.getMergedPeaks()), processedInput.getExperimentInformation().getIonMass(), true));
            }
        };
    }

    private static double fpcos(ProbabilityFingerprint left, ProbabilityFingerprint right) {
        int count=0, intersection=0;
        double vx=0d, vy=0d, vxy=0d;
        for (FPIter2 x : left.foreachPair(right)) {
            final double l = unsmooth(x.getLeftProbability());
            final double r = unsmooth(x.getRightProbability());
            vx += l*l;
            vy += r*r;
            vxy += l*r;
            if (x.isLeftSet() && x.isRightSet()) {
                ++intersection;
            }
            ++count;
        }
        if (intersection<60) return 0d;
        if (vx==0 || vy==0) return 0d;
        return vxy / Math.sqrt(vx*vy);
    }

    private static double tanimotoEstimate(ProbabilityFingerprint left, ProbabilityFingerprint right, TIntHashSet maccsAndPubchemIndizes) {
        double union=0d, intersection=0d, roundedUnion=0d, roundedIntersection=0d, pubchemUnion=0d,pubchemIntersection=0d, cosineLeft=0d, cosineRight=0d, cosine=0d;
        for (FPIter2 f : left.foreachPair(right)) {
            union += 1d - (1d-unsmooth(f.getLeftProbability()))*(1d-unsmooth(f.getRightProbability()));
            intersection += unsmooth(f.getLeftProbability())*unsmooth(f.getRightProbability());
            if (f.isLeftSet() || f.isRightSet()) roundedUnion += 1;
            if (f.isLeftSet() && f.isRightSet()) roundedIntersection += 1;
            cosineLeft += f.getLeftProbability()*f.getLeftProbability();
            cosineRight += f.getRightProbability()*f.getRightProbability();
            cosine += f.getLeftProbability()*f.getRightProbability();
            if (maccsAndPubchemIndizes.contains(f.getIndex())) {
                pubchemUnion += 1d - (1d-unsmooth(f.getLeftProbability()))*(1d-unsmooth(f.getRightProbability()));
                pubchemIntersection += unsmooth(f.getLeftProbability())*unsmooth(f.getRightProbability());
            }
        }
        cosine /= Math.sqrt(cosineLeft*cosineRight);
        final double tanimoto = intersection/union;
        final double tanimotoRounded = roundedIntersection/roundedUnion;
        final double pubchemTanimoto = pubchemIntersection/pubchemUnion;
        return correctTanimoto(cosine, tanimoto, tanimotoRounded, pubchemTanimoto);
    }

    private static void tanimotoEstimateVERBOSE(ProbabilityFingerprint left, ProbabilityFingerprint right, TIntHashSet maccsAndPubchemIndizes) {
        double union=0d, intersection=0d, roundedUnion=0d, roundedIntersection=0d, pubchemUnion=0d,pubchemIntersection=0d, cosineLeft=0d, cosineRight=0d, cosine=0d;
        for (FPIter2 f : left.foreachPair(right)) {
            union += 1d - (1d-unsmooth(f.getLeftProbability()))*(1d-unsmooth(f.getRightProbability()));
            intersection += unsmooth(f.getLeftProbability())*unsmooth(f.getRightProbability());
            if (f.isLeftSet() || f.isRightSet()) roundedUnion += 1;
            if (f.isLeftSet() && f.isRightSet()) roundedIntersection += 1;
            cosineLeft += f.getLeftProbability()*f.getLeftProbability();
            cosineRight += f.getRightProbability()*f.getRightProbability();
            cosine += f.getLeftProbability()*f.getRightProbability();
            if (maccsAndPubchemIndizes.contains(f.getIndex())) {
                pubchemUnion += 1d - (1d-unsmooth(f.getLeftProbability()))*(1d-unsmooth(f.getRightProbability()));
                pubchemIntersection += unsmooth(f.getLeftProbability())*unsmooth(f.getRightProbability());
            }
        }
        cosine /= Math.sqrt(cosineLeft*cosineRight);
        final double tanimoto = intersection/union;
        final double tanimotoRounded = roundedIntersection/roundedUnion;
        final double pubchemTanimoto = pubchemIntersection/pubchemUnion;
        System.out.printf(
                "tanimoto = %f\nrounded = %f\npubchem = %f\ncosine = %f\ncorrected = %f\n",
                tanimoto,tanimotoRounded,pubchemTanimoto,cosine,correctTanimoto(cosine,tanimoto,tanimotoRounded,pubchemTanimoto)
        );
    }

    private static final double[] SCALES=new double[]{
            0.6061987, 3.2440293, 4.502565 , 1.2938799
    }, SHIFTS = new double[]{
            -2.6636639,  4.6013083, -1.8981445,  5.444874
    }, WEIGHTS = new double[]{
            0.6098236 , 0.1809306 , 0.06811084, 0.14113493
    };
    private static double correctTanimoto(double cosine, double tanimoto, double tanimotoRounded, double pubchemTanimoto) {
        double cosineLogit = cosine/smooth(1d-cosine);
        double tanimotoLogit = tanimoto/smooth(1d-tanimoto);
        double tanimotoRoundedLogit = tanimotoRounded/smooth(1d-tanimotoRounded);
        double pubchemTanimotoLogit = pubchemTanimoto/smooth(1d-pubchemTanimoto);
        cosineLogit = logistic((cosineLogit*(1+SCALES[0])) + SHIFTS[0]);
        tanimotoLogit = logistic((tanimotoLogit*(1+SCALES[1])) + SHIFTS[1]);
        tanimotoRoundedLogit = logistic((tanimotoRoundedLogit*(1+SCALES[2])) + SHIFTS[2]);
        pubchemTanimotoLogit = logistic((pubchemTanimotoLogit*(1+SCALES[3])) + SHIFTS[3]);
        return cosineLogit*WEIGHTS[0] + tanimotoLogit*WEIGHTS[1] + tanimotoRoundedLogit*WEIGHTS[2] + pubchemTanimotoLogit*WEIGHTS[3];
    }

    private static double logistic(double x) {
        return 1d / (1+Math.exp(-x));
    }

    private static double smooth(double v) {
        return Math.max(v, 1e-4);
    }

    private static double unsmooth(double val, double clip) {
        if (val > (1-clip)) return 1d;
        if (val < clip) return 0d;
        return val;
    }
    private static double unsmooth(double val) {
        return unsmooth(val, 0.01);
    }*/
}
