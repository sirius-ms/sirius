package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.math.MathUtils;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.PeaklistSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.model.lcms.*;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Ms2CosineSegmenter {

    protected CosineQueryUtils cosine;

    public Ms2CosineSegmenter() {
        cosine = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(new Deviation(20)));
    }

    public List<FragmentedIon> extractMsMSAndSegmentChromatograms(LCMSProccessingInstance instance, ProcessedSample sample) {
        final ArrayList<FragmentedIon> ions = new ArrayList<>();
        // group all MSMS scans into chromatographic peaks
        final HashMap<MutableChromatographicPeak, ArrayList<Scan>> scansPerPeak = new HashMap<>();
        Scan lastMs1 = null;
        for (Scan s : sample.run) {
            if (s.isMsMs()) {
                if (s.getPrecursor().getIndex()>0) {
                    lastMs1 = sample.run.getScanByNumber(s.getPrecursor().getIndex()).filter(x -> !x.isMsMs()).orElse(lastMs1);
                }
                if (lastMs1==null) {
                    LoggerFactory.getLogger(Ms2CosineSegmenter.class).warn("MS2 scan without preceeding MS1 scan is not supported yet.");
                    continue;
                }
                sample.builder.detect(lastMs1, s.getPrecursor().getMass()).ifPresent(peak->scansPerPeak.computeIfAbsent(peak.mutate(),x->new ArrayList<Scan>()).add(s));
            } else {
                lastMs1 = s;
            }
        }

        TLongArrayList medianWidthOfPeaks = new TLongArrayList();
        for (ChromatographicPeak peak : scansPerPeak.keySet()) {
            for (ChromatographicPeak.Segment s : peak.getSegments()) {
                medianWidthOfPeaks.add(s.fwhm());
            }
        }
        medianWidthOfPeaks.sort();
        final long medianWidth = medianWidthOfPeaks.getQuick(medianWidthOfPeaks.size()/2);
        // now iterate over all MSMS and group them by segments:

        int numberOfScansOutside = 0, numberOfMultiple = 0, numberOfInside = 0;

        final TIntObjectHashMap<ArrayList<Scan>> perSegment = new TIntObjectHashMap<>();
        for (Map.Entry<MutableChromatographicPeak, ArrayList<Scan>> entry : scansPerPeak.entrySet()) {
            //System.out.println(entry.getKey().getSegments().size() + " segments and " + entry.getValue().size() + " MS/MS for Scans " + Arrays.toString(entry.getValue().stream().mapToInt(Scan::getScanNumber).toArray()));
            perSegment.clear();
            for (Scan s : entry.getValue()) {
                final Optional<ChromatographicPeak.Segment> segment = entry.getKey().getSegmentForScanId(s.getIndex());
                if (!segment.isPresent()) {
                    LoggerFactory.getLogger(Ms2CosineSegmenter.class).warn("MS2 scan outside of an segment of an chromatographic peak");
                    //System.err.println("MS2 scan outside of an segment of an chromatographic peak: " + s.getScanNumber() + " is not in " + entry.getKey().getSegments().stream().map(x-> Range.closed(x.getStartScanNumber(), x.getEndScanNumber())).collect(Collectors.toList()).toString());
                    ++numberOfScansOutside;
                } else {
                    ++numberOfInside;
                    ArrayList<Scan> scans = perSegment.get(segment.get().getApexScanNumber());
                    if (scans == null) {
                        scans = new ArrayList<>();
                        perSegment.put(segment.get().getApexScanNumber(), scans);
                    }
                    scans.add(s);
                }
            }

            final int[] segmentIds = perSegment.keys();
            Arrays.sort(segmentIds);
            final MergedSpectrum[] spectraPerSegment = new MergedSpectrum[segmentIds.length];
            int k=-1;
            for (int segmentId : segmentIds) {
                ++k;
                CosineQuery[] cos = perSegment.get(segmentId).stream().map(x->prepareForCosine(sample,x)).filter(Objects::nonNull).toArray(CosineQuery[]::new);
                if (cos.length==0) continue;
                MergedSpectrum mergedPeaks = (cos.length==1) ? cos[0].originalSpectrum : mergeViaClustering(sample,cos);
                spectraPerSegment[k] = mergedPeaks;
            }

            // merge across segment ids
            HashSet<ChromatographicPeak.Segment> SEGS = new HashSet<>();
            MergedSpectrum merged = null;
            int j=-1;
            for (int i=0; i < segmentIds.length; ++i) {
                if (spectraPerSegment[i]==null) continue;
                if (merged==null) {
                    merged = spectraPerSegment[i];
                    j = i;
                    continue;
                }
                CosineQuery queryLeft = prepareForCosine(sample, merged);
                CosineQuery queryRight = prepareForCosine(sample, spectraPerSegment[i]);
                SpectralSimilarity cosine = queryLeft.cosine(queryRight);
                final MutableChromatographicPeak mutableChromatographicPeak = entry.getKey().mutate();
                if (cosine.similarity >= 0.75 && cosine.shardPeaks >= 4) {
                    // compute FWHM value for segment
                    final ChromatographicPeak.Segment left = mutableChromatographicPeak.getSegmentForScanId(segmentIds[j]).get();
                    final ChromatographicPeak.Segment right = mutableChromatographicPeak.getSegmentForScanId(segmentIds[i]).get();
                    long gap = entry.getKey().getRetentionTimeAt(right.getFwhmStartIndex()) - entry.getKey().getRetentionTimeAt(left.getFwhmEndIndex());
                    if (gap > medianWidth) {
                        // do not merge
                        //System.out.println("Do not merge " + queryLeft.originalSpectrum.getPrecursor().getMass() + " " + mutableChromatographicPeak.getIntensityAt(left.getApexIndex()) + " with " + mutableChromatographicPeak.getIntensityAt(right.getApexIndex()) + " with cosine "+ cosine.similarity + " (" + cosine.shardPeaks + " peaks), due to gap above " + medianWidth);
                        ions.add(instance.createMs2Ion(sample,merged, entry.getKey(), left));
                        if (!SEGS.add(left))
                            System.out.println("=/");
                        merged = spectraPerSegment[i];
                        j=i;
                    } else {
                        merged = merge(merged, spectraPerSegment[i]);
                        mutableChromatographicPeak.joinAllSegmentsWithinScanIds(segmentIds[j], segmentIds[i]);
                    }
                } else if (cosine.similarity < 0.75) {
                    if (queryLeft.spectrum.size() <= 3 || queryRight.spectrum.size() <= 3) {
                        //System.out.println("Low quality MSMS");
                    } else if (cosine.shardPeaks < Math.min(queryLeft.spectrum.size(),queryRight.spectrum.size())*0.33) {
                        //System.out.println("Split segments");
                        final ChromatographicPeak.Segment left = mutableChromatographicPeak.getSegmentForScanId(segmentIds[j]).get();
                        ions.add(instance.createMs2Ion(sample, merged, entry.getKey(), left));
                        if (!SEGS.add(left))
                            System.out.println("=/");
                        merged = spectraPerSegment[i];
                        j = i;
                    }
                }
            }
            if (merged!=null) {
                final ChromatographicPeak.Segment left = entry.getKey().getSegmentForScanId(segmentIds[j]).get();
                ions.add(instance.createMs2Ion(sample,merged, entry.getKey(), left));
                if (!SEGS.add(left))
                    System.out.println("=/");
            }
            // compute cosine between segments. Check if the cosine is low -> than its probably a different compound
            //System.out.println(ions.size());
        }

        System.out.println("Number of scans outside = " + numberOfScansOutside + ", number of scans inside = " + numberOfInside + ", number of multiplies = " + numberOfMultiple);

        return ions;

    }
    private CosineQuery prepareForCosine(ProcessedSample sample, Scan scan) {
        return prepareForCosine(sample, new MergedSpectrum(scan, sample.storage.getScan(scan), scan.getPrecursor()));
    }
    private CosineQuery prepareForCosine(ProcessedSample sample, MergedSpectrum orig) {
        final SimpleMutableSpectrum buffer = new SimpleMutableSpectrum(orig);
        Spectrums.cutByMassThreshold(buffer,orig.getPrecursor().getMass()-20);
        final double noiseLevel = sample.ms2NoiseModel.getNoiseLevel(orig.getScans().get(0).getIndex(), orig.getScans().get(0).getPrecursor().getMass());
        Spectrums.applyBaseline(buffer, noiseLevel);
        orig.setNoiseLevel(noiseLevel);
        if (buffer.isEmpty()) return null;
        final SimpleSpectrum spec = Spectrums.extractMostIntensivePeaks(buffer,6,100);
        return new CosineQuery(orig, spec);
    }

    protected static class CosineQuery {
        private final double selfNorm;
        private final SimpleSpectrum spectrum;
        private final MergedSpectrum originalSpectrum;

        public CosineQuery(MergedSpectrum orig, SimpleSpectrum spectrum) {
            this.spectrum = spectrum;
            this.originalSpectrum = orig;
            this.selfNorm = new IntensityWeightedSpectralAlignment(new Deviation(20)).score(spectrum,spectrum).similarity;
        }

        public SpectralSimilarity cosine(CosineQuery other) {
            SpectralSimilarity score = new IntensityWeightedSpectralAlignment(new Deviation(20)).score(spectrum, other.spectrum);
            return new SpectralSimilarity(score.similarity / Math.sqrt(selfNorm*other.selfNorm), score.shardPeaks);
        }
    }

    public MergedSpectrum mergeViaClustering(ProcessedSample sample, CosineQuery[] cosines) {

        final double[][] matrix = new double[cosines.length][cosines.length];
        for (int i=0; i < matrix.length; ++i) {
            for (int j=0; j < i; ++j) {
                matrix[i][j] = matrix[j][i]= cosines[i].cosine(cosines[j]).similarity;
            }
        }
        final int[] indizes = new int[cosines.length];
        for (int j=0; j < indizes.length; ++j) indizes[j] = j;
        int maxI=0,maxJ=0, n=matrix.length;
        outerLoop:
        while (n > 1) {
            double max = 0d;
            for (int i=0; i< n; ++i) {
                for (int j=0; j < i; ++j) {
                    if (max < matrix[indizes[i]][indizes[j]]) {
                        max = matrix[indizes[i]][indizes[j]];
                        maxI = indizes[i]; maxJ = indizes[j];
                    }
                }
            }
            if (max < 0.75) {
                break outerLoop;
            } else {
                MergedSpectrum merged = merge(cosines[maxI].originalSpectrum, cosines[maxJ].originalSpectrum);
                cosines[maxI] = prepareForCosine(sample, merged);
                --n;
                for (int k=0; k <= n; ++k) {
                    if (indizes[k]==maxJ) {
                        indizes[k] = indizes[n];
                        break;
                    }
                }
                // recalculate cosines
                for (int i=0; i < n; ++i) {
                    final int index = indizes[i];
                    if (index != maxI)
                        matrix[index][maxI] = matrix[maxI][index] = cosines[maxI].cosine(cosines[index]).similarity;
                }
            }
        }
        double bestTic = 0d; MergedSpectrum bestSpec=null;
        for (int i=0; i < n; ++i) {
            double t = cosines[indizes[i]].originalSpectrum.totalTic();
            if (t > bestTic) {
                bestTic = t;
                bestSpec = cosines[indizes[i]].originalSpectrum;
            }
        }
        return bestSpec;
    }


    public static MergedSpectrum merge(MergedSpectrum left, MergedSpectrum right) {
        // we assume a rather large deviation as signal peaks should be contained in more than one
        // measurement
        final List<MergedPeak> orderedByMz = new ArrayList<>(left.size());
        for (MergedPeak l : left) orderedByMz.add(l);
        final List<MergedPeak> append = new ArrayList<>();
        final Deviation deviation = new Deviation(20,0.05);
        final Spectrum<MergedPeak> orderedByInt;
        {
            final List<MergedPeak> peaks = new ArrayList<>(right.size());
            for (MergedPeak p : right) peaks.add(p);
            peaks.sort(Comparator.comparingDouble(Peak::getIntensity).reversed());
            orderedByInt = new PeaklistSpectrum<MergedPeak>(peaks);
        }

        for (int k = 0; k < orderedByInt.size(); ++k) {
            final double mz =  orderedByInt.getMzAt(k);
            final int mz0 = Spectrums.indexOfFirstPeakWithin(left,mz,deviation);
            if (mz0 >= 0) {
                int mz1 = mz0+1;
                while (mz1 < left.size()) {
                    if (!deviation.inErrorWindow(mz, left.getMzAt(mz1)))
                        break;
                    ++mz1;
                }
                // eigentlich ist dafür das spectral alignment da -_-
                if (mz0 <= mz1) {
                    // merge!
                    int mostIntensive = mz0;
                    double bestScore = Double.NEGATIVE_INFINITY;
                    for (int i = mz0; i < mz1; ++i) {
                        final double massDiff = mz - left.getMzAt(i);
                        final double score =
                               MathUtils.erfc(3 * massDiff) / (deviation.absoluteFor(mz) * Math.sqrt(2)) * left.getIntensityAt(i);
                        if (score > bestScore) {
                            bestScore = score;
                            mostIntensive = i;
                        }
                    }

                    orderedByMz.set(mostIntensive, new MergedPeak(orderedByMz.get(mostIntensive), orderedByInt.getPeakAt(k)));
                }
            } else {
                // append
                append.add(orderedByInt.getPeakAt(k));
            }
        }
        orderedByMz.addAll(append);
        final ArrayList<Scan> scans = new ArrayList<>(left.getScans());
        scans.addAll(right.getScans());
        MergedSpectrum mergedPeaks = new MergedSpectrum(left.getPrecursor(), orderedByMz, scans);
        mergedPeaks.setNoiseLevel(Math.max(left.getNoiseLevel(), right.getNoiseLevel()));
        return mergedPeaks;
    }


}
