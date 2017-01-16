package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import com.google.common.collect.*;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Score;
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
    Multimap<Double, MolecularFormula> multimap = Multimaps.synchronizedListMultimap(
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
                    final Future<FTree> heuTree = treesToCompute.take(); //waits until element available
//                    System.out.println("1");
                    FTree test = heuTree.get();
                    if(heuTree.get() == poison_pill_FTree){
                        //damit scoreMap vollständig
                        while (formulasToConsider.keySet().size() != multimap.size()){
                            Thread.sleep(50);

//                            System.out.println("keyset: "+(formulasToConsider.keySet().size()) + " scoremap: " + (multimap.size()));
                        }
                        System.out.println("hallo");
//                        Set<Double> heuScoreSet = scoreMap.keySet();
//                        Double[] sortedHeuScores = heuScoreSet.toArray(new Double[scoreMap.keySet().size()]);
//                        Arrays.sort(sortedHeuScores);
//                        return sortedHeuScores;
//                        endComputation(sortedHeuScores);
//                        System.out.println(Thread.currentThread().getName());

                        break;
                    }
                    else{
//                        map.put(test.getRoot().getFormula(),test);
//                        System.out.println("2");
//                        Thread.sleep(100);
//                        scoreMap.put(test.getRoot().getFormula(),getScore(test));
//                        scoreMap2.put(getScore(test),test.getRoot().getFormula());
                        multimap.put(getScore(test),test.getRoot().getFormula());
//                        System.out.println(multimap.containsKey(getScore(test)));
//                        System.out.println(getScore(test)+ " Formel:" + test.getRoot().getFormula().toString());
//                        System.out.println("keyset: "+(formulasToConsider.keySet().size()) + " scoremap: " + (multimap.size()));
//                        System.out.println(Thread.currentThread().getName());
//                        System.out.println(getScore(heuTree.get()));
//                        do some clever stuff with the tree
                    }
//                    System.out.println("Consumed: "+ sharedQueue.take());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.out.println("Warning");
//                    Logger.getLogger(ConsumerTrees.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    System.out.println("Warning");
                }
            }
        }


    }
    private class Response {
        int index;
//        double prediction;
        double result;
    }

    public void startComputation() throws InterruptedException, ExecutionException {

        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(processors);
        ExecutorService service2 = Executors.newFixedThreadPool(processors);
//        foo;
//        foo
        for(int i = 0; i<processors; i++){
//            foo.put(service2.submit(new ConsumerTrees()));
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
                            Thread.sleep(1000);
                            return poison_pill_FTree;
                        }
                    }));
//                    Thread.sleep(1000);
                    service.shutdown();
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
                System.out.println("Warning");
                e.printStackTrace();
            }
        }
//        Thread.sleep(2000);
        int x = formulasToConsider.keySet().size();
//        while (x != scoreMap.keySet().size()){
        while (multimap.size() != x){
//            System.out.println("keyset: "+(formulasToConsider.keySet().size()) + " scoremap: " + multimap.size());
            Thread.sleep(100);
        }
        service2.shutdown();
        Multiset<Double> heuScoreSet = multimap.keys();
        Double[] sortedHeuScores = heuScoreSet.toArray(new Double[x]);
        Arrays.sort(sortedHeuScores);
//        endComputation(sortedHeuScores);
//                        System.out.println(Thread.currentThread().getName());

        ///////''''''''''''###########

//        while(true){
//            try {
//                final Future<FTree> heuTree = treesToCompute.take(); //waits until element available
//                FTree test = heuTree.get();
//                if(heuTree.get() == poison_pill_FTree){
//                    //damit scoreMap vollständig
//                    while (formulasToConsider.keySet().size() != scoreMap.keySet().size()){
//                        Thread.sleep(50);
////                            System.out.println("keyset: "+(formulasToConsider.keySet().size()) + " scoremap: " + (scoreMap.keySet().size()));
//                    }
//                    Set<Double> heuScoreSet = scoreMap.keySet();
//                    Double[] sortedHeuScores = heuScoreSet.toArray(new Double[scoreMap.keySet().size()]);
//                    Arrays.sort(sortedHeuScores);
//                    endComputation(sortedHeuScores);
////                        System.out.println(Thread.currentThread().getName());
//
//                    break;
//                }
//                else{
////                        map.put(test.getRoot().getFormula(),test);
//                    scoreMap.put( getScore(test),test.getRoot().getFormula());
////                        System.out.println(Thread.currentThread().getName());
////                        System.out.println(getScore(heuTree.get()));
////                        do some clever stuff with the tree
//                }
////                    System.out.println("Consumed: "+ sharedQueue.take());
//            } catch (InterruptedException e) {
//                e.printStackTrace();
////                    Logger.getLogger(ConsumerTrees.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (ExecutionException e) {
//                e.printStackTrace();
//            }
//        }

        ///////////''''''''''''''''#######





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
    public void endComputation(final Double[] sortedHeuScores) throws ExecutionException, InterruptedException {
        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(processors);
        // build list with response objects
        List<Future<Response>> futures = new ArrayList<Future<Response>>();

        // override the call
        // calc the input for the object
        // return object and put it in the list

        // change startpoint of Array to use heuristic infos for speedup
        for(int i =0; i < sortedHeuScores.length; i++ ){
            final int index = i;
            final Collection<MolecularFormula> formulas_for_exact_method = multimap.get(sortedHeuScores[i]);
            for (final MolecularFormula formula : formulas_for_exact_method){
                futures.add(service.submit(new Callable<Response>() {
                    @Override
                    public Response call() throws Exception {
                        final Response out = new Response();
                        out.index = index; // TODO change to "best prediction in front" CAUTION if I do this than always have to remind this in future!!
//                    out.prediction= sortedHeuScores[index];
                        out.result = getScore(computeTreeExactly(computeGraph(formula)));
                        return out;
                    }
                }));
            }

        }
//        service.shutdown();
        // initialize output
        Double[] presortedExScores = new Double[sortedHeuScores.length];
        int maxPosition = -1;
        double maxValue = -Double.MAX_VALUE;

        // build from list of resonse objects the exact-value-array
        for(Future<Response> future : futures){
            presortedExScores[future.get().index]= future.get().result;
        }
        // search for max in exact-value-array
        for(int j = 0; j < presortedExScores.length; j++){
            if(presortedExScores[j] > maxValue){
                maxValue = presortedExScores[j];
                maxPosition = j;
            }
        }
        int position = sortedHeuScores.length-1 - maxPosition;
        float cut = ((float)maxPosition/(sortedHeuScores.length-1));
//        write in file
        System.out.println("position: "+position);
        System.out.println("prediction: " + sortedHeuScores[maxPosition]);
        System.out.println("result: " + maxValue);
        System.out.println("cut: "+cut);
//        TODO close pools

//        if(position>10 && cut > 0.95){
//            System.out.println("position: "+position);
//            // prints the fraction off exact Scores which are NOT needed to be calculated
//            System.out.println("cut: "+cut);
//            System.out.println("length: "+sortedHeuScores.length);
//
//        }
//        else {
//            System.out.println("fine");
//        }

//        float output = ;
//        System.out.println(output);
//        return(output);
    }

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
