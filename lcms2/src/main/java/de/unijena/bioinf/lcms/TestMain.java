package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.lcms.adducts.AdductManager;
import de.unijena.bioinf.lcms.adducts.AdductNetwork;
import de.unijena.bioinf.lcms.adducts.ProjectSpaceTraceProvider;
import de.unijena.bioinf.lcms.align.AlignmentBackbone;
import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.projectspace.ProjectSpaceImporter;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedIsotopicFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX;

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

        Path storeLocation = Files.createTempFile("nitrite", SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(storeLocation)) {
            Database<?> store = ps.getStorage();
            processing.setImportStrategy(new ProjectSpaceImporter(ps));
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
//            processing.exportFeaturesToFiles(merged, bac);

                // TODO check intensity normalization in aligned features
                // FIXME mz in aligned features is weird
                processing.extractFeaturesAndExportToProjectSpace(merged, bac);

                assert store.countAll(MergedLCMSRun.class) == 1;
                for (MergedLCMSRun run : store.findAll(MergedLCMSRun.class)) {
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
                                
                                Features with MS1          : %d
                                Features with MS2          : %d
                                Features with MS1&MS2      : %d
                                """,
                        store.countAll(LCMSRun.class), store.countAll(Scan.class), store.countAll(MSMSScan.class),
                        store.countAll(SourceTrace.class), store.countAll(de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace.class),
                        store.countAll(Feature.class), store.countAll(AlignedIsotopicFeatures.class), store.countAll(AlignedFeatures.class),

                        (int)(store.findAllStr(MSData.class).filter(x->x.getIsotopePattern()!=null && x.getIsotopePattern().size()>=2).count()),
                        (int)(store.findAllStr(MSData.class).filter(x->x.getMergedMSnSpectrum()!=null).count()),
                        (int)(store.findAllStr(MSData.class).filter(x->x.getMergedMSnSpectrum()!=null && x.getIsotopePattern()!=null && x.getIsotopePattern().size()>=2).count())
                );

                // simplify
                HashMap<Long,Integer> runIds = new HashMap<>();

                int count=0;
                for (AlignedFeatures f : store.findAllStr(AlignedFeatures.class, "averageMass", Database.SortOrder.ASCENDING).toList())  {
                    f.setFeatures(store.findStr(Filter.where("alignedFeatureId").eq(f.getAlignedFeatureId()), Feature.class).toList());
                    System.out.println(f.getAverageMass() + " m/z \t" + f.getRetentionTime().getRetentionTimeInSeconds()/60d + " minutes\t" + f.getFeatures().get().size() + " aligned features [" +
                            f.getFeatures().get().stream().map(x->(simpl(runIds, x.getRunId()))).sorted().map(Object::toString).collect(Collectors.joining(", ")) + "]");
                    ++count;
                }


                AdductManager manager = new AdductManager();
                manager.addAdducts(Set.of(PrecursorIonType.getPrecursorIonType("[M+H]+"), PrecursorIonType.getPrecursorIonType("[M+Na]+"),
                        PrecursorIonType.getPrecursorIonType("[M+K]+"),  PrecursorIonType.getPrecursorIonType("[M+NH3+H]+")));
                manager.addLoss(MolecularFormula.parseOrThrow("H2O"));

                AdductNetwork network = new AdductNetwork(new ProjectSpaceTraceProvider(ps),  store.findAllStr(AlignedFeatures.class).toArray(AlignedFeatures[]::new), manager);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static synchronized int simpl(HashMap<Long,Integer> map, long key) {
        if (map.containsKey(key)) return map.get(key);
        else map.put(key, map.size());
        return map.get(key);
    }
}
