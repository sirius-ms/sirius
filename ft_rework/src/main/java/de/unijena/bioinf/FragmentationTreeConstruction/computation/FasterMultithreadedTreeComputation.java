package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import com.google.common.collect.*;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.ftreeheuristics.solver.CriticalPathSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.model.DecompositionList;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.*;
import java.util.concurrent.*;

/**
 * - compute fragmentation graphs in parallel
 * - compute heuristics in parallel
 * - use heuristics to estimate subset of molecular formulas to compute exactly
 * - (optional) compute trees in parallel if Gurobi is used
 */


public class FasterMultithreadedTreeComputation {

    /*
    I recommend to use queues whenever possible to schedule the jobs.
    Blocking Queues allow to limit the number of instances that can be enqueued at the same
    time (and therefore, avoid memory overflows)
     */
    private final BlockingQueue<Future<FGraph>> graphsToCompute;
    private final BlockingQueue<Future<FTree>> treesToCompute;

    /*
    You can use TreeMaps to store your heuristical or exact computed trees
     */
    private final TreeMap<MolecularFormula, FTree> map;
    private final Multimap<Double, MolecularFormula> multimap = Multimaps.synchronizedListMultimap(
            ArrayListMultimap.<Double, MolecularFormula>create());


    /*
    The underlying framework
     */
    private final FragmentationPatternAnalysis analyzer;

    /*
    The actual input data
     */
    private ProcessedInput input;
    private HashMap<MolecularFormula, Scored<MolecularFormula>> formulasToConsider;
    private MolecularFormula poison_pill = MolecularFormula.emptyFormula();
    private FGraph poison_pill_FGraph = new FGraph();
    private FTree poison_pill_FTree = new FTree(poison_pill);
    private Queue<Future<Response>> futures = new ConcurrentLinkedQueue();
//    private final MolecularFormula POISON_PILL = new MolecularFormula();

    public FasterMultithreadedTreeComputation(FragmentationPatternAnalysis analyzer) {
        this.graphsToCompute = new ArrayBlockingQueue<>(100);
        this.treesToCompute = new ArrayBlockingQueue<>(20); // TODO change back to 10
        this.map = new TreeMap<>();
        this.analyzer = analyzer;

    }

    public void setInput(ProcessedInput input) {
        setInput(input, null);
    }

    public void setInput(ProcessedInput input, HashSet<MolecularFormula> formulas) {
        this.input = input;
        this.formulasToConsider = new HashMap<>();
        for (Scored<MolecularFormula> formula : input.getPeakAnnotationOrThrow(DecompositionList.class).get(input.getParentPeak()).getDecompositions()) {
            if (formulas == null || formulas.contains(formula.getCandidate()))
                formulasToConsider.put(formula.getCandidate(), formula);
        }
    }

    /**
     * start multi-threaded computation
     */
    private class CallToRemoteServiceFormula implements Callable<FGraph> {
        private final MolecularFormula formula;

        public CallToRemoteServiceFormula(MolecularFormula formula) {
            this.formula = formula;
        }

        @Override
        public FGraph call() throws Exception {
            if (formula == poison_pill) {
                return poison_pill_FGraph;
            }
            return computeGraph(formula);
        }

    }

    //Consumer Class in Java
    private class ConsumerTrees implements Runnable {
        @Override
        public void run() {
//            Thread.currentThread().setName("consumerTrees");
            while (true) {
                try {
                    final Future<FTree> heuTree = treesToCompute.take(); //waits until element available
                    FTree test = heuTree.get();
                    if (heuTree.get() == poison_pill_FTree) {
                        //damit scoreMap vollständig, vielleicht nicht mehr nötig
                        while (formulasToConsider.keySet().size() != multimap.size()) {
//                            System.out.println("sleep");
                            Thread.sleep(5);
                            Thread.currentThread().join();
                        }

                        break;
                    } else {
//                        System.out.println("h");
//                        computeTreeHeuristically(computeGraph(test.getRoot().getFormula()));
                        multimap.put(getScore(test), test.getRoot().getFormula());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.out.println("Warning");
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    System.out.println("Warning");
                }
            }
        }


    }

    private class Response {
        public int index;
        public double result;
        public double prediction;
    }

    public class Output extends Response {

        public float cut;
    }

