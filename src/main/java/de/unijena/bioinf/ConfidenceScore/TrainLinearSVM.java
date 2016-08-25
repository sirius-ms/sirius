package de.unijena.bioinf.ConfidenceScore;

import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ConfidenceScore.svm.LinearSVMPredictor;
import de.unijena.bioinf.ConfidenceScore.svm.SVMInterface;
import de.unijena.bioinf.fingerid.OptimizationStrategy;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleObjectHashMap;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by Marcus Ludwig on 07.03.16.
 */
public class TrainLinearSVM  implements Closeable {
    private SVMInterface svmInterface;
    private static final boolean DEBUG = false;

    @Override
    public void close() throws IOException {
        executorService.shutdown();
    }

    public static class Model implements Comparable<Model> {
        private static Comparator<PredictionPerformance> comp = new OptimizationStrategy.ByFScore().getComparator();
        private final PredictionPerformance performance;
        private final double c;
        private final int featureSize;

        public Model(PredictionPerformance performance, double c, int featureSize) {
            this.performance = performance;
            this.c = c;
            this.featureSize = featureSize;
        }

        @Override
        public int compareTo(Model o) {
            return comp.compare(performance, o.performance);
        }

        @Override
        public String toString() {
            return "linear svm, c = " + c;
        }
    }

    public static class Compound {
        private final String identifier;
        private final byte classification;
        private final double[] features;
        private List<SVMInterface.svm_node> nodes;

        public Compound(String identifier, byte classification, double[] features) {
            this.identifier = identifier;
            this.classification = classification;
            this.features = features;
        }
    }


    protected final List<Compound> compounds;
    protected final int featureSize;

    protected final static int FOLDS_DEFAULT = 5;
    protected final static int[] C_EXP_Range_DEFAULT  = new int[]{-5,5};

    protected final int FOLDS;
    protected final int[] C_EXP_Range;
    protected final double[] WEIGHT;
    protected final int[] WEIGHT_LABEL = new int[]{1,-1};

    protected ExecutorService executorService;


    public TrainLinearSVM(ExecutorService executorService, List<Compound> compounds, SVMInterface svmInterface, int folds, int[] c_exp_range){
        this.executorService = executorService;
        this.compounds = compounds;
        this.svmInterface = svmInterface;
        this.featureSize = this.compounds.get(0).features.length;
        this.FOLDS = folds;
        this.C_EXP_Range = c_exp_range;
        createSVM_nodes(this.compounds);
        WEIGHT = computeUnbalancedWeight(compounds);
    }

    public TrainLinearSVM(ExecutorService executorService, List<Compound> compounds, SVMInterface svmInterface){
        this(executorService, compounds, svmInterface, FOLDS_DEFAULT, C_EXP_Range_DEFAULT);
    }

