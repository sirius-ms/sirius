package de.unijena.bioinf.ms.frontend.subtools.similarity;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.Tanimoto;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.ftalign.StandardScoring;
import de.unijena.bioinf.ftalign.analyse.Pearson;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.config.AddConfigsJob;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.ProjectSpaceIO;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.treealign.multijoin.DPMultiJoin;
import de.unijena.bionf.spectral_alignment.CosineQuerySpectrum;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
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

    protected final PreprocessingJob<ProjectSpaceManager> ppj;

    public SimilarityMatrixWorkflow(PreprocessingJob<ProjectSpaceManager> ppj, SimilarityMatrixOptions options, ParameterConfig config) {
        this.ppj = ppj;
        this.options = options;
        this.config = config;
    }

    @Override
    public void run() {
        List<Instance> xs = new ArrayList<>();

        try {
            ps = SiriusJobs.getGlobalJobManager().submitJob(ppj).awaitResult();
            ps.forEach(xs::add);
            if (options.useCosine)
                cosine(xs);
            //filter all instances without a single fragTree
            xs = xs.stream().filter(i -> !i.loadCompoundContainer().getResults().isEmpty())
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
        } catch (ExecutionException e) {
            LoggerFactory.getLogger(this.getClass()).error("Error when parsing project space");
        }
    }

    private void tanimoto(List<Instance> xs) {
        final JobManager J = SiriusJobs.getGlobalJobManager();
        xs.removeIf(x -> x.loadTopFormulaResult(rankSores, FingerprintResult.class).filter(y -> y.hasAnnotation(FingerprintResult.class)).isEmpty());

        if (xs.isEmpty()){
            LoggerFactory.getLogger(getClass()).warn("No Compounds with predicted Fingerprints found! You might want to run CSI:FingerID first. Skipping tanimoto computation!");
            return;
        }


        final ProbabilityFingerprint[] fps = xs.stream().map(f ->
                f.loadTopFormulaResult(rankSores, FingerprintResult.class)
                        .map(it -> it.getAnnotationOrThrow(FingerprintResult.class).fingerprint).orElseThrow()).toArray(ProbabilityFingerprint[]::new);

        final double[][] M = new double[xs.size()][xs.size()];
        J.submitJob(MatrixUtils.parallelizeSymmetricMatrixComputation(M, (i, j) -> Tanimoto.fastTanimoto(fps[i], fps[j]))).takeResult();
        MatrixUtils.normalize(M);
        writeMatrix("tanimoto", M, xs.stream().map(y -> y.getID().getCompoundName()).toArray(String[]::new));
    }

    private void writeMatrix(String name, double[][] M, String[] header) {
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
                            bw.write(String.valueOf(M[i][j]));
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
                SiriusProjectSpace compoundContainerIds = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).openExistingProjectSpace(options.useFtblast.toPath());
                for (CompoundContainerId id : compoundContainerIds) {
                    List<? extends SScored<FormulaResult, ? extends FormulaScore>> list = compoundContainerIds.getFormulaResultsOrderedBy(id, id.getRankingScoreTypes());
                    if (list.size()>0) {
                        compoundContainerIds.getFormulaResult(list.get(0).getCandidate().getId(),FTree.class).getAnnotation(FTree.class).ifPresent(libTrees::add);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Cannot open project space at '" + options.useFtblast + "'");
            }
        } else {
            final HashMap<String, MolecularFormula> cache = new HashMap<>();
            // just take all json files from the directory
            for (File f : options.useFtblast.listFiles()) {
                if (f.getName().endsWith(".json")) {
                   try (BufferedReader br = FileUtils.getReader(f)){
                       libTrees.add(new FTJsonReader(cache).parse(br, f.toURI().toURL()));
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
        writeMatrix("ftblast", M, xs.stream().map(x->x.getID().getCompoundName()).toArray(String[]::new));
    }

    private void align(List<Instance> xs, FTree[] trees) {
        final JobManager J = SiriusJobs.getGlobalJobManager();
        final double[][] M = new double[trees.length][trees.length];
        J.submitJob(MatrixUtils.parallelizeSymmetricMatrixComputation(M, (i,j)->
            new DPMultiJoin<>(new StandardScoring(true), 2, trees[i].getRoot(),trees[j].getRoot(),trees[i].treeAdapter()).compute())).takeResult();
        final double[] norm = MatrixUtils.selectDiagonal(M);
        for (int i=0; i < M.length; ++i) {
            for (int j=0; j < M.length; ++j) {
                M[i][j] = M[i][j] / Math.sqrt(Math.min(norm[i],norm[j]));
            }
        }
        writeMatrix("ftalign", M, xs.stream().map(x->x.getID().getCompoundName()).toArray(String[]::new));

    }

    private void cosine(List<Instance> xs) {
        final JobManager J = SiriusJobs.getGlobalJobManager();

        List<Pair<Instance, CosineQuerySpectrum>> pairs = J.submitJobsInBatches(xs.stream().map(this::getSpectrum).collect(Collectors.toList())).stream().map(JJob::getResult).filter(Objects::nonNull).filter(c -> c.getRight().getSelfSimilarity() > 0 && c.getRight().getSelfSimilarityLosses() > 0).collect(Collectors.toList());
        xs = pairs.stream().map(Pair::getLeft).collect(Collectors.toList());
        final CosineQuerySpectrum[] cosineQuerySpectrums = pairs.stream().map(Pair::getRight).toArray(CosineQuerySpectrum[]::new);
        final CosineQueryUtils cosineQueryUtils = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(config.createInstanceWithDefaults(MS2MassDeviation.class).allowedMassDeviation.multiply(2)));
        final double[][] M = new double[xs.size()][xs.size()];
        J.submitJob(MatrixUtils.parallelizeSymmetricMatrixComputation(M, (i,j)-> cosineQueryUtils.cosineProductWithLosses(cosineQuerySpectrums[i],cosineQuerySpectrums[j]).similarity)).takeResult();
        writeMatrix("cosine", M, xs.stream().map(x->x.getID().getCompoundName()).toArray(String[]::new));
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
}