    private class ConsumeGraphs implements Runnable{
        public void run(){
//            System.out.println("start heu Tree");
            int processors = Runtime.getRuntime().availableProcessors();
            ExecutorService service = Executors.newFixedThreadPool(processors);
            while (true) {
                try {

                    final Future<FGraph> graph = graphsToCompute.take();
                    // check if job is done (gets poison_pill)
                    if (graph.get() == poison_pill_FGraph) {
                        treesToCompute.put(service.submit(new Callable<FTree>() {
                            @Override
                            public FTree call() throws Exception {
//                            adds poison_pill tree to output
                                return poison_pill_FTree;
                            }
                        }));
                        service.shutdown();
//                        service.shutdownNow();
                        break;
                    } else {
                        treesToCompute.put(service.submit(new Callable<FTree>() {
                            @Override
                            public FTree call() throws Exception {
//                                Thread.currentThread().setName("CompHeu");
                                return computeTreeHeuristically(graph.get());
                            }
                        }));
                    }


                } catch (Exception e) {
                    System.out.println("Warning");
                    e.printStackTrace();
                }
            }
        }

    }
    private class ConsumeGraphExact implements Runnable{
        private Double[] sortedHeuScores;

        ConsumeGraphExact(Double[] sortedHeuScores){
            this.sortedHeuScores = sortedHeuScores;
        }
        public void run(){
//            System.out.println("start heu Tree");
            int processors = Runtime.getRuntime().availableProcessors();
            ExecutorService service = Executors.newFixedThreadPool(processors);

            int thresh = (sortedHeuScores.length / 2);
            int j =-1;
            for (int i = 0; i < sortedHeuScores.length; i++) {
                final int index = i;
                final Collection<MolecularFormula> formulas_for_exact_method = multimap.get(sortedHeuScores[i]);
//                if (formulas_for_exact_method.size()>1){
//                    j += formulas_for_exact_method.size()-1;
//                }
                for (final MolecularFormula formula : formulas_for_exact_method) {
                    j++;
//            for (final MolecularFormula formula : formulasToConsider.keySet()) {
                    final int finalJ = j;
                    futures.add(service.submit(new Callable<Response>() {
                        @Override
                    public Response call() throws Exception {

                        final Response out = new Response();

                        out.index = finalJ;
                        out.result = getScore(computeTreeExactly(computeGraph(formula)));
                        out.prediction = sortedHeuScores[index];
                        return out;
                    }
//                        public FTree call() throws Exception {
////                        final Response out = new Response();
////                        out.index = index;
//                            FTree result = computeTreeExactly(computeGraph(formula));
//                            return result;
//                        }
                    }));
                }
            }

            service.shutdown();
            //            while (true) {
//                try {
//
//                    final Future<FGraph> graph = graphsToCompute.take();
//                    // check if job is done (gets poison_pill)
//                    if (graph.get() == poison_pill_FGraph) {
//                        treesToCompute.put(service.submit(new Callable<FTree>() {
//                            @Override
//                            public FTree call() throws Exception {
////                            adds poison_pill tree to output
//                                return poison_pill_FTree;
//                            }
//                        }));
//                        service.shutdown();
////                        service.shutdownNow();
//                        break;
//                    } else {
//                        treesToCompute.put(service.submit(new Callable<FTree>() {
//                            @Override
//                            public FTree call() throws Exception {
////                                Thread.currentThread().setName("CompHeu");
//                                return computeTreeExactly(graph.get());
//                            }
//                        }));
//                    }
//
//
//                } catch (Exception e) {
//                    System.out.println("Warning");
//                    e.printStackTrace();
//                }
//            }
        }
    }

    public Output startComputation() throws InterruptedException, ExecutionException {

        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(processors);
        ExecutorService service2 = Executors.newFixedThreadPool(1);

        for (int i = 0; i < 1; i++) {
            service2.submit(new ConsumerTrees());
        }

        (new Thread(new ConsumeGraphs())).start();
//        boolean isRunning = true;
//        System.out.println("start graph compute");
//        int i = 0;
        for (final MolecularFormula formula : formulasToConsider.keySet()) {
            //hier vieleicht anonyme Klasse anlegen siehe unten
//            System.out.println(i);
            graphsToCompute.put(service.submit(new CallToRemoteServiceFormula(formula)));
//            i++;
        }
        graphsToCompute.put(service.submit(new CallToRemoteServiceFormula(poison_pill)));



        int x = formulasToConsider.keySet().size();
        while (multimap.size() != x) {
            Thread.sleep(5);
        }
//        service2.awaitTermination();
        service.shutdownNow();
        service2.shutdown();


//        System.out.println("end Heu tree");
        Set<Double> heuScoreSet = multimap.keySet();
        Double[] sortedHeuScores = heuScoreSet.toArray(new Double[multimap.keySet().size()]);
        Arrays.sort(sortedHeuScores);
//        service2.shutdownNow();
        return endComputation(sortedHeuScores);
//        return new Output();
    }

