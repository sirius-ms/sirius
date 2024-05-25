package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bionf.spectral_alignment.CosineQuerySpectrum;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Data
public class MergedSpectrum implements Spectrum<MergedPeak> {

    private List<MergedPeak> mergedPeaks=new ArrayList<>();
    //private DoubleArrayList ms2Weights;
    private CosineQuerySpectrum cosineQuerySpectrum;
    private double precursorMz, precursorMzWeighted, scoreSum;
    private Ms2SpectrumHeader[] headers = new Ms2SpectrumHeader[0];

    private IntArrayList sampleIds = new IntArrayList();
    private double chimericPollution;

    public void merge(MsMsQuerySpectrum msms) {
        int sampleId = msms.sampleId;
        final Ms2SpectrumHeader header = msms.header;
        SimpleSpectrum ms2 = msms.originalSpectrum;
        final Deviation highDev = new Deviation(10), lowDev = new Deviation(4);
        final double weight = msms.score;
        final BitSet merged = new BitSet(ms2.size());
        // get rid of satellite peaks
        ms2 = Spectrums.mergePeaksWithinSpectrum(ms2, lowDev, true, false);
        if (!mergedPeaks.isEmpty()) {
            mergedPeaks.sort(Comparator.comparingDouble(MergedPeak::getIntensity).reversed());
            for (int k = 0; k < mergedPeaks.size(); ++k) {
                int match = Spectrums.mostIntensivePeakWithin(ms2, mergedPeaks.get(k).getMass(), lowDev);
                if (match < 0) match = Spectrums.mostIntensivePeakWithin(ms2, mergedPeaks.get(k).getMass(), highDev);
                if (match >= 0 && !merged.get(match)) {
                    merged.set(match);
                    mergedPeaks.set(k, new MergedPeak(mergedPeaks.get(k),
                            new TrackablePeak(ms2.getMzAt(match), (float) ms2.getIntensityAt(match), sampleId, header.getUid(), match, (float) weight))
                    );
                }
            }
        }
        // add all remaining peaks
        for (int unmatched = merged.nextClearBit(0); unmatched < ms2.size(); unmatched = merged.nextClearBit(unmatched+1)  ) {
            mergedPeaks.add(new MergedPeak(new TrackablePeak[]{new TrackablePeak(
                    ms2.getMzAt(unmatched), (float)ms2.getIntensityAt(unmatched), sampleId, header.getUid(), unmatched, (float)weight
            )}));
        }
        scoreSum += msms.score;
        precursorMzWeighted += msms.score*msms.exactParentMass;
        precursorMz = precursorMzWeighted/scoreSum;
        cosineQuerySpectrum=null;
        headers = Arrays.copyOf(headers, headers.length+1);
        headers[headers.length-1] = msms.header;
        sampleIds.add(msms.sampleId);

        this.chimericPollution += weight * (msms.chimericPollution/msms.ms1Intensity);
    }

    public double getChimericPollutionRatio() {
        if (scoreSum==0) return 0d;
        return this.chimericPollution / scoreSum;
    }

    public CollisionEnergy[] getCollisionEnergies() {
        return Arrays.stream(headers).map(x->x.getEnergy().orElse(null)).toArray(CollisionEnergy[]::new);
    }
    public IsolationWindow[] getIsolationWindows() {
        return Arrays.stream(headers).map(x->x.getIsolationWindow().orElse(null)).toArray(IsolationWindow[]::new);
    }

    public int[] getSampleIds() {
        return sampleIds.toIntArray();
    }

    public CosineQuerySpectrum getCosineQuerySpectrum() {
        if (cosineQuerySpectrum==null) {
            cosineQuerySpectrum = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(new Deviation(10))).createQuery(new SimpleSpectrum(this), precursorMz, true, false);
        }
        return cosineQuerySpectrum;
    }

    @Override
    public double getMzAt(int index) {
        return mergedPeaks.get(index).getMass();
    }

    @Override
    public double getIntensityAt(int index) {
        return mergedPeaks.get(index).getIntensity();
    }

    @Override
    public MergedPeak getPeakAt(int index) {
        return mergedPeaks.get(index);
    }

    @Override
    public int size() {
        return mergedPeaks.size();
    }

    @NotNull
    @Override
    public Iterator<MergedPeak> iterator() {
        return mergedPeaks.iterator();
    }

    public double getAveragedPrecursorMz() {
        return precursorMzWeighted/scoreSum;
    }
}
