package de.unijena.bioinf.lcms.ionidentity;

import de.unijena.bioinf.ChemistryBase.exceptions.InvalidInputData;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.io.lcms.LCMSParser;
import de.unijena.bioinf.io.lcms.MzMLParser;
import de.unijena.bioinf.io.lcms.MzXMLParser;
import de.unijena.bioinf.lcms.CorrelatedPeakDetector;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.MemoryFileStorage;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.model.lcms.ChromatographicPeak;
import de.unijena.bioinf.model.lcms.CorrelationGroup;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import de.unijena.bioinf.model.lcms.LCMSRun;
import de.unijena.bioinf.ms.properties.PropertyManager;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.lang3.Range;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class LearnCorrelationScoring {

    private final String[] scores;
    private final TDoubleArrayList[][] trueDistributions;
    private final TDoubleArrayList[] decoyDistributions;
    private final int upsample;

    public static void main(String[] args) {
        //final File mzxmlFile = new File("/home/kaidu/software/utils/canopus_treemap/rosmarin_mzml");

        final File mzxmlFile = new File("/home/kaidu/data/raw/diatoms_mzml");
        final LearnCorrelationScoring LEARN = new LearnCorrelationScoring(1);
        MemoryFileStorage storage = null;
        try {
            final LCMSProccessingInstance i = new LCMSProccessingInstance();
            i.setDetectableIonTypes(PropertyManager.DEFAULTS.createInstanceWithDefaults(AdductSettings.class).getDetectable());
            i.getMs2Storage().keepInMemory();
            int k = 0;
            for (File f : mzxmlFile.listFiles()) {
                if (!f.getName().endsWith(".mzXML") && !f.getName().endsWith(".mzML"))
                    continue;
                if (++k > 100)
                    break;
                storage = new MemoryFileStorage();
                LCMSParser parser;
                if (f.getName().endsWith(".mzXML"))
                    parser = new MzXMLParser();
                else
                    parser = new MzMLParser();
                final LCMSRun parse = parser.parse(f, storage);
                final ProcessedSample sample;
                try {
                    sample = i.addSample(parse, storage);
                } catch (InvalidInputData e) {
                    System.err.println("Error while processing run " + f + ": " + e.getMessage());
                    continue;
                }
                i.detectFeatures(sample);

                sample.ions.removeIf(x->x.getPeakShape().getPeakShapeQuality().notBetterThan(Quality.UNUSABLE));

                System.out.println(sample.run.getIdentifier());
                LEARN.learnFromSample(sample);

                storage.backOnDisc();
                storage.dropBuffer();
            }
            i.getMs2Storage().backOnDisc();
            i.getMs2Storage().dropBuffer();

            LEARN.dump(new File("/home/kaidu/analysis/lcms/correlation_test.csv"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LearnCorrelationScoring(int upsample) {
        this.scores = new String[]{
                "cosine","correlation","maximum-likelihood","maximum-likelihood2","kullback-leibler","patlen"
        };
        this.upsample = upsample;
        this.trueDistributions = new TDoubleArrayList[scores.length][];
        this.decoyDistributions = new TDoubleArrayList[scores.length];
        for (int i=0; i < scores.length; ++i) {
            this.trueDistributions[i] = new TDoubleArrayList[5];
            this.decoyDistributions[i] = new TDoubleArrayList();
            for (int k=0; k < 5; ++k) {
                this.trueDistributions[i][k] = new TDoubleArrayList();
            }
        }
    }

    public void dump(File csv) throws IOException {
        try (final BufferedWriter w = FileUtils.getWriter(csv)) {
            w.write("real\tisotope");
            for (String score : scores) {
                w.write('\t');
                w.write(score);
            }
            w.newLine();
            for (int iso=0; iso < trueDistributions[0].length; ++iso) {
                final int n = trueDistributions[0][iso].size();
                for (int k=0; k < n; ++k) {
                    w.write('1');
                    w.write('\t');
                    w.write(String.valueOf(iso));
                    for (int score = 0; score < trueDistributions.length; ++score) {
                        w.write('\t');
                        w.write(String.valueOf(trueDistributions[score][iso].get(k)));
                    }
                    w.newLine();
                }
            }
            final int n2 = decoyDistributions[0].size();
            for (int k=0; k < n2; ++k) {
                w.write('0');
                w.write('\t');
                w.write('0');
                for (int score = 0; score < decoyDistributions.length; ++score) {
                    w.write('\t');
                    w.write(String.valueOf(decoyDistributions[score].get(k)));
                }
                w.newLine();
            }
        }
    }

    public void learnFromSample(ProcessedSample sample) {
        int totalCors = 0;
        {
            for (int k=0; k < 5; ++k) {
                final ArrayList<CorrelationGroup> groups = new ArrayList<>();
                for (FragmentedIon ion : sample.ions) {
                    var xs = ion.getIsotopes();
                    if (xs.size() > k) {
                        groups.add(xs.get(k));
                    }
                }
                if (groups.size() <= 5)
                    break;

                TDoubleArrayList cosines = trueDistributions[0][k],
                        correlations = trueDistributions[1][k],
                        mls = trueDistributions[2][k],
                        mls2 = trueDistributions[3][k],
                        klbs = trueDistributions[4][k],
                        patlen = trueDistributions[5][k];


                cosines.addAll(groups.stream().mapToDouble(x -> x.getCosine()).toArray());
                correlations.addAll(groups.stream().mapToDouble(x -> x.getCorrelation()).toArray());
                mls.addAll(groups.stream().mapToDouble(x -> x.score).toArray());
                klbs.addAll(groups.stream().mapToDouble(x -> x.getKullbackLeibler()).toArray());
                patlen.addAll(groups.stream().mapToDouble(x->x.getNumberOfCorrelatedPeaks()).toArray());
                totalCors += groups.size();
            }
        }
        totalCors *= upsample;
        {
            TDoubleArrayList cosines = decoyDistributions[0],
                    correlations = decoyDistributions[1],
                    mls = decoyDistributions[2],
                    mls2 = decoyDistributions[3],
                    klbs = decoyDistributions[4],
            patlen = decoyDistributions[5];
            // compare with random correlations
            final ArrayList<FragmentedIon> A = new ArrayList<>(sample.ions), B = new ArrayList<>(sample.ions);
            int counter = 0;
            while (counter < totalCors) {
                Collections.shuffle(A);
                Collections.shuffle(B);
                for (int k = 0; k < A.size(); ++k) {
                    final ChromatographicPeak.Segment a = A.get(k).getSegment();
                    final ChromatographicPeak.Segment b = B.get(k).getSegment();
                    final TDoubleArrayList as = new TDoubleArrayList(), bs = new TDoubleArrayList();
                    final Range<Integer> l = a.calculateFWHM(0.15);
                    final Range<Integer> r = b.calculateFWHM(0.15);
                    int lenL = Math.min(a.getApexIndex()-l.getMinimum(), b.getApexIndex()-r.getMinimum());
                    int lenR = Math.min(l.getMaximum()-a.getApexIndex(), r.getMaximum()-b.getApexIndex());
                    if (lenL>=2 && lenR >= 2) {
                        ++counter;
                        as.add(a.getApexIntensity());
                        bs.add(b.getApexIntensity());
                        for (int x=1; x  <lenR; ++x) {
                            as.add(a.getPeak().getIntensityAt(a.getApexIndex()+x));
                            bs.add(b.getPeak().getIntensityAt(b.getApexIndex()+x));
                        }
                        for (int x=1; x  <lenL; ++x) {
                            as.insert(0,a.getPeak().getIntensityAt(a.getApexIndex()-x));
                            bs.insert(0, b.getPeak().getIntensityAt(b.getApexIndex()-x));
                        }

                        cosines.add(CorrelatedPeakDetector.cosine(as,bs));
                        correlations.add(CorrelatedPeakDetector.pearson(as,bs));
                        mls.add(CorrelatedPeakDetector.maximumLikelihoodIsotopeScore(as,bs));
                        mls2.add(CorrelatedPeakDetector.maximumLikelihoodIsotopeScore2(as,bs));
                        klbs.add(CorrelatedPeakDetector.kullbackLeibler(as,bs, as.size()));
                        patlen.add(bs.size());
                    }
                }
            }
        }
    }

}