    public Output endComputation(final Double[] sortedHeuScores) throws ExecutionException, InterruptedException {
//        int processors = Runtime.getRuntime().availableProcessors();
//        ExecutorService service = Executors.newFixedThreadPool(processors);
        // build list with response objects


//        int thresh = (sortedHeuScores.length / 2);
        int thresh = 0;
        (new Thread(new ConsumeGraphExact(sortedHeuScores))).start();
//        List<Future<Response>> futures = new ArrayList<Future<Response>>();

//        System.out.println("start exact");
//        int thresh = (sortedHeuScores.length / 2);
//        for (int i = thresh; i < sortedHeuScores.length; i++) {
//            final int index = i;
//            final Collection<MolecularFormula> formulas_for_exact_method = multimap.get(sortedHeuScores[i]);
//            for (final MolecularFormula formula : formulas_for_exact_method) {
////            for (final MolecularFormula formula : formulasToConsider.keySet()) {
//                futures.add(service.submit(new Callable<FTree>() {
//                    @Override
////                    public Response call() throws Exception {
////                        final Response out = new Response();
////                        out.index = index;
////                        out.result = getScore(computeTreeExactly(computeGraph(formula)));
////                        return out;
////                    }
//                    public FTree call() throws Exception {
////                        final Response out = new Response();
////                        out.index = index;
//                        FTree result = computeTreeExactly(computeGraph(formula));
//                        return result;
//                    }
//                }));
//            }
//        }
//        service.shutdown();

        int maxPosition = -1;
        double maxValue = -Double.MAX_VALUE;

        // initialize output
        Double[] presortedExScores = new Double[multimap.size()];
        Double[] predictedScores = new Double[multimap.size()];
//        for (Future<Response> future :futures){
//                while (future.isDone() == false){  //Dont need this
//                    System.out.println("wait");
//                    Thread.sleep(100);
//                }
//                presortedExScores[future.get().index]=future.get().result;
//        }

// if queues are prefered
//        System.out.println("JETZT");
//        Thread.sleep(10000);
//        while (futures.size()==0){
//            Thread.sleep(5);
//        }

        while (futures.size() != (presortedExScores.length-thresh)) {
            Thread.sleep(5);
        }
//        Thread.sleep(500);
        while (futures.size() >0 ) {
            Future<Response> future = futures.poll();
            Response response= future.get();
//            future.get();
            presortedExScores[response.index] = response.result;
            predictedScores[response.index] = response.prediction;

        }
//        service.shutdown();
//        service.shutdownNow();
//        System.out.println("end exact");
//        System.out.println("start");
        for (int j = 0; j < presortedExScores.length; j++) {
//            System.out.println(presortedExScores);
            if (presortedExScores[j] > maxValue) {
                maxValue = presortedExScores[j];
                maxPosition = j;
            }
        }
        int position = sortedHeuScores.length - 1 - maxPosition;
        float cut = ((float) maxPosition / (sortedHeuScores.length - 1));
        Output output = new Output();
        output.cut = cut;
        output.result = maxValue;
        output.prediction = predictedScores[maxPosition];
//        output.prediction =
        output.index = position;
//        System.out.println("stop");
        return output;

    }



    /*
        some helper functions you might need
     */

    private double getScore(FTree tree) {
//        System.out.println("2");
        return tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore();
    }

    private synchronized FTree computeTreeExactly(FGraph graph) {
//        System.out.println("3");
//        long starttime = System.nanoTime();
        FTree test = analyzer.computeTree(graph);
//        long endtime = System.nanoTime();
//        System.out.println(endtime-starttime);
        return test;
    }

    private FGraph computeGraph(MolecularFormula formula) {
        return analyzer.buildGraph(input, formulasToConsider.get(formula));
    }

    private FTree computeTreeHeuristically(FGraph graph) {
//        System.out.println("1");
//        long starttime = System.nanoTime();
        FTree tree = new CriticalPathSolver(graph).solve();
        analyzer.addTreeAnnotations(graph, tree);
//        long endtime = System.nanoTime();
//        System.out.println(endtime-starttime);
        return tree;
    }


}
