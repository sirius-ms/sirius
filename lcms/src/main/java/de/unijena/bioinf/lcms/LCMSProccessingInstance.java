package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.model.lcms.*;
import gnu.trove.list.array.TDoubleArrayList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

public class LCMSProccessingInstance {

    protected SpectrumStorage storage;
    protected List<ProcessedSample> samples;

    public LCMSProccessingInstance() {
        this.samples = new ArrayList<>();
        this.storage = new InMemoryStorage();
    }

    public ProcessedSample addSample(LCMSRun run) {
        final ProcessedSample sample = new ProcessedSample(
                run, new GlobalNoiseModel(storage, run.getScans().stream().filter(x->!x.isMsMs()).collect(Collectors.toList()), 0.85),
                new GlobalNoiseModel(storage, run.getScans().stream().filter(x->x.isMsMs()).collect(Collectors.toList()), 0.85),
                new ChromatogramCache(), storage
        );
        this.samples.add(sample);
        return sample;

    }

    public void detectFeatures() {
        for (ProcessedSample sample : samples) {
            final List<FragmentedIon> ions = new Ms2CosineSegmenter().extractMsMSAndSegmentChromatograms(sample);
            ListIterator<FragmentedIon> iter = ions.listIterator();
            final CorrelatedPeakDetector detector = new CorrelatedPeakDetector();
            while (iter.hasNext()) {
                final FragmentedIon ion = iter.next();
                if (!detector.detectCorrelatedPeaks(sample, ion))
                    iter.remove();
            }
            sample.ions.clear();
            sample.ions.addAll(new IonIdentityNetwork().filterByIonIdentity(ions));
        }
    }

    public void alignFeatures() {

        final TDoubleArrayList std = new TDoubleArrayList();

        final FeatureAligner aligner = new FeatureAligner();
        final double[][] alignmentScores = new double[samples.size()][samples.size()];
        final AlignedIon[][][] alignedIons = new AlignedIon[samples.size()][samples.size()][];
        final AlignedIon[] empty = new AlignedIon[0];

        final File target = new File("/home/kaidu/temp/X");


        for (int i=0; i < samples.size(); ++i) {

            {
                try (PrintStream out = new PrintStream(new File(target, i + ".csv"))) {
                    for (FragmentedIon ion : samples.get(i).ions) {
                        out.println(ion.getPeak().getRetentionTimeAt(ion.getSegment().getApexIndex()));
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            alignedIons[i][i] = empty;
            for (int j=0; j < i; ++j) {
                alignedIons[i][j] = alignedIons[j][i] = aligner.align(samples.get(i), samples.get(j));
                for (AlignedIon a : alignedIons[i][j]) {
                    alignmentScores[i][j] += a.getScore();
                    std.add(a.getLeft().getPeak().getRetentionTimeAt(((FragmentedIon)a.getLeft()).getSegment().getApexIndex()) - a.getRight().getPeak().getRetentionTimeAt(((FragmentedIon)a.getRight()).getSegment().getApexIndex()));
                }
                /////
                final File x = new File(target, i + "_" + j+".csv");
                final double[][] aaa = new double[alignedIons[i][j].length][2];
                for (int k=0; k < alignedIons[i][j].length; ++k) {
                    final AlignedIon alignedIon = alignedIons[i][j][k];
                    aaa[k][0] = alignedIon.getLeft().getPeak().getRetentionTimeAt(((FragmentedIon)alignedIon.getLeft()).getSegment().getApexIndex());
                    aaa[k][1] = alignedIon.getRight().getPeak().getRetentionTimeAt(((FragmentedIon)alignedIon.getRight()).getSegment().getApexIndex());
                }
                try {
                    FileUtils.writeDoubleMatrix(x, aaa);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        for (int i=0; i < samples.size(); ++i) {
            System.out.print(alignedIons[i][0].length);
            for (int j=1; j < samples.size(); ++j) {
                System.out.print("\t"); System.out.print(alignedIons[i][j].length);
            }
            System.out.println("");
        }

        std.sort();
        System.out.println("Average = " + (std.sum()/std.size())/60000 + " seconds" );
        double error = 0d;
        for (double vla : std.toArray()) error += vla*vla;
        System.out.println("Average Error = " + (Math.sqrt(error/std.size()))/60000 + " seconds" );
        System.out.println("Median = " + (std.getQuick(std.size())/2d)/60000 + " seconds" );
        std.transformValues(Math::abs);
        std.sort();
        System.out.println("Median = Error" + (std.getQuick(std.size())/2d)/60000 + " seconds" );
    }


    public List<ProcessedSample> getSamples() {
        return samples;
    }

    public SpectrumStorage getStorage() {
        return storage;
    }
}
