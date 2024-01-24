package de.unijena.bioinf.lcms.align;

import de.unijena.bioinf.lcms.trace.Rect;
import lombok.Getter;

import java.util.*;

public class AlignedMoI extends MoI {

    @Getter private final MoI[] aligned;

    AlignedMoI(Rect rect, double retentionTime, float intensity, MoI[] aligned) {
        super(rect, -1, retentionTime, intensity, -1);
        this.aligned = aligned;
    }
    AlignedMoI(Rect rect, double retentionTime, int scanId, int sampleIdx, long uid, float intensity, float confidence, MoI[] aligned, byte state) {
        super(rect, retentionTime, scanId, sampleIdx, uid, intensity, confidence, aligned.length==1 ? aligned[0].getIsotopes() : null, state);
        this.aligned = aligned;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "MoI(mz = %.4f, rt = %.1f, %d alignments)", getRect().avgMz, getRetentionTime(), aligned.length);
    }

    public static AlignedMoI merge(AlignWithRecalibration recalibration, MoI... aligned) {
        double sum=0d;
        double avgMz = 0d, avgRt  = 0d;
        float maxConfidence = 0f;
        byte state = 0;
        double minMz=Double.POSITIVE_INFINITY, maxMz=Double.NEGATIVE_INFINITY, minRt=Double.POSITIVE_INFINITY, maxRt=Double.NEGATIVE_INFINITY;
        for (MoI m : aligned) {
            final RecalibrationFunction f = recalibration.getRecalibrationFor(m);
            avgMz += m.getMz() * m.getIntensity();
            avgRt += f.value(m.getRetentionTime()) * m.getIntensity();
            sum += m.getIntensity();
            maxConfidence = Math.max(m.getConfidence(), maxConfidence);
            minMz = Math.min(m.getMz(), minMz);
            maxMz = Math.max(m.getMz(), maxMz);
            minRt = Math.min(minRt, f.value(m.getRect().minRt));
            maxRt = Math.max(maxRt, f.value(m.getRect().maxRt));
            state |= m.state;
        }
        avgMz /= sum;
        avgRt /= sum;
        final Rect unified = new Rect(minMz,maxMz,minRt,maxRt, avgMz);
        return new AlignedMoI(unified, avgRt, -1, -1, -1, (float)sum, maxConfidence, flatten(aligned), state);
    }

    public static MoI[] flatten(MoI... mois) {
        List<MoI> flattened = new ArrayList<>();
        for (MoI m : mois) {
            if (m instanceof AlignedMoI) {
                flattened.addAll(Arrays.asList(((AlignedMoI) m).aligned));
            } else flattened.add(m);
        }
        return flattened.toArray(MoI[]::new);
    }

    public AlignedMoI finishMerging() {
        Arrays.sort(aligned, Comparator.comparingInt(MoI::getSampleIdx));
        return this;
    }

    public Optional<MoI> forSampleIdx(int sampleIdx) {
        int i = binarySearch(aligned, 0, aligned.length, sampleIdx);
        if (i>=0) return Optional.of(aligned[i]);
        return Optional.empty();
    }

    private int binarySearch(MoI[] a, int fromIndex, int toIndex, int key) {
        int low = fromIndex;
        int high = toIndex - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = a[mid].getSampleIdx();
            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -1;  // key not found.
    }
}
