package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.io.lcms.MzMLParser;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.lcms.align.IntensityNormalization;
import de.unijena.bioinf.lcms.align.MassOfInterest;
import de.unijena.bioinf.lcms.align.TraceAligner;
import de.unijena.bioinf.lcms.io.MZmlSampleParser;
import de.unijena.bioinf.lcms.merge.TraceMerger;
import de.unijena.bioinf.lcms.trace.*;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * Aktuelle Vorgehensweise:
 * - erstmal alle Samples und deren Traces extrahieren und in die DB speichern
 * - Traces werden dann nochmal in "Trace Chains" organisiert, wobei eine Chain alle Traces mit ähnlicher Masse und
 *   unterschiedlicher (nicht überlappender!) Retentionszeit enthält. Dieser Schritt ist nicht notwendig und kann
 *   wieder rausgenommen werden. Der einzige Vorteil von diesem Schritt ist eigentlich, dass man die minimale
 *   Massenabweichung bekommt, die zwei klar unterschiedliche Traces haben dürfen.
 *
 * - danach werden die Apexe von jedem Trace gesammelt und aligniert
 *      - Alignment findet stufenweise statt
 *      - zuerst aligniert man Apexe die sehr gut aussehen (=Isotopenpeaks haben und/oder hohe Intensität)
 *      - danach wird rekalibriert und nochmal neu aligniert, diesmal alle Apexe
 *      - für jeden Apex speichern wir das "Rechteck" ab, in dem sein Trace sich befindet. D.h. wir wissen
 *        die m/z und rt range über die der Trace verläuft
 * - die Rekalibrierung dient erstmal nur dem bestimmen der Rekalibrierungsfunktionen für m/z und rt. m/z
 *   Rekalibrierung scheint auf den Testdaten nichts zu bringen, aber wer weiß
 *
 * - im nächsten Schritt gehen wir über alle gemergten Apexe und bestimmen die Vereinigung aller zugehörigen Rechtecke
 * - liegen zwei Rechtecke ineinander or haben geringe Massenabweichung werden sie gemerged
 * - ansonsten werden sie separiert, einmal in m/z Richtung und einmal in rt Richtung. So bekommt man zwei Rechtecke,
 *   eines ist breiter in der Retentionszeit, hat aber geringere Massenabweichung, eins ist breiter in der Massenabweichung,
 *   hat aber geringe RT Zeit
 * - alle Rechtecke sind jetzt disjunkt, wir können also nochmal über alle Samples durchgehen und jedes Rechteck nehmen
 *   und alle Intensitäten darin aufsummieren. Für die "Doppel-Rechtecke" gehen wir über beides drüber (sammeln also Peaks
 *   im engen Retentionszeitfenster mit höherer Massenabweichung ein und dann nochmal die äußeren Peaks mit geringerer
 *   Massenabweichung).
 *
 * - ob die doppelten Rechtecke sinnvol sind? Keine Ahnung, sie erlauben aber jedenfalls dass wir am Ende klar definierte
 *   Regionen samplen können, was wiederum den Vorteil hat, dass wir nie versehentlich zwei Peaks doppelt samplen.
 *
 *
 *
 */
public class TestMain {

