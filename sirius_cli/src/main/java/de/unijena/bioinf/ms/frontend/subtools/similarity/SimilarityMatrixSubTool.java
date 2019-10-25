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
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.ftalign.StandardScoring;
import de.unijena.bioinf.ftalign.analyse.Pearson;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.io.projectspace.Instance;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.ProjectSpaceIO;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.treealign.multijoin.DPMultiJoin;
import de.unijena.bionf.spectral_alignment.CosineQuerySpectrum;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class SimilarityMatrixSubTool implements Workflow {

    protected final SimilarityMatrixOptions options;
    protected final ProjectSpaceManager ps;
    protected final ParameterConfig config;

    public SimilarityMatrixSubTool(ProjectSpaceManager ps, SimilarityMatrixOptions options, ParameterConfig config) {
        this.ps = ps;
        this.options = options;
        this.config = config;
    }

    @Override
    public void run() {
        final ArrayList<Instance> xs = new ArrayList<>();
        ps.forEach(xs::add);
        if (options.useCosine)
            cosine(xs);
        if (options.useAlignment)
            align(xs);
        if (options.useFtblast!=null)
            ftblast(xs);
        if (options.useTanimoto)
            tanimoto(xs);
    }

    private void tanimoto(List<Instance> xs) {
        final JobManager J = SiriusJobs.getGlobalJobManager();

        final Instance[] ys = xs.stream().filter(x->x.loadTopFormulaResult(FingerprintResult.class).filter(y->y.getAnnotation(FingerprintResult.class).isPresent()).isPresent()).toArray(Instance[]::new);
        final ProbabilityFingerprint[] fps = Arrays.stream(ys).map(f->f.loadTopFormulaResult().get().getAnnotation(FingerprintResult.class).get().fingerprint).toArray(ProbabilityFingerprint[]::new);
        final double[][] M = new double[ys.length][ys.length];

        J.submitJob(MatrixUtils.parallelizeSymmetricMatrixComputation(M, (i,j)-> Tanimoto.fastTanimoto(fps[i],fps[j]))).takeResult();
        MatrixUtils.normalize(M);
        writeMatrix("tanimoto", M, Arrays.stream(ys).map(y->y.getID().getCompoundName()).toArray(String[]::new));
    }

    private void writeMatrix(String name, double[][] M, String[] header) {
        final File file = new File(options.outputDirectory, name + (options.numpy ? ".txt" : ".tsv"));
        try (BufferedWriter bw = FileUtils.getWriter(file)) {
            if (options.numpy) {
                bw.write('#');
                bw.write(header[0]);
                for (int i=1; i < header.length; ++i) {
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
                for (int i=0; i < M.length; ++i) {
                    bw.write(header[i]);
                    for (int j=0; j < M.length; ++j) {
                        bw.write('\t');
                        bw.write(String.valueOf(M[i][j]));
                    }
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(SimilarityMatrixSubTool.class).error(file.getAbsolutePath() + " cannot be written due to: " + e.getMessage(),e);
            System.err.println("Cannot write file '"+file+"' due to IO error: " + e.getMessage());
        }
    }

    private void ftblast(List<Instance> xs) {
        final JobManager Jobs= SiriusJobs.getGlobalJobManager();
        final List<FTree> libTrees = new ArrayList<>();
        FTree[] lib;
        if (ProjectSpaceIO.isExistingProjectspaceDirectory(options.useFtblast)) {
            try {
                SiriusProjectSpace compoundContainerIds = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).openExistingProjectSpace(options.useFtblast);
                for (CompoundContainerId id : compoundContainerIds) {
                    List<? extends SScored<FormulaResult, ? extends FormulaScore>> list = compoundContainerIds.getFormulaResultsOrderedBy(id, id.getRankingScoreType().get());
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
                       libTrees.add(new FTJsonReader(cache).parse(br,f.toURI().toURL()));
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
                }
            }
        }

        System.out.println("Library consists of " + libTrees.size() + " fragmentation libTrees");

        xs.removeIf(x-> x.loadTopFormulaResult(FTree.class).filter(y->y.getAnnotation(FTree.class).isPresent()).isEmpty());
        FTree[] trees = xs.stream().map(x->x.loadTopFormulaResult(FTree.class).get().getAnnotation(FTree.class).get()).toArray(FTree[]::new);

        for (FTree tree : trees) libTrees.add(tree);

        final double[][] M = new double[trees.length][trees.length];
        final double[][] C = new double[trees.length][libTrees.size()];
        final StandardScoring standardScoring = new StandardScoring(true);
        final ArrayList<BasicJJob<Double>> jobs = new ArrayList<>();
        for (int i=0; i < trees.length; ++i) {
            final int I = i;
            final double selfScore = standardScoring.selfAlignScore(trees[i].getRoot());
            for (int j=0; j < libTrees.size(); ++j) {
                final int J = j;
                jobs.add(Jobs.submitJob(new BasicJJob<Double>() {
                    @Override
                    protected Double compute() throws Exception {
                        final double score = new DPMultiJoin(standardScoring, 2, trees[I].getRoot(),libTrees.get(J).getRoot(),trees[I].treeAdapter()).compute();
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

    private void align(List<Instance> xs) {
        xs.removeIf(x-> x.loadTopFormulaResult(FTree.class).filter(y->y.getAnnotation(FTree.class).isPresent()).isEmpty());
        FTree[] trees = xs.stream().map(x->x.loadTopFormulaResult(FTree.class).get().getAnnotation(FTree.class).get()).toArray(FTree[]::new);

        final JobManager J = SiriusJobs.getGlobalJobManager();
        final double[][] M = new double[trees.length][trees.length];
        J.submitJob(MatrixUtils.parallelizeSymmetricMatrixComputation(M, (i,j)->
            new DPMultiJoin(new StandardScoring(true), 2, trees[i].getRoot(),trees[j].getRoot(),trees[i].treeAdapter()).compute())).takeResult();
        final double[] norm = MatrixUtils.selectDiagonal(M);
        for (int i=0; i < M.length; ++i) {
            for (int j=0; j < M.length; ++j) {
                M[i][j] = M[i][j] / Math.sqrt(Math.min(norm[i],norm[j]));
            }
        }
        writeMatrix("ftalign", M, xs.stream().map(x->x.getID().getCompoundName()).toArray(String[]::new));

    }

    private void cosine(List<Instance> xs) {
        final double[][] M = new double[xs.size()][xs.size()];
        final JobManager J = SiriusJobs.getGlobalJobManager();
        CosineQuerySpectrum[] cosineQuerySpectrums = J.submitJobsInBatches(xs.stream().map(this::getSpectrum).collect(Collectors.toList())).stream().map(JJob::takeResult).toArray(CosineQuerySpectrum[]::new);
        final CosineQueryUtils cosineQueryUtils = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(config.createInstanceWithDefaults(MS2MassDeviation.class).allowedMassDeviation.multiply(2)));
        J.submitJob(MatrixUtils.parallelizeSymmetricMatrixComputation(M, (i,j)-> cosineQueryUtils.cosineProductWithLosses(cosineQuerySpectrums[i],cosineQuerySpectrums[j]).similarity)).takeResult();
        writeMatrix("cosine", M, xs.stream().map(x->x.getID().getCompoundName()).toArray(String[]::new));
    }

    private BasicJJob<CosineQuerySpectrum> getSpectrum(Instance i) {
        final Ms2Experiment exp = i.getExperiment();
        final Sirius sirius = ApplicationCore.SIRIUS_PROVIDER.sirius(config.getConfigValue("AlgorithmProfile"));
        final CosineQueryUtils cosineQueryUtils = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(config.createInstanceWithDefaults(MS2MassDeviation.class).allowedMassDeviation.multiply(2)));
        return new BasicMasterJJob<CosineQuerySpectrum>(JJob.JobType.CPU) {
            @Override
            protected CosineQuerySpectrum compute() throws Exception {
                ProcessedInput processedInput = sirius.preprocessForMs2Analysis(exp);
                return cosineQueryUtils.createQueryWithIntensityTransformation(Spectrums.from(processedInput.getMergedPeaks()), processedInput.getExperimentInformation().getIonMass(), true);
            }
        };
    }
}
