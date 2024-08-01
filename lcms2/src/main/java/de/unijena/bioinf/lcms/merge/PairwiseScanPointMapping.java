package de.unijena.bioinf.lcms.merge;

import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.align.RecalibrationFunction;

import java.util.Arrays;

public class PairwiseScanPointMapping {

        private float[] right2left;
        private int[] left2right;
        protected double[] rtRights;

        public PairwiseScanPointMapping(ScanPointMapping left, ScanPointMapping right, RecalibrationFunction right2leftRecal) {
            this.left2right = new int[left.length()];
            this.right2left = new float[right.length()];
            Arrays.fill(left2right, 0);
            Arrays.fill(right2left, 0);
            this.rtRights = new double[right.length()];
            int j=0,i=0;
            for (j=0; j < right2left.length; ++j) {
                rtRights[j] = right2leftRecal.value(right.getRetentionTimeAt(j));
                final double rtRight =  rtRights[j];
                while (i < left2right.length-1 && Math.abs(left.getRetentionTimeAt(i+1)-rtRight) < Math.abs(left.getRetentionTimeAt(i)-rtRight)) {
                    ++i;
                }
                right2left[j] = i;
            }
            i=0;j=0;
            for (i=0; i < left2right.length; ++i) {
                final double rtleft = left.getRetentionTimeAt(i);
                while (j < right2left.length && rtRights[j] < rtleft) ++j;
                while (j < right2left.length-1 && Math.abs(rtRights[j+1]-rtleft) < Math.abs(rtRights[j]-rtleft)) {
                    ++j;
                }
                left2right[i] = j;
            }
        }

        public int left2right(int id) {
            return left2right[id];
        }
        public int right2left(int id) {
            return (int)right2left[id];
        }

        private int reverseRtIndex(double y) {
            int i = Arrays.binarySearch(rtRights, y);
            if (i < 0) i = -(i+1);
            if (i>=rtRights.length) return rtRights.length-1;
            return i;

        }
    }