    public static void main(String[] args) {
        final ProcessedSample[] samples;
        {
            MzMLParser parser = new MzMLParser();
            JobManager globalJobManager = SiriusJobs.getGlobalJobManager();
            File[] files = new File("/home/kaidu/data/raw/polluted_citrus/").listFiles();
            List<BasicJJob<ProcessedSample>> jobs = new ArrayList<>();
            int atmost = 10;
            for (File f : files) {
                if (--atmost < 0) break;
                if (f.getName().endsWith(".mzML")) {
                    jobs.add(SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<ProcessedSample>() {
                        @Override
                        protected ProcessedSample compute() throws Exception {
                            final ProcessedSample sample = new MZmlSampleParser().parse(f, LCMSStorage.temporaryStorage());
                            sample.detectTraces();
                            goodAlignmentPoints(sample);
                            if (false){
                                List<Deviation> devs = new ArrayList<>();
                                Iterator<TraceChain> chains = sample.getTraceStorage().chains();
                                while (chains.hasNext()) {
                                    TraceChain A = chains.next();
                                    Iterator<TraceChain> chains2 = sample.getTraceStorage().chains();
                                    List<Deviation> devs2 = new ArrayList<>();
                                    while (chains2.hasNext()) {
                                        TraceChain B = chains2.next();
                                        if (A.getUid()==B.getUid()) continue;
                                        if (Math.abs(B.averagedMz()-A.averagedMz())>0.1) continue;
                                        devs2.add(Deviation.fromMeasurementAndReference(A.maxMz(),B.maxMz()));
                                        devs2.add(Deviation.fromMeasurementAndReference(A.minMz(),B.minMz()));
                                    }
                                    devs2.stream().min(Comparator.comparingDouble((Deviation x)->Math.abs(x.getAbsolute()))).ifPresent(devs::add);
                                }
                                devs.stream().mapToDouble(x->Math.abs(x.getPpm())).sorted().limit(devs.size()/10).average().ifPresent(System.out::println);
                                devs.stream().mapToDouble(x->Math.abs(x.getAbsolute())).sorted().limit(devs.size()/10).average().ifPresent(System.out::println);
                            }
                            sample.inactive();
                            System.out.println(sample.getReference()  + " done.");
                            return sample;
                        }
                    }));
                }
            }
            samples = jobs.stream().map(JJob::takeResult).toArray(ProcessedSample[]::new);
        }

        TraceAligner traceAligner = new TraceAligner(new IntensityNormalization.QuantileNormalizer(), samples);
        MassOfInterest[] align = traceAligner.align();
        if (false){
            try (final PrintStream moi = new PrintStream("/home/kaidu/analysis/lcms/scripts/mois.csv")) {
                moi.println("mz\trt\tcount");
                for (MassOfInterest m : align) {
                    int n;
                    if (m instanceof TraceAligner.MergedMassOfInterest) {
                        n = ((TraceAligner.MergedMassOfInterest) m).mergedRts.length;
                    } else n = 1;
                    moi.println(m.getMz() + "\t" + m.getRt() + "\t" + n);
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            double m=221.091899;
            try (final PrintStream moi = new PrintStream("/home/kaidu/analysis/lcms/scripts/tr.csv")) {
                moi.println("mz\tintensity\trt\tdelta\tsample\ttrace");
                for (int k=0; k < samples.length; ++k) {
                    ProcessedSample s  = samples[k];
                    UnivariateFunction f = traceAligner.getRecalibrationFunctions()[k];
                    if (f==null) f= new Identity();
                    //List<ContiguousTrace> contigousTracesByMass = s.getTraceStorage().getContigousTracesByMass(m - 0.1, m + 0.1);
                    Iterator<TraceChain> chains = s.getTraceStorage().chains();
                    int j=0;
                    while (chains.hasNext()) {
                        TraceChain t = chains.next();
                        if (Math.abs(t.averagedMz()-221.091899) > 0.1)
                            continue;
                        for (int i=t.startId(); i <= t.endId(); ++i) {
                            final float intensity = t.intensity(i);
                            if (intensity>0) {
                                moi.println(t.mz(i) + "\t" + t.intensity(i) + "\t" + f.value(t.retentionTime(i)) + "\t" + Math.abs(t.mz(i) - t.averagedMz()) + "\t" +
                                        k + "\t" + j + "\n");
                            }
                        }
                        ++j;
                    }
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

        }

        try {
            LCMSStorage mergedStorage = LCMSStorage.temporaryStorage().createNewStorage();
            final ScanPointMapping map = traceAligner.getMergedScanPointMapping();
            TraceMerger merger = new TraceMerger(new TracePicker(mergedStorage, map), map, align);
            merger.merge(samples, Arrays.stream(traceAligner.getRecalibrationFunctions()).map(x->x==null ? new Identity() : x).toArray(UnivariateFunction[]::new), null);
            System.out.println("Done.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void goodAlignmentPoints(ProcessedSample sample) {
        Iterator<TraceChain> chains = sample.getTraceStorage().chains();
        while (chains.hasNext()) {
            TraceChain chain = chains.next();
            for (int traceId : chain.getTraceIds()) {
                TraceNode node = sample.getTraceStorage().getTraceNode(traceId);
                if (node.getConfidence() >= 2) {

                }
            }
        }
    }

}
