package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.ChemistryBase.algorithm.BinarySearch;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.spectrum.SpectrumStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.*;

public class MergeGreedyStrategy implements Ms2MergeStrategy{

    @Override
    public void assignMs2(ProcessedSample mergedSample, MergedTrace trace, TraceSegment[] featureSegments, Int2ObjectOpenHashMap<ProcessedSample> otherSamples, AssignMs2ToFeature assigner) {
        if (featureSegments.length==0) return;
        final ArrayList<MsMsQuerySpectrum>[] queriesPerSegment = new ArrayList[featureSegments.length];
        assignMs2ToSegments(mergedSample, trace, featureSegments, otherSamples, queriesPerSegment);
        for (int k=0; k < queriesPerSegment.length; ++k) {
            if (queriesPerSegment[k] == null || queriesPerSegment[k].isEmpty()) continue;
            score(mergedSample, trace, otherSamples, queriesPerSegment[k]);
            queriesPerSegment[k].sort(Comparator.comparingDouble((MsMsQuerySpectrum x)->x.score).reversed());
            MergedSpectrum mergedSpectrum = merge(queriesPerSegment[k]);
            assigner.assignMs2ToFeature(trace, featureSegments[k], k, mergedSpectrum, queriesPerSegment[k]);
        }
    }

    private MergedSpectrum merge(ArrayList<MsMsQuerySpectrum> msMsQuerySpectrums) {
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

    private void score(ProcessedSample mergedSample, MergedTrace trace, Int2ObjectOpenHashMap<ProcessedSample> otherSamples, ArrayList<MsMsQuerySpectrum> msMsQuerySpectrums) {
        final double maxIntensity = msMsQuerySpectrums.stream().mapToDouble(x->x.ms1Intensity).max().orElse(1d);
        final double maxTic = msMsQuerySpectrums.stream().mapToDouble(x->x.filteredSpectrum.getSelfSimilarity()).max().orElse(1d);
        for (int k=0; k < msMsQuerySpectrums.size(); ++k) {
            MsMsQuerySpectrum msms = msMsQuerySpectrums.get(k);
            msms.score = Math.min(10, msms.ms1Intensity/msms.chimericPollution) + msms.ms1Intensity/maxIntensity + msms.filteredSpectrum.getSelfSimilarity()/maxTic;
        }
    }



    private static void assignMs2ToSegments(ProcessedSample mergedSample, MergedTrace trace, TraceSegment[] featureSegments, Int2ObjectOpenHashMap<ProcessedSample> otherSamples, ArrayList<MsMsQuerySpectrum>[] queriesPerSegment) {
        TraceSegment[] filteredFS = Arrays.stream(featureSegments).filter(Objects::nonNull).toArray(TraceSegment[]::new);
        if (filteredFS.length == 0) {
            return;
        }
        for (int k = 0; k < filteredFS.length; ++k) {
            queriesPerSegment[k] = new ArrayList<>();
        }
        for (Int2ObjectMap.Entry<int[]> entry : trace.getMs2SpectraIds().int2ObjectEntrySet()) {
            int sampleId = entry.getIntKey();
            ProcessedSample sample = otherSamples.get(sampleId);
            SpectrumStorage msStorage = sample.getStorage().getSpectrumStorage();
            Ms2SpectrumHeader[] headers = Arrays.stream(entry.getValue()).mapToObj(msStorage::ms2SpectrumHeader).toArray(Ms2SpectrumHeader[]::new);
            MsMsQuerySpectrum[] querySpectrum = new MsMsQuerySpectrum[headers.length];
            for (int k=0; k < headers.length; ++k) {
                querySpectrum[k] = new MsMsQuerySpectrum(
                        headers[k], sampleId, msStorage.getMs2Spectrum(headers[k].getUid()),
                        msStorage.getSpectrum(headers[k].getParentId())
                );
            }
            // we assign each header to its closest segment
            for (int k=0; k < querySpectrum.length; ++k) {
                // FIXME java.lang.NullPointerException: Cannot read field "rightEdge" because "x" is null
                int i = BinarySearch.searchForDouble(Arrays.asList(filteredFS), x -> mergedSample.getMapping().getRetentionTimeAt(x.rightEdge),
                        sample.getRtRecalibration().value(headers[k].getRetentionTime()));
                if (i<0) {
                    i = -(i+1);
                }
                if (i >= filteredFS.length) {
                    i = filteredFS.length-1;
                }
                if (i < filteredFS[i].leftEdge && i > 0) {
                    if (Math.abs(filteredFS[i-1].rightEdge-i) < Math.abs(filteredFS[i].leftEdge-i)) {
                        i=i-1;
                    }
                } else if (i > filteredFS[i].rightEdge && i+1 < filteredFS.length) {
                    if (Math.abs(filteredFS[i+1].leftEdge-i) < Math.abs(filteredFS[i].rightEdge-i)) {
                        i=i+1;
                    }
                } else {
                    // done
                }
                queriesPerSegment[i].add(querySpectrum[k]);
            }

        }
    }
}
