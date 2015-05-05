package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.SimpleReduction;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GLPKSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GurobiSolver;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CLI {

    protected final static String VERSION_STRING = "Sirius 3.0.0";

    public static void main(String[] args) {
        {
            String s = System.getProperty("java.library.path", "");
            String x = System.getProperty("sirius.root", ".");
            System.setProperty("java.library.path", s + ";" + x);
        }
        final HashMap<String, Task> tasks = new HashMap<String, Task>();
        final IdentifyTask identify = new IdentifyTask();
        final ComputeTask compute = new ComputeTask();
        tasks.put(identify.getName(), identify);
        tasks.put(compute.getName(), compute);
        //tasks.put("test-reduce", new TestReduction());
        //tasks.put("test-glpk", new TestGlkp());
        if (args.length==0) displayHelp(tasks);

        Task currentTask = null;
        int argStart=0;

        for (int k=0; k < args.length; ++k) {
            if (currentTask==null && (args[k].equals("-h") || args[k].equals("--help"))) {
                displayHelp(tasks); return;
            } else if (args[k].equals("--version")) {
                displayVersion(); return;
            } else if (currentTask==null){
                currentTask = tasks.get(args[k]);
                argStart=k+1;
                if (currentTask==null) {
                    System.err.println("Unknown task: " + args[k]);
                    displayHelp(tasks); return;
                }
            } else if (k==args.length-1) {
                final String[] taskArgs = new String[k-argStart+1];
                System.arraycopy(args, argStart, taskArgs, 0, taskArgs.length);
                try {
                    currentTask.setArgs(taskArgs);
                } catch (HelpRequestedException e) {
                    displayVersion();
                    System.out.println(currentTask.getName() + ":\t" + currentTask.getDescription());
                    System.out.println("");
                    System.out.println(e.getMessage());
                    System.exit(0);
                }
                currentTask.run();
            }
        }

    }

    private static void displayVersion() {
        System.out.println(VERSION_STRING);
    }

    private static void displayHelp(HashMap<String, Task> tasks) {
        displayVersion();
        System.out.println("usage: sirius [COMMAND] [OPTIONS] [INPUT]");
        System.out.println("For documentation of available options use: sirius [COMMAND] --help");
        System.out.println("\nAvailable commands:");
        for (Map.Entry<String, Task> entry : tasks.entrySet()) {
            final String descr = entry.getValue().getDescription().replaceAll("\n", "\n\t\t").replace("\n\t\t$","\n");
            System.out.println("\t" + entry.getKey() + ":\t" + descr);
        }
    }

    private static class TestReduction extends ComputeTask{

        @Override
        public String getName() {
            return "test-reduce";
        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public void compute() {
            try {
                final Iterator<Instance> instanceIterator = handleInput(options);
                while (instanceIterator.hasNext()) {
                    Instance i = instanceIterator.next();
                    if (i.experiment.getMolecularFormula() == null && options.getMolecularFormula() == null) {
                        System.err.println("The molecular formula for '" + i.file + "' is missing. Add the molecular formula via --formula option or use sirius identify to predict the correct molecular formula");
                    } else {
                        if (i.experiment.getMolecularFormula()==null) {
                            final MutableMs2Experiment expm;
                            if (i.experiment instanceof MutableMs2Experiment) expm = (MutableMs2Experiment) i.experiment;
                            else expm = new MutableMs2Experiment(i.experiment);
                            expm.setMolecularFormula(MolecularFormula.parse(options.getMolecularFormula()));
                            i = new Instance(expm, i.file);
                        }
                        System.out.println("Compute " + i.file + " (" + i.experiment.getMolecularFormula() + ")");
                        final long time1 = System.nanoTime();
                        final IdentificationResult result = sirius.compute(i.experiment, i.experiment.getMolecularFormula(), options);
                        final long time2 = System.nanoTime();
                        System.out.println("Computation took " + ((time2-time1)/1000000000d) + "ms");
                        try {
                            final long time3 = System.nanoTime();
                            sirius.getMs2Analyzer().setReduction(new SimpleReduction());
                            final IdentificationResult result2 = sirius.compute(i.experiment, i.experiment.getMolecularFormula(), options);
                            final long time4 = System.nanoTime();
                            System.out.println("With Reduction: computation took " + ((time4-time3)/1000000000d) + "ms");
                            if (Math.abs(result.getScore()-result2.getScore()) > 1e-3) {
                                output(i, result);
                                output(new Instance(i.experiment, new File(i.file.getParent(), "reduced.ms")), result2);
                                throw new RuntimeException("Different trees for " + i.file + " | " + result.getScore() + " vs. " + result2.getScore());
                            }
                        } finally {
                            sirius.getMs2Analyzer().setReduction(null);
                        }


                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class TestGlkp extends ComputeTask {

        @Override
        public String getName() {
            return "test-glpk";
        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public void compute() {
            try {
                final Iterator<Instance> instanceIterator = handleInput(options);
                while (instanceIterator.hasNext()) {
                    Instance i = instanceIterator.next();
                    if (i.experiment.getMolecularFormula() == null && options.getMolecularFormula() == null) {
                        System.err.println("The molecular formula for '" + i.file + "' is missing. Add the molecular formula via --formula option or use sirius identify to predict the correct molecular formula");
                    } else {
                        if (i.experiment.getMolecularFormula()==null) {
                            final MutableMs2Experiment expm;
                            if (i.experiment instanceof MutableMs2Experiment) expm = (MutableMs2Experiment) i.experiment;
                            else expm = new MutableMs2Experiment(i.experiment);
                            expm.setMolecularFormula(MolecularFormula.parse(options.getMolecularFormula()));
                            i = new Instance(expm, i.file);
                        }
                        System.out.println("Compute " + i.file + " (" + i.experiment.getMolecularFormula() + ")");
                        final long time1 = System.nanoTime();
                        final IdentificationResult result = sirius.compute(i.experiment, i.experiment.getMolecularFormula(), options);
                        final long time2 = System.nanoTime();
                        System.out.println("Computation took " + ((time2-time1)/1000000000d) + "ms");
                        try {
                            final long time3 = System.nanoTime();
                            sirius.getMs2Analyzer().setTreeBuilder(new GLPKSolver());
                            final IdentificationResult result2 = sirius.compute(i.experiment, i.experiment.getMolecularFormula(), options);
                            final long time4 = System.nanoTime();
                            assert result!=null;
                            assert  result2!=null;
                            System.out.println("With GLPK: computation took " + ((time4-time3)/1000000000d) + "ms");
                            if (Math.abs(result.getScore()-result2.getScore()) > 1e-3) {
                                output(i, result);
                                output(new Instance(i.experiment, new File(i.file.getParent(), "reduced.ms")), result2);
                                throw new RuntimeException("Different trees for " + i.file + " | " + result.getScore() + " vs. " + result2.getScore());
                            }
                        } finally {
                            sirius.getMs2Analyzer().setTreeBuilder(new GurobiSolver());
                        }


                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