    public TrainLinearSVM(List<Compound> compounds, SVMInterface svmInterface){
        this(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()), compounds, svmInterface);
    }


    public Predictor trainWithCrossvalidation() throws InterruptedException {
        final List<Future<Model>> fmodels = new ArrayList<Future<Model>>();

        final List<Compound>[] train = new List[FOLDS];
        final List<Compound>[] eval = new List[FOLDS];

        pickupTrainAndEvalStructureDependent(compounds, train, eval, false);

        final SVMInterface.svm_problem[]  problems = new SVMInterface.svm_problem[FOLDS];
        for (int i = 0; i < train.length; i++) {
            problems[i] = defineProblem(train[i]);
        }


        for (int i = 0; i < FOLDS; i++) {
            if (DEBUG) System.out.println("Fold "+(i+1)+" of "+FOLDS);
            final List<Compound> currentEval = eval[i];

            final SVMInterface.svm_problem problem = problems[i];

            for (int e = C_EXP_Range[0]; e <= C_EXP_Range[1]; e++) {
                double c = Math.pow(2, e);
                if (DEBUG) System.out.println("c: "+c);
                final SVMInterface.svm_parameter parameter = defaultParameters();
                parameter.C = c;
                parameter.weight = WEIGHT;
                parameter.weight_label = WEIGHT_LABEL;

//                learnModel. Future..
                fmodels.add(executorService.submit(new Callable<Model>() {
                    @Override
                    public Model call() throws Exception {
                        final PredictionPerformance performance = trainAndEvaluate(problem, parameter, currentEval);
                        final Model model = new Model(performance, parameter.C, featureSize);
                        return model;
                    }
                }));
            }

        }


        final ArrayList<Model> models = new ArrayList<>();
        for (Future<Model> fm : fmodels) {
            try {
                models.add(fm.get());
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        final List<Model> mergedModels = mergeModels(models);
        final Model bestModel = Collections.max(mergedModels);

        if (DEBUG) System.out.println("best model c "+bestModel.c);

        //train on complete dataset;
        final SVMInterface.svm_model svm_model = train(bestModel.c, compounds);

        //ToDo at the moment unnecessary additional computations
        double[] probAB = trainProABForPlatt(bestModel.c, problems, eval);

        return svmInterface.getPredictor(svm_model, probAB[0], probAB[1]);
    }

    public Predictor trainAntiCrossvalidation() throws InterruptedException {
        final List<Future<Model>> fmodels = new ArrayList<Future<Model>>();

        final SVMInterface.svm_problem  problem = defineProblem(compounds);

        final List<Compound> currentEval = compounds;

        for (int e = C_EXP_Range[0]; e <= C_EXP_Range[1]; e++) {
            double c = Math.pow(2, e);
            if (DEBUG) System.out.println("c: "+c);
            final SVMInterface.svm_parameter parameter = defaultParameters();
            parameter.C = c;
            parameter.weight = WEIGHT;
            parameter.weight_label = WEIGHT_LABEL;

//                learnModel. Future..
            fmodels.add(executorService.submit(new Callable<Model>() {
                @Override
                public Model call() throws Exception {
                    final PredictionPerformance performance = trainAndEvaluate(problem, parameter, currentEval);
                    final Model model = new Model(performance, parameter.C, featureSize);
                    return model;
                }
            }));
        }




        final ArrayList<Model> models = new ArrayList<>();
        for (Future<Model> fm : fmodels) {
            try {
                models.add(fm.get());
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        final Model bestModel = Collections.max(models);

        if (DEBUG) System.out.println("best model c "+bestModel.c);

        //train on complete dataset;
        final SVMInterface.svm_model svm_model = train(bestModel.c, compounds);

        //ToDo at the moment unnecessary additional computations
        double[] probAB = trainProABForPlatt(bestModel.c, new SVMInterface.svm_problem[]{problem}, new List[]{compounds});

        return svmInterface.getPredictor(svm_model, probAB[0], probAB[1]);
    }

    public Predictor trainWithCrossvalidationOptimizeGammaAndDegree(double lowest_gamma, double highest_gamma, int lowest_degree, int highest_degree) throws InterruptedException {
        final List<Future<Model>> fmodels = new ArrayList<Future<Model>>();

        final List<Compound>[] train = new List[FOLDS];
        final List<Compound>[] eval = new List[FOLDS];

        pickupTrainAndEvalStructureDependent(compounds, train, eval, false);

        final SVMInterface.svm_problem[]  problems = new SVMInterface.svm_problem[FOLDS];
        for (int i = 0; i < train.length; i++) {
            problems[i] = defineProblem(train[i]);
        }



        for (int i = 0; i < FOLDS; i++) {
            if (DEBUG) System.out.println("Fold "+(i+1)+" of "+FOLDS);
            final List<Compound> currentEval = eval[i];

            final SVMInterface.svm_problem problem = problems[i];


            for (int e = C_EXP_Range[0]; e <= C_EXP_Range[1]; e++) {
                double c = Math.pow(2, e);
                if (DEBUG) System.out.println("c: "+c);

                double gamma = lowest_gamma;
                while (gamma<=highest_gamma){
                    int degree = lowest_degree;
                    while (degree<=highest_degree){

                        final SVMInterface.svm_parameter parameter = defaultParameters();
                        parameter.C = c;
                        parameter.weight = WEIGHT;
                        parameter.weight_label = WEIGHT_LABEL;
                        parameter.degree = degree;
                        parameter.gamma = gamma;

                        // learnModel. Future..
                        fmodels.add(executorService.submit(new Callable<Model>() {
                            @Override
                            public Model call() throws Exception {
                                final PredictionPerformance performance = trainAndEvaluate(problem, parameter, currentEval);
                                final Model model = new Model(performance, parameter.C, featureSize);
                                return model;
                            }
                        }));


                        degree *=2;
                    }

                    gamma *=2;
                }

            }

        }


        final ArrayList<Model> models = new ArrayList<>();
        for (Future<Model> fm : fmodels) {
            try {
                models.add(fm.get());
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        final List<Model> mergedModels = mergeModels(models);
        final Model bestModel = Collections.max(mergedModels);

        if (DEBUG) System.out.println("best model c "+bestModel.c);

        //train on complete dataset;
        final SVMInterface.svm_model svm_model = train(bestModel.c, compounds);

        //ToDo at the moment unnecessary additional computations
        double[] probAB = trainProABForPlatt(bestModel.c, problems, eval);

        return svmInterface.getPredictor(svm_model, probAB[0], probAB[1]);
    }


    private PredictionPerformance trainAndEvaluate(SVMInterface.svm_problem problem, SVMInterface.svm_parameter parameter, List<Compound> currentEval){
        final SVMInterface.svm_model svm_model = svmInterface.svm_train(problem, parameter);
        final PredictionPerformance performance = evaluate(svm_model, currentEval);
        return performance;
    }

    private void pickupTrainAndEvalStructureDependent(List<Compound> compounds, List<Compound>[] trains, List<Compound>[] eval, boolean removeIdentifierDuplicates) {
        for (int k=0; k < FOLDS; ++k) {
            trains[k] = new ArrayList<>();
            eval[k] = new ArrayList<>();
        }

        final int[] foldSizes = new int[FOLDS];
        final int length = compounds.size();
        final int factor = length/FOLDS;
        final int mod = length % FOLDS;

        for (int i = 0; i < foldSizes.length; i++) {
            foldSizes[i] = factor;
            if (i<mod) ++foldSizes[i];
        }

        int[] randomOrder = new int[length];
        int pos = 0;
        for (int i = 0; i < foldSizes.length; i++) {
            int foldSize = foldSizes[i];
            for (int j = 0; j < foldSize; j++) {
                randomOrder[pos++] = i;
            }
        }
        Statistics.shuffle(randomOrder);

        final List<Compound> sortedCompounds = new ArrayList<>(compounds);
        Collections.sort(sortedCompounds, new Comparator<Compound>() {
            @Override
            public int compare(Compound o1, Compound o2) {
                return o1.identifier.compareTo(o2.identifier);
            }
        });

        List<Compound>[] split = new ArrayList[FOLDS];
        for (int i = 0; i < split.length; i++) {
            split[i] = new ArrayList<>();
        }


        int shufflePos = 0;
        int[] unbalancedSize = new int[FOLDS];
//        Iterator<Compound> iterator = compounds.iterator();
        Iterator<Compound> iterator = sortedCompounds.iterator();
        Compound next = iterator.next();
        while (next != null) {
            Compound compound = next;
            int randBucket = randomOrder[shufflePos];
            while (unbalancedSize[randBucket]>0) {
                unbalancedSize[randBucket]--;
                shufflePos++;
                if (shufflePos>=randomOrder.length) break;
                randBucket = randomOrder[shufflePos];
            }
            shufflePos++;
            final String identifier = compound.identifier;
            split[randBucket].add(compound);
            if (!iterator.hasNext()) break;
            while ((iterator.hasNext()) && (next = iterator.next()).identifier.equals(identifier)){
                if (!removeIdentifierDuplicates){
                    unbalancedSize[randBucket]++;
                    split[randBucket].add(next);

                }
            }

        }

        for (int i = 0; i < split.length; i++) {
            List<Compound> compoundList = split[i];
            for (int j = 0; j < split.length; j++) {
                if (i==j) eval[j].addAll(compoundList);
                else trains[j].addAll(compoundList);
            }
        }

    }

    private void pickupTrainAndEval(List<Compound> compounds, List<Compound>[] trains, List<Compound>[] eval) {
        final int N = trains.length;
        final ArrayList<Compound> positives = new ArrayList<>();
        final ArrayList<Compound> negatives = new ArrayList<>();
        for (Compound c : compounds) {
            if (c.classification > 0) positives.add(c);
            else negatives.add(c);
        }
        Collections.shuffle(positives);
        Collections.shuffle(negatives);

        for (int k=0; k < N; ++k) {
            trains[k] = new ArrayList<>();
            eval[k] = new ArrayList<>();
        }
        for (int k=0; k < positives.size(); ++k) {
            final int fold = k % N;
            eval[fold].add(positives.get(k));
            for (int i=0; i < N; ++i) {
                if (i != fold) trains[i].add(positives.get(k));
            }
        }
        for (int k=0; k < negatives.size(); ++k) {
            final int fold = k % N;
            eval[fold].add(negatives.get(k));
            for (int i=0; i < N; ++i) {
                if (i != fold) trains[i].add(negatives.get(k));
            }
        }
    }


    private List<Model> mergeModels(List<Model> models){
        TDoubleObjectHashMap<List<Model>> map = new TDoubleObjectHashMap<>();
        for (Model model : models) {
            if (map.containsKey(model.c)) map.get(model.c).add(model);
            else {
                final List<Model> list = new ArrayList<>();
                list.add(model);
                map.put(model.c, list);
            }
        }

        List<Model> mergedModels = new ArrayList<>();
        for (List<Model> modelList : map.valueCollection()) {
            assert modelList.size()==FOLDS;
            PredictionPerformance performance = new PredictionPerformance(0,0,0,0);
            for (Model model : modelList) {
                performance.merge(model.performance);
            }
            performance.calc();
            Model model = new Model(performance, modelList.get(0).c, featureSize);
            mergedModels.add(model);
        }
        return mergedModels;
    }

    private double[] computeUnbalancedWeight(List<Compound> data){
        double[] weight = new double[]{0,0};
        for (Compound compound : data) {
            if (compound.classification > 0) ++weight[0];
            else ++weight[1];
        }
        return new double[]{data.size()/(2*weight[0]), data.size()/(2*weight[1])};
    }

    private double[] trainProABForPlatt(double c, final SVMInterface.svm_problem[] problems, List<Compound>[] evals) throws InterruptedException {
        final SVMInterface.svm_parameter parameter = defaultParameters();
        parameter.C = c;
        parameter.weight = WEIGHT;
        parameter.weight_label = WEIGHT_LABEL;

        List<Future<SVMInterface.svm_model>> fmodels = new ArrayList<>();

        TDoubleArrayList scores = new TDoubleArrayList();
        TDoubleArrayList labels = new TDoubleArrayList();
        for (int fold=0; fold < problems.length; ++fold) {
            final int current = fold;
            fmodels.add(executorService.submit(new Callable<SVMInterface.svm_model>() {
                @Override
                public SVMInterface.svm_model call() throws Exception {
                    final SVMInterface.svm_model model = svmInterface.svm_train(problems[current], parameter);
                    return model;
                }
            }));
        }

        final ArrayList<SVMInterface.svm_model> models = new ArrayList<>();
        for (int fold = 0; fold < fmodels.size(); fold++) {
            Future<SVMInterface.svm_model> fm  = fmodels.get(fold);
            try {
                final SVMInterface.svm_model model = fm.get();
                for (Compound compound : evals[fold]) {
                    final double score = svmInterface.svm_predict(model, compound.nodes);
                    scores.add(score);
                    labels.add(compound.classification);
                }
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }

        }


        double[] probAB = new double[2];
        sigmoid_train(scores.size(), scores.toArray(), labels.toArray(), probAB);
        return probAB;
    }


    private SVMInterface.svm_model train(double c, List<Compound> train){
        final SVMInterface.svm_problem problem = defineProblem(train);
        final SVMInterface.svm_parameter parameter = defaultParameters();
        parameter.C = c;
//        parameter.weight = computeUnbalancedWeight(train);
        parameter.weight = WEIGHT;
        parameter.weight_label = WEIGHT_LABEL;

        final SVMInterface.svm_model model = svmInterface.svm_train(problem, parameter);
        if (DEBUG){
            predict(model, problem.getX(), train);
        }
        return model;
    }

    private void predict(SVMInterface.svm_model model, List<List<SVMInterface.svm_node>> nodes, List<Compound> compounds){
        int tp = 0, fp = 0, tn = 0, fn = 0;
        for (int i = 0; i < nodes.size(); i++) {
            List<SVMInterface.svm_node> currentNodes = nodes.get(i);
            Compound compound = compounds.get(i);
            boolean isCorrect = compound.classification>0;
            boolean predicted = svmInterface.svm_predict(model, currentNodes)>0;
            if (predicted){
                if (isCorrect) tp++;
                else fp++;
            } else {
                if (isCorrect) fn++;
                else tn++;
            }

        }


        Predictor predictor = svmInterface.getPredictor(model, Double.NaN, Double.NaN);
        int tp2 = 0, fp2 = 0, tn2 = 0, fn2 = 0;
        for (int i = 0; i < nodes.size(); i++) {
            List<SVMInterface.svm_node> currentNodes = nodes.get(i);
            Compound compound = compounds.get(i);


            double[] features = new double[featureSize];
            for (int j = 0; j < currentNodes.size(); j++) {
                double v = currentNodes.get(j).getValue();
                int index = currentNodes.get(j).getIndex();
                features[index-1] = v;
            }

            boolean isCorrect = compound.classification>0;
            boolean predicted = predictor.predict(features);
            if (predicted){
                if (isCorrect) tp2++;
                else fp2++;
            } else {
                if (isCorrect) fn2++;
                else tn2++;
            }
        }

        assert tp==tp2;
        assert fp==fp2;
        assert tn==tn2;
        assert fn==fn2;
    }

    private Model trainAndEvaluateCrossFolds(SVMInterface.svm_problem[] problems, SVMInterface.svm_parameter param, List<Compound>[] evals) {
        final PredictionPerformance performance = new PredictionPerformance();
        for (int fold=0; fold < problems.length; ++fold) {
            final SVMInterface.svm_model model = svmInterface.svm_train(problems[fold], param);
            final PredictionPerformance performance2 = evaluate(model, evals[fold]);
            performance.merge(performance2);
        }
        performance.calc();
        return new Model(performance, param.C, featureSize);
    }


    // Platt's binary SVM Probablistic Output: an improvement from Lin et al.
    private static void sigmoid_train(int l, double[] dec_values, double[] labels,
                                      double[] probAB)
    {
        double A, B;
        double prior1=0, prior0 = 0;
        int i;

        for (i=0;i<l;i++)
            if (labels[i] > 0) prior1+=1;
            else prior0+=1;

        int max_iter=100;	// Maximal number of iterations
        double min_step=1e-10;	// Minimal step taken in line search
        double sigma=1e-12;	// For numerically strict PD of Hessian
        double eps=1e-5;
        double hiTarget=(prior1+1.0)/(prior1+2.0);
        double loTarget=1/(prior0+2.0);
        double[] t= new double[l];
        double fApB,p,q,h11,h22,h21,g1,g2,det,dA,dB,gd,stepsize;
        double newA,newB,newf,d1,d2;
        int iter;

        // Initial Point and Initial Fun Value
        A=0.0; B=Math.log((prior0+1.0)/(prior1+1.0));
        double fval = 0.0;

        for (i=0;i<l;i++)
        {
            if (labels[i]>0) t[i]=hiTarget;
            else t[i]=loTarget;
            fApB = dec_values[i]*A+B;
            if (fApB>=0)
                fval += t[i]*fApB + Math.log(1+Math.exp(-fApB));
            else
                fval += (t[i] - 1)*fApB +Math.log(1+Math.exp(fApB));
        }
        for (iter=0;iter<max_iter;iter++)
        {
            // Update Gradient and Hessian (use H' = H + sigma I)
            h11=sigma; // numerically ensures strict PD
            h22=sigma;
            h21=0.0;g1=0.0;g2=0.0;
            for (i=0;i<l;i++)
            {
                fApB = dec_values[i]*A+B;
                if (fApB >= 0)
                {
                    p=Math.exp(-fApB)/(1.0+Math.exp(-fApB));
                    q=1.0/(1.0+Math.exp(-fApB));
                }
                else
                {
                    p=1.0/(1.0+Math.exp(fApB));
                    q=Math.exp(fApB)/(1.0+Math.exp(fApB));
                }
                d2=p*q;
                h11+=dec_values[i]*dec_values[i]*d2;
                h22+=d2;
                h21+=dec_values[i]*d2;
                d1=t[i]-p;
                g1+=dec_values[i]*d1;
                g2+=d1;
            }

            // Stopping Criteria
            if (Math.abs(g1)<eps && Math.abs(g2)<eps)
                break;

            // Finding Newton direction: -inv(H') * g
            det=h11*h22-h21*h21;
            dA=-(h22*g1 - h21 * g2) / det;
            dB=-(-h21*g1+ h11 * g2) / det;
            gd=g1*dA+g2*dB;


            stepsize = 1;		// Line Search
            while (stepsize >= min_step)
            {
                newA = A + stepsize * dA;
                newB = B + stepsize * dB;

                // New function value
                newf = 0.0;
                for (i=0;i<l;i++)
                {
                    fApB = dec_values[i]*newA+newB;
                    if (fApB >= 0)
                        newf += t[i]*fApB + Math.log(1+Math.exp(-fApB));
                    else
                        newf += (t[i] - 1)*fApB +Math.log(1+Math.exp(fApB));
                }
                // Check sufficient decrease
                if (newf<fval+0.0001*stepsize*gd)
                {
                    A=newA;B=newB;fval=newf;
                    break;
                }
                else
                    stepsize = stepsize / 2.0;
            }

            if (stepsize < min_step)
            {
                System.err.println("Line search fails in two-class probability estimates\n");
                break;
            }
        }

        if (iter>=max_iter)
            System.err.println("Reaching maximal iterations in two-class probability estimates\n");
        probAB[0]=A;probAB[1]=B;
    }



    private PredictionPerformance evaluate(SVMInterface.svm_model model, List<Compound> compounds) {
        int tp=0,fp=0,tn=0,fn=0;
        for (int k=0; k < compounds.size(); ++k) {
            final Compound c = compounds.get(k);
            final boolean prediction = svmPredict(model, c);
            if (c.classification > 0) {
                if (prediction) {
                    ++tp;
                } else {
                    ++fp;
                }
            } else {
                if (prediction) {
                    ++fn;
                } else {
                    ++tn;
                }
            }
        }
        return new PredictionPerformance(tp, fp, tn, fn);
    }

    private boolean svmPredict(SVMInterface.svm_model model, Compound c) {
        return svmInterface.svm_predict(model,c.nodes) > 0;
    }


    private void createSVM_nodes(Iterable<Compound> compounds){
        for (Compound compound : compounds) {
            final double[] fingerprint = compound.features;
            List<SVMInterface.svm_node> nodes = new ArrayList<>();

            for (int i = 0; i < fingerprint.length; i++) {
                final double value = fingerprint[i];
//                if (value == 0 ) continue;
//                final SVMInterface.svm_node node = svmInterface.createSVM_Node(i+1, compound.features[i]);
                final SVMInterface.svm_node node = svmInterface.createSVM_Node(i+1, value);
                nodes.add(node);
            }
            compound.nodes = nodes;
        }

    }


    private SVMInterface.svm_problem defineProblem(List<Compound> compounds){
        final SVMInterface.svm_problem problem = svmInterface.createSVM_Problem();
        problem.setL(compounds.size());
        List<List<SVMInterface.svm_node>> x = new ArrayList<>();
        problem.setY(new double[problem.getL()]);
        for (int k=0; k < compounds.size(); ++k) {
            final Compound c = compounds.get(k);
            x.add(c.nodes);
            problem.getY()[k] = c.classification;
        }

        problem.setX(x);
        return problem;
    }

    private SVMInterface.svm_parameter defaultParameters() {
        SVMInterface.svm_parameter param = new SVMInterface.svm_parameter();
        // default values
//        param.svm_type = SVMInterface.svm_parameter.C_SVC;
//        param.kernel_type = SVMInterface.svm_parameter.LINEAR;
//        param.nu = 0.5;
        param.cache_size = 200;
        param.C = 1;
        param.eps = 1e-3;
//        param.p = 0.1;
        param.shrinking = 1;
        param.probability = 0;
        param.weight_label = new int[]{1,-1};
        param.weight = new double[]{1d,1d};
        param.nr_weight = param.weight.length;
        return param;
    }


}
