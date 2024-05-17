package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.ChemistryBase.algorithm.BinarySearch;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.spectrum.SpectrumStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.ProjectedTrace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MergeGreedyStrategy implements Ms2MergeStrategy{

    @Override
    public MergedSpectrum[][] assignMs2(ProcessedSample mergedSample, MergedTrace trace, TraceSegment[] mergedSegments, TraceSegment[][] projectedSegments) {
        if (mergedSegments.length==0) return new MergedSpectrum[0][];
        final ArrayList<MsMsQuerySpectrum>[] queriesPerSegment = new ArrayList[mergedSegments.length];
        assignMs2ToSegments(mergedSample, trace, mergedSegments, projectedSegments, queriesPerSegment);
        MergedSpectrum[][] mtrx = new MergedSpectrum[mergedSegments.length][];
        for (int k=0; k < queriesPerSegment.length; ++k) {
            if (queriesPerSegment[k].isEmpty()) continue;
            score(queriesPerSegment[k]);
            queriesPerSegment[k].sort(Comparator.comparingDouble((MsMsQuerySpectrum x)->x.score).reversed());
            mtrx[k] = groupByCeAndMerge(queriesPerSegment[k]);
        }
        return mtrx;
    }

    public static MergedSpectrum[] groupByCeAndMerge(List<MsMsQuerySpectrum> msMsQuerySpectrums) {
        // first group all spectra according to their collision energy
        HashMap<CollisionEnergy, List<MsMsQuerySpectrum>> e2msms = new HashMap<>();
        List<MsMsQuerySpectrum> noce=new ArrayList<>();
        for (MsMsQuerySpectrum spec : msMsQuerySpectrums) {
            if (spec.header.getEnergy().isPresent()) {
                CollisionEnergy ce = spec.header.getEnergy().get();
                e2msms.computeIfAbsent(ce, c->new ArrayList<>()).add(spec);
            } else {
                noce.add(spec);
            }
        }
        MergedSpectrum[] specs = new MergedSpectrum[(noce.isEmpty() ? 0 : 1) + e2msms.size()];
        int k=0;
        if (!noce.isEmpty()) specs[k++] = merge(noce);
        for (List<MsMsQuerySpectrum> alist : e2msms.values()) specs[k++] = merge(alist);
        return specs;
    }

    public static MergedSpectrum merge(List<MsMsQuerySpectrum> msMsQuerySpectrums) {
        final Deviation dev = new Deviation(6);
        final CosineQueryUtils ut = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(new Deviation(10)));
        // start with the MsMs with highest score, then merge in everything that has good cosine
        final MergedSpectrum merged = new MergedSpectrum();
        merged.merge(msMsQuerySpectrums.get(0));
        final List<MsMsQuerySpectrum> unmerged = new ArrayList<>();
         // now merge everything that has at least 0.7 cosine and 6 peaks
        for (int k=1; k < msMsQuerySpectrums.size(); ++k) {
            final MsMsQuerySpectrum msms = msMsQuerySpectrums.get(k);
            if (dev.inErrorWindow(merged.getPrecursorMz(), msms.exactParentMass)) {
                SpectralSimilarity spectralSimilarity = ut.cosineProduct(merged.getCosineQuerySpectrum(), msms.filteredSpectrum);
                if (spectralSimilarity.sharedPeaks >= 4 && spectralSimilarity.similarity >= 0.7) {
                    merged.merge(msms);
                    continue;
                }
            }
            unmerged.add(msMsQuerySpectrums.get(k));
            // do something else with unmerged?
        }
        final int countMerged = (msMsQuerySpectrums.size()-unmerged.size());
        return merged;
    }

    private void score(ArrayList<MsMsQuerySpectrum> msMsQuerySpectrums) {
        final double maxIntensity = msMsQuerySpectrums.stream().mapToDouble(x->x.ms1Intensity).max().orElse(1d);
        final double maxTic = msMsQuerySpectrums.stream().mapToDouble(x->x.filteredSpectrum.getSelfSimilarity()).max().orElse(1d);
        for (int k=0; k < msMsQuerySpectrums.size(); ++k) {
            MsMsQuerySpectrum msms = msMsQuerySpectrums.get(k);
            msms.score = (msms.chimericPollution>0 ? Math.min(10, msms.ms1Intensity/msms.chimericPollution) : 10)/3d + msms.ms1Intensity/maxIntensity + msms.filteredSpectrum.getSelfSimilarity()/maxTic;
        }
    }



    private static void assignMs2ToSegments(ProcessedSample mergedSample, MergedTrace mergedTrace, TraceSegment[] mergeSegments, TraceSegment[][] featureSegments, ArrayList<MsMsQuerySpectrum>[] queriesPerSegment) {
        for (int k = 0; k < mergeSegments.length; ++k) {
            queriesPerSegment[k] = new ArrayList<>();
        }
        ScanPointMapping mapping = mergedSample.getMapping();
        for (int k=0; k < mergedTrace.getTraces().length; ++k) {
            ProcessedSample sample = mergedTrace.getSamples()[k];
            ProjectedTrace projTrace = mergedTrace.getTraces()[k];
            int[] ms2ids = projTrace.getMs2Ids();
            // obtain MsMs spectra
            SpectrumStorage msStorage = sample.getStorage().getSpectrumStorage();
            Ms2SpectrumHeader[] headers = Arrays.stream(ms2ids).mapToObj(msStorage::ms2SpectrumHeader).toArray(Ms2SpectrumHeader[]::new);
            MsMsQuerySpectrum[] querySpectrum = new MsMsQuerySpectrum[headers.length];
            for (int j=0; j < headers.length; ++j) {
                querySpectrum[j] = new MsMsQuerySpectrum(
                        headers[j], sample.getUid(), msStorage.getMs2Spectrum(headers[j].getUid()),
                        msStorage.getSpectrum(headers[j].getParentId())
                );
            }
            TraceSegment[] childsegs = featureSegments[k];
            if (childsegs ==null) {
                LoggerFactory.getLogger(MergeGreedyStrategy.class).warn("MsMs outside of any feature segment!");
                continue;
            }
            double THRESHOLD = Arrays.stream(childsegs).mapToDouble(x->x==null ? 0 : mapping.getRetentionTimeAt(x.rightEdge)-mapping.getRetentionTimeAt(x.leftEdge)).max().orElse(0d);

            // we assign each header to its closest segment
            // note that a header might lie outside a segment but still inside the trace. I would allow this
            // as it might be rather an alignment error than a real measurement issue
            final int sampleIdx = k;
            for (int j=0; j < querySpectrum.length; ++j) {
                int bestAssignment = -1;
                double bestFit = Double.POSITIVE_INFINITY;
                final double rt = sample.getRtRecalibration().value(headers[j].getRetentionTime());
                for (int i = 0; i < childsegs.length; ++i) {
                    TraceSegment c = childsegs[i];
                    if (c==null) continue;
                    double distLeft = mapping.getRetentionTimeAt(c.leftEdge) - rt;
                    double distRight = mapping.getRetentionTimeAt(c.rightEdge) - rt;
                    if (distLeft<=0 && distRight>=0) {
                        bestAssignment = i;
                        bestFit=0;
                        break;
                    } else if (distLeft>0) { // ms2 is left from segment
                        if (distLeft<bestFit) {
                            bestFit=distLeft;
                            bestAssignment=i;
                        }
                    } else if (distRight<0) { //ms2 is right from segment
                        if (-distRight<bestFit) {
                            bestFit = -distRight;
                            bestAssignment=i;
                        }

                    }
                }
                if (bestAssignment>=0 && bestFit <= THRESHOLD) {
                    // assign query to this segment
                    queriesPerSegment[bestAssignment].add(querySpectrum[j]);
                }
            }
        }
    }
}
