package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.lcms.align.AlignmentBackbone;
import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.projectspace.ProjectSpaceImporter;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedIsotopicFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedRun;
import de.unijena.bioinf.ms.persistence.model.core.run.Run;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDatabaseImpl;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

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

    public static void main(String[] args) throws IOException {
        try (InputStream is = TestMain.class.getClassLoader().
                getResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        LCMSOptions ops = new LCMSOptions();
        CommandLine cmd = new CommandLine(ops);
        cmd.parseArgs(args);

        if (cmd.isUsageHelpRequested()){
            cmd.usage(System.err);
            return;
        }

        final de.unijena.bioinf.lcms.trace.ProcessedSample[] samples;
        LCMSProcessing processing = new LCMSProcessing();

        Path storeLocation = Files.createTempFile("nitrite", "db");
        try (NitriteDatabase db = new NitriteDatabase(storeLocation, SiriusProjectDocumentDatabase.buildMetadata())) {
            Database<?> store = new SiriusProjectDatabaseImpl<>(db).getStorage();
            processing.setImportStrategy(new ProjectSpaceImporter(store));
            {
                if (ops.cores >= 1) {
                    SiriusJobs.setGlobalJobManager(ops.cores);
                }
                JobManager globalJobManager = SiriusJobs.getGlobalJobManager();
                System.out.println(globalJobManager.getCPUThreads());
//            File[] files = new File("/home/kaidu/analysis/lcms/diverse_collection/small").listFiles();
//            File[] files = new File("/home/kaidu/analysis/lcms/diverse_collection/MSV000080627/").listFiles();
                //File[] files = new File("/home/kaidu/data/raw/polluted_citrus/").listFiles();
                List<File> files = ops.getInputFiles();
                System.setProperty("lcms.logdir", ops.getLogDir().toAbsolutePath().toString());

                List<BasicJJob<de.unijena.bioinf.lcms.trace.ProcessedSample>> jobs = new ArrayList<>();
                int atmost = Integer.MAX_VALUE;
                for (File f : files) {
                    if (--atmost < 0) break;
                    if (f.getName().toLowerCase().endsWith(".mzml")) {
                        jobs.add(SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<de.unijena.bioinf.lcms.trace.ProcessedSample>() {
                            @Override
                            protected de.unijena.bioinf.lcms.trace.ProcessedSample compute() throws Exception {
                                ProcessedSample sample = processing.processSample(f);
                                int hasIsotopes = 0, hasNoIsotopes = 0;
                                for (MoI m : sample.getStorage().getAlignmentStorage()) {
                                    if (m.hasIsotopes()) ++hasIsotopes;
                                    else ++hasNoIsotopes;
                                }
                                sample.inactive();
                                System.out.println(sample.getUid() + " with " + hasIsotopes + " / " + (hasIsotopes + hasNoIsotopes) + " isotope features");
                                return sample;
                            }
                        }));
                    }
                }
                //samples = jobs.stream().map(JJob::takeResult).toArray(de.unijena.bioinf.lcms.trace.ProcessedSample[]::new);
                int count = 0;
                for (BasicJJob<ProcessedSample> job : jobs) {
                    System.out.println(job.takeResult().getUid() + " (" + ++count + " / " + jobs.size() + ")");
                }
            }
            try {
                AlignmentBackbone bac = processing.align();
                ProcessedSample merged = processing.merge(bac);
                {
                    int hasIsotopes = 0, hasNoIsotopes = 0;
                    for (MergedTrace t : merged.getStorage().getMergeStorage()) {
                        if (t.getIsotopeUids().size() > 0) {
                            ++hasIsotopes;
                        } else ++hasNoIsotopes;
                    }
                    System.out.println("merged sample with " + hasIsotopes + " / " + (hasIsotopes + hasNoIsotopes) + " isotope features");
                }
                DoubleArrayList avgAl = new DoubleArrayList();
                System.out.println("AVERAGE = " + avgAl.doubleStream().sum() / avgAl.size());
                System.out.println("Good Traces = " + avgAl.doubleStream().filter(x -> x >= 5).sum());
//            processing.exportFeaturesToFiles(merged, bac);

                // TODO check intensity normalization in aligned features
                // FIXME mz in aligned features is weird
                // TODO how to handle MS/MS?
                processing.extractFeaturesAndExportToProjectSpace(merged, bac);

                assert store.countAll(MergedRun.class) == 1;
                for (MergedRun run : store.findAll(MergedRun.class)) {
                    System.out.printf("\nMerged Run: %s\n\n", run.getName());
                }

                System.out.printf(
                        """
                                # Run:                     %d
                                # Scan:                    %d
                                # MSMSScan:                %d
                                # SourceTrace:             %d
                                # MergedTrace:             %d
                                # Feature:                 %d
                                # AlignedIsotopicFeatures: %d
                                # AlignedFeatures:         %d
                                
                                Feature                 SNR: %f
                                AlignedIsotopicFeatures SNR: %f
                                AlignedFeatures         SNR: %f
                                """,
                        store.countAll(Run.class), store.countAll(Scan.class), store.countAll(MSMSScan.class),
                        store.countAll(SourceTrace.class), store.countAll(de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace.class),
                        store.countAll(Feature.class), store.countAll(AlignedIsotopicFeatures.class), store.countAll(AlignedFeatures.class),
                        store.findAllStr(Feature.class).mapToDouble(Feature::getSnr).average().orElse(Double.NaN),
                        store.findAllStr(AlignedIsotopicFeatures.class).mapToDouble(AlignedIsotopicFeatures::getSnr).average().orElse(Double.NaN),
                        store.findAllStr(AlignedFeatures.class).mapToDouble(AlignedFeatures::getSnr).average().orElse(Double.NaN)
                );

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
