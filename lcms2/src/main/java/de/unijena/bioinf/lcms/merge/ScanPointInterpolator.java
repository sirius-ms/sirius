package de.unijena.bioinf.lcms.merge;

import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.align.RecalibrationFunction;
import de.unijena.bioinf.lcms.trace.Trace;

public class ScanPointInterpolator {
    private final float[] left2right;
    private final float[] right2left;
    public ScanPointInterpolator(ScanPointMapping left, ScanPointMapping right, RecalibrationFunction right2leftRecal) {
        int m = left.length();
        int n = right.length();
        left2right = new float[m];
        right2left = new float[n];
        final double[] timeRight = new double[n];
        final double[] timeLeft = new double[m];
        timeRight[0] = right2leftRecal.value(right.getRetentionTimeAt(0));
        for (int k=1; k < n; ++k) {
            timeRight[k] = Math.max(timeRight[k-1], right2leftRecal.value(right.getRetentionTimeAt(k))); // enforce monotony
        }
        for (int k=0; k < left.length(); ++k) timeLeft[k] = left.getRetentionTimeAt(k);
        fill(timeLeft, timeRight, left2right);
        fill(timeRight, timeLeft, right2left);
    }

    private void fill(double[] timeLeft, double[] timeRight, float[]left2right) {
        int leftPointer = 0;
        int rightPointer = 0;
        int m = timeLeft.length;
        int n = timeRight.length;
        while (leftPointer < m) {
            if (rightPointer < n && timeRight[rightPointer] == timeLeft[leftPointer]) {
                // Exact match found
                left2right[leftPointer] = rightPointer;
                leftPointer++;
            } else if (rightPointer < n && timeRight[rightPointer] < timeLeft[leftPointer]) {
                // Move rightPointer to the next element in timeRight
                rightPointer++;
            } else {
                // Perform linear interpolation
                int floorIndex = Math.max(0, rightPointer - 1);
                int ceilIndex = Math.min(n - 1, rightPointer);
                double alpha = (timeLeft[leftPointer] - timeRight[floorIndex]) / (timeRight[ceilIndex] - timeRight[floorIndex]);
                left2right[leftPointer] = (float)(floorIndex + alpha);
                leftPointer++;
            }
        }
    }

    public int lowerIndex(int targetIndex) {
        if (targetIndex < 0 || Float.isInfinite(right2left[targetIndex])) return 0;
        return (int)Math.floor(right2left[targetIndex]);
    }
    public int largerIndex(int targetIndex) {
        if (targetIndex >= right2left.length || Float.isInfinite(right2left[targetIndex])) return left2right.length-1;
        return (int)Math.ceil(right2left[targetIndex]);
    }

    public int reverseMapLowerIndex(int targetIndex) {
        if (targetIndex < 0 || Float.isInfinite(left2right[targetIndex])) return 0;
        return (int)Math.floor(left2right[targetIndex]);
    }

    public int reverseMapLargerIndex(int targetIndex) {
        if (targetIndex >= left2right.length || Float.isInfinite(left2right[targetIndex])) return right2left.length-1;
        return (int)Math.ceil(left2right[targetIndex]);
    }

    public double interpolate(double[] srcArray, int targetIndex) {
        float tg = targetIndex >= left2right.length ? Float.NEGATIVE_INFINITY : (left2right[targetIndex]);
        if (!Float.isFinite(tg)) return 0d;
        int targetLeft = (int)tg;
        int targetRight = (int)Math.ceil(tg);
        if (targetLeft==targetRight) return srcArray[targetLeft];
        double alpha = tg-targetLeft;
        return srcArray[targetLeft]*(1-alpha) + (alpha)*srcArray[targetRight];
    }
    public float interpolate(float[] srcArray, int targetIndex) {
        float tg = targetIndex >= left2right.length ? Float.NEGATIVE_INFINITY : (left2right[targetIndex]);
        if (!Float.isFinite(tg)) return 0f;
        int targetLeft = (int)tg;
        int targetRight = (int)Math.ceil(tg);
        if (targetLeft==targetRight) return srcArray[targetLeft];
        double alpha = tg-targetLeft;
        return (float)(srcArray[targetLeft]*(1-alpha) + (alpha)*srcArray[targetRight]);
    }

    public float interpolateIntensity(Trace t, int k) {
        float tg = k >= left2right.length ? Float.NEGATIVE_INFINITY : (left2right[k]);
        if (!Float.isFinite(tg)) return 0f;
        int targetLeft = (int)tg;
        int targetRight = (int)Math.ceil(tg);
        float intensityLeft = (targetLeft < t.startId() || targetLeft > t.endId()) ? 0 : t.intensity(targetLeft);
        float intensityRight = (targetRight < t.startId() || targetRight > t.endId()) ? 0 : t.intensity(targetRight);
        if (targetLeft==targetRight) return intensityLeft;
        double alpha = tg-targetLeft;
        return (float)( intensityLeft*(1-alpha) + (alpha)*intensityRight );
    }
    public double interpolateMz(Trace t, int k) {
        float tg = k >= left2right.length ? Float.NEGATIVE_INFINITY : (left2right[k]);
        if (!Float.isFinite(tg)) return 0f;
        int targetLeft = (int)tg;
        int targetRight = (int)Math.ceil(tg);
        double mzLeft = (targetLeft < t.startId() || targetLeft > t.endId()) ? t.averagedMz() : t.mz(targetLeft);
        double mzRight = (targetRight < t.startId() || targetRight > t.endId()) ? t.averagedMz() : t.mz(targetRight);
        if (!Double.isFinite(mzLeft)) {
            if (!Double.isFinite(mzRight)) return 0f;
            else return mzRight;
        } else if (!Double.isFinite(mzRight)) {
            return mzLeft;
        }
        if (targetLeft==targetRight) return mzLeft;
        double alpha = tg-targetLeft;
        return ( mzLeft*(1-alpha) + (alpha)*mzRight );
    }
}
