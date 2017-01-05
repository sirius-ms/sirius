package de.unijena.bioinf.FragmentationTreeConstruction.computation;

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
import java.util.concurrent.TimeoutException;

import static de.unijena.bioinf.ChemistryBase.chem.MolecularFormula.*;
import static sun.rmi.transport.TransportConstants.Call;

/**
 * - compute fragmentation graphs in parallel
 * - compute heuristics in parallel
 * - use heuristics to estimate subset of molecular formulas to compute exactly
 * - (optional) compute trees in parallel if Gurobi is used
 */
class Response {
    int index;
    double result;
}


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

//    private final MolecularFormula POISON_PILL = new MolecularFormula();

    public FasterMultithreadedTreeComputation(FragmentationPatternAnalysis analyzer) {
        this.graphsToCompute = new ArrayBlockingQueue<>(10000);
        this.treesToCompute = new ArrayBlockingQueue<>(10); // TODO change back to 10
        this.map = new TreeMap<>();
        this.analyzer = analyzer;

    }

    public void setInput(ProcessedInput input) {
        setInput(input,null);
    }

    public void setInput(ProcessedInput input, HashSet<MolecularFormula> formulas) {
        this.input = input;
        this.formulasToConsider = new HashMap<>();
        for (Scored<MolecularFormula> formula : input.getPeakAnnotationOrThrow(DecompositionList.class).get(input.getParentPeak()).getDecompositions()) {
            if (formulas==null || formulas.contains(formula.getCandidate()))
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
            if(formula == poison_pill)
            {
                return poison_pill_FGraph;
            }
            return computeGraph(formula);
        }

    }
    //Consumer Class in Java
    private class ConsumerTrees implements Runnable{

//        private final BlockingQueue sharedQueue;
//
//        public ConsumerTrees (BlockingQueue sharedQueue) {
//            this.sharedQueue = sharedQueue;
//        }

        @Override
        public void run() {
            while(true){
                try {
                    final Future<FTree> heuTree = treesToCompute.take();
                    if(heuTree.get() == poison_pill_FTree){

                        System.out.println(Thread.currentThread().getName());
                        break;
                    }
                    else{
//                        System.out.println(Thread.currentThread().getName());
//                        System.out.println(getScore(heuTree.get()));
//                        do some clever stuff with the tree
                    }
//                    System.out.println("Consumed: "+ sharedQueue.take());
                } catch (InterruptedException e) {
                    e.printStackTrace();
//                    Logger.getLogger(ConsumerTrees.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }


    }


    public void startComputation() throws InterruptedException, ExecutionException {

        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(processors);
        ExecutorService service2 = Executors.newFixedThreadPool(8);
        for(int i = 0; i<processors; i++){
            service2.submit(new ConsumerTrees());
        }



//        for(final MolecularFormula formula : formulasToConsider.keySet()){
//            graphsToCompute.put(formula);
//        }

        boolean isRunning = true;

        for(final MolecularFormula formula : formulasToConsider.keySet()){
            //hier vieleicht anonyme Klasse anlegen siehe unten
            graphsToCompute.put(service.submit(new CallToRemoteServiceFormula(formula)));
        }
        graphsToCompute.put(service.submit(new CallToRemoteServiceFormula(poison_pill)));


        while(isRunning){
            try{
                final Future<FGraph> graph= graphsToCompute.take();
                // check if job is done (gets poison_pill)
                if(graph.get() == poison_pill_FGraph){
//                    System.out.println("trololo");
                    treesToCompute.put(service.submit(new Callable<FTree>() {
                        @Override
                        public FTree call() throws Exception {
//                            adds poison_pill tree to output
                            return poison_pill_FTree;
                        }
                    }));
//                    System.out.println("worked");
                    break;
                }
                else{
                    treesToCompute.put(service.submit(new Callable<FTree>() {
                        @Override
                        public FTree call() throws Exception {
                            return computeTreeHeuristically(graph.get());
                        }
                    }));
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

//        while (isRunning){
//            final Future<FTree> heuTree = treesToCompute.take();
//
//            if(heuTree.get() == poison_pill_FTree){
////                System.out.println("yippie");
//                break;
//            }
//            else{
////                System.out.println(getScore(heuTree.get()));
//            }
//        }
        // TODO was ist der runner bei future object
        // wie schaffen das blocking queue nicht so groß sein muss ohne zweite while(isRunning) in erste zu integrieren
        // brauch ich zweite threadgruppe (service2) ??


        // kann man erkennen ob /wann die heuristic verkackt?!

//        HashMap<Double, MolecularFormula> scoreMap = new HashMap<Double, MolecularFormula>();
//        for(final MolecularFormula formula : formulasToConsider.keySet()){
//            scoreMap.put( getScore(computeTreeHeuristically(computeGraph(formula))),formula);
//        }
//
//        //sorting heuristic scores
//        Set<Double> heuScoreSet = scoreMap.keySet();
//        Double[] sortedHeuScores = heuScoreSet.toArray(new Double[scoreMap.keySet().size()]);
//        Arrays.sort(sortedHeuScores);
//
//
//        // build list with response objects
//        List<Future<Response>> futures = new ArrayList<Future<Response>>();
//
//        // override the call
//        // calc the input for the object
//        // return object and put it in the list
//        for(int i =0; i < sortedHeuScores.length; i++ ){
//            final int index = i;
//            final MolecularFormula foo = scoreMap.get(sortedHeuScores[i]);
//            futures.add(service.submit(new Callable<Response>() {
//                @Override
//                public Response call() throws Exception {
//                    final Response out = new Response();
//                    out.index = index;
//                    out.result = getScore(computeTreeExactly(computeGraph(foo)));
//                    return out;
//                }
//            }));
//        }
//
//        // initialize output
//        Double[] presortedExScores = new Double[sortedHeuScores.length];
//        int maxPosition = -1;
//        double maxValue = -Double.MAX_VALUE;
//
//        // build from list of resonse objects the exact-value-array
//        for(Future<Response> future : futures){
//            presortedExScores[future.get().index]= future.get().result;
//        }
//        // search for max in exact-value-array
//        for(int j = 0; j < presortedExScores.length; j++){
//            if(presortedExScores[j] > maxValue){
//                maxValue = presortedExScores[j];
//                maxPosition = j;
//            }
//        }
//
//        int i = 5;
//        return i;

    }
    //this is used to signal from the main thread that he producer has finished adding stuff to the queue
//    public void finish() {
//        //you can also clear here if you wanted
//        isRunning = false;
//        inputQueue.add(POISON_PILL);
//    }

    public void startComputation_old() throws InterruptedException, ExecutionException {
//
//        HashMap<Double, MolecularFormula> scoreMap = new HashMap<Double, MolecularFormula>();
//        for(final MolecularFormula formula : formulasToConsider.keySet()){
//            scoreMap.put( getScore(computeTreeHeuristically(computeGraph(formula))),formula);
//        }
//
//        //sorting heuristic scores
//        Set<Double> heuScoreSet = scoreMap.keySet();
//        Double[] sortedHeuScores = heuScoreSet.toArray(new Double[scoreMap.keySet().size()]);
//        Arrays.sort(sortedHeuScores);
//
//        int processors = Runtime.getRuntime().availableProcessors();
//        ExecutorService service = Executors.newFixedThreadPool(processors);
//        // build list with response objects
//        List<Future<Response>> futures = new ArrayList<Future<Response>>();
//
//        // override the call
//        // calc the input for the object
//        // return object and put it in the list
//        for(int i =0; i < sortedHeuScores.length; i++ ){
//            final int index = i;
//            final MolecularFormula foo = scoreMap.get(sortedHeuScores[i]);
//            futures.add(service.submit(new Callable<Response>() {
//                @Override
//                public Response call() throws Exception {
//                    final Response out = new Response();
//                    out.index = index;
//                    out.result = getScore(computeTreeExactly(computeGraph(foo)));
//                    return out;
//                }
//            }));
//        }
//
//        // initialize output
//        Double[] presortedExScores = new Double[sortedHeuScores.length];
//        int maxPosition = -1;
//        double maxValue = -Double.MAX_VALUE;
//
//        // build from list of resonse objects the exact-value-array
//        for(Future<Response> future : futures){
//            presortedExScores[future.get().index]= future.get().result;
//        }
//        // search for max in exact-value-array
//        for(int j = 0; j < presortedExScores.length; j++){
//            if(presortedExScores[j] > maxValue){
//                maxValue = presortedExScores[j];
//                maxPosition = j;
//            }
//        }
//
//        System.out.println(sortedHeuScores.length-1 - maxPosition);
//        // prints the fraction off exact Scores which are NOT needed to be calculated
//        float output = ((float)maxPosition/(sortedHeuScores.length-1));
//        System.out.println(output);
//        return(output);

// SINGLE-THREAD
////        HashMap<Double, MolecularFormula> scoreMap = new HashMap<Double, MolecularFormula>();
//        for (MolecularFormula formula : formulasToConsider.keySet()) {
//            double heuScore = getScore(computeTreeHeuristically(computeGraph(formula)));
//            scoreMap.put(heuScore,formula);
//
//        }
//
//        for (int i = 0; i < sortedHeuScores.length; i++){
//            MolecularFormula foo = scoreMap.get(sortedHeuScores[i]);
//            presortedExScores[i] = getScore(computeTreeExactly(computeGraph(foo)));
//
//            if (presortedExScores[i] > maxValue){
//                maxValue = presortedExScores[i];
//                maxPosition = i;
//            }
//        }

    }


    /*
        some helper functions you might need
     */

    private double getScore(FTree tree) {
        return tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore();
    }

    private synchronized FTree computeTreeExactly(FGraph graph) {
        return analyzer.computeTree(graph);
    }

    private FGraph computeGraph(MolecularFormula formula) {
        return analyzer.buildGraph(input, formulasToConsider.get(formula));
    }

    private FTree computeTreeHeuristically(FGraph graph) {
        FTree tree = new CriticalPathSolver(graph).solve();
        analyzer.addTreeAnnotations(graph, tree);
        return tree;
    }







}
