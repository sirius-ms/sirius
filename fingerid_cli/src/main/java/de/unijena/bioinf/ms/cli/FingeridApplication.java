package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.jjobs.MasterJJob;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class FingeridApplication {
    public static void main(String[] args) throws InterruptedException {
        Path w = ApplicationCore.WORKSPACE;
        SiriusJobs.setGlobalJobManager(Integer.parseInt(args[0]));
        try {
            List<JJob<Double>> global = new LinkedList<>();
            for (int i = 1; i < 5000; i++) {
                JJob<Double> job = new MasterJJob<Double>(JJob.JobType.CPU) {
                    @Override
                    protected JobManager jobManager() {
                        return SiriusJobs.getGlobalJobManager();
                    }

                    @Override
                    protected Double compute() throws Exception {
                        List<JJob<Double>> subs = new LinkedList<>();
                        for (int i = 1; i < 1000; i++) {
                            JJob<Double> s = new BasicJJob<Double>() {
                                @Override
                                protected Double compute() throws Exception {
                                    double x = 0d;
                                    for (int i = 1; i < 50000000; ++i) {
                                        if (i % 2 == 0)
                                            x += Math.log(1d / i);
                                        else
                                            x -= Math.log(1d / i);
                                    }
                                    return x;
                                }
                            };
                            submitSubJob(s);
                        }

                        double r = 0;
                        for (JJob<Double> sub : subJobs) {
                            r += sub.takeResult();
                        }
                        return r;
                    }
                };
                SiriusJobs.getGlobalJobManager().submitJob(job);
                global.add(job);
            }

            for (JJob<Double> doubleJJob : global) {
                System.out.println("Job done with result: " + doubleJJob.takeResult());
            }

        } catch (Exception e) {
            LoggerFactory.getLogger(SiriusApplication.class).error("Unkown Error!", e);
        } finally {
//            System.out.println("Time: " + ((double) (System.currentTimeMillis() - t)) / 1000d);
            SiriusJobs.getGlobalJobManager().shutdown();
        }


//        Sirius s = new Sirius();
        /*try {
            Ms2Experiment e = s.parseExperiment(new File("/home/fleisch/Downloads/demo-data/ms/Kaempferol.ms")).next();
//            Ms2Experiment e = s.parseExperiment(new File("/home/qo53kab/demo-data/ms/Kaempferol.ms")).next();
//            File in = new File(args[1]);
//            Ms2Experiment e = s.parseExperiment(in).next();
            s.getMs2Analyzer().setTreeBuilder(new ExtendedCriticalPathHeuristicTreeBuilder());
            LinkedList<JJob> l = new LinkedList<>();
            for (int i = 1; i < 5000; i++) {
                Sirius.SiriusIdentificationJob j = s.makeIdentificationJob(e, 15);
                SiriusJobs.getGlobalJobManager().submitJob(j);
                l.add(j);
            }

            System.out.println("all jobs submitted!");
            while (!l.isEmpty()) {
                System.out.println(l.poll().takeResult().toString());
            }


        } catch (IOException e) {
            e.printStackTrace();
        }*/

        /*long t = System.currentTimeMillis();
        try {
            final FingeridCLI<FingerIdOptions> cli = new FingeridCLI<>();
            cli.parseArgsAndInit(args, FingerIdOptions.class);
            cli.compute();
        } catch (Exception e) {
            LoggerFactory.getLogger(SiriusApplication.class).error("Unkown Error!", e);
        } finally {
            System.out.println("Time: " + ((double) (System.currentTimeMillis() - t)) / 1000d);
            SiriusJobs.getGlobalJobManager().shutdown();
        }*/
    }
}
