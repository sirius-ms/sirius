package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.io.lcms.LCMSParser;
import de.unijena.bioinf.io.lcms.MzMLParser;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.lcms.align.IntensityNormalization;
import de.unijena.bioinf.lcms.align.TraceAligner;
import de.unijena.bioinf.lcms.io.MZmlSampleParser;
import de.unijena.bioinf.lcms.trace.*;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.ApexDetection;
import de.unijena.bioinf.lcms.trace.segmentation.LegacySegmenter;
import de.unijena.bioinf.model.lcms.LCMSRun;
import de.unijena.bioinf.model.lcms.Scan;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
        traceAligner.align();

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
